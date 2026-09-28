import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;

/** Menú de consola para consultar datos y generar reportes de ventas. */
public class Main {
    /** Lee siempre líneas completas para evitar saltos de entrada pendientes. */
    public static void main(String[] args) {
        try (Scanner teclado = new Scanner(System.in)) {
            boolean continuar = true;
            while (continuar) {
                System.out.println("\nVENTAS Y VENDEDORES - Entrega 2");
                System.out.println("1. Generar datos de ejemplo");
                System.out.println("2. Cargar y mostrar archivos de entrada");
                System.out.println("3. Calcular y mostrar reportes");
                System.out.println("4. Exportar reportes CSV");
                System.out.println("5. Salir");
                System.out.print("Opción: ");
                if (!teclado.hasNextLine()) {
                    break;
                }
                String opcion = teclado.nextLine().trim();
                try {
                    switch (opcion) {
                        case "1":
                            if (GenerateInfoFiles.confirmarGeneracion(teclado)) {
                                GenerateInfoFiles.generarEjemplo();
                                System.out.println("Datos generados. Calcule nuevamente los reportes.");
                            }
                            break;
                        case "2":
                            mostrarEntradas();
                            break;
                        case "3":
                            calcularReportes(false);
                            break;
                        case "4":
                            calcularReportes(true);
                            break;
                        case "5":
                            continuar = false;
                            break;
                        default:
                            System.out.println("Opción inválida. Escriba un número del 1 al 5.");
                    }
                } catch (IOException e) {
                    System.out.println("ERROR: " + e.getMessage());
                    System.out.println("Operación incompleta. Los CSV anteriores no son resultados actuales.");
                }
            }
        }
        System.out.println("Programa finalizado.");
    }

    /** Relee los archivos y crea acumulados nuevos en cada operación. */
    private static void calcularReportes(boolean exportar) throws IOException {
        ArrayList<Producto> productos = leerProductos();
        ArrayList<Vendedor> vendedores = leerVendedores();
        int archivosValidos = 0;
        for (File archivo : buscarArchivosVentas()) {
            if (procesarVentas(archivo, productos, vendedores)) {
                archivosValidos++;
            }
        }
        if (archivosValidos == 0) {
            throw new IOException("Ningún archivo de ventas tiene una cabecera válida.");
        }
        ordenarVendedores(vendedores);
        ordenarProductos(productos);
        System.out.println("\nVENDEDORES - nombre;recaudo en pesos");
        for (Vendedor vendedor : vendedores) {
            System.out.println(filaVendedor(vendedor));
        }
        System.out.println("\nPRODUCTOS - nombre;precio en pesos (ordenados por unidades)");
        for (Producto producto : productos) {
            System.out.println(filaProducto(producto) + " | Unidades: " + producto.cantidadVendida);
        }
        if (exportar) {
            escribirReportes(productos, vendedores);
        }
        System.out.println("Cálculo completado con los registros válidos. Revise las advertencias.");
    }

    /** Lee productos y conserva la primera fila válida de cada identificador. */
    private static ArrayList<Producto> leerProductos() throws IOException {
        ArrayList<Producto> productos = new ArrayList<>();
        File archivo = new File("productos.txt");
        try (BufferedReader lector = Files.newBufferedReader(archivo.toPath(), StandardCharsets.UTF_8)) {
            String linea;
            int numero = 0;
            while ((linea = lector.readLine()) != null) {
                numero++;
                try {
                    String[] datos = separar(linea, 3);
                    long precio = enteroNoNegativo(datos[2]);
                    if (buscarProducto(productos, datos[0]) != null) {
                        throw new IllegalArgumentException("ID de producto duplicado.");
                    }
                    productos.add(new Producto(datos[0], datos[1], precio));
                } catch (IllegalArgumentException e) {
                    advertir(archivo, numero, e.getMessage());
                }
            }
        }
        if (productos.isEmpty()) {
            throw new IOException("productos.txt no contiene productos válidos.");
        }
        return productos;
    }

    /** Lee vendedores sin confundir documentos iguales de tipos distintos. */
    private static ArrayList<Vendedor> leerVendedores() throws IOException {
        ArrayList<Vendedor> vendedores = new ArrayList<>();
        File archivo = new File("vendedores.txt");
        try (BufferedReader lector = Files.newBufferedReader(archivo.toPath(), StandardCharsets.UTF_8)) {
            String linea;
            int numero = 0;
            while ((linea = lector.readLine()) != null) {
                numero++;
                try {
                    String[] datos = separar(linea, 4);
                    if (buscarVendedor(vendedores, datos[0], datos[1]) != null) {
                        throw new IllegalArgumentException("Documento de vendedor duplicado.");
                    }
                    vendedores.add(new Vendedor(datos[0], datos[1], datos[2], datos[3]));
                } catch (IllegalArgumentException e) {
                    advertir(archivo, numero, e.getMessage());
                }
            }
        }
        if (vendedores.isEmpty()) {
            throw new IOException("vendedores.txt no contiene vendedores válidos.");
        }
        return vendedores;
    }

