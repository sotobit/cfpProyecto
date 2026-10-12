import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;

/** Lectura, validación, CRUD y reportes; no depende de la interfaz. */
public class GestorVentas {
    private final Path carpeta;
    private final ArrayList<Producto> productos = new ArrayList<>();
    private final ArrayList<Vendedor> vendedores = new ArrayList<>();
    private final ArrayList<Venta> ventas = new ArrayList<>();
    private final ArrayList<ArchivoVentas> archivosVentas = new ArrayList<>();
    private final ArrayList<String> advertencias = new ArrayList<>();
    private final ArrayList<String> nombresOriginales = new ArrayList<>();
    private final ArrayList<String> textosOriginales = new ArrayList<>();
    private boolean cargado;
    private boolean reportesExportados;
    private Path ultimoRespaldo;

    /** Todas las entradas, salidas y respaldos pertenecen a esta carpeta explícita. */
    public GestorVentas(Path carpeta) {
        this.carpeta = carpeta.toAbsolutePath().normalize();
    }

    public Path getCarpeta() { return carpeta; }
    public Path getUltimoRespaldo() { return ultimoRespaldo; }
    public boolean estaCargado() { return cargado; }
    public boolean tieneReportesActuales() { return reportesExportados; }
    public boolean puedeEditar() { return cargado && advertencias.isEmpty(); }
    public ArrayList<Producto> getProductos() { return new ArrayList<>(productos); }
    public ArrayList<Vendedor> getVendedores() { return new ArrayList<>(vendedores); }
    public ArrayList<Venta> getVentas() { return new ArrayList<>(ventas); }
    public ArrayList<String> getAdvertencias() { return new ArrayList<>(advertencias); }

    /** Solo un conjunto totalmente ausente permite generar el ejemplo inicial. */
    public static boolean sinEntradas(Path carpeta) throws IOException {
        return !Files.exists(carpeta.resolve("productos.txt"), LinkOption.NOFOLLOW_LINKS)
                && !Files.exists(carpeta.resolve("vendedores.txt"), LinkOption.NOFOLLOW_LINKS)
                && nombresVentas(carpeta).isEmpty();
    }

    /** Ordena por nombre para que lectura, duplicados y nuevas ventas sean reproducibles. */
    public static ArrayList<String> nombresVentas(Path carpeta) throws IOException {
        File[] contenido = carpeta.toFile().listFiles();
        if (contenido == null) {
            throw new IOException("No se puede leer la carpeta: " + carpeta);
        }
        ArrayList<String> nombres = new ArrayList<>();
        for (File archivo : contenido) {
            String nombre = archivo.getName();
            if (nombre.startsWith("ventas_") && nombre.endsWith(".txt")) {
                int posicion = nombres.size();
                while (posicion > 0 && nombres.get(posicion - 1).compareTo(nombre) > 0) {
                    posicion--;
                }
                nombres.add(posicion, nombre);
            }
        }
        return nombres;
    }

    /** Relee desde cero. Una lectura incompleta no conserva tablas de una carga anterior. */
    public void cargar() throws IOException {
        limpiar();
        try {
            leerProductos();
            leerVendedores();
            ArrayList<String> nombres = nombresVentas(carpeta);
            if (nombres.isEmpty()) {
                throw new IOException("No se encontraron archivos ventas_*.txt.");
            }
            for (String nombre : nombres) {
                leerVentas(nombre);
            }
            if (archivosVentas.isEmpty()) {
                throw new IOException("Ningún archivo de ventas tiene una cabecera válida.");
            }
            calcular(false);
            cargado = true;
        } catch (IOException e) {
            limpiar();
            throw e;
        }
    }

    private void limpiar() {
        cargado = false;
        reportesExportados = false;
        productos.clear(); vendedores.clear(); ventas.clear(); archivosVentas.clear();
        advertencias.clear(); nombresOriginales.clear(); textosOriginales.clear();
    }

