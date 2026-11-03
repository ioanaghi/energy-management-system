package com.ds.energy.auth_service;

import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Hidden;

@RestController
@Validated
public class AuthController {
    private final AccountService accounts;

    public AuthController(AccountService accounts) { this.accounts = accounts; }

    @Operation(summary = "Register a client", description = "Creates a client profile and stores an Argon2 password hash. Public registration always uses CLIENT role.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Client created"), @ApiResponse(responseCode = "400", description = "Invalid registration data"), @ApiResponse(responseCode = "409", description = "Username already exists")})
    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfile register(@Valid @RequestBody AccountRequest request) { return accounts.register(request); }

    @Operation(summary = "Sign in", description = "Verifies username and password and returns an HS256 JWT valid for one hour, plus the user profile.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "JWT and user profile"), @ApiResponse(responseCode = "401", description = "Invalid credentials")})
    @PostMapping("/api/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) { return accounts.login(request); }

    @Hidden
    @PostMapping("/internal/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfile create(@Valid @RequestBody AccountRequest request) { return accounts.create(request); }

    @Hidden
    @PutMapping("/internal/users/{id}")
    public UserProfile update(@Positive @PathVariable Long id, @Valid @RequestBody AccountRequest request) { return accounts.update(id, request); }

    @Hidden
    @DeleteMapping("/internal/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Positive @PathVariable Long id) { accounts.delete(id); }

    @Hidden
    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }
}
