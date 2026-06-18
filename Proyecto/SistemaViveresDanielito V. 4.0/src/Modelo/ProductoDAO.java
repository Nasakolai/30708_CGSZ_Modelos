
package Modelo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * DAO de productos: aqui se guarda, busca, modifica y borra todo lo que
 * tenga que ver con productos en la coleccion "Producto" de mongo.
 */
public class ProductoDAO {

    Conexion conexion = new Conexion();
    DBCollection coleccion = conexion.coleccionProd;

    /**
     * genera el siguiente codigo disponible para un tipo de producto, por
     * ejemplo "LT004" si ya existen LT001, LT002 y LT003. si el tipo no
     * coincide con ninguno de los conocidos (o viene null) se usa el
     * prefijo genérico "OT" (otros), asi nunca se cae por un tipo raro.
     */
    public String generarCodigoProducto(String tipo) {

        String prefijo;
        String tipoNormalizado = (tipo == null) ? "" : tipo.toLowerCase();

        switch (tipoNormalizado) {

            case "lacteos":
            case "lácteos":
                prefijo = "LT";
                break;

            case "limpieza":
                prefijo = "LP";
                break;

            case "snacks":
                prefijo = "SN";
                break;

            case "embutidos":
                prefijo = "EM";
                break;

            case "carnes":
                prefijo = "CR";
                break;

            case "condimentos":
                prefijo = "CD";
                break;

            default:
                prefijo = "OT";
        }

        int mayor = 0;
        DBCursor cursor = coleccion.find();

        while (cursor.hasNext()) {
            DBObject doc = cursor.next();
            Object codigoObj = doc.get("codigo");
            if (codigoObj != null) {
                String codigo = codigoObj.toString();
                if (codigo.startsWith(prefijo)) {
                    try {
                        int numero = Integer.parseInt(codigo.substring(prefijo.length()));
                        if (numero > mayor) {
                            mayor = numero;
                        }
                    } catch (NumberFormatException e) {
                        // el codigo no terminaba en numero, lo ignoramos y seguimos
                    }
                }
            }
        }

        return String.format("%s%03d", prefijo, mayor + 1);
    }

    /**
     * guarda un producto nuevo. se asume que quien llama (el controlador)
     * ya valido los datos, pero igual se revisa que no llegue null para
     * no mandar basura a mongo.
     */
    public void añadirProducto(Producto e) {
        if (e == null) {
            return;
        }
        BasicDBObject documento = new BasicDBObject();
        documento.put("nombre", e.getNombre());
        documento.put("tipo", e.getTipo());
        documento.put("precio unitario", e.getPrecioUnit());
        documento.put("proveedor", e.getProveedor());
        documento.put("stock", e.getStock());
        documento.put("codigo", e.getCodigo());
        coleccion.insert(documento);
    }

