package com.stan.poc.spi2iso.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Emisor y validador de tokens de acceso para este PoC, con un formato JWT (HS256)
 * minimo, implementado a mano con la libreria estandar del JDK (javax.crypto) y
 * Jackson (ya presente como dependencia transitiva de spring-boot-starter-web).
 *
 * IMPORTANTE - limite de este PoC (ver README, seccion Seguridad): esto NO es un
 * servidor de autorizacion OAuth 2.0 real. No hay rotacion de llaves, no hay JWKS,
 * no hay registro dinamico de clientes, y la llave de firma es simetrica y vive en
 * application.properties. Simula la MECANICA de un flujo client_credentials
 * (emision de access_token con scopes y expiracion, validacion Bearer en cada
 * request) para poder demostrar el patron de arquitectura: en un entorno real del
 * Banco, la emision de tokens la haria un Authorization Server dedicado (ej. Keycloak,
 * Okta, o el IdP corporativo), con JWT firmados asimetricamente (RS256/ES256) y
 * rotacion de llaves vía JWKS - ese componente esta fuera del alcance de un PoC de
 * portafolio.
 */
@Component
public class JwtService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder B64URL_ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64URL_DEC = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String issuer;
    private final long ttlSeconds;
    private final SecretKeySpec signingKey;

    public JwtService(
            @Value("${poc.security.jwt.secret}") String secret,
            @Value("${poc.security.jwt.issuer:spi1-iso20022-poc-authz-sim}") String issuer,
            @Value("${poc.security.jwt.ttl-seconds:900}") long ttlSeconds) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "poc.security.jwt.secret debe tener al menos 32 caracteres (HS256)");
        }
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
        this.signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    /** Emite un access token (JWT HS256) para el client_id dado, con los scopes otorgados. */
    public String issueAccessToken(String clientId, List<String> scopes) {
        Instant now = Instant.now();

        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", issuer);
        claims.put("sub", clientId);
        claims.put("aud", "spi1-iso20022-poc-api");
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plusSeconds(ttlSeconds).getEpochSecond());
        claims.put("scope", String.join(" ", scopes));

        String headerB64 = encodeJson(header);
        String claimsB64 = encodeJson(claims);
        String signingInput = headerB64 + "." + claimsB64;
        String signature = sign(signingInput);

        return signingInput + "." + signature;
    }

    /** Valida la firma y la expiracion de un token, y devuelve sus claims. Lanza JwtValidationException si no es valido. */
    public JwtClaims validate(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtValidationException("Token vacio");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtValidationException("Formato de token invalido (se esperaban 3 segmentos)");
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = sign(signingInput);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new JwtValidationException("Firma invalida");
        }

        Map<String, Object> claims;
        try {
            claims = objectMapper.readValue(B64URL_DEC.decode(parts[1]), Map.class);
        } catch (Exception e) {
            throw new JwtValidationException("No se pudo decodificar el payload: " + e.getMessage());
        }

        long exp = ((Number) claims.getOrDefault("exp", 0)).longValue();
        if (Instant.now().getEpochSecond() > exp) {
            throw new JwtValidationException("Token expirado");
        }

        if (!issuer.equals(claims.get("iss"))) {
            throw new JwtValidationException("Emisor (iss) no reconocido");
        }

        String subject = String.valueOf(claims.get("sub"));
        String scopeClaim = String.valueOf(claims.getOrDefault("scope", ""));
        List<String> scopes = scopeClaim.isBlank() ? List.of() : List.of(scopeClaim.split("\\s+"));

        return new JwtClaims(subject, scopes, exp);
    }

    private String sign(String input) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(signingKey);
            byte[] raw = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return B64URL_ENC.encodeToString(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Error firmando el token: " + e.getMessage(), e);
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return B64URL_ENC.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception e) {
            throw new IllegalStateException("Error serializando claims a JSON: " + e.getMessage(), e);
        }
    }

    /** Comparacion en tiempo constante para evitar timing attacks al validar la firma. */
    private boolean constantTimeEquals(String a, String b) {
        byte[] ba = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ba.length != bb.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < ba.length; i++) {
            result |= ba[i] ^ bb[i];
        }
        return result == 0;
    }

    public record JwtClaims(String subject, List<String> scopes, long expiresAtEpochSecond) {
        public boolean hasScope(String scope) {
            return scopes.contains(scope);
        }
    }

    public static class JwtValidationException extends RuntimeException {
        public JwtValidationException(String message) {
            super(message);
        }
    }
}
