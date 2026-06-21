
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

    // este es el texto que aparece en el combo cuando todavia no se ha elegido
    // un tipo de verdad, tiene que coincidir con el primer item del CmbTipo
    private static final String TIPO_SIN_SELECCIONAR = "Seleccione un tipo";

    public void registrarProducto() {
        String nombre = objVista.txtNombre.getText().trim();
        String tipo = objVista.CmbTipo.getSelectedItem() == null ? "" : objVista.CmbTipo.getSelectedItem().toString();
        String precioTexto = objVista.txtPrecio.getText().trim().replace(',', '.');
        String proveedor = objVista.txtProveedor.getText().trim();

        if (nombre.isBlank() || precioTexto.isBlank() || proveedor.isBlank()) {
            JOptionPane.showMessageDialog(objVista, "Rellene todos los campos obligatorios.");
            return;
        }

        if (tipo.isBlank() || tipo.equals(TIPO_SIN_SELECCIONAR)) {
            JOptionPane.showMessageDialog(objVista, "Seleccione un tipo de producto valido.");
            return;
        }

        double precioUnit;
        try {
            precioUnit = Double.parseDouble(precioTexto);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(objVista, "Formato inválido para el precio. Use solo números (ej: 2.50).");
            return;
        }

        if (precioUnit <= 0) {
            JOptionPane.showMessageDialog(objVista, "El precio debe ser un número mayor que cero.");
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

        String codigo = objDAO.generarCodigoProducto(tipo);
        int stock = 0;

        Producto producto = new Producto(nombre, tipo, precioUnit, proveedor, stock, codigo);
        objDAO.añadirProducto(producto);

        JOptionPane.showMessageDialog(
                objVista,
                "Producto registrado correctamente.\nCódigo: " + producto.getCodigo()
        );

        limpiarFormulario();
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
