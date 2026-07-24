package com.spacecargo.stowage.service;

import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.ItemStatus;
import com.spacecargo.stowage.domain.geometry.BoundingBox;
import com.spacecargo.stowage.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exports the current stowage arrangement as CSV: one row per stowed item with its
 * container and occupied box.
 */
@Service
public class ExportService {

    private static final String HEADER =
            "itemId,name,containerId,x,y,z,width,depth,height\n";

    private final ItemRepository items;

    public ExportService(ItemRepository items) {
        this.items = items;
    }

    @Transactional(readOnly = true)
    public String exportArrangementCsv() {
        StringBuilder csv = new StringBuilder(HEADER);
        for (Item item : items.findByStatus(ItemStatus.STOWED)) {
            BoundingBox b = item.stowedBox();
            csv.append(item.getItemId()).append(',')
                    .append(escape(item.getName())).append(',')
                    .append(item.getContainerId()).append(',')
                    .append(b.x()).append(',').append(b.y()).append(',').append(b.z()).append(',')
                    .append(b.w()).append(',').append(b.d()).append(',').append(b.h())
                    .append('\n');
        }
        return csv.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
