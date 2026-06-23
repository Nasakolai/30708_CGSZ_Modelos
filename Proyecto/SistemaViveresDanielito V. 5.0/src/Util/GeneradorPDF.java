package Util;

import java.awt.Color;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * generador de PDF liviano que no necesita ninguna libreria externa,
 * solo el JDK. implementa lo basico de la especificacion PDF 1.4:
 * paginas, texto, rectangulos con color e imagen JPEG embebida.
 *
 * para el contexto de este sistema (reportes de una sola persona con
 * un volumen moderado de datos) esto es mas que suficiente, y ademas
 * funciona en cualquier computadora sin instalar nada extra.
 *
 * colores del sistema:
 *   azul oscuro del header: RGB(0, 38, 102)  -> #002666
 *   azul claro del sidebar: RGB(153,190,255) -> #99BEFF
 *   blanco texto:           RGB(255,255,255) -> #FFFFFF
 */
public class GeneradorPDF {

    // colores del sistema Viveres Danielito
    private static final Color COLOR_HEADER   = new Color(0, 38, 102);
    private static final Color COLOR_SUBHEADER = new Color(153, 190, 255);
    private static final Color COLOR_TEXTO     = new Color(10, 25, 51);
    private static final Color COLOR_BLANCO    = Color.WHITE;
    private static final Color COLOR_GRIS_FILA = new Color(230, 238, 255);
    private static final Color COLOR_BORDE     = new Color(100, 140, 200);

    // dimensiones en puntos PDF (1 punto = 1/72 pulgada)
    // A4: 595 x 842 pts  -- Letter: 612 x 792 pts
    private static final float ANCHO_PAGINA   = 612;
    private static final float ALTO_PAGINA    = 792;
    private static final float MARGEN_IZQ     = 36;
    private static final float MARGEN_DER     = 36;
    private static final float ANCHO_CONTENIDO = ANCHO_PAGINA - MARGEN_IZQ - MARGEN_DER;

    // estado interno del documento
    private final List<byte[]> objetosPDF = new ArrayList<>();
    private final List<Integer> offsetsObjetos = new ArrayList<>();
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private int nObjetos = 0;
    private int offsetActual = 0;

    // para el contenido de la pagina actual
    private StringBuilder contenidoPagina = new StringBuilder();
    private float yActual = ALTO_PAGINA - 50; // posicion vertical actual
    private List<Integer> paginasIds = new ArrayList<>();
    private List<Integer> contenidosIds = new ArrayList<>();
    private int logoImagenId = -1;
    private int logoAnchoOriginal = 200;
    private int logoAltoOriginal  = 129;

    public GeneradorPDF() {}

    // ----------------------------------------------------------------
    // API publica
    // ----------------------------------------------------------------

    /**
     * genera el PDF completo de un reporte de movimientos y lo guarda
     * en el archivo indicado.
     *
     * @param movimientos  cada String[] tiene: {fecha, tipo, producto, codigo, cantidad, precioU, total}
     * @param tipoReporte  descripcion breve del tipo de reporte (ej: "General", "Filtrado por fecha...")
     * @param responsable  nombre de quien genera el reporte
     * @param empresa      nombre de la empresa para el encabezado
     * @param rutaSalida   ruta del archivo .pdf que se va a crear
     */
    public static void generarReporteMovimientos(
            List<String[]> movimientos,
            String tipoReporte,
            String responsable,
            String empresa,
            String logoPath,
            File rutaSalida) throws IOException {

        GeneradorPDF gen = new GeneradorPDF();

        // escribir la cabecera del PDF (reserva los ids 1 y 2 para el
        // catalogo y las paginas; esto debe hacerse ANTES de cargar el
        // logo, o el logo terminaria reservando el id 1 tambien y
        // chocando con el catalogo al cerrar el documento)
        gen.escribirCabeceraPDF();

        // cargar el logo
        gen.cargarLogoDesdeClasspath(logoPath);

        // preparar la primera pagina
        gen.iniciarPagina();

        // encabezado con logo y datos de la empresa
        gen.dibujarEncabezadoReporte(empresa, tipoReporte, responsable,
                new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()),
                movimientos.size());

        // tabla de movimientos
        gen.dibujarTablaMovimientos(movimientos);

        // resumen final
        gen.dibujarResumen(movimientos);

        // pie de pagina
        gen.dibujarPiePagina(empresa);

