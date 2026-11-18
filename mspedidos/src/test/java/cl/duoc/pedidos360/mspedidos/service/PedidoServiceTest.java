package cl.duoc.pedidos360.mspedidos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.pedidos360.mspedidos.messaging.producer.PedidoEventPublisher;
import cl.duoc.pedidos360.mspedidos.messaging.producer.PedidoKafkaPublisher;
import cl.duoc.pedidos360.mspedidos.model.Pedido;
import cl.duoc.pedidos360.mspedidos.repository.PedidoRepository;

class PedidoServiceTest {

    private PedidoRepository repo;
    private PedidoService service;

    @BeforeEach
    void preparar() {
        repo = mock(PedidoRepository.class);

        service = new PedidoService(
                repo,
                mock(PedidoEventPublisher.class),
                mock(PedidoKafkaPublisher.class));
    }

    private Pedido prepararPedido(String estado) {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setEstado(estado);

        when(repo.findById(1L)).thenReturn(Optional.of(pedido));
        when(repo.save(pedido)).thenReturn(pedido);

        return pedido;
    }

    @Test
    void permiteElCicloCompletoEnOrden() {
        Pedido pedido = prepararPedido("CREADO");

        for (String estado : new String[]{
                "ACEPTADO",
                "EN_PREPARACIÓN",
                "DESPACHADO",
                "ENTREGADO"}) {

            Pedido actualizado = service.cambiarEstado(1L, estado)
                    .orElseThrow();

            assertEquals(estado, actualizado.getEstado());
        }

        verify(repo, times(4)).save(pedido);
    }

    @Test
    void rechazaDespacharSinAceptar() {
        Pedido pedido = prepararPedido("CREADO");

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado(1L, "DESPACHADO"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("CREADO", pedido.getEstado());
        verify(repo, never()).save(any(Pedido.class));
    }

    @Test
    void permiteCancelarPedidoCreado() {
        prepararPedido("CREADO");

        Pedido resultado = service.cambiarEstado(1L, "CANCELADO")
                .orElseThrow();

        assertEquals("CANCELADO", resultado.getEstado());
    }

    @Test
    void rechazaModificarPedidoEntregado() {
        prepararPedido("ENTREGADO");

        assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado(1L, "CREADO"));

        verify(repo, never()).save(any(Pedido.class));
    }

    @Test
    void repetirElEstadoNoVuelveAGuardar() {
        Pedido pedido = prepararPedido("ACEPTADO");

        Pedido resultado = service.cambiarEstado(1L, "ACEPTADO")
                .orElseThrow();

        assertSame(pedido, resultado);
        verify(repo, never()).save(any(Pedido.class));
    }

    @Test
    void pedidoInexistenteDevuelveOptionalVacio() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertTrue(service.cambiarEstado(99L, "ACEPTADO").isEmpty());
        verify(repo, never()).save(any(Pedido.class));
    }
}