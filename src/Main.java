import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lee los archivos CSV de entrada y genera los dos reportes de ventas. */
public class Main {
    /**
     * Procesa los archivos existentes sin regenerarlos ni solicitar información.
     */
    public static void main(String[] args) {
        try {
            Map<String, Producto> productos = leerProductos();
            Map<String, Vendedor> vendedores = leerVendedores();
            procesarVentas(productos, vendedores);
            escribirReportes(productos, vendedores);
            System.out.println("Reportes generados exitosamente.");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Lee el catálogo de productos, comprobando formato, precio e identificadores. */
    private static Map<String, Producto> leerProductos() throws IOException {
        Path archivo = Paths.get("productos.csv");
        Map<String, Producto> productos = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea;
            int numero = 0;
            while ((linea = reader.readLine()) != null) {
                numero++;
                String[] datos = separar(linea, 3, archivo, numero);
                BigDecimal precio;
                try {
                    precio = new BigDecimal(datos[2]);
                } catch (NumberFormatException e) {
                    throw error(archivo, numero, "Precio inválido; utilice punto decimal.");
                }
                if (precio.signum() < 0) {
                    throw error(archivo, numero, "El precio no puede ser negativo.");
                }
                if (productos.containsKey(datos[0])) {
                    throw error(archivo, numero, "ID de producto duplicado: " + datos[0]);
                }
                productos.put(datos[0], new Producto(datos[0], datos[1], precio));
            }
        }
        if (productos.isEmpty()) {
            throw error(archivo, 1, "El catálogo está vacío.");
        }
        return productos;
    }

    /** Lee vendedores; su identidad es la combinación de tipo y número de documento. */
    private static Map<String, Vendedor> leerVendedores() throws IOException {
        Path archivo = Paths.get("vendedores.csv");
        Map<String, Vendedor> vendedores = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea;
            int numero = 0;
            while ((linea = reader.readLine()) != null) {
                numero++;
                String[] datos = separar(linea, 4, archivo, numero);
                String clave = datos[0] + ";" + datos[1];
                if (vendedores.containsKey(clave)) {
                    throw error(archivo, numero, "Documento de vendedor duplicado: " + clave);
                }
                vendedores.put(clave, new Vendedor(datos[0], datos[1], datos[2], datos[3]));
            }
        }
        if (vendedores.isEmpty()) {
            throw error(archivo, 1, "El catálogo está vacío.");
        }
        return vendedores;
    }

    /** Busca todos los archivos de ventas de la raíz, sin fijar el número de vendedores. */
    private static void procesarVentas(Map<String, Producto> productos,
            Map<String, Vendedor> vendedores) throws IOException {
        List<Path> archivos = new ArrayList<>();
        try (DirectoryStream<Path> directorio = Files.newDirectoryStream(Paths.get("."), "vendedor_*.csv")) {
            for (Path archivo : directorio) {
                if (Files.isRegularFile(archivo)) {
                    archivos.add(archivo);
                }
            }
        }
        if (archivos.isEmpty()) {
            throw new IOException("No se encontraron archivos vendedor_*.csv en la raíz del proyecto.");
        }
        archivos.sort(Comparator.naturalOrder());
        for (Path archivo : archivos) {
            procesarArchivoVenta(archivo, productos, vendedores);
        }
    }

    /** Acumula ventas; un archivo con solo el documento representa cero ventas. */
    private static void procesarArchivoVenta(Path archivo, Map<String, Producto> productos,
            Map<String, Vendedor> vendedores) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String cabecera = reader.readLine();
            if (cabecera == null) {
                throw error(archivo, 1, "Falta el documento del vendedor.");
            }
            String[] documento = separar(cabecera, 2, archivo, 1);
            Vendedor vendedor = vendedores.get(documento[0] + ";" + documento[1]);
            if (vendedor == null) {
                throw error(archivo, 1, "El vendedor no existe en vendedores.csv.");
            }
            String linea;
            int numero = 1;
            while ((linea = reader.readLine()) != null) {
                numero++;
                if (!linea.endsWith(";")) {
                    throw error(archivo, numero, "La venta debe tener el formato IDProducto;Cantidad;");
                }
                String[] datos = separar(linea.substring(0, linea.length() - 1), 2, archivo, numero);
                Producto producto = productos.get(datos[0]);
                if (producto == null) {
                    throw error(archivo, numero, "Producto inexistente: " + datos[0]);
                }
                long cantidad;
                try {
                    cantidad = Long.parseLong(datos[1]);
                } catch (NumberFormatException e) {
                    throw error(archivo, numero, "La cantidad debe ser un entero válido.");
                }
                if (cantidad < 0) {
                    throw error(archivo, numero, "La cantidad no puede ser negativa.");
                }
                try {
                    producto.cantidadVendida = Math.addExact(producto.cantidadVendida, cantidad);
                } catch (ArithmeticException e) {
                    throw error(archivo, numero, "La cantidad acumulada excede el límite admitido.");
                }
                vendedor.totalRecaudado = vendedor.totalRecaudado.add(
                        producto.precio.multiply(BigDecimal.valueOf(cantidad)));
            }
        }
    }

    /** Escribe los reportes después de validar todas las entradas; los empates se ordenan por ID. */
    private static void escribirReportes(Map<String, Producto> productos,
            Map<String, Vendedor> vendedores) throws IOException {
        List<Vendedor> listaVendedores = new ArrayList<>(vendedores.values());
        listaVendedores.sort(Comparator.comparing((Vendedor v) -> v.totalRecaudado)
                .reversed().thenComparing(v -> v.tipoDocumento).thenComparing(v -> v.documento));
        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get("reporte_vendedores.csv"), StandardCharsets.UTF_8)) {
            for (Vendedor vendedor : listaVendedores) {
                writer.write(vendedor.nombres + " " + vendedor.apellidos + ";"
                        + vendedor.totalRecaudado.setScale(2, RoundingMode.HALF_UP).toPlainString());
                writer.newLine();
            }
        }
        List<Producto> listaProductos = new ArrayList<>(productos.values());
        listaProductos.sort(Comparator.comparingLong((Producto p) -> p.cantidadVendida)
                .reversed().thenComparing(p -> p.id));
        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get("reporte_productos.csv"), StandardCharsets.UTF_8)) {
            for (Producto producto : listaProductos) {
                writer.write(producto.nombre + ";"
                        + producto.precio.setScale(2, RoundingMode.HALF_UP).toPlainString());
                writer.newLine();
            }
        }
    }

    /** Divide una fila y comprueba que tenga exactamente los campos requeridos. */
    private static String[] separar(String linea, int cantidad, Path archivo, int numero)
            throws IOException {
        String[] datos = linea.split(";", -1);
        if (datos.length != cantidad) {
            throw error(archivo, numero, "Se esperaban " + cantidad + " campos separados por punto y coma.");
        }
        for (int i = 0; i < datos.length; i++) {
            datos[i] = datos[i].trim();
            if (datos[i].isEmpty()) {
                throw error(archivo, numero, "No se permiten campos vacíos.");
            }
        }
        return datos;
    }

    /** Construye un mensaje con la ubicación del dato incorrecto. */
    private static IOException error(Path archivo, int linea, String mensaje) {
        return new IOException(archivo.getFileName() + ", línea " + linea + ": " + mensaje);
    }
}
