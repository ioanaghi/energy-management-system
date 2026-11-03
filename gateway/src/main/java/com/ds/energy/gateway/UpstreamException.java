package com.ds.energy.gateway;

import org.springframework.http.HttpStatusCode;

public class UpstreamException extends RuntimeException {
    private final HttpStatusCode status;
    private final byte[] body;

    public UpstreamException(HttpStatusCode status, byte[] body) {
        this.status = status;
        this.body = body;
    }

    public HttpStatusCode getStatus() { return status; }
    public byte[] getBody() { return body; }
}
