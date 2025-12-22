package com.collab.userservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    // This is the "secret key". Keep it long and private.
    private final String SECRET_STRING = "mySecretKeyForJwtMustBeLongEnoughToWork1234567890";
    private final Key key = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());

    // This creates the token
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // Valid for 24 hours
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // This reads the username back out of the token
    public String validateAndGetUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}