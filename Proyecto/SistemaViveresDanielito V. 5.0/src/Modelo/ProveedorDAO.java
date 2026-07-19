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
        guardarSiNoExiste(nombre, null);
    }

    /**
     * igual que guardarSiNoExiste(nombre), pero permitiendo guardar de una
     * vez un numero de telefono opcional. el telefono NO es obligatorio:
     * si viene null o vacio simplemente no se guarda ese campo.
     */
    public void guardarSiNoExiste(String nombre, String telefono) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return;
        }
        String limpio = nombre.trim();
        if (existeProveedor(limpio)) {
            return;
        }
        BasicDBObject documento = new BasicDBObject("nombre", limpio);
        if (telefono != null && !telefono.trim().isEmpty()) {
            documento.put("telefono", telefono.trim());
        }
        coleccion.insert(documento);
    }

    /**
     * cambia (o agrega) el telefono de un proveedor que ya existe en el
     * catalogo. si se manda vacio o null, se borra el telefono guardado
     * (para poder quitarlo si ya no aplica).
     * @return true si se encontro y actualizo el proveedor.
     */
    public boolean actualizarTelefono(String nombre, String telefono) {
        if (nombre == null || nombre.trim().isEmpty()) return false;
        BasicDBObject filtro = new BasicDBObject("nombre",
                new BasicDBObject("$regex", "^" + Pattern.quote(nombre.trim()) + "$")
                        .append("$options", "i"));
        BasicDBObject cambio;
        if (telefono == null || telefono.trim().isEmpty()) {
            cambio = new BasicDBObject("$unset", new BasicDBObject("telefono", ""));
        } else {
            cambio = new BasicDBObject("$set", new BasicDBObject("telefono", telefono.trim()));
        }
        return coleccion.update(filtro, cambio).getN() > 0;
    }

    /**
     * trae el telefono guardado de un proveedor, o cadena vacia si no
     * tiene uno registrado.
     */
    public String obtenerTelefono(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return "";
        BasicDBObject filtro = new BasicDBObject("nombre",
                new BasicDBObject("$regex", "^" + Pattern.quote(nombre.trim()) + "$")
                        .append("$options", "i"));
        DBObject doc = coleccion.findOne(filtro);
        if (doc == null) return "";
        Object tel = doc.get("telefono");
        return tel == null ? "" : tel.toString();
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

    /**
     * cuenta cuantos productos tienen asignado este proveedor.
     * se usa para avisarle al usuario antes de borrar o renombrar
     * un proveedor que ya tiene productos asociados.
     */
    public int contarProductosConProveedor(String nombre, DBCollection colProd) {
        if (nombre == null || nombre.trim().isEmpty()) return 0;
        BasicDBObject filtro = new BasicDBObject("proveedor",
                new BasicDBObject("$regex", "^" + Pattern.quote(nombre.trim()) + "$")
                        .append("$options", "i"));
        return (int) colProd.count(filtro);
    }

    /**
     * cambia el nombre de un proveedor en el catalogo Y en todos los
     * productos que lo tenian asignado (para que queden consistentes).
     * @return true si se cambio al menos un documento en la coleccion proveedor.
     */
    public boolean modificarNombre(String nombreViejo, String nombreNuevo, DBCollection colProd) {
        if (nombreViejo == null || nombreNuevo == null
                || nombreViejo.trim().isEmpty() || nombreNuevo.trim().isEmpty()) {
            return false;
        }
        String limpio = nombreNuevo.trim();
        // actualizar en la coleccion de proveedores
        BasicDBObject filtroViejo = new BasicDBObject("nombre",
                new BasicDBObject("$regex", "^" + Pattern.quote(nombreViejo.trim()) + "$")
                        .append("$options", "i"));
        com.mongodb.WriteResult res = coleccion.update(filtroViejo,
                new BasicDBObject("$set", new BasicDBObject("nombre", limpio)));
        // actualizar en todos los productos que tenian el proveedor viejo
        if (colProd != null) {
            colProd.update(filtroViejo, new BasicDBObject("$set", new BasicDBObject("proveedor", limpio)), false, true);
        }
        return res.getN() > 0;
    }

    /**
     * elimina un proveedor del catalogo. no toca los productos que lo
     * tenian asignado (eso lo decide quien llama, despues de mostrar
     * la alerta correspondiente).
     * @return true si se elimino el documento.
     */
    public boolean eliminar(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return false;
        BasicDBObject filtro = new BasicDBObject("nombre",
                new BasicDBObject("$regex", "^" + Pattern.quote(nombre.trim()) + "$")
                        .append("$options", "i"));
        return coleccion.remove(filtro).getN() > 0;
    }

    /** cuantos proveedores hay en total en el catalogo */
    public long contarTotal() {
        return coleccion.count();
    }
}
