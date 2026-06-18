package Modelo;

public class Movimiento {
    private String tipo; // Entrada / Salida
    private int cantidad;
    private double precioUnitario;
    private String fecha; // dd/MM/yyyy
    private double total;
    private String usuario; // opcional
    private String nombreProducto;
    private String codigoProducto;

    public Movimiento() {}

    public Movimiento(String tipo, int cantidad, double precioUnitario, String fecha, double total, String usuario, String nombreProducto, String codigoProducto) {
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.fecha = fecha;
        this.total = total;
        this.usuario = usuario;
        this.nombreProducto = nombreProducto;
        this.codigoProducto = codigoProducto;
    }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public String getCodigoProducto() { return codigoProducto; }
    public void setCodigoProducto(String codigoProducto) { this.codigoProducto = codigoProducto; }
}
