package com.spacecargo.stowage.service;

import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.geometry.BoundingBox;
import com.spacecargo.stowage.repository.ContainerRepository;
import com.spacecargo.stowage.repository.ItemRepository;
import com.spacecargo.stowage.service.ImportService.ImportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ImportExportServiceTest {

    @Autowired ImportService importService;
    @Autowired ExportService exportService;
    @Autowired ContainerRepository containers;
    @Autowired ItemRepository items;

    @BeforeEach
    void clean() {
        items.deleteAll();
        containers.deleteAll();
    }

    @Test
    void importsValidContainerRows() {
        String csv = """
                containerId,zone,width,depth,height
                C1,Lab,10,10,10
                C2,Airlock,5,5,5
                """;

        ImportResult result = importService.importContainers(new StringReader(csv));

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.errors()).isEmpty();
        assertThat(containers.count()).isEqualTo(2);
    }

    @Test
    void reportsBadRowsWithoutAbortingTheImport() {
        String csv = """
                itemId,name,width,depth,height,mass,priority,expiryDate,usageLimit,preferredZone,preferredContainerId
                I1,Good,2,2,2,1.5,80,,,Lab,
                I2,BadWidth,notanumber,2,2,1,50,,,Lab,
                I3,AlsoGood,3,3,3,2,40,,10,Storage,
                """;

        ImportResult result = importService.importItems(new StringReader(csv));

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0)).contains("Row 3").contains("width");
    }

    @Test
    void importsTheOfficialHackathonColumnFormat() {
        // Containers: zone-first order with _cm suffixes.
        String containerCsv = """
                zone,container_id,width_cm,depth_cm,height_cm
                Crew_Quarters,CQ01,100,85,200
                """;
        ImportResult cResult = importService.importContainers(new StringReader(containerCsv));
        assertThat(cResult.imported()).isEqualTo(1);
        assertThat(containers.findById("CQ01")).isPresent();

        // Items: item_id/width_cm/mass_kg/expiry_date=N/A/usage_limit/preferred_zone.
        String itemCsv = """
                item_id,name,width_cm,depth_cm,height_cm,mass_kg,priority,expiry_date,usage_limit,preferred_zone
                000001,Research_Samples,26.8,17.5,19.4,2.4,84,N/A,2304,Storage_Bay
                000010,Food_Packet,15.7,18.6,29.4,9.3,41,2025-10-23,11,Storage_Bay
                """;
        ImportResult iResult = importService.importItems(new StringReader(itemCsv));

        assertThat(iResult.imported()).isEqualTo(2);
        assertThat(iResult.errors()).isEmpty();

        Item withoutExpiry = items.findById("000001").orElseThrow();
        assertThat(withoutExpiry.getExpiryDate()).isNull();          // "N/A" → null
        assertThat(withoutExpiry.getDimensionW()).isEqualTo(26.8);   // width_cm
        assertThat(withoutExpiry.getMass()).isEqualTo(2.4);          // mass_kg
        assertThat(withoutExpiry.getPreferredZone()).isEqualTo("Storage_Bay");

        Item withExpiry = items.findById("000010").orElseThrow();
        assertThat(withExpiry.getExpiryDate()).isEqualTo(java.time.LocalDate.of(2025, 10, 23));
    }

    @Test
    void exportsStowedItemsAsCsv() {
        containers.save(new com.spacecargo.stowage.domain.Container("C1", "Lab", 10, 10, 10));
        Item item = new Item();
        item.setItemId("I1");
        item.setName("Sample");
        item.setDimensionW(2);
        item.setDimensionD(2);
        item.setDimensionH(2);
        item.setPriority(50);
        item.stow("C1", new BoundingBox(1, 2, 3, 2, 2, 2));
        items.save(item);

        String csv = exportService.exportArrangementCsv();

        assertThat(csv).startsWith("itemId,name,containerId,x,y,z,width,depth,height");
        assertThat(csv).contains("I1,Sample,C1,1.0,2.0,3.0,2.0,2.0,2.0");
    }
}
