package interfaz;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MenuGerenteCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_TARJETA = new Color(24, 45, 85);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS = new Color(210, 210, 210);

    private JLabel lblHora;
    private JLabel lblFecha;
    private final String usuarioActual;

    public MenuGerenteCINEXGUI(String usuario) {
        usuarioActual = usuario == null || usuario.trim().isEmpty()
                ? "gerente"
                : usuario.trim();

        setTitle("CINEX - Menú Gerente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel fondo = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setPaint(new GradientPaint(
                        0,
                        0,
                        AZUL_FONDO_1,
                        getWidth(),
                        getHeight(),
                        AZUL_FONDO_2
                ));

                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillOval(80, 110, 480, 480);
                g2.fillOval(getWidth() - 420, 220, 360, 360);
                g2.dispose();
            }
        };

        setContentPane(fondo);
        fondo.add(crearHeader(), BorderLayout.NORTH);
        fondo.add(crearCentro(), BorderLayout.CENTER);

        actualizarFechaHora();
        new Timer(1000, e -> actualizarFechaHora()).start();
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(18, 28, 12, 28));

        JPanel logoPanel =
                new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoPanel.setOpaque(false);

        JLabel logo = new JLabel();
        logo.setIcon(cargarImagen(
                "imagenes/logocinex.png",
                315,
                115
        ));
        logoPanel.add(logo);

        JPanel info =
                new JPanel(new FlowLayout(FlowLayout.RIGHT, 35, 0));
        info.setOpaque(false);
        info.add(crearTexto("Usuario: " + usuarioActual));
        info.add(crearTexto("Terminal: 01"));

        lblHora = crearTexto("");
        lblFecha = crearTexto("");

        info.add(lblHora);
        info.add(lblFecha);

        header.add(logoPanel, BorderLayout.WEST);
        header.add(info, BorderLayout.EAST);
        return header;
    }

    private JPanel crearCentro() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setBorder(new EmptyBorder(0, 20, 30, 20));

        JLabel titulo = new JLabel("Menú Gerente");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 52));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitulo = new JLabel("Seleccione una opción");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 26));
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        centro.add(titulo);
        centro.add(Box.createVerticalStrut(10));
        centro.add(subtitulo);
        centro.add(Box.createVerticalStrut(35));

        JPanel grid = new JPanel(new GridLayout(2, 3, 24, 24));
        grid.setOpaque(false);
        grid.setPreferredSize(new Dimension(1080, 470));
        grid.setMaximumSize(new Dimension(1080, 470));

        MenuCard dashboard =
                new MenuCard("📊", "Dashboard", true);
        MenuCard reportes =
                new MenuCard("📄", "Reportes de ventas", false);
        MenuCard historial =
                new MenuCard("🎟", "Historial de ventas", false);
        MenuCard clientes =
                new MenuCard("👥", "Lista de clientes", false);
        MenuCard notificaciones =
                new MenuCard("🔔", "Notificaciones", false);
        MenuCard cerrar =
                new MenuCard("↩", "Cerrar Sesión", false);

        grid.add(dashboard);
        grid.add(reportes);
        grid.add(historial);
        grid.add(clientes);
        grid.add(notificaciones);
        grid.add(cerrar);

        centro.add(grid);
        wrapper.add(centro);

        dashboard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CINEXTransiciones.cambiar(
                        MenuGerenteCINEXGUI.this,
                        new DashboardGerenteCINEXGUI(usuarioActual)
                );
            }
        });

        reportes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CINEXTransiciones.cambiar(
                        MenuGerenteCINEXGUI.this,
                        new ReportesGerenteCINEXGUI(usuarioActual)
                );
            }
        });

        historial.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CINEXTransiciones.cambiar(
                        MenuGerenteCINEXGUI.this,
                        new VentasGerenteCINEXGUI(usuarioActual)
                );
            }
        });

        clientes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CINEXTransiciones.cambiar(
                        MenuGerenteCINEXGUI.this,
                        new ClientesGerenteCINEXGUI(usuarioActual)
                );
            }
        });

        notificaciones.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CINEXTransiciones.cambiar(
                        MenuGerenteCINEXGUI.this,
                        new NotificacionesGerenteCINEXGUI(usuarioActual)
                );
            }
        });

        cerrar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (CINEXResponsive.confirmarCerrarSesion(
                        MenuGerenteCINEXGUI.this
                )) {
                    CINEXTransiciones.cambiar(
                            MenuGerenteCINEXGUI.this,
                            new LoginCINEXGUI()
                    );
                }
            }
        });

        return wrapper;
    }

    private JLabel crearTexto(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(BLANCO);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        return label;
    }

    private void actualizarFechaHora() {
        LocalDateTime ahora = LocalDateTime.now();

        lblHora.setText(ahora.format(
                DateTimeFormatter.ofPattern("hh:mm a")
        ));

        lblFecha.setText(ahora.format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
        ));
    }

    private ImageIcon cargarImagen(
            String nombre,
            int ancho,
            int alto
    ) {
        try {
            File archivo = new File(nombre);

            if (!archivo.exists()) {
                File alternativo = new File(
                        "Imagenes/" + new File(nombre).getName()
                );

                if (alternativo.exists()) {
                    archivo = alternativo;
                }
            }

            if (!archivo.exists()) {
                return new ImageIcon();
            }

            BufferedImage original = ImageIO.read(archivo);
            BufferedImage escalada = new BufferedImage(
                    ancho,
                    alto,
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g2 = escalada.createGraphics();
            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            g2.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
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
        private boolean hover;

        MenuCard(String icono, String texto, boolean destacada) {
            this.icono = icono;
            this.texto = texto;
            this.destacada = destacada;

            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(330, 215);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Color fondo = destacada
                    ? (hover ? new Color(255, 214, 30) : AMARILLO)
                    : (hover ? new Color(36, 60, 110) : AZUL_TARJETA);

            Color colorTexto = destacada
                    ? new Color(5, 20, 55)
                    : BLANCO;

            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRoundRect(
                    10,
                    10,
                    getWidth() - 10,
                    getHeight() - 10,
                    28,
                    28
            );

            g2.setColor(fondo);
            g2.fillRoundRect(
                    0,
                    0,
                    getWidth() - 10,
                    getHeight() - 10,
                    28,
                    28
            );

            g2.setColor(new Color(255, 255, 255, 45));
            g2.drawRoundRect(
                    0,
                    0,
                    getWidth() - 11,
                    getHeight() - 11,
                    28,
                    28
            );

            g2.setColor(colorTexto);
            g2.setFont(new Font(
                    "Segoe UI Emoji",
                    Font.PLAIN,
                    58
            ));

            FontMetrics fmIcono = g2.getFontMetrics();
            int anchoIcono = fmIcono.stringWidth(icono);

            g2.drawString(
                    icono,
                    (getWidth() - 10 - anchoIcono) / 2,
                    82
            );

            g2.setFont(new Font(
                    "Arial",
                    Font.BOLD,
                    22
            ));

            FontMetrics fmTexto = g2.getFontMetrics();
            int anchoTexto = fmTexto.stringWidth(texto);

            g2.drawString(
                    texto,
                    (getWidth() - 10 - anchoTexto) / 2,
                    158
            );

            g2.dispose();
        }
    }
}
