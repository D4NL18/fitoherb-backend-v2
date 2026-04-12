package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.PasswordUpdateReq;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.UserReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.UserRes;
import com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestErrorMessage;
import com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestValidationErrorMessage;
import com.fitoherb.fitoherb_backend_v2.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("users")
@Tag(name = "Users", description = "Management of system users and accounts. " +
        "Handles the identity lifecycle, including profile administration, " +
        "security-sensitive password updates, and paginated directory services.")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get user by email", description = "Retrieves detailed profile information for a specific user using their email address. Requires an active authenticated session.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = UserRes.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"User not found with email: user@example.com\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\": \"UNAUTHORIZED\", \"message\": \"Token JWT invalid or expired\"}")))
    })
    @PreAuthorize("@authorizationService.isAuthenticated()")
    @GetMapping("/{email}")
    public ResponseEntity<UserRes> getUserByEmail(
            @Parameter(description = "Registered email of the user", example = "daniel.marinho@example.com")
            @PathVariable @Valid @Email @NotNull String email) {
        UserRes userRes = this.userService.getUserByEmail(email);
        return ResponseEntity.ok(userRes);
    }

    @Operation(summary = "List users with pagination", description = "Returns a paginated directory of system users. Supports global search filters and dynamic sorting.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list of users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\": \"UNAUTHORIZED\", \"message\": \"Token JWT invalid or expired\"}")))
    })
    @PreAuthorize("@authorizationService.isAuthenticated()")
    @GetMapping
    public ResponseEntity<Page<UserRes>> getAllUsersPaginated(
            @Parameter(description = "Search term to filter users by name or email", example = "Daniel")
            @RequestParam(required = false) String search,

            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Field to sort by", schema = @Schema(allowableValues = {"name", "email", "role", "createdAt"}))
            @RequestParam(defaultValue = "name") String sortField,

            @Parameter(description = "Sort direction", schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Page<UserRes> users = this.userService.getAllUsers(search, page, sortField, direction);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Update user profile", description = "Modifies administrative user data (excluding password). This operation is restricted to users with administrative privileges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = RestValidationErrorMessage.class),
                            examples = @ExampleObject(name = "Validation Error", value = "{\"status\": \"BAD_REQUEST\", \"message\": \"Validation failed for one or more fields\", \"errors\": {\"name\": \"must not be blank\"}}"))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"User not found with email: admin@example.com\"}"))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin rights required",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Forbidden", value = "{\"status\": \"FORBIDDEN\", \"message\": \"Access denied: You do not have the necessary permissions to access this resource.\"}"))),
            @ApiResponse(responseCode = "500", description = "Database error during update",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Database Error", value = "{\"status\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Failed to update user in database.\"}")))
    })
    @PreAuthorize("@authorizationService.isAdmin()")
    @PutMapping("/{email}")
    public ResponseEntity<Void> updateUserByEmail(
            @Parameter(description = "Target user email to update", example = "user@example.com")
            @PathVariable @Valid @Email @NotNull String email,
            @RequestBody @Valid UserReq userReq
    ) {
        this.userService.updateUserByEmail(email, userReq);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update user password", description = "Performs a secure password update for the specified account. Requires valid current credentials as part of the request.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = RestValidationErrorMessage.class),
                            examples = @ExampleObject(name = "Password Validation Error", value = "{\"status\": \"BAD_REQUEST\", \"message\": \"Validation failed for one or more fields\", \"errors\": {\"password\": \"size must be between 8 and 100\"}}"))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"User not found with email: user@example.com\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\": \"UNAUTHORIZED\", \"message\": \"Token JWT invalid or expired\"}"))),
            @ApiResponse(responseCode = "500", description = "Failed to encrypt or save new password",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Update Failed", value = "{\"status\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Failed to update password.\"}")))
    })
    @PreAuthorize("@authorizationService.isAuthenticated()")
    @PatchMapping("/update-password/{email}")
    public ResponseEntity<Void> updatePasswordByEmail(
            @Parameter(description = "Email of the account whose password will be changed", example = "user@example.com")
            @PathVariable @Valid @Email @NotNull String email,
            @RequestBody @Valid PasswordUpdateReq passwordUpdateReq
    ) {
        this.userService.updatePasswordByEmail(email, passwordUpdateReq);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete user account", description = "Permanently removes a user record from the system. This action is irreversible and restricted to administrators.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\": \"NOT_FOUND\", \"message\": \"User not found with email: user@example.com\"}"))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Forbidden", value = "{\"status\": \"FORBIDDEN\", \"message\": \"Access denied: You do not have the necessary permissions to access this resource.\"}"))),
            @ApiResponse(responseCode = "500", description = "Database error",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Integrity Constraint", value = "{\"status\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Failed to delete user. Ensure there are no records linked to this account.\"}")))
    })
    @PreAuthorize("@authorizationService.isAdmin()")
    @DeleteMapping("/{email}")
    public ResponseEntity<Void> deleteUserByEmail(
            @Parameter(description = "Target user email to be deleted", example = "user@example.com")
            @PathVariable @Valid @Email @NotNull String email
    ) {
        this.userService.deleteUserByEmail(email);
        return ResponseEntity.ok().build();
    }
}