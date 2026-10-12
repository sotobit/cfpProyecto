import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;

/** Genera catálogos y ventas pseudoaleatorios sin solicitar información. */
public class GenerateInfoFiles {
    private static final Random AZAR = new Random();
    private static final String[] NOMBRES = {"Carlos", "Ana", "Luis", "María", "Juan"};
    private static final String[] APELLIDOS = {"Gómez", "Pérez", "Rodríguez", "Martínez"};
    private static final String[] PRODUCTOS = {"Portátil", "Mouse", "Teclado", "Monitor"};
    private static int cantidadProductos;
    private static Path carpetaGeneracion = Path.of(".");

    /** Usa cantidades predeterminadas y respalda los datos que reemplaza. */
    public static void main(String[] args) {
        try {
            Path respaldo = generarEjemplo(Path.of("."));
            System.out.println("Archivos generados exitosamente. Respaldo: " + respaldo);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("ERROR al generar: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Genera primero en una carpeta temporal y luego sustituye el conjunto completo. */
    public static synchronized Path generarEjemplo(Path carpeta) throws IOException {
        carpeta = carpeta.toAbsolutePath().normalize();
        Path temporal = Files.createTempDirectory(carpeta, ".cfp-generacion-");
        ArrayList<String> nombres = new ArrayList<>();
        ArrayList<String> contenidos = new ArrayList<>();
        try {
            carpetaGeneracion = temporal;
            createProductsFile(4);
            createSalesManInfoFile(3);
            nombres.add("productos.txt");
            nombres.add("vendedores.txt");
            for (int i = 0; i < 3; i++) {
                nombres.add("ventas_" + (100000001L + i) + ".txt");
            }
            for (String nombre : nombres) {
                contenidos.add(Files.readString(temporal.resolve(nombre), StandardCharsets.UTF_8));
            }
            ArrayList<String> eliminar = new ArrayList<>();
            for (String nombre : GestorVentas.nombresVentas(carpeta)) {
                if (!nombres.contains(nombre)) {
                    eliminar.add(nombre);
                }
            }
            // Al cambiar las entradas, los CSV anteriores dejan de representar los datos.
            eliminar.add("reporte_vendedores.csv");
            eliminar.add("reporte_productos.csv");
            return ArchivosSeguros.guardar(carpeta, nombres, contenidos, eliminar);
        } finally {
            carpetaGeneracion = Path.of(".");
            File[] archivos = temporal.toFile().listFiles();
            if (archivos != null) {
                for (File archivo : archivos) {
                    Files.deleteIfExists(archivo.toPath());
                }
            }
            Files.deleteIfExists(temporal);
        }
    }

    /**
     * Crea productos con IDs consecutivos y precios en pesos enteros.
     * @param productsCount cantidad positiva de productos
     * @throws IOException si no puede escribir el catálogo
     */
    public static void createProductsFile(int productsCount) throws IOException {
        if (productsCount <= 0) {
            throw new IllegalArgumentException("La cantidad de productos debe ser positiva.");
        }
        cantidadProductos = 0;
        try (BufferedWriter escritor = Files.newBufferedWriter(carpetaGeneracion.resolve("productos.txt"), StandardCharsets.UTF_8)) {
            for (int i = 0; i < productsCount; i++) {
                long precio = 10000 + AZAR.nextInt(2490001);
                escritor.write((i + 1) + ";" + PRODUCTOS[i % PRODUCTOS.length] + ";" + precio);
                escritor.newLine();
            }
        }
        cantidadProductos = productsCount;
    }

    /**
     * Crea vendedores y ventas coherentes con los productos generados.
     * @param salesmanCount cantidad positiva de vendedores
     * @throws IOException si no puede escribir los archivos
     */
    public static void createSalesManInfoFile(int salesmanCount) throws IOException {
        if (salesmanCount <= 0 || cantidadProductos == 0) {
            throw new IllegalArgumentException("Genere productos y use una cantidad positiva de vendedores.");
        }
        try (BufferedWriter escritor = Files.newBufferedWriter(carpetaGeneracion.resolve("vendedores.txt"), StandardCharsets.UTF_8)) {
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
     * Crea ventas de uno a diez artículos, con IDs de productos existentes.
     * @param randomSalesCount cantidad positiva de ventas
     * @param name nombre no vacío del vendedor
     * @param id documento positivo del vendedor
     * @throws IOException si no puede escribir el archivo
     */
    public static void createSalesMenFile(int randomSalesCount, String name, long id) throws IOException {
        if (randomSalesCount <= 0 || id <= 0 || name == null || name.trim().isEmpty() || cantidadProductos == 0) {
            throw new IllegalArgumentException("Verifique productos, vendedor y cantidad de ventas.");
        }
        try (BufferedWriter escritor = Files.newBufferedWriter(carpetaGeneracion.resolve("ventas_" + id + ".txt"), StandardCharsets.UTF_8)) {
            escritor.write("CC;" + id);
            escritor.newLine();
            for (int i = 0; i < randomSalesCount; i++) {
                escritor.write((1 + AZAR.nextInt(cantidadProductos)) + ";" + (1 + AZAR.nextInt(10)) + ";");
                escritor.newLine();
            }
        }
    }
}
