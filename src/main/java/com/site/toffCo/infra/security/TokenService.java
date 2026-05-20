package com.site.toffCo.infra.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey secretKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            byte[] key = new byte[32];
            System.arraycopy(keyBytes, 0, key, 0, keyBytes.length);
            return Keys.hmacShaKeyFor(key);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
    public String generateToken(String email) {
        var expiration = Instant.now().plus(10, ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(email)
                .issuer("ToffCo")
                .expiration(Date.from(expiration))
                .signWith(secretKey())
                .compact();
    }

    public String validateToken(String token) {
        try{
            return Jwts.parser()
                    .verifyWith(secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception _){
            return null;
        }
    }
}
