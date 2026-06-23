package Modelo;

/**
 * catalogos fijos de motivos para entradas y salidas de inventario.
 * cubren las causas mas comunes en un negocio de viveres: para salidas,
 * ademas de la venta, se contemplan las causas tipicas de merma (producto
 * vencido, dañado, robado/perdido, consumo interno, devuelto al proveedor
 * o un ajuste de conteo); para entradas, ademas de la compra normal, se
 * contemplan devoluciones de clientes, ajustes de inventario y donaciones.
 * en ambos casos se deja la opcion "Otro" para que el usuario escriba
 * cualquier caso que no encaje en la lista.
 */
public class MotivosMovimiento {

    public static final String OTRO = "Otro";

    public static final String[] MOTIVOS_SALIDA = {
        "Venta",
        "Caducidad / Vencimiento",
        "Pérdida / Robo",
        "Daño o deterioro",
        "Consumo interno",
        "Devolución a proveedor",
        "Ajuste de inventario",
        OTRO
    };

    public static final String[] MOTIVOS_ENTRADA = {
        "Compra",
        "Devolución de cliente",
        "Ajuste de inventario",
        "Donación",
        OTRO
    };

    private MotivosMovimiento() {
    }
}
