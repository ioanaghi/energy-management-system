package com.ds.energy.auth_service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record AccountRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "\\S+") String username,
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, description = "Required on creation, optional on update. Between 8 and 128 characters.") String password,
        @Pattern(regexp = "ADMIN|CLIENT") String role,
        @Size(max = 150) String name,
        @Size(max = 250) String address) {
}
