
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
        String codigo = objVista.jTextFieldCodigoProducto.getText().trim();
        int stock = 0;

        if (nombre.isBlank() || precioUnit.isBlank() || codigo.isBlank()) {
            JOptionPane.showMessageDialog(objVista, "Rellene todos los campos obligatorios (nombre, precio, código). ");
            return;
        }

        try {
            double precioKg = Double.parseDouble(precioUnit);
            Producto producto = new Producto(nombre, tipo, precioKg, proveedor, stock, codigo);
            objDAO.añadirProducto(producto);
            JOptionPane.showMessageDialog(objVista, "Se añadió " + producto.getNombre());

            int opcion = JOptionPane.showOptionDialog(
                    objVista,
                    "¿Desea seguir registrando o regresar al sistema principal?",
                    "Continuar registro",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Seguir registrando", "Volver al sistema"},
                    "Seguir registrando"
            );

            if (opcion == JOptionPane.YES_OPTION) {
                limpiarFormulario();
            } else {
                objVista.dispose();
            }

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
