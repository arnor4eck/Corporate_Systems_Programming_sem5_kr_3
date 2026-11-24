package com.arnor4eck.pks_3.security;

import com.arnor4eck.pks_3.domain.RefreshSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Component
public class TokenService {
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    public final long accessSeconds;
    public final long refreshSeconds;

    public TokenService(@Value("${app.auth.jwt-secret:}") String secret,
                        @Value("${app.auth.access-seconds:900}") long accessSeconds,
                        @Value("${app.auth.refresh-seconds:604800}") long refreshSeconds) {
        byte[] bytes;
        if (secret.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = Base64.getDecoder().decode(secret);
            if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET must contain at least 32 random bytes in Base64");
        }
        if (accessSeconds < 1 || refreshSeconds <= accessSeconds) throw new IllegalArgumentException("Invalid token lifetimes");
        var key = new SecretKeySpec(bytes, "HmacSHA256");
        encoder = NimbusJwtEncoder.withSecretKey(key).build();
        var jwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("pks_3"));
        decoder = jwtDecoder;
        this.accessSeconds = accessSeconds;
        this.refreshSeconds = refreshSeconds;
    }

    public String access(RefreshSession session) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("pks_3").subject(session.getEmployee().getId().toString())
                .issuedAt(now).expiresAt(now.plusSeconds(accessSeconds)).claim("sid", session.getId().toString()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public Jwt decode(String token) { return decoder.decode(token); }

    public static String randomToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
