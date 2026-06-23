package com.oliveyoung.mate.infrastructure.crew.auth;

import com.oliveyoung.mate.application.crew.TokenProvider;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider implements TokenProvider {

    private final SecretKey key;
    private final long      expireMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expire-ms:86400000}") long expireMs) {
        this.key      = Keys.hmacShaKeyFor(secret.getBytes());
        this.expireMs = expireMs;
    }

    @Override
    public long expireSeconds() { return expireMs <= 0 ? -1 : expireMs / 1000; }

    @Override
    public String generate(UUID crewId, String role) {
        var builder = Jwts.builder()
            .subject(crewId.toString())
            .claim("role", role)
            .issuedAt(new Date());
        if (expireMs > 0) {
            builder.expiration(new Date(System.currentTimeMillis() + expireMs));
        }
        return builder.signWith(key).compact();
    }

    @Override
    public String generateRefresh(UUID crewId, String role) {
        long refreshExpireMs = expireMs > 0 ? expireMs * 7 : -1;
        var builder = Jwts.builder()
            .subject(crewId.toString())
            .claim("role", role)
            .claim("type", "refresh")
            .issuedAt(new Date());
        if (refreshExpireMs > 0) {
            builder.expiration(new Date(System.currentTimeMillis() + refreshExpireMs));
        }
        return builder.signWith(key).compact();
    }

    public UUID extractCrewId(String token) {
        return UUID.fromString(claims(token).getSubject());
    }

    public String extractRole(String token) {
        return claims(token).get("role", String.class);
    }

    private io.jsonwebtoken.Claims claims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
