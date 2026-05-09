
package Controlador;

import Modelo.Producto;
import Modelo.ProductoDAO;
import Vista.FrmProducto;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;





public class ControladorProducto implements ActionListener{

   FrmProducto objVista;
    ProductoDAO objDAO;

    public ControladorProducto(FrmProducto vista, ProductoDAO dao) {
        this.objVista = vista;
        this.objDAO = dao;

        // Conectar botones con acciones
        objVista.btnRegistrar.addActionListener(this);
        objVista.CmbProveedor.addActionListener(this);
        objVista.CmbTipo.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == objVista.btnRegistrar) {
            registrarProducto();
        }
    }

    public void registrarProducto() {
        String nombre = objVista.txtNombre.getText().trim();
        String tipo = objVista.CmbTipo.getSelectedItem().toString();
        String precioUnit = objVista.txtPrecio.getText().trim().replace(',', '.');
        String proveedor = objVista.CmbProveedor.getSelectedItem().toString();

        if (nombre.isBlank() || precioUnit.isBlank()) {
            JOptionPane.showMessageDialog(objVista, "Rellene todos los campos.");
            return;
        }

        try {
            double precioKg = Double.parseDouble(precioUnit);
            Producto producto = new Producto(nombre, tipo, precioKg, proveedor);
            //esto se añadio para que se mande al hacer click en el boton
            objDAO.añadirProducto(producto);
            JOptionPane.showMessageDialog(objVista, "Se añadió "+producto.getNombre());
            

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(objVista, "Formato inválido para precio por kg.");
        }
   
    }

//    private void eliminarEspecie() {
//        int fila = objVista.tablaProductos.getSelectedRow();
//        if (fila != -1) {
//            String nombre = objVista.tablaProductos.getValueAt(fila, 0).toString();
//            objDAO.eliminarProducto(nombre);
//            JOptionPane.showMessageDialog(objVista, "Especie eliminada.");
//            llenarTabla(objVista.tablaProductos);
//        } else {
//            JOptionPane.showMessageDialog(objVista, "Seleccione un registro para eliminar.");
//        }
//    }

//    private void modificarEspecie() {
//        int fila = objVista.tablaProductos.getSelectedRow();
//        if (fila != -1) {
//            String nombreViejo = objVista.tablaProductos.getValueAt(fila, 0).toString();
//            String nombreNuevo = objVista.txtNombre.getText().trim();
//            String tipo = objVista.CmbTipo.getSelectedItem().toString();
//            String precioUnit = objVista.txtPrecio.getText().trim().replace(',', '.');
//            String proveedor = objVista.CmbProveedor.getSelectedItem().toString();
//            try {
//                double precioUnitario = Double.parseDouble(precioUnit);
//                Producto nuevaEspecie = new Producto(nombreNuevo, tipo, precioUnitario, proveedor);
//                objDAO.modificarProducto(nombreViejo, nuevaEspecie);
//                JOptionPane.showMessageDialog(objVista, "Especie modificada.");
//                llenarTabla(objVista.tablaProductos);
//            } catch (NumberFormatException ex) {
//                JOptionPane.showMessageDialog(objVista, "Formato inválido para precio unitario.");
//            }
//        } else {
//            JOptionPane.showMessageDialog(objVista, "Seleccione un registro para modificar.");
//        }
//    }
//
//    private void migrarProductos() {
//        int contador = 0;
//        for (Producto e : objDAO.migrarLista()) {
//            if (!objDAO.productoExiste(e.getNombre())) {
//                objDAO.añadirProducto(e);
//                contador++;
//            } else {
//                JOptionPane.showMessageDialog(objVista, "El Producto " + e.getNombre() + " ya está registrada.");
//            }
//        }
//        JOptionPane.showMessageDialog(objVista, "Se migraron " + contador + " productos.");
//        llenarTabla(objVista.tablaProductos);
//    }
//
//    private void buscarEspeciePorNombre() {
//        String nombreBuscar = objVista.CmbBuscarProductos.getSelectedItem().toString();
//        ArrayList<Producto> lista = objDAO.buscarEspecie(nombreBuscar);
//        llenarTablaConLista(objVista.tablaProductos, lista);
//    }

    public void llenarTabla(JTable tabla) {
        ArrayList<Producto> lista = objDAO.listarProductos();
        llenarTablaConLista(tabla, lista);
    }

    public void llenarTablaConLista(JTable tabla, ArrayList<Producto> lista) {
        DefaultTableModel modelo = new DefaultTableModel();
        modelo.addColumn("Nombre");
        modelo.addColumn("Tipo");
        modelo.addColumn("Precio/U");
        modelo.addColumn("Proveedor");

        for (Producto e : lista) {
            Object[] fila = {
                e.getNombre(),
                e.getTipo(),
                e.getPrecioUnit(),
                e.getProveedor(),
            };
            modelo.addRow(fila);
        }
        tabla.setModel(modelo);
    }
}
