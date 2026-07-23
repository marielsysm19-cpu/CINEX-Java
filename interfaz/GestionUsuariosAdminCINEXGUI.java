package interfaz;

import control.ControlGestionarUsuariosCINEX;
import control.ControlGestionarUsuariosCINEX.ResultadoUsuario;
import control.ControlGestionarUsuariosCINEX.RestablecimientoContrasena;
import entidad.UsuarioCINEX;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

public class GestionUsuariosAdminCINEXGUI extends JFrame {

    private static final Color AZUL_FONDO = new Color(3, 15, 36);
    private static final Color AZUL_CARD = new Color(11, 46, 91);
    private static final Color AZUL_CARD_2 = new Color(13, 51, 101);
    private static final Color AZUL_BORDE = new Color(61, 113, 170);
    private static final Color AMARILLO = new Color(255, 203, 0);
    private static final Color AZUL_BOTON = new Color(0, 80, 160);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO_SUAVE = new Color(205, 220, 245);
    private static final Color VERDE = new Color(18, 132, 81);
    private static final Color ROJO = new Color(255, 95, 95);
    private static final Color ROJO_BOTON = new Color(120, 35, 48);
    private static final Color GRIS_BOTON = new Color(33, 57, 91);

    private final ControlGestionarUsuariosCINEX control = new ControlGestionarUsuariosCINEX();
    private final String usuarioActual;

    private int idUsuarioSeleccionado = -1;
    private JTextField txtNombre;
    private JTextField txtUsuario;
    private JPasswordField txtContrasena;
    private JComboBox<String> cbRol;
    private JComboBox<String> cbEstado;
    private JTextField txtBuscar;
    private JTable tablaUsuarios;
    private DefaultTableModel modeloTabla;
    private JLabel lblTotal;
    private JLabel lblMensaje;
    private BotonCINEX btnGuardar;
    private BotonCINEX btnActualizar;
    private BotonCINEX btnCambiarEstado;
    private BotonCINEX btnNuevo;
    private BotonCINEX btnEliminar;
    private String usuarioOriginalSeleccionado = "";
    private BotonCINEX btnRestablecerContrasena;

    public GestionUsuariosAdminCINEXGUI() {
        this("admin");
    }

    public GestionUsuariosAdminCINEXGUI(String usuarioActual) {
        this.usuarioActual = usuarioActual == null || usuarioActual.trim().isEmpty()
                ? "admin" : usuarioActual.trim();

        configurarVentana();
        construirInterfaz();
        configurarEventos();
        cargarUsuarios("");
        limpiarFormulario();
    }

    private void configurarVentana() {
        setTitle("CINEX - Gestión de usuarios");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        getContentPane().setBackground(AZUL_FONDO);
    }

    private void construirInterfaz() {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(AZUL_FONDO);
        setContentPane(contenedor);

        // Se utiliza todo el ancho de la ventana.
        // El menú lateral fue eliminado para mantener el mismo formato
        // visual de la interfaz de gestión de funciones.
        contenedor.add(crearContenido(), BorderLayout.CENTER);
    }

    private JPanel crearContenido() {
        JPanel contenido = new JPanel(new BorderLayout(0, 10));
        contenido.setBackground(AZUL_FONDO);
        contenido.setBorder(new EmptyBorder(18, 32, 16, 32));

        contenido.add(crearCabecera(), BorderLayout.NORTH);
        contenido.add(crearCentro(), BorderLayout.CENTER);
        contenido.add(crearZonaInferior(), BorderLayout.SOUTH);

        return contenido;
    }

