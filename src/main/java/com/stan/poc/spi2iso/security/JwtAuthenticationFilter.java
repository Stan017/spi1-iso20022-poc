package com.stan.poc.spi2iso.security;

import com.stan.poc.spi2iso.security.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que valida el header "Authorization: Bearer <token>" en cada request a
 * /api/**, usando JwtService, y puebla el SecurityContext con una autenticacion
 * cuyos GrantedAuthority son "SCOPE_<scope>" por cada scope del token - el mismo
 * convenio que usa spring-boot-starter-oauth2-resource-server, para que las reglas
 * de autorizacion en SecurityConfig (hasAuthority("SCOPE_...")) sean legibles.
 *
 * Si el token falta, es invalido o esta expirado, NO rechaza la request aqui: deja
 * que llegue sin autenticacion al punto de entrada de Spring Security, que la
 * rechazara con 401 segun las reglas de SecurityConfig. Esto evita filtrar detalles
 * de por que fallo la validacion (manejo de errores centralizado y consistente).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length()).trim();
            try {
                JwtService.JwtClaims claims = jwtService.validate(token);

                List<GrantedAuthority> authorities = claims.scopes().stream()
                        .map(scope -> (GrantedAuthority) new SimpleGrantedAuthority("SCOPE_" + scope))
                        .toList();

                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.subject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtService.JwtValidationException e) {
                // Token invalido/expirado: se deja sin autenticar; SecurityConfig decide el 401.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
