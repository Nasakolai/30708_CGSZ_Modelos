package Modelo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * DAO encargado del catalogo de proveedores (coleccion "Proveedor" en
 * mongo). la idea de tener esto aparte de Producto es que el proveedor ya
 * no se escriba como texto libre en cada producto (lo que antes permitia
 * que "Distribuidora XYZ" quedara guardado de formas distintas en
 * productos diferentes), sino que se elija de una lista unica.
 */
public class ProveedorDAO {

    Conexion conexion = new Conexion();
    DBCollection coleccion = conexion.coleccionProveedor;

    /**
     * dice si ya existe un proveedor con ese nombre (sin importar
     * mayusculas/minusculas ni espacios de mas al inicio/final).
     */
    public boolean existeProveedor(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return false;
        }
        String nombreEscapado = Pattern.quote(nombre.trim());
        BasicDBObject filtro = new BasicDBObject("nombre",
                new BasicDBObject("$regex", "^" + nombreEscapado + "$").append("$options", "i"));
        return coleccion.findOne(filtro) != null;
    }

    /**
     * guarda el proveedor en el catalogo si todavia no existe (comparando
     * sin distinguir mayusculas/minusculas). si ya existe no hace nada,
     * asi nunca se duplica el mismo proveedor con distinta escritura.
     */
    public void guardarSiNoExiste(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return;
        }
        String limpio = nombre.trim();
        if (existeProveedor(limpio)) {
            return;
        }
        BasicDBObject documento = new BasicDBObject("nombre", limpio);
        coleccion.insert(documento);
    }

    /**
     * trae los nombres de todos los proveedores guardados en el catalogo,
     * ordenados alfabeticamente (ignorando mayusculas/minusculas), para
     * usarlos en el combo con autocompletado.
     */
    public ArrayList<String> obtenerNombresProveedores() {
        ArrayList<String> nombres = new ArrayList<>();
        DBCursor cursor = coleccion.find();
        while (cursor.hasNext()) {
            DBObject doc = cursor.next();
            Object nombre = doc.get("nombre");
            if (nombre != null) {
                nombres.add(nombre.toString());
            }
        }
        nombres.sort(String.CASE_INSENSITIVE_ORDER);
        return nombres;
    }
}
