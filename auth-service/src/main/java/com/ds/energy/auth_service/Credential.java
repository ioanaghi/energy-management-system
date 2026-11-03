package com.ds.energy.auth_service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "credentials")
public class Credential {
    @Id
    private Long userId;
    @Column(nullable = false, unique = true, length = 100)
    private String username;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    protected Credential() {
    }

    public Credential(Long userId, String username, String passwordHash) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
