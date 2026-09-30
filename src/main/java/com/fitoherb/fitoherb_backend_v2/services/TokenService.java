package com.fitoherb.fitoherb_backend_v2.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.exceptions.InvalidTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            var builder = JWT.create()
                    .withIssuer("auth-api")
                    .withSubject(user.getEmail())
                    .withExpiresAt(genExpirationDate());

            if (user.getRole() != null) {
                builder.withClaim("role", user.getRole().name());
            }
            if (user.getName() != null) {
                builder.withClaim("name", user.getName());
            }

            return builder.sign(algorithm);
        } catch (JWTVerificationException e) {
            throw new InvalidTokenException("Erro ao gerar token JWT");
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("auth-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException e) {
            throw new InvalidTokenException("Token JWT inválido ou expirado");
        }
    }

    public String validateAndGetSubjectEvenIfExpired(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("auth-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (TokenExpiredException e) {
            return JWT.decode(token).getSubject();
        } catch (JWTVerificationException e) {
            throw new InvalidTokenException("Token JWT inválido ou forjado");
        }
    }

    private Instant genExpirationDate() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}