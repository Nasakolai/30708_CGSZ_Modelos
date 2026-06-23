package Vista;

import Controlador.ControladorGestionCategorias;
import Controlador.ControladorGestionInventarioo;
import Controlador.ControladorGestionP;
import Controlador.ControladorProducto;
import Modelo.ProductoDAO;
import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * pantalla para gestionar el catalogo de categorias: ver cuantos
 * productos tiene cada una, agregar nuevas, renombrar o eliminar.
 * mismo diseño que FrmGestionProveedores y el resto de la app.
 */
public class FrmGestionCategorias extends javax.swing.JFrame {

    int xMouse, yMouse;

    public FrmGestionCategorias() {
        initComponents();
        SetImageLabel(agregar, "/imagenes/agregar.png");
        SetImageLabel(agregarblanco, "/imagenes/agregarblanco.png");
        agregarblanco.setVisible(false);
        SetImageLabel(usuario, "/imagenes/usuarioblanco.png");
        SetImageLabel(logo, "/imagenes/logoblanco.png");
        SetImageLabel(entrada, "/imagenes/entradaysalida.png");
        SetImageLabel(entradablanco, "/imagenes/entradaysalidablanco.png");
        entradablanco.setVisible(false);
        SetImageLabel(gestion, "/imagenes/gestionar.png");
        SetImageLabel(gestionblanco, "/imagenes/gestionarblanco.png");
        gestionblanco.setVisible(false);
        SetImageLabel(reportes, "/imagenes/reportes.png");
        SetImageLabel(reportesblanco, "/imagenes/reportesblanco.png");
        reportesblanco.setVisible(false);
        SetImageLabel(salir, "/imagenes/cerrarsesion.png");
        SetImageLabel(salirblanco, "/imagenes/cerrarsesionblanco.png");
        salirblanco.setVisible(false);
        new ControladorGestionCategorias(this);
    }

