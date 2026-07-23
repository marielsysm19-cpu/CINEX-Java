package interfaz;

import control.ControlGestionarPagoCINEX;
import entidad.PrecioCINEX;
import entidad.ReferenciaFuncionCINEX;

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
import java.util.List;
import java.util.Map;

public class SeleccionTipoEntradaCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_PANEL = new Color(8, 24, 55);
    private final Color AZUL_CARD = new Color(5, 18, 43);
    private final Color AZUL_BORDE = new Color(80, 105, 145);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS = new Color(185, 190, 200);
    private final Color VERDE = new Color(35, 190, 85);

    private JLabel lblHora;
    private JLabel lblFecha;
    private JLabel lblAsignados;
    private JLabel lblFaltan;
    private JLabel lblTotal;
    private JButton btnContinuar;

    private String usuarioActual;
    private String peliculaSeleccionada;
    private String funcionSeleccionada;
    private String tipoSalaFuncion;
    private List<String> asientosSeleccionados;

    private final ArrayList<PrecioCINEX> preciosEntrada = new ArrayList<>();
    private final Map<String, Integer> cantidades = new LinkedHashMap<>();
    private final Map<String, JLabel> labelsCantidad = new LinkedHashMap<>();
    private final ArrayList<JButton> botonesMas = new ArrayList<>();
    private final ArrayList<JButton> botonesMenos = new ArrayList<>();

    public SeleccionTipoEntradaCINEXGUI() {
        this("taquillero", "Dune: Parte Dos", "2:30 PM", crearAsientosPrueba());
    }

    private static ArrayList<String> crearAsientosPrueba() {
        ArrayList<String> asientos = new ArrayList<>();
        asientos.add("C5");
        asientos.add("C6");
        return asientos;
    }

    public SeleccionTipoEntradaCINEXGUI(String usuario, String pelicula, String funcion, List<String> asientos) {
        this(usuario, pelicula, funcion, "", asientos);
    }

    public SeleccionTipoEntradaCINEXGUI(String usuario, String pelicula, String funcion, String tipoSalaFuncion, List<String> asientos) {
        this.usuarioActual = usuario == null || usuario.trim().isEmpty() ? "taquillero" : usuario.trim();
        this.peliculaSeleccionada = pelicula == null ? "" : pelicula.trim();
        this.funcionSeleccionada = funcion == null ? "" : funcion.trim();
        this.tipoSalaFuncion = tipoSalaFuncion == null ? "" : tipoSalaFuncion.trim();
        this.asientosSeleccionados = asientos != null ? new ArrayList<>(asientos) : new ArrayList<>();

        cargarPreciosDesdeBD();

        setTitle("CINEX - Lista de precios");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        CINEXResponsive.configurarVentana(this, 1366, 768, 1000, 650);

        JPanel fondo = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
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
        };

        setContentPane(fondo);
        fondo.add(crearHeader(), BorderLayout.NORTH);
        fondo.add(crearContenido(), BorderLayout.CENTER);
        fondo.add(crearFooter(), BorderLayout.SOUTH);

        actualizarResumen();
        actualizarFechaHora();
        new Timer(1000, e -> actualizarFechaHora()).start();
    }

    private void cargarPreciosDesdeBD() {
        preciosEntrada.clear();
        preciosEntrada.addAll(ControlGestionarPagoCINEX.solicitarPreciosParaFuncion(tipoSalaFuncion));

        if (preciosEntrada.isEmpty()) {
            preciosEntrada.add(new PrecioCINEX(0, "Entrada General", 32.00, "Activo"));
        }

        cantidades.clear();
        for (PrecioCINEX precio : preciosEntrada) {
            cantidades.put(precio.getTipoEntrada(), 0);
        }
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(6, 25, 2, 25));

        JLabel logo = new JLabel();
        logo.setIcon(cargarImagen("imagenes/logocinex.png", 255, 90));

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 5));
        infoPanel.setOpaque(false);

        JLabel lblUsuario = crearTextoHeader("Usuario: " + usuarioActual);
        JLabel lblTerminal = crearTextoHeader("Terminal: 01");
        lblHora = crearTextoHeader("");
        lblFecha = crearTextoHeader("");

        infoPanel.add(lblUsuario);
        infoPanel.add(lblTerminal);
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
        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setOpaque(false);
        contenido.add(new SidebarCINEX(4), BorderLayout.WEST);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 20, 0, 25));

        JPanel main = new JPanel(new BorderLayout(26, 0));
        main.setOpaque(false);
        main.setPreferredSize(CINEXResponsive.pantallaPequena() ? new Dimension(970, 565) : new Dimension(1120, 590));
        main.setMaximumSize(new Dimension(1200, 610));

        main.add(crearPanelPrecios(), BorderLayout.CENTER);
        main.add(crearPanelResumen(), BorderLayout.EAST);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        wrapper.add(main, gbc);

        contenido.add(wrapper, BorderLayout.CENTER);
        return contenido;
    }

    private JPanel crearPanelPrecios() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Lista de precios");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 34));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitulo = new JLabel("Asigne un tipo de entrada por cada asiento seleccionado.");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 17));
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(titulo);
        panel.add(Box.createVerticalStrut(6));
        panel.add(subtitulo);
        panel.add(Box.createVerticalStrut(18));

        RoundedPanel lista = new RoundedPanel(18, new Color(5, 18, 43, 190));
        lista.setLayout(null);
        lista.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        lista.setPreferredSize(new Dimension(720, 485));
        lista.setMaximumSize(new Dimension(760, 500));
        lista.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel encabezado = new JLabel("Seleccione sus entradas");
        encabezado.setForeground(BLANCO);
        encabezado.setFont(new Font("Arial", Font.BOLD, 24));
        encabezado.setBounds(32, 25, 500, 34);
        lista.add(encabezado);

        JLabel ayuda = new JLabel("Use + para agregar y - para quitar. No puede superar la cantidad de asientos.");
        ayuda.setForeground(GRIS);
        ayuda.setFont(new Font("Arial", Font.PLAIN, 14));
        ayuda.setBounds(32, 58, 620, 25);
        lista.add(ayuda);

        JLabel aviso3D = new JLabel(textoAviso3D());
        aviso3D.setForeground(esFuncion3D() ? AMARILLO : GRIS);
        aviso3D.setFont(new Font("Arial", Font.BOLD, 13));
        aviso3D.setBounds(32, 78, 620, 22);
        lista.add(aviso3D);

        JPanel filas = new JPanel();
        filas.setOpaque(false);
        filas.setLayout(new BoxLayout(filas, BoxLayout.Y_AXIS));

        int altoFilas = Math.max(325, preciosEntrada.size() * 82);
        for (PrecioCINEX precio : preciosEntrada) {
            filas.add(crearFilaPrecio(precio));
            filas.add(Box.createVerticalStrut(10));
        }
        filas.setPreferredSize(new Dimension(620, altoFilas));
        filas.setMaximumSize(new Dimension(620, altoFilas));

        JScrollPane scroll = new JScrollPane(filas);
        scroll.setBounds(32, 110, 655, 340);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(70, 95, 135);
                trackColor = new Color(5, 18, 43);
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
        lista.add(scroll);

        panel.add(lista);
        return panel;
    }

    private JPanel crearFilaPrecio(PrecioCINEX precio) {
        RoundedPanel fila = new RoundedPanel(14, AZUL_PANEL);
        fila.setLayout(null);
        fila.setPreferredSize(new Dimension(610, 72));
        fila.setMaximumSize(new Dimension(610, 72));
        fila.setMinimumSize(new Dimension(610, 72));
        fila.setBorder(new LineBorder(new Color(65, 95, 140), 1, true));

        JLabel tipo = new JLabel(precio.getTipoEntrada());
        tipo.setForeground(BLANCO);
        tipo.setFont(new Font("Arial", Font.BOLD, 18));
        tipo.setBounds(22, 10, 300, 24);
        fila.add(tipo);

        JLabel monto = new JLabel("S/ " + String.format("%.2f", precio.getMonto()));
        monto.setForeground(AMARILLO);
        monto.setFont(new Font("Arial", Font.BOLD, 22));
        monto.setBounds(22, 38, 180, 26);
        fila.add(monto);

        JButton menos = crearBotonCantidad("−");
        menos.setBounds(415, 16, 46, 40);
        menos.addActionListener(e -> cambiarCantidad(precio.getTipoEntrada(), -1));
        fila.add(menos);
        botonesMenos.add(menos);

        JLabel cantidad = new JLabel("0", SwingConstants.CENTER);
        cantidad.setForeground(BLANCO);
        cantidad.setFont(new Font("Arial", Font.BOLD, 22));
        cantidad.setBounds(467, 16, 50, 40);
        fila.add(cantidad);
        labelsCantidad.put(precio.getTipoEntrada(), cantidad);

        JButton mas = crearBotonCantidad("+");
        mas.setBounds(523, 16, 46, 40);
        mas.addActionListener(e -> cambiarCantidad(precio.getTipoEntrada(), 1));
        fila.add(mas);
        botonesMas.add(mas);

        return fila;
    }

    private JPanel crearPanelResumen() {
        RoundedPanel resumen = new RoundedPanel(18, new Color(5, 18, 43, 190));
        resumen.setLayout(null);
        resumen.setPreferredSize(new Dimension(340, 460));
        resumen.setBorder(new LineBorder(AZUL_BORDE, 1, true));

        JLabel titulo = new JLabel("Resumen");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 28));
        titulo.setBounds(35, 30, 260, 35);
        resumen.add(titulo);

        JLabel lblPelicula = crearEtiquetaResumen("Película:");
        lblPelicula.setBounds(35, 90, 120, 24);
        resumen.add(lblPelicula);

        JLabel valorPelicula = crearValorResumen(peliculaSeleccionada);
        valorPelicula.setBounds(35, 116, 270, 24);
        resumen.add(valorPelicula);

        JLabel lblFuncion = crearEtiquetaResumen("Función:");
        lblFuncion.setBounds(35, 155, 120, 24);
        resumen.add(lblFuncion);

        JLabel valorFuncion = crearValorResumen(
                ReferenciaFuncionCINEX.mostrar(funcionSeleccionada)
        );
        valorFuncion.setBounds(35, 181, 270, 24);
        resumen.add(valorFuncion);

        JLabel lblTipoSala = crearEtiquetaResumen("Tipo de sala:");
        lblTipoSala.setBounds(35, 218, 120, 24);
        resumen.add(lblTipoSala);

        JLabel valorTipoSala = crearValorResumen(tipoSalaFuncion == null || tipoSalaFuncion.trim().isEmpty() ? "2D" : tipoSalaFuncion);
        valorTipoSala.setBounds(35, 244, 270, 24);
        valorTipoSala.setForeground(esFuncion3D() ? AMARILLO : BLANCO);
        resumen.add(valorTipoSala);

        JLabel lblAsientos = crearEtiquetaResumen("Asientos seleccionados:");
        lblAsientos.setBounds(35, 278, 220, 24);
        resumen.add(lblAsientos);

        JLabel valorAsientos = crearValorResumen(String.join(", ", asientosSeleccionados));
        valorAsientos.setBounds(35, 304, 280, 28);
        resumen.add(valorAsientos);

        lblAsignados = crearEtiquetaResumen("Asignados: 0 / " + asientosSeleccionados.size());
        lblAsignados.setForeground(AMARILLO);
        lblAsignados.setBounds(35, 342, 270, 24);
        resumen.add(lblAsignados);

        lblFaltan = crearValorResumen("Asigne las entradas para continuar.");
        lblFaltan.setForeground(GRIS);
        lblFaltan.setFont(new Font("Arial", Font.BOLD, 13));
        lblFaltan.setBounds(35, 370, 285, 24);
        resumen.add(lblFaltan);

        JSeparator separador = new JSeparator();
        separador.setForeground(new Color(70, 100, 145));
        separador.setBounds(35, 405, 270, 1);
        resumen.add(separador);

        JLabel lblTotalText = new JLabel("Total:");
        lblTotalText.setForeground(BLANCO);
        lblTotalText.setFont(new Font("Arial", Font.BOLD, 25));
        lblTotalText.setBounds(35, 420, 100, 35);
        resumen.add(lblTotalText);

        lblTotal = new JLabel("S/ 0.00", SwingConstants.RIGHT);
        lblTotal.setForeground(AMARILLO);
        lblTotal.setFont(new Font("Arial", Font.BOLD, 26));
        lblTotal.setBounds(130, 420, 175, 35);
        resumen.add(lblTotal);

        return resumen;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 25, 25, 25));

        JButton btnAtras = crearBotonSecundario("ATRÁS");
        btnContinuar = crearBotonPrincipal("CONTINUAR A PAGO");
        btnContinuar.setEnabled(false);

        btnAtras.addActionListener(e -> CINEXTransiciones.cambiar(
                this,
                new SeleccionAsientosCINEXGUI(usuarioActual, peliculaSeleccionada, funcionSeleccionada, tipoSalaFuncion)
        ));

        btnContinuar.addActionListener(e -> continuarPago());

        footer.add(btnAtras, BorderLayout.WEST);
        footer.add(btnContinuar, BorderLayout.EAST);
        return footer;
    }

    private void cambiarCantidad(String tipoEntrada, int cambio) {
        int asignados = totalAsignados();
        int maximo = asientosSeleccionados.size();
        int actual = cantidades.getOrDefault(tipoEntrada, 0);

        if (cambio > 0 && asignados >= maximo) {
            actualizarResumen();
            return;
        }

        int nuevo = Math.max(0, actual + cambio);
        if (nuevo == actual) {
            actualizarResumen();
            return;
        }

        cantidades.put(tipoEntrada, nuevo);
        actualizarResumen();
    }

    private void actualizarResumen() {
        int asignados = totalAsignados();
        int totalAsientos = asientosSeleccionados.size();
        double total = calcularTotal();

        for (Map.Entry<String, JLabel> entry : labelsCantidad.entrySet()) {
            entry.getValue().setText(String.valueOf(cantidades.getOrDefault(entry.getKey(), 0)));
        }

        if (lblAsignados != null) {
            lblAsignados.setText("Asignados: " + asignados + " / " + totalAsientos);
        }

        if (lblFaltan != null) {
            int faltan = totalAsientos - asignados;
            if (totalAsientos <= 0) {
                lblFaltan.setText("Regrese y seleccione asientos.");
                lblFaltan.setForeground(GRIS);
            } else if (faltan > 0) {
                lblFaltan.setText("Faltan " + faltan + " entrada(s) por asignar.");
                lblFaltan.setForeground(AMARILLO);
            } else {
                lblFaltan.setText("Listo para continuar al pago.");
                lblFaltan.setForeground(VERDE);
            }
        }

        if (lblTotal != null) {
            lblTotal.setText("S/ " + String.format("%.2f", total));
        }

        boolean completo = totalAsientos > 0 && asignados == totalAsientos;
        if (btnContinuar != null) {
            btnContinuar.setEnabled(completo);
        }

        for (JButton btn : botonesMas) {
            btn.setEnabled(asignados < totalAsientos);
        }

        for (Map.Entry<String, Integer> entry : cantidades.entrySet()) {
            // los botones menos se actualizan visualmente con estado global simple
        }
        for (JButton btn : botonesMenos) {
            btn.setEnabled(asignados > 0);
        }
    }

    private int totalAsignados() {
        int total = 0;
        for (int cantidad : cantidades.values()) {
            total += cantidad;
        }
        return total;
    }

    private double calcularTotal() {
        double total = 0.0;
        for (PrecioCINEX precio : preciosEntrada) {
            int cantidad = cantidades.getOrDefault(precio.getTipoEntrada(), 0);
            total += cantidad * precio.getMonto();
        }
        return total;
    }

    private ArrayList<String> obtenerTiposEntradaSeleccionados() {
        ArrayList<String> tipos = new ArrayList<>();
        for (PrecioCINEX precio : preciosEntrada) {
            int cantidad = cantidades.getOrDefault(precio.getTipoEntrada(), 0);
            for (int i = 0; i < cantidad; i++) {
                tipos.add(precio.getTipoEntrada());
            }
        }
        return tipos;
    }

    private void continuarPago() {
        if (totalAsignados() != asientosSeleccionados.size()) {
            actualizarResumen();
            return;
        }

        CINEXTransiciones.cambiar(this, new PagoCINEXGUI(
                usuarioActual,
                peliculaSeleccionada,
                funcionSeleccionada,
                asientosSeleccionados,
                obtenerTiposEntradaSeleccionados(),
                tipoSalaFuncion,
                calcularTotal()
        ));
    }


    private boolean esFuncion3D() {
        return tipoSalaFuncion != null && tipoSalaFuncion.toUpperCase().contains("3D");
    }

    private String textoAviso3D() {
        if (esFuncion3D()) {
            return "Función 3D: se aplica automáticamente el precio de Sala 3D.";
        }
        return "Función 2D: se muestran los precios generales.";
    }

    private JLabel crearEtiquetaResumen(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(GRIS);
        lbl.setFont(new Font("Arial", Font.BOLD, 16));
        return lbl;
    }

    private JLabel crearValorResumen(String texto) {
        JLabel lbl = new JLabel(texto == null ? "" : texto);
        lbl.setForeground(BLANCO);
        lbl.setFont(new Font("Arial", Font.BOLD, 15));
        return lbl;
    }

    private JButton crearBotonCantidad(String texto) {
        JButton btn = new JButton(texto);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btn.setPreferredSize(new Dimension(46, 40));
        btn.setFont(new Font("Arial", Font.BOLD, 22));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        btn.setBackground(AZUL_CARD);
        btn.setForeground(BLANCO);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton crearBotonPrincipal(String texto) {
        JButton btn = new JButton(texto);
        btn.setPreferredSize(new Dimension(260, 62));
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        CINEXResponsive.estabilizarBoton(btn, AMARILLO, Color.BLACK);
        return btn;
    }

    private JButton crearBotonSecundario(String texto) {
        JButton btn = CINEXResponsive.botonSecundario(texto, 205, 60);
        btn.setForeground(BLANCO);
        return btn;
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

    private class RoundedPanel extends JPanel {
        private final int radio;
        private final Color color;

        RoundedPanel(int radio, Color color) {
            this.radio = radio;
            this.color = color;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D round = new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, radio, radio);
            g2.setColor(color);
            g2.fill(round);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SeleccionTipoEntradaCINEXGUI("taquillero", "Dune: Parte Dos", "2:30 PM", crearAsientosPrueba()).setVisible(true));
    }
}
