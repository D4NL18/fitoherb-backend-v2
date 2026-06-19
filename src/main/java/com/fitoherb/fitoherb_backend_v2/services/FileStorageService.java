package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.exceptions.FileStorageException;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.coobird.thumbnailator.Thumbnails;

@Service
public class FileStorageService {

    @Value("${gcp.bucket.name:fitoherb-images-bucket}")
    private String bucketName;

    // Usaremos as variaveis antigas apenas como nomes de "pastas" dentro do bucket
    private final String supplierFolder = "suppliers";
    private final String categoryFolder = "categories";
    private final String productFolder = "products";
    private final String bannerFolder = "banners";

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    
    private Storage storage;

    @PostConstruct
    private void init() {
        // Inicializa o client do GCS (ele pega as credenciais automaticamente no Cloud Run)
        this.storage = StorageOptions.getDefaultInstance().getService();
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
            String blobName = folderName + "/" + fileName;

            byte[] fileBytes;

            // Comprimir a imagem em memoria antes de enviar pro Google Cloud
            if (isCompressibleImage(extension)) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                Thumbnails.of(file.getInputStream())
                        .scale(1.0)
                        .outputQuality(0.85)
                        .toOutputStream(os);
                fileBytes = os.toByteArray();
            } else {
                fileBytes = file.getBytes();
            }

            // Enviar pro GCS
            BlobId blobId = BlobId.of(bucketName, blobName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).build();
            storage.create(blobInfo, fileBytes);

            return fileName;
        } catch (Exception e) {
            log.error("Failed to upload to GCS", e);
            throw new FileStorageException("Could not store the file in Google Cloud Storage. Error: " + e.getMessage());
        }
    }

    private void deleteFile(String fileName, String folderName) {
        if (fileName == null || fileName.isEmpty()) return;

        try {
            String blobName = folderName + "/" + fileName;
            BlobId blobId = BlobId.of(bucketName, blobName);
            boolean deleted = storage.delete(blobId);
            
            if (deleted) {
                log.info("File deleted successfully from GCS: {}", blobName);
            } else {
                log.warn("Tried to delete inexisting file from GCS: {}", blobName);
            }
        } catch (Exception e) {
            log.error("Error deleting file from GCS: {} - Error: {}", fileName, e.getMessage());
        }
    }

    public String storeSupplierImage(MultipartFile file) {
        return storeFile(file, supplierFolder);
    }

    public void deleteSupplierImage(String fileName) {
        deleteFile(fileName, supplierFolder);
    }

    public String storeCategoryImage(MultipartFile file) {
        return storeFile(file, categoryFolder);
    }

    public void deleteCategoryImage(String fileName) {
        deleteFile(fileName, categoryFolder);
    }

    public String storeProductImage(MultipartFile file) {
        return storeFile(file, productFolder);
    }

    public void deleteProductImage(String fileName) {
        deleteFile(fileName, productFolder);
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
        return storeFile(file, bannerFolder);
    }

    public void deleteBannerImage(String fileName) {
        deleteFile(fileName, bannerFolder);
    }
}