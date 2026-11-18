package cl.duoc.pedidos360.msproductos.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.pedidos360.msproductos.model.EventoAuditoria;

public interface EventoAuditoriaRepository
        extends JpaRepository<EventoAuditoria, Long> {

    boolean existsByEventId(String eventId);

    Page<EventoAuditoria> findByPedidoIdOrderByFechaEventoAscIdAsc(
            Long pedidoId,
            Pageable pageable
    );
}