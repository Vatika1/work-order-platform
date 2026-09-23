package com.vatika.workorder.shared.security;

import com.vatika.workorder.identity.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(UUID userId, String email, Role role, UUID clientId) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .issuer(props.issuer())
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.accessTtl())));

        if (clientId != null) {
            builder.claim("clientId", clientId.toString());
        }
        return builder.signWith(key).compact();
    }

    public Optional<JwtPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(props.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String clientIdClaim = claims.get("clientId", String.class);
            return Optional.of(new JwtPrincipal(
                    UUID.fromString(claims.getSubject()),
                    claims.get("email", String.class),
                    Role.valueOf(claims.get("role", String.class)),
                    clientIdClaim == null ? null : UUID.fromString(clientIdClaim)
            ));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}