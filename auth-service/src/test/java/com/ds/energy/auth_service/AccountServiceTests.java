package com.ds.energy.auth_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountServiceTests {
    @Autowired private AccountService accounts;
    @Autowired private CredentialRepository credentials;
    @Autowired private PasswordEncoder passwords;
    @MockitoBean private UserClient users;

    @Test
    void registersClientWithHashedPasswordAndIssuesJwt() {
        UserProfile profile = new UserProfile(42L, "client", "CLIENT", "Test", "Address");
        when(users.create(any())).thenReturn(profile);
        when(users.get(42L)).thenReturn(profile);
        accounts.register(new AccountRequest("client", "Client123!", "ADMIN", "Test", "Address"));
        verify(users).create(argThat(request -> request.role().equals("CLIENT")));
        var credential = credentials.findByUsername("client").orElseThrow();
        assertNotEquals("Client123!", credential.getPasswordHash());
        assertTrue(passwords.matches("Client123!", credential.getPasswordHash()));
        var result = accounts.login(new LoginRequest("client", "Client123!"));
        var decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec("test-jwt-secret-with-more-than-32-characters".getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();
        var jwt = decoder.decode(result.accessToken());
        assertEquals("42", jwt.getSubject());
        assertEquals("CLIENT", jwt.getClaimAsString("role"));
        assertTrue(jwt.getAudience().contains("energy-api"));
        assertEquals(3600, jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond());
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> accounts.login(new LoginRequest("client", "Wrong123!"))).getStatusCode().value());
    }

    @Test
    void rejectsShortPasswordsAndDuplicateUsernames() {
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> accounts.register(new AccountRequest("client", "short", null, "", ""))).getStatusCode().value());
        credentials.saveAndFlush(new Credential(42L, "client", passwords.encode("Client123!")));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> accounts.create(new AccountRequest("client", "Client123!", "CLIENT", "", ""))).getStatusCode().value());
        verifyNoInteractions(users);
    }

    @Test
    void keepsPasswordWhenOmittedAndChangesItWhenProvided() {
        credentials.saveAndFlush(new Credential(42L, "client", passwords.encode("Client123!")));
        when(users.update(eq(42L), any())).thenReturn(new UserProfile(42L, "renamed", "CLIENT", "", ""));
        accounts.update(42L, new AccountRequest("renamed", null, "CLIENT", "", ""));
        assertTrue(passwords.matches("Client123!", credentials.findById(42L).orElseThrow().getPasswordHash()));
        accounts.update(42L, new AccountRequest("renamed", "Changed123!", "CLIENT", "", ""));
        assertTrue(passwords.matches("Changed123!", credentials.findById(42L).orElseThrow().getPasswordHash()));
        assertFalse(passwords.matches("Client123!", credentials.findById(42L).orElseThrow().getPasswordHash()));
    }
}
