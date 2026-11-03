package com.ds.energy.gateway;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(UpstreamException.class)
    public ResponseEntity<byte[]> upstream(UpstreamException error) {
        return ResponseEntity.status(error.getStatus()).contentType(MediaType.APPLICATION_JSON).body(error.getBody());
    }
}
