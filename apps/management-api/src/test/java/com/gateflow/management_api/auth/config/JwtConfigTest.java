package com.gateflow.management_api.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.gateflow.management_api.user.entity.UserStatus;
import com.gateflow.management_api.user.repository.UserRepository;

import static org.mockito.Mockito.mock;

class JwtConfigTest {

    private static final String ISSUER = "https://gateflow.local";

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private JwtEncoder jwtEncoder;
    private NimbusJwtDecoder jwtDecoder;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {

        String encodedSecret
                = "Z2F0ZWZsb3ctaW50ZWdyYXRpb24tdGVzdC1zZWNyZXQtMzJieXRlcyE=";

        byte[] keyBytes = Base64.getDecoder().decode(encodedSecret);

        SecretKey secretKey
                = new SecretKeySpec(keyBytes, "HmacSHA256");

        jwtEncoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(secretKey)
        );

        userRepository = mock(UserRepository.class);

        jwtDecoder = (NimbusJwtDecoder) new JwtConfig()
                .jwtDecoder(secretKey, userRepository, ISSUER);
    }

    @Test
    void shouldAcceptTokenWithCorrectIssuerAndActiveUser() {

        when(userRepository.existsByIdAndStatus(USER_ID, UserStatus.ACTIVE))
                .thenReturn(true);

        String token = generateToken(ISSUER, USER_ID.toString());

        assertEquals(
                USER_ID.toString(),
                jwtDecoder.decode(token).getSubject()
        );
    }

    @Test
    void shouldRejectTokenWithIncorrectIssuer() {

        when(userRepository.existsByIdAndStatus(USER_ID, UserStatus.ACTIVE))
                .thenReturn(true);

        String token = generateToken(
                "https://another-service.local",
                USER_ID.toString()
        );

        assertThrows(
                JwtValidationException.class,
                () -> jwtDecoder.decode(token)
        );
    }

    @Test
    void shouldRejectTokenWhenUserIsDisabled() {

        when(userRepository.existsByIdAndStatus(USER_ID, UserStatus.ACTIVE))
                .thenReturn(false);

        String token = generateToken(ISSUER, USER_ID.toString());

        assertThrows(
                JwtValidationException.class,
                () -> jwtDecoder.decode(token)
        );
    }

    @Test
    void shouldRejectTokenWithInvalidUserId() {

        String token = generateToken(ISSUER, "invalid-user-id");

        assertThrows(
                JwtValidationException.class,
                () -> jwtDecoder.decode(token)
        );
    }

    private String generateToken(String issuer, String subject) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}
