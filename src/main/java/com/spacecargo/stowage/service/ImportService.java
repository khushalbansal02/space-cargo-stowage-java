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
                        required(record, "containerId", "container_id"),
                        required(record, "zone"),
                        parseDouble(record, "width", "width_cm"),
                        parseDouble(record, "depth", "depth_cm"),
                        parseDouble(record, "height", "height_cm"));
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
        Integer usageLimit = parseOptionalInt(record, "usageLimit", "usage_limit");
        Item item = new Item();
        item.setItemId(required(record, "itemId", "item_id"));
        item.setName(required(record, "name"));
        item.setDimensionW(parseDouble(record, "width", "width_cm"));
        item.setDimensionD(parseDouble(record, "depth", "depth_cm"));
        item.setDimensionH(parseDouble(record, "height", "height_cm"));
        item.setMass(parseOptionalDouble(record, "mass", "mass_kg"));
        item.setPriority((int) parseDouble(record, "priority"));
        item.setExpiryDate(parseOptionalDate(record, "expiryDate", "expiry_date"));
        item.setUsageLimit(usageLimit);
        item.setRemainingUses(usageLimit);
        item.setPreferredZone(value(record, "preferredZone", "preferred_zone"));
        item.setPreferredContainerId(value(record, "preferredContainerId", "preferred_container_id"));
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

    /** First non-blank value among the given column aliases, or null. Treats "N/A" as blank. */
    private String value(Map<String, String> record, String... keys) {
        for (String key : keys) {
            String raw = record.get(key);
            if (raw != null) {
                String trimmed = raw.trim();
                if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase("N/A") && !trimmed.equalsIgnoreCase("NA")) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    private String required(Map<String, String> record, String... keys) {
        String v = value(record, keys);
        if (v == null) {
            throw new IllegalArgumentException("Missing required column '" + keys[0] + "'.");
        }
        return v;
    }

    private double parseDouble(Map<String, String> record, String... keys) {
        try {
            return Double.parseDouble(required(record, keys));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Column '" + keys[0] + "' must be a number.");
        }
    }

    private Double parseOptionalDouble(Map<String, String> record, String... keys) {
        String v = value(record, keys);
        return v == null ? null : Double.parseDouble(v);
    }

    private Integer parseOptionalInt(Map<String, String> record, String... keys) {
        String v = value(record, keys);
        return v == null ? null : Integer.parseInt(v);
    }

    private LocalDate parseOptionalDate(Map<String, String> record, String... keys) {
        String v = value(record, keys);
        return v == null ? null : LocalDate.parse(v);
    }
}
