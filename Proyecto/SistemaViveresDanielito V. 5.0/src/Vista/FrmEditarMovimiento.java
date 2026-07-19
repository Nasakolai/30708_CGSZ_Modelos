package Vista;

import Modelo.Movimiento;
import Modelo.MovimientoDAO;
import Modelo.Producto;
import Modelo.ProductoDAO;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;

/**
 * ventana para corregir un movimiento que ya se registro (por ejemplo si se
 * tecleo mal la cantidad), en vez de tener que borrarlo y crearlo de nuevo.
 *
 * lo importante de esta pantalla es que, al guardar, NO solo cambia el
 * movimiento: tambien ajusta el stock del producto por la diferencia entre
 * el efecto que tenia el movimiento antes y el que tiene despues de la
 * edicion. asi el inventario nunca queda descuadrado por una correccion.
 *
 * el producto al que pertenece el movimiento no se puede cambiar aqui (no
 * tendria sentido "mover" un movimiento a otro producto); solo se puede
 * corregir el tipo (entrada/salida), la cantidad y la fecha.
 */
public class FrmEditarMovimiento extends JDialog {

    private final Movimiento movimiento;
    private final MovimientoDAO movDao;
    private final ProductoDAO prodDao;
    private final Runnable alGuardar;

    private JRadioButton radioEntrada;
    private JRadioButton radioSalida;
    private JSpinner spinnerCantidad;
    private JFormattedTextField campoFecha;
    private JLabel labelProducto;
    private JLabel labelStockActual;

    public FrmEditarMovimiento(java.awt.Frame propietario, Movimiento movimiento, MovimientoDAO movDao, ProductoDAO prodDao, Runnable alGuardar) {
        super(propietario, "Editar Movimiento", true);
        this.movimiento = movimiento;
        this.movDao = movDao;
        this.prodDao = prodDao;
        this.alGuardar = alGuardar;
        construirInterfaz();
        cargarDatos();
        setLocationRelativeTo(propietario);
    }

    private void construirInterfaz() {
        setResizable(false);
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        panel.setBackground(Color.WHITE);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.anchor = GridBagConstraints.WEST;

        Font fuenteLabel = new Font("Roboto", Font.PLAIN, 16);
        Font fuenteCampo = new Font("Roboto", Font.PLAIN, 16);

        // producto (solo lectura, no se puede cambiar de producto al editar)
        c.gridx = 0; c.gridy = 0;
        JLabel tituloProducto = new JLabel("Producto:");
        tituloProducto.setFont(fuenteLabel);
        panel.add(tituloProducto, c);

        c.gridx = 1; c.gridy = 0;
        labelProducto = new JLabel();
        labelProducto.setFont(new Font("Roboto", Font.BOLD, 16));
        panel.add(labelProducto, c);

        // stock actual (informativo)
        c.gridx = 0; c.gridy = 1;
        JLabel tituloStock = new JLabel("Stock actual:");
        tituloStock.setFont(fuenteLabel);
        panel.add(tituloStock, c);

        c.gridx = 1; c.gridy = 1;
        labelStockActual = new JLabel();
        labelStockActual.setFont(new Font("Roboto", Font.BOLD, 16));
        panel.add(labelStockActual, c);

        // tipo (entrada/salida)
        c.gridx = 0; c.gridy = 2;
        JLabel tituloTipo = new JLabel("Tipo:");
        tituloTipo.setFont(fuenteLabel);
        panel.add(tituloTipo, c);

        c.gridx = 1; c.gridy = 2;
        JPanel panelTipo = new JPanel();
        panelTipo.setBackground(Color.WHITE);
        radioEntrada = new JRadioButton("Entrada");
        radioSalida = new JRadioButton("Salida");
        radioEntrada.setFont(fuenteCampo);
        radioSalida.setFont(fuenteCampo);
        radioEntrada.setBackground(Color.WHITE);
        radioSalida.setBackground(Color.WHITE);
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(radioEntrada);
        grupo.add(radioSalida);
        panelTipo.add(radioEntrada);
        panelTipo.add(radioSalida);
        panel.add(panelTipo, c);

        // cantidad
        c.gridx = 0; c.gridy = 3;
        JLabel tituloCantidad = new JLabel("Cantidad:");
        tituloCantidad.setFont(fuenteLabel);
        panel.add(tituloCantidad, c);

        c.gridx = 1; c.gridy = 3;
        spinnerCantidad = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        spinnerCantidad.setFont(fuenteCampo);
        if (spinnerCantidad.getEditor() instanceof JSpinner.DefaultEditor) {
            FiltroNumerico.soloEnteros(((JSpinner.DefaultEditor) spinnerCantidad.getEditor()).getTextField());
        }
        panel.add(spinnerCantidad, c);

        // fecha
        c.gridx = 0; c.gridy = 4;
        JLabel tituloFecha = new JLabel("Fecha:");
        tituloFecha.setFont(fuenteLabel);
        panel.add(tituloFecha, c);

        c.gridx = 1; c.gridy = 4;
        campoFecha = new JFormattedTextField();
        campoFecha.setFont(fuenteCampo);
        campoFecha.setColumns(10);
        try {
            MaskFormatter mascara = new MaskFormatter("##/##/####");
            mascara.setPlaceholderCharacter('_');
            campoFecha.setFormatterFactory(new DefaultFormatterFactory(mascara));
        } catch (ParseException ex) {
            // si por algun motivo no se pudo armar la mascara, el campo
            // sigue funcionando como un texto normal sin formato forzado
        }
        panel.add(campoFecha, c);

        // aviso de que el cambio tambien ajusta el stock
        c.gridx = 0; c.gridy = 5; c.gridwidth = 2;
        JLabel aviso = new JLabel("<html><i>Al guardar, el stock del producto se ajustará<br>"
                + "automáticamente por la diferencia con el movimiento original.</i></html>");
        aviso.setFont(new Font("Roboto", Font.PLAIN, 13));
        aviso.setForeground(new Color(90, 90, 90));
        panel.add(aviso, c);
        c.gridwidth = 1;

        // botones
        c.gridx = 0; c.gridy = 6; c.gridwidth = 2; c.anchor = GridBagConstraints.CENTER;
        JPanel panelBotones = new JPanel();
        panelBotones.setBackground(Color.WHITE);

        JButton btnGuardar = new JButton("Guardar Cambios");
        btnGuardar.setFont(fuenteCampo);
        btnGuardar.addActionListener(e -> guardarCambios());

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(fuenteCampo);
        btnCancelar.addActionListener(e -> dispose());

        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);
        panel.add(panelBotones, c);