    private String leerTexto(String nombre) throws IOException {
        Path archivo = carpeta.resolve(nombre);
        if (!Files.isRegularFile(archivo, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Falta un archivo regular de entrada: " + nombre);
        }
        String texto = Files.readString(archivo, StandardCharsets.UTF_8);
        nombresOriginales.add(nombre);
        textosOriginales.add(texto);
        return texto;
    }

    private void leerProductos() throws IOException {
        try (BufferedReader lector = new BufferedReader(new StringReader(leerTexto("productos.txt")))) {
            String linea; int numero = 0;
            while ((linea = lector.readLine()) != null) {
                numero++;
                try {
                    String[] datos = separar(linea, 3);
                    long precio = entero(datos[2]);
                    if (buscarProducto(datos[0]) != null) {
                        throw new IllegalArgumentException("ID de producto duplicado.");
                    }
                    productos.add(new Producto(datos[0], datos[1], precio));
                } catch (IllegalArgumentException e) {
                    advertir("productos.txt", numero, e.getMessage());
                }
            }
        }
        if (productos.isEmpty()) { throw new IOException("productos.txt no contiene productos válidos."); }
    }

    private void leerVendedores() throws IOException {
        try (BufferedReader lector = new BufferedReader(new StringReader(leerTexto("vendedores.txt")))) {
            String linea; int numero = 0;
            while ((linea = lector.readLine()) != null) {
                numero++;
                try {
                    String[] datos = separar(linea, 4);
                    if (buscarVendedor(datos[0], datos[1]) != null) {
                        throw new IllegalArgumentException("Documento de vendedor duplicado.");
                    }
                    vendedores.add(new Vendedor(datos[0], datos[1], datos[2], datos[3]));
                } catch (IllegalArgumentException e) {
                    advertir("vendedores.txt", numero, e.getMessage());
                }
            }
        }
        if (vendedores.isEmpty()) { throw new IOException("vendedores.txt no contiene vendedores válidos."); }
    }

    private void leerVentas(String nombre) throws IOException {
        try (BufferedReader lector = new BufferedReader(new StringReader(leerTexto(nombre)))) {
            String[] cabecera;
            try {
                cabecera = separar(lector.readLine(), 2);
                if (buscarVendedor(cabecera[0], cabecera[1]) == null) {
                    throw new IllegalArgumentException("Vendedor inexistente; se omite el archivo.");
                }
            } catch (IllegalArgumentException e) {
                advertir(nombre, 1, e.getMessage());
                return;
            }
            archivosVentas.add(new ArchivoVentas(nombre, cabecera[0], cabecera[1]));
            String linea; int numero = 1;
            while ((linea = lector.readLine()) != null) {
                numero++;
                try {
                    linea = linea.trim();
                    if (!linea.endsWith(";")) {
                        throw new IllegalArgumentException("Formato esperado: IDProducto;Cantidad;");
                    }
                    String[] datos = separar(linea.substring(0, linea.length() - 1), 2);
                    if (buscarProducto(datos[0]) == null) {
                        throw new IllegalArgumentException("Producto inexistente: " + datos[0]);
                    }
                    ventas.add(new Venta(cabecera[0], cabecera[1], datos[0], entero(datos[1]), nombre, numero));
                } catch (IllegalArgumentException e) {
                    advertir(nombre, numero, e.getMessage());
                }
            }
        }
    }

    /** El modo estricto rechaza un cambio CRUD que cause desbordamiento. */
    private void calcular(boolean estricto) {
        for (Producto producto : productos) { producto.cantidadVendida = 0; }
        for (Vendedor vendedor : vendedores) { vendedor.totalRecaudado = 0; }
        for (Venta venta : ventas) {
            venta.contabilizada = false;
            try {
                Producto producto = buscarProducto(venta.productoId);
                Vendedor vendedor = buscarVendedor(venta.tipoDocumento, venta.documento);
                if (producto == null || vendedor == null) {
                    throw new IllegalArgumentException("La venta tiene una referencia inexistente.");
                }
                if (venta.cantidad > 0 && producto.precio > Long.MAX_VALUE / venta.cantidad) {
                    throw new IllegalArgumentException("El importe de la venta excede el límite de long.");
                }
                long importe = producto.precio * venta.cantidad;
                if (venta.cantidad > Long.MAX_VALUE - producto.cantidadVendida
                        || importe > Long.MAX_VALUE - vendedor.totalRecaudado) {
                    throw new IllegalArgumentException("El acumulado excede el límite de long.");
                }
                producto.cantidadVendida += venta.cantidad;
                vendedor.totalRecaudado += importe;
                venta.contabilizada = true;
            } catch (IllegalArgumentException e) {
                if (estricto) { throw e; }
                advertir(venta.archivo, venta.linea, e.getMessage());
            }
        }
    }

    private static String[] separar(String linea, int cantidad) {
        if (linea == null) { throw new IllegalArgumentException("Falta la cabecera del vendedor."); }
        String[] datos = linea.split(";", -1);
        if (datos.length != cantidad) { throw new IllegalArgumentException("Se esperaban " + cantidad + " campos."); }
        for (int i = 0; i < datos.length; i++) { datos[i] = texto(datos[i]); }
        return datos;
    }

    /** Texto plano: el separador y los saltos de línea no pueden formar parte de un campo. */
    public static String texto(String valor) {
        if (valor == null || valor.trim().isEmpty()) { throw new IllegalArgumentException("No se permiten campos vacíos."); }
        if (valor.contains(";") || valor.contains("\n") || valor.contains("\r")) {
            throw new IllegalArgumentException("Los campos no pueden contener punto y coma ni saltos de línea.");
        }
        return valor.trim();
    }

    /** Convierte sin redondear; el rango permitido incluye cero. */
    public static long entero(String valor) {
        if (valor == null) { throw new IllegalArgumentException("Se requiere un entero no negativo."); }
        try {
            long numero = Long.parseLong(valor.trim());
            if (numero >= 0) { return numero; }
        } catch (NumberFormatException e) { }
        throw new IllegalArgumentException("Se requiere un entero entre 0 y " + Long.MAX_VALUE + ".");
    }

    private static void noNegativo(long numero) {
        if (numero < 0) { throw new IllegalArgumentException("El precio y la cantidad no pueden ser negativos."); }
    }

    public Producto buscarProducto(String id) {
        for (Producto producto : productos) { if (producto.id.equals(id)) { return producto; } }
        return null;
    }

    public Vendedor buscarVendedor(String tipo, String documento) {
        for (Vendedor vendedor : vendedores) {
            if (vendedor.tipoDocumento.equals(tipo) && vendedor.documento.equals(documento)) { return vendedor; }
        }
        return null;
    }

    public ArrayList<Producto> productosOrdenados() {
        ArrayList<Producto> ordenados = new ArrayList<>(productos);
        for (int i = 1; i < ordenados.size(); i++) {
            Producto actual = ordenados.get(i); int j = i - 1;
            while (j >= 0 && productoVaAntes(actual, ordenados.get(j))) {
                ordenados.set(j + 1, ordenados.get(j)); j--;
            }
            ordenados.set(j + 1, actual);
        }
        return ordenados;
    }

    private static boolean productoVaAntes(Producto a, Producto b) {
        if (a.cantidadVendida != b.cantidadVendida) { return a.cantidadVendida > b.cantidadVendida; }
        return a.id.compareTo(b.id) < 0;
    }

    public ArrayList<Vendedor> vendedoresOrdenados() {
        ArrayList<Vendedor> ordenados = new ArrayList<>(vendedores);
        for (int i = 1; i < ordenados.size(); i++) {
            Vendedor actual = ordenados.get(i); int j = i - 1;
            while (j >= 0 && vendedorVaAntes(actual, ordenados.get(j))) {
                ordenados.set(j + 1, ordenados.get(j)); j--;
            }
            ordenados.set(j + 1, actual);
        }
        return ordenados;
    }

    private static boolean vendedorVaAntes(Vendedor a, Vendedor b) {
        if (a.totalRecaudado != b.totalRecaudado) { return a.totalRecaudado > b.totalRecaudado; }
        int comparacion = a.tipoDocumento.compareTo(b.tipoDocumento);
        if (comparacion != 0) { return comparacion < 0; }
        return a.documento.compareTo(b.documento) < 0;
    }

    /** Exporta ambas salidas con restauración ante un fallo de sustitución. */
    public void exportar() throws IOException {
        reportesExportados = false;
        if (!cargado) { throw new IOException("Cargue un conjunto válido antes de exportar."); }
        ArrayList<String> nombres = new ArrayList<>();
        ArrayList<String> contenidos = new ArrayList<>();
        agregarReportes(nombres, contenidos);
        ultimoRespaldo = ArchivosSeguros.guardar(carpeta, nombres, contenidos, new ArrayList<String>());
        reportesExportados = true;
    }

    private void agregarReportes(ArrayList<String> nombres, ArrayList<String> contenidos) {
        StringBuilder salida = new StringBuilder();
        for (Vendedor vendedor : vendedoresOrdenados()) {
            salida.append(csv(vendedor.nombres + " " + vendedor.apellidos)).append(';')
                    .append(vendedor.totalRecaudado).append('\n');
        }
        nombres.add("reporte_vendedores.csv"); contenidos.add(salida.toString());
        salida = new StringBuilder();
        for (Producto producto : productosOrdenados()) {
            salida.append(csv(producto.nombre)).append(';').append(producto.precio).append('\n');
        }
        nombres.add("reporte_productos.csv"); contenidos.add(salida.toString());
    }

    private static String csv(String valor) {
        if (valor.contains("\"")) { return "\"" + valor.replace("\"", "\"\"") + "\""; }
        return valor;
    }

    /** CRUD opera sobre una copia: los datos visibles no cambian si falla el guardado. */
    private GestorVentas copiaParaEditar() throws IOException {
        if (!puedeEditar()) { throw new IllegalArgumentException("Corrija las advertencias y vuelva a cargar antes de editar."); }
        if (nombresVentas(carpeta).size() != nombresOriginales.size() - 2) {
            throw new IOException("Los archivos cambiaron fuera de la aplicación. Use Cargar datos.");
        }
        for (int i = 0; i < nombresOriginales.size(); i++) {
            if (!Files.readString(carpeta.resolve(nombresOriginales.get(i)), StandardCharsets.UTF_8).equals(textosOriginales.get(i))) {
                throw new IOException("Los archivos cambiaron fuera de la aplicación. Use Cargar datos.");
            }
        }
        GestorVentas copia = new GestorVentas(carpeta);
        for (Producto p : productos) { copia.productos.add(new Producto(p.id, p.nombre, p.precio)); }
        for (Vendedor v : vendedores) { copia.vendedores.add(new Vendedor(v.tipoDocumento, v.documento, v.nombres, v.apellidos)); }
        for (Venta v : ventas) { copia.ventas.add(new Venta(v.tipoDocumento, v.documento, v.productoId, v.cantidad, v.archivo, v.linea)); }
        copia.archivosVentas.addAll(archivosVentas);
        return copia;
    }

    private void guardarCambio(GestorVentas copia) throws IOException {
        copia.calcular(true);
        ArrayList<String> nombres = new ArrayList<>();
        ArrayList<String> contenidos = new ArrayList<>();
        StringBuilder salida = new StringBuilder();
        for (Producto p : copia.productos) { salida.append(p.id).append(';').append(p.nombre).append(';').append(p.precio).append('\n'); }
        nombres.add("productos.txt"); contenidos.add(salida.toString());
        salida = new StringBuilder();
        for (Vendedor v : copia.vendedores) {
            salida.append(v.tipoDocumento).append(';').append(v.documento).append(';').append(v.nombres).append(';').append(v.apellidos).append('\n');
        }
        nombres.add("vendedores.txt"); contenidos.add(salida.toString());
        for (ArchivoVentas archivo : copia.archivosVentas) {
            salida = new StringBuilder(archivo.tipoDocumento + ";" + archivo.documento + "\n");
            for (Venta venta : copia.ventas) {
                if (venta.archivo.equals(archivo.nombre)) { salida.append(venta.productoId).append(';').append(venta.cantidad).append(";\n"); }
            }
            nombres.add(archivo.nombre); contenidos.add(salida.toString());
        }
        copia.agregarReportes(nombres, contenidos);
        ArrayList<String> eliminar = new ArrayList<>();
        for (ArchivoVentas archivo : archivosVentas) {
            if (!nombres.contains(archivo.nombre)) { eliminar.add(archivo.nombre); }
        }
        Path respaldo = ArchivosSeguros.guardar(carpeta, nombres, contenidos, eliminar);
        cargar();
        ultimoRespaldo = respaldo;
        reportesExportados = true;
    }

    /** Crea un ID nuevo y guarda el conjunto junto con sus reportes. */
    public void agregarProducto(String id, String nombre, long precio) throws IOException {
        id = texto(id); nombre = texto(nombre); noNegativo(precio);
        GestorVentas copia = copiaParaEditar();
        if (copia.buscarProducto(id) != null) { throw new IllegalArgumentException("ID de producto duplicado."); }
        copia.productos.add(new Producto(id, nombre, precio)); guardarCambio(copia);
    }

    /** Cambia nombre y precio; conserva el ID y revalúa las ventas existentes. */
    public void modificarProducto(String id, String nombre, long precio) throws IOException {
        nombre = texto(nombre); noNegativo(precio);
        GestorVentas copia = copiaParaEditar(); Producto anterior = copia.buscarProducto(id);
        if (anterior == null) { throw new IllegalArgumentException("Producto inexistente."); }
        copia.productos.set(copia.productos.indexOf(anterior), new Producto(id, nombre, precio)); guardarCambio(copia);
    }

    /** Solo permite borrar productos sin filas de ventas asociadas. */
    public void eliminarProducto(String id) throws IOException {
        GestorVentas copia = copiaParaEditar(); Producto producto = copia.buscarProducto(id);
        if (producto == null) { throw new IllegalArgumentException("Producto inexistente."); }
        if (copia.productos.size() == 1) { throw new IllegalArgumentException("Debe conservar al menos un producto."); }
        for (Venta venta : copia.ventas) {
            if (venta.productoId.equals(id)) { throw new IllegalArgumentException("El producto tiene ventas asociadas."); }
        }
        copia.productos.remove(producto); guardarCambio(copia);
    }

    /** Crea una identidad tipo/documento que no exista en el catálogo. */
    public void agregarVendedor(String tipo, String documento, String nombres, String apellidos) throws IOException {
        tipo = texto(tipo); documento = texto(documento); nombres = texto(nombres); apellidos = texto(apellidos);
        GestorVentas copia = copiaParaEditar();
        if (copia.buscarVendedor(tipo, documento) != null) { throw new IllegalArgumentException("Documento de vendedor duplicado."); }
        copia.vendedores.add(new Vendedor(tipo, documento, nombres, apellidos)); guardarCambio(copia);
    }

    /** Cambia los nombres, conservando tipo y documento. */
    public void modificarVendedor(String tipo, String documento, String nombres, String apellidos) throws IOException {
        nombres = texto(nombres); apellidos = texto(apellidos);
        GestorVentas copia = copiaParaEditar(); Vendedor anterior = copia.buscarVendedor(tipo, documento);
        if (anterior == null) { throw new IllegalArgumentException("Vendedor inexistente."); }
        copia.vendedores.set(copia.vendedores.indexOf(anterior), new Vendedor(tipo, documento, nombres, apellidos)); guardarCambio(copia);
    }

    /** Retira un vendedor sin ventas y los archivos que solo contienen su cabecera. */
    public void eliminarVendedor(String tipo, String documento) throws IOException {
        GestorVentas copia = copiaParaEditar(); Vendedor vendedor = copia.buscarVendedor(tipo, documento);
        if (vendedor == null) { throw new IllegalArgumentException("Vendedor inexistente."); }
        if (copia.vendedores.size() == 1) { throw new IllegalArgumentException("Debe conservar al menos un vendedor."); }
        for (Venta venta : copia.ventas) {
            if (venta.tipoDocumento.equals(tipo) && venta.documento.equals(documento)) {
                throw new IllegalArgumentException("El vendedor tiene ventas asociadas.");
            }
        }
        copia.vendedores.remove(vendedor);
        for (int i = copia.archivosVentas.size() - 1; i >= 0; i--) {
            ArchivoVentas archivo = copia.archivosVentas.get(i);
            if (archivo.tipoDocumento.equals(tipo) && archivo.documento.equals(documento)) { copia.archivosVentas.remove(i); }
        }
        // Si solo quedaban archivos vacíos del vendedor retirado, conservar una cabecera válida.
        if (copia.archivosVentas.isEmpty()) {
            Vendedor primero = copia.vendedores.get(0);
            copia.archivosVentas.add(new ArchivoVentas(copia.nombreNuevoArchivo(), primero.tipoDocumento, primero.documento));
        }
        guardarCambio(copia);
    }

    /** Añade una fila al primer archivo del vendedor o crea un archivo seguro. */
    public void agregarVenta(String tipo, String documento, String productoId, long cantidad) throws IOException {
        noNegativo(cantidad); GestorVentas copia = copiaParaEditar();
        if (copia.buscarVendedor(tipo, documento) == null || copia.buscarProducto(productoId) == null) {
            throw new IllegalArgumentException("Seleccione un vendedor y un producto existentes.");
        }
        ArchivoVentas destino = null;
        for (ArchivoVentas archivo : copia.archivosVentas) {
            if (archivo.tipoDocumento.equals(tipo) && archivo.documento.equals(documento)) { destino = archivo; break; }
        }
        if (destino == null) {
            destino = new ArchivoVentas(copia.nombreNuevoArchivo(), tipo, documento); copia.archivosVentas.add(destino);
        }
        copia.ventas.add(new Venta(tipo, documento, productoId, cantidad, destino.nombre, 0)); guardarCambio(copia);
    }

    private String nombreNuevoArchivo() {
        int numero = 1;
        while (Files.exists(carpeta.resolve("ventas_ui_" + numero + ".txt"), LinkOption.NOFOLLOW_LINKS)) { numero++; }
        return "ventas_ui_" + numero + ".txt";
    }

    /** El índice es la fila de la lista cargada; vendedor y archivo no cambian. */
    public void modificarVenta(int indice, String productoId, long cantidad) throws IOException {
        noNegativo(cantidad); GestorVentas copia = copiaParaEditar();
        if (indice < 0 || indice >= copia.ventas.size() || copia.buscarProducto(productoId) == null) {
            throw new IllegalArgumentException("Seleccione una venta y un producto existentes.");
        }
        Venta anterior = copia.ventas.get(indice);
        copia.ventas.set(indice, new Venta(anterior.tipoDocumento, anterior.documento, productoId, cantidad, anterior.archivo, anterior.linea));
        guardarCambio(copia);
    }

    /** Borra una fila de venta y conserva la cabecera si el archivo queda vacío. */
    public void eliminarVenta(int indice) throws IOException {
        GestorVentas copia = copiaParaEditar();
        if (indice < 0 || indice >= copia.ventas.size()) { throw new IllegalArgumentException("Seleccione una venta."); }
        copia.ventas.remove(indice); guardarCambio(copia);
    }

    private void advertir(String archivo, int linea, String motivo) {
        advertencias.add("ADVERTENCIA: " + archivo + ", línea " + linea + ": " + motivo);
    }
}
