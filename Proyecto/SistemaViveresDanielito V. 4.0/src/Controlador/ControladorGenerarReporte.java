package Controlador;

import Modelo.Movimiento;
import Modelo.MovimientoDAO;
import Modelo.Producto;
import Modelo.ProductoDAO;
import Vista.FrmGenerarReporte;
import com.mongodb.BasicDBObject;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JOptionPane;

/**
 * Controller adapted to use the original NetBeans-generated FrmGenerarReporte fields.
 * It will not modify the form layout; it only uses available components to produce TXT reports.
 */
public class ControladorGenerarReporte {

    // ====== datos que aparecen en el encabezado del reporte ======
    // si algun dia hay que cambiar el nombre de la empresa o quien genera
    // los reportes, este es el UNICO lugar del codigo donde hay que tocarlo.
    private static final String NOMBRE_EMPRESA = "VÍVERES DANIELITO";
    private static final String RESPONSABLE_REPORTE = "Enrique Guaiguacundo";

    private final FrmGenerarReporte vista;
    private final MovimientoDAO movDao;
    private final ProductoDAO prodDao;

    public ControladorGenerarReporte(FrmGenerarReporte vista, MovimientoDAO movDao, ProductoDAO prodDao) {
        this.vista = vista;
        this.movDao = movDao;
        this.prodDao = prodDao;
        cargarDatosIniciales();
        configurarListeners();
    }

    private void cargarDatosIniciales() {
        // poblar productos
        vista.getProductoCombo().removeAllItems();
        vista.getProductoCombo().addItem("Todos los productos");
        for (Producto p : prodDao.listarProductos()) {
            vista.getProductoCombo().addItem(p.getNombre() + " (" + p.getCodigo() + ")");
        }

        vista.getCategoriaCombo().removeAllItems();
        vista.getCategoriaCombo().addItem("Todos los proveedores");
        vista.getCategoriaCombo().setEnabled(false);

        // estados
        vista.getEstadoCombo().removeAllItems();
        vista.getEstadoCombo().addItem("Todos");
        vista.getEstadoCombo().addItem("ENTRADA");
        vista.getEstadoCombo().addItem("SALIDA");
    }

    private void configurarListeners() {
        // antes este boton generaba un reporte de movimientos filtrado;
        // ahora genera el reporte de inventario actual (stock), usando los
        // mismos combos de producto/proveedor (el de estado se ignora
        // porque el stock no tiene "entradas" ni "salidas", es una foto del
        // inventario en este momento)
        vista.getButtonGenerarEspecifico().addActionListener((ActionEvent e) -> generarReporteStock());
        // General report (no filters)
        vista.getButtonGenerarGeneral().addActionListener((ActionEvent e) -> generarReporteGeneral());
        
        // Listener para habilitar proveedorCombo
        vista.getProductoCombo().addActionListener((ActionEvent e) -> {
            String seleccionado = (String) vista.getProductoCombo().getSelectedItem();
            vista.getCategoriaCombo().removeAllItems();
            vista.getCategoriaCombo().addItem("Todos los proveedores");

            if (seleccionado != null && !seleccionado.equals("Todos los productos")) {
                vista.getCategoriaCombo().setEnabled(true);
                java.util.Set<String> proveedores = new java.util.HashSet<>();
                for (Producto p : prodDao.listarProductos()) {
                    String label = p.getNombre() + " (" + p.getCodigo() + ")";
                    if (label.equals(seleccionado) && p.getProveedor() != null && !p.getProveedor().isEmpty()) {
                        proveedores.add(p.getProveedor());
                    }
                }
                for (String p : proveedores) {
                    vista.getCategoriaCombo().addItem(p);
                }
            } else {
                vista.getCategoriaCombo().setEnabled(false);
            }
        });
    }

    private void generarReporteGeneral() {
        BasicDBObject filtro = new BasicDBObject();
        ArrayList<Movimiento> lista = movDao.listarMovimientos(filtro);
        escribirReporteYNotificar(lista, "reporte_movimientos_general", "General (todos los movimientos)");
    }

