package interfaz;

import control.ControlConsultarFuncionesCINEX;
import entidad.FuncionCINEX;
import entidad.PeliculaCINEX;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ConsultarFuncionesCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_CARD = new Color(25, 45, 80);
    private final Color AZUL_CARD_HOVER = new Color(33, 60, 105);
    private final Color AZUL_BORDE = new Color(80, 105, 145);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS = new Color(185, 190, 200);
    private final Color ROJO = new Color(210, 65, 65);

    private JLabel lblHora;
    private JLabel lblFecha;
    private JLabel lblEstado;
    private JPanel listaFuncionesPanel;
    private JButton btnVerAsientos;

    private final String usuarioActual;
    private final PeliculaCINEX peliculaSeleccionada;
    private FuncionCINEX funcionSeleccionada;
    private FuncionCard funcionActiva;
    private final ArrayList<FuncionCINEX> funciones = new ArrayList<>();

    public ConsultarFuncionesCINEXGUI(String usuario, PeliculaCINEX pelicula) {
        this.usuarioActual = usuario == null || usuario.trim().isEmpty() ? "taquillero" : usuario.trim();
        this.peliculaSeleccionada = pelicula == null ? new PeliculaCINEX(0, "Película") : pelicula;
        cargarFunciones();

        setTitle("CINEX - Consultar funciones");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        CINEXResponsive.configurarVentana(this, 1280, 720, 1000, 650);

        JPanel fondo = new FondoPanel();
        fondo.setLayout(new BorderLayout());
        setContentPane(fondo);

        fondo.add(crearHeader(), BorderLayout.NORTH);
        fondo.add(crearContenido(), BorderLayout.CENTER);
        fondo.add(crearFooter(), BorderLayout.SOUTH);

        actualizarFechaHora();
        new Timer(1000, e -> actualizarFechaHora()).start();
    }

    private void cargarFunciones() {
        funciones.clear();
        funciones.addAll(ControlConsultarFuncionesCINEX.consultarFuncionesPorPelicula(peliculaSeleccionada));
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(15, 25, 15, 25));

        JLabel logo = new JLabel();
        logo.setIcon(cargarImagen("imagenes/logocinex.png", 315, 115));

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 5));
        infoPanel.setOpaque(false);
        infoPanel.add(crearHeaderLabel("Usuario: " + usuarioActual));
        infoPanel.add(crearHeaderLabel("Terminal: 01"));
        lblHora = crearHeaderLabel("");
        lblFecha = crearHeaderLabel("");
        infoPanel.add(lblHora);
        infoPanel.add(lblFecha);

        header.add(logo, BorderLayout.WEST);
        header.add(infoPanel, BorderLayout.EAST);
        return header;
    }

    private JLabel crearHeaderLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(BLANCO);
        lbl.setFont(new Font("Arial", Font.BOLD, 16));
        return lbl;
    }

    private JPanel crearContenido() {
        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setOpaque(false);
        contenido.add(new SidebarConsultaCINEX(1), BorderLayout.WEST);

        JPanel mainWrapper = new JPanel(new GridBagLayout());
        mainWrapper.setOpaque(false);

        JPanel main = new JPanel(new BorderLayout(45, 0));
        main.setOpaque(false);
        main.setPreferredSize(CINEXResponsive.pantallaPequena() ? new Dimension(1000, 520) : new Dimension(1120, 530));
        main.setBorder(new EmptyBorder(20, 20, 20, 20));

        main.add(crearPanelPelicula(), BorderLayout.WEST);
        main.add(crearPanelFunciones(), BorderLayout.CENTER);

        mainWrapper.add(CINEXResponsive.envolverConScroll(main));
        contenido.add(mainWrapper, BorderLayout.CENTER);
        return contenido;
    }

    private JPanel crearPanelPelicula() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(310, 480));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        PosterPanel poster = new PosterPanel(peliculaSeleccionada.getImagen());
        poster.setPreferredSize(new Dimension(230, 310));
        poster.setMaximumSize(new Dimension(230, 310));
        poster.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitulo = new JLabel(peliculaSeleccionada.getTitulo());
        lblTitulo.setForeground(BLANCO);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 22));
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblGenero = new JLabel(peliculaSeleccionada.getGenero());
        lblGenero.setForeground(GRIS);
        lblGenero.setFont(new Font("Arial", Font.PLAIN, 15));
        lblGenero.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDuracion = new JLabel("Duración: " + peliculaSeleccionada.getDuracion() + " min");
        lblDuracion.setForeground(GRIS);
        lblDuracion.setFont(new Font("Arial", Font.PLAIN, 15));
        lblDuracion.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(poster);
        panel.add(Box.createVerticalStrut(16));
        panel.add(lblTitulo);
        panel.add(Box.createVerticalStrut(8));
        panel.add(lblGenero);
        panel.add(Box.createVerticalStrut(4));
        panel.add(lblDuracion);
        return panel;
    }

    private JPanel crearPanelFunciones() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Consultar funciones");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 27));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitulo = new JLabel("Venta disponible hasta 10 minutos después del inicio; muestra hoy y mañana");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 16));
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblEstado = new JLabel(funciones.isEmpty()
                ? "No existen funciones programadas"
                : "Seleccione una función solicitada por el cliente.");
        lblEstado.setForeground(funciones.isEmpty() ? ROJO : GRIS);
        lblEstado.setFont(new Font("Arial", Font.BOLD, 14));
        lblEstado.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSeparator separador = new JSeparator();
        separador.setMaximumSize(new Dimension(620, 2));
        separador.setForeground(new Color(70, 100, 145));
        separador.setAlignmentX(Component.LEFT_ALIGNMENT);

        listaFuncionesPanel = new JPanel();
        listaFuncionesPanel.setOpaque(false);
        listaFuncionesPanel.setLayout(new BoxLayout(listaFuncionesPanel, BoxLayout.Y_AXIS));
        listaFuncionesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (funciones.isEmpty()) {
            listaFuncionesPanel.add(crearMensajeSinFunciones());
        } else {
            String ultimoGrupo = "";

            for (FuncionCINEX funcion : funciones) {
                String grupoFecha = obtenerGrupoFecha(funcion);

                if (!grupoFecha.equals(ultimoGrupo)) {
                    if (!ultimoGrupo.isEmpty()) {
                        listaFuncionesPanel.add(
                                Box.createVerticalStrut(4)
                        );
                    }

                    JPanel separadorDia =
                            crearSeparadorDia(grupoFecha);
                    separadorDia.setAlignmentX(
                            Component.LEFT_ALIGNMENT
                    );

                    listaFuncionesPanel.add(separadorDia);
                    listaFuncionesPanel.add(
                            Box.createVerticalStrut(8)
                    );

                    ultimoGrupo = grupoFecha;
                }

                FuncionCard card = new FuncionCard(funcion);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.setMaximumSize(new Dimension(620, 90));

                listaFuncionesPanel.add(card);
                listaFuncionesPanel.add(
                        Box.createVerticalStrut(14)
                );
            }
        }

        JScrollPane scroll = new JScrollPane(listaFuncionesPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(650, 315));
        scroll.setMaximumSize(new Dimension(650, 315));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(Box.createVerticalStrut(35));
        panel.add(titulo);
        panel.add(Box.createVerticalStrut(5));
        panel.add(subtitulo);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblEstado);
        panel.add(Box.createVerticalStrut(10));
        panel.add(separador);
        panel.add(Box.createVerticalStrut(24));
        panel.add(scroll);
        return panel;
    }

    private String obtenerGrupoFecha(FuncionCINEX funcion) {
        if (funcion == null
                || funcion.getFechaTexto() == null
                || funcion.getFechaTexto().trim().isEmpty()) {
            return "FUNCIONES";
        }

        String fechaTexto = funcion.getFechaTexto().trim();

        try {
            LocalDate fecha = LocalDate.parse(
                    fechaTexto,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
            );

            LocalDate hoy = LocalDate.now();

            if (fecha.equals(hoy)) {
                return "HOY";
            }

            if (fecha.equals(hoy.plusDays(1))) {
                return "MAÑANA";
            }

            return fecha.format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
            );

        } catch (Exception e) {
            return fechaTexto;
        }
    }

    private JPanel crearSeparadorDia(String texto) {
        JPanel panel = new JPanel(
                new BorderLayout(10, 0)
        );

        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(620, 22));
        panel.setMaximumSize(new Dimension(620, 22));

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
        RoundedPanel panel = new RoundedPanel(16, new Color(5, 18, 43, 190));
        panel.setLayout(new GridBagLayout());
        panel.setPreferredSize(new Dimension(600, 120));
        panel.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        JLabel lbl = new JLabel("No existen funciones programadas");
        lbl.setForeground(AMARILLO);
        lbl.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(lbl);
        return panel;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 25, 25, 25));

        JButton btnAtras = crearBotonSecundario("ATRÁS");
        btnAtras.addActionListener(e -> CINEXTransiciones.cambiar(this, new ConsultarPeliculasCINEXGUI(usuarioActual)));

        btnVerAsientos = crearBotonPrincipal("CONSULTAR ASIENTOS");
        btnVerAsientos.setEnabled(!funciones.isEmpty());
        btnVerAsientos.setToolTipText(
                funciones.isEmpty()
                        ? "No hay funciones disponibles para hoy ni mañana"
                        : "Seleccione una función para consultar asientos"
        );
        btnVerAsientos.addActionListener(e -> irAAsientos());

        footer.add(btnAtras, BorderLayout.WEST);
        footer.add(btnVerAsientos, BorderLayout.EAST);
        return footer;
    }

    private void irAAsientos() {
        if (funcionSeleccionada == null) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione una función para continuar.",
                    "Función no seleccionada",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!ControlConsultarFuncionesCINEX
                .funcionDisponibleParaVenta(
                        funcionSeleccionada.getIdFuncion()
                )) {
            JOptionPane.showMessageDialog(
                    this,
                    "La función ya no está disponible para venta.\n\n"
                            + "Solo se pueden vender entradas hasta "
                            + "10 minutos después de la hora de inicio.",
                    "Función no disponible",
                    JOptionPane.WARNING_MESSAGE
            );

            CINEXTransiciones.cambiar(
                    this,
                    new ConsultarFuncionesCINEXGUI(
                            usuarioActual,
                            peliculaSeleccionada
                    )
            );
            return;
        }

        CINEXTransiciones.cambiar(
                this,
                new ConsultarDisponibilidadAsientosCINEXGUI(
                        usuarioActual,
                        peliculaSeleccionada,
                        funcionSeleccionada
                )
        );
    }

    private void seleccionarFuncion(FuncionCard card, FuncionCINEX funcion) {
        funcionSeleccionada = funcion;
        if (funcionActiva != null) funcionActiva.setSeleccionada(false);
        funcionActiva = card;
        funcionActiva.setSeleccionada(true);
        lblEstado.setText("Función seleccionada: " + funcion.resumenCorto());
        lblEstado.setForeground(AMARILLO);
    }

    private JButton crearBotonPrincipal(String texto) {
        JButton btn = new JButton(texto);
        btn.setPreferredSize(new Dimension(245, 62));
        btn.setFont(new Font("Arial", Font.BOLD, 17));
        CINEXResponsive.estabilizarBoton(btn, AMARILLO, Color.BLACK);
        return btn;
    }

    private JButton crearBotonSecundario(String texto) {
        return CINEXResponsive.botonSecundario(texto, 190, 58);
    }

    private ImageIcon cargarImagen(String nombre, int ancho, int alto) {
        try {
            if (nombre == null || nombre.trim().isEmpty()) return new ImageIcon(crearPosterPlaceholder(ancho, alto));
            File archivo = new File(nombre);
            if (!archivo.exists()) archivo = new File("imagenes/" + new File(nombre).getName());
            if (!archivo.exists()) archivo = new File("Imagenes/" + new File(nombre).getName());
            if (!archivo.exists()) return new ImageIcon(crearPosterPlaceholder(ancho, alto));
            BufferedImage original = ImageIO.read(archivo);
            if (original == null) return new ImageIcon(crearPosterPlaceholder(ancho, alto));
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
            return new ImageIcon(crearPosterPlaceholder(ancho, alto));
        }
    }

    private BufferedImage crearPosterPlaceholder(int ancho, int alto) {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(6, 20, 50));
        g2.fillRoundRect(0, 0, ancho - 1, alto - 1, 14, 14);
        g2.setColor(new Color(86, 118, 170));
        g2.drawRoundRect(1, 1, ancho - 3, alto - 3, 14, 14);
        g2.setColor(AMARILLO);
        g2.setFont(new Font("Arial", Font.BOLD, Math.max(26, ancho / 4)));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("C", (ancho - fm.stringWidth("C")) / 2, alto / 2 - 18);
        g2.setFont(new Font("Arial", Font.BOLD, Math.max(11, ancho / 11)));
        fm = g2.getFontMetrics();
        g2.drawString("CINEX", (ancho - fm.stringWidth("CINEX")) / 2, alto / 2 + 20);
        g2.dispose();
        return img;
    }

    private void actualizarFechaHora() {
        LocalDateTime ahora = LocalDateTime.now();
        lblHora.setText(ahora.format(DateTimeFormatter.ofPattern("hh:mm a")));
        lblFecha.setText(ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    class FuncionCard extends JPanel {
        private final FuncionCINEX funcion;
        private boolean seleccionada = false;
        private boolean hover = false;

        public FuncionCard(FuncionCINEX funcion) {
            this.funcion = funcion;
            setPreferredSize(new Dimension(620, 90));
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { seleccionarFuncion(FuncionCard.this, funcion); }
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        public void setSeleccionada(boolean seleccionada) { this.seleccionada = seleccionada; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fondo = seleccionada ? new Color(35, 55, 90) : (hover ? AZUL_CARD_HOVER : AZUL_CARD);
            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 14, 14);
            g2.setColor(seleccionada ? AMARILLO : AZUL_BORDE);
            g2.setStroke(new BasicStroke(seleccionada ? 3 : 1));
            g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 14, 14);

            g2.setColor(seleccionada ? AMARILLO : BLANCO);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.drawString(funcion.getHoraBD(), 24, 35);

            g2.setColor(BLANCO);
            g2.setFont(new Font("Arial", Font.BOLD, 15));
            g2.drawString(funcion.getSala() + " | " + funcion.getTipoSala(), 160, 32);

            g2.setColor(GRIS);
            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            g2.drawString("Fecha: " + funcion.getFechaTexto(), 160, 58);
            g2.drawString("Disponibles: " + funcion.getDisponibles() + " / " + funcion.getCapacidad(), 360, 58);
            g2.dispose();
        }
    }

    class PosterPanel extends JPanel {
        private final ImageIcon poster;
        public PosterPanel(String imagen) { setOpaque(false); poster = cargarImagen(imagen, 230, 310); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            poster.paintIcon(this, g, 0, 0);
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
            g2.setColor(new Color(255, 255, 255, 8));
            g2.fillOval(-160, -80, 500, 500);
            g2.fillOval(getWidth() - 420, 220, 360, 360);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ConsultarFuncionesCINEXGUI("taquillero", new PeliculaCINEX(0, "Dune: Parte Dos")).setVisible(true));
    }
}
