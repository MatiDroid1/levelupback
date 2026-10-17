package cl.duoc.pedidos360.msproductos.service;

import java.util.Map;

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

    /** Descuenta todo el pedido en una sola transaccion: o se aplica completo o no se aplica. */
    @Transactional
    public void descontarPedido(Map<Long, Integer> cantidadesPorProducto) {
        cantidadesPorProducto.forEach((productoId, cantidad) -> {
            Producto p = productoRepository.findById(productoId)
                    .orElseThrow(() -> new IllegalArgumentException("El producto " + productoId + " no existe"));
            if (p.getStock() < cantidad) {
                throw new IllegalStateException("Stock insuficiente del producto " + productoId
                        + ": hay " + p.getStock() + " y se piden " + cantidad);
            }
            p.setStock(p.getStock() - cantidad);
        });
    }
}