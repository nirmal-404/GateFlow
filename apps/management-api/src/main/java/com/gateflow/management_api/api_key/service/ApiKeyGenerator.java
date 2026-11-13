
package com.gateflow.management_api.api_key.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.gateflow.management_api.api_key.entity.ApiKeyEnvironment;

@Component
public class ApiKeyGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public GeneratedApiKey generate(ApiKeyEnvironment environment) {

        byte[] prefixBytes = new byte[8];
        secureRandom.nextBytes(prefixBytes);

        String prefix = HexFormat.of().formatHex(prefixBytes);

        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);

        String secret = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(secretBytes);

        String fullKey = "gtf_"
            + environment.name().toLowerCase()
            + "_"
            + prefix
            + "_"
            + secret;

        return new GeneratedApiKey(
            prefix,
            secret,
            fullKey,
            hashSecret(secret)
        );
    }

    private String hashSecret(String secret) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 algorithm is unavailable",
                exception
            );
        }
    }

    public record GeneratedApiKey(
        String prefix,
        String secret,
        String fullKey,
        String hash
    ) {
    }
}
