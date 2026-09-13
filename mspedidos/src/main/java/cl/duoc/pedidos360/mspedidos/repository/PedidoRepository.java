package cl.duoc.pedidos360.mspedidos.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import cl.duoc.pedidos360.mspedidos.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByUsuario(String usuario);
}