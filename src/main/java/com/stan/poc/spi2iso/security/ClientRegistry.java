package com.stan.poc.spi2iso.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registro de clientes OAuth2 "client_credentials" para este PoC.
 *
 * En un entorno real del Banco, esto viviria en el Authorization Server corporativo
 * (con secretos hasheados, rotacion, y administracion propia). Aqui es un registro
 * en memoria con un unico cliente de demo, configurable via application.properties,
 * para poder mostrar el flujo completo sin levantar infraestructura adicional.
 */
@Component
public class ClientRegistry {

    private final Map<String, RegisteredClient> clientes;

    public ClientRegistry(
            @Value("${poc.security.oauth.demo-client-id}") String demoClientId,
            @Value("${poc.security.oauth.demo-client-secret}") String demoClientSecret) {
        RegisteredClient demo = new RegisteredClient(
                demoClientId,
                demoClientSecret,
                List.of("transferencias:read", "transferencias:write"));
        this.clientes = Map.of(demo.clientId(), demo);
    }

    public Optional<RegisteredClient> authenticate(String clientId, String clientSecret) {
        RegisteredClient client = clientes.get(clientId);
        if (client == null || clientSecret == null || !clientSecret.equals(client.clientSecret())) {
            return Optional.empty();
        }
        return Optional.of(client);
    }

    public record RegisteredClient(String clientId, String clientSecret, List<String> scopesPermitidos) {
    }
}
