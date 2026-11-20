package cl.duoc.pedidos360.mspedidos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.pedidos360.mspedidos.dto.NotificacionResponse;
import cl.duoc.pedidos360.mspedidos.service.NotificacionService;

@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionService service;

    public NotificacionController(
            NotificacionService service) {

        this.service = service;
    }

    @GetMapping
    public List<NotificacionResponse> listar(
            @AuthenticationPrincipal Jwt jwt) {

        return service.listar(usuarioAutenticado(jwt));
    }

    @PatchMapping("/{id}/leida")
    public NotificacionResponse marcarLeida(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal Jwt jwt) {

        return service.marcarLeida(
                id,
                usuarioAutenticado(jwt)
        );
    }

    private String usuarioAutenticado(Jwt jwt) {

        if (jwt == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "JWT requerido"
            );
        }

        String usuario = jwt.getClaimAsString(
                "preferred_username"
        );

        if (usuario == null || usuario.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El token no contiene preferred_username"
            );
        }

        return usuario.trim();
    }
}