        setContentPane(panel);
        pack();
    }

    private void cargarDatos() {
        labelProducto.setText(movimiento.getNombreProducto() + " (" + movimiento.getCodigoProducto() + ")");

        Producto producto = prodDao.buscarPorCodigo(movimiento.getCodigoProducto());
        labelStockActual.setText(producto == null ? "N/D (producto ya no existe)" : String.valueOf(producto.getStock()));

        if ("Entrada".equalsIgnoreCase(movimiento.getTipo())) {
            radioEntrada.setSelected(true);
        } else {
            radioSalida.setSelected(true);
        }
        spinnerCantidad.setValue(Math.max(1, movimiento.getCantidad()));
        campoFecha.setText(movimiento.getFecha());
    }

    private void guardarCambios() {
        String tipoNuevo = radioEntrada.isSelected() ? "Entrada" : "Salida";
        int cantidadNueva = ((Number) spinnerCantidad.getValue()).intValue();
        String fechaTexto = campoFecha.getText();

        if (fechaTexto == null || fechaTexto.contains("_") || fechaTexto.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese una fecha válida (dd/mm/aaaa).");
            return;
        }
        try {
            new SimpleDateFormat("dd/MM/yyyy").parse(fechaTexto.trim());
        } catch (ParseException ex) {
            JOptionPane.showMessageDialog(this, "La fecha ingresada no es válida.");
            return;
        }

        Producto producto = prodDao.buscarPorCodigo(movimiento.getCodigoProducto());
        if (producto == null) {
            JOptionPane.showMessageDialog(this,
                    "No se puede guardar: el producto \"" + movimiento.getNombreProducto()
                    + "\" ya no existe en el catálogo, por lo que no se puede ajustar su stock.");
            return;
        }

        // efecto que tenia el movimiento ANTES de editarlo, y el que va a
        // tener DESPUES. la diferencia entre los dos es lo que hay que
        // aplicarle al stock del producto para que quede cuadrado.
        String tipoOriginal = movimiento.getTipo();
        int cantidadOriginal = movimiento.getCantidad();
        int efectoViejo = "Entrada".equalsIgnoreCase(tipoOriginal) ? cantidadOriginal : -cantidadOriginal;
        int efectoNuevo = "Entrada".equalsIgnoreCase(tipoNuevo) ? cantidadNueva : -cantidadNueva;
        int delta = efectoNuevo - efectoViejo;

        int stockActual = producto.getStock();
        int stockNuevo = stockActual + delta;

        if (stockNuevo < 0) {
            JOptionPane.showMessageDialog(this,
                    "No se puede guardar: con este cambio el stock de \"" + producto.getNombre()
                    + "\" quedaría en " + stockNuevo + " (negativo).\n"
                    + "Stock actual: " + stockActual + ". Ajuste la cantidad e intente de nuevo.");
            return;
        }

        // se actualiza primero el stock del producto y despues el
        // movimiento; si algo fallara a mitad de camino es preferible que
        // el stock ya haya quedado bien y el movimiento sea el que
        // requiera revisar, en vez de lo contrario
        producto.setStock(stockNuevo);
        prodDao.modificarProducto(producto.getNombre(), producto);

        movimiento.setTipo(tipoNuevo);
        movimiento.setCantidad(cantidadNueva);
        movimiento.setFecha(fechaTexto.trim());
        movimiento.setTotal(cantidadNueva * movimiento.getPrecioUnitario());

        boolean actualizado = movDao.actualizarMovimiento(movimiento);
        if (!actualizado) {
            JOptionPane.showMessageDialog(this,
                    "El stock se ajustó, pero no se pudo actualizar el movimiento (puede que ya no exista).");
        } else {
            JOptionPane.showMessageDialog(this,
                    "Movimiento actualizado correctamente.\nStock de \"" + producto.getNombre() + "\": "
                    + stockActual + " → " + stockNuevo + ".");
        }

        if (alGuardar != null) {
            alGuardar.run();
        }
        dispose();
    }
}
