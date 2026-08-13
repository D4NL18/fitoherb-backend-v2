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
import java.util.List;
@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductMapper productMapper;

    private final ProductRepository productRepository;

    private final SupplierRepository supplierRepository;

    private final ProductCategoryRepository categoryRepository;

    private final FileStorageService fileStorageService;

    private static final String PRODUCT_NOT_FOUND_MSG = "Produto não encontrado com slug: ";
    private static final String ACCENTED_CHARS = "áàâãäéèêëíìîïóòôõöúùûüçñ";
    private static final String UNACCENTED_CHARS = "aaaaaeeeeiiiiooooouuuucn";

    private jakarta.persistence.criteria.Expression<String> getTranslateExpr(jakarta.persistence.criteria.CriteriaBuilder cb, jakarta.persistence.criteria.Expression<String> expression) {
        return cb.function("translate", String.class, expression, cb.literal(ACCENTED_CHARS), cb.literal(UNACCENTED_CHARS));
    }

    public Page<ProductRes> getAllProductsPaginated(String search, java.util.List<String> categories, java.util.List<String> suppliers, int page, String sortField, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, 10, Sort.by(sortDirection, sortField));

        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (categories != null && !categories.isEmpty()) {
                java.util.List<String> validCategories = categories.stream().filter(c -> c != null && !c.isBlank()).toList();
                if (!validCategories.isEmpty()) {
                    predicates.add(root.get("category").get("slug").in(validCategories));
                }
            }

            if (suppliers != null && !suppliers.isEmpty()) {
                java.util.List<String> validSuppliers = suppliers.stream().filter(s -> s != null && !s.isBlank()).toList();
                if (!validSuppliers.isEmpty()) {
                    predicates.add(root.get("supplier").get("slug").in(validSuppliers));
                }
            }

            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.like(
                        getTranslateExpr(cb, cb.lower(root.get("name"))),
                        getTranslateExpr(cb, cb.literal(searchPattern))
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        org.springframework.data.domain.Page<Product> productPage = productRepository.findAll(spec, pageable);
        return productPage.map(productMapper::entityToRes);
    }

    public Page<ProductRes> getProductGallery(String search, java.util.List<String> categories, java.util.List<String> suppliers, int page, int size, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, "name"));

        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (categories != null && !categories.isEmpty()) {
                java.util.List<String> validCategories = categories.stream().filter(c -> c != null && !c.isBlank()).toList();
                if (!validCategories.isEmpty()) {
                    predicates.add(root.get("category").get("slug").in(validCategories));
                }
            }

            if (suppliers != null && !suppliers.isEmpty()) {
                java.util.List<String> validSuppliers = suppliers.stream().filter(s -> s != null && !s.isBlank()).toList();
                if (!validSuppliers.isEmpty()) {
                    predicates.add(root.get("supplier").get("slug").in(validSuppliers));
                }
            }

            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(
                                getTranslateExpr(cb, cb.lower(root.get("name"))),
                                getTranslateExpr(cb, cb.literal(searchPattern))
                        ),
                        cb.like(
                                getTranslateExpr(cb, cb.lower(root.get("category").get("name"))),
                                getTranslateExpr(cb, cb.literal(searchPattern))
                        ),
                        cb.like(
                                getTranslateExpr(cb, cb.lower(root.get("supplier").get("name"))),
                                getTranslateExpr(cb, cb.literal(searchPattern))
                        )
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

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
        ProductCategory category = categoryRepository.findBySlug(productReq.getCategorySlug())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com slug: " + productReq.getCategorySlug()));

        Supplier supplier = supplierRepository.findBySlug(productReq.getSupplierSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado com slug: " + productReq.getSupplierSlug()));

        String generatedSlug = StringUtils.toSlug(productReq.getName() + " " + supplier.getName());
        if (this.productRepository.findBySlug(generatedSlug).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "Já existe um produto com um nome similar (Conflito de slug: " + generatedSlug + ")"
            );
        }

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

        Supplier supplier = product.getSupplier();
        if (!supplier.getSlug().equals(productReq.getSupplierSlug())) {
             supplier = supplierRepository.findBySlug(productReq.getSupplierSlug())
                 .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado com slug: " + productReq.getSupplierSlug()));
             product.setSupplier(supplier);
        }

        if (!product.getCategory().getSlug().equals(productReq.getCategorySlug())) {
             ProductCategory category = categoryRepository.findBySlug(productReq.getCategorySlug())
                 .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com slug: " + productReq.getCategorySlug()));
             product.setCategory(category);
        }

        String generatedSlug = StringUtils.toSlug(productReq.getName() + " " + supplier.getName());

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

    @Transactional
    public void migrateAllProductSlugs() {
        List<Product> products = this.productRepository.findAll();
        
        // Passo 1: Libera os slugs atuais para evitar colisão Unique durante o loop
        for (Product product : products) {
            product.setSlug(java.util.UUID.randomUUID().toString());
        }
        this.productRepository.saveAllAndFlush(products);

        // Passo 2: Aplica a nova regra final com desempate
        java.util.Set<String> usedSlugs = new java.util.HashSet<>();
        for (Product product : products) {
            String baseSlug = StringUtils.toSlug(product.getName() + " " + product.getSupplier().getName());
            String newSlug = baseSlug;
            int counter = 1;
            while (usedSlugs.contains(newSlug)) {
                newSlug = baseSlug + "-" + counter;
                counter++;
            }
            usedSlugs.add(newSlug);
            product.setSlug(newSlug);
        }
        this.productRepository.saveAllAndFlush(products);
    }

}
