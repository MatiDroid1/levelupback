package cl.duoc.pedidos360.msproductos.messaging.consumer.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.msproductos.service.AuditoriaService;

@Component
public class AuditoriaKafkaListener {

    private static final Logger log =
            LoggerFactory.getLogger(AuditoriaKafkaListener.class);

    private final AuditoriaService auditoriaService;

    public AuditoriaKafkaListener(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @KafkaListener(
        topics = "${app.kafka.topic.pedidos}",
        groupId = "msproductos-auditoria",
        properties = {
            "auto.offset.reset=earliest",
            "enable.auto.commit=false"
        }
    )
    public void recibir(ConsumerRecord<String, String> record) {
        try {
            boolean guardado = auditoriaService.registrar(record);

            if (guardado) {
                log.info(
                        "[AUDITORIA] Evento guardado "
                                + "| topico={} | particion={} | offset={}",
                        record.topic(),
                        record.partition(),
                        record.offset()
                );
            } else {
                log.debug(
                        "[AUDITORIA] Evento duplicado o tipo no procesado "
                                + "| particion={} | offset={}",
                        record.partition(),
                        record.offset()
                );
            }

        } catch (Exception e) {
            log.error(
                    "[AUDITORIA] No se pudo registrar evento "
                            + "| topico={} | particion={} | offset={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    e
            );

            throw e;
        }
    }
}