package com.ds.energy.gateway;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

class JwtSecurityTests {
    private static final String SECRET = "test-jwt-secret-with-more-than-32-characters";
    private final JwtDecoder decoder = new SecurityConfig().jwtDecoder(SECRET);

    private String token(String secret, String audience, String issuer, Instant expiry) {
        var encoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();
        var claims = JwtClaimsSet.builder().subject("42").issuer(issuer).audience(List.of(audience))
                .issuedAt(Instant.now().minusSeconds(3600)).expiresAt(expiry).claim("role", "CLIENT").build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    void acceptsCorrectlySignedUnexpiredJwt() {
        assertEquals("42", decoder.decode(token(SECRET, "energy-api", "energy-auth", Instant.now().plusSeconds(3600))).getSubject());
    }

    @Test
    void rejectsExpiredTokenWrongAudienceAndWrongIssuer() {
        assertThrows(JwtException.class, () -> decoder.decode(token(SECRET, "energy-api", "energy-auth", Instant.now().minusSeconds(10))));
        assertThrows(JwtException.class, () -> decoder.decode(token(SECRET, "other-api", "energy-auth", Instant.now().plusSeconds(3600))));
        assertThrows(JwtException.class, () -> decoder.decode(token(SECRET, "energy-api", "other-issuer", Instant.now().plusSeconds(3600))));
    }

    @Test
    void rejectsWrongSignatureAndMalformedTokens() {
        assertThrows(JwtException.class, () -> decoder.decode(token("a-different-secret-at-least-32-characters", "energy-api", "energy-auth", Instant.now().plusSeconds(3600))));
        assertThrows(JwtException.class, () -> decoder.decode("invalid.jwt.value"));
    }

    @Test
    void rejectsTokenAfterItsSignatureIsChanged() {
        String[] parts = token(SECRET, "energy-api", "energy-auth", Instant.now().plusSeconds(3600)).split("\\.");
        char first = parts[2].charAt(0);
        parts[2] = (first == 'A' ? "B" : "A") + parts[2].substring(1);

        assertThrows(JwtException.class, () -> decoder.decode(String.join(".", parts)));
    }

    @Test
    void rejectsInvalidSubjectRoleAndFutureIssuedAt() {
        var encoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();
        assertThrows(JwtException.class, () -> decoder.decode(tokenWithClaims(encoder, "0", "CLIENT", Instant.now().minusSeconds(60))));
        assertThrows(JwtException.class, () -> decoder.decode(tokenWithClaims(encoder, "42", "OWNER", Instant.now().minusSeconds(60))));
        assertThrows(JwtException.class, () -> decoder.decode(tokenWithClaims(encoder, "42", "CLIENT", Instant.now().plusSeconds(600))));
    }

    private String tokenWithClaims(NimbusJwtEncoder encoder, String subject, String role, Instant issuedAt) {
        var claims = JwtClaimsSet.builder().subject(subject).issuer("energy-auth").audience(List.of("energy-api"))
                .issuedAt(issuedAt).expiresAt(Instant.now().plusSeconds(3600)).claim("role", role).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
