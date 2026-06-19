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

import net.coobird.thumbnailator.Thumbnails;

@Service
public class FileStorageService {

    @Value("${path.supplierImages}")
    private String supplierPath;

    @Value("${path.categoryImages}")
    private String categoryPath;

    @Value("${path.productImages}")
    private String productPath;

    @Value("${path.bannerImages}")
    private String bannerPath;

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private boolean isCompressibleImage(String extension) {
        if (extension == null || extension.isBlank()) return false;
        String ext = extension.toLowerCase();
        return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png");
    }

    private String storeFile(MultipartFile file, String baseDirPath) {
        if (file.isEmpty()) throw new FileStorageException("Cannot store an empty file.");

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
                throw new SecurityException("Invalid path for file storage.");
            }

            if (isCompressibleImage(extension)) {
                Thumbnails.of(file.getInputStream())
                        .scale(1.0)
                        .outputQuality(0.85)
                        .toFile(targetLocation.toFile());
            } else {
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            return fileName;
        } catch (IOException e) {
            throw new FileStorageException("Could not store the file. Error: " + e.getMessage());
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

    public String storeBannerImage(MultipartFile file) {
        try {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new FileStorageException("The uploaded file is not a valid image.");
            }
            int width = image.getWidth();
            int height = image.getHeight();
            
            if (width < 1000) {
                throw new FileStorageException("The banner image must be at least 1000px wide to prevent distortion on the frontend.");
            }
            if (height >= width) {
                throw new FileStorageException("The banner image must have a horizontal orientation (width must be greater than height).");
            }
        } catch (IOException e) {
            throw new FileStorageException("Error reading banner image dimensions.");
        }
        return storeFile(file, bannerPath);
    }

    public void deleteBannerImage(String fileName) {
        deleteFile(fileName, bannerPath);
    }
}