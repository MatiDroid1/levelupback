package cl.duoc.pedidos360.msrabbitadmin.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.msrabbitadmin.dto.BindingRequest;
import cl.duoc.pedidos360.msrabbitadmin.dto.ExchangeRequest;
import cl.duoc.pedidos360.msrabbitadmin.dto.QueueRequest;
import cl.duoc.pedidos360.msrabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;

@RestController
public class RabbitAdminController {

    private final RabbitAdminService service;

    public RabbitAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping("/queues")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crearCola(@Valid @RequestBody QueueRequest req) {
        return service.crearCola(req);
    }

    @GetMapping("/queues/{name}")
    public Map<String, Object> consultarCola(@PathVariable String name) {
        return service.consultarCola(name);
    }

    @DeleteMapping("/queues/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCola(@PathVariable String name) {
        service.eliminarCola(name);
    }

    @PostMapping("/exchanges")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crearExchange(@Valid @RequestBody ExchangeRequest req) {
        return service.crearExchange(req);
    }

    @DeleteMapping("/exchanges/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarExchange(@PathVariable String name) {
        service.eliminarExchange(name);
    }

    @PostMapping("/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crearBinding(@Valid @RequestBody BindingRequest req) {
        return service.crearBinding(req);
    }

    @DeleteMapping("/bindings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarBinding(@Valid @RequestBody BindingRequest req) {
        service.eliminarBinding(req);
    }
}