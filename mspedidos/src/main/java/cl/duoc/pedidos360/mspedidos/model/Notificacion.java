package cl.duoc.pedidos360.mspedidos.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "notificaciones",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notificacion_evento",
                columnNames = "evento_id"
        )
)
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, length = 100)
    private String eventoId;

    @Column(nullable = false, length = 320)
    private String destinatario;

    @Column(nullable = false)
    private Long pedidoId;

    @Column(nullable = false, length = 40)
    private String estado;

    @Column(nullable = false, length = 300)
    private String mensaje;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private boolean leida;

    protected Notificacion() {
    }

    public Notificacion(
            String eventoId,
            String destinatario,
            Long pedidoId,
            String estado,
            String mensaje,
            LocalDateTime fecha) {

        this.eventoId = eventoId;
        this.destinatario = destinatario;
        this.pedidoId = pedidoId;
        this.estado = estado;
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.leida = false;
    }

    public Long getId() {
        return id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public String getEstado() {
        return estado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public boolean isLeida() {
        return leida;
    }

    public void marcarLeida() {
        this.leida = true;
    }
}