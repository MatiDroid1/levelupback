package cl.duoc.pedidos360.mspedidos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import cl.duoc.pedidos360.mspedidos.model.DetallePedido;
import cl.duoc.pedidos360.mspedidos.model.Pedido;
import cl.duoc.pedidos360.mspedidos.repository.PedidoRepository;

@Service
public class PedidoService {

    private final PedidoRepository repo;

    public PedidoService(PedidoRepository repo) {
        this.repo = repo;
    }

    public List<Pedido> listarTodos() {
        return repo.findAll();
    }

    public List<Pedido> listarPorUsuario(String usuario) {
        return repo.findByUsuario(usuario);
    }

    public Optional<Pedido> obtener(Long id) {
        return repo.findById(id);
    }

    public Pedido crear(Pedido pedido) {
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado("CREADO");

        int total = 0;
        for (DetallePedido d : pedido.getDetalles()) {
            d.setSubtotal(d.getPrecioUnitario() * d.getCantidad());
            total += d.getSubtotal();
        }
        pedido.setTotal(total);

        return repo.save(pedido);
    }

    public Optional<Pedido> cambiarEstado(Long id, String nuevoEstado) {
        return repo.findById(id).map(p -> {
            p.setEstado(nuevoEstado);
            return repo.save(p);
        });
    }
}