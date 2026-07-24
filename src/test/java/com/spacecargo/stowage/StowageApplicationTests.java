package com.spacecargo.stowage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifies the Spring context loads and Flyway migrations apply
 * against the H2 (PostgreSQL-mode) test database.
 */
@SpringBootTest
@ActiveProfiles("test")
class StowageApplicationTests {

    @Test
    void contextLoads() {
    }
}
