package com.mentalhealthforum.mentalhealthforum_backend.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.AuthenticationFailedException;
import com.mentalhealthforum.mentalhealthforum_backend.service.impl.AuthServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);
    /**
     * Creates a Spring Security Jwt object from a raw token string.
     * Converts timestamp claims (exp, iat, nbf) from Integer/Long to Instant.
     */
    public Jwt createJwtFromToken(String token){
        try{
            String [] chunks = token.split("\\.");
            if(chunks.length != 3){
                throw new IllegalArgumentException("Invalid Jwt token format");
            }

            String header = new String(java.util.Base64.getUrlDecoder().decode(chunks[0]));
            String payload = new String(java.util.Base64.getUrlDecoder().decode(chunks[1]));
            String signature = chunks[2];

            // Parse claims
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> headers = objectMapper.readValue(header, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> claims = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});

            // Convert timestamp claims from Integer/Long to Instant
            convertTimestampClaim(claims, "exp");
            convertTimestampClaim(claims, "iat");
            convertTimestampClaim(claims, "nbf");

            return Jwt.withTokenValue(token)
                    .headers(h -> h.putAll(headers))
                    .claims(c -> c.putAll(claims))
                    .build();
        }
        catch (Exception e){
            log.error("Failed to extract subject from JWT during login intercept", e);
            throw new AuthenticationFailedException("Authentication failed: invalid session token.", e);
        }
    }

    /**
     * Converts a timestamp claim from Number (Integer/Long) to Instant
     * Keycloak returns this as Unix epoch seconds
     * */
    private void convertTimestampClaim(Map<String, Object> claims, String claimName){
        if(claims.containsKey(claimName)){
            Object value = claims.get(claimName);
            if(value instanceof Number){
                long epochSeconds = ((Number) value).longValue();
                claims.put(claimName, Instant.ofEpochSecond(epochSeconds));
            }
        }
    }

}
