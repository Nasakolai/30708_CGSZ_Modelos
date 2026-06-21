package Controlador;

import Modelo.Producto;
import Modelo.ProductoDAO;
import Vista.FrmGestionInventarioo;
import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import Modelo.Movimiento;
import Modelo.MovimientoDAO;

public class ControladorGestionInventarioo {
    private final FrmGestionInventarioo vista;
    private final ProductoDAO dao;

    public ControladorGestionInventarioo(FrmGestionInventarioo vista, ProductoDAO dao) {
        this.vista = vista;
        this.dao = dao;
        this.movDao = new MovimientoDAO();
        cargarProductos();
        configurarListeners();
        actualizarStockMostrado();
    }

    private final MovimientoDAO movDao;

    private void cargarProductos() {
        vista.jComboBoxProductoMongo.removeAllItems();
        vista.jComboBoxProductoMongo.addItem("Seleccione...");
        for (String nombre : dao.obtenerNombresProductos()) {
            vista.jComboBoxProductoMongo.addItem(nombre);
        }
    }

    private void configurarListeners() {
        vista.jComboBoxProductoMongo.addActionListener((ActionEvent e) -> actualizarStockMostrado());
        vista.jRadioButtonEntrada.addActionListener((ActionEvent e) -> actualizarStockMostrado());
        vista.jRadioButtonSalida.addActionListener((ActionEvent e) -> actualizarStockMostrado());
        vista.jSpinnerCantidad.addChangeListener((ChangeEvent e) -> actualizarStockMostrado());
        vista.btnGuardarMovimiento.addActionListener((ActionEvent e) -> guardarMovimiento());
        vista.jButtonLimpiar.addActionListener((ActionEvent e) -> limpiarFormulario());
        vista.jButton1Cancelar.addActionListener((ActionEvent e) -> volverAlMenuPrincipal());
    }

    /**
     * antes este boton solo hacia vista.dispose(), lo cual dejaba al
     * usuario sin ninguna ventana abierta (la app parecia haberse cerrado
     * de la nada). ahora se comporta igual que el logo: abre el menu
     * principal y luego cierra esta pantalla.
     */
    private void volverAlMenuPrincipal() {
        Vista.FrmSistema frmSistema = new Vista.FrmSistema();
        frmSistema.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
        frmSistema.setVisible(true);
        frmSistema.setLocationRelativeTo(null);
        vista.dispose();
    }

    private void actualizarStockMostrado() {
        String seleccionado = (String) vista.jComboBoxProductoMongo.getSelectedItem();
        if (seleccionado == null || seleccionado.equals("Seleccione...")) {
            vista.jLabelStockActual.setText("0");
            vista.jLabel11StockDespuesdelMovimiento.setText("0");
            vista.jLabelcodigoUnico.setText("-");
            return;
        }

        Producto producto = dao.buscarPorNombre(seleccionado);
        if (producto == null) {
            vista.jLabelStockActual.setText("0");
            vista.jLabel11StockDespuesdelMovimiento.setText("0");
            vista.jLabelcodigoUnico.setText("-");
            return;
        }

        int actual = producto.getStock();
        int cantidad = obtenerCantidad();
        int despues = vista.jRadioButtonEntrada.isSelected() ? actual + cantidad : actual - cantidad;
        if (despues < 0) {
            despues = 0;
        }

        vista.jLabelStockActual.setText(String.valueOf(actual));
        vista.jLabel11StockDespuesdelMovimiento.setText(String.valueOf(despues));
        vista.jLabelcodigoUnico.setText(producto.getCodigo() == null ? "" : producto.getCodigo());
    }

    private int obtenerCantidad() {
        Object valor = vista.jSpinnerCantidad.getValue();
        if (valor instanceof Number) {
            return Math.max(0, ((Number) valor).intValue());
        }
        try {
            return Integer.parseInt(valor.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void guardarMovimiento() {
        String seleccionado = (String) vista.jComboBoxProductoMongo.getSelectedItem();
        if (seleccionado == null || seleccionado.equals("Seleccione...")) {
            JOptionPane.showMessageDialog(vista, "Seleccione un producto.");
            return;
        }

        Producto producto = dao.buscarPorNombre(seleccionado);
        if (producto == null) {
            JOptionPane.showMessageDialog(vista, "Producto no encontrado.");
            return;
        }

        int cantidad = obtenerCantidad();
        if (cantidad <= 0) {
            JOptionPane.showMessageDialog(vista, "Ingrese una cantidad válida.");
            return;
        }

        int nuevoStock = vista.jRadioButtonEntrada.isSelected() ? producto.getStock() + cantidad : producto.getStock() - cantidad;
        if (nuevoStock < 0) {
            JOptionPane.showMessageDialog(vista, "Stock insuficiente para la salida.");
            return;
        }

        Producto actualizado = new Producto(
                producto.getNombre(),
                producto.getTipo(),
                producto.getPrecioUnit(),
                producto.getProveedor(),
                nuevoStock,
                producto.getCodigo()
        );

        dao.modificarProducto(producto.getNombre(), actualizado);
        // Registrar movimiento en colección Movimientos
        try {
            String tipoMov = vista.jRadioButtonEntrada.isSelected() ? "Entrada" : "Salida";
            double precioUnit = producto.getPrecioUnit();
            String fecha = vista.jFormattedTextFieldFecha.getText();
            double total = precioUnit * cantidad;
            Movimiento mov = new Movimiento(tipoMov, cantidad, precioUnit, fecha, total, null, producto.getNombre(), producto.getCodigo());
            movDao.añadirMovimiento(mov);
        } catch (Exception ex) {
            System.out.println("Error al registrar movimiento: " + ex.getMessage());
        }
        JOptionPane.showMessageDialog(vista, "Movimiento registrado. Nuevo stock: " + nuevoStock);
        actualizarStockMostrado();
    }

    private void limpiarFormulario() {
        vista.jComboBoxProductoMongo.setSelectedIndex(0);
        vista.jSpinnerCantidad.setValue(1);
        vista.jTextArea1.setText("");
        vista.jRadioButtonEntrada.setSelected(true);
        vista.jLabelStockActual.setText("0");
        vista.jLabel11StockDespuesdelMovimiento.setText("0");
        vista.jLabelcodigoUnico.setText("-");
    }
}
