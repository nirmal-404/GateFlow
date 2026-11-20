package com.gateflow.management_api.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Base64;

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

class JwtConfigTest {

    private JwtEncoder jwtEncoder;
    private NimbusJwtDecoder jwtDecoder;

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

        jwtDecoder = (NimbusJwtDecoder) new JwtConfig()
                .jwtDecoder(secretKey, "https://gateflow.local");
    }

    @Test
    void shouldAcceptTokenWithCorrectIssuer() {

        String token = generateToken("https://gateflow.local");

        assertEquals(
                "https://gateflow.local",
                jwtDecoder.decode(token).getIssuer().toString()
        );
    }

    @Test
    void shouldRejectTokenWithIncorrectIssuer() {

        String token = generateToken("https://another-service.local");

        assertThrows(
                JwtValidationException.class,
                () -> jwtDecoder.decode(token)
        );
    }

    private String generateToken(String issuer) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("test-user")
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
