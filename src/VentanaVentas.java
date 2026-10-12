import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/** Ventana sencilla: los formularios llaman al gestor y no calculan ventas. */
public final class VentanaVentas extends JFrame {
    private static final long serialVersionUID = 1L;
    private transient GestorVentas gestor;
    private boolean ocupado;
    private boolean actualizando;
    private final JLabel carpeta = new JLabel();
    private final JLabel estado = new JLabel();
    private final JLabel permiso = new JLabel();
    private final JTextArea registro = new JTextArea(4, 60);
    private final JButton cargar = new JButton("Cargar datos");
    private final JButton abrir = new JButton("Abrir carpeta");
    private final JButton generar = new JButton("Generar ejemplos");
    private final JButton recalcular = new JButton("Recalcular");
    private final JButton exportar = new JButton("Exportar CSV");
    private final JTable tablaProductos = tabla("ID", "Nombre", "Precio en pesos", "Unidades");
    private final JTable tablaVendedores = tabla("Tipo", "Documento", "Nombres", "Apellidos", "Recaudo en pesos");
    private final JTable tablaVentas = tabla("Archivo", "Línea", "Vendedor", "Producto", "Cantidad", "Estado");
    private final JTable reporteVendedores = tabla("Tipo", "Documento", "Nombre completo", "Recaudo en pesos");
    private final JTable reporteProductos = tabla("ID", "Nombre", "Precio en pesos", "Unidades vendidas");
    private final JTextField productoId = new JTextField(16);
    private final JTextField productoNombre = new JTextField(24);
    private final JTextField productoPrecio = new JTextField(16);
    private final JTextField vendedorTipo = new JTextField(16);
    private final JTextField vendedorDocumento = new JTextField(16);
    private final JTextField vendedorNombres = new JTextField(24);
    private final JTextField vendedorApellidos = new JTextField(24);
    private final JComboBox<Vendedor> ventaVendedor = new JComboBox<>();
    private final JComboBox<Producto> ventaProducto = new JComboBox<>();
    private final JTextField ventaCantidad = new JTextField(16);
    private final JLabel ventaOrigen = new JLabel("Se asignará al guardar");
    private final JButton nuevoProducto = new JButton("Nuevo");
    private final JButton guardarProducto = new JButton("Guardar");
    private final JButton eliminarProducto = new JButton("Eliminar");
    private final JButton nuevoVendedor = new JButton("Nuevo");
    private final JButton guardarVendedor = new JButton("Guardar");
    private final JButton eliminarVendedor = new JButton("Eliminar");
    private final JButton nuevaVenta = new JButton("Nuevo");
    private final JButton guardarVenta = new JButton("Guardar");
    private final JButton eliminarVenta = new JButton("Eliminar");

