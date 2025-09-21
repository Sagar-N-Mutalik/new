package com.quizApp.backendQuizApp.service;

import com.quizApp.backendQuizApp.dto.auth.AuthRequest;
import com.quizApp.backendQuizApp.dto.auth.AuthResponse;
import com.quizApp.backendQuizApp.dto.auth.RegisterRequest;
import com.quizApp.backendQuizApp.model.RefreshToken;
import com.quizApp.backendQuizApp.model.User;
import com.quizApp.backendQuizApp.repository.RefreshTokenRepository;
import com.quizApp.backendQuizApp.repository.UserRepository;
import com.quizApp.backendQuizApp.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(), request.getPassword()
                )
        );
        UserDetails userDetails = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        String access = jwtService.generateToken(userDetails);
        String refresh = jwtService.generateRefreshToken(userDetails);

        saveRefreshToken((User) userDetails, refresh);

        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .build();
    }

    public AuthResponse refresh(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (token.getRevoked() || token.isExpired()) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found for token"));
        String newAccess = jwtService.generateToken(user);
        String newRefresh = jwtService.generateRefreshToken(user);

        token.setRevoked(true);
        refreshTokenRepository.save(token);
        saveRefreshToken(user, newRefresh);

        return AuthResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .build();
    }

    private void saveRefreshToken(User user, String tokenStr) {
        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .token(tokenStr)
                .userId(user.getId())
                .expiryDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);
    }
}
