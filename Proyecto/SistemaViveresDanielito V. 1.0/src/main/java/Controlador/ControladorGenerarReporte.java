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
        escribirReporteYNotificar(lista, "reporte_movimientos_general");
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

        // filtrar por fechas
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");
        Date desde = null, hasta = null;
        try {
            if (desdeText != null && !desdeText.isEmpty() && !desdeText.contains(".") && !desdeText.contains("/") == false) {
                // try parse; tolerant
                desde = fmt.parse(desdeText);
            } else {
                try { desde = fmt.parse(desdeText); } catch (Exception ex) { desde = null; }
            }
        } catch (Exception ex) {
            desde = null;
        }
        try {
            if (hastaText != null && !hastaText.isEmpty()) {
                hasta = fmt.parse(hastaText);
            }
        } catch (Exception ex) {
            hasta = null;
        }

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

        escribirReporteYNotificar(new ArrayList<>(filtrada), "reporte_movimientos_filtrado");
    }

    private void escribirReporteYNotificar(ArrayList<Movimiento> lista, String prefix) {
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
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {
                String formato = "%-12s %-25s %-15s %-15s %-10s %-10s %-10s %-15s";
                String header = String.format(formato, "Fecha", "Producto", "Codigo", "Movimiento", "Cantidad", "Precio/U", "Total", "Proveedor");
                bw.write(header);
                bw.newLine();
                bw.write("-------------------------------------------------------------------------------------------------------------------------");
                bw.newLine();
                for (Movimiento m : lista) {
                    String prov = codigoAProveedor.getOrDefault(m.getCodigoProducto(), "N/A");
                    if (prov.isEmpty()) prov = "N/A";
                    String linea = String.format(formato, m.getFecha(), m.getNombreProducto(), m.getCodigoProducto(), m.getTipo(), String.valueOf(m.getCantidad()), String.valueOf(m.getPrecioUnitario()), String.valueOf(m.getTotal()), prov);
                    bw.write(linea);
                    bw.newLine();
                }
            }
            JOptionPane.showMessageDialog(vista, "Reporte guardado: " + out.getAbsolutePath() + "\nFilas: " + lista.size());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(vista, "Error al guardar reporte: " + ex.getMessage());
        }
    }
}
