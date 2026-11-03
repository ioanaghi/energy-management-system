package com.ds.energy.auth_service;

import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {
    private final CredentialRepository credentials;
    private final UserClient users;
    private final PasswordEncoder passwords;
    private final JwtEncoder tokens;
    private final String dummyHash;

    public AccountService(CredentialRepository credentials, UserClient users, PasswordEncoder passwords, JwtEncoder tokens) {
        this.credentials = credentials;
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        dummyHash = passwords.encode("invalid-login-password");
    }

    public UserProfile create(AccountRequest request) {
        validatePassword(request.password(), true);
        if (request.role() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }
        if (credentials.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use");
        }
        String hash = passwords.encode(request.password());
        UserProfile user = users.create(request);
        try {
            credentials.saveAndFlush(new Credential(user.id(), request.username(), hash));
        } catch (RuntimeException error) {
            users.delete(user.id());
            if (error instanceof DataIntegrityViolationException) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use");
            }
            throw error;
        }
        return user;
    }

    public UserProfile register(AccountRequest request) {
        return create(new AccountRequest(request.username(), request.password(), "CLIENT", request.name(), request.address()));
    }

    public LoginResponse login(LoginRequest request) {
        Credential credential = credentials.findByUsername(request.username()).orElse(null);
        boolean valid = passwords.matches(request.password(), credential == null ? dummyHash : credential.getPasswordHash());
        if (credential == null || !valid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        UserProfile user = users.get(credential.getUserId());
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().subject(user.id().toString()).issuer("energy-auth")
                .audience(List.of("energy-api")).issuedAt(now).expiresAt(now.plusSeconds(3600))
                .claim("role", user.role()).build();
        String token = tokens.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, "bearer", user);
    }

    public UserProfile update(Long id, AccountRequest request) {
        validatePassword(request.password(), false);
        if (request.role() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }
        if (credentials.existsByUsernameAndUserIdNot(request.username(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use");
        }
        Credential credential = credentials.findById(id).orElse(null);
        if (credential == null) {
            validatePassword(request.password(), true);
        }
        String hash = request.password() == null ? credential.getPasswordHash() : passwords.encode(request.password());
        UserProfile user = users.update(id, request);
        credentials.saveAndFlush(new Credential(id, request.username(), hash));
        return user;
    }

    public void delete(Long id) {
        users.delete(id);
        credentials.deleteById(id);
    }

    private void validatePassword(String password, boolean required) {
        if ((required && password == null) || (password != null && (password.length() < 8 || password.length() > 128))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must have between 8 and 128 characters");
        }
    }
}
