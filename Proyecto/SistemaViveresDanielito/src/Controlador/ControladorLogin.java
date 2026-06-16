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
        objVista.txtUsuario.addActionListener(this);
        objVista.txtContraseña.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == objVista.btnIngresar || e.getSource() == objVista.txtUsuario || e.getSource() == objVista.txtContraseña) {

            iniciarSesion();
        }
    }

    public void iniciarSesion() {

        String usuario = objVista.txtUsuario.getText().trim();

        String contraseña =
                String.valueOf(objVista.txtContraseña.getPassword()).trim();

        if (usuario.isBlank() || contraseña.isBlank()) {

            JOptionPane.showMessageDialog(objVista,
                    "Complete todos los campos por favor");

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
        }
    }
}