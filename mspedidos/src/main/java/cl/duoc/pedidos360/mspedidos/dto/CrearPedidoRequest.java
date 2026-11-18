package cl.duoc.pedidos360.mspedidos.dto;

import java.util.List;

public record CrearPedidoRequest(List<Item> detalles) {

    public record Item(
            Long productoId,
            String nombreProducto,
            Integer precioUnitario,
            Integer cantidad) {
    }
}