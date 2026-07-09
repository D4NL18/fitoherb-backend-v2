package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.LoginReq;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.RegisterReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.LoginRes;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestErrorMessage;
import com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestValidationErrorMessage;
import com.fitoherb.fitoherb_backend_v2.services.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Endpoints for Identity and Access Management (IAM). Handles user registration and secure authentication via JWT (JSON Web Tokens).")
public class AuthController {

    private final AuthorizationService authService;

    @Operation(summary = "Authenticate user", description = "Validates user credentials (email and password) and returns a Bearer Token (JWT) for authorized access.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated", content = @Content(schema = @Schema(implementation = LoginRes.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = RestErrorMessage.class), examples = @ExampleObject(name = "Invalid Credentials", value = "{\"status\": \"UNAUTHORIZED\", \"message\": \"E-mail or password invalid.\"}"))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = RestValidationErrorMessage.class), examples = @ExampleObject(name = "Field Validation Error", value = "{\"status\": \"BAD_REQUEST\", \"message\": \"Validation failed for one or more fields\", \"errors\": {\"email\": \"must be a well-formed email address\", \"password\": \"must not be blank\"}}")))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginRes> login(@RequestBody @Valid LoginReq loginReq) {
        String token = authService.login(loginReq);
        return ResponseEntity.ok(new LoginRes(token));
    }

    @Operation(summary = "Register new user", description = "Creates a new user account in the system database and returns the location URI of the newly created resource.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "409", description = "Conflict - Duplicate E-mail", content = @Content(schema = @Schema(implementation = RestErrorMessage.class), examples = @ExampleObject(name = "Email Conflict", value = "{\"status\": \"CONFLICT\", \"message\": \"E-mail already in use\"}"))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = RestValidationErrorMessage.class), examples = @ExampleObject(name = "Registration Validation Error", value = "{\"status\": \"BAD_REQUEST\", \"message\": \"Validation failed for one or more fields\", \"errors\": {\"birthDate\": \"must be a past date\", \"role\": \"must not be null\"}}"))),
            @ApiResponse(responseCode = "500", description = "Internal error", content = @Content(schema = @Schema(implementation = RestErrorMessage.class), examples = @ExampleObject(name = "Database Error", value = "{\"status\": \"INTERNAL_SERVER_ERROR\", \"message\": \"An unexpected error occurred. Please contact the administrator.\"}")))
    })
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterReq registerReq) {
        User savedUser = authService.register(registerReq);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedUser.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "Refresh token", description = "Receives an expired (but validly signed) token and returns a fresh one.")
    @PostMapping("/refresh")
    public ResponseEntity<LoginRes> refresh(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }

        String token = authHeader.replace("Bearer ", "");
        String newToken = authService.refreshToken(token);

        return ResponseEntity.ok(new LoginRes(newToken));
    }
}