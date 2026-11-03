package com.ds.energy.user_service.controller;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.energy.user_service.dto.UserCreateDTO;
import com.ds.energy.user_service.dto.UserDTO;
import com.ds.energy.user_service.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "List user profiles", description = "Administrator only. Account credentials are managed by the authentication service through the gateway.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operation successful"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "401", description = "Authentication required"), @ApiResponse(responseCode = "403", description = "Administrator access required"), @ApiResponse(responseCode = "404", description = "User not found"), @ApiResponse(responseCode = "409", description = "Username already exists")})
    @GetMapping
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @Operation(summary = "Get a user profile", description = "Administrator only. Account credentials are managed by the authentication service through the gateway.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operation successful"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "401", description = "Authentication required"), @ApiResponse(responseCode = "403", description = "Administrator access required"), @ApiResponse(responseCode = "404", description = "User not found"), @ApiResponse(responseCode = "409", description = "Username already exists")})
    @GetMapping("/{id}")
    public UserDTO getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @Operation(summary = "Create a user profile", description = "Administrator only. Account credentials are managed by the authentication service through the gateway.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Operation successful"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "401", description = "Authentication required"), @ApiResponse(responseCode = "403", description = "Administrator access required"), @ApiResponse(responseCode = "404", description = "User not found"), @ApiResponse(responseCode = "409", description = "Username already exists")})
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody UserCreateDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @Operation(summary = "Update a user profile", description = "Administrator only. Account credentials are managed by the authentication service through the gateway.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operation successful"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "401", description = "Authentication required"), @ApiResponse(responseCode = "403", description = "Administrator access required"), @ApiResponse(responseCode = "404", description = "User not found"), @ApiResponse(responseCode = "409", description = "Username already exists")})
    @PutMapping("/{id}")
    public UserDTO updateUser(@PathVariable Long id, @RequestBody UserCreateDTO request) {
        return userService.updateUser(id, request);
    }

    @Operation(summary = "Delete a user profile", description = "Administrator only. Account credentials are managed by the authentication service through the gateway.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Operation successful"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "401", description = "Authentication required"), @ApiResponse(responseCode = "403", description = "Administrator access required"), @ApiResponse(responseCode = "404", description = "User not found"), @ApiResponse(responseCode = "409", description = "Username already exists")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
