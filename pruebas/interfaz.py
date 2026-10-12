"""Prueba botones y formularios Swing reales; las capturas se obtienen de los componentes en ejecución."""
import argparse
import json
from pathlib import Path
import subprocess
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument('--java-home', required=True)
parser.add_argument('--evidence-dir', type=Path)
args = parser.parse_args()
repo = Path(__file__).resolve().parents[1]
jdk = Path(args.java_home).resolve()
classes = repo/'build/classes'
classes.mkdir(parents=True, exist_ok=True)
compiled = subprocess.run([str(jdk/'bin/javac.exe'), '-encoding', 'UTF-8', '--release', '21', '-Xlint:all',
                           '-d', str(classes), *map(str, (repo/'src').glob('*.java'))], capture_output=True, encoding='utf-8')
assert compiled.returncode == 0 and not compiled.stderr.strip(), compiled.stderr

# El auxiliar existe únicamente en una carpeta temporal, nunca en src ni en el JAR.
driver = r'''
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

public class PruebaInterfazTemporal {
    private static VentanaVentas ventana;
    private static Path capturas;
    private static void exige(boolean condicion, String motivo) {
        if (!condicion) throw new AssertionError(motivo);
    }
    private static Object campo(String nombre) {
        try {
            Field campo = VentanaVentas.class.getDeclaredField(nombre); campo.setAccessible(true); return campo.get(ventana);
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    private static void enUi(Runnable tarea) throws Exception { SwingUtilities.invokeAndWait(tarea); }
    private static void pulsar(final String nombre) throws Exception {
        enUi(new Runnable() {
            public void run() {
                JButton boton = (JButton) campo(nombre);
                exige(boton.isEnabled(), "Botón deshabilitado: " + nombre); boton.doClick();
            }
        });
    }
    private static void escribir(final String nombre, final String texto) throws Exception {
        enUi(new Runnable() { public void run() { ((JTextField) campo(nombre)).setText(texto); } });
    }
    private static void seleccionar(final String nombre, final int fila) throws Exception {
        enUi(new Runnable() { public void run() { ((JTable) campo(nombre)).setRowSelectionInterval(fila, fila); } });
    }
    private static void elegir(final String nombre, final int indice) throws Exception {
        enUi(new Runnable() { public void run() { ((JComboBox<?>) campo(nombre)).setSelectedIndex(indice); } });
    }
    private static void pestaña(final int indice) throws Exception {
        enUi(new Runnable() { public void run() {
            ((JTabbedPane) ((JPanel) ventana.getContentPane()).getComponent(1)).setSelectedIndex(indice);
        } });
    }
    private static void esperar(final String esperado) throws Exception {
        for (int i = 0; i < 200; i++) {
            final String[] estado = new String[1];
            enUi(new Runnable() { public void run() { estado[0] = ((JLabel) campo("estado")).getText(); } });
            if (estado[0].contains(esperado)) return;
            Thread.sleep(50);
        }
        throw new AssertionError("No llegó el estado: " + esperado);
    }
    private static JDialog dialogo(String titulo) {
        for (Window ventana : Window.getWindows()) {
            if (ventana instanceof JDialog && ventana.isVisible() && ((JDialog) ventana).getTitle().equals(titulo)) {
                return (JDialog) ventana;
            }
        }
        return null;
    }
    private static JOptionPane panelConfirmacion(Container contenedor) {
        for (Component hijo : contenedor.getComponents()) {
            if (hijo instanceof JOptionPane) return (JOptionPane) hijo;
            if (hijo instanceof Container) {
                JOptionPane resultado = panelConfirmacion((Container) hijo); if (resultado != null) return resultado;
            }
        }
        return null;
    }
    private static JFileChooser selector(Container contenedor) {
        for (Component hijo : contenedor.getComponents()) {
            if (hijo instanceof JFileChooser) return (JFileChooser) hijo;
            if (hijo instanceof Container) {
                JFileChooser encontrado = selector((Container) hijo); if (encontrado != null) return encontrado;
            }
        }
        return null;
    }
    private static void abrirCarpeta(final Path carpeta) throws Exception {
        SwingUtilities.invokeLater(new Runnable() { public void run() { ((JButton) campo("abrir")).doClick(); } });
        final JFileChooser[] archivo = new JFileChooser[1];
        for (int i = 0; i < 100; i++) {
            enUi(new Runnable() { public void run() {
                for (Window abierta : Window.getWindows()) {
                    if (abierta instanceof JDialog && abierta.isVisible()) {
                        JFileChooser encontrado = selector((Container) abierta); if (encontrado != null) archivo[0] = encontrado;
                    }
                }
            } });
            if (archivo[0] != null) break;
            Thread.sleep(50);
        }
        exige(archivo[0] != null, "Selector de carpeta ausente");
        enUi(new Runnable() { public void run() { archivo[0].setSelectedFile(carpeta.toFile()); archivo[0].approveSelection(); } });
    }
    private static void eliminar(final String boton, String resultado) throws Exception {
        SwingUtilities.invokeLater(new Runnable() { public void run() { ((JButton) campo(boton)).doClick(); } });
        final JDialog[] confirmacion = new JDialog[1];
        for (int i = 0; i < 100; i++) {
            enUi(new Runnable() { public void run() { confirmacion[0] = dialogo("Confirmar operación"); } });
            if (confirmacion[0] != null) break;
            Thread.sleep(50);
        }
        exige(confirmacion[0] != null, "No apareció confirmación de borrado");
        enUi(new Runnable() { public void run() { panelConfirmacion(confirmacion[0]).setValue(JOptionPane.YES_OPTION); } });
        esperar(resultado);
    }
    private static void imagen(Container componente, String nombre) {
        try {
            BufferedImage imagen = new BufferedImage(componente.getWidth(), componente.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D grafico = imagen.createGraphics(); componente.printAll(grafico); grafico.dispose();
            ImageIO.write(imagen, "png", capturas.resolve(nombre).toFile());
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private static void capturar(final String nombre) throws Exception {
        enUi(new Runnable() { public void run() { imagen(ventana.getContentPane(), nombre); } });
    }
    private static void cerrarError(final String imagen) throws Exception {
        enUi(new Runnable() { public void run() {
            JDialog error = dialogo("Operación no completada"); exige(error != null, "No apareció el error");
            imagen(error.getContentPane(), imagen); error.dispose();
        } });
    }
    private static void comprobarFilas(final String tabla, final int filas) throws Exception {
        enUi(new Runnable() { public void run() { exige(((JTable) campo(tabla)).getRowCount() == filas, "Filas en " + tabla); } });
    }
    public static void main(String[] args) throws Exception {
        capturas = Path.of(args[0]); Files.createDirectories(capturas);
        final GestorVentas gestor = new GestorVentas(Path.of(".")); gestor.cargar(); gestor.exportar();
        enUi(new Runnable() { public void run() {
            VentanaVentas.prepararAspecto(); ventana = new VentanaVentas(gestor, "Procesamiento y exportación completados. 0 advertencias.");
            ventana.setVisible(true);
        } });
        try {
            comprobarFilas("tablaProductos", 3); comprobarFilas("tablaVendedores", 3); comprobarFilas("tablaVentas", 4);
            capturar("01_productos.png"); pestaña(3); capturar("02_reportes_manuales.png");
            System.out.println("APROBADO: Arranque, tablas y reportes manuales");

            pestaña(0); escribir("productoId", "4"); escribir("productoNombre", "Audífonos"); escribir("productoPrecio", "80");
            enUi(new Runnable() { public void run() {
                ((JButton) campo("guardarProducto")).doClick();
                exige(!((JButton) campo("guardarProducto")).isEnabled(), "Guardar debe bloquearse mientras trabaja");
            } });
            esperar("Producto guardado"); comprobarFilas("tablaProductos", 4);
            seleccionar("tablaProductos", 3); escribir("productoPrecio", "95"); pulsar("guardarProducto"); esperar("Producto guardado");
            seleccionar("tablaProductos", 3); capturar("03_producto_crud.png");
            enUi(new Runnable() { public void run() { exige(!((JTextField) campo("productoId")).isEnabled(), "ID editable"); } });
            eliminar("eliminarProducto", "Producto eliminado"); comprobarFilas("tablaProductos", 3);
            System.out.println("APROBADO: CRUD de productos e identidad inmutable");
            System.out.println("APROBADO: Controles bloqueados durante escritura y recuperados al terminar");

            pestaña(1); escribir("vendedorTipo", "CE"); escribir("vendedorDocumento", "40");
            escribir("vendedorNombres", "Lina"); escribir("vendedorApellidos", "Rojas");
            pulsar("guardarVendedor"); esperar("Vendedor guardado"); seleccionar("tablaVendedores", 3);
            escribir("vendedorNombres", "Lucía"); pulsar("guardarVendedor"); esperar("Vendedor guardado");
            seleccionar("tablaVendedores", 3); capturar("04_vendedor_crud.png");
            eliminar("eliminarVendedor", "Vendedor eliminado"); comprobarFilas("tablaVendedores", 3);
            System.out.println("APROBADO: CRUD de vendedores");

            pestaña(2); elegir("ventaVendedor", 2); elegir("ventaProducto", 2); escribir("ventaCantidad", "2");
            pulsar("guardarVenta"); esperar("Venta guardada"); comprobarFilas("tablaVentas", 5);
            seleccionar("tablaVentas", 4); elegir("ventaProducto", 0); escribir("ventaCantidad", "3");
            pulsar("guardarVenta"); esperar("Venta guardada"); seleccionar("tablaVentas", 4); capturar("05_venta_crud.png");
            eliminar("eliminarVenta", "Venta eliminada"); comprobarFilas("tablaVentas", 4);
            exige(Files.readString(Path.of("ventas_ui_1.txt")).equals("CC;30\n"), "Cabecera vacía perdida");
            System.out.println("APROBADO: CRUD de ventas, archivo de origen y cabecera vacía");

            pestaña(0); pulsar("nuevoProducto"); escribir("productoId", "4"); escribir("productoNombre", "Prueba"); escribir("productoPrecio", "-1");
            pulsar("guardarProducto"); esperar("ERROR"); cerrarError("06_validacion_precio.png");
            comprobarFilas("tablaProductos", 3); pulsar("nuevoProducto");
            System.out.println("APROBADO: Precio inválido muestra error sin guardar");

            pulsar("cargar"); esperar("Carga y cálculo completados");
            pulsar("recalcular"); esperar("Carga y cálculo completados");
            pulsar("exportar"); esperar("Exportación completada"); comprobarFilas("tablaVentas", 4);
            System.out.println("APROBADO: Cargar, recalcular y exportar sin duplicar ventas");

            String original = Files.readString(Path.of("ventas_10.txt"), StandardCharsets.UTF_8);
            Files.writeString(Path.of("ventas_10.txt"), original + "99;1;\n", StandardCharsets.UTF_8);
            pulsar("cargar"); esperar("Carga y cálculo completados"); capturar("07_advertencias.png");
            enUi(new Runnable() { public void run() {
                exige(!((JButton) campo("guardarProducto")).isEnabled(), "CRUD habilitado con corrupción");
                exige(((JTextArea) campo("registro")).getText().contains("Producto inexistente"), "Advertencia ausente");
            } });
            Files.writeString(Path.of("ventas_10.txt"), original, StandardCharsets.UTF_8);
            pulsar("cargar"); esperar("Carga y cálculo completados");
            System.out.println("APROBADO: Advertencias bloquean CRUD y corregir archivos lo recupera");

            byte[] reporte = Files.readAllBytes(Path.of("reporte_productos.csv"));
            byte[] anterior = Files.readAllBytes(Path.of("reporte_vendedores.csv"));
            Files.delete(Path.of("reporte_productos.csv")); Files.createDirectory(Path.of("reporte_productos.csv"));
            pulsar("exportar"); esperar("ERROR"); cerrarError("08_error_exportacion.png");
            exige(java.util.Arrays.equals(anterior, Files.readAllBytes(Path.of("reporte_vendedores.csv"))), "Se alteró el otro CSV");
            enUi(new Runnable() { public void run() { exige(((JButton) campo("cargar")).isEnabled(), "Controles no recuperados"); } });
            Files.delete(Path.of("reporte_productos.csv")); Files.write(Path.of("reporte_productos.csv"), reporte);
            pulsar("cargar"); esperar("Carga y cálculo completados");
            System.out.println("APROBADO: Error de exportación conserva reportes y recupera controles");

            final GestorVentas releido = new GestorVentas(Path.of(".")); releido.cargar();
            exige(releido.getProductos().size() == 3 && releido.getVendedores().size() == 3 && releido.getVentas().size() == 4, "Persistencia incorrecta");
            exige(releido.buscarVendedor("CC", "10").totalRecaudado == 400 && releido.buscarProducto("2").cantidadVendida == 12, "Acumulados incorrectos");
            System.out.println("APROBADO: Persistencia y acumulados después del recorrido de interfaz");

            Path otraCarpeta = Files.createDirectory(Path.of("otro_conjunto"));
            abrirCarpeta(otraCarpeta.toAbsolutePath()); esperar("ERROR"); cerrarError("09_carga_incompleta.png");
            comprobarFilas("tablaProductos", 0);
            System.out.println("APROBADO: Abrir carpeta incompleta informa error y limpia resultados anteriores");
            eliminar("generar", "Ejemplos generados"); comprobarFilas("tablaProductos", 4); comprobarFilas("tablaVendedores", 3);
            capturar("10_generacion.png");
            abrirCarpeta(Path.of(".").toAbsolutePath().normalize()); esperar("Carga y cálculo completados");
            pulsar("exportar"); esperar("Exportación completada");
            comprobarFilas("tablaProductos", 3); comprobarFilas("tablaVentas", 4);
            System.out.println("APROBADO: Generar ejemplos desde la interfaz y volver a la carpeta original");
        } finally {
            enUi(new Runnable() { public void run() {
                for (Window abierta : Window.getWindows()) abierta.dispose();
            } });
        }
    }
}
'''

