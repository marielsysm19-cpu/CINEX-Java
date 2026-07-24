package interfaz;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import control.ControlConsultarPeliculasCINEX;
import entidad.PeliculaCINEX;


public class VentaEntradasCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_CARD = new Color(16, 20, 28);
    private final Color AZUL_CARD_HOVER = new Color(23, 38, 70);
    private final Color AZUL_BORDE = new Color(63, 96, 145);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 247, 252);

    private JLabel lblHora;
    private JLabel lblFecha;

    private String usuarioActual;
    private String peliculaSeleccionada = "";

    private JButton btnVerFunciones;

    private JPanel panelPeliculas;
    private final Map<String, MovieCard> tarjetas = new LinkedHashMap<>();

    private boolean hayPeliculasDisponibles = false;

    public VentaEntradasCINEXGUI() {
        this("taquillero");
    }

    public VentaEntradasCINEXGUI(String usuario) {
        this.usuarioActual = usuario;

        setTitle("CINEX - Elegir película");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        CINEXResponsive.configurarVentana(this, 1420, 830, 1000, 650);

        JPanel fondo = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();

                GradientPaint gp = new GradientPaint(
                        0, 0, AZUL_FONDO_1,
                        getWidth(), getHeight(), AZUL_FONDO_2
                );

                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(new Color(255, 255, 255, 7));
                g2.fillOval(-170, -90, 520, 520);
                g2.fillOval(getWidth() - 360, 230, 330, 330);
                g2.fillOval(getWidth() - 180, getHeight() - 230, 260, 260);

                g2.dispose();
            }
        };

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

        JLabel lblUsuario = crearHeaderLabel("Usuario: " + usuarioActual);
        JLabel lblTerminal = crearHeaderLabel("Terminal: 01");

        lblHora = crearHeaderLabel("");
        lblFecha = crearHeaderLabel("");

        info.add(lblUsuario);
        info.add(lblTerminal);
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

        contenido.add(new SidebarCINEX(1), BorderLayout.WEST);

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

        JLabel titulo = new JLabel("Elegir película");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 26));
        titulo.setBounds(32, 18, 500, 34);
        panel.add(titulo);

        JLabel descripcion = new JLabel("Visualiza las películas disponibles en cartelera.");
        descripcion.setForeground(new Color(210, 218, 232));
        descripcion.setFont(new Font("Arial", Font.PLAIN, 16));
        descripcion.setBounds(32, 58, 850, 25);
        panel.add(descripcion);

        JLabel ayuda = new JLabel("Elija una película para continuar con las funciones programadas.");
        ayuda.setForeground(new Color(200, 210, 225));
        ayuda.setFont(new Font("Arial", Font.PLAIN, 14));
        ayuda.setBounds(32, 84, 850, 22);
        panel.add(ayuda);

        return panel;
    }

    private JPanel crearTituloPeliculas() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(2000, 40));

        JLabel titulo = new JLabel("Elegir película");
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

        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(70, 95, 135);
                trackColor = new Color(4, 18, 45);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return crearBotonScrollbar();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return crearBotonScrollbar();
            }

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

        ArrayList<PeliculaCINEX> peliculasDisponibles =
                ControlConsultarPeliculasCINEX.solicitarPeliculasDisponibles();

        if (peliculasDisponibles != null && !peliculasDisponibles.isEmpty()) {
            hayPeliculasDisponibles = true;
            panelPeliculas.setLayout(new GridLayout(0, 4, 24, 24));

            for (PeliculaCINEX pelicula : peliculasDisponibles) {
                agregarPelicula(
                        panelPeliculas,
                        pelicula.getTitulo(),
                        pelicula.getGenero(),
                        pelicula.getDuracion() + " min",
                        pelicula.getImagen()
                );
            }

        } else {
            hayPeliculasDisponibles = false;
            panelPeliculas.setLayout(new GridBagLayout());

            JLabel mensaje = new JLabel(
                    "<html><center>No existen películas disponibles<br>en cartelera</center></html>"
            );
            mensaje.setForeground(BLANCO);
            mensaje.setFont(new Font("Arial", Font.BOLD, 28));

            panelPeliculas.add(mensaje);

            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    this,
                    "No existen películas disponibles en cartelera",
                    "Consulta de películas",
                    JOptionPane.INFORMATION_MESSAGE
            ));
        }

        panelPeliculas.revalidate();
        panelPeliculas.repaint();
    }

    private void agregarPelicula(JPanel contenedor, String titulo, String genero, String duracion, String imagen) {
        MovieCard card = new MovieCard(titulo, genero, duracion, imagen);
        tarjetas.put(titulo, card);
        contenedor.add(card);
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 330, 20, 52));

        JButton btnAtras = crearBotonSecundario("←   ATRÁS");
        btnAtras.addActionListener(e -> {
            CINEXTransiciones.cambiar(this, new RegistroClienteCINEXGUI(usuarioActual));
        });

        btnVerFunciones = crearBotonPrincipal("VER FUNCIONES   →");
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
        JButton btn = CINEXResponsive.botonSecundario(texto, 195, 58);
        return btn;
    }

    private void irAFunciones() {
        if (!hayPeliculasDisponibles) {
            JOptionPane.showMessageDialog(
                    this,
                    "No existen películas disponibles en cartelera",
                    "Consulta de películas",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        if (peliculaSeleccionada == null || peliculaSeleccionada.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Elija una película para continuar.",
                    "Película requerida",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        CINEXTransiciones.cambiar(
                this,
                new SeleccionFuncionCINEXGUI(
                        usuarioActual,
                        peliculaSeleccionada
                )
        );
    }

    private void seleccionarPelicula(String titulo) {
        peliculaSeleccionada = titulo;

        for (MovieCard card : tarjetas.values()) {
            card.setSeleccionada(card.getTitulo().equals(titulo));
        }
    }

    private void actualizarFechaHora() {
        LocalDateTime ahora = LocalDateTime.now();

        lblHora.setText(ahora.format(DateTimeFormatter.ofPattern("hh:mm a")));
        lblFecha.setText(ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private ImageIcon cargarImagen(String ruta, int ancho, int alto) {
        try {
            if (ruta == null || ruta.trim().isEmpty() || "null".equalsIgnoreCase(ruta.trim())) {
                return crearPosterPlaceholder(ancho, alto);
            }

            File archivo = new File(ruta);

            if (!archivo.exists()) {
                String nombre = new File(ruta).getName();
                archivo = new File("imagenes/" + nombre);
            }

            if (!archivo.exists()) {
                String nombre = new File(ruta).getName();
                archivo = new File("Imagenes/" + nombre);
            }

            if (!archivo.exists()) {
                return crearPosterPlaceholder(ancho, alto);
            }

            BufferedImage original = ImageIO.read(archivo);

            if (original == null) {
                return crearPosterPlaceholder(ancho, alto);
            }

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

            Shape oldClip = g2.getClip();

            g2.setClip(0, 0, ancho, alto);
            g2.drawImage(original, x, y, nuevoAncho, nuevoAlto, null);
            g2.setClip(oldClip);

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
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int arc = 14;

        g2.setColor(new Color(6, 20, 50));
        g2.fillRoundRect(0, 0, ancho - 1, alto - 1, arc, arc);

        g2.setColor(new Color(86, 118, 170));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(1, 1, ancho - 3, alto - 3, arc, arc);

        g2.setColor(new Color(255, 255, 255, 8));
        g2.fillOval(-ancho / 2, -alto / 3, ancho, alto);
        g2.fillOval(ancho - ancho / 2, alto - alto / 3, ancho, alto);

        int centroY = alto / 2;

        g2.setColor(AMARILLO);
        g2.setFont(new Font("Arial", Font.BOLD, Math.max(28, ancho / 4)));
        FontMetrics fmLetra = g2.getFontMetrics();
        String letra = "C";
        g2.drawString(letra, (ancho - fmLetra.stringWidth(letra)) / 2, centroY - 18);

        g2.setFont(new Font("Arial", Font.BOLD, Math.max(11, ancho / 11)));
        FontMetrics fmCinex = g2.getFontMetrics();
        String cinex = "CINEX";
        g2.drawString(cinex, (ancho - fmCinex.stringWidth(cinex)) / 2, centroY + 20);

        g2.dispose();

        return new ImageIcon(img);
    }

    class MovieCard extends JPanel {

        private final String titulo;
        private final String genero;
        private final String duracion;
        private final String imagen;

        private boolean seleccionada = false;
        private boolean hover = false;

        public MovieCard(String titulo, String genero, String duracion, String imagen) {
            this.titulo = titulo;
            this.genero = genero;
            this.duracion = duracion;
            this.imagen = imagen;

            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(230, 365));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    seleccionarPelicula(titulo);
                }

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

        public String getTitulo() {
            return titulo;
        }

        public void setSeleccionada(boolean seleccionada) {
            this.seleccionada = seleccionada;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 14;
            int w = getWidth();
            int h = getHeight();

            Color fondo = seleccionada ? new Color(30, 48, 80) : AZUL_CARD;

            if (hover && !seleccionada) {
                fondo = AZUL_CARD_HOVER;
            }

            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRoundRect(4, 5, w - 8, h - 6, arc, arc);

            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, w - 8, h - 8, arc, arc);

            g2.setColor(seleccionada ? AMARILLO : new Color(45, 62, 88));
            g2.setStroke(new BasicStroke(seleccionada ? 3 : 1));
            g2.drawRoundRect(0, 0, w - 9, h - 9, arc, arc);

            ImageIcon poster = cargarImagen(imagen, w - 22, 245);
            poster.paintIcon(this, g2, 11, 11);

            g2.setColor(new Color(20, 20, 20));
            g2.fillRect(0, 256, w - 8, h - 264);

            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.setColor(BLANCO);
            dibujarTextoMultilinea(g2, titulo, 15, 292, w - 35, 20, 2);

            g2.setFont(new Font("Arial", Font.PLAIN, 13));
            g2.setColor(new Color(218, 224, 236));
            g2.drawString(genero + "  •  " + duracion, 15, h - 35);

            if (seleccionada) {
                g2.setColor(AMARILLO);
                g2.fillRoundRect(15, h - 68, 130, 25, 7, 7);

                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                g2.drawString("SELECCIONADA", 27, h - 51);
            }

            g2.dispose();
        }

        private void dibujarTextoMultilinea(
                Graphics2D g2,
                String texto,
                int x,
                int y,
                int maxWidth,
                int lineHeight,
                int maxLines
        ) {
            FontMetrics fm = g2.getFontMetrics();

            String[] palabras = texto.split(" ");
            String linea = "";
            int lineas = 0;

            for (String palabra : palabras) {
                String prueba = linea.isEmpty() ? palabra : linea + " " + palabra;

                if (fm.stringWidth(prueba) <= maxWidth) {
                    linea = prueba;
                } else {
                    if (!linea.isEmpty()) {
                        g2.drawString(linea, x, y + lineas * lineHeight);
                        lineas++;
                    }

                    linea = palabra;

                    if (lineas == maxLines - 1) {
                        break;
                    }
                }
            }

            if (!linea.isEmpty() && lineas < maxLines) {
                g2.drawString(linea, x, y + lineas * lineHeight);
            }
        }
    }

    class StepItem extends JPanel {

        private final String texto;
        private final boolean activo;

        public StepItem(String texto, boolean activo) {
            this.texto = texto;
            this.activo = activo;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (activo) {
                g2.setColor(AMARILLO);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Color.BLACK);
            } else {
                g2.setColor(new Color(9, 31, 69));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(178, 188, 205));
            }

            g2.setFont(new Font("Arial", Font.BOLD, 15));
            g2.drawString(texto, 20, 34);

            g2.dispose();
        }
    }

    class RoundedPanel extends JPanel {

        private final int radio;
        private final Color color;

        public RoundedPanel(int radio, Color color) {
            this.radio = radio;
            this.color = color;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            RoundRectangle2D round = new RoundRectangle2D.Double(
                    0,
                    0,
                    getWidth() - 1,
                    getHeight() - 1,
                    radio,
                    radio
            );

            g2.setColor(color);
            g2.fill(round);

            g2.dispose();

            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        CINEXResponsive.iniciar();
        SwingUtilities.invokeLater(() -> new VentaEntradasCINEXGUI("taquillero").setVisible(true));
    }
}