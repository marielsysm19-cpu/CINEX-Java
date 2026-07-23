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

public class MenuTaquilleroCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_TARJETA = new Color(24, 45, 85);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS_CLARO = new Color(210, 210, 210);

    private JLabel lblHora;
    private JLabel lblFecha;
    private final String usuarioActual;

    public MenuTaquilleroCINEXGUI(String usuario) {
        this.usuarioActual = usuario == null || usuario.trim().isEmpty() ? "taquillero" : usuario.trim();

        setTitle("CINEX - Menú " + usuarioActual.toUpperCase());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        CINEXResponsive.configurarVentana(this, 1280, 720, 1000, 650);

        JPanel fondo = new JPanel(new BorderLayout()) {
            @Override
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
        fondo.add(crearHeader(), BorderLayout.NORTH);
        fondo.add(crearCentro(), BorderLayout.CENTER);

        actualizarFechaHora();
        new Timer(1000, e -> actualizarFechaHora()).start();
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(18, 28, 12, 28));

        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoPanel.setOpaque(false);

        JLabel lblLogo = new JLabel();
        lblLogo.setIcon(cargarImagen("imagenes/logocinex.png", 315, 115));
        logoPanel.add(lblLogo);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 35, 0));
        infoPanel.setOpaque(false);

        JLabel lblUsuario = crearTextoHeader("Usuario: " + usuarioActual);
        JLabel lblTerminal = crearTextoHeader("Terminal: 01");
        lblHora = crearTextoHeader("");
        lblFecha = crearTextoHeader("");

        infoPanel.add(lblUsuario);
        infoPanel.add(lblTerminal);
        infoPanel.add(lblHora);
        infoPanel.add(lblFecha);

        header.add(logoPanel, BorderLayout.WEST);
        header.add(infoPanel, BorderLayout.EAST);
        return header;
    }

    private JLabel crearTextoHeader(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(BLANCO);
        lbl.setFont(new Font("Arial", Font.BOLD, 16));
        return lbl;
    }

    private JPanel crearCentro() {
        JPanel centroWrapper = new JPanel(new GridBagLayout());
        centroWrapper.setOpaque(false);

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setBorder(new EmptyBorder(0, 20, 30, 20));

        JLabel titulo = new JLabel("Menú Principal");
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
        gridPanel.setPreferredSize(new Dimension(1050, 470));
        gridPanel.setMaximumSize(new Dimension(1050, 470));

        MenuCard btnVenta = new MenuCard("🎟", "Venta de entrada", true);
        MenuCard btnConsultarPeliculas = new MenuCard("🎬", "Consultar película", false);
        MenuCard btnRegistroCliente = new MenuCard("👤", "Búsqueda o registro de cliente", false);
        MenuCard btnHistorial = new MenuCard("🧾", "Historial de compras", false);
        MenuCard btnReembolso = new MenuCard("💵", "Reembolso de entradas", false);
        MenuCard btnCerrar = new MenuCard("↩", "Cerrar Sesión", false);

        gridPanel.add(btnVenta);
        gridPanel.add(btnConsultarPeliculas);
        gridPanel.add(btnRegistroCliente);
        gridPanel.add(btnHistorial);
        gridPanel.add(btnReembolso);
        gridPanel.add(btnCerrar);

        centro.add(gridPanel);
        centroWrapper.add(centro);

        btnVenta.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CINEXTransiciones.cambiar(MenuTaquilleroCINEXGUI.this, new RegistroClienteCINEXGUI(usuarioActual));
            }
        });

        btnConsultarPeliculas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CINEXTransiciones.cambiar(MenuTaquilleroCINEXGUI.this, new ConsultarPeliculasCINEXGUI(usuarioActual));
            }
        });

        btnRegistroCliente.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CINEXTransiciones.cambiar(MenuTaquilleroCINEXGUI.this, new RegistroClienteCU4CINEXGUI(usuarioActual));
            }
        });

        btnHistorial.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CINEXTransiciones.cambiar(MenuTaquilleroCINEXGUI.this, new HistorialClientesCINEXGUI(usuarioActual));
            }
        });

        btnReembolso.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CINEXTransiciones.cambiar(
                        MenuTaquilleroCINEXGUI.this,
                        new ReembolsoEntradasCINEXGUI(usuarioActual)
                );
            }
        });

        btnCerrar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (CINEXResponsive.confirmarCerrarSesion(
                        MenuTaquilleroCINEXGUI.this
                )) {
                    CINEXTransiciones.cambiar(
                            MenuTaquilleroCINEXGUI.this,
                            new LoginCINEXGUI()
                    );
                }
            }
        });

        return centroWrapper;
    }

    private void actualizarFechaHora() {
        LocalDateTime ahora = LocalDateTime.now();
        lblHora.setText(ahora.format(DateTimeFormatter.ofPattern("hh:mm a")));
        lblFecha.setText(ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private ImageIcon cargarImagen(String nombre, int ancho, int alto) {
        try {
            File archivo = new File(nombre);
            if (!archivo.exists()) {
                File alternativo = new File("Imagenes/" + new File(nombre).getName());
                if (alternativo.exists()) archivo = alternativo;
            }
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
            return new Dimension(330, 220);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color fondoCard;
            Color colorTexto;
            Color colorIcono;

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
            g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
            FontMetrics fmIcon = g2.getFontMetrics();
            int iconW = fmIcon.stringWidth(icono);
            g2.drawString(icono, (getWidth() - 10 - iconW) / 2, 82);

            g2.setColor(colorTexto);
            g2.setFont(new Font("Arial", Font.BOLD, texto.length() > 23 ? 20 : 23));
            dibujarTextoCentrado(g2, texto, 0, 150, getWidth() - 10, 30);
            g2.dispose();
        }

        private void dibujarTextoCentrado(Graphics2D g2, String texto, int x, int y, int ancho, int lineHeight) {
            if (texto == null || texto.trim().isEmpty()) return;

            FontMetrics fm = g2.getFontMetrics();
            if (fm.stringWidth(texto) <= ancho - 24) {
                int textW = fm.stringWidth(texto);
                g2.drawString(texto, x + (ancho - textW) / 2, y);
                return;
            }

            String[] palabras = texto.split(" ");
            String linea1 = "";
            String linea2 = "";

            for (String palabra : palabras) {
                String prueba = linea1.isEmpty() ? palabra : linea1 + " " + palabra;
                if (fm.stringWidth(prueba) <= ancho - 24) {
                    linea1 = prueba;
                } else {
                    linea2 = linea2.isEmpty() ? palabra : linea2 + " " + palabra;
                }
            }

            int y1 = y - 12;
            int y2 = y + lineHeight - 12;
            int w1 = fm.stringWidth(linea1);
            g2.drawString(linea1, x + (ancho - w1) / 2, y1);

            if (!linea2.isEmpty()) {
                int w2 = fm.stringWidth(linea2);
                g2.drawString(linea2, x + (ancho - w2) / 2, y2);
            }
        }
    }

    public static void main(String[] args) {
        CINEXResponsive.iniciar();
        SwingUtilities.invokeLater(() -> new MenuTaquilleroCINEXGUI("taquillero").setVisible(true));
    }
}
