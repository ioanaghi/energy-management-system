package com.ds.energy.auth_service;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    Optional<Credential> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByUsernameAndUserIdNot(String username, Long userId);
}
