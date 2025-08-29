package br.com.nish.calculadora.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Serviço utilitário para geração e validação de tokens JWT (JJWT 0.11.5).
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
     * @param subject identificador do usuário (email)
     * @param extraClaims claims adicionais (ex.: roles)
     * @return token JWT
     */
    public String generateToken(String subject, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds((long) expirationMinutes * 60L);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(issuer)
                .addClaims(extraClaims != null ? extraClaims : Map.of())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valida e retorna as claims do token.
     * Lança exceção se o token for inválido/expirado.
     * @param token JWT
     * @return Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Retorna o subject (email) do token, se válido.
     */
    public String getSubject(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * Verifica rapidamente se o token é válido e não expirou.
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
        // garanta que o segredo tenha pelo menos 32 chars para HS256
        return Keys.hmacShaKeyFor(bytes);
    }
}
