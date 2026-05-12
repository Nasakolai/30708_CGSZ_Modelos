
package Modelo;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;


public class Conexion {
    DB baseDatos;
    DBCollection coleccionProd;
    DBCollection coleccionProv;
    DBCollection coleccionUser;
    
    public Conexion(){
        //ayuda a unirnos a la base de datos
        MongoClient mongo= new MongoClient("localhost",27017);
        baseDatos= mongo.getDB("ViveresDanielito");
        coleccionProd=baseDatos.getCollection("Producto");
        coleccionProv=baseDatos.getCollection("Proveedor");
        coleccionUser=baseDatos.getCollection("Usarios");
        
    }
}
