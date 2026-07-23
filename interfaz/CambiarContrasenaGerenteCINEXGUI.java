package interfaz;

import javax.imageio.ImageIO;
import control.ControlGestionarUsuariosCINEX;
import control.ControlGestionarUsuariosCINEX.ResultadoCambioContrasena;
import entidad.UsuarioCINEX;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class CambiarContrasenaGerenteCINEXGUI extends JDialog {

    private final Color AZUL_FONDO = new Color(3, 12, 30);
    private final Color AZUL_PANEL = new Color(7, 24, 56);
    private final Color AZUL_INPUT = new Color(8, 28, 65);
    private final Color AZUL_BORDE = new Color(63, 96, 145);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 247, 252);
    private final Color GRIS = new Color(185, 195, 210);
    private final Color ROJO = new Color(225, 70, 70);

    private final UsuarioCINEX usuario;
    private final ControlGestionarUsuariosCINEX control;

    private JPasswordField txtNueva;
    private JPasswordField txtConfirmar;
    private JLabel lblMensaje;
    private boolean cambioExitoso;

    private CambiarContrasenaGerenteCINEXGUI(
            Frame padre,
            UsuarioCINEX usuario
    ) {
        super(padre, "Crear contraseña personal", true);
        this.usuario = usuario;
        this.control = new ControlGestionarUsuariosCINEX();

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setSize(560, 470);
        setMinimumSize(new Dimension(520, 440));
        setLocationRelativeTo(padre);
        construirInterfaz();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JOptionPane.showMessageDialog(
                        CambiarContrasenaGerenteCINEXGUI.this,
                        "Debe crear una contraseña personal antes de ingresar al sistema.",
                        "Cambio obligatorio",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });
    }

    public static boolean solicitarCambio(
            Frame padre,
            UsuarioCINEX usuario
    ) {
        CambiarContrasenaGerenteCINEXGUI dialogo =
                new CambiarContrasenaGerenteCINEXGUI(padre, usuario);
        dialogo.setVisible(true);
        return dialogo.cambioExitoso;
    }

    private void construirInterfaz() {
        JPanel fondo = new JPanel(new GridBagLayout());
        fondo.setBackground(AZUL_FONDO);
        fondo.setBorder(new EmptyBorder(24, 28, 24, 28));
        setContentPane(fondo);

        JPanel panel = new JPanel();
        panel.setBackground(AZUL_PANEL);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1, true),
                new EmptyBorder(25, 30, 25, 30)
        ));
        panel.setPreferredSize(new Dimension(470, 365));

        JLabel titulo = new JLabel("Cree su contraseña personal");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 24));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descripcion = new JLabel(
                "<html>Está ingresando con una contraseña temporal. "
                        + "Antes de continuar debe registrar una contraseña "
                        + "que solo usted conozca.</html>"
        );
        descripcion.setForeground(GRIS);
        descripcion.setFont(new Font("Arial", Font.PLAIN, 13));
        descripcion.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel usuarioLabel = new JLabel(
                "Gerente: " + usuario.getUsuario()
        );
        usuarioLabel.setForeground(AMARILLO);
        usuarioLabel.setFont(new Font("Arial", Font.BOLD, 14));
        usuarioLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtNueva = crearCampoPassword();
        txtConfirmar = crearCampoPassword();

        JPanel panelNueva = crearPanelPassword(txtNueva);
        JPanel panelConfirmar = crearPanelPassword(txtConfirmar);

        lblMensaje = new JLabel(
                "Use 8 caracteres, mayúscula, minúscula, número y símbolo."
        );
        lblMensaje.setForeground(GRIS);
        lblMensaje.setFont(new Font("Arial", Font.BOLD, 12));
        lblMensaje.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnGuardar = new JButton("GUARDAR CONTRASEÑA");
        btnGuardar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnGuardar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnGuardar.setPreferredSize(new Dimension(390, 46));
        btnGuardar.setBackground(AMARILLO);
        btnGuardar.setForeground(Color.BLACK);
        btnGuardar.setFont(new Font("Arial", Font.BOLD, 14));
        btnGuardar.setFocusPainted(false);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGuardar.addActionListener(e -> guardarContrasena());

        panel.add(titulo);
        panel.add(Box.createVerticalStrut(8));
        panel.add(descripcion);
        panel.add(Box.createVerticalStrut(12));
        panel.add(usuarioLabel);
        panel.add(Box.createVerticalStrut(17));
        panel.add(crearEtiqueta("Nueva contraseña"));
        panel.add(Box.createVerticalStrut(5));
        panel.add(panelNueva);
        panel.add(Box.createVerticalStrut(12));
        panel.add(crearEtiqueta("Confirmar contraseña"));
        panel.add(Box.createVerticalStrut(5));
        panel.add(panelConfirmar);
        panel.add(Box.createVerticalStrut(12));
        panel.add(lblMensaje);
        panel.add(Box.createVerticalGlue());
        panel.add(btnGuardar);

        fondo.add(panel);
        getRootPane().setDefaultButton(btnGuardar);
    }

    private JLabel crearEtiqueta(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(GRIS);
        label.setFont(new Font("Arial", Font.BOLD, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPasswordField crearCampoPassword() {
        JPasswordField campo = new JPasswordField();
        campo.setBackground(AZUL_INPUT);
        campo.setForeground(BLANCO);
        campo.setCaretColor(BLANCO);
        campo.setSelectionColor(AMARILLO);
        campo.setSelectedTextColor(Color.BLACK);
        campo.setFont(new Font("Arial", Font.BOLD, 14));
        campo.setBorder(new EmptyBorder(0, 10, 0, 8));
        return campo;
    }

    private JPanel crearPanelPassword(
            JPasswordField campo
    ) {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 42)
        );
        contenedor.setPreferredSize(
                new Dimension(390, 42)
        );
        contenedor.setBackground(AZUL_INPUT);
        contenedor.setBorder(
                new LineBorder(AZUL_BORDE, 1, true)
        );
        contenedor.setAlignmentX(Component.LEFT_ALIGNMENT);

        char caracterOculto = campo.getEchoChar();

        JToggleButton btnVer = new JToggleButton();
        btnVer.setPreferredSize(new Dimension(48, 40));
        btnVer.setMinimumSize(new Dimension(48, 40));
        btnVer.setMaximumSize(new Dimension(48, 40));
        btnVer.setIcon(crearIconoOjo(false));
        btnVer.setSelectedIcon(crearIconoOjo(true));
        btnVer.setBackground(AZUL_INPUT);
        btnVer.setForeground(AMARILLO);
        btnVer.setFocusPainted(false);
        btnVer.setBorderPainted(false);
        btnVer.setOpaque(true);
        btnVer.setContentAreaFilled(true);
        btnVer.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVer.setToolTipText("Mostrar contraseña");
        CINEXResponsive.estabilizarBoton(
                btnVer,
                AZUL_INPUT,
                AMARILLO,
                AZUL_INPUT,
                GRIS
        );

        btnVer.addActionListener(e -> {
            boolean mostrar = btnVer.isSelected();
            campo.setEchoChar(
                    mostrar
                            ? (char) 0
                            : caracterOculto
            );
            btnVer.setToolTipText(
                    mostrar
                            ? "Ocultar contraseña"
                            : "Mostrar contraseña"
            );
            campo.requestFocusInWindow();
        });

        contenedor.add(campo, BorderLayout.CENTER);
        contenedor.add(btnVer, BorderLayout.EAST);
        return contenedor;
    }

    private ImageIcon crearIconoOjo(
            boolean tachado
    ) {
        int tamano = 24;
        BufferedImage salida = new BufferedImage(
                tamano,
                tamano,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2 = salida.createGraphics();
        g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        BufferedImage original = null;

        String[] rutas = {
                "Imagenes/icon_ojo.png",
                "imagenes/icon_ojo.png",
                "icon_ojo.png"
        };

        for (String ruta : rutas) {
            try {
                File archivo = new File(ruta);

                if (archivo.exists()) {
                    original = ImageIO.read(archivo);
                    if (original != null) {
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (original != null) {
            g2.drawImage(
                    original,
                    1,
                    1,
                    tamano - 2,
                    tamano - 2,
                    null
            );
        } else {
            g2.setColor(AMARILLO);
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawOval(3, 7, 18, 10);
            g2.fillOval(9, 10, 6, 6);
        }

        if (tachado) {
            g2.setColor(AMARILLO);
            g2.setStroke(
                    new BasicStroke(
                            2.4f,
                            BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND
                    )
            );
            g2.drawLine(4, 4, 20, 20);
        }

        g2.dispose();
        return new ImageIcon(salida);
    }

    private void guardarContrasena() {
        String nueva = new String(txtNueva.getPassword()).trim();
        String confirmar = new String(txtConfirmar.getPassword()).trim();

        if (!nueva.equals(confirmar)) {
            mostrarError("Las contraseñas no coinciden.");
            txtConfirmar.setText("");
            txtConfirmar.requestFocusInWindow();
            return;
        }

        ResultadoCambioContrasena resultado =
                control.cambiarContrasenaPersonalGerente(
                        usuario.getIdUsuario(),
                        usuario.getUsuario(),
                        nueva
                );

        if (!resultado.fueExitoso()) {
            mostrarError(resultado.getMensaje());
            return;
        }

        cambioExitoso = true;
        usuario.setDebeCambiarContrasena(false);
        usuario.setContrasena(nueva);

        JOptionPane.showMessageDialog(
                this,
                resultado.getMensaje(),
                "Contraseña actualizada",
                JOptionPane.INFORMATION_MESSAGE
        );

        dispose();
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setForeground(ROJO);
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Contraseña no válida",
                JOptionPane.WARNING_MESSAGE
        );
    }
}
