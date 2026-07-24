package com.spacecargo.stowage.service;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import com.spacecargo.stowage.domain.Container;
import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.ItemStatus;
import com.spacecargo.stowage.repository.ContainerRepository;
import com.spacecargo.stowage.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bulk-loads containers and items from CSV. Each row is validated independently:
 * a bad row is reported in {@link ImportResult#errors()} and skipped, so one
 * malformed line never aborts the whole file.
 *
 * <p>Container columns: {@code containerId, zone, width, depth, height}.<br>
 * Item columns: {@code itemId, name, width, depth, height, mass, priority,
 * expiryDate, usageLimit, preferredZone, preferredContainerId}.
 */
@Service
public class ImportService {

    private final ContainerRepository containers;
    private final ItemRepository items;

    public ImportService(ContainerRepository containers, ItemRepository items) {
        this.containers = containers;
        this.items = items;
    }

    public record ImportResult(int imported, List<String> errors) {
    }

    @Transactional
    public ImportResult importContainers(Reader source) {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int row = 1;
        for (Map<String, String> record : readAll(source)) {
            row++;
            try {
                Container c = new Container(
                        required(record, "containerId"),
                        required(record, "zone"),
                        parseDouble(record, "width"),
                        parseDouble(record, "depth"),
                        parseDouble(record, "height"));
                containers.save(c);
                imported++;
            } catch (RuntimeException e) {
                errors.add("Row " + row + ": " + e.getMessage());
            }
        }
        return new ImportResult(imported, errors);
    }

    @Transactional
    public ImportResult importItems(Reader source) {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int row = 1;
        for (Map<String, String> record : readAll(source)) {
            row++;
            try {
                items.save(toItem(record));
                imported++;
            } catch (RuntimeException e) {
                errors.add("Row " + row + ": " + e.getMessage());
            }
        }
        return new ImportResult(imported, errors);
    }

    public ImportResult importContainers(java.io.InputStream in) {
        return importContainers(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    public ImportResult importItems(java.io.InputStream in) {
        return importItems(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private Item toItem(Map<String, String> record) {
        Integer usageLimit = parseOptionalInt(record, "usageLimit");
        Item item = new Item();
        item.setItemId(required(record, "itemId"));
        item.setName(required(record, "name"));
        item.setDimensionW(parseDouble(record, "width"));
        item.setDimensionD(parseDouble(record, "depth"));
        item.setDimensionH(parseDouble(record, "height"));
        item.setMass(parseOptionalDouble(record, "mass"));
        item.setPriority((int) parseDouble(record, "priority"));
        item.setExpiryDate(parseOptionalDate(record, "expiryDate"));
        item.setUsageLimit(usageLimit);
        item.setRemainingUses(usageLimit);
        item.setPreferredZone(blankToNull(record.get("preferredZone")));
        item.setPreferredContainerId(blankToNull(record.get("preferredContainerId")));
        item.setStatus(ItemStatus.AVAILABLE);
        return item;
    }

    private List<Map<String, String>> readAll(Reader source) {
        List<Map<String, String>> rows = new ArrayList<>();
        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(source)) {
            Map<String, String> record;
            while ((record = reader.readMap()) != null) {
                rows.add(record);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (CsvValidationException e) {
            throw new IllegalArgumentException("Malformed CSV: " + e.getMessage(), e);
        }
        return rows;
    }

    private String required(Map<String, String> record, String key) {
        String value = record.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required column '" + key + "'.");
        }
        return value.trim();
    }

    private double parseDouble(Map<String, String> record, String key) {
        try {
            return Double.parseDouble(required(record, key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Column '" + key + "' must be a number.");
        }
    }

    private Double parseOptionalDouble(Map<String, String> record, String key) {
        String value = blankToNull(record.get(key));
        return value == null ? null : Double.parseDouble(value);
    }

    private Integer parseOptionalInt(Map<String, String> record, String key) {
        String value = blankToNull(record.get(key));
        return value == null ? null : Integer.parseInt(value);
    }

    private LocalDate parseOptionalDate(Map<String, String> record, String key) {
        String value = blankToNull(record.get(key));
        return value == null ? null : LocalDate.parse(value);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
