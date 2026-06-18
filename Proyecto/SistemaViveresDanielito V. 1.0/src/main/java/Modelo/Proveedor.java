
package Modelo;

public class Proveedor {
    private String nombre;
    private String numero;

    public Proveedor(String nombre, String numero) {
        this.nombre = nombre;
        this.numero = numero;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    @Override
    public String toString() {
        return "Proveedor{" + "nombre=" + nombre + ", numero=" + numero + '}';
    }
    
    
}
