package Modelo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class LoginDAO {

    Conexion conexion = new Conexion();
    DBCollection coleccion = conexion.coleccionUser;

    // Crear usuario fijo
    public void crearUsuarioInicial() {

        // Verificar si ya hay admin
        BasicDBObject filtro = new BasicDBObject("usuario", "admin");

        DBObject resultado = coleccion.findOne(filtro);

        // si no hay admin se crea
        if (resultado == null) {

            BasicDBObject documento = new BasicDBObject();

            documento.put("usuario", "admin");
            documento.put("contraseña", "admin1234");

            coleccion.insert(documento);

            System.out.println("Usuario admin creado");
        }
    }

    // verificar login
    public boolean iniciarSesion(String usuario, String contraseña) {

        BasicDBObject filtro = new BasicDBObject();

        filtro.put("usuario", usuario);
        filtro.put("contraseña", contraseña);

        DBObject resultado = coleccion.findOne(filtro);

        return resultado != null;
    }
}