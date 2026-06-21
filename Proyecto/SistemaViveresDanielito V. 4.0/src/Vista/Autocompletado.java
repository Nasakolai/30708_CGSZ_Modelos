package Vista;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * autocompletado sencillo para un JComboBox editable: a medida que el
 * usuario va escribiendo, la lista desplegable se va filtrando para
 * mostrar solo los proveedores que ya existen en el catalogo y que
 * contienen lo que se lleva tecleado. el usuario siempre puede seguir
 * escribiendo un proveedor nuevo que todavia no este en la lista, esto
 * solo ayuda a reusar los que ya existen para que no se dupliquen con
 * distinta escritura.
 *
 * importante: instalar() debe llamarse una sola vez por combo. la lista
 * que se le pasa se va a seguir leyendo cada vez que el usuario teclee,
 * asi que si el catalogo cambia despues (por ejemplo se agrego un
 * proveedor nuevo) hay que actualizar esa MISMA lista (con
 * actualizarItems) en vez de instalar el autocompletado de nuevo.
 */
public final class Autocompletado {

    private Autocompletado() {
        // clase de solo metodos estaticos, no se instancia
    }

    public static void instalar(JComboBox<String> combo, List<String> elementos) {
        combo.setEditable(true);
        Object editorComponente = combo.getEditor().getEditorComponent();
        if (!(editorComponente instanceof JTextField)) {
            return;
        }
        JTextField editor = (JTextField) editorComponente;

        actualizarItems(combo, elementos);

        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                // las teclas de navegacion/edicion no deben disparar un
                // refiltrado (entre otras cosas porque ENTER/flechas se
                // usan para moverse por la lista que ya esta mostrada)
                int codigo = e.getKeyCode();
                if (codigo == KeyEvent.VK_ENTER || codigo == KeyEvent.VK_UP
                        || codigo == KeyEvent.VK_DOWN || codigo == KeyEvent.VK_ESCAPE) {
                    return;
                }
                SwingUtilities.invokeLater(() -> filtrar(combo, editor, elementos));
            }
        });
    }

    /**
     * vuelve a llenar el combo con los elementos recibidos (sin filtrar),
     * util justo despues de instalar() o cuando el catalogo cambio y hay
     * que refrescar lo que se muestra.
     */
    public static void actualizarItems(JComboBox<String> combo, List<String> elementos) {
        DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>(elementos.toArray(new String[0]));
        combo.setModel(modelo);
        combo.setSelectedItem(null);
    }

    private static void filtrar(JComboBox<String> combo, JTextField editor, List<String> elementos) {
        String texto = editor.getText();
        int posicionCursor = editor.getCaretPosition();

        DefaultComboBoxModel<String> modeloFiltrado = new DefaultComboBoxModel<>();
        if (texto.isEmpty()) {
            for (String item : elementos) {
                modeloFiltrado.addElement(item);
            }
        } else {
            String textoMin = texto.toLowerCase();
            for (String item : elementos) {
                if (item.toLowerCase().contains(textoMin)) {
                    modeloFiltrado.addElement(item);
                }
            }
        }

        combo.setModel(modeloFiltrado);
        // al cambiar el modelo, el combo intenta sincronizar el editor con
        // el item seleccionado (que ahora es ninguno), asi que hay que
        // devolverle a mano el texto que el usuario ya habia escrito
        editor.setText(texto);
        editor.setCaretPosition(Math.min(posicionCursor, texto.length()));

        if (modeloFiltrado.getSize() > 0 && !texto.isEmpty()) {
            combo.showPopup();
        } else {
            combo.hidePopup();
        }
    }
}
