package com.ds.energy.gateway;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@RestController
public class GatewayController {
    private final ServiceClient client;
    private final ObjectMapper mapper;
    private final String users;
    private final String devices;
    private final String auth;

    public GatewayController(ServiceClient client, ObjectMapper mapper,
            @Value("${app.user-service-url}") String users,
            @Value("${app.device-service-url}") String devices,
            @Value("${app.auth-service-url}") String auth) {
        this.client = client;
        this.mapper = mapper;
        this.users = users;
        this.devices = devices;
        this.auth = auth;
    }

    public record Profile(Long id, String username, String role, String name, String address) {}
    public record Device(Long id, String name, Double maxConsumption, String address, Long userId) {}

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }

    @RequestMapping(value = "/api/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<byte[]> route(HttpServletRequest request, @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) Map<String, Object> body) {
        String path = request.getRequestURI().substring("/api/".length());
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        if ((path.equals("auth/login") || path.equals("auth/register")) && method == HttpMethod.POST) {
            requireBody(body);
            return forward(auth, method, "/api/" + path, body);
        }
        Long userId;
        try {
            userId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException error) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user ID in token");
        }
        ResponseEntity<byte[]> profileResponse;
        try {
            profileResponse = forward(users, HttpMethod.GET, "/api/users/" + userId, null);
        } catch (UpstreamException error) {
            if (error.getStatus().value() == 404) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account no longer exists");
            throw error;
        }
        Profile profile = mapper.readValue(profileResponse.getBody(), Profile.class);
        if (path.equals("auth/me") && method == HttpMethod.GET) return profileResponse;
        if (path.equals("auth/me") && method == HttpMethod.PUT) {
            requireBody(body);
            if (!Set.of("name", "address").containsAll(body.keySet()) || !body.containsKey("name") || !body.containsKey("address")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only full name and address can be updated");
            }
            validateProfileField(body.get("name"), 150, "Full name");
            validateProfileField(body.get("address"), 250, "Address");
            Map<String, Object> update = new HashMap<>();
            update.put("username", profile.username());
            update.put("role", profile.role());
            update.put("name", body.get("name"));
            update.put("address", body.get("address"));
            return forward(users, HttpMethod.PUT, "/api/users/" + userId, update);
        }
        if (!"ADMIN".equals(profile.role())) {
            if (method != HttpMethod.GET) forbidden();
            if (path.equals("devices") || path.equals("devices/by-user/" + userId)) {
                return forward(devices, HttpMethod.GET, "/api/devices/by-user/" + userId, null);
            }
            if (path.matches("devices/[1-9]\\d*")) {
                var response = forward(devices, HttpMethod.GET, "/api/" + path, null);
                Device device = mapper.readValue(response.getBody(), Device.class);
                if (!userId.equals(device.userId())) forbidden();
                return response;
            }
            forbidden();
        }
        if (path.matches("users(?:/[1-9]\\d*)?")) {
            if (method == HttpMethod.GET) return forward(users, method, "/api/" + path, null);
            Long targetId = path.contains("/") ? Long.valueOf(path.substring(path.indexOf('/') + 1)) : null;
            if ((method == HttpMethod.POST) != (targetId == null)) {
                throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
            }
            if (method != HttpMethod.DELETE) requireBody(body);
            if (userId.equals(targetId) && (method == HttpMethod.DELETE || !"ADMIN".equals(body.get("role")))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot delete or demote your current administrator account");
            }
            if (method == HttpMethod.DELETE) {
                forward(users, HttpMethod.GET, "/api/" + path, null);
                var response = forward(devices, HttpMethod.GET, "/api/devices/by-user/" + targetId, null);
                for (Device device : mapper.readValue(response.getBody(), Device[].class)) {
                    forward(devices, HttpMethod.DELETE, "/api/devices/" + device.id() + "/assignment", null);
                }
            }
            return client.forward(auth, method, "/internal/" + path, body, true);
        }
        if (path.matches("devices(?:/[1-9]\\d*(?:/assignment(?:/[1-9]\\d*)?)?|/by-user/[1-9]\\d*)?")) {
            if ((method == HttpMethod.POST || method == HttpMethod.PUT) && !path.contains("/assignment/")) requireBody(body);
            Object assignedId = body == null ? null : body.get("userId");
            if (path.contains("/assignment/")) assignedId = path.substring(path.lastIndexOf('/') + 1);
            if (assignedId != null) {
                String value = assignedId.toString();
                if (!value.matches("[1-9]\\d*") || assignedId instanceof Boolean) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID must be a positive integer");
                }
                forward(users, HttpMethod.GET, "/api/users/" + value, null);
            }
            return forward(devices, method, "/api/" + path, body);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Endpoint not found");
    }

    private ResponseEntity<byte[]> forward(String base, HttpMethod method, String path, Map<String, Object> body) {
        return client.forward(base, method, path, body, false);
    }

    private void validateProfileField(Object value, int maxLength, String field) {
        if (value != null && (!(value instanceof String text) || text.length() > maxLength)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be text with at most " + maxLength + " characters");
        }
    }

    private void requireBody(Map<String, Object> body) {
        if (body == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A JSON object is required");
    }

    private void forbidden() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
    }
}
