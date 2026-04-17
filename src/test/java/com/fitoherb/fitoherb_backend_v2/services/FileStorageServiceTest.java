package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.exceptions.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    private String supplierPath;
    private String categoryPath;
    private String productPath;

    @BeforeEach
    void setup() {
        fileStorageService = new FileStorageService();

        supplierPath = tempDir.resolve("suppliers").toString();
        categoryPath = tempDir.resolve("categories").toString();
        productPath = tempDir.resolve("products").toString();

        ReflectionTestUtils.setField(fileStorageService, "supplierPath", supplierPath);
        ReflectionTestUtils.setField(fileStorageService, "categoryPath", categoryPath);
        ReflectionTestUtils.setField(fileStorageService, "productPath", productPath);
    }

    @Nested
    @DisplayName("Testes de Armazenamento")
    class StorageTests {

        @Test
        void storeSupplierImageSuccess() {
            MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "content".getBytes());

            String fileName = fileStorageService.storeSupplierImage(file);

            assertNotNull(fileName);
            assertTrue(fileName.endsWith(".jpg"));
            assertTrue(Files.exists(Path.of(supplierPath).resolve(fileName)));
        }

        @Test
        void storeCategoryImageSuccess() {
            MockMultipartFile file = new MockMultipartFile("file", "icon.png", "image/png", "content".getBytes());

            String fileName = fileStorageService.storeCategoryImage(file);

            assertNotNull(fileName);
            assertTrue(fileName.endsWith(".png"));
            assertTrue(Files.exists(Path.of(categoryPath).resolve(fileName)));
        }

        @Test
        void storeProductImageSuccess() {
            MockMultipartFile file = new MockMultipartFile("file", "product.webp", "image/webp", "content".getBytes());

            String fileName = fileStorageService.storeProductImage(file);

            assertNotNull(fileName);
            assertTrue(fileName.endsWith(".webp"));
            assertTrue(Files.exists(Path.of(productPath).resolve(fileName)));
        }

        @Test
        void storeFileEmptyError() {
            MockMultipartFile file = new MockMultipartFile("file", "", "text/plain", new byte[0]);
            assertThrows(FileStorageException.class, () -> fileStorageService.storeProductImage(file));
        }
    }

    @Nested
    @DisplayName("Testes de Deleção")
    class DeletionTests {

        @Test
        void deleteSupplierImageSuccess() throws IOException {
            Path filePath = Files.createDirectories(Path.of(supplierPath)).resolve("test-image.jpg");
            Files.write(filePath, "data".getBytes());

            fileStorageService.deleteSupplierImage("test-image.jpg");

            assertFalse(Files.exists(filePath));
        }

        @Test
        void deleteInexistingFileShouldNotThrow() {
            assertDoesNotThrow(() -> fileStorageService.deleteProductImage("non-existent.jpg"));
        }

        @Test
        void deleteNullOrEmptyFilenameShouldNotThrow() {
            assertDoesNotThrow(() -> fileStorageService.deleteProductImage(null));
            assertDoesNotThrow(() -> fileStorageService.deleteProductImage(""));
        }
    }

    @Nested
    @DisplayName("Testes de Segurança e Hacking")
    class SecurityTests {

        @Test
        void pathTraversalAttackOnStorage() {
            MockMultipartFile maliciousFile = new MockMultipartFile(
                    "file",
                    "../secrets.txt",
                    "text/plain",
                    "hacked".getBytes()
            );

            String fileName = fileStorageService.storeProductImage(maliciousFile);

            Path storedPath = Path.of(productPath).resolve(fileName);
            assertTrue(storedPath.normalize().startsWith(Path.of(productPath).normalize()));
            assertFalse(fileName.contains(".."));
        }

        @Test
        void pathTraversalAttackOnDelete() throws IOException {
            Path secretFile = tempDir.resolve("important-config.yml");
            Files.write(secretFile, "secret-data".getBytes());

            fileStorageService.deleteProductImage("../important-config.yml");

            assertTrue(Files.exists(secretFile));
        }

        @Test
        void filenameWithSpecialCharacters() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "foto teste @#$%! óú.png",
                    "image/png",
                    "data".getBytes()
            );

            String fileName = fileStorageService.storeCategoryImage(file);

            assertNotNull(fileName);
            assertTrue(fileName.endsWith(".png"));
            assertFalse(fileName.contains(" "));
            assertFalse(fileName.contains("@"));
        }

        @Test
        void storageWithNullByte() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "malicious\0file.jpg",
                    "image/jpeg",
                    "data".getBytes()
            );

            String fileName = fileStorageService.storeSupplierImage(file);
            assertNotNull(fileName);
            assertFalse(fileName.contains("\0"));
        }
    }
}