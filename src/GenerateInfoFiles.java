import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

/** Genera los archivos de prueba. */
public class GenerateInfoFiles {
    private static final Random RANDOM = new Random();
    private static final String[] NOMBRES = {"Carlos", "Ana", "Luis", "Maria", "Juan"};
    private static final String[] APELLIDOS = {"Gomez", "Perez", "Rodriguez", "Martinez"};
    private static final String[] PRODUCTOS = {"Laptop", "Mouse", "Teclado", "Monitor"};
    private static int cantidadProductos = 0;

    /**
     * Genera cuatro productos y tres vendedores con sus ventas.
     * @param args argumentos de ejecución (no se utilizan)
     */
    public static void main(String[] args) {
        try {
            createProductsFile(4);
            createSalesManInfoFile(3);
            System.out.println("Archivos generados exitosamente.");
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            System.err.println("Error al generar los archivos: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Crea productos con identificadores consecutivos y precios aleatorios.
     * @param productsCount cantidad de productos, mayor que cero
     * @throws IOException si no se puede escribir el archivo
     */
    public static void createProductsFile(int productsCount) throws IOException {
        if (productsCount <= 0) {
            throw new IllegalArgumentException("La cantidad de productos debe ser positiva.");
        }
        cantidadProductos = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get("productos.csv"), StandardCharsets.UTF_8)) {
            for (int i = 1; i <= productsCount; i++) {
                String nombre = PRODUCTOS[(i - 1) % PRODUCTOS.length];
                int precio = 10000 + RANDOM.nextInt(2490001);
                writer.write(i + ";" + nombre + ";" + precio);
                writer.newLine();
            }
        }
        cantidadProductos = productsCount;
    }

    /**
     * Crea el listado de vendedores y un archivo de ventas por vendedor.
     * Se deben generar primero los productos.
     * @param salesmanCount cantidad de vendedores, mayor que cero
     * @throws IOException si no se puede escribir algún archivo
     */
    public static void createSalesManInfoFile(int salesmanCount) throws IOException {
        if (salesmanCount <= 0) {
            throw new IllegalArgumentException("La cantidad de vendedores debe ser positiva.");
        }
        if (cantidadProductos == 0) {
            throw new IllegalStateException("Primero se deben generar los productos.");
        }
        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get("vendedores.csv"), StandardCharsets.UTF_8)) {
            for (int i = 0; i < salesmanCount; i++) {
                // El documento también mantiene estable el nombre del archivo.
                long id = 100000001L + i;
                String nombre = NOMBRES[RANDOM.nextInt(NOMBRES.length)];
                String apellido = APELLIDOS[RANDOM.nextInt(APELLIDOS.length)];
                writer.write("CC;" + id + ";" + nombre + ";" + apellido);
                writer.newLine();
                createSalesMenFile(2 + RANDOM.nextInt(4), nombre, id);
            }
        }
    }

    /**
     * Crea ventas con productos existentes y cantidades entre uno y diez.
     * El nombre se usa para identificar al vendedor en el mensaje de consola;
     * el formato del archivo solo incluye su tipo y número de documento.
     * @param randomSalesCount número de registros de venta, mayor que cero
     * @param name nombre del vendedor
     * @param id número de documento del vendedor
     * @throws IOException si no se puede escribir el archivo
     */
    public static void createSalesMenFile(int randomSalesCount, String name, long id)
            throws IOException {
        if (randomSalesCount <= 0 || id <= 0 || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Los datos del vendedor y sus ventas no son válidos.");
        }
        if (cantidadProductos == 0) {
            throw new IllegalStateException("Primero se deben generar los productos.");
        }
        String archivo = "vendedor_" + id + ".csv";
        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(archivo), StandardCharsets.UTF_8)) {
            writer.write("CC;" + id);
            writer.newLine();
            for (int i = 0; i < randomSalesCount; i++) {
                int productoId = 1 + RANDOM.nextInt(cantidadProductos);
                int cantidad = 1 + RANDOM.nextInt(10);
                writer.write(productoId + ";" + cantidad + ";");
                writer.newLine();
            }
        }
        System.out.println("Archivo de ventas creado para " + name + ": " + archivo);
    }
}
