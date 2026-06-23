package Controlador;

import Modelo.LoginDAO;
import Vista.FrmLogin;
import Vista.FrmSistema;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

public class ControladorLogin implements ActionListener {

    FrmLogin objVista;
    LoginDAO objDAO;

    public ControladorLogin(FrmLogin vista, LoginDAO dao) {

        this.objVista = vista;
        this.objDAO = dao;

        // crear usuario admin automático
        objDAO.crearUsuarioInicial();

        // conectar botón
        objVista.btnIngresar.addActionListener(this);
        // ojo: txtUsuario NO se conecta aqui. su Enter ya esta manejado
        // dentro de FrmLogin (mueve el foco a la contraseña); si tambien
        // dispara iniciarSesion() aqui, se intenta loguear de inmediato
        // con la contraseña todavia vacia y sale el mensaje de "complete
        // todos los campos" apenas el usuario presiona Enter.
        objVista.txtContraseña.addActionListener(this);

        // navegacion con las flechas del teclado entre usuario y contraseña
        objVista.txtUsuario.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN
                        || e.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
                    objVista.txtContraseña.requestFocusInWindow();
                }
            }
        });
        objVista.txtContraseña.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP
                        || e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) {
                    objVista.txtUsuario.requestFocusInWindow();
                }
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == objVista.btnIngresar || e.getSource() == objVista.txtContraseña) {

            iniciarSesion();
        }
    }

    public void iniciarSesion() {

        String usuario = objVista.txtUsuario.getText().trim();
        String contraseña = String.valueOf(objVista.txtContraseña.getPassword()).trim();

        // si el usuario no borro el texto de ejemplo del campo usuario,
        // lo tratamos como campo vacio
        if (usuario.equals("Ingrese su nombre de usuario")) {
            usuario = "";
        }

        if (usuario.isBlank() || contraseña.isBlank()) {
            JOptionPane.showMessageDialog(objVista, "Complete todos los campos por favor");
            return;
        }

        boolean acceso =
                objDAO.iniciarSesion(usuario, contraseña);

        if (acceso) {

            JOptionPane.showMessageDialog(objVista,
                    "Bienvenido al sistema");

            // abrir interfaz principal
    FrmSistema frmSistema = new FrmSistema();
    frmSistema.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
    frmSistema.setVisible(true);

    frmSistema.setLocationRelativeTo(null);

            // cerrar login
            objVista.dispose();

        } else {

            JOptionPane.showMessageDialog(objVista,
                    "Usuario o contraseña incorrectos");

            // se borra solo la contraseña; el usuario se queda escrito
            // para que la persona no tenga que volver a teclearlo
            objVista.txtContraseña.setText("");
            objVista.txtContraseña.requestFocusInWindow();
        }
    }
}