package cl.duoc.pedidos360.msproductos.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import cl.duoc.pedidos360.msproductos.model.Producto;
import cl.duoc.pedidos360.msproductos.repository.ProductoRepository;

@Service
public class ProductoService {

    private final ProductoRepository repo;

    public ProductoService(ProductoRepository repo) {
        this.repo = repo;
    }

    public List<Producto> porCategoria(String categoria) {
        return repo.findByCategoriaIgnoreCase(categoria);
    }

    public List<Producto> listar() {
        return repo.findAll();
    }

    public Optional<Producto> obtener(Long id) {
        return repo.findById(id);
    }

    public Producto crear(Producto producto) {
        return repo.save(producto);
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}