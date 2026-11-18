package cl.duoc.pedidos360.mspedidos.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.pedidos360.mspedidos.dto.CrearPedidoRequest;
import cl.duoc.pedidos360.mspedidos.model.Pedido;
import cl.duoc.pedidos360.mspedidos.service.PedidoService;

class PedidoControllerTest {

    private PedidoService service;
    private PedidoController controller;

    @BeforeEach
    void preparar() {
        service = mock(PedidoService.class);
        controller = new PedidoController(service);
    }

    private Jwt token(String usuario, String rol) {
        return Jwt.withTokenValue("token-solo-para-test")
                .header("alg", "none")
                .subject(usuario)
                .claim("preferred_username", usuario)
                .claim("roles", List.of(rol))
                .build();
    }

    @Test
    void clienteSoloListaSusPedidosAunquePidaOtroUsuario() {
        when(service.listarPorUsuario("clienteA@ejemplo.cl"))
                .thenReturn(List.of());

        controller.listar(
                "clienteB@ejemplo.cl",
                token("clienteA@ejemplo.cl", "cliente"));

        verify(service).listarPorUsuario("clienteA@ejemplo.cl");
        verify(service, never()).listarTodos();
        verify(service, never())
                .listarPorUsuario("clienteB@ejemplo.cl");
    }

    @Test
    void administradorPuedeListarTodos() {
        when(service.listarTodos()).thenReturn(List.of());

        controller.listar(
                null,
                token("admin@ejemplo.cl", "admin"));

        verify(service).listarTodos();
    }

    @Test
    void clienteNoPuedeConsultarPedidoAjeno() {
        Pedido pedido = new Pedido();
        pedido.setUsuario("clienteB@ejemplo.cl");

        when(service.obtener(10L)).thenReturn(Optional.of(pedido));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.obtener(
                        10L,
                        token("clienteA@ejemplo.cl", "cliente")));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void clientePuedeConsultarSuPedido() {
        Pedido pedido = new Pedido();
        pedido.setUsuario("clienteA@ejemplo.cl");

        when(service.obtener(10L)).thenReturn(Optional.of(pedido));

        var respuesta = controller.obtener(
                10L,
                token("clienteA@ejemplo.cl", "cliente"));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(pedido, respuesta.getBody());
    }

    @Test
    void administradorPuedeConsultarPedidoDeOtroUsuario() {
        Pedido pedido = new Pedido();
        pedido.setUsuario("clienteB@ejemplo.cl");

        when(service.obtener(10L)).thenReturn(Optional.of(pedido));

        var respuesta = controller.obtener(
                10L,
                token("admin@ejemplo.cl", "admin"));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(pedido, respuesta.getBody());
    }

    @Test
    void crearUsaIdentidadDelTokenYEntidadesNuevas() {
        CrearPedidoRequest request = new CrearPedidoRequest(List.of(
                new CrearPedidoRequest.Item(
                        1L,
                        "Teclado",
                        10000,
                        2)));

        when(service.crear(any(Pedido.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Pedido resultado = controller.crear(
                request,
                token("clienteA@ejemplo.cl", "cliente"));

        assertEquals("clienteA@ejemplo.cl", resultado.getUsuario());
        assertNull(resultado.getId());
        assertNull(resultado.getEstado());
        assertNull(resultado.getFecha());
        assertNull(resultado.getTotal());

        assertEquals(1, resultado.getDetalles().size());

        var detalle = resultado.getDetalles().get(0);

        assertNull(detalle.getId());
        assertNull(detalle.getSubtotal());
        assertEquals(Long.valueOf(1L), detalle.getProductoId());
        assertEquals("Teclado", detalle.getNombreProducto());
        assertEquals(Integer.valueOf(10000), detalle.getPrecioUnitario());
        assertEquals(Integer.valueOf(2), detalle.getCantidad());

        verify(service).crear(any(Pedido.class));
    }

    @Test
    void tokenSinIdentidadNoPuedeCrearPedido() {
        Jwt sinUsuario = Jwt.withTokenValue("token-solo-para-test")
                .header("alg", "none")
                .subject("identificador")
                .build();

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.crear(
                        new CrearPedidoRequest(List.of()),
                        sinUsuario));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void rechazaPedidoSinProductos() {
        CrearPedidoRequest request =
                new CrearPedidoRequest(List.of());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.crear(
                        request,
                        token("clienteA@ejemplo.cl", "cliente")));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void rechazaCantidadNegativa() {
        CrearPedidoRequest request = new CrearPedidoRequest(List.of(
                new CrearPedidoRequest.Item(
                        1L,
                        "Teclado",
                        10000,
                        -1)));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.crear(
                        request,
                        token("clienteA@ejemplo.cl", "cliente")));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(service);
    }
}