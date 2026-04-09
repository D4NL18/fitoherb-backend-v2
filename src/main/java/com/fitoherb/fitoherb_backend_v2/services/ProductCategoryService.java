package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.ProductCategoryReq;
import com.fitoherb.fitoherb_backend_v2.DTOs.Responses.ProductCategoryRes;
import com.fitoherb.fitoherb_backend_v2.entities.ProductCategory;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.mappers.ProductCategoryMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductCategoryRepository;
import com.fitoherb.fitoherb_backend_v2.utils.StringUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ProductCategoryService {

    @Autowired
    ProductCategoryRepository categoryRepository;

    @Autowired
    ProductCategoryMapper categoryMapper;

    @Autowired
    private FileStorageService fileStorageService;

    public ProductCategoryRes getProductCategoryBySlug(String slug) {
        ProductCategory category = this.categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        ProductCategoryRes categoryRes = categoryMapper.entityToRes(category);
        return categoryRes;
    }

    public List<ProductCategoryRes> getAllProductCategories() {
        List<ProductCategory> categoriesList = this.categoryRepository.findAll();

        return categoryMapper.toResList(categoriesList);
    }

    public Page<ProductCategoryRes> getAllProductCategoriesPaginated(String search, int page, String sortField, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, 10, Sort.by(sortDirection, sortField));

        String searchTerm = (search == null) ? "" : search;
        org.springframework.data.domain.Page<ProductCategory> categoryPage = categoryRepository.findAllFiltered(searchTerm, pageable);

        return categoryPage.map(categoryMapper::entityToRes);
    }

    @Transactional
    public ProductCategory createProductCategory(ProductCategoryReq categoryReq, MultipartFile image) {
        if(this.categoryRepository.findByName(categoryReq.getName()).isPresent()) {
            throw new ResourceAlreadyExistsException("Category with that name already exists");
        }

        String generatedSlug = StringUtils.toSlug(categoryReq.getName());

        if (this.categoryRepository.findBySlug(generatedSlug).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "A category with a similar name already exists (Slug conflict: " + generatedSlug + ")"
            );
        }
        ProductCategory category = categoryMapper.reqToEntity(categoryReq);

        String fileName = null;
        if (image != null && !image.isEmpty()) {
            fileName = fileStorageService.storeCategoryImage(image);
        }

        try {
            category.setSlug(generatedSlug);
            category.setImagePath(fileName);
            return categoryRepository.save(category);
        }catch (Exception e) {
            throw new DatabaseOperationException("Failed to create category.");
        }
    }

    @Transactional
    public void updateProductCategoryBySlug(ProductCategoryReq categoryReq, String slug, MultipartFile image) {
        ProductCategory category = this.categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));

        String generatedSlug = StringUtils.toSlug(categoryReq.getName());

        if (!category.getSlug().equals(generatedSlug)) {
            if (this.categoryRepository.findBySlug(generatedSlug).isPresent()) {
                throw new ResourceAlreadyExistsException(
                        "A category with a similar name already exists (Slug conflict: " + generatedSlug + ")"
                );
            }
        }
        if (image != null && !image.isEmpty()) {
            fileStorageService.deleteCategoryImage(category.getImagePath());
            String newFileName = fileStorageService.storeCategoryImage(image);
            category.setImagePath(newFileName);
        }
        try {
            categoryMapper.updateEntityFromReq(categoryReq, category);
            category.setSlug(generatedSlug);

            categoryRepository.save(category);
        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to update category in database.");
        }
    }

    @Transactional
    public void deleteProductCategoryBySlug(String slug) {
        ProductCategory category = this.categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        try {
            fileStorageService.deleteCategoryImage(category.getImagePath());
            this.categoryRepository.delete(category);
        }catch(Exception e) {
            throw new DatabaseOperationException("Failed to delete category. Ensure there are no records linked to this account.");
        }
    }
}
