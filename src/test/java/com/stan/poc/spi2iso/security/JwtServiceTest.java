package com.stan.poc.spi2iso.security;

import com.stan.poc.spi2iso.security.jwt.JwtService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de humo del emisor/validador de JWT de demo (sin contexto de Spring).
 */
class JwtServiceTest {

    private static final String SECRETO = "este-es-un-secreto-de-prueba-de-al-menos-32-caracteres";

    private JwtService nuevoServicio(long ttlSeconds) {
        return new JwtService(SECRETO, "issuer-test", ttlSeconds);
    }

    @Test
    void emiteYValidaUnTokenConSusScopes() {
        JwtService service = nuevoServicio(900);
        String token = service.issueAccessToken("poc-demo-client",
                List.of("transferencias:read", "transferencias:write"));

        JwtService.JwtClaims claims = service.validate(token);

        assertEquals("poc-demo-client", claims.subject());
        assertTrue(claims.hasScope("transferencias:read"));
        assertTrue(claims.hasScope("transferencias:write"));
        assertFalse(claims.hasScope("algo:inexistente"));
    }

    @Test
    void rechazaUnTokenExpirado() throws InterruptedException {
        JwtService service = nuevoServicio(0); // expira inmediatamente
        String token = service.issueAccessToken("cliente", List.of("transferencias:read"));
        Thread.sleep(1100); // asegura que el segundo "exp" ya quedo atras

        assertThrows(JwtService.JwtValidationException.class, () -> service.validate(token));
    }

    @Test
    void rechazaUnTokenConFirmaAlterada() {
        JwtService service = nuevoServicio(900);
        String token = service.issueAccessToken("cliente", List.of("transferencias:read"));
        String tokenAlterado = token.substring(0, token.length() - 2) + "xx";

        assertThrows(JwtService.JwtValidationException.class, () -> service.validate(tokenAlterado));
    }

    @Test
    void rechazaUnTokenFirmadoConOtroSecreto() {
        JwtService emisor = new JwtService(SECRETO, "issuer-test", 900);
        JwtService validadorConOtroSecreto =
                new JwtService("otro-secreto-totalmente-distinto-de-32-caracteres", "issuer-test", 900);

        String token = emisor.issueAccessToken("cliente", List.of("transferencias:read"));

        assertThrows(JwtService.JwtValidationException.class,
                () -> validadorConOtroSecreto.validate(token));
    }

    @Test
    void rechazaUnTokenMalformado() {
        JwtService service = nuevoServicio(900);
        assertThrows(JwtService.JwtValidationException.class, () -> service.validate("no-es-un-jwt"));
    }
}
