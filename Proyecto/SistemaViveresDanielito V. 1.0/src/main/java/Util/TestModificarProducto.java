package Util;

import Modelo.Producto;
import Modelo.ProductoDAO;

public class TestModificarProducto {
    public static void main(String[] args) {
        ProductoDAO dao = new ProductoDAO();
        String nombre = "maiz";
        Producto antes = dao.buscarPorNombre(nombre);
        System.out.println("ANTES: " + (antes == null ? "null" : antes.getNombre() + ", stock=" + antes.getStock()));
        if (antes != null) {
            Producto nuevo = new Producto(antes.getNombre(), antes.getTipo(), antes.getPrecioUnit(), antes.getProveedor(), antes.getStock() + 1);
            dao.modificarProducto(antes.getNombre(), nuevo);
            Producto despues = dao.buscarPorNombre(nombre);
            System.out.println("DESPUES: " + (despues == null ? "null" : despues.getNombre() + ", stock=" + despues.getStock()));
        }
    }
}
