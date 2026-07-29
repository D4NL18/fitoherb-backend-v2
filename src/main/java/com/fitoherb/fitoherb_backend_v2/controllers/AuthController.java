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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

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
    public ResponseEntity<LoginRes> login(@RequestBody @Valid LoginReq loginReq, HttpServletResponse response) {
        String token = authService.login(loginReq);
        setAuthCookies(response, token, loginReq.getEmail(), loginReq.getRememberMe());
        return ResponseEntity.ok(new LoginRes(token)); // Kept for backwards compatibility if needed, but not used by frontend anymore
    }

    @Operation(summary = "Register new user", description = "Creates a new user account in the system database and returns the location URI of the newly created resource.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "409", description = "Conflict - Duplicate E-mail", content = @Content(schema = @Schema(implementation = RestErrorMessage.class), examples = @ExampleObject(name = "Email Conflict", value = "{\"status\": \"CONFLICT\", \"message\": \"E-mail already in use\"}"))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = RestValidationErrorMessage.class), examples = @ExampleObject(name = "Registration Validation Error", value = "{\"status\": \"BAD_REQUEST\", \"message\": \"Validation failed for one or more fields\", \"errors\": {\"role\": \"must not be null\"}}"))),
            @ApiResponse(responseCode = "500", description = "Internal error", content = @Content(schema = @Schema(implementation = RestErrorMessage.class), examples = @ExampleObject(name = "Database Error", value = "{\"status\": \"INTERNAL_SERVER_ERROR\", \"message\": \"An unexpected error occurred. Please contact the administrator.\"}")))
    })
    @PreAuthorize("@authorizationService.isAdmin()")
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
    public ResponseEntity<LoginRes> refresh(
            @org.springframework.web.bind.annotation.CookieValue(value = "fitoherb_jwt", required = false) String jwtCookie,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletResponse response) {
            
        String token = jwtCookie;
        if (token == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.replace("Bearer ", "");
        }

        if (token == null) {
            return ResponseEntity.badRequest().build();
        }

        String newToken = authService.refreshToken(token);
        
        // We can't know the user's email directly here without decoding the token, but TokenService has a method for it.
        // Let's decode it:
        String email = com.auth0.jwt.JWT.decode(newToken).getSubject();
        
        // We assume session cookie if we are refreshing, or maybe long lived. We don't have rememberMe flag here.
        // For safety, we keep it as a session cookie or 30 days. Let's just do 30 days.
        setAuthCookies(response, newToken, email, true);

        return ResponseEntity.ok(new LoginRes(newToken));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        setAuthCookies(response, null, null, false); // This will effectively clear the cookies since token is null, we can customize this
        return ResponseEntity.ok().build();
    }

    private void setAuthCookies(HttpServletResponse response, String token, String email, Boolean rememberMe) {
        int maxAge = (token == null) ? 0 : ((rememberMe != null && rememberMe) ? 30 * 24 * 60 * 60 : -1);

        Cookie jwtCookie = new Cookie("fitoherb_jwt", token == null ? "" : token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(maxAge);
        response.addCookie(jwtCookie);

        Cookie emailCookie = new Cookie("fitoherb_user_email", email == null ? "" : email);
        emailCookie.setHttpOnly(false);
        emailCookie.setPath("/");
        emailCookie.setMaxAge(maxAge);
        response.addCookie(emailCookie);
    }
}