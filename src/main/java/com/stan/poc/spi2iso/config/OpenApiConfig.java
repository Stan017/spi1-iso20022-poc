package com.stan.poc.spi2iso.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos OpenAPI 3.0 del PoC.
 *
 * El esquema de seguridad "bearerAuth" refleja el flujo real del proyecto:
 * primero POST /oauth/token (client_credentials) para obtener el Bearer token;
 * luego incluirlo en el header Authorization de cada llamada a la API de pagos.
 *
 * Para probarlo desde Swagger UI:
 *   1. Ejecutar POST /oauth/token con los valores del application.properties de demo.
 *   2. Copiar el access_token de la respuesta.
 *   3. Usar el boton "Authorize" (candado) de Swagger UI: ingresar "Bearer <token>".
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "SPI1 → ISO 20022 Migration API",
                version = "0.1.0",
                description = """
                        PoC técnico: motor de transformación de archivos **SPI1** (Banco Central del Ecuador) \
                        hacia mensajería **ISO 20022** (pacs.008 / pacs.002), conforme a la \
                        Resolución BCE-GG-007-2026 y la Circular BCE-GMPSN-2026-008-C.

                        Las rutas REST siguen la nomenclatura **BIAN v12** de Service Domain / Behavior Qualifier:
                        - **Payment Execution** (`/payment-execution/payment-instructions/...`): \
                          Behavior Qualifiers Initiate y Retrieve.
                        - **Clearing and Settlement** (`/clearing-and-settlement/settlement-status/...`): \
                          Behavior Qualifier Retrieve.

                        > **Nota honesta**: este es un PoC de portafolio para evidencia técnica, no software \
                        de producción. Ver README.md para el detalle completo de supuestos y límites.
                        """,
                contact = @Contact(name = "Stan", email = "devopstanley@gmail.com"),
                license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")
        ),
        servers = @Server(url = "http://localhost:8080", description = "Local dev")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT obtenido via POST /oauth/token (grant_type=client_credentials). " +
                "Demo: client_id=banco-pichincha-poc-client / client_secret=demo-secret-cambiar-en-uso-real"
)
public class OpenApiConfig {
}
