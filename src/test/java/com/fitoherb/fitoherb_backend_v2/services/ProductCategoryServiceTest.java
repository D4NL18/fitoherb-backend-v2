package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductCategoryReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductCategoryRes;
import com.fitoherb.fitoherb_backend_v2.entities.ProductCategory;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.mappers.ProductCategoryMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductCategoryRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCategoryServiceTest {

    @Mock private ProductCategoryRepository categoryRepository;
    @Mock private ProductCategoryMapper categoryMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private MultipartFile image;

    @InjectMocks
    private ProductCategoryService productCategoryService;

    @Nested
    @DisplayName("Testes de Busca")
    class FindTests {

        @Test
        void getProductCategoryBySlugSuccess() {
            String slug = "cha-verde";
            ProductCategory entity = new ProductCategory();
            ProductCategoryRes res = new ProductCategoryRes();

            when(categoryRepository.findBySlug(slug)).thenReturn(Optional.of(entity));
            when(categoryMapper.entityToRes(entity)).thenReturn(res);

            ProductCategoryRes result = productCategoryService.getProductCategoryBySlug(slug);

            assertNotNull(result);
            verify(categoryRepository).findBySlug(slug);
        }

        @Test
        void getProductCategoryBySlugNotFound() {
            when(categoryRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> productCategoryService.getProductCategoryBySlug("invalid"));
        }
    }

    @Nested
    @DisplayName("Testes de Listagem e Paginação")
    class ListTests {

        @Test
        void getAllProductCategoriesSuccess() {
            when(categoryRepository.findAll()).thenReturn(List.of(new ProductCategory()));
            when(categoryMapper.toResList(any())).thenReturn(List.of(new ProductCategoryRes()));

            List<ProductCategoryRes> result = productCategoryService.getAllProductCategories();

            assertEquals(1, result.size());
        }

        @Test
        void getAllProductCategoriesPaginatedSuccess() {
            Page<ProductCategory> page = new PageImpl<>(List.of(new ProductCategory()));
            when(categoryRepository.findAllFiltered(anyString(), any(Pageable.class))).thenReturn(page);
            when(categoryMapper.entityToRes(any())).thenReturn(new ProductCategoryRes());

            Page<ProductCategoryRes> result = productCategoryService.getAllProductCategoriesPaginated("test", 0, "name", "ASC");

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("Testes de Criação")
    class CreateTests {

        @Test
        void createProductCategorySuccessWithImage() {
            ProductCategoryReq req = new ProductCategoryReq();
            req.setName("Suplementos");
            ProductCategory entity = new ProductCategory();

            when(categoryRepository.findByName(req.getName())).thenReturn(Optional.empty());
            when(categoryRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            when(categoryMapper.reqToEntity(req)).thenReturn(entity);
            when(image.isEmpty()).thenReturn(false);
            when(fileStorageService.storeCategoryImage(image)).thenReturn("image.jpg");
            when(categoryRepository.save(any())).thenReturn(entity);

            ProductCategory result = productCategoryService.createProductCategory(req, image);

            assertNotNull(result);
            verify(fileStorageService).storeCategoryImage(image);
            verify(categoryRepository).save(entity);
        }

        @Test
        void createProductCategoryDuplicateName() {
            ProductCategoryReq req = new ProductCategoryReq();
            req.setName("Existente");

            when(categoryRepository.findByName(req.getName())).thenReturn(Optional.of(new ProductCategory()));

            assertThrows(ResourceAlreadyExistsException.class, () -> productCategoryService.createProductCategory(req, null));
        }

        @Test
        void createProductCategoryDatabaseError() {
            ProductCategoryReq req = new ProductCategoryReq();
            req.setName("Erro");

            when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(categoryMapper.reqToEntity(any())).thenReturn(new ProductCategory());
            when(categoryRepository.save(any())).thenThrow(new RuntimeException());

            assertThrows(DatabaseOperationException.class, () -> productCategoryService.createProductCategory(req, null));
        }
    }

    @Nested
    @DisplayName("Testes de Atualização")
    class UpdateTests {

        @Test
        void updateProductCategorySuccess() {
            String slug = "antigo";
            ProductCategoryReq req = new ProductCategoryReq();
            req.setName("Novo Nome");
            ProductCategory entity = new ProductCategory();
            entity.setSlug(slug);

            when(categoryRepository.findBySlug(slug)).thenReturn(Optional.of(entity));
            when(categoryRepository.findBySlug("novo-nome")).thenReturn(Optional.empty());
            when(image.isEmpty()).thenReturn(false);

            productCategoryService.updateProductCategoryBySlug(req, slug, image);

            verify(fileStorageService).deleteCategoryImage(any());
            verify(fileStorageService).storeCategoryImage(image);
            verify(categoryRepository).save(entity);
        }

        @Test
        void updateProductCategoryNotFound() {
            when(categoryRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> productCategoryService.updateProductCategoryBySlug(new ProductCategoryReq(), "slug", null));
        }
    }

    @Nested
    @DisplayName("Testes de Deleção")
    class DeleteTests {

        @Test
        void deleteProductCategorySuccess() {
            String slug = "cha";
            ProductCategory entity = new ProductCategory();
            entity.setImagePath("img.jpg");

            when(categoryRepository.findBySlug(slug)).thenReturn(Optional.of(entity));

            productCategoryService.deleteProductCategoryBySlug(slug);

            verify(fileStorageService).deleteCategoryImage("img.jpg");
            verify(categoryRepository).delete(entity);
        }

        @Test
        void deleteProductCategoryWithError() {
            when(categoryRepository.findBySlug(anyString())).thenReturn(Optional.of(new ProductCategory()));
            doThrow(new RuntimeException()).when(categoryRepository).delete(any());

            assertThrows(DatabaseOperationException.class, () -> productCategoryService.deleteProductCategoryBySlug("slug"));
        }
    }

    @Nested
    @DisplayName("Testes de Segurança e Robustez")
    class SecurityTests {

        @Test
        void sqlInjectionOnSearch() {
            Page<ProductCategory> page = new PageImpl<>(Collections.emptyList());
            String maliciousSearch = "'; DROP TABLE product_categories; --";

            when(categoryRepository.findAllFiltered(eq(maliciousSearch), any(Pageable.class))).thenReturn(page);

            assertDoesNotThrow(() -> productCategoryService.getAllProductCategoriesPaginated(maliciousSearch, 0, "name", "ASC"));
        }

        @Test
        void xssOnCategoryName() {
            ProductCategoryReq req = new ProductCategoryReq();
            req.setName("<script>alert('xss')</script>");

            when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(categoryMapper.reqToEntity(req)).thenReturn(new ProductCategory());
            when(categoryRepository.save(any())).thenReturn(new ProductCategory());

            assertDoesNotThrow(() -> productCategoryService.createProductCategory(req, null));
        }

        @Test
        void extremeSlugLength() {
            String longSlug = "a".repeat(2000);
            when(categoryRepository.findBySlug(longSlug)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> productCategoryService.getProductCategoryBySlug(longSlug));
        }

        @Test
        void nullByteInSlug() {
            String maliciousSlug = "category\0.php";
            when(categoryRepository.findBySlug(maliciousSlug)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> productCategoryService.getProductCategoryBySlug(maliciousSlug));
        }
    }
}