package Modelo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.Collections;
import com.mongodb.DBCollection;
import java.util.regex.Pattern;
import java.util.regex.Pattern;

/**
 * maneja el catalogo de categorias (tipos de producto) en mongo.
 * funciona igual que ProveedorDAO: si se agrega una categoria nueva
 * desde el formulario se guarda en la coleccion "Categoria" y ya
 * aparece en la lista para los demas productos.
 *
 * las categorias que vienen de siempre (Lacteos, Limpieza, etc.) se
 * crean automaticamente la primera vez que alguien abra el programa y
 * la coleccion este vacia, para que el combo no aparezca en blanco.
 */
public class CategoriaDAO {

    Conexion conexion = new Conexion();
    DBCollection coleccion = conexion.coleccionCategoria;

    // estas son las que se meten por defecto si la base de datos es nueva
    private static final String[] CATEGORIAS_DEFECTO = {
        "Lacteos", "Limpieza", "Snacks", "Embutidos", "Carnes", "Condimentos", "Otros"
    };

    /**
     * si la coleccion de categorias esta vacia, mete las de siempre.
     * si ya tiene datos no toca nada.
     */
    public void inicializarSiVacia() {
        if (coleccion.count() > 0) {
            return;
        }
        for (String nombre : CATEGORIAS_DEFECTO) {
            coleccion.insert(new BasicDBObject("nombre", nombre));
        }
    }

    /**
     * dice si ya existe una categoria con ese nombre sin importar mayusculas.
     */
    public boolean existe(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return false;
        }
        String escapado = Pattern.quote(nombre.trim());
        BasicDBObject filtro = new BasicDBObject("nombre",
                new BasicDBObject("$regex", "^" + escapado + "$").append("$options", "i"));
        return coleccion.findOne(filtro) != null;
    }

    /**
     * agrega una categoria nueva. si ya existe una con el mismo nombre
     * (ignorando mayusculas) no hace nada y devuelve false.
     *
     * @return true si se agrego, false si ya existia
     */
    public boolean agregar(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return false;
        }
        String limpio = nombre.trim().replaceAll("\\s+", " ");
        if (existe(limpio)) {
            return false;
        }
        coleccion.insert(new BasicDBObject("nombre", limpio));
        return true;
    }

    /**
     * devuelve todos los nombres de categorias en orden alfabetico.
     * esta lista es la que llena el combo de "tipo de producto".
     */
    public ArrayList<String> listar() {
        ArrayList<String> lista = new ArrayList<>();
        DBCursor cursor = coleccion.find();
        while (cursor.hasNext()) {
            Object nombre = cursor.next().get("nombre");
            if (nombre != null && !nombre.toString().trim().isEmpty()) {
                lista.add(nombre.toString());
            }
        }
        Collections.sort(lista, String.CASE_INSENSITIVE_ORDER);
        return lista;
    }
    /**
     * cuenta cuantos productos estan usando esta categoria como tipo.
     * se usa antes de borrar o renombrar para avisarle al usuario.
     */
    public int contarProductosConCategoria(String nombre, DBCollection colProd) {
        if (nombre == null || nombre.trim().isEmpty() || colProd == null) return 0;
        BasicDBObject filtro = new BasicDBObject("tipo",
                new BasicDBObject("$regex", "^" + Pattern.quote(nombre.trim()) + "$")
                        .append("$options", "i"));
        return (int) colProd.count(filtro);
    }

    /**
     * renombra la categoria Y actualiza el campo "tipo" de todos los
     * productos que la tenian para que queden consistentes.
     */
    public boolean modificarNombre(String nombreViejo, String nombreNuevo, DBCollection colProd) {
        if (nombreViejo == null || nombreNuevo == null
                || nombreViejo.trim().isEmpty() || nombreNuevo.trim().isEmpty()) {
            return false;
        }
        String limpio = nombreNuevo.trim().replaceAll("\\s+", " ");
        BasicDBObject filtroViejo = new BasicDBObject("nombre",
                new BasicDBObject("$regex", "^" + Pattern.quote(nombreViejo.trim()) + "$")
                        .append("$options", "i"));
        com.mongodb.WriteResult res = coleccion.update(filtroViejo,
                new BasicDBObject("$set", new BasicDBObject("nombre", limpio)));
        // actualizar el campo "tipo" en los productos
        if (colProd != null) {
            BasicDBObject filtroTipo = new BasicDBObject("tipo",
                    new BasicDBObject("$regex", "^" + Pattern.quote(nombreViejo.trim()) + "$")
                            .append("$options", "i"));
            colProd.update(filtroTipo, new BasicDBObject("$set", new BasicDBObject("tipo", limpio)), false, true);
        }
        return res.getN() > 0;
    }

    /**
     * borra la categoria del catalogo. no afecta los productos que la
     * tenian asignada (eso lo decide quien llama).
     */
    public boolean eliminar(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return false;
        BasicDBObject filtro = new BasicDBObject("nombre",
                new BasicDBObject("$regex", "^" + Pattern.quote(nombre.trim()) + "$")
                        .append("$options", "i"));
        return coleccion.remove(filtro).getN() > 0;
    }

    /** cuantas categorias hay en total */
    public long contarTotal() {
        return coleccion.count();
    }

}
