package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.LoginReq;
import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.RegisterReq;
import com.fitoherb.fitoherb_backend_v2.services.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth")
public class Auth {

    @Autowired
    AuthorizationService authService;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid LoginReq loginReq) {
        return authService.login(loginReq);
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody @Valid RegisterReq registerReq) {
        return authService.register(registerReq);
    }
}

