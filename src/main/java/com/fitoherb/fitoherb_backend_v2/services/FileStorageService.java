package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.exceptions.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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

    @Value("${path.categoryImages}")
    private String categoryPath;

    @Value("${path.productImages}")
    private String productPath;

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private String storeFile(MultipartFile file, String baseDirPath) {
        if (file.isEmpty()) throw new FileStorageException("Failed to store empty file.");

        try {
            Path directory = Paths.get(baseDirPath).toAbsolutePath().normalize();
            if (!Files.exists(directory)) Files.createDirectories(directory);

            String rawFilename = file.getOriginalFilename();
            String extension = "";

            if (rawFilename != null && !rawFilename.isBlank()) {
                String originalFilename = StringUtils.cleanPath(rawFilename);
                int lastIndex = originalFilename.lastIndexOf('.');
                if (lastIndex >= 0) {
                    extension = originalFilename.substring(lastIndex);
                }
            }

            String fileName = UUID.randomUUID().toString() + extension;
            Path targetLocation = directory.resolve(fileName).normalize();

            if (!targetLocation.startsWith(directory)) {
                throw new SecurityException("Invalid path attempted for file storage.");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            throw new FileStorageException("Could not store file. Error: " + e.getMessage());
        }
    }

    private void deleteFile(String fileName, String baseDirPath) {
        if (fileName == null || fileName.isEmpty()) return;

        try {
            Path directory = Paths.get(baseDirPath).toAbsolutePath().normalize();

            String cleanFileName = Paths.get(fileName).getFileName().toString();
            Path filePath = directory.resolve(cleanFileName).normalize();

            if (!filePath.startsWith(directory)) {
                log.error("Security violation: Attempted path traversal for file deletion: {}", fileName);
                return;
            }

            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("File deleted successfully: {}", cleanFileName);
            } else {
                log.warn("Tried to delete inexisting file: {}", cleanFileName);
            }
        } catch (IOException e) {
            log.error("Error deleting file: {} - Error: {}", fileName, e.getMessage());
        }
    }

    public String storeSupplierImage(MultipartFile file) {
        return storeFile(file, supplierPath);
    }

    public void deleteSupplierImage(String fileName) {
        deleteFile(fileName, supplierPath);
    }

    public String storeCategoryImage(MultipartFile file) {
        return storeFile(file, categoryPath);
    }

    public void deleteCategoryImage(String fileName) {
        deleteFile(fileName, categoryPath);
    }

    public String storeProductImage(MultipartFile file) {
        return storeFile(file, productPath);
    }

    public void deleteProductImage(String fileName) {
        deleteFile(fileName, productPath);
    }
}