package com.quizApp.backendQuizApp.controller;

import com.quizApp.backendQuizApp.dto.auth.AuthRequest;
import com.quizApp.backendQuizApp.dto.auth.AuthResponse;
import com.quizApp.backendQuizApp.dto.auth.RefreshRequest;
import com.quizApp.backendQuizApp.dto.auth.RegisterRequest;
import com.quizApp.backendQuizApp.model.User;
import com.quizApp.backendQuizApp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody @Valid RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }
}
