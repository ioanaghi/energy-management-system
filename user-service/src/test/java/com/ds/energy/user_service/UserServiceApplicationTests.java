package com.ds.energy.user_service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ds.energy.user_service.dto.UserCreateDTO;
import com.ds.energy.user_service.entity.Role;
import com.ds.energy.user_service.service.UserService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceApplicationTests {
    @Autowired
    private UserService service;

    private UserCreateDTO request(String username) {
        UserCreateDTO data = new UserCreateDTO();
        data.setUsername(username);
        data.setRole(Role.CLIENT);
        data.setName("Test Client");
        return data;
    }

    @Test
    void createsUpdatesAndDeletesProfiles() {
        var created = service.createUser(request("client"));
        assertEquals("client", service.getUserById(created.getId()).getUsername());
        var updated = service.updateUser(created.getId(), request("renamed"));
        assertEquals("renamed", updated.getUsername());
        assertEquals(1, service.getAllUsers().size());
        service.deleteUser(created.getId());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.getUserById(created.getId())).getStatusCode().value());
    }

    @Test
    void rejectsDuplicateUsernameOnCreateAndUpdate() {
        service.createUser(request("client"));
        var other = service.createUser(request("other"));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.createUser(request("client"))).getStatusCode().value());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.updateUser(other.getId(), request("client"))).getStatusCode().value());
    }

    @Test
    void rejectsMissingUsernameOrRole() {
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.createUser(request("  "))).getStatusCode().value());
        var missingRole = request("client");
        missingRole.setRole(null);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.createUser(missingRole)).getStatusCode().value());
    }
}
