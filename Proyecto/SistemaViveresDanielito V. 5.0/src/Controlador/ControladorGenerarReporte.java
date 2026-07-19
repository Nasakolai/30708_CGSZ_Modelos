package Controlador;

import Modelo.ConfiguracionDAO;
import Modelo.Movimiento;
import Modelo.MovimientoDAO;
import Modelo.MotivosMovimiento;
import Modelo.Producto;
import Modelo.ProductoDAO;
import Util.GeneradorPDF;
import Vista.FrmGenerarReporte;
import com.mongodb.BasicDBObject;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * controlador de la pantalla de reportes. antes generaba archivos .txt,
 * ahora genera PDFs con el logo y los colores de Viveres Danielito.
 * el cambio en el archivo de salida es lo unico que cambia visualmente
 * para el usuario: todo lo demas (filtros, botones, interfaz) queda igual.
 */
public class ControladorGenerarReporte {

    private static final Logger logger = Logger.getLogger(ControladorGenerarReporte.class.getName());

    // nombre de la empresa, siempre fijo. el "responsable" (a nombre de
    // quien sale el reporte) ya NO es fijo: se lee de ConfiguracionDAO,
    // se puede cambiar desde esta misma pantalla y queda guardado para
    // la proxima vez (ver jTextFieldResponsable).
    private static final String NOMBRE_EMPRESA = "VIVERES DANIELITO";

    // "Todos los motivos" siempre va primero en el filtro
    private static final String TODOS_LOS_MOTIVOS = "Todos los motivos";

    // ruta del logo dentro del classpath (va en el encabezado del PDF)
    private static final String RUTA_LOGO = "/imagenes/logo_reporte.jpg";

    private final FrmGenerarReporte vista;
    private final MovimientoDAO movDao;
    private final ProductoDAO prodDao;
    private final ConfiguracionDAO configDao;

    public ControladorGenerarReporte(FrmGenerarReporte vista, MovimientoDAO movDao, ProductoDAO prodDao) {
        this.vista = vista;
        this.movDao = movDao;
        this.prodDao = prodDao;
        this.configDao = new ConfiguracionDAO();
        cargarDatosIniciales();
        cargarResponsable();
        cargarMotivos();
        configurarListeners();
    }

    private void cargarDatosIniciales() {
        vista.getProductoCombo().removeAllItems();
        vista.getProductoCombo().addItem("Todos los productos");
        for (Producto p : prodDao.listarProductos()) {
            String etiqueta = p.getNombre() + " (" + p.getCodigo() + ")";
            vista.getProductoCombo().addItem(etiqueta);
        }

        vista.getCategoriaCombo().removeAllItems();
        vista.getCategoriaCombo().addItem("Todos los proveedores");
        for (String prov : new Modelo.ProveedorDAO().obtenerNombresProveedores()) {
            vista.getCategoriaCombo().addItem(prov);
        }
    }

    /**
     * precarga el campo de "a nombre de quien" con el ultimo nombre
     * guardado (o "Enrique Guaiguacundo" la primerísima vez).
     */
    private void cargarResponsable() {
        vista.jTextFieldResponsable.setText(configDao.obtenerResponsable());
    }

    /**
     * llena el combo de motivo (reutilizando el antiguo combo "Estado",
     * que no se usaba) con todos los motivos posibles de entrada y de
     * salida juntos, sin repetidos, para poder filtrar el reporte de
     * movimientos por cualquiera de ellos.
     */
    private void cargarMotivos() {
        LinkedHashSet<String> motivos = new LinkedHashSet<>();
        motivos.add(TODOS_LOS_MOTIVOS);
        for (String m : MotivosMovimiento.MOTIVOS_ENTRADA) motivos.add(m);
        for (String m : MotivosMovimiento.MOTIVOS_SALIDA) motivos.add(m);
        vista.getEstadoCombo().removeAllItems();
        for (String m : motivos) {
            vista.getEstadoCombo().addItem(m);
        }
        vista.getEstadoCombo().setSelectedIndex(0);
        actualizarComentarioHabilitado();
    }

