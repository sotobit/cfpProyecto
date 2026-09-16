import java.math.BigDecimal;

/** Datos de un vendedor y dinero recaudado durante la lectura de ventas. */
public class Vendedor {
    final String tipoDocumento;
    final String documento;
    final String nombres;
    final String apellidos;
    BigDecimal totalRecaudado = BigDecimal.ZERO;

    /**
     * Construye un vendedor sin ventas.
     * @param tipoDocumento tipo de documento
     * @param documento número de documento
     * @param nombres nombres del vendedor
     * @param apellidos apellidos del vendedor
     */
    public Vendedor(String tipoDocumento, String documento, String nombres, String apellidos) {
        this.tipoDocumento = tipoDocumento;
        this.documento = documento;
        this.nombres = nombres;
        this.apellidos = apellidos;
    }
}