        // cerrar la pagina y el documento
        gen.cerrarPaginaActual();
        gen.cerrarDocumento(rutaSalida);
    }

    /**
     * genera el PDF de inventario actual (stock de productos). las columnas
     * son: codigo, producto, tipo, proveedor, stock, precio unitario, valor total.
     */
    public static void generarReporteStock(
            List<String[]> filas,
            String descripcionTipo,
            String responsable,
            String empresa,
            String logoPath,
            int totalUnidades,
            double valorTotalInventario,
            File rutaSalida) throws IOException {

        GeneradorPDF gen = new GeneradorPDF();
        gen.escribirCabeceraPDF();
        gen.cargarLogoDesdeClasspath(logoPath);
        gen.iniciarPagina();
        gen.dibujarEncabezadoStock(empresa, descripcionTipo, responsable,
                new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()), filas.size());
        gen.dibujarTablaStock(filas);
        gen.dibujarResumenStock(filas.size(), totalUnidades, valorTotalInventario);
        gen.dibujarPiePagina(empresa);
        gen.cerrarPaginaActual();
        gen.cerrarDocumento(rutaSalida);
    }

    // ----------------------------------------------------------------
    // implementacion interna
    // ----------------------------------------------------------------

    private void cargarLogoDesdeClasspath(String ruta) {
        // los bytes del logo se embeben directamente en el PDF como imagen JPEG
        try {
            URL url = getClass().getResource(ruta);
            if (url == null) {
                System.err.println("aviso: no se encontro el logo en " + ruta);
                return;
            }
            try (InputStream is = url.openStream()) {
                byte[] bytes = is.readAllBytes();
                // registrar como objeto de imagen JPEG en el PDF
                logoImagenId = reservarIdObjeto();
                objetosPDF.add(construirObjetoImagen(logoImagenId, bytes,
                        logoAnchoOriginal, logoAltoOriginal));
            }
        } catch (IOException ex) {
            System.err.println("error cargando logo: " + ex.getMessage());
        }
    }

    private void iniciarPagina() {
        contenidoPagina = new StringBuilder();
        yActual = ALTO_PAGINA - 30;
    }

    private void cerrarPaginaActual() {
        // objeto de contenido de la pagina (stream de operadores PDF)
        int idContenido = reservarIdObjeto();
        byte[] streamBytes = contenidoPagina.toString().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        String objContenido = idContenido + " 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n"
                + contenidoPagina + "\nendstream\nendobj\n";
        objetosPDF.add(objContenido.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        contenidosIds.add(idContenido);

        // objeto de pagina
        int idPagina = reservarIdObjeto();
        paginasIds.add(idPagina);

        // el contenido de recursos dependera de si hay imagen
        String recursos;
        if (logoImagenId >= 0) {
            recursos = "<< /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >> "
                     + "/F2 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >> >> "
                     + "/XObject << /ImgLogo " + logoImagenId + " 0 R >> >>";
        } else {
            recursos = "<< /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >> "
                     + "/F2 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >> >> >>";
        }

        String objPagina = idPagina + " 0 obj\n"
                + "<< /Type /Page /Parent 2 0 R "
                + "/MediaBox [0 0 " + (int)ANCHO_PAGINA + " " + (int)ALTO_PAGINA + "] "
                + "/Contents " + idContenido + " 0 R "
                + "/Resources " + recursos + " >>\nendobj\n";
        objetosPDF.add(objPagina.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
    }

    private void dibujarEncabezadoReporte(String empresa, String tipoReporte,
            String responsable, String fechaHora, int totalRegistros) {

        // banda azul oscura de fondo del encabezado
        rect(MARGEN_IZQ, ALTO_PAGINA - 110, ANCHO_CONTENIDO, 90, COLOR_HEADER, true);

        // logo (si se cargo correctamente)
        if (logoImagenId >= 0) {
            // en PDF las coordenadas Y van de abajo hacia arriba
            float logoY = ALTO_PAGINA - 105;
            float logoAncho = 130;
            float logoAlto  = 84;
            contenidoPagina.append("q\n");
            contenidoPagina.append(logoAncho + " 0 0 " + logoAlto + " "
                    + MARGEN_IZQ + " " + logoY + " cm\n");
            contenidoPagina.append("/ImgLogo Do\n");
            contenidoPagina.append("Q\n");
        }

        // nombre de la empresa (blanco sobre azul)
        texto(empresa, MARGEN_IZQ + 140, ALTO_PAGINA - 55, 18, true, COLOR_BLANCO);
        texto("Reporte de Movimientos de Inventario",
                MARGEN_IZQ + 140, ALTO_PAGINA - 75, 11, false, new Color(200, 220, 255));

        // banda azul claro debajo del encabezado (metadatos del reporte).
        // antes estos 4 datos iban en 2 columnas lado a lado y se encimaban
        // en cuanto el texto de la izquierda (tipo de reporte, nombre largo,
        // etc) era mas ancho que la mitad del espacio disponible. ahora van
        // apilados en una sola columna para que nunca se encimen.
        rect(MARGEN_IZQ, ALTO_PAGINA - 168, ANCHO_CONTENIDO, 56, COLOR_SUBHEADER, true);
        textoEnColor("Tipo: " + tipoReporte, MARGEN_IZQ + 6, ALTO_PAGINA - 122, 8, false, COLOR_TEXTO);
        textoEnColor("Generado por: " + responsable, MARGEN_IZQ + 6, ALTO_PAGINA - 134, 8, false, COLOR_TEXTO);
        textoEnColor("Fecha: " + fechaHora, MARGEN_IZQ + 6, ALTO_PAGINA - 146, 8, false, COLOR_TEXTO);
        textoEnColor("Total de registros: " + totalRegistros, MARGEN_IZQ + 6, ALTO_PAGINA - 158, 8, false, COLOR_TEXTO);

        yActual = ALTO_PAGINA - 180;
    }

    private void dibujarTablaMovimientos(List<String[]> movimientos) {
        // anchos de columna (en puntos), suman ANCHO_CONTENIDO
        float[] anchos   = {60, 45, 115, 55, 45, 60, 60, 100};
        String[] headers = {"Fecha", "Tipo", "Producto", "Codigo", "Cant.", "Precio/U", "Total", "Proveedor"};

        // fila de encabezado de la tabla
        float xBase = MARGEN_IZQ;
        float altoFila = 16;
        rect(xBase, yActual - altoFila, ANCHO_CONTENIDO, altoFila, COLOR_HEADER, true);
        float x = xBase + 3;
        for (int i = 0; i < headers.length; i++) {
            textoEnColor(headers[i], x, yActual - 11, 8, true, COLOR_BLANCO);
            x += anchos[i];
        }
        yActual -= altoFila;

        // filas de datos
        boolean filaAlterna = false;
        for (String[] fila : movimientos) {
            if (yActual < 120) {
                // nueva pagina (simplificado: en produccion habria salto de pagina real)
                yActual = 80; // evitar que salga de la pagina
                break;
            }
            Color colorFondo = filaAlterna ? COLOR_GRIS_FILA : COLOR_BLANCO;
            rect(xBase, yActual - altoFila, ANCHO_CONTENIDO, altoFila, colorFondo, true);
            // borde inferior de la fila
            linea(xBase, yActual - altoFila, xBase + ANCHO_CONTENIDO, yActual - altoFila, COLOR_BORDE);

            x = xBase + 3;
            for (int i = 0; i < Math.min(fila.length, anchos.length); i++) {
                String celda = fila[i] == null ? "" : fila[i];
                // truncar si es muy largo
                if (celda.length() > 20 && i == 2) celda = celda.substring(0, 18) + "..";
                textoEnColor(celda, x, yActual - 11, 7, false, COLOR_TEXTO);
                x += anchos[i];
            }
            yActual -= altoFila;
            filaAlterna = !filaAlterna;
        }

        // borde de toda la tabla
        borde(xBase, yActual, ANCHO_CONTENIDO, ALTO_PAGINA - 162 - yActual, COLOR_BORDE);
        yActual -= 12;
    }

    private void dibujarResumen(List<String[]> movimientos) {
        if (yActual < 130) return;
        int entradas = 0, salidas = 0;
        double montoEnt = 0, montoSal = 0;
        for (String[] f : movimientos) {
            if (f.length > 6) {
                boolean esEntrada = f.length > 1 && "Entrada".equalsIgnoreCase(f[1]);
                try { double t = Double.parseDouble(f[6]); if(esEntrada){entradas++;montoEnt+=t;}else{salidas++;montoSal+=t;} } catch(Exception ignored){}
            }
        }
        yActual -= 6;
        rect(MARGEN_IZQ, yActual - 48, ANCHO_CONTENIDO, 48, COLOR_SUBHEADER, true);
        borde(MARGEN_IZQ, yActual - 48, ANCHO_CONTENIDO, 48, COLOR_BORDE);
        texto("RESUMEN", MARGEN_IZQ + 6, yActual - 12, 10, true, COLOR_TEXTO);
        textoEnColor(String.format(Locale.US, "Entradas: %d  |  Monto total entradas: $%.2f", entradas, montoEnt),
                MARGEN_IZQ + 6, yActual - 25, 9, false, COLOR_TEXTO);
        textoEnColor(String.format(Locale.US, "Salidas:  %d  |  Monto total salidas:  $%.2f", salidas, montoSal),
                MARGEN_IZQ + 6, yActual - 37, 9, false, COLOR_TEXTO);
        textoEnColor(String.format(Locale.US, "Balance del periodo: $%.2f", montoEnt - montoSal),
                MARGEN_IZQ + 6, yActual - 48 + 3, 9, true, COLOR_TEXTO);
        yActual -= 55;
    }

    private void dibujarEncabezadoStock(String empresa, String tipoReporte,
            String responsable, String fechaHora, int totalProductos) {

        rect(MARGEN_IZQ, ALTO_PAGINA - 110, ANCHO_CONTENIDO, 90, COLOR_HEADER, true);

        if (logoImagenId >= 0) {
            float logoY = ALTO_PAGINA - 105;
            contenidoPagina.append("q\n");
            contenidoPagina.append("130 0 0 84 " + MARGEN_IZQ + " " + logoY + " cm\n");
            contenidoPagina.append("/ImgLogo Do\n");
            contenidoPagina.append("Q\n");
        }

        texto(empresa, MARGEN_IZQ + 140, ALTO_PAGINA - 55, 18, true, COLOR_BLANCO);
        texto("Reporte de Inventario Actual (Stock)",
                MARGEN_IZQ + 140, ALTO_PAGINA - 75, 11, false, new Color(200, 220, 255));

        rect(MARGEN_IZQ, ALTO_PAGINA - 168, ANCHO_CONTENIDO, 56, COLOR_SUBHEADER, true);
        textoEnColor("Tipo: " + tipoReporte, MARGEN_IZQ + 6, ALTO_PAGINA - 122, 8, false, COLOR_TEXTO);
        textoEnColor("Generado por: " + responsable, MARGEN_IZQ + 6, ALTO_PAGINA - 134, 8, false, COLOR_TEXTO);
        textoEnColor("Fecha: " + fechaHora, MARGEN_IZQ + 6, ALTO_PAGINA - 146, 8, false, COLOR_TEXTO);
        textoEnColor("Total de productos: " + totalProductos, MARGEN_IZQ + 6, ALTO_PAGINA - 158, 8, false, COLOR_TEXTO);

        yActual = ALTO_PAGINA - 180;
    }

    private void dibujarTablaStock(List<String[]> filas) {
        // columnas: codigo, producto, tipo, proveedor, stock, precio/u, valor total
        float[] anchos   = {60, 120, 65, 100, 40, 60, 70};
        String[] headers = {"Codigo", "Producto", "Tipo", "Proveedor", "Stock", "Precio/U", "Valor Total"};

        float xBase = MARGEN_IZQ;
        float altoFila = 16;
        rect(xBase, yActual - altoFila, ANCHO_CONTENIDO, altoFila, COLOR_HEADER, true);
        float x = xBase + 3;
        for (int i = 0; i < headers.length; i++) {
            textoEnColor(headers[i], x, yActual - 11, 8, true, COLOR_BLANCO);
            x += anchos[i];
        }
        yActual -= altoFila;

        boolean filaAlterna = false;
        for (String[] fila : filas) {
            if (yActual < 130) { yActual = 80; break; }
            Color colorFondo = filaAlterna ? COLOR_GRIS_FILA : COLOR_BLANCO;
            rect(xBase, yActual - altoFila, ANCHO_CONTENIDO, altoFila, colorFondo, true);
            linea(xBase, yActual - altoFila, xBase + ANCHO_CONTENIDO, yActual - altoFila, COLOR_BORDE);
            x = xBase + 3;
            for (int i = 0; i < Math.min(fila.length - 1, anchos.length); i++) {
                String celda = fila[i] == null ? "" : fila[i];
                if (celda.length() > 18 && i == 1) celda = celda.substring(0, 16) + "..";
                textoEnColor(celda, x, yActual - 11, 7, false, COLOR_TEXTO);
                x += anchos[i];
            }
            yActual -= altoFila;
            filaAlterna = !filaAlterna;
        }
        borde(xBase, yActual, ANCHO_CONTENIDO, ALTO_PAGINA - 162 - yActual, COLOR_BORDE);
        yActual -= 12;
    }

    private void dibujarResumenStock(int totalProductos, int totalUnidades, double valorTotal) {
        if (yActual < 130) return;
        yActual -= 6;
        rect(MARGEN_IZQ, yActual - 52, ANCHO_CONTENIDO, 52, COLOR_SUBHEADER, true);
        borde(MARGEN_IZQ, yActual - 52, ANCHO_CONTENIDO, 52, COLOR_BORDE);
        texto("RESUMEN", MARGEN_IZQ + 6, yActual - 12, 10, true, COLOR_TEXTO);
        textoEnColor("Productos listados:          " + totalProductos,
                MARGEN_IZQ + 6, yActual - 25, 9, false, COLOR_TEXTO);
        textoEnColor("Unidades totales en stock:   " + totalUnidades,
                MARGEN_IZQ + 6, yActual - 37, 9, false, COLOR_TEXTO);
        textoEnColor(String.format(Locale.US, "Valor total del inventario:  $%.2f", valorTotal),
                MARGEN_IZQ + 6, yActual - 49 + 2, 9, true, COLOR_TEXTO);
        yActual -= 58;
    }

    private void dibujarPiePagina(String empresa) {
        float yPie = 30;
        linea(MARGEN_IZQ, yPie + 12, MARGEN_IZQ + ANCHO_CONTENIDO, yPie + 12, COLOR_HEADER);
        textoEnColor("Fin del reporte  —  " + empresa + "  —  Sistema de Gestión de Inventario",
                MARGEN_IZQ, yPie, 7, false, COLOR_TEXTO);
    }

    // ----------------------------------------------------------------
    // primitivas de dibujo (operadores PDF)
    // ----------------------------------------------------------------

    private void rect(float x, float y, float w, float h, Color color, boolean relleno) {
        String colorStr = pdfColor(color, relleno);
        contenidoPagina.append(colorStr)
                .append(String.format(Locale.US, "%.2f %.2f %.2f %.2f re ", x, y, w, h))
                .append(relleno ? "f\n" : "S\n");
    }

    private void borde(float x, float y, float w, float h, Color color) {
        contenidoPagina.append(pdfColor(color, false))
                .append(String.format(Locale.US, "%.2f w ", 0.5f))
                .append(String.format(Locale.US, "%.2f %.2f %.2f %.2f re S\n", x, y, w, h));
    }

    private void linea(float x1, float y1, float x2, float y2, Color color) {
        contenidoPagina.append(pdfColor(color, false))
                .append(String.format(Locale.US, "%.2f w %.2f %.2f m %.2f %.2f l S\n", 0.5f, x1, y1, x2, y2));
    }

    private void texto(String txt, float x, float y, int tamaño, boolean negrita, Color color) {
        textoEnColor(txt, x, y, tamaño, negrita, color);
    }

    private void textoEnColor(String txt, float x, float y, int tamaño, boolean negrita, Color color) {
        if (txt == null || txt.isEmpty()) return;
        String fuente = negrita ? "/F2" : "/F1";
        String rgb = String.format(Locale.US, "%.4f %.4f %.4f rg",
                color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
        // escapar parentesis y backslash en el texto
        String escaped = txt.replace("\\", "\\\\")
                            .replace("(", "\\(")
                            .replace(")", "\\)");
        contenidoPagina.append("BT ")
                .append(rgb).append(" ")
                .append(fuente).append(" ").append(tamaño).append(" Tf ")
                .append(String.format(Locale.US, "%.2f %.2f Td ", x, y))
                .append("(").append(escaped).append(") Tj ET\n");
    }

    private String pdfColor(Color c, boolean relleno) {
        String op = relleno ? "rg" : "RG";
        return String.format(Locale.US, "%.4f %.4f %.4f %s\n",
                c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, op);
    }

    // ----------------------------------------------------------------
    // estructura del documento PDF
    // ----------------------------------------------------------------

    private void escribirCabeceraPDF() {
        // los primeros 3 objetos son fijos en nuestra estructura:
        // 1 = catalogo, 2 = paginas, 3 = imagen logo (si hay)
        nObjetos = 2; // reservamos el 1 (catalogo) y el 2 (paginas) de antemano
    }

    private int reservarIdObjeto() {
        return ++nObjetos;
    }

    private byte[] construirObjetoImagen(int id, byte[] jpegBytes, int ancho, int alto) {
        String header = id + " 0 obj\n"
                + "<< /Type /XObject /Subtype /Image"
                + " /Width " + ancho + " /Height " + alto
                + " /ColorSpace /DeviceRGB /BitsPerComponent 8"
                + " /Filter /DCTDecode /Length " + jpegBytes.length + " >>\nstream\n";
        String footer = "\nendstream\nendobj\n";
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try {
            buf.write(header.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
            buf.write(jpegBytes);
            buf.write(footer.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        } catch (IOException ignored) {}
        return buf.toByteArray();
    }

    private void cerrarDocumento(File archivo) throws IOException {
        // escribir el PDF a un buffer temporal para calcular offsets correctos
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buf, false, "ISO-8859-1");

        // cabecera
        out.print("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");
        int offsetActual = buf.size();

        List<Integer> offsets = new ArrayList<>();

        // objeto 1: catalogo
        offsets.add(offsetActual);
        out.print("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        offsetActual = buf.size();

        // objeto 2: paginas (referencia a todas las paginas)
        offsets.add(offsetActual);
        StringBuilder kidsStr = new StringBuilder("[");
        for (int i = 0; i < paginasIds.size(); i++) {
            if (i > 0) kidsStr.append(" ");
            kidsStr.append(paginasIds.get(i)).append(" 0 R");
        }
        kidsStr.append("]");
        out.print("2 0 obj\n<< /Type /Pages /Kids " + kidsStr + " /Count " + paginasIds.size() + " >>\nendobj\n");
        offsetActual = buf.size();

        // resto de objetos (imagen, paginas, contenidos)
        for (byte[] obj : objetosPDF) {
            int id = extraerIdObjeto(obj);
            // nos aseguramos de que exista la posicion id-1 en la lista
            // (rellenando con 0 si hace falta) y luego SIEMPRE la
            // sobreescribimos con el offset real. antes, cuando el relleno
            // dejaba el tamaño de la lista igual al id, el codigo hacia un
            // "add" en vez de un "set", lo que insertaba un offset de mas
            // y desfasaba la correspondencia id->offset para todos los
            // objetos siguientes. eso era lo que dejaba corrupta la tabla
            // xref (y por lo tanto el PDF) en cualquier reporte con mas de
            // un objeto dinamico, que es siempre el caso (logo + contenido
            // + pagina como minimo).
            while (offsets.size() < id) offsets.add(0);
            offsets.set(id - 1, offsetActual);
            out.write(obj);
            offsetActual = buf.size();
        }

        int totalObjetos = nObjetos;
        int xrefOffset = offsetActual;

        // tabla de referencias cruzadas (xref)
        out.print("xref\n0 " + (totalObjetos + 1) + "\n");
        out.print("0000000000 65535 f \n");
        for (int i = 0; i < totalObjetos; i++) {
            int off = (i < offsets.size()) ? offsets.get(i) : 0;
            out.print(String.format(Locale.US, "%010d 00000 n \n", off));
        }

        // trailer
        out.print("trailer\n<< /Size " + (totalObjetos + 1) + " /Root 1 0 R >>\n");
        out.print("startxref\n" + xrefOffset + "\n%%EOF\n");
        out.flush();

        // guardar en disco
        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            fos.write(buf.toByteArray());
        }
        System.out.println("reporte PDF generado en: " + archivo.getAbsolutePath());
    }

    private int extraerIdObjeto(byte[] obj) {
        // el primer token antes de " 0 obj" es el id
        String s = new String(obj, 0, Math.min(20, obj.length), java.nio.charset.StandardCharsets.ISO_8859_1);
        try {
            return Integer.parseInt(s.trim().split("\\s+")[0]);
        } catch (Exception ex) {
            return nObjetos;
        }
    }
}