    /** Busca solo ventas_*.txt de la raíz y las ordena por nombre. */
    private static ArrayList<File> buscarArchivosVentas() throws IOException {
        File[] contenido = new File(".").listFiles();
        if (contenido == null) {
            throw new IOException("No se puede leer la carpeta de trabajo.");
        }
        ArrayList<File> archivos = new ArrayList<>();
        for (File archivo : contenido) {
            String nombre = archivo.getName();
            if (archivo.isFile() && nombre.startsWith("ventas_") && nombre.endsWith(".txt")) {
                int posicion = archivos.size();
                while (posicion > 0 && archivos.get(posicion - 1).getName().compareTo(nombre) > 0) {
                    posicion--;
                }
                archivos.add(posicion, archivo);
            }
        }
        if (archivos.isEmpty()) {
            throw new IOException("No se encontraron archivos ventas_*.txt.");
        }
        return archivos;
    }

    /** Devuelve false cuando se omite un archivo por su cabecera. */
    private static boolean procesarVentas(File archivo, ArrayList<Producto> productos,
            ArrayList<Vendedor> vendedores) throws IOException {
        try (BufferedReader lector = Files.newBufferedReader(archivo.toPath(), StandardCharsets.UTF_8)) {
            Vendedor vendedor;
            try {
                String[] datos = separar(lector.readLine(), 2);
                vendedor = buscarVendedor(vendedores, datos[0], datos[1]);
                if (vendedor == null) {
                    throw new IllegalArgumentException("Vendedor inexistente; se omite el archivo.");
                }
            } catch (IllegalArgumentException e) {
                advertir(archivo, 1, e.getMessage());
                return false;
            }
            String linea;
            int numero = 1;
            while ((linea = lector.readLine()) != null) {
                numero++;
                try {
                    linea = linea.trim();
                    if (!linea.endsWith(";")) {
                        throw new IllegalArgumentException("Formato esperado: IDProducto;Cantidad;");
                    }
                    String[] datos = separar(linea.substring(0, linea.length() - 1), 2);
                    Producto producto = buscarProducto(productos, datos[0]);
                    if (producto == null) {
                        throw new IllegalArgumentException("Producto inexistente: " + datos[0]);
                    }
                    acumularVenta(producto, vendedor, enteroNoNegativo(datos[1]));
                } catch (IllegalArgumentException e) {
                    advertir(archivo, numero, e.getMessage());
                }
            }
        }
        return true;
    }

    /** Comprueba todos los límites antes de cambiar cualquier acumulado. */
    private static void acumularVenta(Producto producto, Vendedor vendedor, long cantidad) {
        if (cantidad > 0 && producto.precio > Long.MAX_VALUE / cantidad) {
            throw new IllegalArgumentException("El importe de la venta excede el límite de long.");
        }
        long importe = producto.precio * cantidad;
        if (cantidad > Long.MAX_VALUE - producto.cantidadVendida
                || importe > Long.MAX_VALUE - vendedor.totalRecaudado) {
            throw new IllegalArgumentException("El acumulado excede el límite de long.");
        }
        producto.cantidadVendida += cantidad;
        vendedor.totalRecaudado += importe;
    }

    /** Separa campos y rechaza filas vacías o incompletas. */
    private static String[] separar(String linea, int cantidad) {
        if (linea == null) {
            throw new IllegalArgumentException("Falta la cabecera del vendedor.");
        }
        String[] datos = linea.split(";", -1);
        if (datos.length != cantidad) {
            throw new IllegalArgumentException("Se esperaban " + cantidad + " campos.");
        }
        for (int i = 0; i < datos.length; i++) {
            datos[i] = datos[i].trim();
            if (datos[i].isEmpty()) {
                throw new IllegalArgumentException("No se permiten campos vacíos.");
            }
        }
        return datos;
    }

    /** Convierte a entero; no redondea ni acepta precios fraccionarios. */
    private static long enteroNoNegativo(String texto) {
        try {
            long valor = Long.parseLong(texto);
            if (valor >= 0) {
                return valor;
            }
        } catch (NumberFormatException e) {
            // Decimales, letras y valores fuera de rango comparten este mensaje.
        }
        throw new IllegalArgumentException("Se requiere un entero entre 0 y " + Long.MAX_VALUE + ".");
    }

