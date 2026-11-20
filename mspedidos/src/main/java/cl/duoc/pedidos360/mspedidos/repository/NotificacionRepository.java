package cl.duoc.pedidos360.mspedidos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.pedidos360.mspedidos.model.Notificacion;

public interface NotificacionRepository
        extends JpaRepository<Notificacion, Long> {

    boolean existsByEventoId(String eventoId);

    List<Notificacion>
            findTop100ByDestinatarioIgnoreCaseOrderByFechaDescIdDesc(
                    String destinatario
            );

    Optional<Notificacion> findByIdAndDestinatarioIgnoreCase(
            Long id,
            String destinatario
    );
}