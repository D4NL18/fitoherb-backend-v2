package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.BannerReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.BannerRes;
import com.fitoherb.fitoherb_backend_v2.entities.Banner;
import com.fitoherb.fitoherb_backend_v2.services.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.MSG_REQUIRED_FIELD;

@RestController
@RequiredArgsConstructor
@RequestMapping("/banners")
@Tag(name = "Banners", description = "Management of homepage banners.")
public class BannerController {

    private final BannerService bannerService;

    @Operation(summary = "Get active banners", description = "Public endpoint to retrieve all active banners sorted by position.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BannerRes.class))))
    })
    @GetMapping("/active")
    public ResponseEntity<List<BannerRes>> getActiveBanners() {
        return ResponseEntity.ok(bannerService.getActiveBanners());
    }

    @Operation(summary = "List banners for administration", description = "Returns a paginated list of all banners. Requires authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestErrorMessage.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Unauthorized", value = "{\"status\": \"UNAUTHORIZED\", \"message\": \"Token JWT invalid or expired\"}")))
    })
    @PreAuthorize("@authorizationService.isAuthenticated()")
    @GetMapping
    public ResponseEntity<Page<BannerRes>> getAllBannersPaginated(
            @Parameter(description = "Search by banner title", example = "Promo")
            @RequestParam(required = false) String search,

            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Field to sort by", schema = @Schema(allowableValues = {"position", "title", "createdAt"}))
            @RequestParam(defaultValue = "position") String sortField,

            @Parameter(description = "Sort direction", schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        if ("name".equalsIgnoreCase(sortField)) {
            sortField = "title";
        }
        Page<BannerRes> banners = bannerService.getAllBannersPaginated(search, page, sortField, direction);
        return ResponseEntity.ok(banners);
    }

    @Operation(summary = "Get banner by id", description = "Retrieves detailed information of a specific banner. Restricted to administrators.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Banner found",
                    content = @Content(schema = @Schema(implementation = BannerRes.class))),
            @ApiResponse(responseCode = "404", description = "Banner not found",
                    content = @Content(schema = @Schema(implementation = com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestErrorMessage.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"Banner not found with id: 123\"}"))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestErrorMessage.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Forbidden", value = "{\"status\": \"FORBIDDEN\", \"message\": \"Access denied: You do not have the necessary permissions to access this resource.\"}")))
    })
    @PreAuthorize("@authorizationService.isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<BannerRes> getBannerById(@PathVariable String id) {
        return ResponseEntity.ok(bannerService.getBannerById(id));
    }

    @Operation(summary = "Create a new banner", description = "Registers a new banner in the system. Requires a JSON part for metadata and a Multipart file for the image asset.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Banner created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestValidationErrorMessage.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Validation Error", value = "{\"status\": \"BAD_REQUEST\", \"message\": \"Validation failed for one or more fields\", \"errors\": {\"title\": \"This field cannot be empty or null\"}}"))),
            @ApiResponse(responseCode = "500", description = "Internal error",
                    content = @Content(schema = @Schema(implementation = com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestErrorMessage.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Database Error", value = "{\"status\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Failed to save banner.\" }")))
    })
    @PreAuthorize("@authorizationService.isAuthenticated()")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createBanner(
            @RequestBody(content = @Content(encoding = @Encoding(name = "banner", contentType = MediaType.APPLICATION_JSON_VALUE)))
            @RequestPart(value = "banner") @Valid BannerReq bannerReq,
            @RequestPart(value = "image") @Valid @NotNull(message = MSG_REQUIRED_FIELD) MultipartFile image
    ) {
        Banner savedBanner = bannerService.createBanner(bannerReq, image);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedBanner.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "Update banner by id", description = "Updates an existing banner's data and image.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Banner updated successfully"),
            @ApiResponse(responseCode = "404", description = "Banner not found",
                    content = @Content(schema = @Schema(implementation = com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestErrorMessage.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"Banner not found with id: 123\"}"))),
            @ApiResponse(responseCode = "500", description = "Failed to update banner",
                    content = @Content(schema = @Schema(implementation = com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestErrorMessage.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Database Error", value = "{\"status\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Failed to update banner in the database.\"}")))
    })
    @PreAuthorize("@authorizationService.isAuthenticated()")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateBanner(
            @RequestBody(content = @Content(encoding = @Encoding(name = "banner", contentType = MediaType.APPLICATION_JSON_VALUE)))
            @RequestPart(value = "banner") @Valid BannerReq bannerReq,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @PathVariable @Valid @NotBlank(message = MSG_REQUIRED_FIELD) String id
    ) {
        bannerService.updateBanner(bannerReq, image, id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete banner by id", description = "Permanently removes a banner and its associated image from storage.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Banner deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Banner not found",
                    content = @Content(schema = @Schema(implementation = com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestErrorMessage.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"Banner not found with id: 123\"}"))),
            @ApiResponse(responseCode = "500", description = "Database error",
                    content = @Content(schema = @Schema(implementation = com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestErrorMessage.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Integrity Constraint", value = "{\"status\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Failed to delete banner.\"}")))
    })
    @PreAuthorize("@authorizationService.isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable @Valid @NotBlank(message = MSG_REQUIRED_FIELD) String id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok().build();
    }
}
