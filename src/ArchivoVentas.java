/** Cabecera de un archivo de ventas; también conserva archivos sin filas de venta. */
public class ArchivoVentas {
    final String nombre;
    final String tipoDocumento;
    final String documento;

    /** Identifica al propietario de un archivo, aunque todavía no tenga ventas. */
    public ArchivoVentas(String nombre, String tipoDocumento, String documento) {
        this.nombre = nombre;
        this.tipoDocumento = tipoDocumento;
        this.documento = documento;
    }
}