files = {'productos.txt': '1;Teclado;100\n2;Mouse;50\n3;Monitor;200\n',
         'vendedores.txt': 'CC;10;Ana;Pérez\nCC;20;Luis;Gómez\nCC;30;Eva;Ruiz\n',
         'ventas_10.txt': 'CC;10\n1;2;\n2;1;\n', 'ventas_10_extra.txt': 'CC;10\n2;3;\n', 'ventas_20.txt': 'CC;20\n2;8;\n'}
with tempfile.TemporaryDirectory(prefix='cfp-interfaz-') as td:
    temp = Path(td); work = temp/'datos'; work.mkdir()
    evidence = args.evidence_dir.resolve() if args.evidence_dir else temp/'evidencias'
    if evidence == repo or repo in evidence.parents:
        parser.error('Las evidencias deben guardarse fuera del repositorio')
    evidence.mkdir(parents=True, exist_ok=True)
    for name, text in files.items():
        (work/name).write_text(text, encoding='utf-8', newline='\n')
    source = temp/'PruebaInterfazTemporal.java'; source.write_text(driver, encoding='utf-8')
    subprocess.run([str(jdk/'bin/javac.exe'), '-encoding', 'UTF-8', '--release', '21', '-cp', str(classes),
                    '-d', str(temp), str(source)], check=True)
    import os
    proc = subprocess.run([str(jdk/'bin/java.exe'), '-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8',
                           '-cp', str(classes)+os.pathsep+str(temp), 'PruebaInterfazTemporal', str(evidence/'capturas')],
                          cwd=work, capture_output=True, encoding='utf-8', timeout=90)
    print(proc.stdout)
    assert proc.returncode == 0, proc.stderr
    scenarios = [line.removeprefix('APROBADO: ') for line in proc.stdout.splitlines() if line.startswith('APROBADO: ')]
    assert len(scenarios) == 12, scenarios
    # Un nuevo proceso procesa los datos que dejaron los botones del recorrido CRUD.
    restarted = subprocess.run([str(jdk/'bin/java.exe'), '-Dstdout.encoding=UTF-8', '-cp', str(classes), 'main', '--sin-interfaz'],
                               cwd=work, encoding='utf-8', capture_output=True, timeout=20)
    assert restarted.returncode == 0 and 'Ana Pérez;400' in restarted.stdout
    assert (work/'reporte_vendedores.csv').read_text(encoding='utf-8') == 'Ana Pérez;400\nLuis Gómez;400\nEva Ruiz;0\n'
    assert (work/'reporte_productos.csv').read_text(encoding='utf-8') == 'Mouse;50\nTeclado;100\nMonitor;200\n'
    if args.evidence_dir:
        (evidence/'salida_interfaz.txt').write_text(proc.stdout, encoding='utf-8')
        (evidence/'resultados_interfaz.json').write_text(json.dumps({
            'metodo': 'Controles Swing reales mediante sus eventos; printAll captura los componentes en ejecución, sin otras ventanas del escritorio.',
            'casos': [{'caso': name, 'resultado': 'APROBADO'} for name in scenarios],
            'reinicio_proceso': 'APROBADO', 'capturas': [p.name for p in sorted((evidence/'capturas').glob('*.png'))]
        }, ensure_ascii=False, indent=2), encoding='utf-8')
print('12 escenarios de interfaz APROBADOS y persistencia verificada en un nuevo proceso')
