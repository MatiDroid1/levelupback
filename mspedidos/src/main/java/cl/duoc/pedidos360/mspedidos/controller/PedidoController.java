package cl.duoc.pedidos360.mspedidos.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.pedidos360.mspedidos.model.Pedido;
import cl.duoc.pedidos360.mspedidos.service.PedidoService;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Pedido> listar(
            @RequestParam(name = "usuario", required = false) String usuario,
            @AuthenticationPrincipal Jwt jwt) {

        if (esAdmin(jwt)) {
            return usuario == null || usuario.isBlank()
                    ? service.listarTodos()
                    : service.listarPorUsuario(usuario.trim());
        }

        // Un cliente no puede elegir otra identidad mediante el parámetro.
        return service.listarPorUsuario(usuarioAutenticado(jwt));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtener(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal Jwt jwt) {

        Pedido pedido = service.obtener(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        if (!esAdmin(jwt)) {
            String usuario = usuarioAutenticado(jwt);

            if (!usuario.equalsIgnoreCase(pedido.getUsuario())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No tienes permiso para consultar este pedido");
            }
        }

        return ResponseEntity.ok(pedido);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pedido crear(
            @RequestBody Pedido pedido,
            @AuthenticationPrincipal Jwt jwt) {

        // El propietario proviene del JWT validado, no del navegador.
        pedido.setUsuario(usuarioAutenticado(jwt));

        return service.crear(pedido);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Pedido> cambiarEstado(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {

        String estado = body.get("estado");

        if (estado == null || estado.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes indicar el estado");
        }

        // SecurityConfig conserva la restricción de administrador.
        return service.cambiarEstado(id, estado.trim())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private boolean esAdmin(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "JWT requerido");
        }

        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("admin");
    }

    private String usuarioAutenticado(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "JWT requerido");
        }

        String usuario = jwt.getClaimAsString("preferred_username");

        if (usuario == null || usuario.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El token no contiene preferred_username");
        }

        return usuario.trim();
    }
}