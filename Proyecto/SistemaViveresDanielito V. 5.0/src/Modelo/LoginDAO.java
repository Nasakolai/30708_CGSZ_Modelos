package Modelo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * encargado de crear el usuario inicial y validar el inicio de sesion.
 *
 * antes la contraseña se guardaba TAL CUAL en mongo (texto plano), lo cual
 * es una mala practica de seguridad bien conocida: cualquiera que vea la
 * base de datos (o un respaldo, o un log) ve la clave de una. ahora se
 * guarda un hash SHA-256, que no se puede "deshacer" para recuperar la
 * clave original.
 *
 * para no romper instalaciones viejas que ya tengan el usuario admin
 * guardado con la clave en texto plano, iniciarSesion() tambien acepta esa
 * forma vieja UNA vez y de paso actualiza el registro al nuevo formato con
 * hash, asi la migracion es transparente para quien ya tenia el sistema instalado.
 */
public class LoginDAO {

    Conexion conexion = new Conexion();
    DBCollection coleccion = conexion.coleccionUser;

    // crea el usuario admin por defecto si todavia no existe ninguno
    public void crearUsuarioInicial() {

        BasicDBObject filtro = new BasicDBObject("usuario", "admin");
        DBObject resultado = coleccion.findOne(filtro);

        if (resultado == null) {
            BasicDBObject documento = new BasicDBObject();
            documento.put("usuario", "admin");
            documento.put("contraseña", hashSha256("admin1234"));
            coleccion.insert(documento);
            System.out.println("se creo el usuario admin con clave por defecto 'admin1234' (recuerden cambiarla)");
        }
    }

    /**
     * revisa que el usuario y la contraseña sean correctos.
     * @return true si las credenciales son validas, false si no.
     */
    public boolean iniciarSesion(String usuario, String contraseña) {
        if (usuario == null || contraseña == null) {
            return false;
        }

        String hashIngresado = hashSha256(contraseña);

        // primero intentamos con el formato nuevo (hash)
        BasicDBObject filtroHash = new BasicDBObject();
        filtroHash.put("usuario", usuario);
        filtroHash.put("contraseña", hashIngresado);
        if (coleccion.findOne(filtroHash) != null) {
            return true;
        }

        // si no hubo suerte, probamos el formato viejo (texto plano) por si
        // el usuario todavia no se ha "migrado". si coincide, lo actualizamos
        // a hash de una vez para que la proxima vez ya entre por el camino normal.
        BasicDBObject filtroPlano = new BasicDBObject();
        filtroPlano.put("usuario", usuario);
        filtroPlano.put("contraseña", contraseña);
        DBObject usuarioPlano = coleccion.findOne(filtroPlano);
        if (usuarioPlano != null) {
            BasicDBObject actualizacion = new BasicDBObject("$set",
                    new BasicDBObject("contraseña", hashIngresado));
            coleccion.update(filtroPlano, actualizacion);
            return true;
        }

        return false;
    }

    /**
     * convierte un texto en su hash SHA-256 (en hexadecimal). SHA-256
     * viene incluido en el propio Java, no hace falta ninguna libreria
     * externa para esto.
     */
    private String hashSha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytesHash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytesHash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 siempre deberia estar disponible en cualquier JVM moderna,
            // pero por si las dudas no dejamos que esto tumbe el programa
            throw new RuntimeException("no se pudo calcular el hash de la contraseña", ex);
        }
    }
}
