package interfaz;

import control.ControlConsultarPlanoAsientosCINEX;
import control.ControlIdentificarDisponibilidadCINEX;
import entidad.AsientoCINEX;
import entidad.FuncionCINEX;
import entidad.PeliculaCINEX;
import entidad.SalaCINEX;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ConsultarDisponibilidadAsientosCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_PANEL = new Color(8, 24, 55);
    private final Color AZUL_BORDE = new Color(80, 105, 145);
    private final Color AZUL_DISPONIBLE = new Color(28, 100, 180);
    private final Color GRIS_OCUPADO = new Color(85, 98, 115);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS = new Color(185, 190, 200);
    private final Color VERDE = new Color(35, 180, 85);

    private JLabel lblHora;
    private JLabel lblFecha;
    private JLabel lblTituloSala;
    private JLabel lblResumen;
    private JLabel lblTotal;
    private JLabel lblDisponibles;
    private JLabel lblOcupados;
    private JPanel mapaPanel;

    private final String usuarioActual;
    private final PeliculaCINEX peliculaSeleccionada;
    private final FuncionCINEX funcionSeleccionada;
    private SalaCINEX salaActual;
    private final ArrayList<AsientoCINEX> asientosConsultados = new ArrayList<>();

    public ConsultarDisponibilidadAsientosCINEXGUI(String usuario, PeliculaCINEX pelicula, FuncionCINEX funcion) {
        this.usuarioActual = usuario == null || usuario.trim().isEmpty() ? "taquillero" : usuario.trim();
        this.peliculaSeleccionada = pelicula == null ? new PeliculaCINEX(0, "Película") : pelicula;
        this.funcionSeleccionada = funcion;

        setTitle("CINEX - Consultar disponibilidad de asientos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        CINEXResponsive.configurarVentana(this, 1366, 768, 1000, 650);

        JPanel fondo = new FondoPanel();
        fondo.setLayout(new BorderLayout());
        setContentPane(fondo);

        fondo.add(crearHeader(), BorderLayout.NORTH);
        fondo.add(crearContenido(), BorderLayout.CENTER);
        fondo.add(crearFooter(), BorderLayout.SOUTH);

        actualizarFechaHora();
        new Timer(1000, e -> actualizarFechaHora()).start();
        SwingUtilities.invokeLater(this::consultarDisponibilidad);
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(8, 25, 4, 25));

        JLabel logo = new JLabel();
        logo.setIcon(cargarImagen("imagenes/logocinex.png", 235, 82));

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 4));
        infoPanel.setOpaque(false);
        infoPanel.add(crearTextoHeader("Usuario: " + usuarioActual));
        infoPanel.add(crearTextoHeader("Terminal: 01"));
        lblHora = crearTextoHeader("");
        lblFecha = crearTextoHeader("");
        infoPanel.add(lblHora);
        infoPanel.add(lblFecha);

        header.add(logo, BorderLayout.WEST);
        header.add(infoPanel, BorderLayout.EAST);
        return header;
    }

    private JLabel crearTextoHeader(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(BLANCO);
        lbl.setFont(new Font("Arial", Font.BOLD, 16));
        return lbl;
    }

    private JPanel crearContenido() {
        JPanel contenido = new JPanel(new BorderLayout(26, 0));
        contenido.setOpaque(false);
        contenido.setBorder(new EmptyBorder(6, 24, 6, 24));
        contenido.add(new SidebarConsultaCINEX(2), BorderLayout.WEST);
        contenido.add(crearPanelConsulta(), BorderLayout.WEST);
        contenido.add(crearPanelMapa(), BorderLayout.CENTER);
        return contenido;
    }

    private JPanel crearPanelConsulta() {
        RoundedPanel panel = new RoundedPanel(20, new Color(5, 18, 43, 170));
        panel.setPreferredSize(CINEXResponsive.pantallaPequena() ? new Dimension(330, 0) : new Dimension(355, 0));
        panel.setLayout(null);
        panel.setBorder(new LineBorder(AZUL_BORDE, 1, true));

        JLabel titulo = new JLabel("<html>Consultar disponibilidad<br>de asientos</html>");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 24));
        titulo.setBounds(25, 22, 310, 58);
        panel.add(titulo);

        JLabel subtitulo = new JLabel("Plano de asientos de la función seleccionada.");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitulo.setBounds(25, 82, 310, 24);
        panel.add(subtitulo);

        RoundedPanel info = new RoundedPanel(16, new Color(8, 24, 55, 220));
        info.setLayout(null);
        info.setBounds(25, 128, 305, 230);
        info.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        panel.add(info);

        JLabel lblInfo = crearLabelInfo("Película:");
        lblInfo.setBounds(18, 18, 260, 22);
        info.add(lblInfo);

        JLabel lblPelicula = crearLabelDato("<html>" + peliculaSeleccionada.getTitulo() + "</html>");
        lblPelicula.setBounds(18, 43, 260, 48);
        info.add(lblPelicula);

        JLabel lblFuncion = crearLabelInfo("Función:");
        lblFuncion.setBounds(18, 98, 260, 22);
        info.add(lblFuncion);

        JLabel lblDetalleFuncion = crearLabelDato("<html>" + (funcionSeleccionada == null ? "-" : funcionSeleccionada.resumenCorto()) + "</html>");
        lblDetalleFuncion.setBounds(18, 123, 260, 55);
        info.add(lblDetalleFuncion);

        JLabel lblEstado = crearLabelDato("Información disponible para consulta");
        lblEstado.setForeground(VERDE);
        lblEstado.setBounds(18, 185, 260, 24);
        info.add(lblEstado);

        RoundedPanel resumen = new RoundedPanel(16, new Color(8, 24, 55, 220));
        resumen.setLayout(null);
        resumen.setBounds(25, 390, 305, 150);
        resumen.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        panel.add(resumen);

        JLabel lblCaja = new JLabel("Resumen actual");
        lblCaja.setForeground(BLANCO);
        lblCaja.setFont(new Font("Arial", Font.BOLD, 16));
        lblCaja.setBounds(18, 14, 220, 24);
        resumen.add(lblCaja);

        lblTotal = crearLabelResumen("Total: 0");
        lblTotal.setBounds(18, 48, 250, 22);
        resumen.add(lblTotal);

        lblDisponibles = crearLabelResumen("Disponibles: 0");
        lblDisponibles.setBounds(18, 75, 250, 22);
        resumen.add(lblDisponibles);

        lblOcupados = crearLabelResumen("Ocupados: 0");
        lblOcupados.setBounds(18, 102, 250, 22);
        resumen.add(lblOcupados);

        JPanel leyenda = crearLeyenda();
        leyenda.setBounds(25, 560, 305, 40);
        panel.add(leyenda);
        return panel;
    }

    private JLabel crearLabelInfo(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(GRIS);
        lbl.setFont(new Font("Arial", Font.BOLD, 13));
        return lbl;
    }

    private JLabel crearLabelDato(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(BLANCO);
        lbl.setFont(new Font("Arial", Font.BOLD, 14));
        return lbl;
    }

    private JLabel crearLabelResumen(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(BLANCO);
        lbl.setFont(new Font("Arial", Font.BOLD, 15));
        return lbl;
    }

    private JPanel crearPanelMapa() {
        RoundedPanel panel = new RoundedPanel(20, new Color(5, 18, 43, 170));
        panel.setLayout(new BorderLayout());
        panel.setBorder(new LineBorder(AZUL_BORDE, 1, true));

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        cabecera.setBorder(new EmptyBorder(18, 22, 8, 22));

        lblTituloSala = new JLabel("Plano de asientos");
        lblTituloSala.setForeground(BLANCO);
        lblTituloSala.setFont(new Font("Arial", Font.BOLD, 25));
        cabecera.add(lblTituloSala, BorderLayout.WEST);

        panel.add(cabecera, BorderLayout.NORTH);

        mapaPanel = new JPanel();
        mapaPanel.setOpaque(false);
        mapaPanel.setBorder(new EmptyBorder(18, 35, 18, 35));
        panel.add(mapaPanel, BorderLayout.CENTER);

        lblResumen = new JLabel("Consultando disponibilidad de asientos.", SwingConstants.CENTER);
        lblResumen.setForeground(GRIS);
        lblResumen.setFont(new Font("Arial", Font.BOLD, 14));
        lblResumen.setBorder(new EmptyBorder(8, 20, 18, 20));
        panel.add(lblResumen, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(8, 25, 18, 25));

        JButton btnAtras = CINEXResponsive.botonSecundario(
                "ATRÁS",
                195,
                58
        );

        btnAtras.addActionListener(e ->
                CINEXTransiciones.cambiar(
                        this,
                        new ConsultarFuncionesCINEXGUI(
                                usuarioActual,
                                peliculaSeleccionada
                        )
                )
        );

        JButton btnMenuPrincipal = CINEXResponsive.botonAmarillo(
                "MENÚ PRINCIPAL",
                230,
                58
        );

        btnMenuPrincipal.addActionListener(e ->
                CINEXTransiciones.cambiar(
                        this,
                        new MenuTaquilleroCINEXGUI(usuarioActual)
                )
        );

        footer.add(btnAtras, BorderLayout.WEST);
        footer.add(btnMenuPrincipal, BorderLayout.EAST);

        return footer;
    }

    private void consultarDisponibilidad() {
        if (funcionSeleccionada == null) {
            mostrarMensajeEnMapa("No hay funciones programadas", AMARILLO);
            return;
        }

        salaActual = ControlConsultarPlanoAsientosCINEX.obtenerSalaAsociada(funcionSeleccionada);
        asientosConsultados.clear();
        asientosConsultados.addAll(ControlIdentificarDisponibilidadCINEX.identificarDisponibilidadAsientos(funcionSeleccionada));

        int ocupados = ControlIdentificarDisponibilidadCINEX.contarOcupados(asientosConsultados);
        int disponibles = ControlIdentificarDisponibilidadCINEX.contarDisponibles(asientosConsultados);

        construirMapaAsientos(salaActual, obtenerCodigosOcupados(asientosConsultados));
        actualizarResumen(disponibles, ocupados, salaActual.getCapacidad());

        lblTituloSala.setText("Plano de asientos - " + salaActual.getNombre());
        lblResumen.setText("Información consultada disponible para " + funcionSeleccionada.resumenCorto() + ".");
    }

    private Set<String> obtenerCodigosOcupados(ArrayList<AsientoCINEX> asientos) {
        Set<String> ocupados = new HashSet<>();
        for (AsientoCINEX asiento : asientos) {
            if (asiento != null && asiento.isOcupado()) ocupados.add(asiento.getCodigo().toUpperCase());
        }
        return ocupados;
    }

    private void construirMapaAsientos(SalaCINEX sala, Set<String> ocupados) {
        mapaPanel.removeAll();
        SalaCINEX disenoSala = ControlConsultarPlanoAsientosCINEX.obtenerDisenoSala(sala);
        int capacidad = Math.max(0, disenoSala.getCapacidad());
        int filas = Math.max(1, disenoSala.getFilas());
        int columnas = Math.max(1, disenoSala.getColumnas());
        mapaPanel.setLayout(new GridLayout(filas, columnas + 1, 12, 10));

        int numero = 1;
        for (int f = 0; f < filas; f++) {
            String letra = String.valueOf((char) ('A' + f));
            JLabel lblFila = new JLabel(letra, SwingConstants.CENTER);
            lblFila.setForeground(BLANCO);
            lblFila.setFont(new Font("Arial", Font.BOLD, 16));
            mapaPanel.add(lblFila);

            for (int c = 1; c <= columnas; c++) {
                if (numero <= capacidad) {
                    String codigo = letra + c;
                    boolean ocupado = ocupados.contains(codigo.toUpperCase());
                    mapaPanel.add(new MapaSeatButton(codigo, ocupado));
                } else {
                    JPanel vacio = new JPanel();
                    vacio.setOpaque(false);
                    mapaPanel.add(vacio);
                }
                numero++;
            }
        }
        mapaPanel.revalidate();
        mapaPanel.repaint();
    }

    private void mostrarMensajeEnMapa(String mensaje, Color color) {
        mapaPanel.removeAll();
        mapaPanel.setLayout(new GridBagLayout());
        JLabel lbl = new JLabel("<html><center>" + mensaje + "</center></html>");
        lbl.setForeground(color);
        lbl.setFont(new Font("Arial", Font.BOLD, 18));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        mapaPanel.add(lbl);
        mapaPanel.revalidate();
        mapaPanel.repaint();
    }

    private void actualizarResumen(int disponibles, int ocupados, int total) {
        lblTotal.setText("Total: " + total);
        lblDisponibles.setText("Disponibles: " + disponibles);
        lblOcupados.setText("Ocupados: " + ocupados);
    }

    private JPanel crearLeyenda() {
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        leyenda.setOpaque(false);
        leyenda.add(crearItemLeyenda(AZUL_DISPONIBLE, "Disponible"));
        leyenda.add(crearItemLeyenda(GRIS_OCUPADO, "Ocupado"));
        return leyenda;
    }

    private JPanel crearItemLeyenda(Color color, String texto) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        item.setOpaque(false);
        JPanel cuadro = new JPanel();
        cuadro.setPreferredSize(new Dimension(22, 22));
        cuadro.setBackground(color);
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(BLANCO);
        lbl.setFont(new Font("Arial", Font.PLAIN, 14));
        item.add(cuadro);
        item.add(lbl);
        return item;
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

    private void actualizarFechaHora() {
        LocalDateTime ahora = LocalDateTime.now();
        lblHora.setText(ahora.format(DateTimeFormatter.ofPattern("hh:mm a")));
        lblFecha.setText(ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    class MapaSeatButton extends JPanel {
        private final String codigo;
        private final boolean ocupado;
        public MapaSeatButton(String codigo, boolean ocupado) {
            this.codigo = codigo;
            this.ocupado = ocupado;
            setOpaque(false);
            setToolTipText(codigo + " - " + (ocupado ? "Ocupado" : "Disponible"));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(ocupado ? GRIS_OCUPADO : AZUL_DISPONIBLE);
            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 8, 8);
            g2.setColor(ocupado ? new Color(120, 130, 145) : new Color(95, 145, 210));
            g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 8, 8);
            g2.setColor(BLANCO);
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(codigo)) / 2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(codigo, x, y);
            g2.dispose();
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
            g2.fillOval(getWidth() - 430, 220, 360, 360);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ConsultarDisponibilidadAsientosCINEXGUI("taquillero", new PeliculaCINEX(0, "Película"), null).setVisible(true));
    }
}
