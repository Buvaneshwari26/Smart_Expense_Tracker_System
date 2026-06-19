package com.tracker.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Smart Expense Tracker API",
        version = "1.0.0",
        description = "Complete REST API for Smart Expense Tracker System — manages income, expenses, budgets, savings goals, reports, and notifications.",
        contact = @Contact(name = "Smart Expense Tracker", email = "admin@expensetracker.com"),
        license = @License(name = "MIT License")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local Development Server"),
        @Server(url = "https://api.expensetracker.com", description = "Production Server")
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "JWT Bearer token. Login to get token, then use: Bearer <token>"
)
public class SwaggerConfig {
    // Configured via annotations above
}
