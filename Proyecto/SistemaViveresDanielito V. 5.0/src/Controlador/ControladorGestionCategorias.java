package Controlador;

import Modelo.CategoriaDAO;
import Modelo.Conexion;
import Vista.FrmGestionCategorias;
import com.mongodb.DBCollection;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 * controla la pantalla de gestion de categorias: listar, agregar,
 * renombrar y eliminar. si una categoria tiene productos asociados
 * muestra una alerta clara antes de permitir el cambio.
 */
public class ControladorGestionCategorias {

    private final FrmGestionCategorias vista;
    private final CategoriaDAO dao;

    public ControladorGestionCategorias(FrmGestionCategorias vista) {
        this.vista = vista;
        this.dao = new CategoriaDAO();
        dao.inicializarSiVacia();
        cargarTabla();
        configurarListeners();
    }

    private void configurarListeners() {
        vista.btnAgregar.addActionListener(e -> agregarCategoria());
        vista.btnRenombrar.addActionListener(e -> renombrarCategoria());
        vista.btnEliminar.addActionListener(e -> eliminarCategoria());
    }

    public void cargarTabla() {
        DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{"Categoría", "Productos asociados"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        Conexion cx = new Conexion();
        DBCollection coleccionProd = cx.getColeccionProd();
        for (String nombre : dao.listar()) {
            int prods = dao.contarProductosConCategoria(nombre, coleccionProd);
            modelo.addRow(new Object[]{nombre, prods});
        }
        vista.tabla.setModel(modelo);
    }

    private void agregarCategoria() {
        String nombre = JOptionPane.showInputDialog(vista,
                "Nombre de la nueva categoría:", "Agregar categoría", JOptionPane.PLAIN_MESSAGE);
        if (nombre == null || nombre.trim().isEmpty()) return;
        boolean ok = dao.agregar(nombre.trim());
        if (ok) {
            JOptionPane.showMessageDialog(vista, "Categoría \"" + nombre.trim() + "\" agregada.");
        } else {
            JOptionPane.showMessageDialog(vista,
                    "Ya existe una categoría con ese nombre.", "Duplicada", JOptionPane.WARNING_MESSAGE);
        }
        cargarTabla();
    }

    private void renombrarCategoria() {
        int fila = vista.tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(vista, "Seleccione una categoría de la tabla para renombrar.");
            return;
        }
        String nombreViejo = vista.tabla.getValueAt(fila, 0).toString();
        int productos = (int) vista.tabla.getValueAt(fila, 1);

        String nuevo = JOptionPane.showInputDialog(vista,
                "Nuevo nombre para \"" + nombreViejo + "\":", nombreViejo);
        if (nuevo == null || nuevo.trim().isEmpty()) return;
        if (nuevo.trim().equalsIgnoreCase(nombreViejo)) return;

        if (dao.existe(nuevo.trim())) {
            JOptionPane.showMessageDialog(vista,
                    "Ya existe una categoría con ese nombre.", "Duplicada", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (productos > 0) {
            int confirm = JOptionPane.showConfirmDialog(vista,
                    "Esta categoría tiene " + productos + " producto(s) asociado(s).\n"
                    + "Se actualizará el tipo en todos ellos.\n\n¿Desea continuar?",
                    "Productos asociados", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.OK_OPTION) return;
        }

        Conexion cx = new Conexion();
        boolean ok = dao.modificarNombre(nombreViejo, nuevo.trim(), cx.getColeccionProd());
        JOptionPane.showMessageDialog(vista, ok
                ? "Categoría renombrada correctamente."
                : "No se pudo renombrar la categoría.");
        cargarTabla();
    }

    private void eliminarCategoria() {
        int fila = vista.tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(vista, "Seleccione una categoría de la tabla para eliminar.");
            return;
        }
        String nombre = vista.tabla.getValueAt(fila, 0).toString();
        int productos = (int) vista.tabla.getValueAt(fila, 1);

        String mensaje = productos > 0
                ? "La categoría \"" + nombre + "\" tiene " + productos + " producto(s) asociado(s).\n"
                  + "Esos productos quedarán sin categoría asignada.\n\n¿Desea continuar de todas formas?"
                : "¿Está seguro que desea eliminar la categoría \"" + nombre + "\"?";

        int confirm = JOptionPane.showConfirmDialog(vista, mensaje,
                "Confirmar eliminación", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) return;

        boolean ok = dao.eliminar(nombre);
        JOptionPane.showMessageDialog(vista, ok
                ? "Categoría eliminada correctamente."
                : "No se pudo eliminar la categoría.");
        cargarTabla();
    }
}
