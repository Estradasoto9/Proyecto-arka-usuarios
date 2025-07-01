package com.projectArka.user_service.infrastructure.adapter.out.security;

import com.projectArka.user_service.application.port.out.JwtServicePort;
import com.projectArka.user_service.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtServiceAdapter implements JwtServicePort {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    @Override
    public Mono<String> generateToken(User user) {
        return generateToken(new HashMap<>(), user);
    }

    public Mono<String> generateToken(Map<String, Object> extraClaims, User user) {
        return Mono.fromCallable(() -> {
            Date now = new Date(System.currentTimeMillis());
            Date expirationDate = new Date(now.getTime() + jwtExpiration);

            return Jwts.builder()
                    .setClaims(extraClaims)
                    .setSubject(user.getUsername())
                    .claim("userId", user.getId())
                    .claim("roles", user.getRoles())
                    .setIssuedAt(now)
                    .setExpiration(expirationDate)
                    .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                    .compact();
        });
    }

    public Mono<String> generateRefreshToken(User user) {
        return generateToken(new HashMap<>(), user, refreshExpiration);
    }

    public Mono<String> generateToken(Map<String, Object> extraClaims, User user, long expiration) {
        return Mono.fromCallable(() -> {
            Date now = new Date(System.currentTimeMillis());
            Date expirationDate = new Date(now.getTime() + expiration);

            return Jwts.builder()
                    .setClaims(extraClaims)
                    .setSubject(user.getUsername())
                    .claim("userId", user.getId())
                    .claim("roles", user.getRoles())
                    .setIssuedAt(now)
                    .setExpiration(expirationDate)
                    .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                    .compact();
        });
    }

    @Override
    public Mono<Boolean> validateToken(String token) {
        return Mono.fromCallable(() -> !isTokenExpired(token));
    }

    @Override
    public Mono<String> extractUsername(String token) {
        return Mono.just(extractClaim(token, Claims::getSubject));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}