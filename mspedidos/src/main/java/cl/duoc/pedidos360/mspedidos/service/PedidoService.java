package cl.duoc.pedidos360.mspedidos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.pedidos360.mspedidos.messaging.producer.PedidoEventPublisher;
import cl.duoc.pedidos360.mspedidos.messaging.producer.PedidoKafkaPublisher;
import cl.duoc.pedidos360.mspedidos.model.DetallePedido;
import cl.duoc.pedidos360.mspedidos.model.Pedido;
import cl.duoc.pedidos360.mspedidos.repository.PedidoRepository;

@Service
public class PedidoService {

    private final PedidoRepository repo;
    private final PedidoEventPublisher publisher;
    private final PedidoKafkaPublisher kafkaPublisher;

    public PedidoService(
            PedidoRepository repo,
            PedidoEventPublisher publisher,
            PedidoKafkaPublisher kafkaPublisher) {

        this.repo = repo;
        this.publisher = publisher;
        this.kafkaPublisher = kafkaPublisher;
    }

    public List<Pedido> listarTodos() {
        return repo.findAll();
    }

    public List<Pedido> listarPorUsuario(String usuario) {
        return repo.findByUsuario(usuario);
    }

    public Optional<Pedido> obtener(Long id) {
        return repo.findById(id);
    }

    public Pedido crear(Pedido pedido) {
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado("CREADO");

        int total = 0;

        for (DetallePedido detalle : pedido.getDetalles()) {
            detalle.setSubtotal(
                    detalle.getPrecioUnitario()
                            * detalle.getCantidad()
            );

            total += detalle.getSubtotal();
        }

        pedido.setTotal(total);

        Pedido guardado = repo.save(pedido);

        publisher.publicarPedidoCreado(guardado);
        kafkaPublisher.publicarPedidoCreado(guardado);

        return guardado;
    }

    public Optional<Pedido> cambiarEstado(
            Long id,
            String nuevoEstado,
            String actor) {

        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes indicar el estado"
            );
        }

        if (actor == null || actor.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes identificar al autor del cambio"
            );
        }

        return repo.findById(id).map(pedido -> {
            String actual = pedido.getEstado();

            if (nuevoEstado.equals(actual)) {
                return pedido;
            }

            boolean permitido = actual != null && switch (actual) {
                case "CREADO" ->
                        nuevoEstado.equals("ACEPTADO")
                                || nuevoEstado.equals("CANCELADO");

                case "ACEPTADO" ->
                        nuevoEstado.equals("EN_PREPARACIÓN");

                case "EN_PREPARACIÓN" ->
                        nuevoEstado.equals("DESPACHADO");

                case "DESPACHADO" ->
                        nuevoEstado.equals("ENTREGADO");

                default -> false;
            };

            if (!permitido) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Transición no permitida: "
                                + actual + " -> " + nuevoEstado
                );
            }

            pedido.setEstado(nuevoEstado);
            Pedido guardado = repo.save(pedido);

            if ("ACEPTADO".equals(nuevoEstado)) {
                publisher.solicitarDescuentoStock(guardado);
            }

            kafkaPublisher.publicarCambioEstado(
                    guardado,
                    actual,
                    actor
            );

            return guardado;
        });
    }
}