package com.example.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.Date;
import java.util.Properties;

public class JwtUtil {
    private static final String SECRET;
    private static final long EXPIRATION;
    private static final Key KEY;

    static {
        Properties props = new Properties();
        try (InputStream input = JwtUtil.class.getClassLoader().getResourceAsStream("application-local.properties")) {
            if (input != null) props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Could not load application-local.properties", e);
        }
        String secret = System.getenv("JWT_SECRET");
        if (secret == null) secret = props.getProperty("jwt.secret", "REDACTED");
        String expiration = System.getenv("JWT_EXPIRATION");
        if (expiration == null) expiration = props.getProperty("jwt.expiration", "86400000");
        SECRET = secret;
        EXPIRATION = Long.parseLong(expiration);
        KEY = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    public static String generateToken(String username) {
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
            .signWith(KEY, SignatureAlgorithm.HS512)
            .compact();
    }

    public static String validateToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(KEY)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }

    public static long getExpiration(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration().getTime();
    }
}
