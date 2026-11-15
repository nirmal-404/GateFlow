package com.gateflow.management_api.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gateflow.management_api.auth.dto.LoginResponse;
import com.gateflow.management_api.auth.jwt.JwtService;
import com.gateflow.management_api.user.entity.User;
import com.gateflow.management_api.user.entity.UserStatus;
import com.gateflow.management_api.user.repository.UserRepository;

@Service
@Transactional
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(
            String fullName,
            String email,
            String rawPassword
    ) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        User user = new User(fullName, email, passwordHash);

        return userRepository.save(user);
    }

    public User authenticate(String email, String rawPassword) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid email or password"
                ));

        if (!passwordEncoder.matches(
                rawPassword,
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "User account is disabled"
            );
        }

        return user;
    }

    public LoginResponse login(String email, String rawPassword) {

        User user = authenticate(email, rawPassword);

        String accessToken = jwtService.generateAccessToken(user);

        return new LoginResponse(
                accessToken,
                "Bearer",
                900, // 15 minutes in seconds
                user.getId(),
                user.getEmail(),
                user.getFullName()
        );
    }
}
