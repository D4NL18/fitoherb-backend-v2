package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.exceptions.FileStorageException;
import jakarta.annotation.PostConstruct;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Profile("!prod")
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    @Value("${local.storage.path}")
    private String storagePath;

    private final String supplierFolder = "suppliers";
    private final String categoryFolder = "categories";
    private final String productFolder = "products";
    private final String bannerFolder = "banners";

    @PostConstruct
    private void init() {
        try {
            Files.createDirectories(Paths.get(storagePath, supplierFolder));
            Files.createDirectories(Paths.get(storagePath, categoryFolder));
            Files.createDirectories(Paths.get(storagePath, productFolder));
            Files.createDirectories(Paths.get(storagePath, bannerFolder));
        } catch (IOException e) {
            log.error("Could not create local storage directories", e);
            throw new FileStorageException("Could not create local storage directories: " + e.getMessage());
        }
    }

    private boolean isCompressibleImage(String extension) {
        if (extension == null || extension.isBlank()) return false;
        String ext = extension.toLowerCase();
        return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png");
    }

    private String storeFile(MultipartFile file, String folderName) {
        if (file.isEmpty()) throw new FileStorageException("Cannot store an empty file.");

        try {
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
            Path targetLocation = Paths.get(storagePath, folderName).resolve(fileName).normalize();
            
            // Security check for path traversal
            if (!targetLocation.startsWith(Paths.get(storagePath, folderName).normalize())) {
                throw new FileStorageException("Cannot store file outside current directory.");
            }

            if (isCompressibleImage(extension)) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                Thumbnails.of(file.getInputStream())
                        .scale(1.0)
                        .outputQuality(0.85)
                        .toOutputStream(os);
                Files.write(targetLocation, os.toByteArray());
            } else {
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            return fileName;
        } catch (Exception e) {
            log.error("Failed to store file locally", e);
            throw new FileStorageException("Could not store the file locally. Error: " + e.getMessage());
        }
    }

    private void deleteFile(String fileName, String folderName) {
        if (fileName == null || fileName.isEmpty()) return;

        try {
            Path targetLocation = Paths.get(storagePath, folderName).resolve(fileName).normalize();
            
            // Security check for path traversal
            if (!targetLocation.startsWith(Paths.get(storagePath, folderName).normalize())) {
                log.warn("Path traversal attempt on delete: {}", fileName);
                return;
            }
            
            boolean deleted = Files.deleteIfExists(targetLocation);
            if (deleted) {
                log.info("File deleted successfully from local storage: {}", fileName);
            } else {
                log.warn("Tried to delete inexisting file from local storage: {}", fileName);
            }
        } catch (Exception e) {
            log.error("Error deleting file from local storage: {} - Error: {}", fileName, e.getMessage());
        }
    }

    @Override
    public String storeSupplierImage(MultipartFile file) {
        return storeFile(file, supplierFolder);
    }

    @Override
    public void deleteSupplierImage(String fileName) {
        deleteFile(fileName, supplierFolder);
    }

    @Override
    public String storeCategoryImage(MultipartFile file) {
        return storeFile(file, categoryFolder);
    }

    @Override
    public void deleteCategoryImage(String fileName) {
        deleteFile(fileName, categoryFolder);
    }

    @Override
    public String storeProductImage(MultipartFile file) {
        return storeFile(file, productFolder);
    }

    @Override
    public void deleteProductImage(String fileName) {
        deleteFile(fileName, productFolder);
    }

    @Override
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
        return storeFile(file, bannerFolder);
    }

    @Override
    public void deleteBannerImage(String fileName) {
        deleteFile(fileName, bannerFolder);
    }
}
