package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductCategoryReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductCategoryRes;
import com.fitoherb.fitoherb_backend_v2.entities.ProductCategory;
import com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestErrorMessage;
import com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestValidationErrorMessage;
import com.fitoherb.fitoherb_backend_v2.services.ProductCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("product_categories")
@Tag(name = "Product Categories", description = "Management of product classifications. " +
        "Provides operations to organize the catalog into logical groups, including image assets and SEO-friendly slug management.")
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @Operation(summary = "List all categories", description = "Retrieves a complete list of all product categories without pagination. Ideal for small dropdowns or menus.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductCategoryRes.class))))
    })
    @GetMapping("/get-all")
    ResponseEntity<List<ProductCategoryRes>> getAllProductCategories() {
        List<ProductCategoryRes> productsList = this.productCategoryService.getAllProductCategories();
        return ResponseEntity.ok(productsList);
    }

    @Operation(summary = "Get category by slug", description = "Retrieves detailed information of a specific category using its unique SEO slug. Restricted to administrators.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category found",
                    content = @Content(schema = @Schema(implementation = ProductCategoryRes.class))),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"Category not found with slug: teas\"}"))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Forbidden", value = "{\"status\": \"FORBIDDEN\", \"message\": \"Access denied: You do not have the necessary permissions to access this resource.\"}")))
    })
    @PreAuthorize("@authorizationService.isAdmin()")
    @GetMapping("/{slug}")
    ResponseEntity<ProductCategoryRes> getProductCategoryBySlug(
            @PathVariable @Valid @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        ProductCategoryRes categoryRes = this.productCategoryService.getProductCategoryBySlug(slug);
        return ResponseEntity.ok(categoryRes);
    }

    @Operation(summary = "List categories with pagination", description = "Returns a paginated collection of categories. Supports global search and dynamic sorting parameters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\": \"UNAUTHORIZED\", \"message\": \"Token JWT invalid or expired\"}")))
    })
    @PreAuthorize("@authorizationService.isAuthenticated()")
    @GetMapping
    ResponseEntity<Page<ProductCategoryRes>> getAllProductCategoriesPaginated(
            @Parameter(description = "Search term for filtering by name", example = "Tea")
            @RequestParam(required = false) String search,

            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Field to sort by", schema = @Schema(allowableValues = {"name", "createdAt"}))
            @RequestParam(defaultValue = "name") String sortField,

            @Parameter(description = "Sort direction", schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Page<ProductCategoryRes> categories = this.productCategoryService.getAllProductCategoriesPaginated(search, page, sortField, direction);
        return ResponseEntity.ok(categories);
    }

    @Operation(summary = "Create new category", description = "Registers a new category in the system. Requires a JSON part for metadata and a Multipart file for the image asset.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = RestValidationErrorMessage.class),
                            examples = @ExampleObject(name = "Validation Error", value = "{\"status\": \"BAD_REQUEST\", \"message\": \"Validation failed for one or more fields\", \"errors\": {\"name\": \"must not be blank\"}}"))),
            @ApiResponse(responseCode = "409", description = "Conflict: Name or slug already exists",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = {
                                    @ExampleObject(name = "Name Conflict", value = "{\"status\": \"CONFLICT\", \"message\": \"Category with that name already exists\"}"),
                                    @ExampleObject(name = "Slug Conflict", value = "{\"status\": \"CONFLICT\", \"message\": \"A category with a similar name already exists (Slug conflict: teas)\"}")
                            })),
            @ApiResponse(responseCode = "500", description = "Internal error",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Database Error", value = "{\"status\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Failed to create category.\" }")))
    })
    @PreAuthorize("@authorizationService.isAdmin()")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Void> createProductCategory(
            @RequestPart("product_category") @Valid ProductCategoryReq categoryReq,
            @RequestPart(value = "image", required = true)@Valid @NotNull(message = MSG_REQUIRED_FIELD) MultipartFile image

    ) {
        ProductCategory savedProductCategory = productCategoryService.createProductCategory(categoryReq, image);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedProductCategory.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "Update category by slug", description = "Updates an existing category's data and image. Performs slug conflict validation if the name is changed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category updated successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"Category not found with slug: teas\"}"))),
            @ApiResponse(responseCode = "409", description = "Conflict: New slug already in use",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Slug Conflict", value = "{\"status\": \"CONFLICT\", \"message\": \"A category with a similar name already exists (Slug conflict: new-slug)\"}"))),
            @ApiResponse(responseCode = "500", description = "Failed to update category",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Database Error", value = "{\"status\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Failed to update category in database.\"}")))
    })
    @PreAuthorize("@authorizationService.isAdmin()")
    @PutMapping(value = "/{slug}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Void> updateProductCategoryBySlug(
            @RequestPart("product_category") @Valid ProductCategoryReq categoryReq,
            @RequestPart(value = "image", required = true) MultipartFile image,
            @PathVariable @Valid @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        productCategoryService.updateProductCategoryBySlug(categoryReq, slug, image);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete category by slug", description = "Permanently removes a category and its associated image from storage. Fails if there are linked products.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"Category not found with slug: teas\"}"))),
            @ApiResponse(responseCode = "500", description = "Database error: Category might have linked products",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Integrity Constraint", value = "{\"status\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Failed to delete category. Ensure there are no records linked to this account.\"}")))
    })
    @PreAuthorize("@authorizationService.isAdmin()")
    @DeleteMapping("/{slug}")
    ResponseEntity<Void> deleteProductCategoryBySlug(
            @PathVariable @Valid @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug
    ) {
        productCategoryService.deleteProductCategoryBySlug(slug);
        return ResponseEntity.ok().build();
    }
}