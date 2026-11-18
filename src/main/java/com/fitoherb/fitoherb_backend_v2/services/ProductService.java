package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductReq;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductSkuReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ErpCatalogItemSnapshot;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductAdminRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductRes;
import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.entities.ProductCategory;
import com.fitoherb.fitoherb_backend_v2.entities.ProductSku;
import com.fitoherb.fitoherb_backend_v2.entities.Supplier;
import com.fitoherb.fitoherb_backend_v2.enums.ErpMappingStatus;
import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.mappers.ProductMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductCategoryRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductSkuRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.SupplierRepository;
import com.fitoherb.fitoherb_backend_v2.utils.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductRepository productRepository;
    private final ProductSkuRepository productSkuRepository;
    private final SupplierRepository supplierRepository;
    private final ProductCategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    private final ErpCatalogValidationService erpCatalogValidationService;
    private final ProductTopologyValidator topologyValidator;
    private final ProductVariantSanitizer variantSanitizer;
    private final ProductVisibilityCalculator visibilityCalculator;
    private final EntityManager entityManager;

    private static final String PRODUCT_NOT_FOUND_MSG = "Produto não encontrado com slug: ";
    private static final String ACCENTED_CHARS = "áàâãäéèêëíìîïóòôõöúùûüçñ";
    private static final String UNACCENTED_CHARS = "aaaaaeeeeiiiiooooouuuucn";

    private static final Set<String> STOP_WORDS = Set.of(
            "quero", "queria", "gostaria", "ver", "lista", "listar", "opcoes", "opcao",
            "opções", "opção", "tem", "têm", "temos", "mostrar", "mostra", "me",
            "de", "do", "da", "dos", "das", "em", "no", "na", "nas",
            "a", "o", "as", "os", "um", "uma", "uns", "umas",
            "para", "pra", "pro", "pras", "pros", "por", "com", "sem",
            "produtos", "produto", "marca", "marcas", "qual", "quais", "que", "onde", "acho"
    );

    private List<String> extractMeaningfulTokens(String search) {
        if (search == null || search.isBlank()) return Collections.emptyList();
        String[] rawTokens = search.trim().split("\\s+");
        List<String> meaningful = new ArrayList<>();
        for (String raw : rawTokens) {
            String clean = raw.trim().toLowerCase();
            String norm = clean.replaceAll("[^a-z0-9%]", "");
            if (norm.isBlank()) continue;
            if (!STOP_WORDS.contains(norm) && !STOP_WORDS.contains(clean)) {
                meaningful.add(raw);
            }
        }
        if (meaningful.isEmpty()) {
            for (String raw : rawTokens) {
                if (!raw.isBlank()) meaningful.add(raw);
            }
        }
        return meaningful;
    }

    private jakarta.persistence.criteria.Expression<String> getTranslateExpr(
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Expression<String> expression
    ) {
        return cb.function("translate", String.class, expression, cb.literal(ACCENTED_CHARS), cb.literal(UNACCENTED_CHARS));
    }

    public Page<ProductAdminRes> getAllProductsPaginated(
            String search,
            List<String> categories,
            List<String> suppliers,
            List<ErpMappingStatus> erpStatuses,
            Boolean active,
            ProductCurationStatus curationStatus,
            int page,
            String sortField,
            String direction
    ) {
        Sort.Direction sortDirection = direction != null && direction.equalsIgnoreCase("DESC")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, 10);
        Specification<Product> spec = buildProductSpecification(
                search, categories, suppliers, erpStatuses, active, curationStatus, sortField, sortDirection, false, true
        );

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        return productPage.map(productMapper::entityToAdminRes);
    }

    public Page<ProductRes> getProductGallery(
            String search,
            List<String> categories,
            List<String> suppliers,
            int page,
            int size,
            String direction
    ) {
        Sort.Direction sortDirection = direction != null && direction.equalsIgnoreCase("DESC")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size);
        Specification<Product> spec = buildProductSpecification(
                search, categories, suppliers, null, true, null, "name", sortDirection, true, false
        );

        return productRepository.findAll(spec, pageable)
                .map(productMapper::entityToPublicRes);
    }

    private Specification<Product> buildProductSpecification(
            String search,
            List<String> categories,
            List<String> suppliers,
            List<ErpMappingStatus> erpStatuses,
            Boolean activeFilter,
            ProductCurationStatus curationStatusFilter,
            String sortField,
            Sort.Direction sortDirection,
            boolean includeCategoryAndSupplierSearch,
            boolean isAdminSearch
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (activeFilter != null) {
                predicates.add(activeFilter ? cb.isTrue(root.get("active")) : cb.isFalse(root.get("active")));
            }

            if (curationStatusFilter != null) {
                predicates.add(cb.equal(root.get("curationStatus"), curationStatusFilter));
            }

            if (categories != null && !categories.isEmpty()) {
                List<String> validCategories = categories.stream().filter(c -> c != null && !c.isBlank()).toList();
                if (!validCategories.isEmpty()) {
                    predicates.add(root.get("category").get("slug").in(validCategories));
                }
            }

            if (suppliers != null && !suppliers.isEmpty()) {
                List<String> validSuppliers = suppliers.stream().filter(s -> s != null && !s.isBlank()).toList();
                if (!validSuppliers.isEmpty()) {
                    predicates.add(root.get("supplier").get("slug").in(validSuppliers));
                }
            }

            if (isAdminSearch && erpStatuses != null && !erpStatuses.isEmpty() && query != null) {
                List<Predicate> statusPredicates = new ArrayList<>();
                for (ErpMappingStatus status : erpStatuses) {
                    if (status == null) continue;
                    if (status == ErpMappingStatus.UNMAPPED) {
                        Subquery<Long> sq = query.subquery(Long.class);
                        Root<ProductSku> skuRoot = sq.from(ProductSku.class);
                        sq.select(cb.literal(1L)).where(cb.equal(skuRoot.get("product"), root));
                        statusPredicates.add(cb.not(cb.exists(sq)));
                    } else if (status == ErpMappingStatus.INACTIVE_ALERT) {
                        Subquery<Long> sq = query.subquery(Long.class);
                        Root<ProductSku> skuRoot = sq.from(ProductSku.class);
                        sq.select(cb.literal(1L)).where(
                                cb.equal(skuRoot.get("product"), root),
                                cb.isTrue(skuRoot.get("erpInactive"))
                        );
                        statusPredicates.add(cb.exists(sq));
                    } else if (status == ErpMappingStatus.COMPLETE) {
                        Subquery<Long> sqExists = query.subquery(Long.class);
                        Root<ProductSku> skuExistsRoot = sqExists.from(ProductSku.class);
                        sqExists.select(cb.literal(1L)).where(cb.equal(skuExistsRoot.get("product"), root));

                        Subquery<Long> sqInactive = query.subquery(Long.class);
                        Root<ProductSku> skuInactiveRoot = sqInactive.from(ProductSku.class);
                        sqInactive.select(cb.literal(1L)).where(
                                cb.equal(skuInactiveRoot.get("product"), root),
                                cb.isTrue(skuInactiveRoot.get("erpInactive"))
                        );
                        statusPredicates.add(cb.and(cb.exists(sqExists), cb.not(cb.exists(sqInactive))));
                    }
                }
                if (!statusPredicates.isEmpty()) {
                    predicates.add(cb.or(statusPredicates.toArray(new Predicate[0])));
                }
            }

            if (search != null && !search.isBlank()) {
                List<String> tokens = extractMeaningfulTokens(search);
                for (String token : tokens) {
                    if (token.isBlank()) continue;
                    String cleanToken = token.replace("%", "").trim();
                    if (cleanToken.isBlank()) cleanToken = token.trim();
                    String pattern = "%" + cleanToken.toLowerCase() + "%";

                    List<Predicate> orPredicates = new ArrayList<>();
                    orPredicates.add(cb.like(
                            getTranslateExpr(cb, cb.lower(root.get("name"))),
                            getTranslateExpr(cb, cb.literal(pattern))
                    ));

                    if (includeCategoryAndSupplierSearch) {
                        orPredicates.add(cb.like(
                                getTranslateExpr(cb, cb.lower(root.get("category").get("name"))),
                                getTranslateExpr(cb, cb.literal(pattern))
                        ));
                        orPredicates.add(cb.like(
                                getTranslateExpr(cb, cb.lower(root.get("supplier").get("name"))),
                                getTranslateExpr(cb, cb.literal(pattern))
                        ));
                    }

                    orPredicates.add(cb.like(
                            getTranslateExpr(cb, cb.lower(cb.coalesce(root.get("description"), ""))),
                            getTranslateExpr(cb, cb.literal(pattern))
                    ));
                    orPredicates.add(cb.like(
                            getTranslateExpr(cb, cb.lower(cb.function("array_to_string", String.class, root.get("flavours"), cb.literal(" ")))),
                            getTranslateExpr(cb, cb.literal(pattern))
                    ));
                    orPredicates.add(cb.like(
                            getTranslateExpr(cb, cb.lower(cb.function("array_to_string", String.class, root.get("presentation"), cb.literal(" ")))),
                            getTranslateExpr(cb, cb.literal(pattern))
                    ));

                    if (isAdminSearch && query != null && cleanToken.matches("\\d+")) {
                        Subquery<Long> erpSq = query.subquery(Long.class);
                        Root<ProductSku> skuRoot = erpSq.from(ProductSku.class);
                        erpSq.select(cb.literal(1L)).where(
                                cb.equal(skuRoot.get("product"), root),
                                cb.like(skuRoot.get("erpCode").as(String.class), "%" + cleanToken + "%")
                        );
                        orPredicates.add(cb.exists(erpSq));
                    }

                    predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
                }

                applyOrdering(query, cb, root, tokens, sortField, sortDirection);
            } else {
                applyOrdering(query, cb, root, null, sortField, sortDirection);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void applyOrdering(
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Root<Product> root,
            List<String> tokens,
            String sortField,
            Sort.Direction sortDirection
    ) {
        if (query == null || Long.class == query.getResultType() || long.class == query.getResultType()) {
            return;
        }

        String safeSortField = (sortField != null && !sortField.isBlank()) ? sortField : "name";
        jakarta.persistence.criteria.Path<?> sortPath = resolveSortPath(root, safeSortField);
        jakarta.persistence.criteria.Order fieldOrder = sortDirection.isAscending()
                ? cb.asc(sortPath)
                : cb.desc(sortPath);

        if (tokens != null && !tokens.isEmpty()) {
            String meaningfulPhrase = String.join(" ", tokens);
            String exactPattern = "%" + meaningfulPhrase.trim().toLowerCase() + "%";
            jakarta.persistence.criteria.Expression<Integer> priorityCase = cb.<Integer>selectCase()
                    .when(cb.like(getTranslateExpr(cb, cb.lower(root.get("name"))), getTranslateExpr(cb, cb.literal(exactPattern))), 0)
                    .otherwise(1);

            jakarta.persistence.criteria.Order priorityOrder = cb.asc(priorityCase);
            query.orderBy(priorityOrder, fieldOrder);
        } else {
            query.orderBy(fieldOrder);
        }
    }

    private jakarta.persistence.criteria.Path<?> resolveSortPath(Root<Product> root, String sortField) {
        if (sortField.contains(".")) {
            String[] parts = sortField.split("\\.");
            jakarta.persistence.criteria.Path<?> path = root;
            for (String part : parts) {
                path = path.get(part);
            }
            return path;
        }
        return root.get(sortField);
    }

    public ProductAdminRes getProductBySlug(String slug) {
        Product product = this.productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MSG + slug));
        return productMapper.entityToAdminRes(product);
    }

    @Transactional
    public Product createProduct(ProductReq productReq, MultipartFile image) {
        ProductCurationStatus targetCuration = productReq.getCurationStatus() != null
                ? productReq.getCurationStatus()
                : ProductCurationStatus.COMPLETE;

        boolean hasImage = image != null && !image.isEmpty();
        if (!hasImage && targetCuration != ProductCurationStatus.PENDING_IMAGE) {
            throw new IllegalArgumentException("A imagem do produto é obrigatória.");
        }

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

        Map<Long, ErpCatalogItemSnapshot> validatedSnapshots = erpCatalogValidationService.validateForProduct(productReq, null);

        String fileName = null;
        if (hasImage) {
            fileName = fileStorageService.storeProductImage(image);
        }

        Product product = productMapper.reqToEntity(productReq);
        if (product == null) {
            product = new Product();
        }
        product.setName(productReq.getName());
        product.setDescription(productReq.getDescription());
        product.setSlug(generatedSlug);
        product.setImagePath(fileName);
        product.setCategory(category);
        product.setSupplier(supplier);
        product.setActive(true);
        product.setCurationStatus(hasImage ? ProductCurationStatus.COMPLETE : targetCuration);

        applySkusAndProjection(product, productReq, validatedSnapshots);

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

        Map<Long, ErpCatalogItemSnapshot> validatedSnapshots =
                erpCatalogValidationService.validateForProduct(productReq, product.getId());

        if (image != null && !image.isEmpty()) {
            if (product.getImagePath() != null && !product.getImagePath().isBlank()) {
                fileStorageService.deleteProductImage(product.getImagePath());
            }
            String newFileName = fileStorageService.storeProductImage(image);
            product.setImagePath(newFileName);
            if (product.getCurationStatus() == ProductCurationStatus.PENDING_IMAGE) {
                product.setCurationStatus(ProductCurationStatus.COMPLETE);
            }
        }

        productMapper.updateEntityFromReq(productReq, product);
        product.setName(productReq.getName());
        product.setDescription(productReq.getDescription());
        product.setSlug(generatedSlug);

        if (product.getSkus() != null && !product.getSkus().isEmpty()) {
            product.getSkus().clear();
            entityManager.flush();
        }

        applySkusAndProjection(product, productReq, validatedSnapshots);

        productRepository.save(product);
    }

    private void applySkusAndProjection(
            Product product,
            ProductReq productReq,
            Map<Long, ErpCatalogItemSnapshot> snapshots
    ) {
        List<ProductSkuReq> skuReqs = productReq.getSkus();
        if (skuReqs == null || skuReqs.isEmpty()) {
            return;
        }

        Map<Long, ErpCatalogItemSnapshot> safeSnapshots = snapshots != null ? snapshots : Collections.emptyMap();
        List<ProductSku> newSkus = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < skuReqs.size(); i++) {
            ProductSkuReq skuReq = skuReqs.get(i);
            ErpCatalogItemSnapshot snap = safeSnapshots.get(skuReq.getErpCode());
            boolean isInactive = snap != null && snap.isInativoPro();
            String variantKey = variantSanitizer.buildNormalizedVariantKey(skuReq.getFlavour(), skuReq.getPresentation());
            if (variantKey == null || variantKey.isBlank()) {
                variantKey = "__SIMPLE__";
            }

            ProductSku sku = new ProductSku();
            sku.setProduct(product);
            sku.setErpCode(skuReq.getErpCode());
            sku.setFlavour(skuReq.getFlavour());
            sku.setPresentation(skuReq.getPresentation());
            sku.setNormalizedVariantKey(variantKey);
            sku.setEan(snap != null ? snap.getEan() : null);
            sku.setSortOrder(i);
            sku.setErpInactive(isInactive);
            sku.setErpInactiveConfirmed(isInactive && Boolean.TRUE.equals(productReq.getAllowInactiveErp()));
            sku.setErpLastVerifiedAt(now);
            newSkus.add(sku);
        }

        if (product.getSkus() == null) {
            product.setSkus(new ArrayList<>(newSkus));
        } else {
            product.getSkus().clear();
            product.getSkus().addAll(newSkus);
        }

        List<String> activeFlavours = variantSanitizer.projectActiveFlavours(newSkus);
        List<String> activePresentations = variantSanitizer.projectActivePresentations(newSkus);
        if (activeFlavours != null) {
            product.setFlavours(activeFlavours);
        }
        if (activePresentations != null) {
            product.setPresentation(activePresentations);
        }

        visibilityCalculator.recalculate(product);
    }

    @Transactional
    public void deleteProductBySlug(String slug) {
        Product product = this.productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MSG + slug));
        try {
            fileStorageService.deleteProductImage(product.getImagePath());
            this.productRepository.delete(product);
        } catch (Exception e) {
            throw new DatabaseOperationException("Falha ao excluir produto. Verifique se não há registros vinculados a este cadastro.", e);
        }
    }

    @Transactional
    public void migrateAllProductSlugs() {
        List<Product> products = this.productRepository.findAll();

        for (Product product : products) {
            product.setSlug(UUID.randomUUID().toString());
        }
        this.productRepository.saveAllAndFlush(products);

        Set<String> usedSlugs = new HashSet<>();
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
