package cl.duoc.pedidos360.msproductos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.pedidos360.msproductos.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByCategoriaIgnoreCase(String categoria);
}
