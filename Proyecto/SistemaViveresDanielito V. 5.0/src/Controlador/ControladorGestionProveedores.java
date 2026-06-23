package Controlador;

import Modelo.Conexion;
import Modelo.ProductoDAO;
import Modelo.ProveedorDAO;
import Vista.FrmGestionProveedores;
import com.mongodb.DBCollection;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 * controla la pantalla de gestion de proveedores: listar, agregar,
 * renombrar y eliminar. antes de renombrar o eliminar avisa si ese
 * proveedor ya tiene productos asociados para que el usuario decida
 * si quiere continuar o no.
 */
public class ControladorGestionProveedores {

    private final FrmGestionProveedores vista;
    private final ProveedorDAO dao;
    private final ProductoDAO prodDao;

    public ControladorGestionProveedores(FrmGestionProveedores vista) {
        this.vista = vista;
        this.dao = new ProveedorDAO();
        this.prodDao = new ProductoDAO();
        cargarTabla();
        configurarListeners();
    }

    private void configurarListeners() {
        vista.btnAgregar.addActionListener(e -> agregarProveedor());
        vista.btnRenombrar.addActionListener(e -> renombrarProveedor());
        vista.btnEliminar.addActionListener(e -> eliminarProveedor());
        vista.btnTelefono.addActionListener(e -> editarTelefono());
    }

    public void cargarTabla() {
        DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{"Proveedor", "Teléfono", "Productos asociados"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        Conexion cx = new Conexion();
        DBCollection coleccionProd = cx.getColeccionProd();
        for (String nombre : dao.obtenerNombresProveedores()) {
            int productos = dao.contarProductosConProveedor(nombre, coleccionProd);
            String telefono = dao.obtenerTelefono(nombre);
            modelo.addRow(new Object[]{nombre, telefono.isEmpty() ? "-" : telefono, productos});
        }
        vista.tabla.setModel(modelo);
    }

    private void agregarProveedor() {
        String nombre = JOptionPane.showInputDialog(vista,
                "Nombre del nuevo proveedor:", "Agregar proveedor", JOptionPane.PLAIN_MESSAGE);
        if (nombre == null || nombre.trim().isEmpty()) return;
        if (dao.existeProveedor(nombre.trim())) {
            JOptionPane.showMessageDialog(vista,
                    "Ya existe un proveedor con ese nombre.", "Duplicado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // el telefono es opcional: si se deja en blanco o se cancela, el
        // proveedor se guarda igual, solo que sin telefono
        String telefono = JOptionPane.showInputDialog(vista,
                "Teléfono del proveedor (opcional):", "Agregar proveedor", JOptionPane.PLAIN_MESSAGE);
        dao.guardarSiNoExiste(nombre.trim(), telefono);
        JOptionPane.showMessageDialog(vista, "Proveedor \"" + nombre.trim() + "\" agregado.");
        cargarTabla();
    }

    /**
     * deja agregar o cambiar el telefono de un proveedor que ya esta en
     * la tabla. dejar el campo vacio quita el telefono guardado.
     */
    private void editarTelefono() {
        int fila = vista.tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(vista, "Seleccione un proveedor de la tabla para editar su teléfono.");
            return;
        }
        String nombre = vista.tabla.getValueAt(fila, 0).toString();
        String telefonoActual = dao.obtenerTelefono(nombre);

        String nuevoTelefono = JOptionPane.showInputDialog(vista,
                "Teléfono para \"" + nombre + "\" (deje vacío para quitarlo):",
                telefonoActual);
        if (nuevoTelefono == null) return; // cancelado

        boolean ok = dao.actualizarTelefono(nombre, nuevoTelefono);
        if (ok) {
            JOptionPane.showMessageDialog(vista, "Teléfono actualizado.");
        } else {
            JOptionPane.showMessageDialog(vista, "No se pudo actualizar el teléfono.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        cargarTabla();
    }

    private void renombrarProveedor() {
        int fila = vista.tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(vista, "Seleccione un proveedor de la tabla para renombrar.");
            return;
        }
        String nombreViejo = vista.tabla.getValueAt(fila, 0).toString();
        int productos = (int) vista.tabla.getValueAt(fila, 2);

        String nuevo = JOptionPane.showInputDialog(vista,
                "Nuevo nombre para \"" + nombreViejo + "\":", nombreViejo);
        if (nuevo == null || nuevo.trim().isEmpty()) return;
        if (nuevo.trim().equalsIgnoreCase(nombreViejo)) return;

        if (dao.existeProveedor(nuevo.trim())) {
            JOptionPane.showMessageDialog(vista,
                    "Ya existe un proveedor con ese nombre.", "Duplicado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // si tiene productos asociados, advertir antes de continuar
        if (productos > 0) {
            int confirm = JOptionPane.showConfirmDialog(vista,
                    "Este proveedor tiene " + productos + " producto(s) asociado(s).\n"
                    + "Se actualizara el nombre en todos ellos.\n\n¿Desea continuar?",
                    "Productos asociados", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.OK_OPTION) return;
        }

        Conexion cx = new Conexion();
        DBCollection coleccionProd = cx.getColeccionProd();
        boolean ok = dao.modificarNombre(nombreViejo, nuevo.trim(), coleccionProd);
        if (ok) {
            JOptionPane.showMessageDialog(vista, "Proveedor renombrado correctamente.");
        } else {
            JOptionPane.showMessageDialog(vista, "No se pudo renombrar el proveedor.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        cargarTabla();
    }

    private void eliminarProveedor() {
        int fila = vista.tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(vista, "Seleccione un proveedor de la tabla para eliminar.");
            return;
        }
        String nombre = vista.tabla.getValueAt(fila, 0).toString();
        int productos = (int) vista.tabla.getValueAt(fila, 2);

        // si tiene productos asociados, advertir claramente
        String mensaje;
        if (productos > 0) {
            mensaje = "El proveedor \"" + nombre + "\" tiene " + productos + " producto(s) asociado(s).\n"
                    + "Al eliminarlo, esos productos quedarán sin proveedor asignado.\n\n"
                    + "¿Desea continuar de todas formas?";
        } else {
            mensaje = "¿Está seguro que desea eliminar el proveedor \"" + nombre + "\"?";
        }

        int confirm = JOptionPane.showConfirmDialog(vista, mensaje,
                "Confirmar eliminación", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) return;

        boolean ok = dao.eliminar(nombre);
        if (ok) {
            JOptionPane.showMessageDialog(vista, "Proveedor eliminado correctamente.");
        } else {
            JOptionPane.showMessageDialog(vista, "No se pudo eliminar el proveedor.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        cargarTabla();
    }
}