    /**
     * borra el producto que tenga ese nombre exacto. si el nombre viene
     * vacio no hacemos nada (para no borrar "todo" por accidente, ya que
     * un filtro con nombre="" podria llegar a coincidir con algo raro).
     */
    public void eliminarProducto(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return;
        }
        BasicDBObject filtro = new BasicDBObject("nombre", nombre);
        coleccion.remove(filtro);
    }

    public Producto buscarPorNombre(String nombre) {
        if (nombre == null) {
            return null;
        }
        BasicDBObject query = new BasicDBObject("nombre", nombre);
        DBObject obj = coleccion.findOne(query);

        if (obj == null) {
            return null;
        }

        Producto e = new Producto();
        e.setNombre((String) obj.get("nombre"));
        e.setTipo((String) obj.get("tipo"));
        e.setPrecioUnit(numeroSeguro(obj.get("precio unitario")));
        e.setProveedor((String) obj.get("proveedor"));
        e.setCodigo((String) obj.get("codigo"));
        Object stockObj = obj.get("stock");
        e.setStock(stockObj instanceof Number ? ((Number) stockObj).intValue() : 0);
        return e;
    }

    /**
     * busca un producto por su codigo unico (por ejemplo "LT004"). se usa
     * sobre todo al editar/eliminar un movimiento, donde lo unico que se
     * tiene guardado de forma confiable es el codigo del producto (el
     * nombre podria haber cambiado despues de registrar el movimiento).
     */
    public Producto buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return null;
        }
        BasicDBObject query = new BasicDBObject("codigo", codigo.trim());
        DBObject obj = coleccion.findOne(query);

        if (obj == null) {
            return null;
        }

        Producto e = new Producto();
        e.setNombre((String) obj.get("nombre"));
        e.setTipo((String) obj.get("tipo"));
        e.setPrecioUnit(numeroSeguro(obj.get("precio unitario")));
        e.setProveedor((String) obj.get("proveedor"));
        e.setCodigo((String) obj.get("codigo"));
        Object stockObj = obj.get("stock");
        e.setStock(stockObj instanceof Number ? ((Number) stockObj).intValue() : 0);
        return e;
    }

    /**
     * cambia los datos de un producto buscandolo por su nombre original.
     * @param nombre nombre con el que esta guardado actualmente en mongo
     * @param nueva los datos nuevos que va a tener
     */
    public void modificarProducto(String nombre, Producto nueva) {
        if (nombre == null || nueva == null) {
            return;
        }
        BasicDBObject filtro = new BasicDBObject("nombre", nombre);
        BasicDBObject nuevosDatos = new BasicDBObject();
        nuevosDatos.put("nombre", nueva.getNombre());
        nuevosDatos.put("tipo", nueva.getTipo());
        nuevosDatos.put("precio unitario", nueva.getPrecioUnit());
        nuevosDatos.put("proveedor", nueva.getProveedor());
        nuevosDatos.put("stock", nueva.getStock());
        nuevosDatos.put("codigo", nueva.getCodigo());
        BasicDBObject actualizacion = new BasicDBObject("$set", nuevosDatos);
        WriteResult res = coleccion.update(filtro, actualizacion);
        if (res.getN() == 0) {
            // no se encontro ningun documento con ese nombre para actualizar,
            // dejamos la pista en consola por si alguien esta debugueando
            System.out.println("aviso: no se encontro ningun producto llamado '" + nombre + "' para modificar");
        }
    }

    // trae todos los productos guardados en mongo
    public ArrayList<Producto> listarProductos() {
        ArrayList<Producto> productos = new ArrayList<>();
        DBCursor cursor = coleccion.find();
        while (cursor.hasNext()) {
            DBObject doc = cursor.next();
            Producto e = new Producto();
            e.setNombre((String) doc.get("nombre"));
            e.setTipo((String) doc.get("tipo"));
            e.setPrecioUnit(numeroSeguro(doc.get("precio unitario")));
            e.setProveedor((String) doc.get("proveedor"));
            e.setCodigo((String) doc.get("codigo"));
            Object stockObj = doc.get("stock");
            e.setStock(stockObj instanceof Number ? ((Number) stockObj).intValue() : 0);
            productos.add(e);
        }
        return productos;
    }

    /**
     * dice si ya existe un producto con ese nombre (sin importar mayusculas
     * o minusculas). usa una consulta directa con regex en vez de traer
     * toda la coleccion al programa y comparar uno por uno, que era lo que
     * se hacia antes y de paso se caia si algun producto no tenia nombre guardado.
     */
    public boolean existeProducto(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return false;
        }
        String nombreEscapado = Pattern.quote(nombre.trim());
        BasicDBObject filtro = new BasicDBObject("nombre",
                new BasicDBObject("$regex", "^" + nombreEscapado + "$").append("$options", "i"));
        return coleccion.findOne(filtro) != null;
    }

    /**
     * busca productos cuyo nombre empiece por el texto recibido (sin
     * distinguir mayusculas/minusculas). el texto se escapa antes de
     * meterlo en la expresion regular para que caracteres especiales como
     * ".", "*", "(" o "[" no rompan la busqueda ni boten una excepcion.
     */
    public ArrayList<Producto> buscarEspecie(String prefijo) {
        ArrayList<Producto> coincidencias = new ArrayList<>();
        if (prefijo == null) {
            return coincidencias;
        }

        String prefijoEscapado = Pattern.quote(prefijo);
        BasicDBObject filtro = new BasicDBObject("nombre",
                new BasicDBObject("$regex", "^" + prefijoEscapado).append("$options", "i"));

        DBCursor cursor = coleccion.find(filtro);

        while (cursor.hasNext()) {
            DBObject doc = cursor.next();
            Producto e = new Producto();
            e.setNombre((String) doc.get("nombre"));
            e.setTipo((String) doc.get("tipo"));
            e.setPrecioUnit(numeroSeguro(doc.get("precio unitario")));
            e.setProveedor((String) doc.get("proveedor"));
            e.setCodigo((String) doc.get("codigo"));
            Object stockObj = doc.get("stock");
            e.setStock(stockObj instanceof Number ? ((Number) stockObj).intValue() : 0);
            coincidencias.add(e);
        }

        return coincidencias;
    }

    public ArrayList<String> obtenerNombresProductos() {
        ArrayList<String> nombres = new ArrayList<>();
        DBCursor cursor = coleccion.find();

        while (cursor.hasNext()) {
            DBObject doc = cursor.next();
            Object nombre = doc.get("nombre");
            if (nombre != null) {
                nombres.add(nombre.toString());
            }
        }

        return nombres;
    }

    /**
     * pequeña ayuda para no repetir el mismo "if instanceof Number" en
     * cada metodo. si el valor no es un numero (o no existe) regresa 0.0
     * en vez de tronar con un NullPointerException.
     */
    private double numeroSeguro(Object valor) {
        return (valor instanceof Number) ? ((Number) valor).doubleValue() : 0.0;
    }
}
