/** Datos de un producto y sus unidades vendidas. */
public class Producto {
    final String id;
    final String nombre;
    final long precio;
    long cantidadVendida;

    /** Crea un producto con precio en pesos enteros y sin ventas. */
    public Producto(String id, String nombre, long precio) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        cantidadVendida = 0;
    }
}
