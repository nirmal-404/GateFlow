package com.gateflow.management_api.security.service;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public UUID getCurrentUserId() {

        Authentication authentication
                = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException(
                    "User is not authenticated with a JWT"
            );
        }

        return UUID.fromString(jwtAuth.getToken().getSubject());
    }
}
