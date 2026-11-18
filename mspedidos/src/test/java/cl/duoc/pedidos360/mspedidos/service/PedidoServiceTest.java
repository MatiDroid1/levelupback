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

    private static final String ACTOR = "admin@example.com";

    private PedidoRepository repo;
    private PedidoEventPublisher publisher;
    private PedidoKafkaPublisher kafkaPublisher;
    private PedidoService service;

    @BeforeEach
    void preparar() {
        repo = mock(PedidoRepository.class);
        publisher = mock(PedidoEventPublisher.class);
        kafkaPublisher = mock(PedidoKafkaPublisher.class);

        service = new PedidoService(
                repo,
                publisher,
                kafkaPublisher
        );
    }

    private Pedido prepararPedido(String estado) {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuario("cliente@example.com");
        pedido.setEstado(estado);

        when(repo.findById(1L)).thenReturn(Optional.of(pedido));
        when(repo.save(pedido)).thenReturn(pedido);

        return pedido;
    }

    @Test
    void permiteElCicloCompletoEnOrden() {
        Pedido pedido = prepararPedido("CREADO");
        String anterior = "CREADO";

        for (String estado : new String[]{
                "ACEPTADO",
                "EN_PREPARACIÓN",
                "DESPACHADO",
                "ENTREGADO"}) {

            Pedido actualizado = service
                    .cambiarEstado(1L, estado, ACTOR)
                    .orElseThrow();

            assertEquals(estado, actualizado.getEstado());

            verify(kafkaPublisher).publicarCambioEstado(
                    pedido,
                    anterior,
                    ACTOR
            );

            anterior = estado;
        }

        verify(repo, times(4)).save(pedido);

        verify(publisher, times(1))
                .solicitarDescuentoStock(pedido);

        verifyNoMoreInteractions(publisher, kafkaPublisher);
    }

    @Test
    void rechazaDespacharSinAceptar() {
        Pedido pedido = prepararPedido("CREADO");

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado(
                        1L,
                        "DESPACHADO",
                        ACTOR
                )
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("CREADO", pedido.getEstado());

        verify(repo, never()).save(any(Pedido.class));
        verifyNoInteractions(publisher, kafkaPublisher);
    }

    @Test
    void permiteCancelarPedidoCreado() {
        Pedido pedido = prepararPedido("CREADO");

        Pedido resultado = service
                .cambiarEstado(1L, "CANCELADO", ACTOR)
                .orElseThrow();

        assertEquals("CANCELADO", resultado.getEstado());

        verify(repo).save(pedido);

        verify(kafkaPublisher).publicarCambioEstado(
                pedido,
                "CREADO",
                ACTOR
        );

        verifyNoInteractions(publisher);
    }

    @Test
    void rechazaModificarPedidoEntregado() {
        Pedido pedido = prepararPedido("ENTREGADO");

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado(
                        1L,
                        "CREADO",
                        ACTOR
                )
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("ENTREGADO", pedido.getEstado());

        verify(repo, never()).save(any(Pedido.class));
        verifyNoInteractions(publisher, kafkaPublisher);
    }

    @Test
    void repetirElEstadoNoVuelveAGuardarNiPublicar() {
        Pedido pedido = prepararPedido("ACEPTADO");

        Pedido resultado = service
                .cambiarEstado(1L, "ACEPTADO", ACTOR)
                .orElseThrow();

        assertSame(pedido, resultado);

        verify(repo, never()).save(any(Pedido.class));
        verifyNoInteractions(publisher, kafkaPublisher);
    }

    @Test
    void pedidoInexistenteDevuelveOptionalVacio() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertTrue(
                service.cambiarEstado(
                        99L,
                        "ACEPTADO",
                        ACTOR
                ).isEmpty()
        );

        verify(repo, never()).save(any(Pedido.class));
        verifyNoInteractions(publisher, kafkaPublisher);
    }

    @Test
    void rechazaEstadoVacio() {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado(1L, " ", ACTOR)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repo, publisher, kafkaPublisher);
    }

    @Test
    void rechazaActorVacio() {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado(
                        1L,
                        "ACEPTADO",
                        " "
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repo, publisher, kafkaPublisher);
    }
}