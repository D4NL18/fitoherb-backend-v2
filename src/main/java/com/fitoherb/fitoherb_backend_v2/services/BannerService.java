package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.BannerReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.BannerRes;
import com.fitoherb.fitoherb_backend_v2.entities.Banner;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.mappers.BannerMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.BannerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BannerService {

    private final BannerRepository bannerRepository;
    private final BannerMapper bannerMapper;
    private final FileStorageService fileStorageService;

    private static final String NOT_FOUND_MSG = "Banner not found with id: ";

    public List<BannerRes> getActiveBanners() {
        return bannerRepository.findAllByIsActiveTrueOrderByPositionAsc()
                .stream()
                .map(bannerMapper::entityToRes)
                .collect(Collectors.toList());
    }

    public Page<BannerRes> getAllBannersPaginated(String search, int page, String sortField, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, 10, Sort.by(sortDirection, sortField));
        
        String searchTerm = (search == null) ? "" : search;
        return bannerRepository.findAllFiltered(searchTerm, pageable)
                .map(bannerMapper::entityToRes);
    }

    public BannerRes getBannerById(String id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND_MSG + id));
        return bannerMapper.entityToRes(banner);
    }

    @Transactional
    public Banner createBanner(BannerReq bannerReq, MultipartFile image) {
        String fileName = null;
        if (image != null && !image.isEmpty()) {
            fileName = fileStorageService.storeBannerImage(image);
        } else {
            throw new IllegalArgumentException("Banner image is required.");
        }

        Banner banner = bannerMapper.reqToEntity(bannerReq);
        banner.setImagePath(fileName);
        return bannerRepository.save(banner);
    }

    @Transactional
    public void updateBanner(BannerReq bannerReq, MultipartFile image, String id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND_MSG + id));

        if (image != null && !image.isEmpty()) {
            fileStorageService.deleteBannerImage(banner.getImagePath());
            String newFileName = fileStorageService.storeBannerImage(image);
            banner.setImagePath(newFileName);
        }

        try {
            bannerMapper.updateEntityFromReq(bannerReq, banner);
            bannerRepository.save(banner);
        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to update banner in the database.", e);
        }
    }

    @Transactional
    public void deleteBanner(String id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND_MSG + id));
        try {
            fileStorageService.deleteBannerImage(banner.getImagePath());
            bannerRepository.delete(banner);
        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to delete banner.", e);
        }
    }
}
