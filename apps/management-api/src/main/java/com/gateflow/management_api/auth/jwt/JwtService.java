package com.gateflow.management_api.auth.jwt;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.gateflow.management_api.user.entity.User;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long accessTokenExpiration;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${security.jwt.access-token-expiration}") long accessTokenExpiration
    ) {
        this.jwtEncoder = jwtEncoder;
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String generateAccessToken(User user) {

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(accessTokenExpiration);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("gateflow")
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}
