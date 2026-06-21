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
