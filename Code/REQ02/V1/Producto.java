
package Modelo;


public class Producto {
  private String nombre;
  private String tipo;
  private double precioUnit;
  private String proveedor;

    public Producto(String nombre, String tipo, double precioUnit, String proveedor) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.precioUnit = precioUnit;
        this.proveedor = proveedor;
    }

    public Producto() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getPrecioUnit() {
        return precioUnit;
    }

    public void setPrecioUnit(double precioUnit) {
        this.precioUnit = precioUnit;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public String toString() {
        return "Producto{" + "nombre=" + nombre + ", tipo=" + tipo + ", precioUnit=" + precioUnit + ", proveedor=" + proveedor + '}';
    }
   
}