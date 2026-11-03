package com.ds.energy.auth_service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserClient {
    private final RestClient client;

    public UserClient(@Value("${app.user-service-url}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        client = RestClient.builder().baseUrl(baseUrl + "/api/users").requestFactory(factory).build();
    }

    public UserProfile get(Long id) {
        return execute(() -> client.get().uri("/{id}", id).retrieve().body(UserProfile.class));
    }

    public UserProfile create(AccountRequest request) {
        return execute(() -> client.post().body(profileData(request)).retrieve().body(UserProfile.class));
    }

    public UserProfile update(Long id, AccountRequest request) {
        return execute(() -> client.put().uri("/{id}", id).body(profileData(request)).retrieve().body(UserProfile.class));
    }

    public void delete(Long id) {
        execute(() -> client.delete().uri("/{id}", id).retrieve().toBodilessEntity());
    }

    private Map<String, Object> profileData(AccountRequest request) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", request.username());
        data.put("role", request.role());
        data.put("name", request.name());
        data.put("address", request.address());
        return data;
    }

    private <T> T execute(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (RestClientResponseException error) {
            throw new ResponseStatusException(error.getStatusCode(), "User operation failed");
        } catch (ResourceAccessException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "User service is unavailable");
        }
    }
}
