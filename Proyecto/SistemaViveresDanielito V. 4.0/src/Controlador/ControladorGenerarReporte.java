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
        // Specific filtered report
        vista.getButtonGenerarEspecifico().addActionListener((ActionEvent e) -> generarReporteFiltrado());
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

    private void generarReporteFiltrado() {
        String producto = (String) vista.getProductoCombo().getSelectedItem();
        String categoria = (String) vista.getCategoriaCombo().getSelectedItem();
        String estado = (String) vista.getEstadoCombo().getSelectedItem();
        String desdeText = vista.getFechaDesdeField().getText().trim();
        String hastaText = vista.getFechaHastaField().getText().trim();

        BasicDBObject filtro = new BasicDBObject();
        if (producto != null && !producto.equals("Todos los productos")) {
            int start = producto.lastIndexOf("(");
            int end = producto.lastIndexOf(")");
            if (start != -1 && end != -1) {
                String codigo = producto.substring(start + 1, end);
                filtro.put("codigoProducto", codigo);
            } else {
                filtro.put("nombreProducto", producto);
            }
        }

        ArrayList<Movimiento> lista = movDao.listarMovimientos(filtro);

        // filtrar por fechas. si el campo esta vacio o el texto no tiene un
        // formato de fecha valido, simplemente no se filtra por esa fecha
        // (antes habia una condicion rara con dobles negaciones que hacia
        // exactamente lo mismo pero de una forma muy dificil de entender)
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");
        Date desde = parsearFechaSegura(fmt, desdeText);
        Date hasta = parsearFechaSegura(fmt, hastaText);

        java.util.Map<String, String> codigoAProveedor = new java.util.HashMap<>();
        for (Producto p : prodDao.listarProductos()) {
            if (p.getCodigo() != null && p.getProveedor() != null) {
                codigoAProveedor.put(p.getCodigo(), p.getProveedor());
            }
        }

        java.util.List<Movimiento> filtrada = new java.util.ArrayList<>();
        for (Movimiento m : lista) {
            boolean ok = true;
            // proveedor
            if (ok && categoria != null && !categoria.equals("Todos los proveedores")) {
                String prov = codigoAProveedor.getOrDefault(m.getCodigoProducto(), "");
                if (!categoria.equals(prov)) ok = false;
            }
            
            // fecha
            try {
                Date f = fmt.parse(m.getFecha());
                if (desde != null && f.before(desde)) ok = false;
                if (hasta != null && f.after(hasta)) ok = false;
            } catch (Exception ex) {
                // ignore parse
            }
            // estado (ahora es tipo de movimiento ENTRADA / SALIDA)
            if (ok && estado != null && !estado.equals("Todos")) {
                String tipoMovimiento = m.getTipo() != null ? m.getTipo().toUpperCase() : "";
                if (estado.equals("ENTRADA") && !tipoMovimiento.equals("ENTRADA")) ok = false;
                if (estado.equals("SALIDA") && !tipoMovimiento.equals("SALIDA")) ok = false;
            }
            if (ok) filtrada.add(m);
        }

        // armamos una descripcion legible de los filtros que se usaron, para
        // que quede anotada en el encabezado del reporte (asi quien lo lea
        // despues sabe exactamente que se filtro, sin tener que adivinar)
        StringBuilder descripcion = new StringBuilder("Filtrado");
        descripcion.append(" | Producto: ").append(producto == null ? "Todos los productos" : producto);
        descripcion.append(" | Proveedor: ").append(categoria == null ? "Todos los proveedores" : categoria);
        descripcion.append(" | Movimiento: ").append(estado == null ? "Todos" : estado);
        descripcion.append(" | Desde: ").append(desde == null ? "(sin filtro)" : desdeText);
        descripcion.append(" | Hasta: ").append(hasta == null ? "(sin filtro)" : hastaText);

        escribirReporteYNotificar(new ArrayList<>(filtrada), "reporte_movimientos_filtrado", descripcion.toString());
    }

    /**
     * intenta convertir un texto a fecha con el formato dado. si el texto
     * viene vacio, null, o no tiene un formato de fecha valido (por ejemplo
     * si el usuario dejo el "dd/MM/yyyy" de ejemplo sin cambiar) regresa
     * null en vez de tronar, total que estas fechas son filtros opcionales.
     */
    private Date parsearFechaSegura(SimpleDateFormat formato, String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        try {
            return formato.parse(texto.trim());
        } catch (ParseException ex) {
            return null;
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
