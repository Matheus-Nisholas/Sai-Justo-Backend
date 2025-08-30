package br.com.nish.calculadora.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey; // <-- importe correto
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-minutes}")
    private Integer expirationMinutes;

    public String generateToken(String subject, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds((long) expirationMinutes * 60L);

        return Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .claims(extraClaims != null ? extraClaims : Map.of())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(getKey())      // SecretKey
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())    // SecretKey
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getSubject(String token) {
        return parseToken(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parseToken(token);
            Date exp = claims.getExpiration();
            return exp != null && exp.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private SecretKey getKey() {          // <-- mude para SecretKey
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes); // retorna um SecretKey
    }
}
