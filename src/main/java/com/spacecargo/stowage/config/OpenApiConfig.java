package com.spacecargo.stowage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stowageOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Space Cargo Stowage API")
                .version("1.0.0")
                .description("""
                        Backend for managing cargo aboard a space station: 3D bin-packing
                        placement, obstruction-aware retrieval planning, waste identification
                        and return, time simulation, and CSV import/export.

                        Interactive docs: /swagger-ui.html""")
                .license(new License().name("MIT")));
    }
}
