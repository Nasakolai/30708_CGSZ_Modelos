package Controlador;

import Modelo.Movimiento;
import Modelo.MovimientoDAO;
import Modelo.Producto;
import Modelo.ProductoDAO;
import Vista.FrmEditarMovimiento;
import Vista.FrmGestionMovimientos;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * controlador de la pantalla nueva "ver movimientos". se encarga de mostrar
 * la tabla con el historial de entradas/salidas y de borrar el movimiento
 * que el usuario seleccione.
 *
 * la columna "ID" de la tabla esta ahi a proposito pero queda oculta (ancho
 * 0): es el _id que mongo le pone a cada movimiento, y se necesita para
 * poder borrar exactamente el que el usuario selecciono sin confundirlo con
 * otro que tenga los mismos datos (fecha, producto, cantidad, etc pueden
 * repetirse entre dos movimientos distintos, el id nunca se repite).
 */
public class ControladorGestionMovimientos {

    private static final int COLUMNA_ID = 0;
    private static final int COLUMNA_TIPO = 2;
    private static final int COLUMNA_PRODUCTO = 3;
    private static final int COLUMNA_FECHA = 1;
    private static final int COLUMNA_CODIGO = 4;
    private static final int COLUMNA_CANTIDAD = 5;

    private final FrmGestionMovimientos vista;
    private final MovimientoDAO dao;
    private final ProductoDAO productoDao;

    public ControladorGestionMovimientos(FrmGestionMovimientos vista, MovimientoDAO dao) {
        this.vista = vista;
        this.dao = dao;
        this.productoDao = new ProductoDAO();
        cargarComboProductos();
        cargarTabla();
        configurarListeners();
    }

    private void cargarComboProductos() {
        vista.cmbFiltroProducto.removeAllItems();
        vista.cmbFiltroProducto.addItem("Todos los productos");
        for (Producto p : productoDao.listarProductos()) {
            vista.cmbFiltroProducto.addItem(p.getNombre() + " (" + p.getCodigo() + ")");
        }
    }

    private void configurarListeners() {
        vista.btnEliminar.addActionListener(e -> eliminarSeleccionado());
        vista.btnEditar.addActionListener(e -> editarSeleccionado());
        vista.btnFiltrar.addActionListener(e -> cargarTabla());
        vista.btnLimpiarFiltro.addActionListener(e -> limpiarFiltro());
    }

    private void limpiarFiltro() {
        vista.cmbFiltroProducto.setSelectedIndex(0);
        vista.txtFiltroFechaDesde.setText("");
        vista.txtFiltroFechaHasta.setText("");
        cargarTabla();
    }