    /** Usa controles del sistema y un tamaño de texto legible, sin temas externos. */
    public static void prepararAspecto() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) { }
        javax.swing.plaf.FontUIResource fuente = new javax.swing.plaf.FontUIResource("SansSerif", java.awt.Font.PLAIN, 14);
        String[] claves = {"Label.font", "Button.font", "TextField.font", "TextArea.font", "Table.font",
            "TableHeader.font", "ComboBox.font", "TabbedPane.font", "OptionPane.messageFont", "OptionPane.buttonFont"};
        for (String clave : claves) { UIManager.put(clave, fuente); }
    }

    /** Se construye únicamente desde el hilo de eventos de Swing. */
    public VentanaVentas(GestorVentas gestor, String estadoInicial) {
        super("Ventas y vendedores - Entrega final");
        this.gestor = gestor;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 800);
        setMinimumSize(new Dimension(920, 680));
        setLocationRelativeTo(null);
        JPanel contenido = new JPanel(new BorderLayout(10, 10));
        contenido.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        setContentPane(contenido);
        JPanel cabecera = new JPanel(new BorderLayout(6, 8));
        JLabel titulo = new JLabel("VENTAS Y VENDEDORES · Gestión y reportes");
        titulo.setFont(titulo.getFont().deriveFont(java.awt.Font.BOLD, 20f));
        cabecera.add(titulo, BorderLayout.NORTH);
        cabecera.add(carpeta, BorderLayout.CENTER);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acciones.add(cargar); acciones.add(abrir); acciones.add(generar); acciones.add(recalcular); acciones.add(exportar);
        cabecera.add(acciones, BorderLayout.SOUTH);
        contenido.add(cabecera, BorderLayout.NORTH);
        JTabbedPane pestañas = new JTabbedPane();
        pestañas.addTab("Productos", panelProductos());
        pestañas.addTab("Vendedores", panelVendedores());
        pestañas.addTab("Ventas", panelVentas());
        pestañas.addTab("Reportes", panelReportes());
        contenido.add(pestañas, BorderLayout.CENTER);
        JPanel pie = new JPanel(new BorderLayout(4, 4));
        JPanel mensajes = new JPanel(new GridLayout(0, 1, 0, 4));
        mensajes.add(estado); mensajes.add(permiso);
        pie.add(mensajes, BorderLayout.NORTH);
        registro.setEditable(false); registro.setLineWrap(true); registro.setWrapStyleWord(true);
        pie.add(new JScrollPane(registro), BorderLayout.CENTER);
        contenido.add(pie, BorderLayout.SOUTH);
        conectarAcciones();
        refrescar();
        mostrarEstado(estadoInicial, estadoInicial.startsWith("ERROR"));
    }

    private static JTable tabla(String... columnas) {
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
        JTable tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(27);
        tabla.getTableHeader().setReorderingAllowed(false);
        return tabla;
    }

    private static JPanel formulario(String[] etiquetas, JComponent[] campos) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 12, 7));
        for (int i = 0; i < etiquetas.length; i++) {
            JLabel etiqueta = new JLabel(etiquetas[i]); etiqueta.setLabelFor(campos[i]);
            panel.add(etiqueta); panel.add(campos[i]);
        }
        return panel;
    }

    private static JPanel panelCrud(JPanel formulario, JTable tabla, String nota, JButton... botones) {
        JPanel panel = new JPanel(new BorderLayout(8, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 10, 8, 10));
        JPanel superior = new JPanel(new BorderLayout(6, 8));
        superior.add(formulario, BorderLayout.CENTER);
        superior.add(new JLabel(nota), BorderLayout.SOUTH);
        panel.add(superior, BorderLayout.NORTH);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        for (JButton boton : botones) { acciones.add(boton); }
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel panelProductos() {
        return panelCrud(formulario(new String[]{"Identificador", "Nombre", "Precio unitario (pesos enteros)"},
                new JComponent[]{productoId, productoNombre, productoPrecio}), tablaProductos,
                "Cambiar el precio recalcula las ventas con el precio actual del catálogo.",
                nuevoProducto, guardarProducto, eliminarProducto);
    }

    private JPanel panelVendedores() {
        return panelCrud(formulario(new String[]{"Tipo de documento", "Documento", "Nombres", "Apellidos"},
                new JComponent[]{vendedorTipo, vendedorDocumento, vendedorNombres, vendedorApellidos}), tablaVendedores,
                "Tipo y documento identifican al vendedor y se conservan al editar.",
                nuevoVendedor, guardarVendedor, eliminarVendedor);
    }

    private JPanel panelVentas() {
        return panelCrud(formulario(new String[]{"Vendedor", "Producto", "Cantidad (entero no negativo)", "Archivo de origen"},
                new JComponent[]{ventaVendedor, ventaProducto, ventaCantidad, ventaOrigen}), tablaVentas,
                "Al editar una venta se conserva su vendedor y archivo; cambian producto y cantidad.",
                nuevaVenta, guardarVenta, eliminarVenta);
    }

    private JPanel panelReportes() {
        JPanel arriba = new JPanel(new BorderLayout(6, 6));
        arriba.add(new JLabel("Vendedores · Orden descendente de recaudo"), BorderLayout.NORTH);
        arriba.add(new JScrollPane(reporteVendedores), BorderLayout.CENTER);
        JPanel abajo = new JPanel(new BorderLayout(6, 6));
        abajo.add(new JLabel("Productos · Orden descendente de unidades; el CSV contiene solo nombre y precio"), BorderLayout.NORTH);
        abajo.add(new JScrollPane(reporteProductos), BorderLayout.CENTER);
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, arriba, abajo);
        division.setResizeWeight(0.5); division.setDividerLocation(230);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 10, 8, 10)); panel.add(division);
        return panel;
    }

    private void conectarAcciones() {
        conectarArchivos();
        conectarNuevos();
        conectarSeleccion();
        conectarGuardado();
        conectarBorrado();
    }

    private void conectarArchivos() {
        cargar.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { cargarCarpeta(gestor.getCarpeta(), false); }
        });
        abrir.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                JFileChooser selector = new JFileChooser(gestor.getCarpeta().toFile());
                selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (selector.showOpenDialog(VentanaVentas.this) == JFileChooser.APPROVE_OPTION) {
                    cargarCarpeta(selector.getSelectedFile().toPath(), false);
                }
            }
        });
        generar.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!confirmar("Se respaldarán y sustituirán los datos de la carpeta activa. ¿Generar ejemplos?")) { return; }
                final Path destino = gestor.getCarpeta();
                ejecutar("Generando ejemplos", new Tarea() {
                    @Override public String realizar() throws Exception {
                        Path respaldo = GenerateInfoFiles.generarEjemplo(destino);
                        gestor = new GestorVentas(destino); gestor.cargar(); gestor.exportar();
                        return "Ejemplos generados y reportes exportados. Respaldo de entradas: " + respaldo;
                    }
                }, true);
            }
        });
        recalcular.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { cargarCarpeta(gestor.getCarpeta(), false); }
        });
        exportar.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { cargarCarpeta(gestor.getCarpeta(), true); }
        });
    }

    private void conectarNuevos() {
        nuevoProducto.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { limpiarProducto(); }
        });
        nuevoVendedor.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { limpiarVendedor(); }
        });
        nuevaVenta.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { limpiarVenta(); }
        });
    }

    private void conectarSeleccion() {
        tablaProductos.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                int indice = tablaProductos.getSelectedRow();
                if (!actualizando && !e.getValueIsAdjusting() && indice >= 0) {
                    Producto producto = gestor.getProductos().get(indice);
                    productoId.setText(producto.id); productoNombre.setText(producto.nombre);
                    productoPrecio.setText(String.valueOf(producto.precio)); actualizarControles();
                }
            }
        });
        tablaVendedores.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                int indice = tablaVendedores.getSelectedRow();
                if (!actualizando && !e.getValueIsAdjusting() && indice >= 0) {
                    Vendedor v = gestor.getVendedores().get(indice);
                    vendedorTipo.setText(v.tipoDocumento); vendedorDocumento.setText(v.documento);
                    vendedorNombres.setText(v.nombres); vendedorApellidos.setText(v.apellidos); actualizarControles();
                }
            }
        });
        tablaVentas.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                int indice = tablaVentas.getSelectedRow();
                if (!actualizando && !e.getValueIsAdjusting() && indice >= 0) {
                    Venta v = gestor.getVentas().get(indice);
                    ventaVendedor.setSelectedItem(gestor.buscarVendedor(v.tipoDocumento, v.documento));
                    ventaProducto.setSelectedItem(gestor.buscarProducto(v.productoId));
                    ventaCantidad.setText(String.valueOf(v.cantidad));
                    ventaOrigen.setText(v.archivo + " · línea " + v.linea); actualizarControles();
                }
            }
        });
    }

    private void conectarGuardado() {
        guardarProducto.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                final boolean nuevo = tablaProductos.getSelectedRow() < 0;
                final String id = productoId.getText(), nombre = productoNombre.getText(), precio = productoPrecio.getText();
                ejecutar("Guardando producto", new Tarea() {
                    @Override public String realizar() throws Exception {
                        if (nuevo) { gestor.agregarProducto(id, nombre, GestorVentas.entero(precio)); }
                        else { gestor.modificarProducto(id, nombre, GestorVentas.entero(precio)); }
                        return "Producto guardado. TXT y reportes actualizados.";
                    }
                }, false);
            }
        });
        guardarVendedor.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                final boolean nuevo = tablaVendedores.getSelectedRow() < 0;
                final String tipo = vendedorTipo.getText(), documento = vendedorDocumento.getText();
                final String nombres = vendedorNombres.getText(), apellidos = vendedorApellidos.getText();
                ejecutar("Guardando vendedor", new Tarea() {
                    @Override public String realizar() throws Exception {
                        if (nuevo) { gestor.agregarVendedor(tipo, documento, nombres, apellidos); }
                        else { gestor.modificarVendedor(tipo, documento, nombres, apellidos); }
                        return "Vendedor guardado. TXT y reportes actualizados.";
                    }
                }, false);
            }
        });
        guardarVenta.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                final int indice = tablaVentas.getSelectedRow();
                final Vendedor vendedor = (Vendedor) ventaVendedor.getSelectedItem();
                final Producto producto = (Producto) ventaProducto.getSelectedItem();
                final String cantidad = ventaCantidad.getText();
                ejecutar("Guardando venta", new Tarea() {
                    @Override public String realizar() throws Exception {
                        if (vendedor == null || producto == null) { throw new IllegalArgumentException("Seleccione vendedor y producto."); }
                        if (indice < 0) { gestor.agregarVenta(vendedor.tipoDocumento, vendedor.documento, producto.id, GestorVentas.entero(cantidad)); }
                        else { gestor.modificarVenta(indice, producto.id, GestorVentas.entero(cantidad)); }
                        return "Venta guardada en su archivo. TXT y reportes actualizados.";
                    }
                }, false);
            }
        });
    }

    private void conectarBorrado() {
        eliminarProducto.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                final String id = productoId.getText();
                if (confirmar("¿Eliminar el producto seleccionado?")) {
                    ejecutar("Eliminando producto", new Tarea() {
                        @Override public String realizar() throws Exception { gestor.eliminarProducto(id); return "Producto eliminado. Reportes actualizados."; }
                    }, false);
                }
            }
        });
        eliminarVendedor.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                final String tipo = vendedorTipo.getText(), documento = vendedorDocumento.getText();
                if (confirmar("¿Eliminar el vendedor seleccionado?")) {
                    ejecutar("Eliminando vendedor", new Tarea() {
                        @Override public String realizar() throws Exception { gestor.eliminarVendedor(tipo, documento); return "Vendedor eliminado. Reportes actualizados."; }
                    }, false);
                }
            }
        });
        eliminarVenta.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                final int indice = tablaVentas.getSelectedRow();
                if (confirmar("¿Eliminar la fila de venta seleccionada?")) {
                    ejecutar("Eliminando venta", new Tarea() {
                        @Override public String realizar() throws Exception { gestor.eliminarVenta(indice); return "Venta eliminada. Se conserva la cabecera del archivo."; }
                    }, false);
                }
            }
        });
    }

    private void cargarCarpeta(final Path destino, final boolean escribirCsv) {
        ejecutar("Cargando y calculando", new Tarea() {
            @Override public String realizar() throws Exception {
                gestor = new GestorVentas(destino); gestor.cargar();
                if (escribirCsv) {
                    gestor.exportar();
                    return "Exportación completada en " + gestor.getCarpeta();
                }
                return "Carga y cálculo completados. Pulse Exportar CSV para actualizar los archivos.";
            }
        }, true);
    }

    private interface Tarea { String realizar() throws Exception; }

    /** Solo doInBackground toca archivos; done actualiza los controles en el hilo de eventos. */
    private void ejecutar(String mensaje, final Tarea tarea, final boolean refrescarError) {
        ocupado = true; actualizarControles(); mostrarEstado(mensaje + "…", false);
        SwingWorker<String, Void> trabajador = new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception { return tarea.realizar(); }
            @Override protected void done() {
                ocupado = false;
                try {
                    String resultado = get(); refrescar(); mostrarEstado(resultado, false);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); mostrarError(e, refrescarError);
                } catch (ExecutionException e) {
                    mostrarError(e.getCause(), refrescarError);
                } finally { actualizarControles(); }
            }
        };
        trabajador.execute();
    }

    private void mostrarError(Throwable error, boolean refrescarError) {
        if (refrescarError) { refrescar(); }
        String mensaje = "ERROR: " + error.getMessage(); mostrarEstado(mensaje, true);
        JTextArea texto = new JTextArea(mensaje, 6, 60);
        texto.setEditable(false); texto.setLineWrap(true); texto.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this, new JScrollPane(texto), "Operación no completada", JOptionPane.ERROR_MESSAGE);
    }

    private boolean confirmar(String mensaje) {
        return JOptionPane.showConfirmDialog(this, mensaje, "Confirmar operación",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void refrescar() {
        actualizando = true;
        DefaultTableModel p = modelo(tablaProductos), v = modelo(tablaVendedores), s = modelo(tablaVentas);
        DefaultTableModel rv = modelo(reporteVendedores), rp = modelo(reporteProductos);
        p.setRowCount(0); v.setRowCount(0); s.setRowCount(0); rv.setRowCount(0); rp.setRowCount(0);
        ventaProducto.removeAllItems(); ventaVendedor.removeAllItems();
        for (Producto producto : gestor.getProductos()) {
            p.addRow(new Object[]{producto.id, producto.nombre, producto.precio, producto.cantidadVendida});
            ventaProducto.addItem(producto);
        }
        for (Vendedor vendedor : gestor.getVendedores()) {
            v.addRow(new Object[]{vendedor.tipoDocumento, vendedor.documento, vendedor.nombres, vendedor.apellidos, vendedor.totalRecaudado});
            ventaVendedor.addItem(vendedor);
        }
        for (Venta venta : gestor.getVentas()) {
            s.addRow(new Object[]{venta.archivo, venta.linea, venta.tipoDocumento + " " + venta.documento,
                gestor.buscarProducto(venta.productoId).toString(), venta.cantidad, venta.contabilizada ? "Contabilizada" : "Omitida"});
        }
        for (Vendedor vendedor : gestor.vendedoresOrdenados()) {
            rv.addRow(new Object[]{vendedor.tipoDocumento, vendedor.documento, vendedor.nombres + " " + vendedor.apellidos, vendedor.totalRecaudado});
        }
        for (Producto producto : gestor.productosOrdenados()) {
            rp.addRow(new Object[]{producto.id, producto.nombre, producto.precio, producto.cantidadVendida});
        }
        carpeta.setText("Carpeta activa: " + gestor.getCarpeta()); carpeta.setToolTipText(gestor.getCarpeta().toString());
        actualizando = false;
        limpiarProducto(); limpiarVendedor(); limpiarVenta(); actualizarControles();
    }

    private static DefaultTableModel modelo(JTable tabla) { return (DefaultTableModel) tabla.getModel(); }

    private void limpiarProducto() {
        tablaProductos.clearSelection(); productoId.setText(""); productoNombre.setText(""); productoPrecio.setText(""); actualizarControles();
    }
    private void limpiarVendedor() {
        tablaVendedores.clearSelection(); vendedorTipo.setText(""); vendedorDocumento.setText("");
        vendedorNombres.setText(""); vendedorApellidos.setText(""); actualizarControles();
    }
    private void limpiarVenta() {
        tablaVentas.clearSelection(); ventaCantidad.setText(""); ventaOrigen.setText("Se asignará al guardar"); actualizarControles();
    }

    private void actualizarControles() {
        boolean editar = !ocupado && gestor.puedeEditar();
        cargar.setEnabled(!ocupado); abrir.setEnabled(!ocupado); generar.setEnabled(!ocupado);
        recalcular.setEnabled(!ocupado); exportar.setEnabled(!ocupado && gestor.estaCargado());
        nuevoProducto.setEnabled(editar); guardarProducto.setEnabled(editar);
        eliminarProducto.setEnabled(editar && tablaProductos.getSelectedRow() >= 0);
        nuevoVendedor.setEnabled(editar); guardarVendedor.setEnabled(editar);
        eliminarVendedor.setEnabled(editar && tablaVendedores.getSelectedRow() >= 0);
        nuevaVenta.setEnabled(editar); guardarVenta.setEnabled(editar);
        eliminarVenta.setEnabled(editar && tablaVentas.getSelectedRow() >= 0);
        productoId.setEnabled(editar && tablaProductos.getSelectedRow() < 0);
        productoNombre.setEnabled(editar); productoPrecio.setEnabled(editar);
        vendedorTipo.setEnabled(editar && tablaVendedores.getSelectedRow() < 0);
        vendedorDocumento.setEnabled(editar && tablaVendedores.getSelectedRow() < 0);
        vendedorNombres.setEnabled(editar); vendedorApellidos.setEnabled(editar);
        ventaVendedor.setEnabled(editar && tablaVentas.getSelectedRow() < 0);
        ventaProducto.setEnabled(editar); ventaCantidad.setEnabled(editar);
        tablaProductos.setEnabled(!ocupado); tablaVendedores.setEnabled(!ocupado); tablaVentas.setEnabled(!ocupado);
        permiso.setText(gestor.puedeEditar() ? "Seleccione una fila para editar o pulse Nuevo. Guardar actualiza TXT y CSV."
                : "Edición bloqueada: cargue un conjunto válido sin advertencias; consulte el registro.");
    }

    private void mostrarEstado(String mensaje, boolean error) {
        estado.setText(mensaje.length() > 150 ? mensaje.substring(0, 150) + "…" : mensaje);
        estado.setToolTipText(mensaje); estado.setForeground(error ? new Color(170, 25, 25) : new Color(25, 90, 65));
        StringBuilder texto = new StringBuilder(mensaje + "\n");
        for (String aviso : gestor.getAdvertencias()) { texto.append(aviso).append('\n'); }
        if (gestor.getUltimoRespaldo() != null) { texto.append("Último respaldo: ").append(gestor.getUltimoRespaldo()).append('\n'); }
        if (gestor.tieneReportesActuales()) {
            texto.append("CSV: ").append(gestor.getCarpeta().resolve("reporte_vendedores.csv")).append('\n');
            texto.append("CSV: ").append(gestor.getCarpeta().resolve("reporte_productos.csv"));
        } else if (gestor.estaCargado()) {
            texto.append("Reportes calculados en pantalla; exportación pendiente. Los CSV anteriores no son resultados actuales.");
        }
        registro.setText(texto.toString()); registro.setCaretPosition(0);
    }
}
