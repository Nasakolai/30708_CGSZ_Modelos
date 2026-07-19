package Util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * utilidad para cargar imagenes del sistema con buena calidad.
 * la ponemos aqui centralizada para no tener el mismo codigo de escalar
 * copiado en cada ventana (antes habia 7 copias identicas del mismo metodo).
 *
 * el truco de escalar por pasos hace que los iconos queden mas nitidos,
 * especialmente cuando hay que bajar mucho de tamaño (ej: de 512x512 a 30x30).
 * si lo hacemos de un solo paso java hace un muestreo muy brusco y quedan
 * pixeleados. con pasos del 50% cada uno el resultado es bastante mejor.
 */
public class ImagenUtil {

    private ImagenUtil() {
        // clase de solo metodos estaticos
    }

    /**
     * carga la imagen en la ruta dada (dentro del classpath) y la pone en
     * el JLabel con el mejor suavizado posible. si la imagen no existe
     * solo imprime un aviso en consola y no tronamos el programa.
     *
     * el parametro caller es cualquier objeto de la ventana que llama,
     * sirve para encontrar el classloader correcto.
     */
    public static void poner(JLabel label, String ruta, Object caller) {
        URL url = caller.getClass().getResource(ruta);
        if (url == null) {
            System.err.println("aviso: no se encontro la imagen " + ruta);
            return;
        }

        // si el label todavia no se mostro en pantalla getWidth/getHeight
        // devuelve 0. usamos el preferredSize que el AbsoluteLayout ya fijo,
        // y si tampoco tiene, ponemos 32 como minimo para que no sea invisible.
        int w = label.getWidth();
        int h = label.getHeight();
        if (w <= 0) w = Math.max(label.getPreferredSize().width, 32);
        if (h <= 0) h = Math.max(label.getPreferredSize().height, 32);

        try {
            BufferedImage original = ImageIO.read(url);
            if (original == null) {
                System.err.println("aviso: no se pudo leer la imagen " + ruta);
                return;
            }
            BufferedImage escalada = escalarPorPasos(original, w, h);
            label.setIcon(new ImageIcon(escalada));
        } catch (IOException ex) {
            System.err.println("error cargando imagen " + ruta + ": " + ex.getMessage());
        }
    }

    // baja el tamaño a la mitad repetidas veces hasta llegar al tamaño final.
    // esto da mucho mejor resultado que bajar directamente de 512 a 30 de golpe.
    private static BufferedImage escalarPorPasos(BufferedImage src, int anchoFinal, int altoFinal) {
        int aw = src.getWidth();
        int ah = src.getHeight();
        BufferedImage actual = src;

        while (aw > anchoFinal * 2 || ah > altoFinal * 2) {
            int nw = Math.max(aw / 2, anchoFinal);
            int nh = Math.max(ah / 2, altoFinal);
            actual = paso(actual, nw, nh);
            aw = nw;
            ah = nh;
        }
        return paso(actual, anchoFinal, altoFinal);
    }

    private static BufferedImage paso(BufferedImage src, int w, int h) {
        BufferedImage res = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = res.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return res;
    }
}
