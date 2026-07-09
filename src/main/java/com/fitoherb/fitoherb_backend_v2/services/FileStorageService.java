package com.fitoherb.fitoherb_backend_v2.services;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeSupplierImage(MultipartFile file);
    void deleteSupplierImage(String fileName);

    String storeCategoryImage(MultipartFile file);
    void deleteCategoryImage(String fileName);

    String storeProductImage(MultipartFile file);
    void deleteProductImage(String fileName);

    String storeBannerImage(MultipartFile file);
    void deleteBannerImage(String fileName);
}
