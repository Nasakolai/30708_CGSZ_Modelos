
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
        objVista.txtProveedor.addActionListener(this);
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
        String proveedor = objVista.txtProveedor.getText().trim();
       String codigo = objDAO.generarCodigoProducto(tipo);
        int stock = 0;

        if (nombre.isBlank() || precioUnit.isBlank()) {
            JOptionPane.showMessageDialog(objVista, "Rellene todos los campos obligatorios.");
            return;
        }
        
        if (objDAO.existeProducto(nombre)) {

    JOptionPane.showMessageDialog(
            objVista,
            "Ya existe un producto con ese nombre.",
            "Producto duplicado",
            JOptionPane.WARNING_MESSAGE
    );

    return;
}
        try {
            double precioKg = Double.parseDouble(precioUnit);
            Producto producto = new Producto(nombre, tipo, precioKg, proveedor, stock, codigo);
            objDAO.añadirProducto(producto);
            JOptionPane.showMessageDialog(objVista, "Se añadió " + producto.getNombre());
            JOptionPane.showMessageDialog(
        objVista,
        "Producto registrado correctamente.\nCódigo: " + producto.getCodigo()
);

limpiarFormulario();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(objVista, "Formato inválido para precio.");
        }
    }

    private void limpiarFormulario() {
        objVista.txtNombre.setText("");
        objVista.CmbTipo.setSelectedIndex(0);
        objVista.txtPrecio.setText("");
        objVista.txtProveedor.setText("");
        objVista.jTextFieldCodigoProducto.setText("");
        objVista.txtNombre.requestFocus();
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
        modelo.addColumn("Stock");

        for (Producto e : lista) {
            Object[] fila = {
                e.getNombre(),
                e.getTipo(),
                e.getPrecioUnit(),
                e.getProveedor(),
                e.getStock()
            };
            modelo.addRow(fila);
        }
        tabla.setModel(modelo);
    }
}
