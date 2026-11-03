package com.ds.energy.user_service.dto;

import com.ds.energy.user_service.entity.Role;

public class UserCreateDTO {
    @io.swagger.v3.oas.annotations.media.Schema(description = "Password for account creation; omit on update to keep the current password. Minimum 8 characters.", accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY)
    private String password;
    private String username;
    private Role role;
    private String name;
    private String address;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
