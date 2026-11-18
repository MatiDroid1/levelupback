package cl.duoc.pedidos360.msproductos.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.pedidos360.msproductos.model.EventoAuditoria;
import cl.duoc.pedidos360.msproductos.repository.EventoAuditoriaRepository;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final EventoAuditoriaRepository repo;

    public AuditoriaController(EventoAuditoriaRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public PaginaAuditoria listar(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        validarPaginacion(page, size);

        PageRequest paginacion = PageRequest.of(
                page,
                size,
                Sort.by(
                    Sort.Order.desc("fechaEvento"),
                    Sort.Order.desc("id")
                )
        );

        return convertirPagina(repo.findAll(paginacion));
    }

    @GetMapping("/pedidos/{pedidoId}")
    public PaginaAuditoria historialPorPedido(
            @PathVariable("pedidoId") Long pedidoId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        validarPaginacion(page, size);

        if (pedidoId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "pedidoId debe ser mayor que cero"
            );
        }

        Page<EventoAuditoria> resultado =
                repo.findByPedidoIdOrderByFechaEventoAscIdAsc(
                    pedidoId,
                    PageRequest.of(page, size)
                );

        return convertirPagina(resultado);
    }

    private void validarPaginacion(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "page no puede ser negativo"
            );
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "size debe estar entre 1 y 100"
            );
        }
    }

    private PaginaAuditoria convertirPagina(
            Page<EventoAuditoria> pagina) {

        List<EventoAuditoriaRespuesta> contenido =
                pagina.getContent()
                    .stream()
                    .map(EventoAuditoriaRespuesta::desde)
                    .toList();

        return new PaginaAuditoria(
                contenido,
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast()
        );
    }

    public record PaginaAuditoria(
            List<EventoAuditoriaRespuesta> content,
            int number,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
    }

    public record EventoAuditoriaRespuesta(
            Long id,
            String eventId,
            String tipo,
            Long pedidoId,
            String usuario,
            String actor,
            String estadoAnterior,
            String estado,
            Long total,
            LocalDateTime fechaPedido,
            LocalDateTime fechaEvento,
            LocalDateTime fechaRegistro,
            String topico,
            Integer particion,
            Long offsetKafka
    ) {

        public static EventoAuditoriaRespuesta desde(
                EventoAuditoria evento) {

            return new EventoAuditoriaRespuesta(
                    evento.getId(),
                    evento.getEventId(),
                    evento.getTipo(),
                    evento.getPedidoId(),
                    evento.getUsuario(),
                    evento.getActor(),
                    evento.getEstadoAnterior(),
                    evento.getEstado(),
                    evento.getTotal(),
                    evento.getFechaPedido(),
                    evento.getFechaEvento(),
                    evento.getFechaRegistro(),
                    evento.getTopico(),
                    evento.getParticion(),
                    evento.getOffsetKafka()
            );
        }
    }
}