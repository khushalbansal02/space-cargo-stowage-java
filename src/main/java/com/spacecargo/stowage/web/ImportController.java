package com.spacecargo.stowage.web;

import com.spacecargo.stowage.service.ImportService;
import com.spacecargo.stowage.service.ImportService.ImportResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

@RestController
@RequestMapping("/api/v1/import")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/containers")
    public ImportResult importContainers(@RequestParam("file") MultipartFile file) {
        return importService.importContainers(open(file));
    }

    @PostMapping("/items")
    public ImportResult importItems(@RequestParam("file") MultipartFile file) {
        return importService.importItems(open(file));
    }

    private java.io.InputStream open(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is missing or empty.");
        }
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
