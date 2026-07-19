package Controlador;

import Modelo.Producto;
import Modelo.ProductoDAO;
import Vista.FrmGestionInventarioo;
import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import Modelo.ConfiguracionDAO;
import Modelo.Movimiento;
import Modelo.MovimientoDAO;
import Modelo.MotivosMovimiento;

public class ControladorGestionInventarioo {
    private final FrmGestionInventarioo vista;
    private final ProductoDAO dao;

    public ControladorGestionInventarioo(FrmGestionInventarioo vista, ProductoDAO dao) {
        this.vista = vista;
        this.dao = dao;
        this.movDao = new MovimientoDAO();
        this.configDao = new ConfiguracionDAO();
        cargarProductos();
        configurarListeners();
        cargarMotivos();
        actualizarDetalleHabilitado();
        actualizarStockMostrado();
    }

    private final MovimientoDAO movDao;
    private final ConfiguracionDAO configDao;

    private void cargarProductos() {
        vista.jComboBoxProductoMongo.removeAllItems();
        vista.jComboBoxProductoMongo.addItem("Seleccione...");
        for (String nombre : dao.obtenerNombresProductos()) {
            vista.jComboBoxProductoMongo.addItem(nombre);
        }
    }

    private void configurarListeners() {
        vista.jComboBoxProductoMongo.addActionListener((ActionEvent e) -> actualizarStockMostrado());
        vista.jRadioButtonEntrada.addActionListener((ActionEvent e) -> { cargarMotivos(); actualizarStockMostrado(); });
        vista.jRadioButtonSalida.addActionListener((ActionEvent e) -> { cargarMotivos(); actualizarStockMostrado(); });
        vista.jComboBoxMotivo.addActionListener((ActionEvent e) -> actualizarDetalleHabilitado());
        vista.jSpinnerCantidad.addChangeListener((ChangeEvent e) -> actualizarStockMostrado());
        vista.btnGuardarMovimiento.addActionListener((ActionEvent e) -> guardarMovimiento());
        vista.jButtonLimpiar.addActionListener((ActionEvent e) -> limpiarFormulario());
        vista.jButton1Cancelar.addActionListener((ActionEvent e) -> volverAlMenuPrincipal());
    }

    /**
     * llena el combo de motivos segun si esta marcado Entrada o Salida.
     * cada tipo de movimiento tiene sus propias causas tipicas (ver
     * Modelo.MotivosMovimiento), y ambos terminan siempre con "Otro" para
     * cualquier caso que no encaje en la lista.
     */
    private void cargarMotivos() {
        Object seleccionActual = vista.jComboBoxMotivo.getSelectedItem();
        String[] motivos = vista.jRadioButtonEntrada.isSelected()
                ? MotivosMovimiento.MOTIVOS_ENTRADA
                : MotivosMovimiento.MOTIVOS_SALIDA;
        vista.jComboBoxMotivo.removeAllItems();
        for (String motivo : motivos) {
            vista.jComboBoxMotivo.addItem(motivo);
        }
        // si el motivo que ya estaba elegido sigue existiendo en la lista
        // nueva (ej. "Otro" esta en ambas) lo dejamos seleccionado
        if (seleccionActual != null) {
            vista.jComboBoxMotivo.setSelectedItem(seleccionActual);
        }
        actualizarDetalleHabilitado();
    }

    /**
     * el campo de detalle (jTextArea1) solo tiene sentido llenarlo cuando
     * el motivo elegido es "Otro"; en cualquier otro caso lo deshabilita
     * y lo deja vacio para no guardar basura sin querer.
     */
    private void actualizarDetalleHabilitado() {
        boolean esOtro = MotivosMovimiento.OTRO.equals(vista.jComboBoxMotivo.getSelectedItem());
        vista.jTextArea1.setEnabled(esOtro);
        if (!esOtro) {
            vista.jTextArea1.setText("");
        }
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

        String motivo = (String) vista.jComboBoxMotivo.getSelectedItem();
        if (motivo == null || motivo.isBlank()) {
            JOptionPane.showMessageDialog(vista, "Seleccione un motivo para el movimiento.");
            return;
        }
        String detalleMotivo = vista.jTextArea1.getText() == null ? "" : vista.jTextArea1.getText().trim();
        if (MotivosMovimiento.OTRO.equals(motivo) && detalleMotivo.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Escriba el detalle del motivo \"Otro\".");
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
            // el responsable siempre es el nombre configurado actualmente
            // en la pantalla de Reportes (por defecto "Enrique Guaiguacundo")
            String responsable = configDao.obtenerResponsable();
            Movimiento mov = new Movimiento(tipoMov, cantidad, precioUnit, fecha, total,
                    responsable, producto.getNombre(), producto.getCodigo(),
                    motivo, MotivosMovimiento.OTRO.equals(motivo) ? detalleMotivo : null);
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
        vista.jRadioButtonEntrada.setSelected(true);
        cargarMotivos();
        vista.jTextArea1.setText("");
        vista.jLabelStockActual.setText("0");
        vista.jLabel11StockDespuesdelMovimiento.setText("0");
        vista.jLabelcodigoUnico.setText("-");
    }
}
