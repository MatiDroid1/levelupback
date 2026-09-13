package cl.duoc.pedidos360.mspedidos.config;
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

/**
 * Configura mspedidos como un OAuth2 Resource Server: valida el JWT emitido
 * por Azure AD (Entra ID) en cada peticion antes de dejarla pasar a los
 * controllers. El issuer-uri y audiences se leen desde application.properties.
 *
 * No reemplaza a CorsConfig.java: ambas configuraciones conviven, y aqui
 * se reutiliza el CorsConfigurationSource ya definido en CorsConfig
 * (Spring Security lo detecta automaticamente via .cors(Customizer.withDefaults())).
 *
 * Reglas de autorizacion (segun README del proyecto):
 * - GET /pedidos            (todos los pedidos)         -> solo admin
 * - GET /pedidos?usuario=.. (pedidos de un usuario)      -> cualquier autenticado
 *                                                           (el controller debe
 *                                                           forzar usuario = propio
 *                                                           si el rol es cliente)
 * - GET /pedidos/{id}       (detalle)                    -> cualquier autenticado
 * - POST /pedidos           (crear pedido)                -> cualquier autenticado
 * - PATCH /pedidos/{id}/estado (cambiar estado)           -> solo admin
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
                // Cambio de estado: solo admin.
                .requestMatchers(HttpMethod.PATCH, "/pedidos/*/estado").hasAuthority("APPROLE_admin")
                // Listar TODOS los pedidos (sin filtro ?usuario=): solo admin.
                // Como Spring no distingue por query param en requestMatchers,
                // esta regla de "todos vs propios" se refuerza en el controller
                // (ver PedidoController): si el rol no es admin, se ignora
                // cualquier ?usuario= recibido y se usa el username del JWT.
                .requestMatchers(HttpMethod.GET, "/pedidos", "/pedidos/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/pedidos").authenticated()
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