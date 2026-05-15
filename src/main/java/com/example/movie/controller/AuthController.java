package com.example.movie.controller;

import com.example.movie.dto.SendEmailCodeRequest;
import com.example.movie.dto.auth.LoginRequest;
import com.example.movie.dto.auth.LoginResponse;
import com.example.movie.dto.auth.RegisterRequest;
import com.example.movie.dto.auth.RegisterResponse;
import com.example.movie.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/email-code")
    public ResponseEntity<Void> sendEmailCode(@Valid @RequestBody SendEmailCodeRequest request) {
        authService.sendEmailCode(request.getEmail());
        return ResponseEntity.ok().build();
    }
}