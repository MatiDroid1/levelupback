package cl.duoc.pedidos360.mspedidos.dto;

import java.time.LocalDateTime;

import cl.duoc.pedidos360.mspedidos.model.Notificacion;

public record NotificacionResponse(
        Long id,
        Long pedidoId,
        String estado,
        String mensaje,
        LocalDateTime fecha,
        boolean leida) {

    public static NotificacionResponse desde(
            Notificacion notificacion) {

        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getPedidoId(),
                notificacion.getEstado(),
                notificacion.getMensaje(),
                notificacion.getFecha(),
                notificacion.isLeida()
        );
    }
}