package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.SupplierReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.SupplierRes;
import com.fitoherb.fitoherb_backend_v2.entities.Supplier;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.mappers.SupplierMapper;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock private SupplierRepository supplierRepository;
    @Mock private SupplierMapper supplierMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private MultipartFile image;

    @InjectMocks
    private SupplierService supplierService;

    private Supplier supplierEntity;
    private SupplierRes supplierRes;

    @BeforeEach
    void setup() {
        supplierEntity = new Supplier();
        supplierEntity.setName("Fornecedor Original");
        supplierEntity.setSlug("fornecedor-original");
        supplierEntity.setImagePath("logo.jpg");

        supplierRes = new SupplierRes();
    }

    @Nested
    @DisplayName("Testes de Busca")
    class FindTests {

        @Test
        void getAllSuppliersSuccess() {
            when(supplierRepository.findAll(org.springframework.data.domain.Sort.by("name"))).thenReturn(List.of(supplierEntity));
            when(supplierMapper.toResList(any())).thenReturn(List.of(supplierRes));
            when(supplierRepository.countProductsPerSupplier()).thenReturn(Collections.singletonList(new Object[]{"fornecedor-original", 10L}));
            List<SupplierRes> result = supplierService.getAllSuppliers();

            assertFalse(result.isEmpty());
            verify(supplierRepository).findAll(org.springframework.data.domain.Sort.by("name"));
            verify(supplierRepository).countProductsPerSupplier();
        }

        @Test
        void getSupplierBySlugSuccess() {
            when(supplierRepository.findBySlug("fornecedor-original")).thenReturn(Optional.of(supplierEntity));
            when(supplierMapper.entityToRes(supplierEntity)).thenReturn(supplierRes);
            when(supplierRepository.countProductsBySupplierSlug("fornecedor-original")).thenReturn(5);

            SupplierRes result = supplierService.getSupplierBySlug("fornecedor-original");

            assertNotNull(result);
            verify(supplierRepository).findBySlug("fornecedor-original");
            verify(supplierRepository).countProductsBySupplierSlug("fornecedor-original");
        }

        @Test
        void getAllSuppliersPaginatedSuccess() {
            Page<Supplier> page = new PageImpl<>(List.of(supplierEntity));
            when(supplierRepository.findAllFiltered(anyString(), any(Pageable.class))).thenReturn(page);
            when(supplierMapper.entityToRes(any())).thenReturn(supplierRes);
            when(supplierRepository.countProductsPerSupplier()).thenReturn(Collections.singletonList(new Object[]{"fornecedor-original", 2L}));
            Page<SupplierRes> result = supplierService.getAllSuppliersPaginated("teste", 0, "name", "ASC");

            assertEquals(1, result.getTotalElements());
            verify(supplierRepository).countProductsPerSupplier();
        }

        @Test
        void getAllSuppliersHandlesMissingCountGracefully() {
            when(supplierRepository.findAll(org.springframework.data.domain.Sort.by("name"))).thenReturn(List.of(supplierEntity));
            when(supplierMapper.toResList(any())).thenReturn(List.of(supplierRes));
            when(supplierRepository.countProductsPerSupplier()).thenReturn(Collections.emptyList());

            List<SupplierRes> result = supplierService.getAllSuppliers();

            assertFalse(result.isEmpty());
            assertEquals(0, result.get(0).getCount());
        }
    }

    @Nested
    @DisplayName("Testes de Criação")
    class CreateTests {

        @Test
        void createSupplierSuccessWithImage() {
            SupplierReq req = new SupplierReq();
            req.setName("Novo Fornecedor");

            when(supplierRepository.findByName(req.getName())).thenReturn(Optional.empty());
            when(supplierRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            when(image.isEmpty()).thenReturn(false);
            when(fileStorageService.storeSupplierImage(image)).thenReturn("new-logo.jpg");
            when(supplierMapper.reqToEntity(req)).thenReturn(supplierEntity);
            when(supplierRepository.save(any())).thenReturn(supplierEntity);

            Supplier result = supplierService.createSupplier(req, image);

            assertNotNull(result);
            verify(fileStorageService).storeSupplierImage(image);
            verify(supplierRepository).save(any());
        }

        @Test
        void createSupplierDuplicateName() {
            SupplierReq req = new SupplierReq();
            req.setName("Existente");
            when(supplierRepository.findByName(req.getName())).thenReturn(Optional.of(supplierEntity));

            assertThrows(ResourceAlreadyExistsException.class, () -> supplierService.createSupplier(req, null));
        }
    }

    @Nested
    @DisplayName("Testes de Atualização")
    class UpdateTests {

        @Test
        void updateSupplierSuccessWithImageChange() {
            SupplierReq req = new SupplierReq();
            req.setName("Nome Novo");

            when(supplierRepository.findBySlug("fornecedor-original")).thenReturn(Optional.of(supplierEntity));
            when(supplierRepository.findBySlug("nome-novo")).thenReturn(Optional.empty());
            when(image.isEmpty()).thenReturn(false);
            when(fileStorageService.storeSupplierImage(image)).thenReturn("updated.jpg");

            supplierService.updateSupplierBySlug(req, "fornecedor-original", image);

            verify(fileStorageService).deleteSupplierImage("logo.jpg");
            verify(fileStorageService).storeSupplierImage(image);
            verify(supplierRepository).save(supplierEntity);
        }

        @Test
        void updateSupplierSlugConflict() {
            SupplierReq req = new SupplierReq();
            req.setName("Outro Fornecedor");
            Supplier other = new Supplier();
            other.setSlug("outro-fornecedor");

            when(supplierRepository.findBySlug("fornecedor-original")).thenReturn(Optional.of(supplierEntity));
            when(supplierRepository.findBySlug("outro-fornecedor")).thenReturn(Optional.of(other));

            assertThrows(ResourceAlreadyExistsException.class, () ->
                    supplierService.updateSupplierBySlug(req, "fornecedor-original", null));
        }
    }

    @Nested
    @DisplayName("Testes de Deleção")
    class DeleteTests {

        @Test
        void deleteSupplierSuccess() {
            when(supplierRepository.findBySlug("slug")).thenReturn(Optional.of(supplierEntity));

            supplierService.deleteSupplierBySlug("slug");

            verify(fileStorageService).deleteSupplierImage("logo.jpg");
            verify(supplierRepository).delete(supplierEntity);
        }

        @Test
        void deleteSupplierDatabaseError() {
            when(supplierRepository.findBySlug(anyString())).thenReturn(Optional.of(supplierEntity));
            // Explicit type to avoid ambiguity
            doThrow(new RuntimeException()).when(supplierRepository).delete(any(Supplier.class));

            assertThrows(DatabaseOperationException.class, () -> supplierService.deleteSupplierBySlug("slug"));
        }
    }

    @Nested
    @DisplayName("Testes de Segurança e Robustez")
    class SecurityTests {

        @Test
        void sqlInjectionOnSearch() {
            Page<Supplier> emptyPage = new PageImpl<>(Collections.emptyList());
            String malicious = "'; DROP TABLE suppliers; --";
            when(supplierRepository.findAllFiltered(eq(malicious), any(Pageable.class))).thenReturn(emptyPage);

            assertDoesNotThrow(() -> supplierService.getAllSuppliersPaginated(malicious, 0, "name", "ASC"));
        }

        @Test
        void xssOnSupplierName() {
            SupplierReq req = new SupplierReq();
            req.setName("<img src=x onerror=alert(1)>");

            when(supplierRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(supplierMapper.reqToEntity(req)).thenReturn(supplierEntity);
            when(supplierRepository.save(any())).thenReturn(supplierEntity);

            assertDoesNotThrow(() -> supplierService.createSupplier(req, null));
        }

        @Test
        void nullByteInSlug() {
            when(supplierRepository.findBySlug("test\0")).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> supplierService.getSupplierBySlug("test\0"));
        }
    }
}