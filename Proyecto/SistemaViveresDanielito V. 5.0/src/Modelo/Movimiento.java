package Modelo;

/**
 * representa un movimiento de inventario (entrada o salida) ya guardado
 * en mongo. incluye el motivo (por que se hizo el movimiento) y el
 * responsable (quien lo hizo), que se guarda automaticamente con el
 * nombre configurado en la pantalla de Reportes.
 */
public class Movimiento {
    private String id;
    private String tipo; // "Entrada" o "Salida"
    private int cantidad;
    private double precioUnitario;
    private String fecha;
    private double total;
    private String responsable; // quien registro el movimiento
    private String nombreProducto;
    private String codigoProducto;
    private String motivo;         // ej: Venta, Compra, Caducidad, Otro...
    private String detalleMotivo;  // solo cuando motivo == "Otro"

    public Movimiento() {
    }

    public Movimiento(String tipo, int cantidad, double precioUnitario, String fecha,
                       double total, String responsable, String nombreProducto, String codigoProducto,
                       String motivo, String detalleMotivo) {
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.fecha = fecha;
        this.total = total;
        this.responsable = responsable;
        this.nombreProducto = nombreProducto;
        this.codigoProducto = codigoProducto;
        this.motivo = motivo;
        this.detalleMotivo = detalleMotivo;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getCodigoProducto() { return codigoProducto; }
    public void setCodigoProducto(String codigoProducto) { this.codigoProducto = codigoProducto; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getDetalleMotivo() { return detalleMotivo; }
    public void setDetalleMotivo(String detalleMotivo) { this.detalleMotivo = detalleMotivo; }
}
