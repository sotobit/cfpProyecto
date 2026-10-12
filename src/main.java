import java.io.IOException;
import java.nio.file.Path;
import javax.swing.SwingUtilities;

/** Entrada automática del procesador y de la interfaz de la entrega final. */
public class main {
    /** No lee System.in; --sin-interfaz permite verificar el mismo procesamiento. */
    public static void main(String[] args) {
        boolean sinInterfaz = false;
        if (args.length == 1 && args[0].equals("--sin-interfaz")) {
            sinInterfaz = true;
        } else if (args.length != 0) {
            System.err.println("ERROR: argumento desconocido. Use --sin-interfaz o ejecute sin argumentos.");
            System.exit(1);
        }
        final GestorVentas gestor = new GestorVentas(Path.of("."));
        String estado;
        boolean exito = false;
        try {
            if (GestorVentas.sinEntradas(gestor.getCarpeta())) {
                GenerateInfoFiles.generarEjemplo(gestor.getCarpeta());
                System.out.println("Ejemplo inicial generado automáticamente.");
            }
            gestor.cargar();
            gestor.exportar();
            estado = "Procesamiento y exportación completados. " + gestor.getAdvertencias().size() + " advertencias.";
            System.out.println(estado);
            for (String aviso : gestor.getAdvertencias()) { System.out.println(aviso); }
            System.out.println("VENDEDORES - nombre;recaudo en pesos");
            for (Vendedor vendedor : gestor.vendedoresOrdenados()) {
                System.out.println(vendedor.nombres + " " + vendedor.apellidos + ";" + vendedor.totalRecaudado);
            }
            System.out.println("PRODUCTOS - nombre;precio en pesos (ordenados por unidades)");
            for (Producto producto : gestor.productosOrdenados()) {
                System.out.println(producto.nombre + ";" + producto.precio + " | Unidades: " + producto.cantidadVendida);
            }
            System.out.println(gestor.getCarpeta().resolve("reporte_vendedores.csv"));
            System.out.println(gestor.getCarpeta().resolve("reporte_productos.csv"));
            exito = true;
        } catch (IOException | IllegalArgumentException e) {
            estado = "ERROR: " + e.getMessage() + " Los CSV anteriores no son resultados actuales.";
            System.err.println(estado);
        }
        if (sinInterfaz) {
            if (!exito) { System.exit(1); }
            return;
        }
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            System.err.println("ERROR: no hay entorno gráfico. Use --sin-interfaz para procesar archivos.");
            System.exit(1);
        }
        final String estadoInicial = estado;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                VentanaVentas.prepararAspecto();
                new VentanaVentas(gestor, estadoInicial).setVisible(true);
            }
        });
    }
}
