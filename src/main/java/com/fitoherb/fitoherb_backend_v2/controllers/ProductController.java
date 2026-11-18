package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductAdminRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductRes;
import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.enums.ErpMappingStatus;
import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestErrorMessage;
import com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestValidationErrorMessage;
import com.fitoherb.fitoherb_backend_v2.services.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/products")
@Tag(name = "Products", description = "Comprehensive management of the product catalog. " +
        "Handles the complete product lifecycle, including administrative CRUD operations, image assets, " +
        "and a public-facing gallery with advanced filtering.")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get product details by slug", description = "Retrieves full details of a specific product using its unique slug. Restricted to administrators.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product details retrieved",
                    content = @Content(schema = @Schema(implementation = ProductAdminRes.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Product Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"Product not found with slug: chamomile-tea\"}"))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Forbidden", value = "{\"status\": \"FORBIDDEN\", \"message\": \"Access denied: You do not have the necessary permissions to access this resource.\"}")))
    })
    @PreAuthorize("@authorizationService.isAdminOrUser()")
    @GetMapping("/{slug}")
    public ResponseEntity<ProductAdminRes> getProductBySlug(
            @PathVariable @Valid @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        ProductAdminRes productRes = productService.getProductBySlug(slug);
        return ResponseEntity.ok(productRes);
    }

    @Operation(summary = "List products for administration", description = "Returns a paginated list of products for internal management. Requires authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\": \"UNAUTHORIZED\", \"message\": \"Token JWT invalid or expired\"}")))
    })
    @PreAuthorize("@authorizationService.isAdminOrUser()")
    @GetMapping({"", "/admin"})
    public ResponseEntity<Page<ProductAdminRes>> getAllProductsPaginated(
            @Parameter(description = "Global search term for product name or ERP code", example = "131477")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by category slugs", example = "essential-oils,herbs")
            @RequestParam(required = false) List<String> category,

            @Parameter(description = "Filter by supplier slugs", example = "nature-labs,vita-supplements")
            @RequestParam(required = false) List<String> supplier,

            @Parameter(description = "Filter by ERP mapping status")
            @RequestParam(required = false) List<ErpMappingStatus> erpStatus,

            @Parameter(description = "Filter by active visibility")
            @RequestParam(required = false) Boolean active,

            @Parameter(description = "Filter by curation status")
            @RequestParam(required = false) ProductCurationStatus curationStatus,

            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Field to sort by", schema = @Schema(allowableValues = {"name", "createdAt", "category.name", "supplier.name"}))
            @RequestParam(defaultValue = "name") String sortField,

            @Parameter(description = "Sort direction", schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Page<ProductAdminRes> products = this.productService.getAllProductsPaginated(
                search, category, supplier, erpStatus, active, curationStatus, page, sortField, direction
        );
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Fetch products for public gallery", description = "Optimized endpoint for the public-facing storefront. Supports filtering by name, category, and supplier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Gallery data retrieved"),
            @ApiResponse(responseCode = "404", description = "Category or Supplier not found",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = {
                                    @ExampleObject(name = "Category Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"Category not found with slug: essential-oils\"}"),
                                    @ExampleObject(name = "Supplier Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"Supplier not found with slug: nature-labs\"}")
                            }))
    })
    @GetMapping("/gallery")
    public ResponseEntity<Page<ProductRes>> getProductGallery(
            @Parameter(description = "Search by product name", example = "Tea")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by category slugs", example = "essential-oils,herbs")
            @RequestParam(required = false) List<String> category,

            @Parameter(description = "Filter by supplier slugs", example = "nature-labs,vita-supplements")
            @RequestParam(required = false) List<String> supplier,

            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page", example = "15")
            @RequestParam(defaultValue = "15") int size,

            @Parameter(description = "Sort direction", schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Page<ProductRes> products = productService.getProductGallery(search, category, supplier, page, size, direction);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Create a new product", description = "Registers a product with its metadata and a required image file (unless PENDING_IMAGE). Automatically generates a unique SEO slug.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = RestValidationErrorMessage.class),
                            examples = @ExampleObject(name = "Validation Error", value = "{\"status\": \"BAD_REQUEST\", \"message\": \"Validation failed for one or more fields\", \"errors\": {\"name\": \"must not be blank\", \"categorySlug\": \"must not be null\"}}"))),
            @ApiResponse(responseCode = "409", description = "Conflict: Name, slug or ERP code already exists",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity: ERP validation or topology rule failed",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Category or Supplier not found",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class)))
    })
    @PreAuthorize("@authorizationService.isAdminOrUser()")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createProduct(
            @RequestBody(content = @Content(encoding = @Encoding(name = "product", contentType = MediaType.APPLICATION_JSON_VALUE)))
            @RequestPart(value = "product") @Valid ProductReq productReq,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        Product savedProduct = productService.createProduct(productReq, image);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedProduct.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "Update product by slug", description = "Updates product information and replaces the image if a new one is provided.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "404", description = "Resource not found",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class))),
            @ApiResponse(responseCode = "409", description = "Conflict: New slug or ERP code already in use",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity: ERP validation or topology rule failed",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Internal error during update",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class)))
    })
    @PreAuthorize("@authorizationService.isAdminOrUser()")
    @PutMapping(value = "/{slug}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateProductBySlug(
            @RequestBody(content = @Content(encoding = @Encoding(name = "product", contentType = MediaType.APPLICATION_JSON_VALUE)))
            @RequestPart(value = "product") @Valid ProductReq productReq,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @PathVariable @Valid @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        productService.updateProductBySlug(productReq, image, slug);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete product by slug", description = "Permanently removes the product from the database and storage.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Database error during deletion",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class)))
    })
    @PreAuthorize("@authorizationService.isAdminOrUser()")
    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> deleteProductBySlug(
            @PathVariable @Valid @NotBlank(message = MSG_REQUIRED_FIELD) @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        productService.deleteProductBySlug(slug);
        return ResponseEntity.ok().build();
    }
}
