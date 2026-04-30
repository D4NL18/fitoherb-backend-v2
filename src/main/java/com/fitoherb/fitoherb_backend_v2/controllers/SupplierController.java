package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.SupplierReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.SupplierRes;
import com.fitoherb.fitoherb_backend_v2.entities.Supplier;
import com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestErrorMessage;
import com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestValidationErrorMessage;
import com.fitoherb.fitoherb_backend_v2.services.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("suppliers")
@Tag(name = "Suppliers", description = "Management of product manufacturers and vendors. " +
        "Handles the lifecycle of business partners, including branding assets and SEO-optimized identification.")
public class SupplierController {

    private final SupplierService supplierService;

    @Operation(summary = "List all suppliers", description = "Retrieves a non-paginated list of all suppliers. Recommended for populating dropdowns and selection menus.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of suppliers retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SupplierRes.class))))
    })
    @GetMapping("/get-all")
    ResponseEntity<List<SupplierRes>> getAllSuppliers() {
        List<SupplierRes> suppliersList = this.supplierService.getAllSuppliers();
        return ResponseEntity.ok(suppliersList);
    }

    @Operation(summary = "Get supplier details by slug", description = "Returns the full profile of a supplier based on their unique slug. Access is restricted to administrators.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Supplier found",
                    content = @Content(schema = @Schema(implementation = SupplierRes.class))),
            @ApiResponse(responseCode = "404", description = "Supplier not found",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"Supplier not found with slug: nature-labs\"}"))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Forbidden", value = "{\"status\": \"FORBIDDEN\", \"message\": \"Access denied: You do not have the necessary permissions to access this resource.\"}")))
    })
    @PreAuthorize("@authorizationService.isAdmin()")
    @GetMapping("/{slug}")
    ResponseEntity<SupplierRes> getSupplierBySlug(
            @PathVariable @Valid @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        SupplierRes supplierRes = this.supplierService.getSupplierBySlug(slug);
        return ResponseEntity.ok(supplierRes);
    }

    @Operation(summary = "List suppliers with pagination", description = "Fetches a paginated list of suppliers. Supports query-based search and custom sorting criteria.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\": \"UNAUTHORIZED\", \"message\": \"Token JWT invalid or expired\"}")))
    })
    @PreAuthorize("@authorizationService.isAuthenticated()")
    @GetMapping
    ResponseEntity<Page<SupplierRes>> getAllSuppliersPaginated(
            @Parameter(description = "Search term for filtering by supplier name", example = "Nature")
            @RequestParam(required = false) String search,

            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Field to sort by", schema = @Schema(allowableValues = {"name", "createdAt"}))
            @RequestParam(defaultValue = "name") String sortField,

            @Parameter(description = "Sort direction", schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Page<SupplierRes> suppliers = this.supplierService.getAllSuppliersPaginated(search, page, sortField, direction);
        return ResponseEntity.ok(suppliers);
    }

    @Operation(summary = "Create new supplier", description = "Registers a new supplier in the system. Requires a multipart request containing the 'supplier' JSON data and a branding 'image' file.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Supplier created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = RestValidationErrorMessage.class),
                            examples = @ExampleObject(name = "Validation Error", value = "{\"status\": \"BAD_REQUEST\", \"message\": \"Validation failed for one or more fields\", \"errors\": {\"name\": \"must not be blank\"}}"))),
            @ApiResponse(responseCode = "409", description = "Conflict: Name or slug already exists",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = {
                                    @ExampleObject(name = "Name Conflict", value = "{\"status\": \"CONFLICT\", \"message\": \"Supplier with that name already exists\"}"),
                                    @ExampleObject(name = "Slug Conflict", value = "{\"status\": \"CONFLICT\", \"message\": \"A supplier with a similar name already exists (Slug conflict: nature-labs)\"}")
                            })),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Forbidden", value = "{\"status\": \"FORBIDDEN\", \"message\": \"Access denied: You do not have the necessary permissions to access this resource.\"}")))
    })
    @PreAuthorize("@authorizationService.isAdmin()")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Void> createSupplier(
            @RequestBody(content = @Content(encoding = @Encoding(name = "supplier", contentType = MediaType.APPLICATION_JSON_VALUE)))
            @RequestPart("supplier") @Valid SupplierReq supplierReq,
            @RequestPart(value = "image", required = true) @Valid @NotNull(message = MSG_REQUIRED_FIELD) MultipartFile image
    ) {
        Supplier savedSupplier = supplierService.createSupplier(supplierReq, image);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedSupplier.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "Update supplier by slug", description = "Updates an existing supplier's information and branding asset. Validates for slug conflicts if the name is modified.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Supplier updated successfully"),
            @ApiResponse(responseCode = "404", description = "Supplier not found",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"Supplier not found with slug: nature-labs\"}"))),
            @ApiResponse(responseCode = "409", description = "Conflict: New name generates an existing slug",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Slug Conflict", value = "{\"status\": \"CONFLICT\", \"message\": \"A supplier with a similar name already exists (Slug conflict: nature-labs)\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal error during update",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Database Error", value = "{\"status\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Failed to update supplier in database.\"}")))
    })
    @PreAuthorize("@authorizationService.isAdmin()")
    @PutMapping(value = "/{slug}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Void> updateSupplierBySlug(
            @RequestBody(content = @Content(encoding = @Encoding(name = "supplier", contentType = MediaType.APPLICATION_JSON_VALUE)))
            @RequestPart("supplier") @Valid SupplierReq supplierReq,
            @PathVariable @Valid @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        supplierService.updateSupplierBySlug(supplierReq, slug, image);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete supplier by slug", description = "Permanently removes a supplier record and its associated image from the storage. Fails if products are still linked to this provider.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Supplier deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Supplier not found",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"Supplier not found with slug: nature-labs\"}"))),
            @ApiResponse(responseCode = "500", description = "Database error: Supplier may have linked products",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Integrity Constraint", value = "{\"status\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Failed to delete supplier. Ensure there are no records linked to this account.\"}")))
    })
    @PreAuthorize("@authorizationService.isAdmin()")
    @DeleteMapping("/{slug}")
    ResponseEntity<Void> deleteSupplierBySlug(
            @PathVariable @Valid @NotBlank(message = MSG_REQUIRED_FIELD) @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        supplierService.deleteSupplierBySlug(slug);
        return ResponseEntity.ok().build();
    }
}