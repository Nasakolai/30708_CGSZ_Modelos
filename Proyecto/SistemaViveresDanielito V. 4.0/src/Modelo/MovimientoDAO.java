package Modelo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.WriteResult;
import java.util.ArrayList;
import org.bson.types.ObjectId;

/**
 * encargado de guardar, listar y borrar los movimientos (entradas/salidas)
 * de inventario en la coleccion "Movimientos" de mongo.
 */
public class MovimientoDAO {
    Conexion conexion = new Conexion();
    DBCollection coleccion = conexion.coleccionMov;

    /**
     * guarda un movimiento nuevo. si llega null no hace nada, asi no se
     * cae el programa por un descuido de quien use este metodo.
     */
    public void añadirMovimiento(Movimiento m) {
        if (m == null) {
            return;
        }
        BasicDBObject doc = new BasicDBObject();
        doc.put("tipo", m.getTipo());
        doc.put("cantidad", m.getCantidad());
        doc.put("precioUnitario", m.getPrecioUnitario());
        doc.put("fecha", m.getFecha());
        doc.put("total", m.getTotal());
        doc.put("usuario", m.getUsuario());
        doc.put("nombreProducto", m.getNombreProducto());
        doc.put("codigoProducto", m.getCodigoProducto());
        coleccion.insert(doc);
        // mongo le pone el _id al mismo objeto "doc" despues de insertar,
        // por si a alguien le sirve tener el id de una vez en el objeto que mandaron
        Object idGenerado = doc.get("_id");
        if (idGenerado != null) {
            m.setId(idGenerado.toString());
        }
    }

    /**
     * trae todos los movimientos que cumplan el filtro (si el filtro es
     * null, trae todos). vienen ordenados del mas nuevo al mas viejo segun
     * el orden natural de inserccion de mongo (_id), para que la tabla de
     * "ver movimientos" muestre primero lo mas reciente.
     */
    public ArrayList<Movimiento> listarMovimientos(BasicDBObject filtro) {
        ArrayList<Movimiento> lista = new ArrayList<>();
        DBCursor cursor = coleccion.find(filtro == null ? new BasicDBObject() : filtro);
        cursor.sort(new BasicDBObject("_id", -1));
        while (cursor.hasNext()) {
            DBObject d = cursor.next();
            Movimiento m = new Movimiento();
            Object id = d.get("_id");
            if (id != null) {
                m.setId(id.toString());
            }
            m.setTipo((String) d.get("tipo"));
            Object cant = d.get("cantidad");
            if (cant instanceof Number) m.setCantidad(((Number) cant).intValue());
            Object pu = d.get("precioUnitario");
            if (pu instanceof Number) m.setPrecioUnitario(((Number) pu).doubleValue());
            m.setFecha((String) d.get("fecha"));
            Object tot = d.get("total");
            if (tot instanceof Number) m.setTotal(((Number) tot).doubleValue());
            m.setUsuario((String) d.get("usuario"));
            m.setNombreProducto((String) d.get("nombreProducto"));
            m.setCodigoProducto((String) d.get("codigoProducto"));
            lista.add(m);
        }
        return lista;
    }

    /**
     * trae todos los movimientos sin filtrar nada, es solo un atajo para
     * no tener que escribir listarMovimientos(null) por todos lados.
     */
    public ArrayList<Movimiento> listarMovimientos() {
        return listarMovimientos(null);
    }

    /**
     * busca un movimiento puntual por su id de mongo. regresa null si el id
     * no tiene formato valido o si no se encontro ningun movimiento con ese
     * id (por ejemplo si alguien mas ya lo habia borrado).
     */
    public Movimiento buscarPorId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        try {
            ObjectId objectId = new ObjectId(id.trim());
            DBObject d = coleccion.findOne(new BasicDBObject("_id", objectId));
            if (d == null) {
                return null;
            }
            Movimiento m = new Movimiento();
            m.setId(d.get("_id").toString());
            m.setTipo((String) d.get("tipo"));
            Object cant = d.get("cantidad");
            if (cant instanceof Number) m.setCantidad(((Number) cant).intValue());
            Object pu = d.get("precioUnitario");
            if (pu instanceof Number) m.setPrecioUnitario(((Number) pu).doubleValue());
            m.setFecha((String) d.get("fecha"));
            Object tot = d.get("total");
            if (tot instanceof Number) m.setTotal(((Number) tot).doubleValue());
            m.setUsuario((String) d.get("usuario"));
            m.setNombreProducto((String) d.get("nombreProducto"));
            m.setCodigoProducto((String) d.get("codigoProducto"));
            return m;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * actualiza un movimiento ya existente (se identifica por su id, el
     * producto al que pertenece no cambia). esto es lo que permite corregir
     * una cantidad mal tecleada sin tener que borrar y crear de nuevo el
     * movimiento. regresa true si de verdad se actualizo algo.
     */
    public boolean actualizarMovimiento(Movimiento m) {
        if (m == null || m.getId() == null || m.getId().trim().isEmpty()) {
            return false;
        }
        try {
            ObjectId objectId = new ObjectId(m.getId().trim());
            BasicDBObject filtro = new BasicDBObject("_id", objectId);
            BasicDBObject nuevosDatos = new BasicDBObject();
            nuevosDatos.put("tipo", m.getTipo());
            nuevosDatos.put("cantidad", m.getCantidad());
            nuevosDatos.put("precioUnitario", m.getPrecioUnitario());
            nuevosDatos.put("fecha", m.getFecha());
            nuevosDatos.put("total", m.getTotal());
            nuevosDatos.put("usuario", m.getUsuario());
            nuevosDatos.put("nombreProducto", m.getNombreProducto());
            nuevosDatos.put("codigoProducto", m.getCodigoProducto());
            BasicDBObject actualizacion = new BasicDBObject("$set", nuevosDatos);
            WriteResult resultado = coleccion.update(filtro, actualizacion);
            return resultado.getN() > 0;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * borra el movimiento con el id indicado. regresa true si de verdad se
     * borro algo y false si no (id invalido, no existe, etc), asi la
     * pantalla puede avisarle al usuario en vez de asumir que siempre sale bien.
     */
    public boolean eliminarMovimiento(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        try {
            ObjectId objectId = new ObjectId(id.trim());
            BasicDBObject filtro = new BasicDBObject("_id", objectId);
            WriteResult resultado = coleccion.remove(filtro);
            return resultado.getN() > 0;
        } catch (IllegalArgumentException ex) {
            // esto pasa si el id que llego no tiene el formato de un ObjectId de mongo
            return false;
        }
    }
}
