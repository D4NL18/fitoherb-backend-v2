package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.ProductCategoryReq;
import com.fitoherb.fitoherb_backend_v2.DTOs.Responses.ProductCategoryRes;
import com.fitoherb.fitoherb_backend_v2.entities.ProductCategory;
import com.fitoherb.fitoherb_backend_v2.services.ProductCategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("product_categories")
public class ProductCategoryController {

    @Autowired
    ProductCategoryService productCategoryService;

    @GetMapping("/get-all")
    ResponseEntity<List<ProductCategoryRes>> getAllProductCategories() {
        List<ProductCategoryRes> productsList = this.productCategoryService.getAllProductCategories();
        return ResponseEntity.ok(productsList);
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @GetMapping("/{slug}")
    ResponseEntity<ProductCategoryRes> getProductCategoryBySlug(
            @PathVariable @Valid @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        ProductCategoryRes categoryRes = this.productCategoryService.getProductCategoryBySlug(slug);
        return ResponseEntity.ok(categoryRes);
    }

    @PreAuthorize("@authorizationService.isAuthenticated()")
    @GetMapping
    ResponseEntity<Page<ProductCategoryRes>> getAllProductCategoriesPaginated(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "name") String sortField,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Page<ProductCategoryRes> categories = this.productCategoryService.getAllProductCategoriesPaginated(search, page, sortField, direction);
        return ResponseEntity.ok(categories);
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @PostMapping
    ResponseEntity<Void> createProductCategory(
            @RequestBody @Valid ProductCategoryReq categoryReq
    ) {
        ProductCategory savedProductCategory = productCategoryService.createProductCategory(categoryReq);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedProductCategory.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @PutMapping("/{slug}")
    ResponseEntity<Void> updateProductCategoryBySlug(
            @RequestBody @Valid ProductCategoryReq categoryReq,
            @PathVariable @Valid @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        productCategoryService.updateProductCategoryBySlug(categoryReq, slug);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @DeleteMapping("/{slug}")
    ResponseEntity<Void> deleteProductCategoryBySlug(
            @PathVariable @Valid @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        productCategoryService.deleteProductCategoryBySlug(slug);
        return ResponseEntity.ok().build();
    }
}
