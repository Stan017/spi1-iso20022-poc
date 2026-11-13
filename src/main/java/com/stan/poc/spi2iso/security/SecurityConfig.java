package com.stan.poc.spi2iso.security;

import com.stan.poc.spi2iso.security.jwt.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

/**
 * Configuracion de Spring Security del PoC.
 *
 * Reglas (rutas con nomenclatura de Service Domain BIAN - ver paquete controller.bian):
 *   - POST /oauth/token                                      -> publico (emite el token)
 *   - /h2-console/**                                         -> publico (solo para inspeccionar
 *                                                                la demo localmente; NUNCA se
 *                                                                expondria asi en un entorno real)
 *   - POST /payment-execution/payment-instructions/**        -> scope "transferencias:write"
 *                                                                (Behavior Qualifier: Initiate)
 *   - GET  /payment-execution/payment-instructions/**        -> scope "transferencias:read"
 *                                                                (Behavior Qualifier: Retrieve)
 *   - GET  /clearing-and-settlement/settlement-status/**     -> scope "transferencias:read"
 *                                                                (Behavior Qualifier: Retrieve)
 *   - cualquier otra ruta                                     -> requiere autenticacion
 *
 * Sin estado de sesion (stateless): cada request se autentica por su propio Bearer
 * token, como corresponde a una API maquina-a-maquina. CSRF deshabilitado porque no
 * hay sesiones ni cookies de autenticacion que proteger.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // necesario para /h2-console
                .exceptionHandling(eh -> eh.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/oauth/token").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // Sin esto, un error (ej. 415) en una ruta publica se redispacha a /error,
                        // que por defecto exige autenticacion, y termina mostrandose como un 401
                        // generico que oculta el codigo/causa real del error.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/payment-execution/payment-instructions/**")
                            .hasAuthority("SCOPE_transferencias:write")
                        .requestMatchers(HttpMethod.GET, "/payment-execution/payment-instructions/**")
                            .hasAuthority("SCOPE_transferencias:read")
                        .requestMatchers(HttpMethod.GET, "/clearing-and-settlement/settlement-status/**")
                            .hasAuthority("SCOPE_transferencias:read")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
