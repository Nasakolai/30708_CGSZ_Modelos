
package Modelo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JTable;


public class Archivo {
    
    
// Leer cuentas desde un archivo .txt y devolverlas en una lista
    public ArrayList<Producto> leerDesdeArchivo(String ruta) {
        ArrayList<Producto> productos = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                // Separar los campos por coma
                String[] partes = linea.split(",");

                // Validar formato
                if (partes.length >= 3) {
                    String nombre = partes[0].trim();
                    String tipo = partes[1].trim();
                    //obtener el precio y transformar a string
                    String precioUnitS = partes[2].trim();
                    double precioUnit = Double.parseDouble(precioUnitS);
                    //no se si este bien el formato
                    String proveedor = partes[3].trim();

                    Producto producto = new Producto(nombre, tipo, precioUnit, proveedor);
                    productos.add(producto);
                } else {
                    System.out.println("Línea inválida: " + linea);
                }
            }
        } catch (IOException e) {
            System.out.println("Error al leer archivo: " + e.getMessage());
        }

        return productos;
    }


    public void exportarTabla(JTable tabla, String ruta) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ruta))) {
            for (int i = 0; i < tabla.getRowCount(); i++) {
                StringBuilder fila = new StringBuilder();
                for (int j = 0; j < tabla.getColumnCount(); j++) {
                    fila.append(tabla.getValueAt(i, j));
                    if (j < tabla.getColumnCount() - 1) fila.append(","); // separador
                }
                bw.write(fila.toString());
                bw.newLine();
            }
            System.out.println("exportado");
        } catch (IOException e) {
            System.out.println("error al exportar");
        }
    }
    
}
