package cl.duoc.pedidos360.msproductos.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "evento_auditoria",
    indexes = {
        @Index(
            name = "idx_auditoria_pedido_fecha",
            columnList = "pedido_id,fecha_evento"
        )
    }
)
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
        name = "event_id",
        nullable = false,
        unique = true,
        length = 100
    )
    private String eventId;

    @Column(nullable = false, length = 80)
    private String tipo;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(length = 320)
    private String usuario;

    @Column(length = 320)
    private String actor;

    @Column(name = "estado_anterior", length = 40)
    private String estadoAnterior;

    @Column(length = 40)
    private String estado;

    private Long total;

    @Column(name = "fecha_pedido", nullable = false)
    private LocalDateTime fechaPedido;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(nullable = false, length = 200)
    private String topico;

    @Column(nullable = false)
    private Integer particion;

    @Column(name = "offset_kafka", nullable = false)
    private Long offsetKafka;

    @Lob
    @Column(name = "payload_original", nullable = false)
    private String payloadOriginal;
}