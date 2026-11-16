package com.dairyfarm.security;

import com.dairyfarm.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiration;
    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expiration) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT_SECRET is required and must be Base64-encoded with at least 32 random bytes.");
        }
        byte[] decodedSecret;
        try {
            decodedSecret = Decoders.BASE64.decode(secret);
        } catch (DecodingException ex) {
            throw new IllegalArgumentException("JWT_SECRET must be valid Base64 and contain at least 32 random bytes.");
        }
        if (decodedSecret.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be Base64-encoded and contain at least 32 bytes.");
        }
        this.key = Keys.hmacShaKeyFor(decodedSecret);
        if (expiration < 1) throw new IllegalArgumentException("JWT_EXPIRATION_MS must be greater than zero.");
        this.expiration = expiration;
    }
    public String generate(AppUser user) {
        Date now = new Date();
        return Jwts.builder().subject(user.getUsername()).issuedAt(now)
                .claim("tokenVersion", user.getTokenVersion())
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key).compact();
    }
    public String username(String token) {
        return claims(token).getSubject();
    }
    public long tokenVersion(String token) {
        return claims(token).get("tokenVersion", Long.class);
    }
    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
}
