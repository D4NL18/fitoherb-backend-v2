package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductCategoryReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductCategoryRes;
import com.fitoherb.fitoherb_backend_v2.entities.ProductCategory;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.mappers.ProductCategoryMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductCategoryRepository;
import com.fitoherb.fitoherb_backend_v2.utils.StringUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductCategoryService {

    private final ProductCategoryRepository categoryRepository;

    private final ProductCategoryMapper categoryMapper;

    private final FileStorageService fileStorageService;

    private static final String NOT_FOUND_MSG = "Category not found with slug: ";

    public ProductCategoryRes getProductCategoryBySlug(String slug) {
        ProductCategory category = this.categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND_MSG + slug));

        ProductCategoryRes res = categoryMapper.entityToRes(category);

        res.setCount(this.categoryRepository.countProductsByCategorySlug(slug));

        return res;
    }

    public List<ProductCategoryRes> getAllProductCategories() {
        List<ProductCategory> categoriesList = this.categoryRepository.findAll();
        List<ProductCategoryRes> resList = categoryMapper.toResList(categoriesList);

        enrichWithProductCount(resList);

        return resList;
    }

    public Page<ProductCategoryRes> getAllProductCategoriesPaginated(String search, int page, String sortField, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, 10, Sort.by(sortDirection, sortField));

        String searchTerm = (search == null) ? "" : search;
        Page<ProductCategory> categoryPage = categoryRepository.findAllFiltered(searchTerm, pageable);

        Page<ProductCategoryRes> resPage = categoryPage.map(categoryMapper::entityToRes);

        enrichWithProductCount(resPage.getContent());

        return resPage;
    }

    private void enrichWithProductCount(List<ProductCategoryRes> dtos) {
        Map<String, Long> countsMap = categoryRepository.countProductsPerCategory()
                .stream()
                .collect(Collectors.toMap(
                        obj -> (String) obj[0],
                        obj -> (Long) obj[1]
                ));

        dtos.forEach(dto -> {
            dto.setCount(countsMap.getOrDefault(dto.getSlug(), 0L).intValue());
        });
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
            throw new DatabaseOperationException("Failed to create category.", e);
        }
    }

    @Transactional
    public void updateProductCategoryBySlug(ProductCategoryReq categoryReq, String slug, MultipartFile image) {
        ProductCategory category = this.categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND_MSG + slug));

        String generatedSlug = StringUtils.toSlug(categoryReq.getName());

        if (!category.getSlug().equals(generatedSlug) && this.categoryRepository.findBySlug(generatedSlug).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "A category with a similar name already exists (Slug conflict: " + generatedSlug + ")"
            );
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
            throw new DatabaseOperationException("Failed to update category in database.", e);
        }
    }

    @Transactional
    public void deleteProductCategoryBySlug(String slug) {
        ProductCategory category = this.categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND_MSG + slug));
        try {
            fileStorageService.deleteCategoryImage(category.getImagePath());
            this.categoryRepository.delete(category);
        }catch(Exception e) {
            throw new DatabaseOperationException("Failed to delete category. Ensure there are no records linked to this account.", e);
        }
    }
}
