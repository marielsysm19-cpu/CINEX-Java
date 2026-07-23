package interfaz;

import control.ControlConsultarPeliculasCINEX;
import entidad.PeliculaCINEX;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConsultarPeliculasCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_CARD = new Color(16, 20, 28);
    private final Color AZUL_CARD_HOVER = new Color(23, 38, 70);
    private final Color AZUL_BORDE = new Color(63, 96, 145);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 247, 252);
    private final Color GRIS = new Color(200, 210, 225);

    private JLabel lblHora;
    private JLabel lblFecha;
    private JLabel lblEstado;
    private JButton btnVerFunciones;
    private JPanel panelPeliculas;

    private final String usuarioActual;
    private PeliculaCINEX peliculaSeleccionada;
    private final Map<Integer, MovieCard> tarjetas = new LinkedHashMap<>();
    private boolean hayPeliculasDisponibles = false;

    public ConsultarPeliculasCINEXGUI() {
        this("taquillero");
    }

    public ConsultarPeliculasCINEXGUI(String usuario) {
        this.usuarioActual = usuario == null || usuario.trim().isEmpty() ? "taquillero" : usuario.trim();

        setTitle("CINEX - Consultar película");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        CINEXResponsive.configurarVentana(this, 1420, 830, 1000, 650);

        JPanel fondo = new FondoPanel();
        fondo.setLayout(new BorderLayout());
        setContentPane(fondo);

        fondo.add(crearHeader(), BorderLayout.NORTH);
        fondo.add(crearContenido(), BorderLayout.CENTER);
        fondo.add(crearFooter(), BorderLayout.SOUTH);

        actualizarFechaHora();
        new Timer(1000, e -> actualizarFechaHora()).start();
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 118));
        header.setBorder(new EmptyBorder(16, 50, 6, 55));

        JLabel logo = new JLabel();
        logo.setIcon(cargarImagen("imagenes/logocinex.png", 245, 85));
        header.add(logo, BorderLayout.WEST);

        JPanel info = new JPanel(new FlowLayout(FlowLayout.RIGHT, 28, 6));
        info.setOpaque(false);

        info.add(crearHeaderLabel("Usuario: " + usuarioActual));
        info.add(crearHeaderLabel("Terminal: 01"));
        lblHora = crearHeaderLabel("");
        lblFecha = crearHeaderLabel("");
        info.add(lblHora);
        info.add(lblFecha);

        header.add(info, BorderLayout.EAST);
        return header;
    }

    private JLabel crearHeaderLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(BLANCO);
        lbl.setFont(new Font("Arial", Font.BOLD, 15));
        return lbl;
    }

    private JPanel crearContenido() {
        JPanel contenido = new JPanel(new BorderLayout(28, 0));
        contenido.setOpaque(false);
        contenido.add(new SidebarConsultaCINEX(0), BorderLayout.WEST);

        JPanel centroWrapper = new JPanel(new BorderLayout());
        centroWrapper.setOpaque(false);
        centroWrapper.setBorder(new EmptyBorder(0, 45, 0, 45));

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));

        centro.add(crearPanelConsultaPeliculas());
        centro.add(Box.createVerticalStrut(16));
        centro.add(crearTituloPeliculas());
        centro.add(Box.createVerticalStrut(12));
        centro.add(crearScrollPeliculas());

        centroWrapper.add(centro, BorderLayout.CENTER);
        contenido.add(centroWrapper, BorderLayout.CENTER);
        return contenido;
    }

    private JPanel crearPanelConsultaPeliculas() {
        RoundedPanel panel = new RoundedPanel(18, new Color(4, 18, 45, 225));
        panel.setLayout(null);
        panel.setPreferredSize(CINEXResponsive.pantallaPequena() ? new Dimension(900, 115) : new Dimension(1080, 115));
        panel.setMaximumSize(new Dimension(2000, 115));
        panel.setBorder(new LineBorder(new Color(55, 84, 128), 1, true));

        JLabel titulo = new JLabel("Consultar película");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 26));
        titulo.setBounds(32, 18, 500, 34);
        panel.add(titulo);

        JLabel descripcion = new JLabel("El sistema muestra la interfaz de consulta de películas.");
        descripcion.setForeground(GRIS);
        descripcion.setFont(new Font("Arial", Font.PLAIN, 16));
        descripcion.setBounds(32, 58, 900, 25);
        panel.add(descripcion);

        lblEstado = new JLabel("Visualiza el listado de películas disponibles.");
        lblEstado.setForeground(GRIS);
        lblEstado.setFont(new Font("Arial", Font.BOLD, 14));
        lblEstado.setBounds(32, 84, 900, 22);
        panel.add(lblEstado);

        return panel;
    }

    private JPanel crearTituloPeliculas() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(2000, 40));

        JLabel titulo = new JLabel("Elija una película");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 30));
        panel.add(titulo);
        return panel;
    }

    private JScrollPane crearScrollPeliculas() {
        panelPeliculas = new JPanel(new GridLayout(0, 4, 24, 24));
        panelPeliculas.setOpaque(false);
        panelPeliculas.setBorder(new EmptyBorder(0, 0, 18, 18));

        cargarPeliculasDesdeBD();

        JScrollPane scroll = new JScrollPane(panelPeliculas);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setPreferredSize(CINEXResponsive.pantallaPequena() ? new Dimension(900, 420) : new Dimension(1080, 480));
        scroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(70, 95, 135);
                trackColor = new Color(4, 18, 45);
            }
            @Override protected JButton createDecreaseButton(int orientation) { return crearBotonScrollbar(); }
            @Override protected JButton createIncreaseButton(int orientation) { return crearBotonScrollbar(); }
            private JButton crearBotonScrollbar() {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(0, 0));
                btn.setMinimumSize(new Dimension(0, 0));
                btn.setMaximumSize(new Dimension(0, 0));
                return btn;
            }
        });
        return scroll;
    }

    private void cargarPeliculasDesdeBD() {
        tarjetas.clear();
        panelPeliculas.removeAll();
        ArrayList<PeliculaCINEX> peliculas = ControlConsultarPeliculasCINEX.solicitarPeliculasDisponibles();

        if (peliculas != null && !peliculas.isEmpty()) {
            hayPeliculasDisponibles = true;
            panelPeliculas.setLayout(new GridLayout(0, 4, 24, 24));
            for (PeliculaCINEX pelicula : peliculas) {
                MovieCard card = new MovieCard(pelicula);
                tarjetas.put(pelicula.getIdPelicula(), card);
                panelPeliculas.add(card);
            }
        } else {
            hayPeliculasDisponibles = false;
            panelPeliculas.setLayout(new GridBagLayout());
            JLabel mensaje = new JLabel("<html><center>No existen películas disponibles<br>en cartelera</center></html>");
            mensaje.setForeground(BLANCO);
            mensaje.setFont(new Font("Arial", Font.BOLD, 28));
            panelPeliculas.add(mensaje);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "No existen películas disponibles en cartelera",
                    "Consulta de películas",
                    JOptionPane.INFORMATION_MESSAGE));
        }
        panelPeliculas.revalidate();
        panelPeliculas.repaint();
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 330, 20, 52));

        JButton btnAtras = crearBotonSecundario("←   ATRÁS");
        btnAtras.addActionListener(e -> CINEXTransiciones.cambiar(this, new MenuTaquilleroCINEXGUI(usuarioActual)));

        btnVerFunciones = crearBotonPrincipal("CONSULTAR FUNCIONES   →");
        btnVerFunciones.setEnabled(hayPeliculasDisponibles);
        btnVerFunciones.addActionListener(e -> irAFunciones());

        footer.add(btnAtras, BorderLayout.WEST);
        footer.add(btnVerFunciones, BorderLayout.EAST);
        return footer;
    }

    private JButton crearBotonPrincipal(String texto) {
        JButton btn = new JButton(texto);
        btn.setPreferredSize(new Dimension(250, 60));
        btn.setFont(new Font("Arial", Font.BOLD, 15));
        CINEXResponsive.estabilizarBoton(btn, AMARILLO, Color.BLACK);
        return btn;
    }

    private JButton crearBotonSecundario(String texto) {
        return CINEXResponsive.botonSecundario(texto, 195, 58);
    }

    private void irAFunciones() {
        if (!hayPeliculasDisponibles) {
            JOptionPane.showMessageDialog(this,
                    "No existen películas disponibles en cartelera",
                    "Consulta de películas",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (peliculaSeleccionada == null || peliculaSeleccionada.getTitulo().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Elija una película para continuar.",
                    "Película requerida",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        CINEXTransiciones.cambiar(
                this,
                new ConsultarFuncionesCINEXGUI(
                        usuarioActual,
                        peliculaSeleccionada
                )
        );
    }

    private void seleccionarPelicula(PeliculaCINEX pelicula) {
        peliculaSeleccionada = pelicula;
        for (MovieCard card : tarjetas.values()) {
            card.setSeleccionada(card.getPelicula().getIdPelicula() == pelicula.getIdPelicula());
        }
        lblEstado.setText("Película seleccionada: " + pelicula.getTitulo());
        lblEstado.setForeground(AMARILLO);
    }

    private void actualizarFechaHora() {
        LocalDateTime ahora = LocalDateTime.now();
        lblHora.setText(ahora.format(DateTimeFormatter.ofPattern("hh:mm a")));
        lblFecha.setText(ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private ImageIcon cargarImagen(String ruta, int ancho, int alto) {
        try {
            if (ruta == null || ruta.trim().isEmpty() || "null".equalsIgnoreCase(ruta.trim())) return crearPosterPlaceholder(ancho, alto);
            File archivo = new File(ruta);
            if (!archivo.exists()) archivo = new File("imagenes/" + new File(ruta).getName());
            if (!archivo.exists()) archivo = new File("Imagenes/" + new File(ruta).getName());
            if (!archivo.exists()) return crearPosterPlaceholder(ancho, alto);

            BufferedImage original = ImageIO.read(archivo);
            if (original == null) return crearPosterPlaceholder(ancho, alto);
            BufferedImage escalada = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = escalada.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double ratio = Math.max((double) ancho / original.getWidth(), (double) alto / original.getHeight());
            int nuevoAncho = (int) (original.getWidth() * ratio);
            int nuevoAlto = (int) (original.getHeight() * ratio);
            int x = (ancho - nuevoAncho) / 2;
            int y = (alto - nuevoAlto) / 2;
            g2.setClip(0, 0, ancho, alto);
            g2.drawImage(original, x, y, nuevoAncho, nuevoAlto, null);
            g2.dispose();
            return new ImageIcon(escalada);
        } catch (Exception e) {
            return crearPosterPlaceholder(ancho, alto);
        }
    }

    private ImageIcon crearPosterPlaceholder(int ancho, int alto) {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(6, 20, 50));
        g2.fillRoundRect(0, 0, ancho - 1, alto - 1, 14, 14);
        g2.setColor(new Color(86, 118, 170));
        g2.drawRoundRect(1, 1, ancho - 3, alto - 3, 14, 14);
        g2.setColor(AMARILLO);
        g2.setFont(new Font("Arial", Font.BOLD, Math.max(28, ancho / 4)));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("C", (ancho - fm.stringWidth("C")) / 2, alto / 2 - 18);
        g2.setFont(new Font("Arial", Font.BOLD, Math.max(11, ancho / 11)));
        fm = g2.getFontMetrics();
        g2.drawString("CINEX", (ancho - fm.stringWidth("CINEX")) / 2, alto / 2 + 20);
        g2.dispose();
        return new ImageIcon(img);
    }

    class MovieCard extends JPanel {
        private final PeliculaCINEX pelicula;
        private boolean seleccionada = false;
        private boolean hover = false;

        public MovieCard(PeliculaCINEX pelicula) {
            this.pelicula = pelicula;
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(230, 365));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { seleccionarPelicula(pelicula); }
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        public PeliculaCINEX getPelicula() { return pelicula; }
        public void setSeleccionada(boolean seleccionada) { this.seleccionada = seleccionada; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            Color fondo = seleccionada ? new Color(30, 48, 80) : (hover ? AZUL_CARD_HOVER : AZUL_CARD);
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRoundRect(4, 5, w - 8, h - 6, 14, 14);
            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, w - 8, h - 8, 14, 14);
            g2.setColor(seleccionada ? AMARILLO : new Color(45, 62, 88));
            g2.setStroke(new BasicStroke(seleccionada ? 3 : 1));
            g2.drawRoundRect(0, 0, w - 9, h - 9, 14, 14);
            cargarImagen(pelicula.getImagen(), w - 22, 245).paintIcon(this, g2, 11, 11);
            g2.setColor(new Color(20, 20, 20));
            g2.fillRect(0, 256, w - 8, h - 264);
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.setColor(BLANCO);
            dibujarTextoMultilinea(g2, pelicula.getTitulo(), 15, 292, w - 35, 20, 2);
            g2.setFont(new Font("Arial", Font.PLAIN, 13));
            g2.setColor(new Color(218, 224, 236));
            g2.drawString(pelicula.getGenero() + "  •  " + pelicula.getDuracion() + " min", 15, h - 35);
            if (seleccionada) {
                g2.setColor(AMARILLO);
                g2.fillRoundRect(15, h - 68, 130, 25, 7, 7);
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                g2.drawString("SELECCIONADA", 27, h - 51);
            }
            g2.dispose();
        }

        private void dibujarTextoMultilinea(Graphics2D g2, String texto, int x, int y, int maxWidth, int lineHeight, int maxLines) {
            FontMetrics fm = g2.getFontMetrics();
            String[] palabras = (texto == null ? "" : texto).split(" ");
            String linea = "";
            int lineas = 0;
            for (String palabra : palabras) {
                String prueba = linea.isEmpty() ? palabra : linea + " " + palabra;
                if (fm.stringWidth(prueba) <= maxWidth) linea = prueba;
                else {
                    if (!linea.isEmpty()) { g2.drawString(linea, x, y + lineas * lineHeight); lineas++; }
                    linea = palabra;
                    if (lineas == maxLines - 1) break;
                }
            }
            if (!linea.isEmpty() && lineas < maxLines) g2.drawString(linea, x, y + lineas * lineHeight);
        }
    }

    class RoundedPanel extends JPanel {
        private final int radio;
        private final Color color;
        public RoundedPanel(int radio, Color color) { this.radio = radio; this.color = color; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, radio, radio));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class FondoPanel extends JPanel {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            GradientPaint gp = new GradientPaint(0, 0, AZUL_FONDO_1, getWidth(), getHeight(), AZUL_FONDO_2);
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 255, 255, 7));
            g2.fillOval(-170, -90, 520, 520);
            g2.fillOval(getWidth() - 360, 230, 330, 330);
            g2.fillOval(getWidth() - 180, getHeight() - 230, 260, 260);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        CINEXResponsive.iniciar();
        SwingUtilities.invokeLater(() -> new ConsultarPeliculasCINEXGUI("taquillero").setVisible(true));
    }
}
