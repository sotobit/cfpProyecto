/** Identidad del vendedor y su recaudo en pesos enteros. */
public class Vendedor {
    final String tipoDocumento;
    final String documento;
    final String nombres;
    final String apellidos;
    long totalRecaudado;

    /** Crea un vendedor sin recaudo; tipo y documento forman su identidad. */
    public Vendedor(String tipoDocumento, String documento, String nombres, String apellidos) {
        this.tipoDocumento = tipoDocumento;
        this.documento = documento;
        this.nombres = nombres;
        this.apellidos = apellidos;
        totalRecaudado = 0;
    }
}
