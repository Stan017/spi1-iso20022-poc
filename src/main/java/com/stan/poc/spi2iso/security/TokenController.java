package com.stan.poc.spi2iso.security;

import com.stan.poc.spi2iso.security.jwt.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Simula el endpoint de token de un Authorization Server OAuth 2.0, flujo
 * "client_credentials" (RFC 6749, seccion 4.4) - el flujo tipico para integraciones
 * sistema-a-sistema como esta (SPI/switch de pagos), a diferencia de un flujo con
 * usuario humano (authorization_code).
 *
 * Ver JwtService y README.md (seccion Seguridad) para el alcance y los limites de
 * esta simulacion frente a un Authorization Server real.
 */
@Tag(name = "Authorization (OAuth2 simulado)",
        description = "Simula un Authorization Server OAuth 2.0 con flujo `client_credentials` (RFC 6749 §4.4). " +
                "Usar primero para obtener el Bearer token que las demás rutas requieren.")
@RestController
public class TokenController {

    private final ClientRegistry clientRegistry;
    private final JwtService jwtService;
    private final long ttlSeconds;

    public TokenController(ClientRegistry clientRegistry,
                            JwtService jwtService,
                            @Value("${poc.security.jwt.ttl-seconds:900}") long ttlSeconds) {
        this.clientRegistry = clientRegistry;
        this.jwtService = jwtService;
        this.ttlSeconds = ttlSeconds;
    }

    @Operation(
            summary = "Emitir token OAuth2 (client_credentials)",
            description = """
                    Emite un Bearer token JWT (HMAC-SHA256, TTL configurable, por defecto 900 s).

                    Credenciales de demo (definidas en `application.properties`):
                    - `client_id`: `banco-pichincha-poc-client`
                    - `client_secret`: `demo-secret-cambiar-en-uso-real`
                    - `grant_type`: `client_credentials`
                    - `scope`: `transferencias:read transferencias:write` (ambos, o solo uno)

                    El token resultante incluye los scopes como claims y debe enviarse como \
                    `Authorization: Bearer <token>` en las demás rutas.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Token emitido — `{ access_token, token_type, expires_in, scope }`",
                            content = @Content(schema = @Schema(type = "object",
                                    example = "{\"access_token\":\"eyJ...\",\"token_type\":\"Bearer\"," +
                                            "\"expires_in\":900,\"scope\":\"transferencias:read transferencias:write\"}"))),
                    @ApiResponse(responseCode = "400",
                            description = "grant_type no soportado o scope inválido",
                            content = @Content(schema = @Schema(type = "object",
                                    example = "{\"error\":\"unsupported_grant_type\"}"))),
                    @ApiResponse(responseCode = "401",
                            description = "client_id o client_secret incorrectos",
                            content = @Content(schema = @Schema(type = "object",
                                    example = "{\"error\":\"invalid_client\"}")))
            }
    )
    @PostMapping(value = "/oauth/token", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<?> token(
            @Parameter(description = "Debe ser `client_credentials`", example = "client_credentials")
            @RequestParam("grant_type") String grantType,
            @Parameter(description = "ID del cliente OAuth2", example = "banco-pichincha-poc-client")
            @RequestParam("client_id") String clientId,
            @Parameter(description = "Secreto del cliente OAuth2", example = "demo-secret-cambiar-en-uso-real")
            @RequestParam("client_secret") String clientSecret,
            @Parameter(description = "Scopes solicitados (space-delimited)",
                    example = "transferencias:read transferencias:write")
            @RequestParam(value = "scope", required = false) String requestedScope) {

        if (!"client_credentials".equals(grantType)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "unsupported_grant_type",
                            "error_description", "Este PoC solo soporta client_credentials"));
        }

        var clienteOpt = clientRegistry.authenticate(clientId, clientSecret);
        if (clienteOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_client",
                            "error_description", "client_id o client_secret invalidos"));
        }

        var cliente = clienteOpt.get();
        List<String> scopesOtorgados = resolverScopes(requestedScope, cliente.scopesPermitidos());
        if (scopesOtorgados.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "invalid_scope",
                            "error_description", "Ninguno de los scopes solicitados esta permitido para este cliente"));
        }

        String accessToken = jwtService.issueAccessToken(cliente.clientId(), scopesOtorgados);

        return ResponseEntity.ok(Map.of(
                "access_token", accessToken,
                "token_type", "Bearer",
                "expires_in", ttlSeconds,
                "scope", String.join(" ", scopesOtorgados)
        ));
    }

    private List<String> resolverScopes(String requested, List<String> permitidos) {
        if (requested == null || requested.isBlank()) {
            return permitidos;
        }
        return List.of(requested.split("\\s+")).stream()
                .filter(permitidos::contains)
                .toList();
    }
}
