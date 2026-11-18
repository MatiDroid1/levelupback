package cl.duoc.pedidos360.msproductos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cl.duoc.pedidos360.msproductos.model.Producto;
import cl.duoc.pedidos360.msproductos.repository.ProductoRepository;

class StockServiceTest {

    private ProductoRepository repo;
    private StockService service;

    @BeforeEach
    void preparar() {
        repo = mock(ProductoRepository.class);
        service = new StockService(repo);
    }

    private Producto producto(Long id, int stock) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setStock(stock);

        when(repo.findByIdParaActualizar(id))
                .thenReturn(Optional.of(producto));

        return producto;
    }

    @Test
    void descuentaCantidadSolicitada() {
        Producto producto = producto(1L, 10);

        service.descontarPedido(Map.of(1L, 3));

        assertEquals(Integer.valueOf(7), producto.getStock());
    }

    @Test
    void rechazaCantidadNegativaSinConsultarRepositorio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.descontarPedido(Map.of(1L, -3)));

        verifyNoInteractions(repo);
    }

    @Test
    void rechazaCantidadCero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.descontarPedido(Map.of(1L, 0)));

        verifyNoInteractions(repo);
    }

    @Test
    void rechazaPedidoVacio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.descontarPedido(Map.of()));

        verifyNoInteractions(repo);
    }

    @Test
    void rechazaProductoInexistente() {
        when(repo.findByIdParaActualizar(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.descontarPedido(Map.of(99L, 1)));
    }

    @Test
    void stockInsuficienteNoModificaNingunProducto() {
        Producto primero = producto(1L, 10);
        Producto segundo = producto(2L, 1);

        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        cantidades.put(1L, 3);
        cantidades.put(2L, 5);

        assertThrows(
                IllegalStateException.class,
                () -> service.descontarPedido(cantidades));

        assertEquals(Integer.valueOf(10), primero.getStock());
        assertEquals(Integer.valueOf(1), segundo.getStock());
    }

    @Test
    void permiteAgotarStockExacto() {
        Producto producto = producto(1L, 5);

        service.descontarPedido(Map.of(1L, 5));

        assertEquals(Integer.valueOf(0), producto.getStock());
    }
}