    /**
     * genera el reporte de "inventario actual": lo que hay en bodega hoy
     * por cada producto, junto con su valor total (precio unitario x
     * stock). a diferencia del reporte de movimientos, aqui no se filtra
     * por fecha ni por tipo de movimiento (estado), porque el stock es una
     * sola foto del momento, no un historial.
     */
    private void generarReporteStock() {
        String producto = (String) vista.getProductoCombo().getSelectedItem();
        String categoria = (String) vista.getCategoriaCombo().getSelectedItem();

        ArrayList<Producto> productos = prodDao.listarProductos();
        ArrayList<Producto> filtrados = new ArrayList<>();
        for (Producto p : productos) {
            boolean ok = true;
            if (producto != null && !producto.equals("Todos los productos")) {
                String etiqueta = p.getNombre() + " (" + p.getCodigo() + ")";
                if (!etiqueta.equals(producto)) ok = false;
            }
            if (ok && categoria != null && !categoria.equals("Todos los proveedores")) {
                String prov = p.getProveedor() == null ? "" : p.getProveedor();
                if (!categoria.equals(prov)) ok = false;
            }
            if (ok) filtrados.add(p);
        }

        StringBuilder descripcion = new StringBuilder("Inventario actual");
        descripcion.append(" | Producto: ").append(producto == null ? "Todos los productos" : producto);
        descripcion.append(" | Proveedor: ").append(categoria == null ? "Todos los proveedores" : categoria);

        escribirReporteStockYNotificar(filtrados, descripcion.toString());
    }

    private void escribirReporteStockYNotificar(ArrayList<Producto> productos, String descripcionTipo) {
        try {
            File dir = new File("reports");
            if (!dir.exists()) dir.mkdirs();
            String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File out = new File(dir, "reporte_stock_" + time + ".txt");

            final int ANCHO = 110;
            String lineaDoble = "=".repeat(ANCHO);
            String lineaSimple = "-".repeat(ANCHO);

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(out, java.nio.charset.StandardCharsets.UTF_8))) {

                bw.write(lineaDoble);
                bw.newLine();
                bw.write(centrar(NOMBRE_EMPRESA, ANCHO));
                bw.newLine();
                bw.write(centrar("Reporte de Inventario Actual (Stock)", ANCHO));
                bw.newLine();
                bw.write(lineaDoble);
                bw.newLine();
                bw.write(String.format("Tipo de reporte    : %s", descripcionTipo));
                bw.newLine();
                bw.write(String.format("Generado por       : %s", RESPONSABLE_REPORTE));
                bw.newLine();
                bw.write(String.format("Fecha de emisión   : %s", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date())));
                bw.newLine();
                bw.write(String.format("Total de productos : %d", productos.size()));
                bw.newLine();
                bw.write(lineaDoble);
                bw.newLine();

                String formato = "%-12s %-25s %-15s %-15s %10s %12s %15s";
                String header = String.format(formato, "Codigo", "Producto", "Tipo", "Proveedor", "Stock", "Precio/U", "Valor Total");
                bw.write(header);
                bw.newLine();
                bw.write(lineaSimple);
                bw.newLine();

                int totalUnidades = 0;
                double valorTotalInventario = 0.0;

                for (Producto p : productos) {
                    double valor = p.getPrecioUnit() * p.getStock();
                    totalUnidades += p.getStock();
                    valorTotalInventario += valor;
                    String prov = (p.getProveedor() == null || p.getProveedor().isEmpty()) ? "N/A" : p.getProveedor();
                    String linea = String.format(formato,
                            p.getCodigo(), p.getNombre(), p.getTipo(), prov,
                            String.valueOf(p.getStock()),
                            String.format("$%.2f", p.getPrecioUnit()),
                            String.format("$%.2f", valor));
                    bw.write(linea);
                    bw.newLine();
                }
                bw.write(lineaSimple);
                bw.newLine();
                bw.newLine();

