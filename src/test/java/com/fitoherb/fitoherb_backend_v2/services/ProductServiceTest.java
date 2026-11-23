package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductReq;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductSkuReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ErpCatalogItemSnapshot;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductAdminRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductRes;
import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.entities.ProductCategory;
import com.fitoherb.fitoherb_backend_v2.entities.ProductSku;
import com.fitoherb.fitoherb_backend_v2.entities.Supplier;
import com.fitoherb.fitoherb_backend_v2.enums.ErpMappingStatus;
import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.exceptions.UnprocessableEntityException;
import com.fitoherb.fitoherb_backend_v2.mappers.ProductMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductCategoryRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductSkuRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.SupplierRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductSkuRepository productSkuRepository;
    @Mock private ProductCategoryRepository categoryRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductMapper productMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private ErpCatalogValidationService erpCatalogValidationService;
    @Mock private ProductTopologyValidator topologyValidator;
    @Mock private ProductVariantSanitizer variantSanitizer;
    @Mock private ProductVisibilityCalculator visibilityCalculator;
    @Mock private EntityManager entityManager;
    @Mock private MultipartFile image;

    @InjectMocks
    private ProductService productService;

    private Product productEntity;
    private ProductRes productRes;
    private ProductAdminRes productAdminRes;
    private ProductCategory category;
    private Supplier supplier;

    @BeforeEach
    void setup() {
        productEntity = new Product();
        productEntity.setId("prod-uuid-1");
        productEntity.setSlug("produto-teste");
        productEntity.setImagePath("old-image.jpg");
        productEntity.setActive(true);
        productEntity.setCurationStatus(ProductCurationStatus.COMPLETE);
        productEntity.setSkus(new ArrayList<>());

        productRes = new ProductRes();
        productAdminRes = new ProductAdminRes();

        category = new ProductCategory();
        category.setSlug("categoria-teste");
        category.setName("Categoria Teste");

        supplier = new Supplier();
        supplier.setSlug("fornecedor-teste");
        supplier.setName("Fornecedor Teste");

        productEntity.setCategory(category);
        productEntity.setSupplier(supplier);
    }

    private ProductReq simpleProductReq(String name, String categorySlug, String supplierSlug, Long erpCode) {
        ProductReq req = new ProductReq();
        req.setName(name);
        req.setCategorySlug(categorySlug);
        req.setSupplierSlug(supplierSlug);
        req.setCurationStatus(ProductCurationStatus.COMPLETE);
        req.setSkus(List.of(new ProductSkuReq(erpCode, null, null)));
        return req;
    }

    @Nested
    @DisplayName("Testes de Busca Admin e Galeria Pública (P-408 & P-411)")
    class RetrievalTests {

        @Test
        @DisplayName("GET /products/{slug}: Deve retornar ProductAdminRes via entityToAdminRes")
        void getProductBySlugSuccess() {
            when(productRepository.findBySlug("produto-teste")).thenReturn(Optional.of(productEntity));
            when(productMapper.entityToAdminRes(productEntity)).thenReturn(productAdminRes);

            ProductAdminRes result = productService.getProductBySlug("produto-teste");

            assertNotNull(result);
            verify(productRepository).findBySlug("produto-teste");
            verify(productMapper).entityToAdminRes(productEntity);
            verify(productMapper, never()).entityToPublicRes(any());
        }

        @Test
        @DisplayName("GET /products: Deve paginar produtos admin com suporte a filtros erpStatus, active e curationStatus")
        @SuppressWarnings("unchecked")
        void getAllProductsPaginatedSuccess() {
            Page<Product> page = new PageImpl<>(List.of(productEntity));
            when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(productMapper.entityToAdminRes(any())).thenReturn(productAdminRes);

            Page<ProductAdminRes> result = productService.getAllProductsPaginated(
                    "131477", null, null, List.of(ErpMappingStatus.COMPLETE), true, ProductCurationStatus.COMPLETE, 0, "name", "ASC"
            );

            assertEquals(1, result.getTotalElements());
            verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("GET /products/gallery: Deve filtrar active = true e usar exclusivamente entityToPublicRes (P-408)")
        @SuppressWarnings("unchecked")
        void getProductGalleryWithFiltersSuccess() {
            Page<Product> page = new PageImpl<>(List.of(productEntity));
            when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(productMapper.entityToPublicRes(productEntity)).thenReturn(productRes);

            Page<ProductRes> result = productService.getProductGallery("query", List.of("cat"), List.of("sup"), 0, 10, "ASC");

            assertEquals(1, result.getTotalElements());
            verify(productMapper).entityToPublicRes(productEntity);
            verify(productMapper, never()).entityToAdminRes(any());
        }
    }

    @Nested
    @DisplayName("Testes de Criação com Validação 1:1 ERP e Curadoria PENDING_IMAGE (P-401..P-412)")
    class CreateTests {

        @Test
        @DisplayName("C-01: Deve criar produto simples com SKU validado contra ERP, active=true e curationStatus=COMPLETE")
        void createProductSuccess() {
            ProductReq req = simpleProductReq("Novo Produto", "cat", "fornecedor-teste", 440100L);

            when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            when(categoryRepository.findBySlug("cat")).thenReturn(Optional.of(category));
            when(supplierRepository.findBySlug("fornecedor-teste")).thenReturn(Optional.of(supplier));
            when(productMapper.reqToEntity(req)).thenReturn(productEntity);
            when(image.isEmpty()).thenReturn(false);
            when(fileStorageService.storeProductImage(image)).thenReturn("new-image.jpg");
            when(productRepository.save(any())).thenReturn(productEntity);

            Product result = productService.createProduct(req, image);

            assertNotNull(result);
            assertTrue(result.getActive());
            assertEquals(ProductCurationStatus.COMPLETE, result.getCurationStatus());
            verify(erpCatalogValidationService).validateForProduct(eq(req), isNull());
            verify(fileStorageService).storeProductImage(image);
            verify(productRepository).save(any());
        }

        @Test
        @DisplayName("P-412: Deve permitir criar produto sem imagem quando curationStatus == PENDING_IMAGE")
        void createProductWithoutImageAllowedWhenPendingImage() {
            ProductReq req = simpleProductReq("Creatina Turbo", "cat", "fornecedor-teste", 991050L);
            req.setCurationStatus(ProductCurationStatus.PENDING_IMAGE);

            Product newEntity = new Product();
            when(categoryRepository.findBySlug("cat")).thenReturn(Optional.of(category));
            when(supplierRepository.findBySlug("fornecedor-teste")).thenReturn(Optional.of(supplier));
            when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            when(productMapper.reqToEntity(req)).thenReturn(newEntity);
            when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Product saved = productService.createProduct(req, null);

            assertNull(saved.getImagePath());
            assertTrue(saved.getActive());
            assertEquals(ProductCurationStatus.PENDING_IMAGE, saved.getCurationStatus());
            verify(fileStorageService, never()).storeProductImage(any());
        }

        @Test
        @DisplayName("IMG-001: Deve rejeitar criação sem imagem quando curationStatus == COMPLETE")
        void createProductWithoutImageRejectedWhenComplete() {
            ProductReq req = simpleProductReq("Mel Silvestre", "cat", "fornecedor-teste", 41025L);
            req.setCurationStatus(ProductCurationStatus.COMPLETE);

            assertThrows(IllegalArgumentException.class, () -> productService.createProduct(req, null));
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("P-403: Deve propagar 422 ERP_CODE_NOT_FOUND sem salvar quando ERP estiver disponível")
        void createProductPropagatesErpValidationErrors() {
            ProductReq req = simpleProductReq("Produto Invalido", "cat", "fornecedor-teste", 9999999L);

            when(categoryRepository.findBySlug("cat")).thenReturn(Optional.of(category));
            when(supplierRepository.findBySlug("fornecedor-teste")).thenReturn(Optional.of(supplier));
            when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            doThrow(new UnprocessableEntityException("ERP_CODE_NOT_FOUND", "Código ERP não encontrado"))
                    .when(erpCatalogValidationService).validateForProduct(eq(req), isNull());

            UnprocessableEntityException ex422 = assertThrows(
                    UnprocessableEntityException.class,
                    () -> productService.createProduct(req, image)
            );
            assertEquals("ERP_CODE_NOT_FOUND", ex422.getCode());
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("P-403: Em bypass por ERP indisponível (mapa vazio), deve persistir SKUs com erpLastVerifiedAt=null, ean=null, erpInactive=false, erpInactiveConfirmed=false e product.active=true")
        void createProductWithErpBypassPersistsUnverifiedSkus() {
            ProductReq req = new ProductReq();
            req.setName("Whey Bypass");
            req.setCategorySlug("cat");
            req.setSupplierSlug("fornecedor-teste");
            req.setCurationStatus(ProductCurationStatus.COMPLETE);
            req.setAllowInactiveErp(true);
            req.setSkus(List.of(
                    new ProductSkuReq(131476L, "Baunilha", null),
                    new ProductSkuReq(131477L, "Chocolate", null)
            ));

            when(categoryRepository.findBySlug("cat")).thenReturn(Optional.of(category));
            when(supplierRepository.findBySlug("fornecedor-teste")).thenReturn(Optional.of(supplier));
            when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            when(erpCatalogValidationService.validateForProduct(eq(req), isNull())).thenReturn(Collections.emptyMap());
            when(productMapper.reqToEntity(req)).thenReturn(new Product());
            when(image.isEmpty()).thenReturn(false);
            when(fileStorageService.storeProductImage(image)).thenReturn("bypass-image.webp");
            when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Product saved = productService.createProduct(req, image);

            assertNotNull(saved);
            assertTrue(saved.getActive());
            assertEquals(2, saved.getSkus().size());
            for (ProductSku sku : saved.getSkus()) {
                assertNull(sku.getErpLastVerifiedAt());
                assertNull(sku.getEan());
                assertFalse(Boolean.TRUE.equals(sku.getErpInactive()));
                assertFalse(Boolean.TRUE.equals(sku.getErpInactiveConfirmed()));
            }
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("P-403: Quando ERP disponível retorna snapshot válido, deve preencher erpLastVerifiedAt != null e ean do snapshot")
        void createProductWithValidErpSnapshotPopulatesEanAndLastVerifiedAt() {
            ProductReq req = simpleProductReq("Novo Produto ERP", "cat", "fornecedor-teste", 440100L);
            ErpCatalogItemSnapshot snapshot = ErpCatalogItemSnapshot.builder()
                    .erpCode(440100L)
                    .description("NOVO PRODUTO ERP")
                    .ean("7896311770011")
                    .inativoPro(false)
                    .build();

            when(categoryRepository.findBySlug("cat")).thenReturn(Optional.of(category));
            when(supplierRepository.findBySlug("fornecedor-teste")).thenReturn(Optional.of(supplier));
            when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            when(erpCatalogValidationService.validateForProduct(eq(req), isNull()))
                    .thenReturn(Map.of(440100L, snapshot));
            when(productMapper.reqToEntity(req)).thenReturn(new Product());
            when(image.isEmpty()).thenReturn(false);
            when(fileStorageService.storeProductImage(image)).thenReturn("erp-image.webp");
            when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Product saved = productService.createProduct(req, image);

            assertNotNull(saved);
            assertTrue(saved.getActive());
            assertEquals(1, saved.getSkus().size());
            ProductSku sku = saved.getSkus().get(0);
            assertNotNull(sku.getErpLastVerifiedAt());
            assertEquals("7896311770011", sku.getEan());
            assertFalse(Boolean.TRUE.equals(sku.getErpInactive()));
            assertFalse(Boolean.TRUE.equals(sku.getErpInactiveConfirmed()));
        }

        @Test
        @DisplayName("P-406 / C-09: Deve rejeitar SKU inativo sem allowInactiveErp=true")
        void createProductRejectsInactiveErpWithoutConfirmation() {
            ProductReq req = simpleProductReq("Whey Cookies", "cat", "fornecedor-teste", 131479L);
            req.setAllowInactiveErp(false);

            when(categoryRepository.findBySlug("cat")).thenReturn(Optional.of(category));
            when(supplierRepository.findBySlug("fornecedor-teste")).thenReturn(Optional.of(supplier));
            when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            doThrow(new UnprocessableEntityException("ERP_SKU_INACTIVE_CONFIRMATION_REQUIRED", "SKU inativo"))
                    .when(erpCatalogValidationService).validateForProduct(eq(req), isNull());

            UnprocessableEntityException ex = assertThrows(
                    UnprocessableEntityException.class,
                    () -> productService.createProduct(req, image)
            );
            assertEquals("ERP_SKU_INACTIVE_CONFIRMATION_REQUIRED", ex.getCode());
        }

        @Test
        @DisplayName("P-404 / C-04: Deve lançar 409 Conflict quando código ERP já pertencer a outro produto")
        void createProductRejectsErpCodeMappedToAnotherProduct() {
            ProductReq req = simpleProductReq("Outro Whey", "cat", "fornecedor-teste", 131476L);

            when(categoryRepository.findBySlug("cat")).thenReturn(Optional.of(category));
            when(supplierRepository.findBySlug("fornecedor-teste")).thenReturn(Optional.of(supplier));
            when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            doThrow(new ResourceAlreadyExistsException("O código ERP '131476' já está vinculado ao produto '100% Pure Whey 900g'."))
                    .when(erpCatalogValidationService).validateForProduct(eq(req), isNull());

            assertThrows(ResourceAlreadyExistsException.class, () -> productService.createProduct(req, image));
            verify(productRepository, never()).save(any());
        }

        @Test
        void shouldAllowSameProductNameForDifferentSuppliers() {
            ProductReq req1 = simpleProductReq("Camomila", "cat", "fornecedor-teste", 440100L);
            ProductReq req2 = simpleProductReq("Camomila", "cat", "outro-fornecedor", 440101L);

            Supplier supplier2 = new Supplier();
            supplier2.setSlug("outro-fornecedor");
            supplier2.setName("Outro Fornecedor");

            when(categoryRepository.findBySlug("cat")).thenReturn(Optional.of(category));
            when(supplierRepository.findBySlug("fornecedor-teste")).thenReturn(Optional.of(supplier));
            when(supplierRepository.findBySlug("outro-fornecedor")).thenReturn(Optional.of(supplier2));

            when(productRepository.findBySlug("camomila-fornecedor-teste")).thenReturn(Optional.empty());
            when(productRepository.findBySlug("camomila-outro-fornecedor")).thenReturn(Optional.empty());

            when(productMapper.reqToEntity(req1)).thenReturn(new Product());
            when(productMapper.reqToEntity(req2)).thenReturn(new Product());
            when(image.isEmpty()).thenReturn(false);
            when(fileStorageService.storeProductImage(image)).thenReturn("img.webp");
            when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Product p1 = productService.createProduct(req1, image);
            Product p2 = productService.createProduct(req2, image);

            assertEquals("camomila-fornecedor-teste", p1.getSlug());
            assertEquals("camomila-outro-fornecedor", p2.getSlug());
        }

        @Test
        void createProductCategoryNotFound() {
            ProductReq req = simpleProductReq("Teste", "invalid", "fornecedor-teste", 440100L);

            when(categoryRepository.findBySlug("invalid")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> productService.createProduct(req, image));
        }
    }

    @Nested
    @DisplayName("Testes de Atualização, Reconciliação Atômica de SKUs e Curadoria (P-404, P-412, P-413)")
    class UpdateTests {

        @Test
        @DisplayName("C-05: Deve permitir trocar códigos ERP entre dois sabores do mesmo produto com flush intermediário")
        void updateProductAllowsSwappingErpCodesBetweenSameProductVariants() {
            // Arrange
            ProductSku sku1 = new ProductSku();
            sku1.setId("sku-1");
            sku1.setProduct(productEntity);
            sku1.setErpCode(131476L);
            sku1.setFlavour("Baunilha");

            ProductSku sku2 = new ProductSku();
            sku2.setId("sku-2");
            sku2.setProduct(productEntity);
            sku2.setErpCode(131477L);
            sku2.setFlavour("Chocolate");

            productEntity.setSkus(new ArrayList<>(List.of(sku1, sku2)));

            ProductReq req = new ProductReq();
            req.setName("Produto Teste");
            req.setCategorySlug("categoria-teste");
            req.setSupplierSlug("fornecedor-teste");
            req.setSkus(List.of(
                    new ProductSkuReq(131477L, "Baunilha", null),
                    new ProductSkuReq(131476L, "Chocolate", null)
            ));

            when(productRepository.findBySlug("produto-teste")).thenReturn(Optional.of(productEntity));
            when(productRepository.save(any())).thenReturn(productEntity);

            // Act
            assertDoesNotThrow(() -> productService.updateProductBySlug(req, null, "produto-teste"));

            // Assert
            verify(erpCatalogValidationService).validateForProduct(req, "prod-uuid-1");
            verify(entityManager, atLeastOnce()).flush();
            verify(productRepository).save(productEntity);
        }

        @Test
        @DisplayName("P-403: Na edição em bypass por ERP indisponível (mapa vazio), deve persistir SKUs com erpLastVerifiedAt=null, ean=null, erpInactive=false e erpInactiveConfirmed=false")
        void updateProductWithErpBypassPersistsUnverifiedSkus() {
            ProductReq req = simpleProductReq("Produto Teste", "categoria-teste", "fornecedor-teste", 131476L);
            when(productRepository.findBySlug("produto-teste")).thenReturn(Optional.of(productEntity));
            when(erpCatalogValidationService.validateForProduct(req, "prod-uuid-1")).thenReturn(Collections.emptyMap());
            when(productRepository.save(any())).thenReturn(productEntity);

            productService.updateProductBySlug(req, null, "produto-teste");

            assertTrue(productEntity.getActive());
            assertEquals(1, productEntity.getSkus().size());
            ProductSku sku = productEntity.getSkus().get(0);
            assertNull(sku.getErpLastVerifiedAt());
            assertNull(sku.getEan());
            assertFalse(Boolean.TRUE.equals(sku.getErpInactive()));
            assertFalse(Boolean.TRUE.equals(sku.getErpInactiveConfirmed()));
        }

        @Test
        @DisplayName("C-16: Não deve mascarar DataIntegrityViolationException / ResourceAlreadyExistsException como 500 DatabaseOperationException")
        void updateProductDoesNotMaskConflictAs500() {
            ProductReq req = simpleProductReq("Produto Teste", "categoria-teste", "fornecedor-teste", 131476L);

            when(productRepository.findBySlug("produto-teste")).thenReturn(Optional.of(productEntity));
            when(productRepository.save(any())).thenThrow(new DataIntegrityViolationException("uk_product_skus_erp_code"));

            assertThrows(
                    DataIntegrityViolationException.class,
                    () -> productService.updateProductBySlug(req, null, "produto-teste")
            );
        }

        @Test
        @DisplayName("P-412 & P-413: Edição de produto PENDING_IMAGE enviando imagem deve promover para COMPLETE")
        void updatePendingImageWithValidImagePromotesToComplete() {
            // Arrange
            productEntity.setImagePath(null);
            productEntity.setCurationStatus(ProductCurationStatus.PENDING_IMAGE);

            ProductReq req = simpleProductReq("Produto Teste", "categoria-teste", "fornecedor-teste", 991050L);

            when(productRepository.findBySlug("produto-teste")).thenReturn(Optional.of(productEntity));
            when(image.isEmpty()).thenReturn(false);
            when(fileStorageService.storeProductImage(image)).thenReturn("uploaded.webp");

            // Act
            productService.updateProductBySlug(req, image, "produto-teste");

            // Assert
            assertEquals("uploaded.webp", productEntity.getImagePath());
            assertEquals(ProductCurationStatus.COMPLETE, productEntity.getCurationStatus());
        }

        @Test
        @DisplayName("P-413: Edição de produto PENDING_IMAGE sem enviar imagem deve manter PENDING_IMAGE")
        void updatePendingImageWithoutImageKeepsPendingImage() {
            // Arrange
            productEntity.setImagePath(null);
            productEntity.setCurationStatus(ProductCurationStatus.PENDING_IMAGE);

            ProductReq req = simpleProductReq("Produto Teste", "categoria-teste", "fornecedor-teste", 991050L);
            when(productRepository.findBySlug("produto-teste")).thenReturn(Optional.of(productEntity));

            // Act
            productService.updateProductBySlug(req, null, "produto-teste");

            // Assert
            assertNull(productEntity.getImagePath());
            assertEquals(ProductCurationStatus.PENDING_IMAGE, productEntity.getCurationStatus());
        }

        @Test
        void updateProductSlugConflict() {
            ProductReq req = simpleProductReq("Produto Existente", "categoria-teste", "fornecedor-teste", 440100L);

            Product otherProduct = new Product();
            otherProduct.setSlug("produto-existente-fornecedor-teste");

            when(productRepository.findBySlug("original")).thenReturn(Optional.of(productEntity));
            when(productRepository.findBySlug("produto-existente-fornecedor-teste")).thenReturn(Optional.of(otherProduct));

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
        @SuppressWarnings("unchecked")
        void sqlInjectionOnSearch() {
            Page<Product> emptyPage = new PageImpl<>(Collections.emptyList());
            String maliciousSearch = "'; DROP TABLE products; --";

            when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

            assertDoesNotThrow(() -> productService.getAllProductsPaginated(
                    maliciousSearch, null, null, null, null, null, 0, "name", "ASC"
            ));
        }

        @Test
        void pathTraversalOnSlug() {
            String maliciousSlug = "../../../etc/passwd";
            when(productRepository.findBySlug(maliciousSlug)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> productService.getProductBySlug(maliciousSlug));
        }
    }
}
