package com.gateflow.management_api.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.gateflow.management_api.auth.dto.LoginRequest;
import com.gateflow.management_api.auth.dto.LoginResponse;
import com.gateflow.management_api.auth.dto.RegisterRequest;
import com.gateflow.management_api.auth.dto.RegisterResponse;
import com.gateflow.management_api.auth.service.AuthenticationService;
import com.gateflow.management_api.user.entity.User;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        User user = authenticationService.register(
                request.fullName(),
                request.email(),
                request.password()
        );

        RegisterResponse response = new RegisterResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getStatus().name()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authenticationService.login(
                request.email(),
                request.password()
        );

        return ResponseEntity.ok(response);
    }
}
