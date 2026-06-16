package Modelo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import java.util.ArrayList;

public class MovimientoDAO {
    Conexion conexion = new Conexion();
    DBCollection coleccion = conexion.coleccionMov;

    public void añadirMovimiento(Movimiento m) {
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
    }

    public ArrayList<Movimiento> listarMovimientos(BasicDBObject filtro) {
        ArrayList<Movimiento> lista = new ArrayList<>();
        DBCursor cursor = coleccion.find(filtro == null ? new BasicDBObject() : filtro);
        while (cursor.hasNext()) {
            DBObject d = cursor.next();
            Movimiento m = new Movimiento();
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
}
