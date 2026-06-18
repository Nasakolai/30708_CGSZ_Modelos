package Modelo;

/**
 * representa un proveedor del catalogo. por ahora solo guardamos el nombre,
 * pero al tener su propia clase es facil agregarle mas datos en el futuro
 * (telefono, direccion, etc) sin tener que tocar todo lo que ya usa Proveedor.
 */
public class Proveedor {
    private String id; // _id de mongo en texto, viene null si todavia no se ha guardado
    private String nombre;

    public Proveedor() {
    }

    public Proveedor(String nombre) {
        this.nombre = nombre;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
