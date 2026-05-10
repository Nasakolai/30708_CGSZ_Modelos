
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
            
            objDAO.añadirProducto(producto);
            JOptionPane.showMessageDialog(objVista, "Se añadió "+producto.getNombre());
            

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(objVista, "Formato inválido para precio por kg.");
        }
   
    }



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
