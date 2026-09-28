import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.Scanner;

/** Genera catálogos y ventas coherentes para practicar con el programa. */
public class GenerateInfoFiles {
    private static final Random AZAR = new Random();
    private static final String[] NOMBRES = {"Carlos", "Ana", "Luis", "María", "Juan"};
    private static final String[] APELLIDOS = {"Gómez", "Pérez", "Rodríguez", "Martínez"};
    private static final String[] PRODUCTOS = {"Portátil", "Mouse", "Teclado", "Monitor"};
    private static int cantidadProductos;

    /** Ejecuta el generador independiente con protección de datos existentes. */
    public static void main(String[] args) {
        try (Scanner teclado = new Scanner(System.in)) {
            if (confirmarGeneracion(teclado)) {
                generarEjemplo();
                System.out.println("Archivos generados exitosamente.");
            }
        } catch (IOException e) {
            System.err.println("ERROR al generar: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Solicita confirmación solo cuando ya existen archivos que se reemplazarán. */
    public static boolean confirmarGeneracion(Scanner teclado) {
        boolean existen = new File("productos.txt").exists() || new File("vendedores.txt").exists();
        for (int i = 0; i < 3; i++) {
            if (new File("ventas_" + (100000001L + i) + ".txt").exists()) {
                existen = true;
            }
        }
        System.out.println("Se generarán cuatro productos y tres vendedores de ejemplo.");
        System.out.println("Otros archivos ventas_*.txt se conservan y también se procesarán.");
        if (existen) {
            System.out.print("¿Reemplazar los cinco archivos de ejemplo? Escriba SI para confirmar: ");
            if (!teclado.hasNextLine() || !teclado.nextLine().trim().equalsIgnoreCase("SI")) {
                System.out.println("Generación cancelada. No se modificaron los datos.");
                return false;
            }
        }
        return true;
    }

    /** Genera primero productos para que todas las ventas usen IDs existentes. */
    public static void generarEjemplo() throws IOException {
        createProductsFile(4);
        createSalesManInfoFile(3);
    }

    /**
     * Crea productos de precio entero y con IDs consecutivos.
     * @param productsCount número de productos, mayor que cero
     * @throws IOException si no puede escribir el catálogo
     */
    public static void createProductsFile(int productsCount) throws IOException {
        if (productsCount <= 0) {
            throw new IllegalArgumentException("La cantidad de productos debe ser positiva.");
        }
        cantidadProductos = 0;
        try (BufferedWriter escritor = Files.newBufferedWriter(Path.of("productos.txt"), StandardCharsets.UTF_8)) {
            for (int i = 0; i < productsCount; i++) {
                long precio = 10000 + AZAR.nextInt(2490001);
                escritor.write((i + 1) + ";" + PRODUCTOS[i % PRODUCTOS.length] + ";" + precio);
                escritor.newLine();
            }
        }
        cantidadProductos = productsCount;
    }

    /**
     * Crea vendedores y sus ventas; requiere generar antes los productos.
     * @param salesmanCount número de vendedores, mayor que cero
     * @throws IOException si no puede escribir los archivos
     */
    public static void createSalesManInfoFile(int salesmanCount) throws IOException {
        if (salesmanCount <= 0 || cantidadProductos == 0) {
            throw new IllegalArgumentException("Genere productos y use una cantidad positiva de vendedores.");
        }
        try (BufferedWriter escritor = Files.newBufferedWriter(Path.of("vendedores.txt"), StandardCharsets.UTF_8)) {
            for (int i = 0; i < salesmanCount; i++) {
                long documento = 100000001L + i;
                String nombre = NOMBRES[AZAR.nextInt(NOMBRES.length)];
                String apellido = APELLIDOS[AZAR.nextInt(APELLIDOS.length)];
                escritor.write("CC;" + documento + ";" + nombre + ";" + apellido);
                escritor.newLine();
                createSalesMenFile(2 + AZAR.nextInt(4), nombre, documento);
            }
        }
    }

    /**
     * Crea ventas con productos existentes y cantidades entre uno y diez.
     * @param randomSalesCount cantidad positiva de ventas
     * @param name nombre del vendedor usado para validar los datos
     * @param id documento positivo del vendedor
     * @throws IOException si no puede escribir el archivo
     */
    public static void createSalesMenFile(int randomSalesCount, String name, long id) throws IOException {
        if (randomSalesCount <= 0 || id <= 0 || name == null || name.trim().isEmpty() || cantidadProductos == 0) {
            throw new IllegalArgumentException("Verifique productos, vendedor y cantidad de ventas.");
        }
        Path archivo = Path.of("ventas_" + id + ".txt");
        try (BufferedWriter escritor = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            escritor.write("CC;" + id);
            escritor.newLine();
            for (int i = 0; i < randomSalesCount; i++) {
                int productoId = 1 + AZAR.nextInt(cantidadProductos);
                int cantidad = 1 + AZAR.nextInt(10);
                escritor.write(productoId + ";" + cantidad + ";");
                escritor.newLine();
            }
        }
    }
}
