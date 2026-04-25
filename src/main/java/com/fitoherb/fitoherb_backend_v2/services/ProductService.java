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
import com.fitoherb.fitoherb_backend_v2.utils.StringUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductMapper productMapper;

    private final ProductRepository productRepository;

    private final SupplierRepository supplierRepository;

    private final ProductCategoryRepository categoryRepository;

    private final FileStorageService fileStorageService;

    private static final String PRODUCT_NOT_FOUND_MSG = "Produto não encontrado com slug: ";

    public Page<ProductRes> getAllProductsPaginated(String search, int page, String sortField, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, 10, Sort.by(sortDirection, sortField));

        String searchTerm = (search == null) ? "" : search;
        org.springframework.data.domain.Page<Product> productPage = productRepository.findAllFiltered(searchTerm, pageable);

        return productPage.map(productMapper::entityToRes);
    }

    public Page<ProductRes> getProductGallery(String search, String category, String supplier, int page, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, 9, Sort.by(sortDirection, "name"));

        Specification<Product> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if (category != null && !category.isBlank()) {
            if (!categoryRepository.findBySlug(category).isPresent()) {
                throw new ResourceNotFoundException("Categoria não encontrada com slug: " + category);
            }
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("slug"), category)
            );
        }

        if (supplier != null && !supplier.isBlank()) {
            if (!supplierRepository.findBySlug(supplier).isPresent()) {
                throw new ResourceNotFoundException("Fornecedor não encontrado com slug: " + supplier);
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
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MSG + slug));
        return productMapper.entityToRes(product);
    }

    @Transactional
    public Product createProduct(ProductReq productReq, MultipartFile image) {
        if (this.productRepository.findByName(productReq.getName()).isPresent()) {
            throw new ResourceAlreadyExistsException("Já existe um produto com esse nome");
        }

        String generatedSlug = StringUtils.toSlug(productReq.getName());
        if (this.productRepository.findBySlug(generatedSlug).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "Já existe um produto com um nome similar (Conflito de slug: " + generatedSlug + ")"
            );
        }

        ProductCategory category = categoryRepository.findBySlug(productReq.getCategorySlug())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com slug: " + productReq.getCategorySlug()));

        Supplier supplier = supplierRepository.findBySlug(productReq.getSupplierSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado com slug: " + productReq.getSupplierSlug()));

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
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MSG + slug));

        String generatedSlug = StringUtils.toSlug(productReq.getName());

        if (!product.getSlug().equals(generatedSlug) && this.productRepository.findBySlug(generatedSlug).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "Já existe um produto com um nome similar (Conflito de slug: " + generatedSlug + ")"
            );
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
            throw new DatabaseOperationException("Falha ao atualizar produto no banco de dados.", e);
        }
    }

    @Transactional
    public void deleteProductBySlug(String slug) {
        Product product = this.productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MSG + slug));
        try {
            fileStorageService.deleteProductImage(product.getImagePath());
            this.productRepository.delete(product);
        }catch(Exception e) {
            throw new DatabaseOperationException("Falha ao excluir produto. Verifique se não há registros vinculados a este cadastro.", e);
        }
    }


}
