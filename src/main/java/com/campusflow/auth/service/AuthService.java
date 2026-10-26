package com.campusflow.auth.service;

import com.campusflow.auth.dto.AuthResponse;
import com.campusflow.auth.dto.LoginRequest;
import com.campusflow.auth.dto.RegisterRequest;
import com.campusflow.common.exception.ApiException;
import com.campusflow.security.AppUserPrincipal;
import com.campusflow.security.JwtService;
import com.campusflow.user.dto.UserResponse;
import com.campusflow.user.entity.Role;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.RoleRepository;
import com.campusflow.user.repository.UserRepository;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    // TODO: restrict to student email domains and require verification (users.is_verified)
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Email is already registered");
        }
        Role studentRole = roleRepository.findByName(Role.STUDENT)
                .orElseThrow(() -> new IllegalStateException(Role.STUDENT + " is missing; check the seed migration"));

        User user = userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .studentId(request.studentId())
                .university(request.university())
                .roles(Set.of(studentRole))
                .build());
        return issueTokens(user, refreshTokenService.create(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(normalizeEmail(request.email()), request.password()));
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        User user = userRepository.getReferenceById(principal.id());
        return issueTokens(user, refreshTokenService.create(user));
    }

    public AuthResponse refresh(String refreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        return issueTokens(rotation.user(), rotation.refreshToken());
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResponse issueTokens(User user, String refreshToken) {
        UserResponse userResponse = UserResponse.from(user);
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), userResponse.roles());
        return AuthResponse.bearer(accessToken, refreshToken, jwtService.accessTokenTtlSeconds(), userResponse);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