    private void SetImageLabel(JLabel label, String root) {
        Util.ImagenUtil.poner(label, root, this);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        menu = new javax.swing.JPanel();
        agregar = new javax.swing.JLabel();
        gestion = new javax.swing.JLabel();
        entrada = new javax.swing.JLabel();
        reportes = new javax.swing.JLabel();
        salir = new javax.swing.JLabel();
        txtSalirMenu = new javax.swing.JLabel();
        txtGestion = new javax.swing.JLabel();
        txtEntrada = new javax.swing.JLabel();
        txtReporte = new javax.swing.JLabel();
        txtAgregar = new javax.swing.JLabel();
        salirblanco = new javax.swing.JLabel();
        agregarblanco = new javax.swing.JLabel();
        gestionblanco = new javax.swing.JLabel();
        entradablanco = new javax.swing.JLabel();
        reportesblanco = new javax.swing.JLabel();
        usuario = new javax.swing.JLabel();
        header = new javax.swing.JPanel();
        btnSalir = new javax.swing.JPanel();
        txtSalir = new javax.swing.JLabel();
        logo = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tabla = new javax.swing.JTable();
        btnAgregar = new javax.swing.JButton();
        btnRenombrar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setUndecorated(true);
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(0, 38, 102));
        jPanel1.setPreferredSize(new java.awt.Dimension(1009, 670));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel6.setFont(new java.awt.Font("Roboto", 0, 24));
        jLabel6.setForeground(new java.awt.Color(204, 223, 255));
        jLabel6.setText("Gestión de Categorías");
        jPanel1.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 180, -1, -1));

        menu.setBackground(new java.awt.Color(153, 190, 255));
        menu.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(10, 25, 51), 3));
        menu.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        menu.add(agregar,       new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 30, 30, 30));
        menu.add(agregarblanco, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 30, 30, 30));
        gestion.setPreferredSize(new java.awt.Dimension(30,30));
        menu.add(gestion,       new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 80, -1, -1));
        gestionblanco.setPreferredSize(new java.awt.Dimension(30,30));
        menu.add(gestionblanco, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 80, -1, -1));
        entrada.setPreferredSize(new java.awt.Dimension(30,30));
        menu.add(entrada,       new org.netbeans.lib.awtextra.AbsoluteConstraints(9, 126, -1, -1));
        entradablanco.setPreferredSize(new java.awt.Dimension(30,30));
        menu.add(entradablanco, new org.netbeans.lib.awtextra.AbsoluteConstraints(9, 126, -1, -1));
        reportes.setPreferredSize(new java.awt.Dimension(30,30));
        menu.add(reportes,      new org.netbeans.lib.awtextra.AbsoluteConstraints(9, 174, -1, -1));
        reportesblanco.setPreferredSize(new java.awt.Dimension(30,30));
        menu.add(reportesblanco,new org.netbeans.lib.awtextra.AbsoluteConstraints(9, 174, -1, -1));
        salir.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        salir.setPreferredSize(new java.awt.Dimension(30,30));
        menu.add(salir,         new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 390, -1, -1));
        salirblanco.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        salirblanco.setPreferredSize(new java.awt.Dimension(30,30));
        menu.add(salirblanco,   new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 390, -1, -1));

        configurarItemMenu(txtAgregar,  "      Agregar Productos",  10, 30,  220, 30, this::txtAgregarClick);
        configurarItemMenu(txtGestion,  "       Gestión Productos",  8, 80,  230, 30, this::txtGestionClick);
        configurarItemMenu(txtEntrada,  "        Gestión Inventario", 0, 130, 230, -1, this::txtEntradaClick);
        configurarItemMenu(txtReporte,  "        Generar Reportes",  2, 180, 230, 20, this::txtReporteClick);
        configurarItemMenu(txtSalirMenu,"Salir del Sistema",        48, 390, -1, 30,  this::txtSalirMenuClick);

        jPanel1.add(menu, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 220, 270, 430));

        usuario.setBackground(new java.awt.Color(153, 190, 255));
        jPanel1.add(usuario, new org.netbeans.lib.awtextra.AbsoluteConstraints(890, 80, 110, 120));

        header.setBackground(new java.awt.Color(0, 38, 102));
        header.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                setLocation(evt.getXOnScreen()-xMouse, evt.getYOnScreen()-yMouse);
            }
        });
        header.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) { xMouse=evt.getX(); yMouse=evt.getY(); }
        });

        btnSalir.setBackground(new java.awt.Color(0, 38, 102));
        btnSalir.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btnSalir.setBackground(Color.red); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btnSalir.setBackground(new java.awt.Color(0,38,102)); }
        });

        txtSalir.setFont(new java.awt.Font("Roboto Condensed Black", 0, 24));
        txtSalir.setForeground(Color.WHITE);
        txtSalir.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtSalir.setText("X");
        txtSalir.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        txtSalir.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) { System.exit(0); }
        });

        javax.swing.GroupLayout bsl = new javax.swing.GroupLayout(btnSalir);
        btnSalir.setLayout(bsl);
        bsl.setHorizontalGroup(bsl.createParallelGroup().addComponent(txtSalir, javax.swing.GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE));
        bsl.setVerticalGroup(bsl.createParallelGroup().addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bsl.createSequentialGroup().addGap(0,0,Short.MAX_VALUE).addComponent(txtSalir, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)));

        javax.swing.GroupLayout hl = new javax.swing.GroupLayout(header);
        header.setLayout(hl);
        hl.setHorizontalGroup(hl.createParallelGroup().addGroup(javax.swing.GroupLayout.Alignment.TRAILING, hl.createSequentialGroup().addGap(0,946,Short.MAX_VALUE).addComponent(btnSalir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)));
        hl.setVerticalGroup(hl.createParallelGroup().addGroup(javax.swing.GroupLayout.Alignment.TRAILING, hl.createSequentialGroup().addGap(0,0,Short.MAX_VALUE).addComponent(btnSalir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)));

        jPanel1.add(header, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1010, 50));

        logo.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        logo.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                Vista.FrmSistema frm = new Vista.FrmSistema();
                frm.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
                frm.setVisible(true); frm.setLocationRelativeTo(null); dispose();
            }
        });
        jPanel1.add(logo, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 70, 210, 140));

        tabla.setFont(new java.awt.Font("Roboto", 0, 14));
        tabla.setBackground(new java.awt.Color(153, 190, 255));
        tabla.setForeground(Color.BLACK);
        tabla.setGridColor(new java.awt.Color(153, 190, 255));
        tabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        jScrollPane1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(10, 25, 51), 3));
        jScrollPane1.setViewportView(tabla);
        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 220, 710, 360));

        configurarBoton(btnAgregar,   "Agregar Categoría");
        configurarBoton(btnRenombrar, "Renombrar");
        configurarBoton(btnEliminar,  "Eliminar");
        jPanel1.add(btnAgregar,   new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 600, 220, 40));
        jPanel1.add(btnRenombrar, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 600, 220, 40));
        jPanel1.add(btnEliminar,  new org.netbeans.lib.awtextra.AbsoluteConstraints(750, 600, 220, 40));

        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));
        setSize(1009, 670);
        pack();
    }

    private void configurarItemMenu(javax.swing.JLabel label, String texto, int x, int y, int w, int h, Runnable onClick) {
        label.setFont(new java.awt.Font("Roboto", 0, 22));
        label.setForeground(Color.BLACK);
        label.setText(texto);
        label.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e)  { onClick.run(); }
            public void mouseEntered(java.awt.event.MouseEvent e)  { label.setForeground(Color.WHITE); }
            public void mouseExited(java.awt.event.MouseEvent e)   { label.setForeground(Color.BLACK); }
        });
        menu.add(label, new org.netbeans.lib.awtextra.AbsoluteConstraints(x, y, w, h));
    }

    private void configurarBoton(javax.swing.JButton btn, String texto) {
        btn.setBackground(Color.WHITE);
        btn.setFont(new java.awt.Font("Roboto", 0, 18));
        btn.setForeground(Color.BLACK);
        btn.setText(texto);
        btn.setBorder(null);
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(new Color(82,126,204)); btn.setForeground(Color.WHITE); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(Color.WHITE); btn.setForeground(Color.BLACK); }
        });
    }

    private void txtAgregarClick() {
        FrmProducto v = new FrmProducto(); v.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        new ControladorProducto(v, new ProductoDAO()); v.setVisible(true); v.setLocationRelativeTo(null); dispose();
    }
    private void txtGestionClick() {
        FrmGestionProductos v = new FrmGestionProductos(); v.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        new ControladorGestionP(v, new ProductoDAO()); v.setVisible(true); v.setLocationRelativeTo(null); dispose();
    }
    private void txtEntradaClick() {
        FrmGestionInventarioo v = new FrmGestionInventarioo(); v.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        new ControladorGestionInventarioo(v, new ProductoDAO()); v.setVisible(true); v.setLocationRelativeTo(null); dispose();
    }
    private void txtReporteClick() {
        FrmGenerarReporte v = new FrmGenerarReporte(); v.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        new Controlador.ControladorGenerarReporte(v, new Modelo.MovimientoDAO(), new ProductoDAO()); v.setVisible(true); v.setLocationRelativeTo(null); dispose();
    }
    private void txtSalirMenuClick() { cerrarSesion(); }

    private void cerrarSesion() {
        Vista.FrmLogin login = new Vista.FrmLogin();
        Modelo.LoginDAO loginDao = new Modelo.LoginDAO();
        new Controlador.ControladorLogin(login, loginDao);
        login.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        login.setVisible(true);
        login.setLocationRelativeTo(null);
        this.dispose();
    }

    public javax.swing.JButton btnAgregar;
    public javax.swing.JButton btnRenombrar;
    public javax.swing.JButton btnEliminar;
    public javax.swing.JTable tabla;
    private javax.swing.JLabel agregar, agregarblanco, gestion, gestionblanco;
    private javax.swing.JLabel entrada, entradablanco, reportes, reportesblanco;
    private javax.swing.JLabel salir, salirblanco, usuario, logo;
    private javax.swing.JPanel header, btnSalir, menu, jPanel1;
    private javax.swing.JLabel jLabel6, txtSalir, txtSalirMenu;
    private javax.swing.JLabel txtAgregar, txtGestion, txtEntrada, txtReporte;
    private javax.swing.JScrollPane jScrollPane1;
}
