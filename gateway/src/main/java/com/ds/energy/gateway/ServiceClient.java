package com.ds.energy.gateway;

import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ServiceClient {
    private final RestClient client;
    private final String internalKey;

    public ServiceClient(@Value("${app.internal-key}") String internalKey) {
        this.internalKey = internalKey;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        client = RestClient.builder().requestFactory(factory).build();
    }

    public ResponseEntity<byte[]> forward(String base, HttpMethod method, String path, Map<String, Object> body, boolean internal) {
        try {
            var request = client.method(method).uri(base + path);
            if (internal) request.header("X-Internal-Key", internalKey);
            if (body != null) request.contentType(MediaType.APPLICATION_JSON).body(body);
            var response = request.retrieve().toEntity(byte[].class);
            return ResponseEntity.status(response.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(response.getBody());
        } catch (RestClientResponseException error) {
            throw new UpstreamException(error.getStatusCode(), error.getResponseBodyAsByteArray());
        } catch (ResourceAccessException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "A required service is unavailable");
        }
    }
}
