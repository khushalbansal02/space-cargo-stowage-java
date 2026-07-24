package com.spacecargo.stowage;

import com.spacecargo.stowage.domain.ItemStatus;
import com.spacecargo.stowage.placement.PlacementOutcome;
import com.spacecargo.stowage.repository.ContainerRepository;
import com.spacecargo.stowage.repository.ItemRepository;
import com.spacecargo.stowage.service.ImportService;
import com.spacecargo.stowage.service.PlacementService;
import com.spacecargo.stowage.support.EnabledIfDockerAvailable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the application works against a real PostgreSQL instance (not just the
 * H2 stand-in) by spinning one up with Testcontainers. This is the closest a test
 * gets to production: real Flyway migrations, real SQL dialect, real driver.
 *
 * <p>Requires a running Docker daemon; skipped implicitly where Docker is absent.
 */
@SpringBootTest
@Testcontainers
@EnabledIfDockerAvailable
class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired ImportService importService;
    @Autowired PlacementService placementService;
    @Autowired ContainerRepository containers;
    @Autowired ItemRepository items;

    @Test
    void importsAndPlacesAgainstRealPostgres() {
        importService.importContainers(new StringReader("""
                containerId,zone,width,depth,height
                C1,Lab,10,10,10
                """));
        importService.importItems(new StringReader("""
                itemId,name,width,depth,height,mass,priority,expiryDate,usageLimit,preferredZone,preferredContainerId
                I1,Sample,2,2,2,1.0,80,,,Lab,
                """));

        List<PlacementOutcome> outcomes = placementService.placeItems(List.of("I1"), "it");

        assertThat(outcomes).singleElement().satisfies(o -> {
            assertThat(o.placed()).isTrue();
            assertThat(o.containerId()).isEqualTo("C1");
        });
        assertThat(items.findByStatus(ItemStatus.STOWED)).hasSize(1);
        assertThat(containers.findById("C1")).isPresent();
    }
}
