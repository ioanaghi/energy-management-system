package com.ds.energy.auth_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.bootstrap-admin", havingValue = "true", matchIfMissing = true)
public class AdminInitializer implements CommandLineRunner {
    private final AccountService accounts;
    private final CredentialRepository credentials;
    private final String username;
    private final String password;

    public AdminInitializer(AccountService accounts, CredentialRepository credentials,
            @Value("${app.admin-username}") String username, @Value("${app.admin-password}") String password) {
        this.accounts = accounts;
        this.credentials = credentials;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (!credentials.existsByUsername(username)) {
            accounts.create(new AccountRequest(username, password, "ADMIN", "Administrator", ""));
        }
    }
}