    private JPanel crearCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        cabecera.setPreferredSize(new Dimension(0, 82));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new javax.swing.BoxLayout(textos, javax.swing.BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Gestión de usuarios");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 34));

        JLabel subtitulo = new JLabel("Administra los accesos de administradores, gerentes y taquilleros.");
        subtitulo.setForeground(TEXTO_SUAVE);
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitulo.setBorder(new EmptyBorder(5, 0, 0, 0));

        textos.add(titulo);
        textos.add(subtitulo);

        JPanel buscador = new RoundedPanel(AZUL_CARD, 10);
        buscador.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 6));
        buscador.setPreferredSize(new Dimension(300, 50));
        buscador.setBorder(new EmptyBorder(0, 8, 0, 8));

        JLabel lblBuscar = new JLabel("Buscar");
        lblBuscar.setForeground(AMARILLO);
        lblBuscar.setFont(new Font("Segoe UI", Font.BOLD, 13));

        txtBuscar = crearCampoTexto();
        txtBuscar.setPreferredSize(new Dimension(175, 34));

        buscador.add(lblBuscar);
        buscador.add(txtBuscar);

        JPanel contBuscador = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        contBuscador.setOpaque(false);
        contBuscador.add(buscador);

        cabecera.add(textos, BorderLayout.WEST);
        cabecera.add(contBuscador, BorderLayout.EAST);

        return cabecera;
    }

    private JPanel crearCentro() {
        JPanel centro = new JPanel(new BorderLayout(20, 0));
        centro.setOpaque(false);

        JPanel panelFormulario = crearPanelFormulario();
        panelFormulario.setPreferredSize(new Dimension(500, 0));

        centro.add(panelFormulario, BorderLayout.WEST);
        centro.add(crearPanelTabla(), BorderLayout.CENTER);

        return centro;
    }

    private JPanel crearPanelFormulario() {
        JPanel panel = new RoundedPanel(AZUL_CARD, 0);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1),
                new EmptyBorder(15, 24, 15, 24)
        ));

        JPanel cuerpo = new JPanel(new GridBagLayout());
        cuerpo.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 0, 2, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel titulo = new JLabel("Datos del usuario");
        titulo.setForeground(AMARILLO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 23));
        cuerpo.add(titulo, gbc);

        gbc.gridy++;
        JLabel ayuda = new JLabel("Complete la información para registrar o editar.");
        ayuda.setForeground(TEXTO_SUAVE);
        ayuda.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ayuda.setBorder(new EmptyBorder(1, 0, 5, 0));
        cuerpo.add(ayuda, gbc);

        txtNombre = crearCampoTexto();
        txtUsuario = crearCampoTexto();
        txtContrasena = crearCampoPassword();
        cbRol = crearCombo(new String[]{"Administrador", "Gerente", "Taquillero"});
        cbEstado = crearCombo(new String[]{"Activo", "Inactivo"});

        agregarCampo(cuerpo, gbc, "Nombre completo", txtNombre);
        agregarCampo(cuerpo, gbc, "Usuario", txtUsuario);
        agregarCampo(cuerpo, gbc, "Contraseña", txtContrasena);
        agregarCampo(cuerpo, gbc, "Rol", cbRol);
        agregarCampo(cuerpo, gbc, "Estado", cbEstado);

        gbc.gridy++;
        JLabel nota = new JLabel("La contraseña personal del gerente no puede verse ni editarse desde esta pantalla.");
        nota.setForeground(TEXTO_SUAVE);
        nota.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        nota.setBorder(new EmptyBorder(2, 0, 2, 0));
        cuerpo.add(nota, gbc);

        gbc.gridy++;
        btnGuardar = crearBotonAccion("GUARDAR", AMARILLO, AZUL_FONDO, true);
        btnGuardar.setPreferredSize(new Dimension(0, 38));
        cuerpo.add(btnGuardar, gbc);

        gbc.gridy++;
        JPanel fila1 = new JPanel(new GridLayoutCINEX(1, 2, 10, 0));
        fila1.setOpaque(false);
        btnActualizar = crearBotonAccion("ACTUALIZAR", GRIS_BOTON, BLANCO, false);
        btnCambiarEstado = crearBotonAccion("ACTIVAR / INACTIVAR", GRIS_BOTON, BLANCO, false);
        fila1.add(btnActualizar);
        fila1.add(btnCambiarEstado);
        cuerpo.add(fila1, gbc);

        gbc.gridy++;
        JPanel fila2 = new JPanel(new GridLayoutCINEX(1, 2, 10, 0));
        fila2.setOpaque(false);
        btnNuevo = crearBotonAccion("NUEVO / LIMPIAR", AZUL_CARD, BLANCO, false);
        btnEliminar = crearBotonAccion("ELIMINAR", ROJO_BOTON, BLANCO, false);
        fila2.add(btnNuevo);
        fila2.add(btnEliminar);
        cuerpo.add(fila2, gbc);

        gbc.gridy++;
        btnRestablecerContrasena = crearBotonAccion(
                "RESTABLECER CONTRASEÑA DEL GERENTE",
                AZUL_BOTON,
                BLANCO,
                false
        );
        btnRestablecerContrasena.setPreferredSize(new Dimension(0, 38));
        btnRestablecerContrasena.setEnabled(false);
        cuerpo.add(btnRestablecerContrasena, gbc);

        gbc.gridy++;
        gbc.weighty = 1;
        cuerpo.add(new JLabel(""), gbc);

        panel.add(cuerpo, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelTabla() {
        JPanel panel = new RoundedPanel(AZUL_CARD, 0);
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_CARD, 1),
                new EmptyBorder(26, 28, 26, 28)
        ));

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);

        JLabel titulo = new JLabel("Usuarios registrados");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 25));

        lblTotal = new JLabel("0 usuario(s)");
        lblTotal.setForeground(AMARILLO);
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);

        encabezado.add(titulo, BorderLayout.WEST);
        encabezado.add(lblTotal, BorderLayout.EAST);

        modeloTabla = new DefaultTableModel(new Object[]{"ID", "Nombre", "Usuario", "Rol", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaUsuarios = new JTable(modeloTabla);
        tablaUsuarios.setRowHeight(42);
        tablaUsuarios.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        tablaUsuarios.setForeground(BLANCO);
        tablaUsuarios.setBackground(AZUL_CARD);
        tablaUsuarios.setSelectionBackground(new Color(21, 80, 142));
        tablaUsuarios.setSelectionForeground(BLANCO);
        tablaUsuarios.setGridColor(AZUL_BORDE);
        tablaUsuarios.setShowGrid(true);
        tablaUsuarios.setFillsViewportHeight(true);
        tablaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaUsuarios.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tablaUsuarios.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = tablaUsuarios.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setPreferredSize(new Dimension(0, 38));
        header.setDefaultRenderer(new RendererEncabezado());

        tablaUsuarios.setDefaultRenderer(Object.class, new RendererTabla());

        JScrollPane scroll = new JScrollPane(tablaUsuarios);
        scroll.setBorder(new LineBorder(AZUL_BORDE, 1));
        scroll.getViewport().setBackground(AZUL_CARD);
        scroll.setBackground(AZUL_CARD);

        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        SwingUtilities.invokeLater(this::ajustarColumnasTabla);

        return panel;
    }


    private JPanel crearZonaInferior() {
        JPanel inferior = new JPanel(new BorderLayout(14, 0));
        inferior.setOpaque(false);

        // El mensaje y el botón comparten la misma fila para ahorrar altura.
        inferior.add(crearBarraMensaje(), BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        acciones.setOpaque(false);

        JButton btnMenuPrincipal = crearBotonMenuPrincipal();
        btnMenuPrincipal.addActionListener(
                e -> abrirVentana("interfaz.MenuAdministradorCINEXGUI")
        );

        acciones.add(btnMenuPrincipal);
        inferior.add(acciones, BorderLayout.EAST);

        return inferior;
    }

    private JPanel crearBarraMensaje() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AZUL_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(40, 90, 145), 1),
                new EmptyBorder(10, 18, 10, 18)
        ));
        panel.setPreferredSize(new Dimension(0, 46));

        lblMensaje = new JLabel("Formulario listo para registrar un usuario.");
        lblMensaje.setForeground(new Color(35, 230, 142));
        lblMensaje.setFont(new Font("Segoe UI", Font.BOLD, 14));

        panel.add(lblMensaje, BorderLayout.CENTER);
        return panel;
    }

    private void agregarCampo(JPanel panel, GridBagConstraints gbc, String texto, Component campo) {
        gbc.gridy++;
        JLabel label = new JLabel(texto);
        label.setForeground(TEXTO_SUAVE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(label, gbc);

        gbc.gridy++;
        campo.setPreferredSize(new Dimension(0, 38));
        panel.add(campo, gbc);
    }

    private JTextField crearCampoTexto() {
        JTextField campo = new JTextField();
        campo.setBackground(AZUL_CARD_2);
        campo.setForeground(BLANCO);
        campo.setCaretColor(AMARILLO);
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        campo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1),
                new EmptyBorder(0, 12, 0, 12)
        ));
        return campo;
    }

    private JPasswordField crearCampoPassword() {
        JPasswordField campo = new JPasswordField();
        campo.setBackground(AZUL_CARD_2);
        campo.setForeground(BLANCO);
        campo.setCaretColor(AMARILLO);
        campo.setEchoChar('•');
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        campo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1),
                new EmptyBorder(0, 12, 0, 12)
        ));
        return campo;
    }

    private JComboBox<String> crearCombo(String[] opciones) {
        JComboBox<String> combo = new JComboBox<>(opciones);
        combo.setBackground(AZUL_CARD_2);
        combo.setForeground(BLANCO);
        combo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        combo.setFocusable(false);
        combo.setBorder(new LineBorder(AZUL_BORDE, 1));
        combo.setOpaque(true);
        combo.setUI(new ComboCINEXUI());
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setText(value == null ? "" : value.toString());
                lbl.setOpaque(true);
                lbl.setBackground(isSelected ? new Color(18, 74, 132) : AZUL_CARD_2);
                lbl.setForeground(BLANCO);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
                lbl.setBorder(new EmptyBorder(8, 12, 8, 12));
                return lbl;
            }
        });
        return combo;
    }


    private JButton crearBotonMenuPrincipal() {
        JButton boton = new JButton("MENÚ PRINCIPAL") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                Color fondo;
                if (!isEnabled()) {
                    fondo = new Color(55, 67, 90);
                } else if (getModel().isPressed()) {
                    fondo = AZUL_BOTON.darker();
                } else if (getModel().isRollover()) {
                    fondo = new Color(18, 98, 184);
                } else {
                    fondo = AZUL_BOTON;
                }

                g2.setColor(fondo);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                g2.setColor(new Color(255, 255, 255, 35));
                g2.drawRoundRect(
                        0,
                        0,
                        getWidth() - 1,
                        getHeight() - 1,
                        10,
                        10
                );

                g2.dispose();
                super.paintComponent(g);
            }
        };

        boton.setPreferredSize(new Dimension(185, 46));
        boton.setForeground(BLANCO);
        boton.setFont(new Font("Arial", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setContentAreaFilled(false);
        boton.setOpaque(false);
        boton.setRolloverEnabled(true);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return boton;
    }

    private BotonCINEX crearBotonAccion(String texto, Color fondo, Color textoColor, boolean principal) {
        BotonCINEX boton = new BotonCINEX(texto, fondo, textoColor, principal);
        boton.setPreferredSize(new Dimension(0, 36));
        return boton;
    }

    private void configurarEventos() {
        btnGuardar.addActionListener(e -> guardarUsuario());
        btnActualizar.addActionListener(e -> actualizarUsuario());
        btnCambiarEstado.addActionListener(e -> cambiarEstadoUsuario());
        btnNuevo.addActionListener(e -> limpiarFormulario());
        btnEliminar.addActionListener(e -> eliminarUsuario());
        btnRestablecerContrasena.addActionListener(
                e -> restablecerContrasenaGerente()
        );
        cbRol.addActionListener(e -> actualizarControlesContrasena());

        tablaUsuarios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccionTabla();
            }
        });

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                cargarUsuarios(txtBuscar.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                cargarUsuarios(txtBuscar.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                cargarUsuarios(txtBuscar.getText());
            }
        });
    }

    private void cargarUsuarios(String filtro) {
        ArrayList<UsuarioCINEX> usuarios = control.buscarUsuarios(filtro);
        modeloTabla.setRowCount(0);

        for (UsuarioCINEX usuario : usuarios) {
            modeloTabla.addRow(new Object[]{
                    usuario.getIdUsuario(),
                    usuario.getNombre(),
                    usuario.getUsuario(),
                    usuario.getRol(),
                    usuario.getEstado()
            });
        }

        lblTotal.setText(usuarios.size() + " usuario(s)");
        ajustarColumnasTabla();
        mostrarMensaje("Usuarios cargados correctamente.", false);
    }

    private void ajustarColumnasTabla() {
        if (tablaUsuarios == null || tablaUsuarios.getColumnModel().getColumnCount() < 5) {
            return;
        }

        tablaUsuarios.getColumnModel().getColumn(0).setPreferredWidth(70);
        tablaUsuarios.getColumnModel().getColumn(0).setMaxWidth(90);
        tablaUsuarios.getColumnModel().getColumn(1).setPreferredWidth(280);
        tablaUsuarios.getColumnModel().getColumn(2).setPreferredWidth(210);
        tablaUsuarios.getColumnModel().getColumn(3).setPreferredWidth(170);
        tablaUsuarios.getColumnModel().getColumn(4).setPreferredWidth(140);
    }

    private void cargarSeleccionTabla() {
        int fila = tablaUsuarios.getSelectedRow();
        if (fila < 0) {
            return;
        }

        idUsuarioSeleccionado =
                parsearEntero(valorTabla(fila, 0));

        usuarioOriginalSeleccionado =
                valorTabla(fila, 2);

        txtNombre.setText(valorTabla(fila, 1));
        txtUsuario.setText(usuarioOriginalSeleccionado);
        txtContrasena.setText("");
        cbRol.setSelectedItem(valorTabla(fila, 3));
        cbEstado.setSelectedItem(valorTabla(fila, 4));

        boolean esCuentaActual =
                usuarioOriginalSeleccionado
                        .equalsIgnoreCase(usuarioActual);

        btnActualizar.setEnabled(true);
        btnCambiarEstado.setEnabled(true);
        btnEliminar.setEnabled(!esCuentaActual);
        btnGuardar.setEnabled(false);
        actualizarControlesContrasena();

        if (esCuentaActual) {
            mostrarMensaje(
                    "Esta es su cuenta actual. "
                            + "Por seguridad no puede autoeliminarse.",
                    true
            );
        } else {
            mostrarMensaje(
                    "Usuario seleccionado. Puede actualizar sus datos "
                            + "o restablecer la contraseña si es gerente.",
                    false
            );
        }
    }

    private String valorTabla(int fila, int columna) {
        Object valor = modeloTabla.getValueAt(fila, columna);
        return valor == null ? "" : String.valueOf(valor);
    }

    private void guardarUsuario() {
        UsuarioCINEX usuario = obtenerDatosFormulario(false);
        ResultadoUsuario resultado = control.registrarUsuario(usuario);

        switch (resultado) {
            case REGISTRADO:
                cargarUsuarios(txtBuscar.getText());
                limpiarFormulario();
                mostrarMensaje("Usuario registrado correctamente.", false);
                break;
            case USUARIO_EXISTE:
                mostrarMensaje("El usuario ya existe en el sistema.", true);
                break;
            case DATOS_INCOMPLETOS:
                mostrarMensaje("Complete nombre, usuario, contraseña, rol y estado.", true);
                break;
            default:
                mostrarMensaje("Error al registrar usuario. Revise la conexión o la estructura de la tabla usuarios.", true);
                break;
        }
    }

    private void actualizarUsuario() {
        UsuarioCINEX usuario = obtenerDatosFormulario(true);
        ResultadoUsuario resultado = control.actualizarUsuario(usuario);

        switch (resultado) {
            case ACTUALIZADO:
                cargarUsuarios(txtBuscar.getText());
                limpiarFormulario();
                mostrarMensaje("Usuario actualizado correctamente.", false);
                break;
            case USUARIO_EXISTE:
                mostrarMensaje("Ya existe otro usuario con ese nombre de acceso.", true);
                break;
            case DATOS_INCOMPLETOS:
                mostrarMensaje("Seleccione un usuario y complete nombre, usuario, rol y estado.", true);
                break;
            default:
                mostrarMensaje("Error al actualizar usuario.", true);
                break;
        }
    }

    private void actualizarControlesContrasena() {
        if (txtContrasena == null || cbRol == null) {
            return;
        }

        boolean editando = idUsuarioSeleccionado > 0;
        boolean gerente = "Gerente".equalsIgnoreCase(
                String.valueOf(cbRol.getSelectedItem())
        );

        /*
         * Al registrar un gerente, el administrador escribe únicamente una
         * contraseña temporal. Al editarlo, la contraseña personal queda
         * protegida y solo puede restablecerse.
         */
        txtContrasena.setEnabled(!editando || !gerente);
        txtContrasena.setEditable(!editando || !gerente);

        if (editando && gerente) {
            txtContrasena.setText("");
            txtContrasena.setToolTipText(
                    "La contraseña personal del gerente está protegida. Use RESTABLECER CONTRASEÑA."
            );
        } else {
            txtContrasena.setToolTipText(
                    gerente
                            ? "Ingrese una contraseña temporal. El gerente deberá cambiarla al iniciar sesión."
                            : "Ingrese la contraseña del usuario."
            );
        }

        if (btnRestablecerContrasena != null) {
            btnRestablecerContrasena.setEnabled(editando && gerente);
        }
    }

    private void restablecerContrasenaGerente() {
        int id = obtenerIdSeleccionado();
        if (id <= 0) {
            mostrarMensaje("Seleccione un gerente.", true);
            return;
        }

        if (!"Gerente".equalsIgnoreCase(
                String.valueOf(cbRol.getSelectedItem())
        )) {
            mostrarMensaje(
                    "La opción de restablecimiento corresponde únicamente al gerente.",
                    true
            );
            return;
        }

        int confirmar = JOptionPane.showConfirmDialog(
                this,
                "¿Desea restablecer la contraseña de este gerente?\n\n"
                        + "Se generará una contraseña temporal y el gerente deberá "
                        + "crear una contraseña personal al iniciar sesión.",
                "Restablecer contraseña",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }

        RestablecimientoContrasena resultado =
                control.restablecerContrasenaGerente(id);

        if (!resultado.fueExitoso()) {
            mostrarMensaje(resultado.getMensaje(), true);
            JOptionPane.showMessageDialog(
                    this,
                    resultado.getMensaje(),
                    "No se pudo restablecer",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        mostrarMensaje(resultado.getMensaje(), false);
        JOptionPane.showMessageDialog(
                this,
                "Contraseña temporal: "
                        + resultado.getContrasenaTemporal()
                        + "\n\nEntréguela al gerente de forma privada. "
                        + "Esta contraseña solo servirá para su próximo ingreso.",
                "Contraseña temporal generada",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void cambiarEstadoUsuario() {
        int id = obtenerIdSeleccionado();
        if (id <= 0) {
            mostrarMensaje("Seleccione un usuario para activar o inactivar.", true);
            return;
        }

        String estadoActual = String.valueOf(cbEstado.getSelectedItem());
        String nuevoEstado = "Activo".equalsIgnoreCase(estadoActual) ? "Inactivo" : "Activo";

        ResultadoUsuario resultado = control.cambiarEstadoUsuario(id, nuevoEstado);

        if (resultado == ResultadoUsuario.ESTADO_CAMBIADO) {
            cargarUsuarios(txtBuscar.getText());
            limpiarFormulario();
            mostrarMensaje("Estado del usuario actualizado correctamente.", false);
        } else {
            mostrarMensaje("No se pudo cambiar el estado del usuario.", true);
        }
    }


    private void eliminarUsuario() {
        int id = obtenerIdSeleccionado();
        if (id <= 0) {
            mostrarMensaje("Seleccione un usuario para eliminar.", true);
            return;
        }

        if (usuarioOriginalSeleccionado
                .equalsIgnoreCase(usuarioActual)) {
            mostrarMensaje(
                    "No puedes eliminar el usuario "
                            + "con el que iniciaste sesión.",
                    true
            );
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Seguro que deseas eliminar este usuario?\n\nSi tiene ventas asociadas, se inactivará para conservar el historial.",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            mostrarMensaje("Eliminación cancelada.", false);
            return;
        }

        ResultadoUsuario resultado = control.eliminarUsuarioSeguro(id);

        switch (resultado) {
            case ELIMINADO:
                cargarUsuarios(txtBuscar.getText());
                limpiarFormulario();
                mostrarMensaje("Usuario eliminado correctamente.", false);
                break;
            case USUARIO_CON_VENTAS_INACTIVADO:
                cargarUsuarios(txtBuscar.getText());
                limpiarFormulario();
                mostrarMensaje("El usuario tenía ventas asociadas, por eso se inactivó en lugar de eliminarse.", false);
                break;
            case NO_ENCONTRADO:
                mostrarMensaje("El usuario seleccionado ya no existe.", true);
                break;
            case DATOS_INCOMPLETOS:
                mostrarMensaje("Seleccione un usuario para eliminar.", true);
                break;
            default:
                mostrarMensaje("No se pudo eliminar el usuario. Revise la conexión o restricciones de la BD.", true);
                break;
        }
    }

    private UsuarioCINEX obtenerDatosFormulario(boolean incluirId) {
        UsuarioCINEX usuario = new UsuarioCINEX();
        if (incluirId) {
            usuario.setIdUsuario(obtenerIdSeleccionado());
        }
        usuario.setNombre(txtNombre.getText().trim());
        usuario.setUsuario(txtUsuario.getText().trim());
        usuario.setContrasena(new String(txtContrasena.getPassword()).trim());
        usuario.setRol(String.valueOf(cbRol.getSelectedItem()));
        usuario.setEstado(String.valueOf(cbEstado.getSelectedItem()));
        return usuario;
    }

    private int obtenerIdSeleccionado() {
        return idUsuarioSeleccionado;
    }

    private int parsearEntero(String texto) {
        try {
            return Integer.parseInt(texto == null ? "" : texto.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private void limpiarFormulario() {
        idUsuarioSeleccionado = -1;
        usuarioOriginalSeleccionado = "";

        txtNombre.setText("");
        txtUsuario.setText("");
        txtContrasena.setText("");
        cbRol.setSelectedItem("Administrador");
        cbEstado.setSelectedItem("Activo");
        tablaUsuarios.clearSelection();

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnCambiarEstado.setEnabled(false);
        btnEliminar.setEnabled(false);
        if (btnRestablecerContrasena != null) {
            btnRestablecerContrasena.setEnabled(false);
        }
        actualizarControlesContrasena();
        mostrarMensaje("Formulario listo para registrar un usuario.", false);
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setText(mensaje);
        lblMensaje.setForeground(error ? ROJO : new Color(35, 230, 142));
    }

    private void abrirVentana(String nombreClase) {
        try {
            Class<?> clase = Class.forName(nombreClase);
            Object ventana;

            try {
                Constructor<?> constructor = clase.getConstructor(String.class);
                ventana = constructor.newInstance(usuarioActual);
            } catch (NoSuchMethodException ex) {
                Constructor<?> constructor = clase.getConstructor();
                ventana = constructor.newInstance();
            }

            if (ventana instanceof JFrame) {
                ((JFrame) ventana).setVisible(true);
                dispose();
            }
        } catch (Exception e) {
            mostrarMensaje("No se pudo abrir la ventana solicitada.", true);
        }
    }

    private static class RendererEncabezado extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setBackground(AZUL_FONDO);
            lbl.setForeground(AMARILLO);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setBorder(new LineBorder(AZUL_BORDE, 1));
            return lbl;
        }
    }

    private static class RendererTabla extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setOpaque(true);
            lbl.setFont(new Font("Segoe UI", column == 4 ? Font.BOLD : Font.PLAIN, 15));
            lbl.setBorder(new LineBorder(AZUL_BORDE, 1));
            lbl.setForeground(BLANCO);

            if (isSelected) {
                lbl.setBackground(new Color(22, 87, 154));
            } else if (column == 4 && "Activo".equalsIgnoreCase(String.valueOf(value))) {
                lbl.setBackground(VERDE);
            } else if (column == 4) {
                lbl.setBackground(new Color(125, 45, 45));
            } else {
                lbl.setBackground(AZUL_CARD);
            }

            if (column == 0 || column == 4) {
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
            }

            return lbl;
        }
    }

    private static class BotonCINEX extends JButton {
        private final Color fondo;
        private final Color texto;
        private final boolean principal;

        BotonCINEX(String textoBoton, Color fondo, Color texto, boolean principal) {
            super(textoBoton);
            this.fondo = fondo;
            this.texto = texto;
            this.principal = principal;
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color colorFondo = isEnabled() ? fondo : new Color(36, 61, 96);
            Color colorTexto = isEnabled() ? texto : new Color(160, 175, 200);

            g2.setColor(colorFondo);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(principal ? AMARILLO : AMARILLO);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

            g2.dispose();
            setForeground(colorTexto);
            super.paintComponent(g);
        }
    }

    private static class RoundedPanel extends JPanel {
        private final Color color;
        private final int radio;

        RoundedPanel(Color color, int radio) {
            this.color = color;
            this.radio = radio;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            if (radio > 0) {
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radio, radio);
            } else {
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class ComboCINEXUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton boton = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(AZUL_CARD_2);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(AMARILLO);
                    int x = getWidth() / 2;
                    int y = getHeight() / 2;
                    int[] xs = {x - 7, x + 7, x};
                    int[] ys = {y - 4, y - 4, y + 7};
                    g2.fillPolygon(xs, ys, 3);
                    g2.dispose();
                }
            };
            boton.setBorder(null);
            boton.setContentAreaFilled(false);
            boton.setFocusPainted(false);
            boton.setOpaque(false);
            return boton;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            g.setColor(AZUL_CARD_2);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            ListCellRenderer renderer = comboBox.getRenderer();
            Component c = renderer.getListCellRendererComponent(
                    listBox,
                    comboBox.getSelectedItem(),
                    -1,
                    false,
                    false
            );
            c.setBackground(AZUL_CARD_2);
            c.setForeground(BLANCO);
            currentValuePane.paintComponent(
                    g,
                    c,
                    comboBox,
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    true
            );
        }
    }

    private static class GridLayoutCINEX extends java.awt.GridLayout {
        GridLayoutCINEX(int rows, int cols, int hgap, int vgap) {
            super(rows, cols, hgap, vgap);
        }
    }
}
