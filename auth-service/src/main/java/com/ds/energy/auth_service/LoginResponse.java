package com.ds.energy.auth_service;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @JsonProperty("access_token") @Schema(description = "Signed JWT, valid for one hour") String accessToken,
        @JsonProperty("token_type") @Schema(example = "bearer") String tokenType,
        UserProfile user) {
}
