
package Modelo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;



public class ProductoDAO {

 Conexion conexion = new Conexion();
    DBCollection coleccion = conexion.coleccionProd;

    // Registrar producto
    public void añadirProducto(Producto e) {
        BasicDBObject documento = new BasicDBObject();
        documento.put("nombre", e.getNombre());
        documento.put("tipo", e.getTipo());
        documento.put("precio unitario", e.getPrecioUnit());
        documento.put("proveedor",e.getProveedor());
        coleccion.insert(documento);
        System.out.println("se mandó al mongo :D");
    }

    // Eliminar producto por nombre
    public void eliminarProducto(String nombre) {
        BasicDBObject filtro = new BasicDBObject("nombre", nombre);
        coleccion.remove(filtro);
    }
    
    
    
     public Producto buscarPorNombre(String nombre) {
        BasicDBObject query = new BasicDBObject();
        query.put("nombre", nombre);
//        
//
        DBObject obj = coleccion.findOne(query);
        System.out.println("Query producto: " + nombre);

System.out.println("Resultado producto: " + obj);

        if (obj != null) {
            Producto e = new Producto();
            e.setNombre((String) obj.get("nombre"));
            e.setPrecioUnit(((Number) obj.get("precio unitario")).doubleValue());
            System.out.println("PRODUCTO DEBUG — producto encontrado:");
System.out.println("PRODUCTO: Nombre: " + obj.get("nombre"));
System.out.println("PRODUCTO: Precio unitario: " + obj.get("precio unitario"));

            return e;
        }
        return null;
    }


    // Modificar prod existente por nombre
    public void modificarProducto(String nombre, Producto nueva) {
        BasicDBObject filtro = new BasicDBObject("nombre", nombre);
        BasicDBObject nuevosDatos = new BasicDBObject();
        nuevosDatos.put("nombre", nueva.getNombre());
        nuevosDatos.put("tipo", nueva.getTipo());
        nuevosDatos.put("precio unitario", nueva.getPrecioUnit());
        nuevosDatos.put("proveedor", nueva.getProveedor());
        BasicDBObject actualizacion = new BasicDBObject("$set", nuevosDatos);
        coleccion.update(filtro, actualizacion);
    }

    // Listar todo producto de la base MongoDB
    public ArrayList<Producto> listarProductos() {
        ArrayList<Producto> especies = new ArrayList<>();
        DBCursor cursor = coleccion.find();
        while (cursor.hasNext()) {
            DBObject doc = cursor.next();
            Producto e = new Producto();
            e.setNombre((String) doc.get("nombre"));
            e.setTipo((String) doc.get("tipo"));
            e.setPrecioUnit((double) doc.get("precio unitario"));
            especies.add(e);
        }
        return especies;
    }

    // Verificar si un producto ya existe
    public boolean productoExiste(String nombre) {
        BasicDBObject filtro = new BasicDBObject("nombre", nombre);
        DBObject resultado = coleccion.findOne(filtro);
        return resultado != null;
    }

    // Buscar productos por nombre (segun prefijo)
public ArrayList<Producto> buscarEspecie(String prefijo) {
    ArrayList<Producto> coincidencias = new ArrayList<>();
    
    // Usamos una expresión regular para buscar por prefijo, sin importar mayúsculas
    BasicDBObject filtro = new BasicDBObject("nombre", 
        new BasicDBObject("$regex", "^" + prefijo).append("$options", "i") // "i" = case-insensitive
    );

    DBCursor cursor = coleccion.find(filtro);

    while (cursor.hasNext()) {
        DBObject doc = cursor.next();
        Producto e = new Producto();
        e.setNombre((String) doc.get("nombre"));
        e.setTipo((String) doc.get("tipo"));
        e.setPrecioUnit((double) doc.get("precio unitario"));
        coincidencias.add(e);
    }

    return coincidencias;
}

    
    

    // Mostrar la tabla (retorna el modelo ya listo)
    public DefaultTableModel mostrarTabla(ArrayList<Producto> listaEspecies) {
        DefaultTableModel modelo = new DefaultTableModel();
        modelo.addColumn("Nombre");
        modelo.addColumn("Tamaño");
        modelo.addColumn("Precio/Kg");
        modelo.addColumn("Dureza");

        for (Producto e : listaEspecies) {
            Object[] fila = {
                e.getNombre(),
                e.getTipo(),
                e.getPrecioUnit(),
            };
            modelo.addRow(fila);
        }
        return modelo;
    }


    // Migrar desde archivo txt a Mongo
    public ArrayList<Producto> migrarLista() {
        Archivo ar = new Archivo();
        ArrayList<Producto> datosMigrados = ar.leerDesdeArchivo("C:\\Users\\HP\\Desktop\\migrar.txt");
        return datosMigrados;
    }
    
    public ArrayList<String> obtenerNombresProductos() {
    ArrayList<String> nombres = new ArrayList<>();
    DBCursor cursor = coleccion.find();

    while (cursor.hasNext()) {
        DBObject doc = cursor.next();
        nombres.add((String) doc.get("nombre"));
    }

    return nombres;
}


public void generarReporteProductos() {
    ArrayList<Producto> especies = listarProductos();
    String rutaArchivo = "C:\\Users\\Personal\\Documents\\CamaroneraReportes\\reporte_especies.txt";

    try (BufferedWriter bw = new BufferedWriter(new FileWriter(rutaArchivo))) {
        bw.write("Reporte de Especies\n");
        bw.write("==================\n\n");

        for (Producto e : especies) {
            bw.write("Nombre: " + e.getNombre() + "\n");
            bw.write("Tipo: " + e.getTipo() + "\n");
            bw.write("Precio/u: " + e.getPrecioUnit() + "\n");
            bw.write("------------------------\n");
        }

        System.out.println("Reporte TXT generado correctamente en: " + rutaArchivo);
    } catch (IOException ex) {
        ex.printStackTrace();
    }
}  
}
