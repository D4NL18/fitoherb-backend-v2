package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.PasswordUpdateReq;
import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.UserReq;
import com.fitoherb.fitoherb_backend_v2.DTOs.Responses.UserRes;
import com.fitoherb.fitoherb_backend_v2.services.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

@RestController
@RequestMapping("users")
public class UserController {

    @Autowired
    private UserService userService;

    @PreAuthorize("@authorizationService.isAuthenticated()")
    @GetMapping("/{email}")
    public ResponseEntity<UserRes> getUserByEmail(@PathVariable @Valid @Email @NotNull String email) {
        UserRes userRes = this.userService.getUserByEmail(email);
        return ResponseEntity.ok(userRes);
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @PutMapping("/{email}")
    public ResponseEntity updateUserByEmail(
            @PathVariable @Valid @Email @NotNull String email,
            @RequestBody @Valid UserReq userReq
    ) {
        this.userService.updateUserByEmail(email, userReq);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@authorizationService.isAuthenticated()")
    @PutMapping("/update-password/{email}")
    public ResponseEntity<Void> updatePasswordByEmail(
            @PathVariable @Valid @Email @NotNull String email,
            @RequestBody @Valid PasswordUpdateReq passwordUpdateReq
    ) {
        this.userService.updatePasswordByEmail(email, passwordUpdateReq);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@authorizationService.isAdmin()")
    @DeleteMapping("/{email}")
    public ResponseEntity<Void> deleteUserByEmail(
            @PathVariable @Valid @Email @NotNull String email
    ) {
        this.userService.deleteUserByEmail(email);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@authorizationService.isAuthenticated()")
    @GetMapping
    public ResponseEntity<Page<UserRes>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "name") String sortField,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Page<UserRes> users = this.userService.getAllUsers(search, page, sortField, direction);
        return ResponseEntity.ok(users);
    }


}
