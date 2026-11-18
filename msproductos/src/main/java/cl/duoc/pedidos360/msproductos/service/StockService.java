package cl.duoc.pedidos360.msproductos.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.pedidos360.msproductos.model.Producto;
import cl.duoc.pedidos360.msproductos.repository.ProductoRepository;

@Service
public class StockService {

    private final ProductoRepository productoRepository;

    public StockService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Transactional
    public void descontarPedido(Map<Long, Integer> cantidadesPorProducto) {
        if (cantidadesPorProducto == null
                || cantidadesPorProducto.isEmpty()) {
            throw new IllegalArgumentException(
                    "El pedido debe contener productos");
        }

        // Validar todos los datos antes de consultar los productos.
        cantidadesPorProducto.forEach((productoId, cantidad) -> {
            if (productoId == null || productoId <= 0) {
                throw new IllegalArgumentException(
                        "El identificador del producto es inválido");
            }

            if (cantidad == null || cantidad <= 0) {
                throw new IllegalArgumentException(
                        "La cantidad debe ser mayor que cero");
            }
        });

        Map<Long, Producto> productos = new LinkedHashMap<>();

        // Consultar y bloquear los productos en orden de identificador.
        for (Long productoId :
                new TreeSet<>(cantidadesPorProducto.keySet())) {

            Integer cantidad = cantidadesPorProducto.get(productoId);

            Producto producto = productoRepository
                    .findByIdParaActualizar(productoId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El producto " + productoId + " no existe"));

            if (producto.getStock() == null
                    || producto.getStock() < cantidad) {
                throw new IllegalStateException(
                        "Stock insuficiente del producto " + productoId);
            }

            productos.put(productoId, producto);
        }

        // Descontar únicamente después de comprobar todos los productos.
        cantidadesPorProducto.forEach((productoId, cantidad) -> {
            Producto producto = productos.get(productoId);
            producto.setStock(producto.getStock() - cantidad);
        });
    }
}