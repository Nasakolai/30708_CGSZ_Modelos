package Vista;

import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

/**
 * pequeña ayuda para que ciertos campos de texto NO dejen ni siquiera
 * escribir letras, en vez de solo avisar con un mensaje despues de que el
 * usuario ya le dio "Registrar"/"Guardar". se usa sobre todo en los campos
 * de precio y cantidad, donde no tiene sentido permitir nada que no sea un
 * numero.
 *
 * son metodos estaticos porque no guardan ningun estado propio, solo le
 * instalan un DocumentFilter al campo que se les pase.
 */
public final class FiltroNumerico {

    private FiltroNumerico() {
        // clase de solo metodos estaticos, no se instancia
    }

    /**
     * deja escribir solo digitos y, opcionalmente, un punto decimal (para
     * precios como "2.50"). la coma no se admite directamente porque el
     * resto del sistema siempre usa punto como separador decimal.
     */
    public static void soloDecimales(JTextField campo) {
        ((PlainDocument) campo.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
                if (esTextoDecimalValido(fb, offset, 0, text)) {
                    super.insertString(fb, offset, text, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (esTextoDecimalValido(fb, offset, length, text)) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }

    /**
     * deja escribir solo digitos (sin punto ni signo), para cantidades en
     * unidades enteras.
     */
    public static void soloEnteros(JTextField campo) {
        ((PlainDocument) campo.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
                if (esSoloDigitos(text)) {
                    super.insertString(fb, offset, text, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (esSoloDigitos(text)) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }

    /**
     * version para el campo de texto interno que usan los JSpinner (su
     * "editor" es en realidad un JFormattedTextField). se usa para que la
     * cantidad de un movimiento tampoco deje escribir letras a mano.
     */
    public static void soloEnteros(JFormattedTextField campo) {
        ((PlainDocument) campo.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
                if (esSoloDigitos(text)) {
                    super.insertString(fb, offset, text, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (esSoloDigitos(text)) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }

    private static boolean esSoloDigitos(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * valida que, despues de insertar/reemplazar el texto nuevo, el
     * resultado siga pareciendo un numero decimal valido (digitos con como
     * mucho un punto). se construye el texto resultante completo para
     * poder rechazar, por ejemplo, un segundo punto decimal.
     */
    private static boolean esTextoDecimalValido(DocumentFilter.FilterBypass fb, int offset, int length, String textoNuevo) throws BadLocationException {
        if (textoNuevo == null || textoNuevo.isEmpty()) {
            return true;
        }
        for (int i = 0; i < textoNuevo.length(); i++) {
            char c = textoNuevo.charAt(i);
            if (!Character.isDigit(c) && c != '.') {
                return false;
            }
        }
        String actual = fb.getDocument().getText(0, fb.getDocument().getLength());
        String resultado = actual.substring(0, offset) + textoNuevo + actual.substring(offset + length);
        // no se permite mas de un punto decimal en todo el campo
        return resultado.indexOf('.') == resultado.lastIndexOf('.');
    }
}
