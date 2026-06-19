package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.BannerReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.BannerRes;
import com.fitoherb.fitoherb_backend_v2.entities.Banner;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.mappers.BannerMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.BannerRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BannerServiceTest {

    @Mock private BannerRepository bannerRepository;
    @Mock private BannerMapper bannerMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private MultipartFile image;

    @InjectMocks
    private BannerService bannerService;

    @Nested
    @DisplayName("Testes de Busca")
    class FindTests {

        @Test
        void getBannerByIdSuccess() {
            String id = "uuid-1234";
            Banner entity = new Banner();
            entity.setId(id);
            BannerRes res = new BannerRes();
            res.setId(id);

            when(bannerRepository.findById(id)).thenReturn(Optional.of(entity));
            when(bannerMapper.entityToRes(entity)).thenReturn(res);

            BannerRes result = bannerService.getBannerById(id);

            assertNotNull(result);
            assertEquals(id, result.getId());
            verify(bannerRepository).findById(id);
        }

        @Test
        void getBannerByIdNotFound() {
            when(bannerRepository.findById(anyString())).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> bannerService.getBannerById("invalid"));
        }
    }

    @Nested
    @DisplayName("Testes de Listagem e Paginação")
    class ListTests {

        @Test
        void getActiveBannersSuccess() {
            Banner entity = new Banner();
            BannerRes res = new BannerRes();

            when(bannerRepository.findAllByIsActiveTrueOrderByPositionAsc()).thenReturn(List.of(entity));
            when(bannerMapper.entityToRes(entity)).thenReturn(res);

            List<BannerRes> result = bannerService.getActiveBanners();

            assertEquals(1, result.size());
            verify(bannerRepository).findAllByIsActiveTrueOrderByPositionAsc();
        }

        @Test
        void getAllBannersPaginatedSuccess() {
            Page<Banner> page = new PageImpl<>(List.of(new Banner()));
            BannerRes res = new BannerRes();

            when(bannerRepository.findAllFiltered(anyString(), any(Pageable.class))).thenReturn(page);
            when(bannerMapper.entityToRes(any())).thenReturn(res);

            Page<BannerRes> result = bannerService.getAllBannersPaginated("test", 0, "position", "ASC");

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(bannerRepository).findAllFiltered(eq("test"), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Testes de Criação")
    class CreateTests {

        @Test
        void createBannerSuccess() {
            BannerReq req = new BannerReq();
            req.setTitle("Promo");
            Banner entity = new Banner();

            when(bannerMapper.reqToEntity(req)).thenReturn(entity);
            when(image.isEmpty()).thenReturn(false);
            when(fileStorageService.storeBannerImage(image)).thenReturn("banner.jpg");
            when(bannerRepository.save(any())).thenReturn(entity);

            Banner result = bannerService.createBanner(req, image);

            assertNotNull(result);
            assertEquals("banner.jpg", entity.getImagePath());
            verify(fileStorageService).storeBannerImage(image);
            verify(bannerRepository).save(entity);
        }

        @Test
        void createBannerWithoutImage() {
            BannerReq req = new BannerReq();

            assertThrows(IllegalArgumentException.class, () -> bannerService.createBanner(req, null));
        }

        @Test
        void createBannerDatabaseError() {
            BannerReq req = new BannerReq();

            when(bannerMapper.reqToEntity(any())).thenReturn(new Banner());
            when(image.isEmpty()).thenReturn(false);
            when(fileStorageService.storeBannerImage(image)).thenReturn("banner.jpg");
            when(bannerRepository.save(any())).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> bannerService.createBanner(req, image));
        }
    }

    @Nested
    @DisplayName("Testes de Atualização")
    class UpdateTests {

        @Test
        void updateBannerSuccessWithImage() {
            String id = "uuid-123";
            BannerReq req = new BannerReq();
            Banner entity = new Banner();
            entity.setId(id);
            entity.setImagePath("old.jpg");

            when(bannerRepository.findById(id)).thenReturn(Optional.of(entity));
            when(image.isEmpty()).thenReturn(false);
            when(fileStorageService.storeBannerImage(image)).thenReturn("new.jpg");

            bannerService.updateBanner(req, image, id);

            verify(fileStorageService).deleteBannerImage("old.jpg");
            verify(fileStorageService).storeBannerImage(image);
            verify(bannerMapper).updateEntityFromReq(req, entity);
            verify(bannerRepository).save(entity);
            assertEquals("new.jpg", entity.getImagePath());
        }

        @Test
        void updateBannerSuccessWithoutImage() {
            String id = "uuid-123";
            BannerReq req = new BannerReq();
            Banner entity = new Banner();
            entity.setId(id);

            when(bannerRepository.findById(id)).thenReturn(Optional.of(entity));

            bannerService.updateBanner(req, null, id);

            verify(fileStorageService, never()).deleteBannerImage(anyString());
            verify(fileStorageService, never()).storeBannerImage(any());
            verify(bannerRepository).save(entity);
        }

        @Test
        void updateBannerNotFound() {
            when(bannerRepository.findById(anyString())).thenReturn(Optional.empty());

            BannerReq req = new BannerReq();

            assertThrows(ResourceNotFoundException.class, () -> bannerService.updateBanner(req, null, "invalid"));
        }
    }

    @Nested
    @DisplayName("Testes de Deleção")
    class DeleteTests {

        @Test
        void deleteBannerSuccess() {
            String id = "uuid-123";
            Banner entity = new Banner();
            entity.setImagePath("banner.jpg");

            when(bannerRepository.findById(id)).thenReturn(Optional.of(entity));

            bannerService.deleteBanner(id);

            verify(fileStorageService).deleteBannerImage("banner.jpg");
            verify(bannerRepository).delete(entity);
        }

        @Test
        void deleteBannerWithError() {
            String id = "uuid-123";
            when(bannerRepository.findById(id)).thenReturn(Optional.of(new Banner()));
            doThrow(new RuntimeException()).when(bannerRepository).delete(any());

            assertThrows(DatabaseOperationException.class, () -> bannerService.deleteBanner(id));
        }
    }

    @Nested
    @DisplayName("Testes de Segurança e Robustez")
    class SecurityTests {

        @Test
        void sqlInjectionOnSearch() {
            Page<Banner> page = new PageImpl<>(Collections.emptyList());
            String maliciousSearch = "'; DROP TABLE banners; --";

            when(bannerRepository.findAllFiltered(eq(maliciousSearch), any(Pageable.class))).thenReturn(page);

            assertDoesNotThrow(() -> bannerService.getAllBannersPaginated(maliciousSearch, 0, "position", "ASC"));
        }
    }
}