    private void cargarTabla() {
        ArrayList<Movimiento> lista = dao.listarMovimientos();
        lista = aplicarFiltros(lista);

        DefaultTableModel modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                // la tabla es solo para ver y seleccionar, no para editar a mano
                return false;
            }
        };
        modelo.addColumn("ID");
        modelo.addColumn("Fecha");
        modelo.addColumn("Tipo");
        modelo.addColumn("Producto");
        modelo.addColumn("Código");
        modelo.addColumn("Cantidad");
        modelo.addColumn("Precio Unitario");
        modelo.addColumn("Total");

        for (Movimiento m : lista) {
            modelo.addRow(new Object[]{
                m.getId(),
                m.getFecha(),
                m.getTipo(),
                m.getNombreProducto(),
                m.getCodigoProducto(),
                m.getCantidad(),
                String.format("%.2f", m.getPrecioUnitario()),
                String.format("%.2f", m.getTotal())
            });
        }

        vista.getTablaMovimientos().setModel(modelo);
        ocultarColumnaId();

        if (lista.isEmpty()) {
            // no es un error, simplemente todavia no hay nada que mostrar.
            // se podria poner un mensaje aqui pero una tabla vacia ya
            // comunica eso sin necesidad de un popup molesto.
        }
    }

    /**
     * aplica los filtros que el usuario haya puesto en el panel de arriba
     * (producto y rango de fechas). si un filtro esta vacio o en "Todos
     * los productos" simplemente no se aplica esa condicion. el mismo
     * formato de fecha (dd/MM/yyyy) que usa el resto del sistema.
     */
    private ArrayList<Movimiento> aplicarFiltros(ArrayList<Movimiento> lista) {
        String productoSeleccionado = (String) vista.cmbFiltroProducto.getSelectedItem();
        String codigoFiltro = null;
        if (productoSeleccionado != null && !productoSeleccionado.equals("Todos los productos")) {
            int inicio = productoSeleccionado.lastIndexOf("(");
            int fin = productoSeleccionado.lastIndexOf(")");
            if (inicio != -1 && fin != -1) {
                codigoFiltro = productoSeleccionado.substring(inicio + 1, fin);
            }
        }

        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd/MM/yyyy");
        java.util.Date desde = parsearFechaSegura(fmt, vista.txtFiltroFechaDesde.getText());
        java.util.Date hasta = parsearFechaSegura(fmt, vista.txtFiltroFechaHasta.getText());

        if (codigoFiltro == null && desde == null && hasta == null) {
            return lista;
        }

        ArrayList<Movimiento> filtrada = new ArrayList<>();
        for (Movimiento m : lista) {
            if (codigoFiltro != null && !codigoFiltro.equals(m.getCodigoProducto())) {
                continue;
            }
            if (desde != null || hasta != null) {
                try {
                    java.util.Date fechaMov = fmt.parse(m.getFecha());
                    if (desde != null && fechaMov.before(desde)) continue;
                    if (hasta != null && fechaMov.after(hasta)) continue;
                } catch (java.text.ParseException ex) {
                    // si la fecha guardada no se puede interpretar, mejor
                    // no excluirla solo por eso cuando no hay filtro de
                    // fecha forzoso; pero como aqui si hay filtro de fecha
                    // activo, un movimiento con fecha invalida no se puede
                    // verificar, asi que se omite para no mostrar datos
                    // que no se sabe si cumplen el filtro
                    continue;
                }
            }
            filtrada.add(m);
        }
        return filtrada;
    }

    /**
     * intenta convertir un texto a fecha. si esta vacio, tiene los guiones
     * bajos de la mascara sin llenar, o no es una fecha valida, regresa
     * null en vez de tronar (las fechas de este filtro son opcionales).
     */
    private java.util.Date parsearFechaSegura(java.text.SimpleDateFormat formato, String texto) {
        if (texto == null || texto.trim().isEmpty() || texto.contains("_")) {
            return null;
        }
        try {
            return formato.parse(texto.trim());
        } catch (java.text.ParseException ex) {
            return null;
        }
    }

    /**
     * abre la ventana para corregir el movimiento seleccionado (por
     * ejemplo si se tecleo mal la cantidad). antes la unica opcion era
     * borrar y volver a crear el movimiento, lo cual ademas no corregia el
     * stock que ya se habia modificado con el movimiento original.
     */
    private void editarSeleccionado() {
        int fila = vista.getTablaMovimientos().getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(vista, "Seleccione un movimiento de la tabla para editar.");
            return;
        }

        Object idObj = vista.getTablaMovimientos().getValueAt(fila, COLUMNA_ID);
        if (idObj == null) {
            JOptionPane.showMessageDialog(vista, "No se pudo identificar el movimiento seleccionado.");
            return;
        }

        Movimiento movimiento = dao.buscarPorId(idObj.toString());
        if (movimiento == null) {
            JOptionPane.showMessageDialog(vista, "No se pudo cargar el movimiento. Puede que ya no exista.");
            cargarTabla();
            return;
        }

        java.awt.Window ventana = SwingUtilities.windowForComponent(vista);
        java.awt.Frame propietario = (ventana instanceof java.awt.Frame) ? (java.awt.Frame) ventana : null;

        FrmEditarMovimiento dialogo = new FrmEditarMovimiento(propietario, movimiento, dao, productoDao, this::cargarTabla);
        dialogo.setVisible(true);
    }

    /**
     * la columna 0 (el id de mongo) no le interesa al usuario, solo la
     * necesitamos internamente para saber que registro borrar. en vez de
     * quitarla del modelo (lo que complicaria los indices de las demas
     * columnas) simplemente la dejamos con ancho 0 para que no se vea ni
     * se pueda redimensionar.
     */
    private void ocultarColumnaId() {
        javax.swing.table.TableColumnModel columnas = vista.getTablaMovimientos().getColumnModel();
        if (columnas.getColumnCount() > COLUMNA_ID) {
            javax.swing.table.TableColumn columnaId = columnas.getColumn(COLUMNA_ID);
            columnaId.setMinWidth(0);
            columnaId.setMaxWidth(0);
            columnaId.setPreferredWidth(0);
            columnaId.setResizable(false);
        }
    }

    private void eliminarSeleccionado() {
        int fila = vista.getTablaMovimientos().getSelectedRow();

        if (fila == -1) {
            JOptionPane.showMessageDialog(vista, "Seleccione un movimiento de la tabla para eliminar.");
            return;
        }

        Object idObj = vista.getTablaMovimientos().getValueAt(fila, COLUMNA_ID);
        if (idObj == null) {
            JOptionPane.showMessageDialog(vista, "No se pudo identificar el movimiento seleccionado.");
            return;
        }
        String id = idObj.toString();

        String tipo = String.valueOf(vista.getTablaMovimientos().getValueAt(fila, COLUMNA_TIPO));
        String producto = String.valueOf(vista.getTablaMovimientos().getValueAt(fila, COLUMNA_PRODUCTO));
        String fecha = String.valueOf(vista.getTablaMovimientos().getValueAt(fila, COLUMNA_FECHA));
        String codigoProducto = String.valueOf(vista.getTablaMovimientos().getValueAt(fila, COLUMNA_CODIGO));
        int cantidad = 0;
        try {
            cantidad = Integer.parseInt(String.valueOf(vista.getTablaMovimientos().getValueAt(fila, COLUMNA_CANTIDAD)));
        } catch (NumberFormatException ex) {
            // si por algun motivo la cantidad no se puede leer, simplemente
            // no se va a poder ofrecer revertir el stock (se sigue pudiendo
            // borrar el movimiento normalmente)
        }

        int confirmar = JOptionPane.showConfirmDialog(
                vista,
                "¿Está seguro que desea eliminar este movimiento?\n\n"
                + "Tipo: " + tipo + "\n"
                + "Producto: " + producto + "\n"
                + "Fecha: " + fecha + "\n\n"
                + "Esta acción no se puede deshacer.",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }

        // al registrar el movimiento, el stock del producto ya se modifico
        // (una entrada lo sube, una salida lo baja). si solo se borra el
        // movimiento de la lista pero nadie toca el stock, el inventario
        // queda descuadrado: por eso aqui se le pregunta al usuario si
        // tambien quiere revertir ese efecto sobre el stock del producto.
        boolean revertirStock = false;
        if (cantidad > 0 && !codigoProducto.isBlank() && !codigoProducto.equals("null")) {
            int respuestaRevertir = JOptionPane.showConfirmDialog(
                    vista,
                    "¿Desea también revertir el efecto de este movimiento sobre el stock\n"
                    + "del producto \"" + producto + "\"?\n\n"
                    + (tipo.equalsIgnoreCase("Entrada")
                            ? "Esto le restará " + cantidad + " unidad(es) de su stock actual."
                            : "Esto le sumará " + cantidad + " unidad(es) a su stock actual."),
                    "Revertir stock",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            revertirStock = respuestaRevertir == JOptionPane.YES_OPTION;
        }

        boolean eliminado = dao.eliminarMovimiento(id);
        if (!eliminado) {
            // esto pasaria, por ejemplo, si alguien mas (u otra ventana) ya
            // habia borrado ese mismo movimiento un instante antes
            JOptionPane.showMessageDialog(vista, "No se pudo eliminar el movimiento. Puede que ya no exista.");
            cargarTabla();
            return;
        }

        String mensaje = "Movimiento eliminado correctamente.";
        if (revertirStock) {
            mensaje += "\n" + revertirEfectoEnStock(codigoProducto, tipo, cantidad, producto);
        }
        JOptionPane.showMessageDialog(vista, mensaje);
        cargarTabla();
    }

    /**
     * deshace en el stock del producto el efecto que tuvo el movimiento que
     * se acaba de borrar: si era una "Entrada" se le resta la cantidad, si
     * era una "Salida" se le suma. regresa un texto para avisarle al
     * usuario lo que paso (incluyendo si no se pudo hacer nada).
     */
    private String revertirEfectoEnStock(String codigoProducto, String tipo, int cantidad, String nombreProductoMovimiento) {
        Producto productoActual = productoDao.buscarPorCodigo(codigoProducto);
        if (productoActual == null) {
            return "No se pudo revertir el stock: el producto \"" + nombreProductoMovimiento
                    + "\" (código " + codigoProducto + ") ya no existe en el catálogo.";
        }

        int stockActual = productoActual.getStock();
        int stockNuevo;
        if (tipo.equalsIgnoreCase("Entrada")) {
            stockNuevo = stockActual - cantidad;
        } else {
            stockNuevo = stockActual + cantidad;
        }

        if (stockNuevo < 0) {
            // no dejamos que el stock quede en negativo por una reversion;
            // se avisa y se deja el stock en 0 en vez de un numero invalido
            stockNuevo = 0;
        }

        productoActual.setStock(stockNuevo);
        productoDao.modificarProducto(productoActual.getNombre(), productoActual);

        return "Stock de \"" + productoActual.getNombre() + "\" actualizado: " + stockActual + " → " + stockNuevo + ".";
    }
}
