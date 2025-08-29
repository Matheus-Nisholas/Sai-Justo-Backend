package br.com.nish.calculadora.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Serviço utilitário para geração e validação de tokens JWT.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-minutes}")
    private Integer expirationMinutes;

    /**
     * Gera um JWT assinado com HS256.
     * @param subject identificador do usuário, geralmente o email
     * @param extraClaims claims adicionais (ex.: roles)
     * @return token JWT
     */
    public String generateToken(String subject, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds((long) expirationMinutes * 60L);

        String token = Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .claims(extraClaims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(getKey())
                .compact();

        return token;
    }

    /**
     * Valida e retorna as claims do token.
     * Lança exceção do jjwt se o token for inválido/expirado.
     * @param token JWT
     * @return Claims do token
     */
    public Claims parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims;
    }

    /**
     * Retorna o subject (email) do token, se válido.
     * @param token JWT
     * @return subject
     */
    public String getSubject(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * Verifica rapidamente se o token é válido e não expirou.
     * @param token JWT
     * @return true se válido
     */
    public boolean isValid(String token) {
        try {
            Claims claims = parseToken(token);
            Date exp = claims.getExpiration();
            return exp != null && exp.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Key getKey() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        Key key = Keys.hmacShaKeyFor(bytes);
        return key;
    }
}
