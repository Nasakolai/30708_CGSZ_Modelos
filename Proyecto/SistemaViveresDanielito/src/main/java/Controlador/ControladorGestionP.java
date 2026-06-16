
package Controlador;

import Modelo.Producto;
import Modelo.ProductoDAO;
import Vista.FrmEditaPrduct;
import Vista.FrmGestionProductos;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class ControladorGestionP implements ActionListener {
    
    private FrmGestionProductos vista;
    private ProductoDAO dao;

    public ControladorGestionP(FrmGestionProductos vista, ProductoDAO dao) {
        this.vista = vista;
        this.dao = dao;
        this.vista.setVisible(true);
        this.vista.setLocationRelativeTo(null);
        this.vista.tablaProductos.setVisible(true);
        cargarTabla();

        this.vista.btnBuscar.addActionListener(this);
        this.vista.btnModificar.addActionListener(this);
        this.vista.btnEliminar.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == vista.btnBuscar) {
            buscarPorNombre();
        } else if (e.getSource() == vista.btnModificar) {
            modificar();
        } else if (e.getSource() == vista.btnEliminar) {
            eliminar();
        }
    }

    private void cargarTabla() {
        ArrayList<Producto> lista = dao.listarProductos();
        DefaultTableModel modelo = new DefaultTableModel();
        modelo.addColumn("Nombre");
        modelo.addColumn("Tipo");
        modelo.addColumn("Precio U");
        modelo.addColumn("Proveedor");
        modelo.addColumn("Stock");

        for (Producto p : lista) {
            modelo.addRow(new Object[]{
                p.getNombre(),
                p.getTipo(),
                p.getPrecioUnit(),
                p.getProveedor(),
                p.getStock()
            });
        }

        vista.tablaProductos.setModel(modelo);
    }

    private void buscarPorNombre() {
        try {
            String prefijo = vista.txtBuscar.getText().trim();
            if (prefijo.isBlank()) {
                cargarTabla();
                return;
            }

            ArrayList<Producto> lista = dao.buscarEspecie(prefijo);
            if (lista.isEmpty()) {
                JOptionPane.showMessageDialog(vista, "No se encontraron productos con ese nombre.");
                return;
            }

            DefaultTableModel modelo = new DefaultTableModel();
            modelo.addColumn("Nombre");
            modelo.addColumn("Tipo");
            modelo.addColumn("Precio U");
            modelo.addColumn("Proveedor");
            modelo.addColumn("Stock");

            for (Producto p : lista) {
                modelo.addRow(new Object[]{
                    p.getNombre(),
                    p.getTipo(),
                    p.getPrecioUnit(),
                    p.getProveedor(),
                    p.getStock()
                });
            }

            vista.tablaProductos.setModel(modelo);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(vista, "Error al buscar: " + ex.getMessage());
        }
    }

    private void modificar() {
        int fila = vista.tablaProductos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(vista, "Seleccione un producto de la tabla para modificar.");
            return;
        }

        String nombre = vista.tablaProductos.getValueAt(fila, 0).toString();
        Producto producto = dao.buscarPorNombre(nombre);
        if (producto == null) {
            JOptionPane.showMessageDialog(vista, "No se encontró el producto seleccionado.");
            return;
        }

        FrmEditaPrduct editar = new FrmEditaPrduct(producto, dao, this::cargarTabla);
        editar.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        editar.setVisible(true);
    }

    private void eliminar() {
        int fila = vista.tablaProductos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(vista, "Seleccione un producto de la tabla para eliminar.");
            return;
        }

        String nombre = vista.tablaProductos.getValueAt(fila, 0).toString();
        int confirm = JOptionPane.showConfirmDialog(vista,
                "¿Está seguro que desea eliminar el producto '" + nombre + "'?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            dao.eliminarProducto(nombre);
            JOptionPane.showMessageDialog(vista, "Producto eliminado correctamente.");
            cargarTabla();
        }
    }
}
