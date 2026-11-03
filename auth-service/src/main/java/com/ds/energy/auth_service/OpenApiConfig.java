package com.ds.energy.auth_service;

import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Authentication Service", version = "1.0"), servers = @Server(url = "/"))
public class OpenApiConfig {
}
