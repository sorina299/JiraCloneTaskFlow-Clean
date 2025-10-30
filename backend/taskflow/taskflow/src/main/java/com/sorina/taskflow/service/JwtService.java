package com.sorina.taskflow.service;

import com.sorina.taskflow.entity.User;
import com.sorina.taskflow.repository.TokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretB64;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpire;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpire;

    @Value("${jwt.issuer}")
    private String issuer;

    private final TokenRepository tokens;
    private SecretKey key;

    public JwtService(TokenRepository tokens) { this.tokens = tokens; }

    @PostConstruct
    void init() { this.key = Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(secretB64)); }

    public String generateAccessToken(UserDetails user) {
        return buildToken(user.getUsername(), Map.of("roles", user.getAuthorities().stream().map(a->a.getAuthority()).toList()), accessTokenExpire);
    }

    public String generateRefreshToken(UserDetails user) {
        return buildToken(user.getUsername(), Collections.emptyMap(), refreshTokenExpire);
    }

    private String buildToken(String subject, Map<String,Object> claims, long ttlMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMs)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isAccessTokenValid(String token, UserDetails user) {
        try {
            var c = parseClaims(token);
            var notExpired = c.getExpiration().after(new Date());
            var subjectOk  = user.getUsername().equals(c.getSubject());
            var notRevoked = tokens.findByAccessToken(token).map(t->!t.isLoggedOut()).orElse(true);
            return notExpired && subjectOk && notRevoked;
        } catch (JwtException | IllegalArgumentException e) { return false; }
    }

    public boolean isRefreshTokenValid(String token, User user) {
        try {
            var c = parseClaims(token);
            var notExpired = c.getExpiration().after(new Date());
            var subjectOk  = user.getUsername().equals(c.getSubject());
            var notRevoked = tokens.findByRefreshToken(token).map(t->!t.isLoggedOut()).orElse(true);
            return notExpired && subjectOk && notRevoked;
        } catch (JwtException | IllegalArgumentException e) { return false; }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
