
package Modelo;


public class Producto {
  private String nombre;
  private String tipo;
  private double precioUnit;
  private String proveedor;
  private int stock;
  private String codigo;

    public Producto(String nombre, String tipo, double precioUnit, String proveedor) {
        this(nombre, tipo, precioUnit, proveedor, 0, "");
    }

    public Producto(String nombre, String tipo, double precioUnit, String proveedor, int stock) {
        this(nombre, tipo, precioUnit, proveedor, stock, "");
    }

    public Producto(String nombre, String tipo, double precioUnit, String proveedor, int stock, String codigo) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.precioUnit = precioUnit;
        this.proveedor = proveedor;
        this.stock = stock;
        this.codigo = codigo;
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

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    @Override
    public String toString() {
        return "Producto{" + "nombre=" + nombre + ", tipo=" + tipo + ", precioUnit=" + precioUnit + ", proveedor=" + proveedor + ", stock=" + stock + ", codigo=" + codigo + '}';
    }
   
}