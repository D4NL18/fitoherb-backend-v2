package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductRes;
import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.entities.ProductCategory;
import com.fitoherb.fitoherb_backend_v2.entities.Supplier;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.mappers.ProductMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductCategoryRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductCategoryRepository categoryRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductMapper productMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private MultipartFile image;

    @InjectMocks
    private ProductService productService;

    private Product productEntity;
    private ProductRes productRes;
    private ProductCategory category;
    private Supplier supplier;

    @BeforeEach
    void setup() {
        productEntity = new Product();
        productEntity.setSlug("produto-teste");
        productEntity.setImagePath("old-image.jpg");

        productRes = new ProductRes();

        category = new ProductCategory();
        category.setSlug("categoria-teste");

        supplier = new Supplier();
        supplier.setSlug("fornecedor-teste");
    }

    @Nested
    @DisplayName("Testes de Busca e Galeria")
    class RetrievalTests {

        @Test
        void getProductBySlugSuccess() {
            when(productRepository.findBySlug("produto-teste")).thenReturn(Optional.of(productEntity));
            when(productMapper.entityToRes(productEntity)).thenReturn(productRes);

            ProductRes result = productService.getProductBySlug("produto-teste");

            assertNotNull(result);
            verify(productRepository).findBySlug("produto-teste");
        }

        @Test
        void getAllProductsPaginatedSuccess() {
            Page<Product> page = new PageImpl<>(List.of(productEntity));
            when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(productMapper.entityToRes(any())).thenReturn(productRes);

            Page<ProductRes> result = productService.getAllProductsPaginated("search", null, null, 0, "name", "ASC");

            assertEquals(1, result.getTotalElements());
        }

        @Test
        @SuppressWarnings("unchecked")
        void getProductGalleryWithFiltersSuccess() {
            Page<Product> page = new PageImpl<>(List.of(productEntity));
            when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            Page<ProductRes> result = productService.getProductGallery("query", List.of("cat"), List.of("sup"), 0, "ASC");

            assertNotNull(result);
            verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Testes de Criação")
    class CreateTests {

        @Test
        void createProductSuccess() {
            ProductReq req = new ProductReq();
            req.setName("Novo Produto");
            req.setCategorySlug("cat");
            req.setSupplierSlug("sup");

            when(productRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            when(categoryRepository.findBySlug("cat")).thenReturn(Optional.of(category));
            when(supplierRepository.findBySlug("sup")).thenReturn(Optional.of(supplier));
            when(productMapper.reqToEntity(req)).thenReturn(productEntity);
            when(image.isEmpty()).thenReturn(false);
            when(fileStorageService.storeProductImage(image)).thenReturn("new-image.jpg");
            when(productRepository.save(any())).thenReturn(productEntity);

            Product result = productService.createProduct(req, image);

            assertNotNull(result);
            verify(fileStorageService).storeProductImage(image);
            verify(productRepository).save(any());
        }

        @Test
        void createProductCategoryNotFound() {
            ProductReq req = new ProductReq();
            req.setCategorySlug("invalid");

            when(productRepository.findByName(any())).thenReturn(Optional.empty());
            when(productRepository.findBySlug(any())).thenReturn(Optional.empty());
            when(categoryRepository.findBySlug("invalid")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> productService.createProduct(req, null));
        }
    }

    @Nested
    @DisplayName("Testes de Atualização")
    class UpdateTests {

        @Test
        void updateProductSuccessWithNewImage() {
            ProductReq req = new ProductReq();
            req.setName("Nome Atualizado");

            when(productRepository.findBySlug("slug")).thenReturn(Optional.of(productEntity));
            when(productRepository.findBySlug("nome-atualizado")).thenReturn(Optional.empty());
            when(image.isEmpty()).thenReturn(false);
            when(fileStorageService.storeProductImage(image)).thenReturn("updated.jpg");

            productService.updateProductBySlug(req, image, "slug");

            verify(fileStorageService).deleteProductImage("old-image.jpg");
            verify(fileStorageService).storeProductImage(image);
            verify(productRepository).save(productEntity);
        }

        @Test
        void updateProductSlugConflict() {
            ProductReq req = new ProductReq();
            req.setName("Produto Existente");
            Product otherProduct = new Product();
            otherProduct.setSlug("produto-existente");

            when(productRepository.findBySlug("original")).thenReturn(Optional.of(productEntity));
            when(productRepository.findBySlug("produto-existente")).thenReturn(Optional.of(otherProduct));

            assertThrows(ResourceAlreadyExistsException.class, () ->
                    productService.updateProductBySlug(req, null, "original"));
        }
    }

    @Nested
    @DisplayName("Testes de Deleção")
    class DeleteTests {

        @Test
        void deleteProductSuccess() {
            when(productRepository.findBySlug("slug")).thenReturn(Optional.of(productEntity));

            productService.deleteProductBySlug("slug");

            verify(fileStorageService).deleteProductImage("old-image.jpg");
            verify(productRepository).delete(productEntity);
        }

        @Test
        void deleteProductDatabaseError() {
            when(productRepository.findBySlug(anyString())).thenReturn(Optional.of(productEntity));
            doThrow(new RuntimeException()).when(productRepository).delete(any(Product.class));
            assertThrows(DatabaseOperationException.class, () -> productService.deleteProductBySlug("slug"));
        }
    }

    @Nested
    @DisplayName("Testes de Segurança e Hacking")
    class SecurityTests {

        @Test
        void sqlInjectionOnSearch() {
            Page<Product> emptyPage = new PageImpl<>(Collections.emptyList());
            String maliciousSearch = "'; DROP TABLE products; --";

            when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

            assertDoesNotThrow(() -> productService.getAllProductsPaginated(maliciousSearch, null, null, 0, "name", "ASC"));
        }

        @Test
        void xssOnProductName() {
            ProductReq req = new ProductReq();
            req.setName("<script>alert('hack')</script>");
            req.setCategorySlug("cat");
            req.setSupplierSlug("sup");

            when(categoryRepository.findBySlug(any())).thenReturn(Optional.of(category));
            when(supplierRepository.findBySlug(any())).thenReturn(Optional.of(supplier));
            when(productMapper.reqToEntity(any())).thenReturn(productEntity);
            when(productRepository.save(any())).thenReturn(productEntity);

            assertDoesNotThrow(() -> productService.createProduct(req, null));
        }

        @Test
        void pathTraversalOnSlug() {
            String maliciousSlug = "../../../etc/passwd";
            when(productRepository.findBySlug(maliciousSlug)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> productService.getProductBySlug(maliciousSlug));
        }
    }
}