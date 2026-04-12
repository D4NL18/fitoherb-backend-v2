package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.ProductReq;
import com.fitoherb.fitoherb_backend_v2.DTOs.Responses.ProductRes;
import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.services.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

@RestController
@RequestMapping("products")
public class ProductController {

    @Autowired
    ProductService productService;

    @PreAuthorize("@authorizationService.isAdmin()")
    @GetMapping("/{slug}")
    ResponseEntity<ProductRes> getProductBySlug(
            @PathVariable @Valid @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        ProductRes productRes = productService.getProductBySlug(slug);
        return ResponseEntity.ok(productRes);
    }

    @PreAuthorize("@authorizationService.isAuthenticated()")
    @GetMapping
    ResponseEntity<Page<ProductRes>> getAllProductsPaginated(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "name") String sortField,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Page<ProductRes> products = this.productService.getAllProductsPaginated(search, page, sortField, direction);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/gallery")
    public ResponseEntity<Page<ProductRes>> getProductGallery(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String supplier,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Page<ProductRes> products = productService.getProductGallery(search, category, supplier, page, direction);
        return ResponseEntity.ok(products);
    }


    @PreAuthorize("@authorizationService.isAdmin()")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Void> createProduct(
            @RequestPart(value = "product") @Valid ProductReq productReq,
            @RequestPart(value = "image") @Valid @NotNull(message = MSG_REQUIRED_FIELD) MultipartFile image
    ) {
        Product savedProduct = productService.createProduct(productReq, image);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedProduct.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @PutMapping(value = "/{slug}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Void> updateProductBySlug(
            @RequestPart(value = "product") @Valid ProductReq productReq,
            @RequestPart(value = "image") @Valid @NotNull(message = MSG_REQUIRED_FIELD) MultipartFile image,
            @PathVariable @Valid @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        productService.updateProductBySlug(productReq, image, slug);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @DeleteMapping("/{slug}")
    ResponseEntity<Void> deleteProductBySlug(
            @PathVariable @Valid @NotBlank(message = MSG_REQUIRED_FIELD) @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        productService.deleteProductBySlug(slug);
        return ResponseEntity.ok().build();
    }
}
