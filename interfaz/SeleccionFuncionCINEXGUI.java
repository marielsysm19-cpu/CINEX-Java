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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import control.ControlConsultarFuncionesCINEX;
import entidad.PeliculaCINEX;
import entidad.FuncionCINEX;
import entidad.ReferenciaFuncionCINEX;

public class SeleccionFuncionCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_CARD = new Color(25, 45, 80);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS = new Color(185, 190, 200);

    private JLabel lblHora;
    private JLabel lblFecha;
    private String usuarioActual;
    private String peliculaSeleccionada;
    private String funcionSeleccionada = "";
    private String tipoSalaSeleccionada = "";
    private FuncionCard funcionActiva;
    private JPanel listaFuncionesPanel;
    private DatosPelicula datos;
    private final ArrayList<FuncionCINEX> funcionesBD = new ArrayList<>();
    private final ControlConsultarFuncionesCINEX controlFunciones =
            new ControlConsultarFuncionesCINEX();

    public SeleccionFuncionCINEXGUI() { this("taquillero", "Dune: Parte Dos"); }

    public SeleccionFuncionCINEXGUI(String usuario, String pelicula) {
        this.usuarioActual = usuario == null || usuario.trim().isEmpty() ? "taquillero" : usuario.trim();
        this.peliculaSeleccionada = pelicula == null ? "" : pelicula.trim();
        cargarFuncionesDesdeBD();
        setTitle("CINEX - Seleccionar función");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        CINEXResponsive.configurarVentana(this, 1280, 720, 1000, 650);

        JPanel fondo = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, AZUL_FONDO_1, getWidth(), getHeight(), AZUL_FONDO_2);
                g2.setPaint(gp); g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 8));
                g2.fillOval(-160, -80, 500, 500); g2.fillOval(getWidth() - 420, 220, 360, 360);
                g2.dispose();
            }
        };
        setContentPane(fondo);
        fondo.add(crearHeader(), BorderLayout.NORTH);
        fondo.add(crearContenido(), BorderLayout.CENTER);
        actualizarFechaHora();
        new Timer(1000, e -> actualizarFechaHora()).start();
    }

    private void cargarFuncionesDesdeBD() {
        funcionesBD.clear();
        funcionesBD.addAll(
                controlFunciones.verificarFunciones(
                        peliculaSeleccionada
                )
        );

        if (!funcionesBD.isEmpty()) {
            FuncionCINEX funcion = funcionesBD.get(0);

            datos = new DatosPelicula(
                    funcion.getPelicula(),
                    funcion.getImagen(),
                    funcion.getTipoSala(),
                    funcion.getDuracionMinutos() + " min",
                    funcion.getClasificacion(),
                    funcion.getGenero()
            );

            return;
        }

        /*
         * Cuando no existen funciones, se recuperan directamente
         * los datos de la película para conservar su póster.
         */
        PeliculaCINEX pelicula =
                controlFunciones.obtenerPeliculaPorTitulo(
                        peliculaSeleccionada
                );

        if (pelicula != null) {
            datos = new DatosPelicula(
                    pelicula.getTitulo(),
                    pelicula.getImagen(),
                    "-",
                    pelicula.getDuracion() + " min",
                    pelicula.getClasificacion(),
                    pelicula.getGenero()
            );

            return;
        }

        datos = obtenerDatosPeliculaRespaldo(
                peliculaSeleccionada
        );
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false); header.setBorder(new EmptyBorder(15, 25, 15, 25));
        JLabel logo = new JLabel(); logo.setIcon(cargarImagen("imagenes/logocinex.png", 315, 115));
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 5)); infoPanel.setOpaque(false);
        JLabel lblUsuario = new JLabel("Usuario: " + usuarioActual); lblUsuario.setForeground(BLANCO); lblUsuario.setFont(new Font("Arial", Font.BOLD, 16));
        JLabel lblTerminal = new JLabel("Terminal: 01"); lblTerminal.setForeground(BLANCO); lblTerminal.setFont(new Font("Arial", Font.BOLD, 16));
        lblHora = new JLabel(); lblHora.setForeground(BLANCO); lblHora.setFont(new Font("Arial", Font.BOLD, 16));
        lblFecha = new JLabel(); lblFecha.setForeground(BLANCO); lblFecha.setFont(new Font("Arial", Font.BOLD, 16));
        infoPanel.add(lblUsuario); infoPanel.add(lblTerminal); infoPanel.add(lblHora); infoPanel.add(lblFecha);
        header.add(logo, BorderLayout.WEST); header.add(infoPanel, BorderLayout.EAST); return header;
    }

    private JPanel crearContenido() {
        JPanel contenido = new JPanel(new BorderLayout()); contenido.setOpaque(false);
        contenido.add(new SidebarCINEX(2), BorderLayout.WEST);
        JPanel mainWrapper = new JPanel(new GridBagLayout()); mainWrapper.setOpaque(false);
        JPanel main = new JPanel(new BorderLayout(45, 0)); main.setOpaque(false);
        main.setPreferredSize(CINEXResponsive.pantallaPequena() ? new Dimension(1000, 520) : new Dimension(1120, 530));
        main.setBorder(new EmptyBorder(20, 20, 20, 20));
        main.add(crearPanelPelicula(), BorderLayout.WEST); main.add(crearPanelFunciones(), BorderLayout.CENTER);
        mainWrapper.add(CINEXResponsive.envolverConScroll(main)); contenido.add(mainWrapper, BorderLayout.CENTER); contenido.add(crearFooter(), BorderLayout.SOUTH);
        return contenido;
    }

    private JPanel crearPanelPelicula() {
        JPanel panel = new JPanel(); panel.setOpaque(false); panel.setPreferredSize(new Dimension(310, 480)); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        PosterPanel poster = new PosterPanel(datos.imagen); poster.setPreferredSize(new Dimension(230, 310)); poster.setMaximumSize(new Dimension(230, 310)); poster.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblTitulo = label(datos.titulo, 22, Font.BOLD, BLANCO); JLabel lblFormato = label(datos.formato, 16, Font.BOLD, BLANCO);
        JLabel lblDuracion = label("Duración: " + datos.duracion, 15, Font.PLAIN, GRIS); JLabel lblClasificacion = label("Clasificación: " + datos.clasificacion, 15, Font.PLAIN, GRIS); JLabel lblDescripcion = label(datos.descripcion, 15, Font.PLAIN, GRIS);
        panel.add(poster); panel.add(Box.createVerticalStrut(18)); panel.add(lblTitulo); panel.add(Box.createVerticalStrut(8)); panel.add(lblFormato); panel.add(Box.createVerticalStrut(8)); panel.add(lblDuracion); panel.add(Box.createVerticalStrut(8)); panel.add(lblClasificacion); panel.add(Box.createVerticalStrut(25)); panel.add(lblDescripcion);
        return panel;
    }

    private JLabel label(String texto, int size, int style, Color color) { JLabel l = new JLabel(texto); l.setForeground(color); l.setFont(new Font("Arial", style, size)); l.setAlignmentX(Component.LEFT_ALIGNMENT); return l; }

    private JPanel crearPanelFunciones() {
        JPanel panel = new JPanel(); panel.setOpaque(false); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel titulo = label("Seleccionar función", 27, Font.BOLD, BLANCO); JLabel subtitulo = label("Funciones programadas para hoy y mañana", 16, Font.PLAIN, GRIS);
        JSeparator separador = new JSeparator(); separador.setMaximumSize(new Dimension(560, 2)); separador.setForeground(new Color(70, 100, 145)); separador.setAlignmentX(Component.LEFT_ALIGNMENT);
        listaFuncionesPanel = new JPanel(); listaFuncionesPanel.setOpaque(false); listaFuncionesPanel.setLayout(new BoxLayout(listaFuncionesPanel, BoxLayout.Y_AXIS)); listaFuncionesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (funcionesBD.isEmpty()) {
            listaFuncionesPanel.add(crearMensajeSinFunciones());
        } else {
            String ultimoGrupo = "";

            for (FuncionCINEX funcion : funcionesBD) {
                String grupo = obtenerGrupoFecha(funcion);

                if (!grupo.equals(ultimoGrupo)) {
                    if (!ultimoGrupo.isEmpty()) {
                        listaFuncionesPanel.add(
                                Box.createVerticalStrut(4)
                        );
                    }

                    JPanel separadorDia =
                            crearSeparadorDia(grupo);
                    separadorDia.setAlignmentX(
                            Component.LEFT_ALIGNMENT
                    );

                    listaFuncionesPanel.add(separadorDia);
                    listaFuncionesPanel.add(
                            Box.createVerticalStrut(8)
                    );

                    ultimoGrupo = grupo;
                }

                FuncionCard card = new FuncionCard(funcion);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.setMaximumSize(new Dimension(600, 82));

                listaFuncionesPanel.add(card);
                listaFuncionesPanel.add(
                        Box.createVerticalStrut(14)
                );
            }
        }
        JScrollPane scroll = new JScrollPane(listaFuncionesPanel); scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.setBorder(null); scroll.setPreferredSize(new Dimension(630, 300)); scroll.setMaximumSize(new Dimension(630, 300)); scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(Box.createVerticalStrut(35)); panel.add(titulo); panel.add(Box.createVerticalStrut(5)); panel.add(subtitulo); panel.add(Box.createVerticalStrut(8)); panel.add(separador); panel.add(Box.createVerticalStrut(30)); panel.add(scroll); return panel;
    }

    private String obtenerGrupoFecha(FuncionCINEX funcion) {
        if (funcion == null) {
            return "FUNCIONES";
        }

        LocalDate fecha = funcion.getFecha();
        if (fecha == null) {
            String fechaTexto = funcion.getFechaTexto();
            try {
                fecha = LocalDate.parse(fechaTexto, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception e) {
                return fechaTexto == null || fechaTexto.trim().isEmpty() ? "FUNCIONES" : fechaTexto;
            }
        }

        LocalDate hoy = LocalDate.now();
        if (fecha.equals(hoy)) return "HOY";
        if (fecha.equals(hoy.plusDays(1))) return "MAÑANA";
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private JPanel crearSeparadorDia(String texto) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(600, 22));
        panel.setPreferredSize(new Dimension(600, 22));

        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(AMARILLO);
        etiqueta.setFont(
                new Font("Arial", Font.BOLD, 12)
        );

        JSeparator linea = new JSeparator();
        linea.setForeground(new Color(70, 100, 145));
        linea.setBackground(new Color(70, 100, 145));

        panel.add(etiqueta, BorderLayout.WEST);
        panel.add(linea, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearMensajeSinFunciones() {
        RoundedPanel panel = new RoundedPanel(16, new Color(5, 18, 43, 190)); panel.setLayout(new GridBagLayout()); panel.setPreferredSize(new Dimension(560, 120)); panel.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));
        JLabel lbl = new JLabel("No existen funciones programadas"); lbl.setForeground(AMARILLO); lbl.setFont(new Font("Arial", Font.BOLD, 20)); panel.add(lbl); return panel;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout()); footer.setOpaque(false); footer.setBorder(new EmptyBorder(10, 25, 25, 25));
        JButton btnAtras = crearBotonSecundario("ATRÁS");
        JButton btnSiguiente = crearBotonPrincipal("SIGUIENTE");

        btnSiguiente.setEnabled(!funcionesBD.isEmpty());

        btnAtras.addActionListener(e ->
                CINEXTransiciones.cambiar(
                        this,
                        new VentaEntradasCINEXGUI(usuarioActual)
                )
        );

        btnSiguiente.addActionListener(e -> {
            if (funcionSeleccionada.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Seleccione una función para continuar.",
                        "Función no seleccionada",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            CINEXTransiciones.cambiar(
                    this,
                    new SeleccionAsientosCINEXGUI(
                            usuarioActual,
                            datos.titulo,
                            funcionSeleccionada,
                            tipoSalaSeleccionada
                    )
            );
        });
        footer.add(btnAtras, BorderLayout.WEST); footer.add(btnSiguiente, BorderLayout.EAST); return footer;
    }

    private void seleccionarFuncion(
            FuncionCard card,
            int idFuncion,
            String horaVisible,
            String tipoSala
    ) {
        funcionSeleccionada = ReferenciaFuncionCINEX.crear(
                idFuncion,
                horaVisible
        );

        tipoSalaSeleccionada = tipoSala == null
                || tipoSala.trim().isEmpty()
                ? datos.formato
                : tipoSala.trim();

        if (funcionActiva != null) {
            funcionActiva.setSeleccionado(false);
        }

        funcionActiva = card;
        funcionActiva.setSeleccionado(true);
    }

    private JButton crearBotonPrincipal(String texto) { JButton btn = new JButton(texto); btn.setPreferredSize(new Dimension(245, 62)); btn.setFont(new Font("Arial", Font.BOLD, 17)); CINEXResponsive.estabilizarBoton(btn, AMARILLO, Color.BLACK); return btn; }
    private JButton crearBotonSecundario(String texto) { return CINEXResponsive.botonSecundario(texto, 190, 58); }
    private void actualizarFechaHora() { LocalDateTime ahora = LocalDateTime.now(); lblHora.setText(ahora.format(DateTimeFormatter.ofPattern("hh:mm a"))); lblFecha.setText(ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))); }
    private DatosPelicula obtenerDatosPeliculaRespaldo(
            String pelicula
    ) {
        String titulo = pelicula == null
                || pelicula.trim().isEmpty()
                ? "Película"
                : pelicula.trim();

        /*
         * No se utiliza el póster de otra película.
         * Al no existir funciones se muestra el placeholder de CINEX.
         */
        return new DatosPelicula(
                titulo,
                "",
                "-",
                "-",
                "-",
                "Sin funciones programadas."
        );
    }

    private File resolverArchivoImagen(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }

        File archivo = new File(nombre.trim());

        if (archivo.exists() && archivo.isFile()) {
            return archivo;
        }

        String nombreArchivo =
                new File(nombre.trim()).getName();

        archivo = new File("imagenes", nombreArchivo);

        if (archivo.exists() && archivo.isFile()) {
            return archivo;
        }

        archivo = new File("Imagenes", nombreArchivo);

        if (archivo.exists() && archivo.isFile()) {
            return archivo;
        }

        return null;
    }

    private ImageIcon cargarImagen(
            String nombre,
            int ancho,
            int alto
    ) {
        try {
            File archivo = resolverArchivoImagen(nombre);

            if (archivo == null) {
                return new ImageIcon();
            }

            BufferedImage original = ImageIO.read(archivo);

            if (original == null) {
                return new ImageIcon();
            }

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
            g2.drawImage(
                    original,
                    0,
                    0,
                    ancho,
                    alto,
                    null
            );
            g2.dispose();

            return new ImageIcon(escalada);

        } catch (Exception e) {
            return new ImageIcon();
        }
    }

    private BufferedImage cargarBufferedImage(String nombre) {
        try {
            File archivo = resolverArchivoImagen(nombre);

            if (archivo == null) {
                return null;
            }

            return ImageIO.read(archivo);

        } catch (Exception e) {
            return null;
        }
    }
    private void dibujarImagenCover(Graphics2D g2, BufferedImage original, int x, int y, int ancho, int alto, int radio) { if (original == null) { g2.setColor(new Color(15, 30, 60)); g2.fillRoundRect(x, y, ancho, alto, radio, radio); return; } double escala = Math.max((double) ancho / original.getWidth(), (double) alto / original.getHeight()); int nuevoAncho = (int) Math.round(original.getWidth() * escala); int nuevoAlto = (int) Math.round(original.getHeight() * escala); int posX = x - (nuevoAncho - ancho) / 2; int posY = y - (nuevoAlto - alto) / 2; Shape clipAnterior = g2.getClip(); RoundRectangle2D clip = new RoundRectangle2D.Double(x, y, ancho, alto, radio, radio); g2.setClip(clip); g2.drawImage(original, posX, posY, nuevoAncho, nuevoAlto, null); g2.setClip(clipAnterior); }

    class FuncionCard extends JPanel {
        private final FuncionCINEX funcion;
        private boolean seleccionado = false;
        private boolean hover = false;

        public FuncionCard(FuncionCINEX funcion) {
            this.funcion = funcion;
            setOpaque(false);
            setPreferredSize(new Dimension(600, 82));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    seleccionarFuncion(
                            FuncionCard.this,
                            funcion.getIdFuncion(),
                            funcion.getHoraBD(),
                            funcion.getTipoSala()
                    );
                }

                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        public void setSeleccionado(boolean seleccionado) {
            this.seleccionado = seleccionado;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fondo = seleccionado ? AMARILLO : hover ? new Color(35, 60, 100) : AZUL_CARD;
            Color texto = seleccionado ? Color.BLACK : BLANCO;
            Color textoSec = seleccionado ? new Color(20, 20, 20) : GRIS;
            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 15, 15);
            g2.setColor(new Color(80, 105, 145));
            g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 15, 15);
            g2.setColor(texto);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.drawString(funcion.getHoraBD(), 25, 35);
            g2.setFont(new Font("Arial", Font.BOLD, 15));
            g2.drawString(funcion.getSala() + " | " + funcion.getTipoSala(), 160, 32);
            g2.setColor(textoSec);
            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            g2.drawString("Fecha: " + funcion.getFechaTexto(), 160, 58);
            g2.drawString("Disponibles: " + funcion.getDisponibles() + " / " + funcion.getCapacidad(), 360, 58);
            g2.dispose();
        }
    }
    class PosterPanel extends JPanel { private final BufferedImage poster; public PosterPanel(String imagen) { this.poster = cargarBufferedImage(imagen); setOpaque(false); } @Override protected void paintComponent(Graphics g) { super.paintComponent(g); Graphics2D g2 = (Graphics2D) g.create(); dibujarImagenCover(g2, poster, 0, 0, getWidth(), getHeight(), 8); g2.dispose(); } }
    class RoundedPanel extends JPanel { private final int radio; private final Color color; public RoundedPanel(int radio, Color color) { this.radio = radio; this.color = color; setOpaque(false); } @Override protected void paintComponent(Graphics g) { super.paintComponent(g); Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(color); g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, radio, radio)); g2.dispose(); } }
    class DatosPelicula { String titulo, imagen, formato, duracion, clasificacion, descripcion; public DatosPelicula(String titulo, String imagen, String formato, String duracion, String clasificacion, String descripcion) { this.titulo = titulo; this.imagen = imagen; this.formato = formato; this.duracion = duracion; this.clasificacion = clasificacion; this.descripcion = descripcion; } }
    public static void main(String[] args) { CINEXResponsive.iniciar(); SwingUtilities.invokeLater(() -> new SeleccionFuncionCINEXGUI("taquillero", "Dune: Parte Dos").setVisible(true)); }
}
