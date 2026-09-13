package cl.duoc.pedidos360.msproductos.config;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configura msproductos como un OAuth2 Resource Server: valida el JWT
 * emitido por Azure AD (Entra ID) en cada peticion antes de dejarla pasar
 * a los controllers. El issuer-uri y audiences se leen desde
 * application.properties.
 *
 * GET /productos es publico (catalogo visible sin login, usado en la
 * pagina de inicio y en /catalogo). POST y DELETE requieren autenticacion
 * Y el rol admin (APPROLE_admin), leido desde el claim "roles" que Azure AD
 * incluye en el JWT segun los App Roles asignados al usuario.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/productos", "/productos/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/productos", "/productos/**").hasAuthority("APPROLE_admin")
                .requestMatchers(HttpMethod.DELETE, "/productos", "/productos/**").hasAuthority("APPROLE_admin")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * Azure AD (Entra ID) entrega los App Roles asignados al usuario en el
     * claim "roles" del JWT, como un array de strings (ej: ["admin"]).
     * Spring Security no sabe leer ese claim por defecto: hay que decirle
     * explicitamente que lo convierta en GrantedAuthority con el prefijo
     * APPROLE_, que es la convencion usada en los matchers de arriba.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extraerAuthoritiesDesdeRoles);
        return converter;
    }

    private Collection<GrantedAuthority> extraerAuthoritiesDesdeRoles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
            .map(rol -> new SimpleGrantedAuthority("APPROLE_" + rol))
            .collect(Collectors.toList());
    }
}