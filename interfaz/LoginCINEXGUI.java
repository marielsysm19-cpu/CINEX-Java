package interfaz;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import control.ControlIniciarSesionCINEX;
import entidad.UsuarioCINEX;


public class LoginCINEXGUI extends JFrame {

    private PlaceholderTextField txtUsuario;
    private PlaceholderPasswordField txtPassword;
    private boolean mostrarPassword = false;

    private final Color AZUL_FONDO = new Color(2, 19, 51);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BORDE_INPUT = new Color(130, 145, 175);
    private final Color TEXTO_PLACEHOLDER = new Color(135, 142, 160);

    private JPanel panelDerechoWrapper;

    public LoginCINEXGUI() {
        CINEXResponsive.iniciar();

        setTitle("CINEX - Iniciar sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        CINEXResponsive.configurarVentana(this, 1340, 860, 900, 620);
        setResizable(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AZUL_FONDO);
        setContentPane(root);

        JPanel leftPanel = crearPanelIzquierdo();
        panelDerechoWrapper = crearPanelDerecho();

        root.add(leftPanel, BorderLayout.CENTER);
        root.add(panelDerechoWrapper, BorderLayout.EAST);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ajustarPanelDerecho();
            }
        });

        SwingUtilities.invokeLater(this::ajustarPanelDerecho);
    }

    private JPanel crearPanelIzquierdo() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(AZUL_FONDO);
        wrapper.setBorder(new EmptyBorder(28, 38, 28, 38));

        JPanel formulario = new JPanel();
        formulario.setOpaque(false);
        formulario.setLayout(new BoxLayout(formulario, BoxLayout.Y_AXIS));
        formulario.setPreferredSize(new Dimension(520, 650));
        formulario.setMaximumSize(new Dimension(560, 720));

        JLabel lblLogo = new JLabel(loadScaledIcon("imagenes/logocinex.png", 315, 115));
        lblLogo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitulo = new JLabel("Login");
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 30));
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedInputPanel usuarioPanel = new RoundedInputPanel();
        usuarioPanel.setLayout(new BorderLayout(16, 0));
        usuarioPanel.setBorder(new EmptyBorder(0, 20, 0, 18));
        usuarioPanel.setPreferredSize(new Dimension(500, 68));
        usuarioPanel.setMaximumSize(new Dimension(520, 68));
        usuarioPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel iconUsuario = new JLabel(loadScaledIcon("imagenes/icon_usuario.png", 26, 26));
        iconUsuario.setPreferredSize(new Dimension(32, 68));
        usuarioPanel.add(iconUsuario, BorderLayout.WEST);

        txtUsuario = new PlaceholderTextField("Usuario");
        usuarioPanel.add(txtUsuario, BorderLayout.CENTER);

        RoundedInputPanel passwordPanel = new RoundedInputPanel();
        passwordPanel.setLayout(new BorderLayout(16, 0));
        passwordPanel.setBorder(new EmptyBorder(0, 20, 0, 16));
        passwordPanel.setPreferredSize(new Dimension(500, 68));
        passwordPanel.setMaximumSize(new Dimension(520, 68));
        passwordPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel iconCandado = new JLabel(loadScaledIcon("imagenes/icon_candado.png", 26, 26));
        iconCandado.setPreferredSize(new Dimension(32, 68));
        passwordPanel.add(iconCandado, BorderLayout.WEST);

        txtPassword = new PlaceholderPasswordField("Contraseña");
        txtPassword.setEchoChar('•');
        passwordPanel.add(txtPassword, BorderLayout.CENTER);

        JButton btnOjo = new JButton(loadScaledIcon("imagenes/icon_ojo.png", 24, 24));
        btnOjo.setPreferredSize(new Dimension(42, 68));
        btnOjo.setBorderPainted(false);
        btnOjo.setContentAreaFilled(false);
        btnOjo.setFocusPainted(false);
        btnOjo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOjo.addActionListener(e -> togglePassword());
        passwordPanel.add(btnOjo, BorderLayout.EAST);

        JButton btnIngresar = new JButton("INGRESAR");
        btnIngresar.setPreferredSize(new Dimension(500, 72));
        btnIngresar.setMaximumSize(new Dimension(520, 72));
        btnIngresar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnIngresar.setFont(new Font("Arial", Font.BOLD, 22));
        CINEXResponsive.estabilizarBoton(btnIngresar, AMARILLO, Color.BLACK);
        btnIngresar.addActionListener(e -> iniciarSesion());
        CINEXResponsive.hacerBotonAccesible(btnIngresar, "Ingresar al sistema CINEX");


        formulario.add(lblLogo);
        formulario.add(Box.createVerticalStrut(32));
        formulario.add(lblTitulo);
        formulario.add(Box.createVerticalStrut(48));
        formulario.add(usuarioPanel);
        formulario.add(Box.createVerticalStrut(22));
        formulario.add(passwordPanel);
        formulario.add(Box.createVerticalStrut(56));
        formulario.add(btnIngresar);
        formulario.add(Box.createVerticalGlue());
       

        wrapper.add(formulario);

        txtUsuario.addActionListener(e -> iniciarSesion());
        txtPassword.addActionListener(e -> iniciarSesion());

        return wrapper;
    }

    private JPanel crearPanelDerecho() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(620, 0));
        wrapper.add(new RightImagePanel("imagenes/fondo_cine_login.png"), BorderLayout.CENTER);
        return wrapper;
    }

    private void ajustarPanelDerecho() {
        if (panelDerechoWrapper == null) return;

        int anchoVentana = getWidth();

        if (anchoVentana < 1050) {
            panelDerechoWrapper.setPreferredSize(new Dimension(0, 0));
            panelDerechoWrapper.setVisible(false);
        } else {
            int anchoDerecho = Math.max(420, (int) (anchoVentana * 0.48));
            panelDerechoWrapper.setPreferredSize(new Dimension(anchoDerecho, 0));
            panelDerechoWrapper.setVisible(true);
        }

        revalidate();
        repaint();
    }

    private void togglePassword() {
        mostrarPassword = !mostrarPassword;
        txtPassword.setEchoChar(mostrarPassword ? (char) 0 : '•');
    }

    private void iniciarSesion() {
        String usuario = txtUsuario.getRealText().trim().toLowerCase();
        String contrasena = new String(txtPassword.getPassword()).trim();

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ingrese usuario y contraseña.",
                    "Campos vacíos",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        ControlIniciarSesionCINEX controlLogin =
                new ControlIniciarSesionCINEX();
        UsuarioCINEX usuarioAutenticado =
                controlLogin.iniciarSesion(usuario, contrasena);

        if (usuarioAutenticado == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Usuario o contraseña incorrectos, o usuario inactivo.",
                    "Acceso denegado",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String rol = usuarioAutenticado.getRol();

        if (usuarioAutenticado.esGerente()
                && usuarioAutenticado.debeCambiarContrasena()) {
            boolean cambioRealizado =
                    CambiarContrasenaGerenteCINEXGUI.solicitarCambio(
                            this,
                            usuarioAutenticado
                    );

            if (!cambioRealizado) {
                return;
            }
        }

        switch (rol) {
            case "Administrador":
                CINEXTransiciones.cambiar(
                        this,
                        new MenuAdministradorCINEXGUI(usuario)
                );
                break;

            case "Gerente":
                CINEXTransiciones.cambiar(
                        this,
                        new MenuGerenteCINEXGUI(usuario)
                );
                break;

            case "Taquillero":
                CINEXTransiciones.cambiar(
                        this,
                        new MenuTaquilleroCINEXGUI(usuario)
                );
                break;

            default:
                JOptionPane.showMessageDialog(
                        this,
                        "Rol no reconocido.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                break;
        }
    }

    private ImageIcon loadScaledIcon(String path, int width, int height) {
        try {
            BufferedImage img = ImageIO.read(new File(path));
            if (img == null) return new ImageIcon();

            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(img, 0, 0, width, height, null);
            g2.dispose();
            return new ImageIcon(scaled);
        } catch (Exception e) {
            System.out.println("No se pudo cargar la imagen: " + path);
            return new ImageIcon();
        }
    }

    class RightImagePanel extends JPanel {
        private BufferedImage image;

        public RightImagePanel(String imagePath) {
            setOpaque(false);
            try {
                image = ImageIO.read(new File(imagePath));
            } catch (Exception e) {
                System.out.println("No se pudo cargar fondo: " + imagePath);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(AZUL_FONDO);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (image != null) {
                g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
                g2.setColor(new Color(2, 19, 51, 65));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            g2.dispose();
        }
    }

    class RoundedInputPanel extends JPanel {
        public RoundedInputPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 25));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.setColor(BORDE_INPUT);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class PlaceholderTextField extends JTextField {
        private final String placeholder;

        public PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(new EmptyBorder(0, 0, 0, 0));
            setForeground(Color.WHITE);
            setCaretColor(Color.WHITE);
            setFont(new Font("Arial", Font.PLAIN, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(TEXTO_PLACEHOLDER);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, 0, y);
                g2.dispose();
            }
        }

        public String getRealText() {
            return getText();
        }
    }

    class PlaceholderPasswordField extends JPasswordField {
        private final String placeholder;

        public PlaceholderPasswordField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(new EmptyBorder(0, 0, 0, 0));
            setForeground(Color.WHITE);
            setCaretColor(Color.WHITE);
            setFont(new Font("Arial", Font.PLAIN, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getPassword().length == 0 && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(TEXTO_PLACEHOLDER);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, 0, y);
                g2.dispose();
            }
        }
    }

    public static void main(String[] args) {
        CINEXResponsive.iniciar();
        SwingUtilities.invokeLater(() -> new LoginCINEXGUI().setVisible(true));
    }
}
