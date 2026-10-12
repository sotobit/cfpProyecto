/** Una fila de venta y su ubicación en el archivo de entrada. */
public class Venta {
    final String tipoDocumento;
    final String documento;
    final String productoId;
    final long cantidad;
    final String archivo;
    final int linea;
    boolean contabilizada = true;

    /** Conserva los datos de la fila y su procedencia para editar el archivo correcto. */
    public Venta(String tipoDocumento, String documento, String productoId,
            long cantidad, String archivo, int linea) {
        this.tipoDocumento = tipoDocumento;
        this.documento = documento;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.archivo = archivo;
        this.linea = linea;
    }
}