    private void actualizarComentarioHabilitado() {
        boolean esOtro = MotivosMovimiento.OTRO.equals(vista.getEstadoCombo().getSelectedItem());
        vista.jTextFieldComentarioMotivo.setEnabled(esOtro);
        if (!esOtro) {
            vista.jTextFieldComentarioMotivo.setText("");
        }
    }

    private void configurarListeners() {
        vista.getButtonGenerarEspecifico().addActionListener((ActionEvent e) -> generarReporteStock());
        vista.getButtonGenerarGeneral().addActionListener((ActionEvent e) -> generarReporteMovimientos());
        vista.getEstadoCombo().addActionListener((ActionEvent e) -> actualizarComentarioHabilitado());
        // el nombre se guarda apenas el usuario sale del campo (asi no
        // hace falta un boton aparte de "guardar nombre"), y tambien se
        // vuelve a guardar justo antes de generar cualquier reporte por
        // si el usuario lo cambio y le dio clic directo a generar.
        vista.jTextFieldResponsable.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                guardarResponsableSiCambio();
            }
        });
    }

    private void guardarResponsableSiCambio() {
        String nombre = vista.jTextFieldResponsable.getText();
        if (nombre != null && !nombre.trim().isEmpty()) {
            configDao.guardarResponsable(nombre.trim());
        } else {
            // si lo dejan vacio, volvemos a mostrar el ultimo guardado
            // en vez de dejar el campo en blanco
            vista.jTextFieldResponsable.setText(configDao.obtenerResponsable());
        }
    }

    private String obtenerResponsableActual() {
        guardarResponsableSiCambio();
        return configDao.obtenerResponsable();
    }

    // -----------------------------------------------------------------
    // reporte de movimientos (antes: "general")
    // -----------------------------------------------------------------

    private void generarReporteMovimientos() {
        BasicDBObject filtro = new BasicDBObject();
        String motivoSel = (String) vista.getEstadoCombo().getSelectedItem();
        StringBuilder descripcion = new StringBuilder("General - todos los movimientos");

        if (motivoSel != null && !TODOS_LOS_MOTIVOS.equals(motivoSel)) {
            filtro.put("motivo", motivoSel);
            descripcion = new StringBuilder("Motivo: ").append(motivoSel);
            if (MotivosMovimiento.OTRO.equals(motivoSel)) {
                String comentario = vista.jTextFieldComentarioMotivo.getText();
                if (comentario != null && !comentario.trim().isEmpty()) {
                    // busqueda de texto libre (sin distinguir mayusculas) dentro
                    // del detalle que el usuario escribio al registrar el movimiento
                    filtro.put("detalleMotivo", new BasicDBObject("$regex",
                            java.util.regex.Pattern.quote(comentario.trim())).append("$options", "i"));
                    descripcion.append(" (\"").append(comentario.trim()).append("\")");
                }
            }
        }

        ArrayList<Movimiento> lista = movDao.listarMovimientos(filtro);
        generarPdfMovimientos(lista, descripcion.toString());
    }

    // -----------------------------------------------------------------
    // reporte de inventario actual / stock (antes: "especifico")
    // -----------------------------------------------------------------

    private void generarReporteStock() {
        String productoSel = (String) vista.getProductoCombo().getSelectedItem();
        String proveedorSel = (String) vista.getCategoriaCombo().getSelectedItem();

        ArrayList<Producto> productos = prodDao.listarProductos();
        ArrayList<Producto> filtrados = new ArrayList<>();

        for (Producto p : productos) {
            boolean ok = true;
            if (productoSel != null && !productoSel.equals("Todos los productos")) {
                String etiqueta = p.getNombre() + " (" + p.getCodigo() + ")";
                if (!etiqueta.equals(productoSel)) ok = false;
            }
            if (ok && proveedorSel != null && !proveedorSel.equals("Todos los proveedores")) {
                String prov = p.getProveedor() == null ? "" : p.getProveedor();
                if (!proveedorSel.equals(prov)) ok = false;
            }
            if (ok) filtrados.add(p);
        }

        StringBuilder descripcion = new StringBuilder("Inventario actual");
        descripcion.append(" | Producto: ").append(productoSel == null ? "Todos" : productoSel);
        descripcion.append(" | Proveedor: ").append(proveedorSel == null ? "Todos" : proveedorSel);

        generarPdfStock(filtrados, descripcion.toString());
    }

    // -----------------------------------------------------------------
    // generacion PDF de movimientos
    // -----------------------------------------------------------------

    private void generarPdfMovimientos(ArrayList<Movimiento> movimientos, String descripcionTipo) {
        // construimos el mapa codigo -> proveedor para agregar esa columna
        java.util.Map<String, String> codigoAProv = new java.util.HashMap<>();
        for (Producto p : prodDao.listarProductos()) {
            if (p.getCodigo() != null) {
                codigoAProv.put(p.getCodigo(), p.getProveedor() == null ? "N/A" : p.getProveedor());
            }
        }

        // convertir la lista al formato que espera GeneradorPDF
        // columnas: fecha, tipo, producto, codigo, cantidad, precioU, total, proveedor
        List<String[]> filas = new ArrayList<>();
        for (Movimiento m : movimientos) {
            String prov = codigoAProv.getOrDefault(m.getCodigoProducto(), "N/A");
            filas.add(new String[]{
                m.getFecha(),
                m.getTipo(),
                m.getNombreProducto(),
                m.getCodigoProducto(),
                String.valueOf(m.getCantidad()),
                String.format(Locale.US, "$%.2f", m.getPrecioUnitario()),
                String.format(Locale.US, "$%.2f", m.getTotal()),
                prov
            });
        }

        try {
            File dir = crearCarpetaReportes();
            String marca = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File archivo = new File(dir, "reporte_movimientos_" + marca + ".pdf");

            GeneradorPDF.generarReporteMovimientos(
                    filas, descripcionTipo, obtenerResponsableActual(),
                    NOMBRE_EMPRESA, RUTA_LOGO, archivo);

            avisarExito(archivo, movimientos.size(), "movimiento(s)");

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "error generando PDF de movimientos", ex);
            JOptionPane.showMessageDialog(vista, "Error al generar el PDF:\n" + ex.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // generacion PDF de stock
    // -----------------------------------------------------------------

    private void generarPdfStock(ArrayList<Producto> productos, String descripcionTipo) {
        // columnas: codigo, producto, tipo, proveedor, stock, precio/u, valor total
        List<String[]> filas = new ArrayList<>();
        int totalUnidades = 0;
        double valorTotal = 0;

        for (Producto p : productos) {
            double valor = p.getPrecioUnit() * p.getStock();
            totalUnidades += p.getStock();
            valorTotal += valor;
            String prov = (p.getProveedor() == null || p.getProveedor().isEmpty()) ? "N/A" : p.getProveedor();
            filas.add(new String[]{
                p.getCodigo(),
                p.getNombre(),
                p.getTipo(),
                prov,
                String.valueOf(p.getStock()),
                String.format(Locale.US, "$%.2f", p.getPrecioUnit()),
                String.format(Locale.US, "$%.2f", valor),
                ""  // columna proveedor repetida, se ignora
            });
        }

        try {
            File dir = crearCarpetaReportes();
            String marca = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File archivo = new File(dir, "reporte_stock_" + marca + ".pdf");

            GeneradorPDF.generarReporteStock(
                    filas, descripcionTipo, obtenerResponsableActual(),
                    NOMBRE_EMPRESA, RUTA_LOGO, totalUnidades, valorTotal, archivo);

            avisarExito(archivo, productos.size(), "producto(s)");

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "error generando PDF de stock", ex);
            JOptionPane.showMessageDialog(vista, "Error al generar el PDF:\n" + ex.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // utilidades
    // -----------------------------------------------------------------

    private File crearCarpetaReportes() {
        File dir = new File("reports");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void avisarExito(File archivo, int cantidad, String unidad) {
        JOptionPane.showMessageDialog(vista,
                "Reporte PDF generado correctamente.\n\n"
                + "Archivo: " + archivo.getAbsolutePath() + "\n"
                + "Registros incluidos: " + cantidad + " " + unidad + "\n\n"
                + "Puede abrir el archivo con cualquier lector de PDF\n"
                + "(Adobe Acrobat, el navegador, etc.)");
    }

    /**
     * intenta convertir un texto a fecha con el formato dado. si viene
     * vacio o mal formateado devuelve null (los filtros de fecha son
     * opcionales, si no se pueden parsear simplemente no se aplican).
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
}
