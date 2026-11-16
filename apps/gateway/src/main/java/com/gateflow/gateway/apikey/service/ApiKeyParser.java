package com.gateflow.gateway.apikey.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class ApiKeyParser {

    private static final Pattern API_KEY_PATTERN =
            Pattern.compile("^gtf_(live|test)_([a-fA-F0-9]{16})_([A-Za-z0-9_-]+)$");

    public ParsedApiKey parse(String apiKey) {
        if (apiKey == null) {
            return null;
        }

        Matcher matcher = API_KEY_PATTERN.matcher(apiKey);

        if (!matcher.matches()) {
            return null;
        }

        return new ParsedApiKey(
                matcher.group(1),
                matcher.group(2),
                matcher.group(3)
        );
    }

    public record ParsedApiKey(
            String environment,
            String prefix,
            String secret
    ) {
    }
}