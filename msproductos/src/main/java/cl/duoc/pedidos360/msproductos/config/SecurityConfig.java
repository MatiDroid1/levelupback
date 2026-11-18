package cl.duoc.pedidos360.msproductos.config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth

                // Documentacion OpenAPI y Swagger UI
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml"
                ).permitAll()

                // Estado del servicio
                .requestMatchers("/actuator/health").permitAll()

                // Auditoria: solo administrador
                .requestMatchers(
                    "/api/auditoria",
                    "/api/auditoria/**"
                ).hasAuthority("APPROLE_admin")

                // Catalogo publico
                .requestMatchers(
                    HttpMethod.GET,
                    "/productos",
                    "/productos/**"
                ).permitAll()

                // Crear productos: solo administrador
                .requestMatchers(
                    HttpMethod.POST,
                    "/productos",
                    "/productos/**"
                ).hasAuthority("APPROLE_admin")

                // Eliminar productos: solo administrador
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/productos",
                    "/productos/**"
                ).hasAuthority("APPROLE_admin")

                // Cualquier otra ruta requiere autenticacion
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(
                        jwtAuthenticationConverter()
                    )
                )
            );

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                this::extraerAuthoritiesDesdeRoles
        );

        return converter;
    }

    private Collection<GrantedAuthority> extraerAuthoritiesDesdeRoles(
            Jwt jwt) {

        List<String> roles = jwt.getClaimAsStringList("roles");

        if (roles == null) {
            return List.of();
        }

        return roles.stream()
                .map(rol ->
                    new SimpleGrantedAuthority("APPROLE_" + rol))
                .collect(Collectors.toList());
    }
}