                bw.write("RESUMEN");
                bw.newLine();
                bw.write("-------");
                bw.newLine();
                bw.write(String.format("Productos listados         : %d", productos.size()));
                bw.newLine();
                bw.write(String.format("Unidades totales en stock  : %d", totalUnidades));
                bw.newLine();
                bw.write(String.format("Valor total del inventario : $%.2f", valorTotalInventario));
                bw.newLine();
                bw.newLine();
                bw.write(lineaDoble);
                bw.newLine();
                bw.write(centrar("Fin del reporte — Sistema Víveres Danielito", ANCHO));
                bw.newLine();
                bw.write(lineaDoble);
                bw.newLine();
            }
            JOptionPane.showMessageDialog(vista, "Reporte guardado: " + out.getAbsolutePath() + "\nProductos: " + productos.size());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(vista, "Error al guardar reporte: " + ex.getMessage());
        }
    }

    private void escribirReporteYNotificar(ArrayList<Movimiento> lista, String prefix, String descripcionTipo) {
        java.util.Map<String, String> codigoAProveedor = new java.util.HashMap<>();
        for (Producto p : prodDao.listarProductos()) {
            if (p.getCodigo() != null && p.getProveedor() != null) {
                codigoAProveedor.put(p.getCodigo(), p.getProveedor());
            }
        }
        try {
            File dir = new File("reports");
            if (!dir.exists()) dir.mkdirs();
            String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File out = new File(dir, prefix + "_" + time + ".txt");

            // ancho total del reporte (cuantos caracteres mide cada linea
            // separadora). si se agregan o quitan columnas mas adelante,
            // este numero hay que ajustarlo para que las rayas sigan
            // midiendo lo mismo que el encabezado de la tabla.
            final int ANCHO = 119;
            String lineaDoble = "=".repeat(ANCHO);
            String lineaSimple = "-".repeat(ANCHO);

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(out, java.nio.charset.StandardCharsets.UTF_8))) {

                // ---------- encabezado del reporte ----------
                bw.write(lineaDoble);
                bw.newLine();
                bw.write(centrar(NOMBRE_EMPRESA, ANCHO));
                bw.newLine();
                bw.write(centrar("Reporte de Movimientos de Inventario", ANCHO));
                bw.newLine();
                bw.write(lineaDoble);
                bw.newLine();
                bw.write(String.format("Tipo de reporte   : %s", descripcionTipo));
                bw.newLine();
                bw.write(String.format("Generado por      : %s", RESPONSABLE_REPORTE));
                bw.newLine();
                bw.write(String.format("Fecha de emisión  : %s", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date())));
                bw.newLine();
                bw.write(String.format("Total de registros: %d", lista.size()));
                bw.newLine();
                bw.write(lineaDoble);
                bw.newLine();

                // ---------- tabla de movimientos ----------
                String formato = "%-12s %-25s %-15s %-15s %-10s %-10s %-10s %-15s";
                String header = String.format(formato, "Fecha", "Producto", "Codigo", "Movimiento", "Cantidad", "Precio/U", "Total", "Proveedor");
                bw.write(header);
                bw.newLine();
                bw.write(lineaSimple);
                bw.newLine();

                // de paso vamos sumando los totales para el resumen de abajo,
                // asi no hay que recorrer la lista una segunda vez
                int totalEntradas = 0;
                int totalSalidas = 0;
                double montoEntradas = 0.0;
                double montoSalidas = 0.0;

                for (Movimiento m : lista) {
                    String prov = codigoAProveedor.getOrDefault(m.getCodigoProducto(), "N/A");
                    if (prov.isEmpty()) prov = "N/A";
                    String linea = String.format(formato, m.getFecha(), m.getNombreProducto(), m.getCodigoProducto(), m.getTipo(), String.valueOf(m.getCantidad()), String.valueOf(m.getPrecioUnitario()), String.valueOf(m.getTotal()), prov);
                    bw.write(linea);
                    bw.newLine();

                    if ("Entrada".equalsIgnoreCase(m.getTipo())) {
                        totalEntradas++;
                        montoEntradas += m.getTotal();
                    } else if ("Salida".equalsIgnoreCase(m.getTipo())) {
                        totalSalidas++;
                        montoSalidas += m.getTotal();
                    }
                }
                bw.write(lineaSimple);
                bw.newLine();
                bw.newLine();

                // ---------- resumen final ----------
                bw.write("RESUMEN");
                bw.newLine();
                bw.write("-------");
                bw.newLine();
                bw.write(String.format("Entradas registradas : %-6d Monto total entradas : $%.2f", totalEntradas, montoEntradas));
                bw.newLine();
                bw.write(String.format("Salidas registradas  : %-6d Monto total salidas  : $%.2f", totalSalidas, montoSalidas));
                bw.newLine();
                bw.write(String.format("Balance del periodo  : $%.2f", montoEntradas - montoSalidas));
                bw.newLine();
                bw.newLine();
                bw.write(lineaDoble);
                bw.newLine();
                bw.write(centrar("Fin del reporte — Sistema Víveres Danielito", ANCHO));
                bw.newLine();
                bw.write(lineaDoble);
                bw.newLine();
            }
            JOptionPane.showMessageDialog(vista, "Reporte guardado: " + out.getAbsolutePath() + "\nFilas: " + lista.size());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(vista, "Error al guardar reporte: " + ex.getMessage());
        }
    }

    /**
     * centra un texto dentro de un ancho total, rellenando con espacios a
     * los dos lados. es solo para que el encabezado del reporte se vea
     * prolijo, como un titulo de verdad en vez de texto pegado a la izquierda.
     */
    private String centrar(String texto, int ancho) {
        if (texto.length() >= ancho) {
            return texto;
        }
        int espacios = ancho - texto.length();
        int izquierda = espacios / 2;
        int derecha = espacios - izquierda;
        return " ".repeat(izquierda) + texto + " ".repeat(derecha);
    }
}
