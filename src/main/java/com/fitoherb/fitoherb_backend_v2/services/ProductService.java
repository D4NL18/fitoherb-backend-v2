package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.ProductReq;
import com.fitoherb.fitoherb_backend_v2.DTOs.Responses.ProductRes;
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
import com.fitoherb.fitoherb_backend_v2.utils.StringUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductService {

    @Autowired
    ProductMapper productMapper;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    SupplierRepository supplierRepository;

    @Autowired
    ProductCategoryRepository categoryRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public Page<ProductRes> getAllProductsPaginated(String search, int page, String sortField, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, 10, Sort.by(sortDirection, sortField));

        String searchTerm = (search == null) ? "" : search;
        org.springframework.data.domain.Page<Product> productPage = productRepository.findAllFiltered(searchTerm, pageable);

        return productPage.map(productMapper::entityToRes);
    }

    public Page<ProductRes> getProductGallery(String search, String category, String supplier, int page, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, 18, Sort.by(sortDirection, "name"));

        Specification<Product> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if (category != null && !category.isBlank()) {
            if (!categoryRepository.findBySlug(category).isPresent()) {
                throw new ResourceNotFoundException("Category not found with slug: " + category);
            }
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("slug"), category)
            );
        }

        if (supplier != null && !supplier.isBlank()) {
            if (!supplierRepository.findBySlug(supplier).isPresent()) {
                throw new ResourceNotFoundException("Supplier not found with slug: " + supplier);
            }
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("supplier").get("slug"), supplier)
            );
        }

        if (search != null && !search.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%")
            );
        }

        return productRepository.findAll(spec, pageable)
                .map(productMapper::entityToRes);
    }

    public ProductRes getProductBySlug(String slug) {
        Product product = this.productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
        ProductRes productRes = productMapper.entityToRes(product);
        return productRes;
    }

    @Transactional
    public Product createProduct(ProductReq productReq, MultipartFile image) {
        if (this.productRepository.findByName(productReq.getName()).isPresent()) {
            throw new ResourceAlreadyExistsException("Product with that name already exists");
        }

        String generatedSlug = StringUtils.toSlug(productReq.getName());
        if (this.productRepository.findBySlug(generatedSlug).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "A product with a similar name already exists (Slug conflict: " + generatedSlug + ")"
            );
        }

        ProductCategory category = categoryRepository.findBySlug(productReq.getCategorySlug())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + productReq.getCategorySlug()));

        Supplier supplier = supplierRepository.findBySlug(productReq.getSupplierSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with slug: " + productReq.getSupplierSlug()));

        String fileName = null;
        if (image != null && !image.isEmpty()) {
            fileName = fileStorageService.storeProductImage(image);
        }

        Product product = productMapper.reqToEntity(productReq);
        product.setSlug(generatedSlug);
        product.setImagePath(fileName);
        product.setCategory(category);
        product.setSupplier(supplier);

        return productRepository.save(product);
    }

    @Transactional
    public void updateProductBySlug(ProductReq productReq, MultipartFile image, String slug) {
        Product product = this.productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));

        String generatedSlug = StringUtils.toSlug(productReq.getName());

        if (!product.getSlug().equals(generatedSlug)) {
            if (this.productRepository.findBySlug(generatedSlug).isPresent()) {
                throw new ResourceAlreadyExistsException(
                        "A product with a similar name already exists (Slug conflict: " + generatedSlug + ")"
                );
            }
        }

        if (image != null && !image.isEmpty()) {
            fileStorageService.deleteProductImage(product.getImagePath());
            String newFileName = fileStorageService.storeProductImage(image);
            product.setImagePath(newFileName);
        }

        try {
            productMapper.updateEntityFromReq(productReq, product);
            product.setSlug(generatedSlug);

            productRepository.save(product);
        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to update product in database.");
        }
    }

    @Transactional
    public void deleteProductBySlug(String slug) {
        Product product = this.productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
        try {
            fileStorageService.deleteProductImage(product.getImagePath());
            this.productRepository.delete(product);
        }catch(Exception e) {
            throw new DatabaseOperationException("Failed to delete product. Ensure there are no records linked to this account.");
        }
    }


}
