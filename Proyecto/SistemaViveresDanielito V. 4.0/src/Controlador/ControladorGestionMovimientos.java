package Controlador;

import Modelo.Movimiento;
import Modelo.MovimientoDAO;
import Vista.FrmGestionMovimientos;
import java.util.ArrayList;
import javax.swing.JOptionPane;
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

    private final FrmGestionMovimientos vista;
    private final MovimientoDAO dao;

    public ControladorGestionMovimientos(FrmGestionMovimientos vista, MovimientoDAO dao) {
        this.vista = vista;
        this.dao = dao;
        cargarTabla();
        configurarListeners();
    }

    private void configurarListeners() {
        vista.btnEliminar.addActionListener(e -> eliminarSeleccionado());
    }

    private void cargarTabla() {
        ArrayList<Movimiento> lista = dao.listarMovimientos();

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

        boolean eliminado = dao.eliminarMovimiento(id);
        if (eliminado) {
            JOptionPane.showMessageDialog(vista, "Movimiento eliminado correctamente.");
        } else {
            // esto pasaria, por ejemplo, si alguien mas (u otra ventana) ya
            // habia borrado ese mismo movimiento un instante antes
            JOptionPane.showMessageDialog(vista, "No se pudo eliminar el movimiento. Puede que ya no exista.");
        }
        cargarTabla();
    }
}
