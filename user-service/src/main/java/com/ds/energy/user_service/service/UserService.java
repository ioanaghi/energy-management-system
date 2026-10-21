package com.ds.energy.user_service.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ds.energy.user_service.dto.UserCreateDTO;
import com.ds.energy.user_service.dto.UserDTO;
import com.ds.energy.user_service.entity.User;
import com.ds.energy.user_service.repository.UserRepository;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        return toDTO(findUser(id));
    }

    public UserDTO createUser(UserCreateDTO request) {
        validateRequiredFields(request, true);
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use");
        }

        User user = new User();
        applyRequest(user, request, true);
        return toDTO(userRepository.save(user));
    }

    public UserDTO updateUser(Long id, UserCreateDTO request) {
        validateRequiredFields(request, false);
        User user = findUser(id);
        String username = request.getUsername().trim();
        if (userRepository.existsByUsernameAndIdNot(username, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use");
        }

        applyRequest(user, request, false);
        return toDTO(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        userRepository.delete(findUser(id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void validateRequiredFields(UserCreateDTO request, boolean requirePassword) {
        if (request == null
                || isBlank(request.getUsername())
                || request.getRole() == null
                || (requirePassword && isBlank(request.getPassword()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Username and role are required; password is required when creating a user");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void applyRequest(User user, UserCreateDTO request, boolean creating) {
        user.setUsername(request.getUsername().trim());
        user.setRole(request.getRole());
        user.setName(request.getName());
        user.setAddress(request.getAddress());
        if (creating || !isBlank(request.getPassword())) {
            user.setPassword(request.getPassword());
        }
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setName(user.getName());
        dto.setAddress(user.getAddress());
        return dto;
    }
}
