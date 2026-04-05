package com.fitoherb.fitoherb_backend_v2.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FileStorageService {

    @Value("${path.supplierImages}")
    private String supplierPath;

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    public String storeSupplierImage(MultipartFile file) {
        try {
            Path directory = Paths.get(supplierPath);
            if (!Files.exists(directory)) Files.createDirectories(directory);

            String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path targetLocation = directory.resolve(fileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file. Error: " + e.getMessage());
        }
    }

    public void deleteSupplierImage(String fileName) {
        if (fileName == null || fileName.isEmpty()) return;

        try {
            Path filePath = Paths.get(supplierPath).resolve(fileName);
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                log.info("File deleted successfully: {}", fileName);
            } else {
                log.warn("Tried to delete inexisting file: {}", fileName);
            }
        } catch (IOException e) {
            log.error("Error deleting file: {} - Error: {}", fileName, e.getMessage());
        }
    }
}