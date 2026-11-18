package cl.duoc.pedidos360.mskafkaadmin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http)
                        throws Exception {

                JwtGrantedAuthoritiesConverter rolesConverter = new JwtGrantedAuthoritiesConverter();

                rolesConverter.setAuthoritiesClaimName("roles");
                rolesConverter.setAuthorityPrefix("APPROLE_");

                JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();

                jwtConverter.setJwtGrantedAuthoritiesConverter(rolesConverter);

                http
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Documentación pública.
                                                .requestMatchers(
                                                                "/swagger-ui.html",
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs",
                                                                "/v3/api-docs/**")
                                                .permitAll()

                                                // El resto del servicio mantiene el acceso exclusivo de administrador.
                                                .anyRequest().hasAuthority("APPROLE_admin"))
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt
                                                                .jwtAuthenticationConverter(jwtConverter)));

                return http.build();
        }
}