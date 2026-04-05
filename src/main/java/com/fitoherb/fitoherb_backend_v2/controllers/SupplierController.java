package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.SupplierReq;
import com.fitoherb.fitoherb_backend_v2.DTOs.Responses.SupplierRes;
import com.fitoherb.fitoherb_backend_v2.entities.Supplier;
import com.fitoherb.fitoherb_backend_v2.services.SupplierService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import java.util.List;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.MSG_SLUG_INVALID;
import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.SLUG_REGEX;

@RestController
@RequestMapping("suppliers")
public class SupplierController {
    @Autowired
    SupplierService supplierService;

    @GetMapping("/get-all")
    ResponseEntity<List<SupplierRes>> getAllSuppliers() {
        List<SupplierRes> suppliersList = this.supplierService.getAllSuppliers();
        return ResponseEntity.ok(suppliersList);
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @GetMapping("/{slug}")
    ResponseEntity<SupplierRes> getSupplierBySlug(
            @PathVariable @Valid @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        SupplierRes supplierRes = this.supplierService.getSupplierBySlug(slug);
        return ResponseEntity.ok(supplierRes);
    }

    @PreAuthorize("@authorizationService.isAuthenticated()")
    @GetMapping
    ResponseEntity<Page<SupplierRes>> getAllSuppliersPaginated(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "name") String sortField,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Page<SupplierRes> suppliers = this.supplierService.getAllSuppliersPaginated(search, page, sortField, direction);
        return ResponseEntity.ok(suppliers);
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Void> createSupplier(
            @RequestPart("supplier") @Valid SupplierReq supplierReq,
            @RequestPart(value = "image", required = true) MultipartFile image
    ) {
        Supplier savedSupplier = supplierService.createSupplier(supplierReq, image);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedSupplier.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @PutMapping(value = "/{slug}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Void> updateSupplierBySlug(
            @RequestPart("supplier") @Valid SupplierReq supplierReq,
            @PathVariable @Valid @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug,
            @RequestPart(value = "image", required = true) MultipartFile image
    ) {
        supplierService.updateSupplierBySlug(supplierReq, slug, image);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @DeleteMapping("/{slug}")
    ResponseEntity<Void> deleteSupplierBySlug(
            @PathVariable @Valid @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        supplierService.deleteSupplierBySlug(slug);
        return ResponseEntity.ok().build();
    }
}
