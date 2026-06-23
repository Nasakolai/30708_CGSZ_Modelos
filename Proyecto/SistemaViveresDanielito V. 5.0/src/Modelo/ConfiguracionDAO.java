package Modelo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * guarda ajustes chiquitos y globales del sistema en un solo documento por
 * clave (patron "clave/valor"). por ahora solo se usa para el nombre del
 * "responsable" que sale por defecto en Reportes y que se graba en cada
 * movimiento de inventario, pero sirve para cualquier otro ajuste simple
 * que se necesite mas adelante sin tener que crear una coleccion nueva.
 */
public class ConfiguracionDAO {

    private static final String CLAVE_RESPONSABLE = "responsablePorDefecto";
    private static final String RESPONSABLE_INICIAL = "Enrique Guaiguacundo";

    Conexion conexion = new Conexion();
    DBCollection coleccion = conexion.getColeccionConfiguracion();

    /**
     * nombre que se debe usar como "responsable"/"generado por" mientras
     * nadie lo haya cambiado nunca. si ya se guardo un valor antes, ese es
     * el que se devuelve siempre (persiste entre reinicios del programa).
     */
    public String obtenerResponsable() {
        DBObject doc = coleccion.findOne(new BasicDBObject("clave", CLAVE_RESPONSABLE));
        if (doc == null) {
            return RESPONSABLE_INICIAL;
        }
        Object valor = doc.get("valor");
        if (valor == null || valor.toString().trim().isEmpty()) {
            return RESPONSABLE_INICIAL;
        }
        return valor.toString();
    }

    /**
     * cambia el responsable por defecto. de ahi en adelante, hasta que se
     * vuelva a cambiar, este es el nombre que aparece en los reportes y el
     * que se graba en cada movimiento nuevo que se registre.
     */
    public void guardarResponsable(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return;
        }
        BasicDBObject filtro = new BasicDBObject("clave", CLAVE_RESPONSABLE);
        BasicDBObject cambio = new BasicDBObject("$set",
                new BasicDBObject("clave", CLAVE_RESPONSABLE).append("valor", nombre.trim()));
        coleccion.update(filtro, cambio, true, false); // upsert=true: lo crea si todavia no existe
    }
}
