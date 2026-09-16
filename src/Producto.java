import java.math.BigDecimal;

/** Datos de un producto y unidades acumuladas durante la lectura de ventas. */
public class Producto {
    final String id;
    final String nombre;
    final BigDecimal precio;
    long cantidadVendida = 0;

    /**
     * Construye un producto sin ventas.
     * @param id identificador único
     * @param nombre nombre del producto
     * @param precio precio unitario
     */
    public Producto(String id, String nombre, BigDecimal precio) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
    }
}