    /** Busca por ID; null significa que no existe. */
    private static Producto buscarProducto(ArrayList<Producto> productos, String id) {
        for (Producto producto : productos) {
            if (producto.id.equals(id)) {
                return producto;
            }
        }
        return null;
    }

    /** Busca por tipo y documento, conservados como texto. */
    private static Vendedor buscarVendedor(ArrayList<Vendedor> vendedores, String tipo, String documento) {
        for (Vendedor vendedor : vendedores) {
            if (vendedor.tipoDocumento.equals(tipo) && vendedor.documento.equals(documento)) {
                return vendedor;
            }
        }
        return null;
    }

    /** Ordena por inserción: recaudo descendente, tipo y documento ascendentes. */
    private static void ordenarVendedores(ArrayList<Vendedor> vendedores) {
        for (int i = 1; i < vendedores.size(); i++) {
            Vendedor actual = vendedores.get(i);
            int j = i - 1;
            while (j >= 0 && vendedorVaAntes(actual, vendedores.get(j))) {
                vendedores.set(j + 1, vendedores.get(j));
                j--;
            }
            vendedores.set(j + 1, actual);
        }
    }

    private static boolean vendedorVaAntes(Vendedor primero, Vendedor segundo) {
        if (primero.totalRecaudado != segundo.totalRecaudado) {
            return primero.totalRecaudado > segundo.totalRecaudado;
        }
        int comparacion = primero.tipoDocumento.compareTo(segundo.tipoDocumento);
        if (comparacion != 0) {
            return comparacion < 0;
        }
        return primero.documento.compareTo(segundo.documento) < 0;
    }

    /** Ordena por inserción: unidades descendentes e ID ascendente. */
    private static void ordenarProductos(ArrayList<Producto> productos) {
        for (int i = 1; i < productos.size(); i++) {
            Producto actual = productos.get(i);
            int j = i - 1;
            while (j >= 0 && productoVaAntes(actual, productos.get(j))) {
                productos.set(j + 1, productos.get(j));
                j--;
            }
            productos.set(j + 1, actual);
        }
    }

    private static boolean productoVaAntes(Producto primero, Producto segundo) {
        if (primero.cantidadVendida != segundo.cantidadVendida) {
            return primero.cantidadVendida > segundo.cantidadVendida;
        }
        return primero.id.compareTo(segundo.id) < 0;
    }

    private static String filaVendedor(Vendedor vendedor) {
        return vendedor.nombres + " " + vendedor.apellidos + ";" + vendedor.totalRecaudado;
    }

    private static String filaProducto(Producto producto) {
        return producto.nombre + ";" + producto.precio;
    }

    /** Escribe sin encabezados; un error de escritura impide anunciar éxito. */
    private static void escribirReportes(ArrayList<Producto> productos,
            ArrayList<Vendedor> vendedores) throws IOException {
        Path destinoVendedores = Path.of("reporte_vendedores.csv");
        Path destinoProductos = Path.of("reporte_productos.csv");
        try (BufferedWriter escritor = Files.newBufferedWriter(destinoVendedores, StandardCharsets.UTF_8)) {
            for (Vendedor vendedor : vendedores) {
                escritor.write(filaVendedor(vendedor));
                escritor.newLine();
            }
        }
        try (BufferedWriter escritor = Files.newBufferedWriter(destinoProductos, StandardCharsets.UTF_8)) {
            for (Producto producto : productos) {
                escritor.write(filaProducto(producto));
                escritor.newLine();
            }
        }
        System.out.println("Exportación completada:");
        System.out.println(destinoVendedores.toAbsolutePath());
        System.out.println(destinoProductos.toAbsolutePath());
    }

    /** Muestra el texto original; la opción 3 valida los registros. */
    private static void mostrarEntradas() throws IOException {
        mostrarArchivo(new File("productos.txt"));
        mostrarArchivo(new File("vendedores.txt"));
        for (File archivo : buscarArchivosVentas()) {
            mostrarArchivo(archivo);
        }
        System.out.println("Entradas mostradas. Use 3 para validar y calcular o 4 para exportar.");
    }

    private static void mostrarArchivo(File archivo) throws IOException {
        System.out.println("\nARCHIVO: " + archivo.getName());
        try (BufferedReader lector = Files.newBufferedReader(archivo.toPath(), StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                System.out.println(linea);
            }
        }
    }

    private static void advertir(File archivo, int linea, String motivo) {
        System.out.println("ADVERTENCIA: " + archivo.getName() + ", línea " + linea + ": " + motivo);
    }
}
