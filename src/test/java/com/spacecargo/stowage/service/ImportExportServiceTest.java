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
