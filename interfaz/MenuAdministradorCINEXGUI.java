package interfaz;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import control.BDCINEX;


public class MenuAdministradorCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_TARJETA = new Color(24, 45, 85);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS_CLARO = new Color(210, 210, 210);

    private JLabel lblHora;
    private JLabel lblFecha;
    private String usuarioActual;

    public MenuAdministradorCINEXGUI(String usuario) {
        this.usuarioActual = usuario;

        setTitle("CINEX - Menú Administrador");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel fondo = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, AZUL_FONDO_1, getWidth(), getHeight(), AZUL_FONDO_2);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillOval(80, 110, 480, 480);
                g2.fillOval(getWidth() - 420, 220, 360, 360);
                g2.dispose();
            }
        };
        setContentPane(fondo);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(18, 28, 18, 28));

        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoPanel.setOpaque(false);

        JLabel lblLogo = new JLabel();
        lblLogo.setIcon(cargarImagen("imagenes/logocinex.png", 315, 115));
        logoPanel.add(lblLogo);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 35, 0));
        infoPanel.setOpaque(false);

        JLabel lblUsuario = crearTexto("Usuario: " + usuarioActual);
        JLabel lblTerminal = crearTexto("Terminal: 01");
        lblHora = crearTexto("");
        lblFecha = crearTexto("");

        infoPanel.add(lblUsuario);
        infoPanel.add(lblTerminal);
        infoPanel.add(lblHora);
        infoPanel.add(lblFecha);

        header.add(logoPanel, BorderLayout.WEST);
        header.add(infoPanel, BorderLayout.EAST);
        fondo.add(header, BorderLayout.NORTH);

        actualizarFechaHora();
        new Timer(1000, e -> actualizarFechaHora()).start();

        JPanel centroWrapper = new JPanel(new GridBagLayout());
        centroWrapper.setOpaque(false);

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setBorder(new EmptyBorder(10, 20, 30, 20));

        JLabel titulo = new JLabel("Menú Administrador");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 52));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitulo = new JLabel("Seleccione una opción");
        subtitulo.setForeground(GRIS_CLARO);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 26));
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        centro.add(titulo);
        centro.add(Box.createVerticalStrut(10));
        centro.add(subtitulo);
        centro.add(Box.createVerticalStrut(35));

        JPanel gridPanel = new JPanel(new GridLayout(2, 3, 24, 24));
        gridPanel.setOpaque(false);
        gridPanel.setMaximumSize(new Dimension(1100, 520));
        gridPanel.setPreferredSize(new Dimension(1100, 520));

        MenuCard btnPeliculas = new MenuCard("🎬", "Registrar película", true);
        MenuCard btnPrecios = new MenuCard("🏷", "Configurar precios", false);
        MenuCard btnFunciones = new MenuCard("⚙", "Programar funciones", false);
        MenuCard btnUsuarios = new MenuCard("👥", "Agregar usuarios", false);
        MenuCard btnCerrar = new MenuCard("↩", "Cerrar Sesión", false);

        gridPanel.add(btnPeliculas);
        gridPanel.add(btnPrecios);
        gridPanel.add(btnFunciones);
        gridPanel.add(btnUsuarios);
        gridPanel.add(btnCerrar);

        centro.add(gridPanel);
        centroWrapper.add(centro);
        fondo.add(centroWrapper, BorderLayout.CENTER);

        btnPeliculas.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                new GestionPeliculasAdminCINEXGUI(usuarioActual).setVisible(true);
                dispose();
            }
        });

        btnPrecios.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                new PreciosAdminCINEXGUI(usuarioActual).setVisible(true);
                dispose();
            }
        });

        btnFunciones.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                new FuncionesAdminCINEXGUI(usuarioActual).setVisible(true);
                dispose();
            }
        });

        btnUsuarios.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                new GestionUsuariosAdminCINEXGUI(usuarioActual).setVisible(true);
                dispose();
            }
        });

        btnCerrar.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (CINEXResponsive.confirmarCerrarSesion(
                        MenuAdministradorCINEXGUI.this
                )) {
                    CINEXTransiciones.cambiar(
                            MenuAdministradorCINEXGUI.this,
                            new LoginCINEXGUI()
                    );
                }
            }
        });
    }

    private JLabel crearTexto(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(BLANCO);
        lbl.setFont(new Font("Arial", Font.BOLD, 16));
        return lbl;
    }

    private void actualizarFechaHora() {
        LocalDateTime ahora = LocalDateTime.now();
        lblHora.setText(ahora.format(DateTimeFormatter.ofPattern("hh:mm a")));
        lblFecha.setText(ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private ImageIcon cargarImagen(String nombre, int ancho, int alto) {
        try {
            File archivo = new File(nombre);
            if (!archivo.exists()) archivo = new File("logocinex.png");
            if (!archivo.exists()) return new ImageIcon();

            BufferedImage original = ImageIO.read(archivo);
            BufferedImage escalada = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = escalada.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(original, 0, 0, ancho, alto, null);
            g2.dispose();
            return new ImageIcon(escalada);
        } catch (Exception e) {
            return new ImageIcon();
        }
    }

    class MenuCard extends JPanel {
        private final String icono;
        private final String texto;
        private final boolean destacada;
        private boolean hover = false;

        public MenuCard(String icono, String texto, boolean destacada) {
            this.icono = icono;
            this.texto = texto;
            this.destacada = destacada;
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        public Dimension getPreferredSize() {
            return new Dimension(320, 230);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color fondoCard, colorTexto, colorIcono;

            if (destacada) {
                fondoCard = hover ? new Color(255, 214, 30) : AMARILLO;
                colorTexto = new Color(5, 20, 55);
                colorIcono = new Color(5, 20, 55);
            } else {
                fondoCard = hover ? new Color(36, 60, 110) : AZUL_TARJETA;
                colorTexto = BLANCO;
                colorIcono = BLANCO;
            }

            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRoundRect(10, 10, getWidth() - 10, getHeight() - 10, 28, 28);
            g2.setColor(fondoCard);
            g2.fillRoundRect(0, 0, getWidth() - 10, getHeight() - 10, 28, 28);
            g2.setColor(new Color(255, 255, 255, 45));
            g2.drawRoundRect(0, 0, getWidth() - 11, getHeight() - 11, 28, 28);

            g2.setColor(colorIcono);
            g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
            FontMetrics fmIcon = g2.getFontMetrics();
            int iconW = fmIcon.stringWidth(icono);
            g2.drawString(icono, (getWidth() - 10 - iconW) / 2, 90);

            g2.setColor(colorTexto);
            g2.setFont(new Font("Arial", Font.BOLD, texto.length() > 20 ? 20 : 23));
            dibujarTextoCentrado(g2, texto, 0, 157, getWidth() - 10, 28);

            g2.dispose();
        }
        private void dibujarTextoCentrado(Graphics2D g2, String texto, int x, int y, int ancho, int altoLinea) {
            FontMetrics fm = g2.getFontMetrics();
            int limite = ancho - 24;
            if (fm.stringWidth(texto) <= limite) {
                int w = fm.stringWidth(texto);
                g2.drawString(texto, x + (ancho - w) / 2, y + 10);
                return;
            }

            String[] palabras = texto.split(" ");
            String linea1 = "";
            String linea2 = "";
            for (String palabra : palabras) {
                String prueba = linea1.isEmpty() ? palabra : linea1 + " " + palabra;
                if (fm.stringWidth(prueba) <= limite && linea2.isEmpty()) {
                    linea1 = prueba;
                } else {
                    linea2 = linea2.isEmpty() ? palabra : linea2 + " " + palabra;
                }
            }
            int w1 = fm.stringWidth(linea1);
            int w2 = fm.stringWidth(linea2);
            g2.drawString(linea1, x + (ancho - w1) / 2, y);
            g2.drawString(linea2, x + (ancho - w2) / 2, y + altoLinea);
        }

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MenuAdministradorCINEXGUI("admin01").setVisible(true));
    }
}
