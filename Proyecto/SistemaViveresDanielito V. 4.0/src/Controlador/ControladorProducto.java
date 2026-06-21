
package Controlador;

import Modelo.Producto;
import Modelo.ProductoDAO;
import Modelo.ProveedorDAO;
import Vista.Autocompletado;
import Vista.FrmProducto;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;





public class ControladorProducto implements ActionListener{

   FrmProducto objVista;
    ProductoDAO objDAO;
    ProveedorDAO objProveedorDAO;
    private final ArrayList<String> proveedoresConocidos = new ArrayList<>();

    public ControladorProducto(FrmProducto vista, ProductoDAO dao) {
        this.objVista = vista;
        this.objDAO = dao;
        this.objProveedorDAO = new ProveedorDAO();

        // el proveedor ya no es un campo de texto libre: se carga el
        // catalogo de proveedores ya usados y se activa el autocompletado,
        // asi un mismo proveedor siempre se escribe igual en vez de quedar
        // guardado de varias formas distintas entre productos.
        proveedoresConocidos.addAll(objProveedorDAO.obtenerNombresProveedores());
        Autocompletado.instalar(objVista.txtProveedor, proveedoresConocidos);

        // Conectar botones con acciones
        objVista.btnRegistrar.addActionListener(this);
        objVista.txtProveedor.addActionListener(this);
        objVista.CmbTipo.addActionListener(this);
    }

    private void actualizarCatalogoProveedores() {
        proveedoresConocidos.clear();
        proveedoresConocidos.addAll(objProveedorDAO.obtenerNombresProveedores());
        Autocompletado.actualizarItems(objVista.txtProveedor, proveedoresConocidos);
    }

    /**
     * el combo de proveedor es editable, asi que lo que el usuario escribio
     * vive en el componente de texto de su editor, no necesariamente en
     * getSelectedItem() (eso solo se actualiza si se selecciona un item de
     * la lista o se presiona Enter).
     */
    private String obtenerTextoProveedor() {
        Object editor = objVista.txtProveedor.getEditor().getEditorComponent();
        if (editor instanceof JTextField) {
            return ((JTextField) editor).getText().trim();
        }
        Object seleccionado = objVista.txtProveedor.getSelectedItem();
        return seleccionado == null ? "" : seleccionado.toString().trim();
    }

    private void establecerTextoProveedor(String texto) {
        Object editor = objVista.txtProveedor.getEditor().getEditorComponent();
        if (editor instanceof JTextField) {
            ((JTextField) editor).setText(texto);
        }
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
        String proveedor = obtenerTextoProveedor();

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

        // si el proveedor escrito todavia no estaba en el catalogo, se
        // agrega ahora (comparando sin distinguir mayusculas/minusculas
        // para no terminar con el mismo proveedor duplicado)
        objProveedorDAO.guardarSiNoExiste(proveedor);
        actualizarCatalogoProveedores();

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
        establecerTextoProveedor("");
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
