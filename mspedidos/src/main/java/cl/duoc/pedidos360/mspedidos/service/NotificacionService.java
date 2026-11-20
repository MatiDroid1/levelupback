package cl.duoc.pedidos360.mspedidos.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.pedidos360.mspedidos.dto.NotificacionResponse;
import cl.duoc.pedidos360.mspedidos.model.Notificacion;
import cl.duoc.pedidos360.mspedidos.repository.NotificacionRepository;

@Service
public class NotificacionService {

    private final NotificacionRepository repo;

    public NotificacionService(
            NotificacionRepository repo) {

        this.repo = repo;
    }

    public boolean eventoProcesado(String eventoId) {
        return repo.existsByEventoId(eventoId);
    }

    @Transactional
    public void registrar(
            String eventoId,
            String usuario,
            Long pedidoId,
            String estado,
            LocalDateTime fecha) {

        if (repo.existsByEventoId(eventoId)) {
            return;
        }

        String texto = switch (estado) {
            case "CREADO" ->
                    "Recibimos tu compra. Está pendiente de aceptación.";

            case "ACEPTADO" ->
                    "Tu compra fue aceptada.";

            case "EN_PREPARACIÓN" ->
                    "Tu compra está en preparación.";

            case "DESPACHADO" ->
                    "Tu compra fue despachada.";

            case "ENTREGADO" ->
                    "Tu compra fue entregada.";

            case "CANCELADO" ->
                    "Tu compra fue cancelada.";

            default ->
                    throw new IllegalArgumentException(
                            "Estado no reconocido"
                    );
        };

        Notificacion notificacion = new Notificacion(
                eventoId,
                usuario.trim(),
                pedidoId,
                estado,
                "Pedido #" + pedidoId + ": " + texto,
                fecha
        );

        repo.saveAndFlush(notificacion);
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(
            String usuario) {

        return repo
                .findTop100ByDestinatarioIgnoreCaseOrderByFechaDescIdDesc(
                        usuario
                )
                .stream()
                .map(NotificacionResponse::desde)
                .toList();
    }

    @Transactional
    public NotificacionResponse marcarLeida(
            Long id,
            String usuario) {

        Notificacion notificacion = repo
                .findByIdAndDestinatarioIgnoreCase(id, usuario)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Notificación no encontrada"
                        )
                );

        notificacion.marcarLeida();

        return NotificacionResponse.desde(notificacion);
    }
}