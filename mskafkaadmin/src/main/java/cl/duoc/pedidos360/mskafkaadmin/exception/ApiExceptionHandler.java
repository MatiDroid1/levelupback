package cl.duoc.pedidos360.mskafkaadmin.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception) {

        Map<String, String> fields = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> fields.put(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        return ResponseEntity.badRequest().body(
                Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 400,
                        "message", "Validación fallida",
                        "fields", fields
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(
            Exception exception) {

        Throwable cause = exception;

        while (cause instanceof ExecutionException
                && cause.getCause() != null) {

            cause = cause.getCause();
        }

        HttpStatus status;
        String message;

        if (cause instanceof UnknownTopicOrPartitionException) {

            status = HttpStatus.NOT_FOUND;
            message = "El tópico no existe";

        } else if (cause instanceof TopicExistsException) {

            status = HttpStatus.CONFLICT;
            message = "Ya existe un tópico con ese nombre";

        } else if (cause instanceof HttpMessageNotReadableException) {

            status = HttpStatus.BAD_REQUEST;
            message = "JSON inválido o tipo de campo incorrecto";

        } else if (cause instanceof HttpRequestMethodNotSupportedException) {

            status = HttpStatus.METHOD_NOT_ALLOWED;
            message = "Método HTTP no permitido para esta ruta";

        } else if (cause instanceof IllegalArgumentException
                || cause instanceof InvalidTopicException
                || cause instanceof InvalidConfigurationException
                || cause instanceof InvalidPartitionsException
                || cause instanceof InvalidReplicationFactorException) {

            status = HttpStatus.BAD_REQUEST;
            message = cause.getMessage();

        } else if (
                cause instanceof java.util.concurrent.TimeoutException
                || cause instanceof org.apache.kafka.common.errors.TimeoutException) {

            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "Kafka no confirmó dentro del plazo. "
                    + "Comprueba el resultado antes de repetir una escritura";

        } else if (cause instanceof AuthenticationException
                || cause instanceof AuthorizationException) {

            status = HttpStatus.BAD_GATEWAY;
            message = "El servicio no pudo autenticarse "
                    + "o no tiene permisos contra Kafka";

        } else if (cause instanceof InterruptedException) {

            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Operación interrumpida. Comprueba su resultado";

        } else {

            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Error interno. Revisa los logs del microservicio";
        }

        log.warn(
                "Error administrativo: tipo={} detalle={}",
                cause.getClass().getSimpleName(),
                cause.getMessage()
        );

        return ResponseEntity.status(status).body(
                Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", status.value(),
                        "message", message == null
                                ? "Solicitud inválida"
                                : message
                )
        );
    }
}