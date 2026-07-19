package Controlador;

import Modelo.CategoriaDAO;
import Modelo.Producto;
import Modelo.ProductoDAO;
import Modelo.ProveedorDAO;
import Vista.FrmProducto;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class ControladorProducto implements ActionListener {

    FrmProducto objVista;
    ProductoDAO objDAO;
    ProveedorDAO objProveedorDAO;
    CategoriaDAO categoriaDAO;
    private final ArrayList<String> proveedoresConocidos = new ArrayList<>();

    // tiene que coincidir con el primer item del CmbTipo
    private static final String TIPO_SIN_SELECCIONAR = "Seleccione un tipo";
    // tiene que coincidir con el primer item de txtProveedor
    private static final String PROVEEDOR_SIN_SELECCIONAR = "Seleccione una opción";

    public ControladorProducto(FrmProducto vista, ProductoDAO dao) {
        this.objVista = vista;
        this.objDAO = dao;
        this.objProveedorDAO = new ProveedorDAO();
        this.categoriaDAO = new CategoriaDAO();

        // si la coleccion de categorias esta vacia (primera vez que arranca
        // el sistema) se meten las categorias de siempre para que el combo
        // no aparezca en blanco
        categoriaDAO.inicializarSiVacia();

        // proveedores: igual que las categorias, es una lista de solo
        // seleccion. ya no se puede escribir un proveedor nuevo aqui, eso
        // solo se hace desde Gestion de Proveedores.
        cargarProveedores();

        // categorias dinamicas desde mongo
        cargarCategorias();

        // conectar botones
        objVista.btnRegistrar.addActionListener(this);
        objVista.txtProveedor.addActionListener(this);
        objVista.CmbTipo.addActionListener(this);
        objVista.btnNuevaCategoria.addActionListener(e -> agregarNuevaCategoria());
    }

    /**
     * llena el combo con las categorias guardadas en mongo.
     * siempre empieza con "Seleccione un tipo" para que no se pueda
     * guardar un producto sin elegir una categoria de verdad.
     */
    private void cargarCategorias() {
        objVista.CmbTipo.removeAllItems();
        objVista.CmbTipo.addItem(TIPO_SIN_SELECCIONAR);
        for (String cat : categoriaDAO.listar()) {
            objVista.CmbTipo.addItem(cat);
        }
        objVista.CmbTipo.setSelectedIndex(0);
    }

    /**
     * llena el combo de proveedores con el catalogo guardado en mongo.
     * siempre empieza con "Seleccione una opción" (que tampoco se puede
     * registrar, igual que el tipo) para que no se pueda guardar un
     * producto sin elegir un proveedor de verdad.
     */
    private void cargarProveedores() {
        proveedoresConocidos.clear();
        proveedoresConocidos.addAll(objProveedorDAO.obtenerNombresProveedores());
        objVista.txtProveedor.removeAllItems();
        objVista.txtProveedor.addItem(PROVEEDOR_SIN_SELECCIONAR);
        for (String prov : proveedoresConocidos) {
            objVista.txtProveedor.addItem(prov);
        }
        objVista.txtProveedor.setSelectedIndex(0);
    }

    private void agregarNuevaCategoria() {
        String nombre = JOptionPane.showInputDialog(
                objVista,
                "Escriba el nombre de la nueva categoria:",
                "Nueva categoria",
                JOptionPane.PLAIN_MESSAGE
        );
        if (nombre == null || nombre.trim().isEmpty()) {
            return;
        }
        boolean ok = categoriaDAO.agregar(nombre.trim());
        if (ok) {
            JOptionPane.showMessageDialog(objVista, "Categoria \"" + nombre.trim() + "\" agregada.");
            cargarCategorias();
            // dejamos seleccionada la nueva para que no tenga que buscarla
            objVista.CmbTipo.setSelectedItem(nombre.trim());
        } else {
            JOptionPane.showMessageDialog(objVista,
                    "Ya existe una categoria con ese nombre (o muy parecida).\nRevisa la lista.",
                    "Categoria duplicada", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * el combo de proveedor ya no es editable: el proveedor elegido siempre
     * vive en getSelectedItem().
     */
    private String obtenerTextoProveedor() {
        Object seleccionado = objVista.txtProveedor.getSelectedItem();
        return seleccionado == null ? "" : seleccionado.toString().trim();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == objVista.btnRegistrar) {
            registrarProducto();
        }
    }

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

        if (proveedor.isBlank() || proveedor.equals(PROVEEDOR_SIN_SELECCIONAR)) {
            JOptionPane.showMessageDialog(objVista, "Seleccione un proveedor valido.");
            return;
        }

        double precioUnit;
        try {
            precioUnit = Double.parseDouble(precioTexto);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(objVista, "Formato invalido para el precio. Use solo numeros (ej: 2.50).");
            return;
        }

        if (precioUnit <= 0) {
            JOptionPane.showMessageDialog(objVista, "El precio debe ser mayor que cero.");
            return;
        }

        if (objDAO.existeProducto(nombre)) {
            JOptionPane.showMessageDialog(objVista, "Ya existe un producto con ese nombre.",
                    "Producto duplicado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String codigo = objDAO.generarCodigoProducto(tipo);
        Producto producto = new Producto(nombre, tipo, precioUnit, proveedor, 0, codigo);
        objDAO.añadirProducto(producto);

        JOptionPane.showMessageDialog(objVista,
                "Producto registrado correctamente.\nCodigo: " + producto.getCodigo());

        limpiarFormulario();
    }

    private void limpiarFormulario() {
        objVista.txtNombre.setText("");
        objVista.CmbTipo.setSelectedIndex(0);
        objVista.txtPrecio.setText("");
        objVista.txtProveedor.setSelectedIndex(0);
        objVista.jTextFieldCodigoProducto.setText("");
        objVista.txtNombre.requestFocus();
    }

    public void llenarTabla(JTable tabla) {
        llenarTablaConLista(tabla, objDAO.listarProductos());
    }

    public void llenarTablaConLista(JTable tabla, ArrayList<Producto> lista) {
        DefaultTableModel modelo = new DefaultTableModel();
        modelo.addColumn("Nombre");
        modelo.addColumn("Tipo");
        modelo.addColumn("Precio/U");
        modelo.addColumn("Proveedor");
        modelo.addColumn("Stock");
        for (Producto e : lista) {
            modelo.addRow(new Object[]{e.getNombre(), e.getTipo(), e.getPrecioUnit(), e.getProveedor(), e.getStock()});
        }
        tabla.setModel(modelo);
    }
}
