package cl.duoc.pedidos360.mspedidos.controller;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import cl.duoc.pedidos360.mspedidos.dto.CrearPedidoRequest;
import cl.duoc.pedidos360.mspedidos.model.DetallePedido;

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
            @RequestBody CrearPedidoRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String usuario = usuarioAutenticado(jwt);

        if (request == null
                || request.detalles() == null
                || request.detalles().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El pedido debe contener productos");
        }

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);

        List<DetallePedido> detalles = new ArrayList<>();
        long total = 0;

        for (CrearPedidoRequest.Item item : request.detalles()) {
            if (item == null
                    || item.productoId() == null
                    || item.productoId() <= 0
                    || item.nombreProducto() == null
                    || item.nombreProducto().isBlank()
                    || item.precioUnitario() == null
                    || item.precioUnitario() < 0
                    || item.cantidad() == null
                    || item.cantidad() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Producto, nombre, precio o cantidad inválidos");
            }

            long subtotal = (long) item.precioUnitario() * item.cantidad();

            if (subtotal > Integer.MAX_VALUE
                    || total > Integer.MAX_VALUE - subtotal) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El total supera el máximo admitido");
            }

            total += subtotal;

            DetallePedido detalle = new DetallePedido();
            detalle.setProductoId(item.productoId());
            detalle.setNombreProducto(item.nombreProducto().trim());
            detalle.setPrecioUnitario(item.precioUnitario());
            detalle.setCantidad(item.cantidad());
            detalles.add(detalle);
        }

        pedido.setDetalles(detalles);
        return service.crear(pedido);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Pedido> cambiarEstado(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {

        if (!esAdmin(jwt)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un administrador puede cambiar el estado");
        }

        String estado = body == null ? null : body.get("estado");

        if (estado == null || estado.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes indicar el estado");
        }

        String actor = usuarioAutenticado(jwt);

        return service.cambiarEstado(id, estado.trim(), actor)
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