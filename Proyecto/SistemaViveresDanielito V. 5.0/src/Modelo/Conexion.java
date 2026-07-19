
package Modelo;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

/**
 * esta clase es la unica encargada de hablar con mongo, todas las demas
 * clases DAO crean un objeto de esta para sacar las colecciones que necesitan.
 *
 * ojo: si mongo no esta corriendo en la maquina esto puede tardar un rato en
 * fallar (el driver intenta reconectar varias veces antes de avisar), por
 * eso agregamos el metodo hayConexion() para probar rapido al iniciar el
 * programa y no dejar que el usuario se quede con la pantalla pegada sin
 * saber que paso.
 */
public class Conexion {

    // host y puerto por defecto de un mongo local, si el de ustedes esta en
    // otro lado solo toca cambiar estas dos constantes
    private static final String HOST = "localhost";
    private static final int PUERTO = 27017;
    private static final String NOMBRE_BD = "ViveresDanielito";

    DB baseDatos;
    DBCollection coleccionProd;
    DBCollection coleccionUser;
    DBCollection coleccionMov;
    DBCollection coleccionProveedor;
    DBCollection coleccionCategoria;
    DBCollection coleccionConfiguracion;

    public Conexion() {
        // esto nos une a la base de datos de mongo
        MongoClient mongo = new MongoClient(HOST, PUERTO);
        baseDatos = mongo.getDB(NOMBRE_BD);
        coleccionProd = baseDatos.getCollection("Producto");
        coleccionUser = baseDatos.getCollection("Usuarios");
        coleccionMov = baseDatos.getCollection("Movimientos");
        // catalogo de proveedores: antes el proveedor era solo un texto
        // libre guardado dentro de cada producto, lo que permitia que el
        // mismo proveedor quedara escrito de varias formas distintas
        // (mayusculas, espacios, etc). ahora se guarda ademas en su propia
        // coleccion para tener una lista unica y poder ofrecer autocompletado.
        coleccionProveedor = baseDatos.getCollection("Proveedor");
        // igual que con los proveedores: las categorias de producto ahora
        // son dinamicas y se guardan en mongo. se pueden agregar nuevas
        // desde el formulario sin tocar el codigo.
        coleccionCategoria = baseDatos.getCollection("Categoria");
        // coleccion chiquita de una sola linea/documento para guardar ajustes
        // simples del sistema, como el nombre de "responsable" por defecto
        // que se usa en los reportes y en cada movimiento de inventario.
        coleccionConfiguracion = baseDatos.getCollection("Configuracion");
    }

    public DBCollection getColeccionProd() {
        return coleccionProd;
    }

    public DBCollection getColeccionUser() {
        return coleccionUser;
    }

    public DBCollection getColeccionMov() {
        return coleccionMov;
    }

    public DBCollection getColeccionProveedor() {
        return coleccionProveedor;
    }

    public DBCollection getColeccionCategoria() {
        return coleccionCategoria;
    }

    public DBCollection getColeccionConfiguracion() {
        return coleccionConfiguracion;
    }
    

    /**
     * hace una prueba rapida y real contra el servidor de mongo para saber
     * si esta disponible antes de que el resto del programa intente usarlo.
     * la idea es fallar rapido y con un mensaje claro en vez de que el
     * usuario reciba un stacktrace feo despues de llenar un formulario.
     *
     * @return true si se pudo hablar con el servidor, false si no.
     */
    public static boolean hayConexion() {
        MongoClient prueba = null;
        try {
            prueba = new MongoClient(HOST, PUERTO);
            // esta linea obliga a hacer un viaje real al servidor, si mongo
            // no esta prendido aqui es donde explota
            prueba.getDatabaseNames();
            return true;
        } catch (RuntimeException ex) {
            // MongoTimeoutException (cuando no hay servidor) y cualquier otro
            // problema de conexion son subclases de RuntimeException, asi que
            // basta con atrapar esta para cubrirlas todas
            return false;
        } finally {
            if (prueba != null) {
                prueba.close();
            }
        }
    }
}
