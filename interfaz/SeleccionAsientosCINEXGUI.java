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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import control.ControlVerificarDisponibilidadCINEX;
import control.ControlGestionarPagoCINEX;
import control.BDCINEX;
import entidad.PrecioCINEX;
import entidad.ReferenciaFuncionCINEX;


public class SeleccionAsientosCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_SIDEBAR = new Color(5, 18, 43);
    private final Color AZUL_DISPONIBLE = new Color(28, 100, 180);
    private final Color AZUL_PANEL = new Color(8, 24, 55);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color GRIS_OCUPADO = new Color(85, 98, 115);
    private final Color ROJO_ERROR = new Color(210, 65, 65);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS = new Color(185, 190, 200);

    private int filasMapa = 10;
    private int columnasMapa = 10;
    private int totalAsientos = 100;

    private JLabel lblHora;
    private JLabel lblFecha;
    private JLabel lblDisponibles;
    private JLabel lblTotal;
    private JLabel lblValidacionTipos;
    private JPanel resumenAsientosPanel;
    private JPanel panelTiposEntrada;
    private JButton btnConfirmar;
    private boolean actualizandoTiposEntrada = false;
    private final ArrayList<PrecioCINEX> preciosEntrada = new ArrayList<>();
    private final Map<String, JSpinner> spinnersTipoEntrada = new LinkedHashMap<>();

    private String usuarioActual;
    private String peliculaSeleccionada;
    private String funcionSeleccionada;
    private String tipoSalaFuncion;

    private final Set<String> asientosSeleccionados = new LinkedHashSet<>();
    private final Set<String> asientosOcupados = new LinkedHashSet<>();
    private final Map<String, SeatButton> botonesAsientos = new LinkedHashMap<>();

    public SeleccionAsientosCINEXGUI() {
        this("taquillero", "Dune: Parte Dos", "2:30 PM");
    }

    public SeleccionAsientosCINEXGUI(String usuario, String pelicula, String funcion) {
        this(usuario, pelicula, funcion, "");
    }

    public SeleccionAsientosCINEXGUI(String usuario, String pelicula, String funcion, String tipoSalaFuncion) {
        this.usuarioActual = usuario;
        this.peliculaSeleccionada = pelicula;
        this.funcionSeleccionada = funcion;
        this.tipoSalaFuncion = tipoSalaFuncion == null ? "" : tipoSalaFuncion.trim();

        configurarMapaSegunSala();
        cargarAsientosOcupados();
        cargarTiposEntradaDesdeBD();

        setTitle("CINEX - Gestionar selección de asientos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        CINEXResponsive.configurarVentana(this, 1400, 820, 1000, 650);

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

        actualizarFechaHora();
        Timer timer = new Timer(1000, e -> actualizarFechaHora());
        timer.start();

        SwingUtilities.invokeLater(this::verificarFuncionAgotadaAlAbrir);
    }

    private void cargarAsientosOcupados() {
        asientosOcupados.clear();
        asientosOcupados.addAll(ControlVerificarDisponibilidadCINEX.consultarAsientosOcupados(peliculaSeleccionada, funcionSeleccionada));
        System.out.println("Asientos ocupados: " + asientosOcupados);
    }

    private void cargarTiposEntradaDesdeBD() {
        preciosEntrada.clear();
        preciosEntrada.addAll(ControlGestionarPagoCINEX.solicitarPreciosActivos());

        if (preciosEntrada.isEmpty()) {
            preciosEntrada.add(new PrecioCINEX(0, "Entrada General", 32.00, "Activo"));
        }
    }

    private void refrescarMapaDesdeBD() {
        cargarAsientosOcupados();
        aplicarEstadoAsientosAlMapa();
    }

    private void refrescarMapaDesdeBDAsync() {
        if (btnConfirmar != null) {
            btnConfirmar.setEnabled(false);
        }

        SwingWorker<Set<String>, Void> worker = new SwingWorker<Set<String>, Void>() {
            @Override
            protected Set<String> doInBackground() {
                return new LinkedHashSet<>(ControlVerificarDisponibilidadCINEX.consultarAsientosOcupados(peliculaSeleccionada, funcionSeleccionada));
            }

            @Override
            protected void done() {
                try {
                    asientosOcupados.clear();
                    asientosOcupados.addAll(get());
                    aplicarEstadoAsientosAlMapa();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            SeleccionAsientosCINEXGUI.this,
                            "No se pudo actualizar el mapa de asientos.",
                            "Error de actualización",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    private void aplicarEstadoAsientosAlMapa() {
        for (Map.Entry<String, SeatButton> entry : botonesAsientos.entrySet()) {
            String codigo = entry.getKey();
            SeatButton boton = entry.getValue();

            boolean ocupado = asientosOcupados.contains(codigo);
            boton.setOcupado(ocupado);

            if (ocupado && asientosSeleccionados.contains(codigo)) {
                asientosSeleccionados.remove(codigo);
                boton.setSeleccionado(false);
            }
        }

        actualizarResumen();
        actualizarDisponibilidad();
    }

    private void verificarFuncionAgotadaAlAbrir() {
        actualizarDisponibilidad();

        if (contarDisponibles() <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "No existen asientos disponibles para esta función",
                    "Función agotada",
                    JOptionPane.WARNING_MESSAGE
            );

            if (btnConfirmar != null) {
                btnConfirmar.setEnabled(false);
            }
        }
    }

    private void configurarMapaSegunSala() {
        int capacidadBD = ControlVerificarDisponibilidadCINEX.consultarCapacidadFuncion(peliculaSeleccionada, funcionSeleccionada);

        if (capacidadBD > 0) {
            totalAsientos = capacidadBD;
        } else {
            totalAsientos = 100;
        }

        if (totalAsientos <= 80) {
            columnasMapa = 10;
        } else if (totalAsientos <= 100) {
            columnasMapa = 10;
        } else if (totalAsientos <= 120) {
            columnasMapa = 12;
        } else {
            columnasMapa = 14;
        }

        filasMapa = (int) Math.ceil(totalAsientos / (double) columnasMapa);
    }

    private int contarOcupadosEnMapa() {
        int ocupados = 0;

        for (String asiento : asientosOcupados) {
            if (esAsientoDentroDelMapa(asiento)) {
                ocupados++;
            }
        }

        return ocupados;
    }

    private boolean esAsientoDentroDelMapa(String codigo) {
        if (codigo == null || codigo.trim().length() < 2) {
            return false;
        }

        String asiento = codigo.trim().toUpperCase();
        char filaLetra = asiento.charAt(0);

        if (filaLetra < 'A' || filaLetra >= ('A' + filasMapa)) {
            return false;
        }

        try {
            int numero = Integer.parseInt(asiento.substring(1));
            int filaIndice = filaLetra - 'A';
            int posicion = filaIndice * columnasMapa + numero;

            return numero >= 1 && numero <= columnasMapa && posicion <= totalAsientos;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int contarDisponibles() {
        return Math.max(0, totalAsientos - contarOcupadosEnMapa());
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(6, 25, 2, 25));

        JLabel logo = new JLabel();
        logo.setIcon(cargarImagen("imagenes/logocinex.png", 255, 90));

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 5));
        infoPanel.setOpaque(false);

        JLabel lblUsuario = new JLabel("Usuario: " + usuarioActual);
        lblUsuario.setForeground(BLANCO);
        lblUsuario.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel lblTerminal = new JLabel("Terminal: 01");
        lblTerminal.setForeground(BLANCO);
        lblTerminal.setFont(new Font("Arial", Font.BOLD, 16));

        lblHora = new JLabel();
        lblHora.setForeground(BLANCO);
        lblHora.setFont(new Font("Arial", Font.BOLD, 16));

        lblFecha = new JLabel();
        lblFecha.setForeground(BLANCO);
        lblFecha.setFont(new Font("Arial", Font.BOLD, 16));

        infoPanel.add(lblUsuario);
        infoPanel.add(lblTerminal);
        infoPanel.add(lblHora);
        infoPanel.add(lblFecha);

        header.add(logo, BorderLayout.WEST);
        header.add(infoPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel crearContenido() {
        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setOpaque(false);

        contenido.add(new SidebarCINEX(3), BorderLayout.WEST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel main = new JPanel(new BorderLayout(24, 0));
        main.setOpaque(false);
        // Tamaño controlado para que no aparezcan barras de scroll y todo entre en pantalla.
        main.setPreferredSize(CINEXResponsive.pantallaPequena() ? new Dimension(1030, 600) : new Dimension(1180, 625));
        main.setMaximumSize(CINEXResponsive.pantallaPequena() ? new Dimension(1060, 610) : new Dimension(1200, 635));
        main.setBorder(new EmptyBorder(0, 12, 8, 12));

        main.add(crearPanelAsientos(), BorderLayout.CENTER);
        main.add(crearPanelResumen(), BorderLayout.EAST);

        wrapper.add(main, BorderLayout.CENTER);
        contenido.add(wrapper, BorderLayout.CENTER);
        contenido.add(crearFooter(), BorderLayout.SOUTH);

        return contenido;
    }

    private JPanel crearSidebar() {
        JPanel sidebar = new JPanel(null);
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBackground(AZUL_SIDEBAR);

        JLabel titulo = new JLabel("");
        titulo.setForeground(AMARILLO);
        titulo.setFont(new Font("Arial", Font.BOLD, 26));
        titulo.setBounds(28, 28, 150, 35);
        sidebar.add(titulo);

        String[] pasos = {
                "1. Película",
                "2. Función",
                "3. Asientos",
                "4. Pago",
                "5. Confirmación"
        };

        int y = 90;
        for (int i = 0; i < pasos.length; i++) {
            StepItem item = new StepItem(pasos[i], i == 2);
            item.setBounds(12, y, 185, 55);
            sidebar.add(item);
            y += 62;
        }

        return sidebar;
    }

    private JPanel crearPanelAsientos() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Gestionar selección de asientos");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 30));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitulo = new JLabel("Seleccione los asientos disponibles para la función elegida");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 16));
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblDisponibles = new JLabel();
        lblDisponibles.setForeground(AMARILLO);
        lblDisponibles.setFont(new Font("Arial", Font.BOLD, 16));
        lblDisponibles.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titulo);
        panel.add(Box.createVerticalStrut(4));
        panel.add(subtitulo);
        panel.add(Box.createVerticalStrut(8));
        panel.add(lblDisponibles);
        panel.add(Box.createVerticalStrut(10));

        int anchoMapa = calcularAnchoMapa();
        int altoMapa = calcularAltoMapa();

        PantallaPanel pantalla = new PantallaPanel();
        pantalla.setPreferredSize(new Dimension(anchoMapa, 42));
        pantalla.setMaximumSize(new Dimension(anchoMapa, 42));
        pantalla.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(pantalla);
        panel.add(Box.createVerticalStrut(10));

        JPanel matrizWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        matrizWrapper.setOpaque(false);
        matrizWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel matriz = new JPanel(new GridLayout(filasMapa, columnasMapa + 1, 10, 8));
        matriz.setOpaque(false);
        matriz.setPreferredSize(new Dimension(anchoMapa, altoMapa));
        matriz.setMaximumSize(new Dimension(anchoMapa, altoMapa));

        int contador = 0;

        for (int f = 0; f < filasMapa; f++) {
            String fila = String.valueOf((char) ('A' + f));

            JLabel lblFila = new JLabel(fila, SwingConstants.CENTER);
            lblFila.setForeground(BLANCO);
            lblFila.setFont(new Font("Arial", Font.BOLD, 16));
            matriz.add(lblFila);

            for (int col = 1; col <= columnasMapa; col++) {
                contador++;

                if (contador > totalAsientos) {
                    JPanel vacio = new JPanel();
                    vacio.setOpaque(false);
                    matriz.add(vacio);
                    continue;
                }

                String codigo = fila + col;
                boolean ocupado = asientosOcupados.contains(codigo);

                SeatButton seat = new SeatButton(codigo, ocupado);
                botonesAsientos.put(codigo, seat);

                seat.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        gestionarClickAsiento(seat);
                    }
                });

                matriz.add(seat);
            }
        }

        matrizWrapper.add(matriz);
        panel.add(matrizWrapper);
        panel.add(Box.createVerticalStrut(18));
        panel.add(crearLeyenda());

        return panel;
    }

    private int calcularAnchoMapa() {
        int ancho = 46 + (columnasMapa * 62) + ((columnasMapa + 1) * 10);
        int max = CINEXResponsive.pantallaPequena() ? 780 : 860;
        int min = CINEXResponsive.pantallaPequena() ? 720 : 800;
        return Math.min(max, Math.max(min, ancho));
    }

    private int calcularAltoMapa() {
        int alto = filasMapa * 40 + ((filasMapa - 1) * 9);
        int max = CINEXResponsive.pantallaPequena() ? 430 : 470;
        int min = CINEXResponsive.pantallaPequena() ? 390 : 420;
        return Math.min(max, Math.max(min, alto));
    }

    private void gestionarClickAsiento(SeatButton seat) {
        String codigo = seat.getCodigo();

        
        if (seat.isOcupado() || asientosOcupados.contains(codigo)) {
            seat.setOcupado(true);
            asientosSeleccionados.remove(codigo);
            actualizarResumen();
            actualizarDisponibilidad();
            mostrarMensajeAsientoOcupado();
            return;
        }

        alternarAsiento(seat);
    }

    private void mostrarMensajeAsientoOcupado() {
        JOptionPane.showMessageDialog(
                this,
                "El asiento seleccionado ya no se encuentra disponible",
                "Asiento ocupado",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private JPanel crearLeyenda() {
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.CENTER, 45, 0));
        leyenda.setOpaque(false);
        leyenda.setAlignmentX(Component.CENTER_ALIGNMENT);

        leyenda.add(crearItemLeyenda(AZUL_DISPONIBLE, "Disponible"));
        leyenda.add(crearItemLeyenda(AMARILLO, "Seleccionado temporalmente"));
        leyenda.add(crearItemLeyenda(GRIS_OCUPADO, "Ocupado"));

        return leyenda;
    }

    private JPanel crearItemLeyenda(Color color, String texto) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        item.setOpaque(false);

        JPanel cuadro = new JPanel();
        cuadro.setPreferredSize(new Dimension(24, 24));
        cuadro.setBackground(color);

        JLabel lbl = new JLabel(texto);
        lbl.setForeground(BLANCO);
        lbl.setFont(new Font("Arial", Font.PLAIN, 15));

        item.add(cuadro);
        item.add(lbl);
        return item;
    }

    private JPanel crearPanelResumen() {
        RoundedPanel resumen = new RoundedPanel(18, new Color(5, 18, 43, 190));
        resumen.setLayout(null);
        resumen.setPreferredSize(CINEXResponsive.pantallaPequena() ? new Dimension(315, 535) : new Dimension(330, 555));
        resumen.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));

        JLabel titulo = new JLabel("Resumen");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 25));
        titulo.setBounds(35, 30, 260, 35);
        resumen.add(titulo);

        JLabel lblPelicula = crearEtiquetaResumen("Película:");
        lblPelicula.setBounds(35, 95, 100, 25);
        resumen.add(lblPelicula);

        JLabel valorPelicula = crearValorResumen(peliculaSeleccionada);
        valorPelicula.setBounds(135, 95, 225, 25);
        resumen.add(valorPelicula);

        JLabel lblFuncion = crearEtiquetaResumen("Función:");
        lblFuncion.setBounds(35, 135, 100, 25);
        resumen.add(lblFuncion);

        JLabel valorFuncion = crearValorResumen(
                ReferenciaFuncionCINEX.mostrar(funcionSeleccionada)
        );
        valorFuncion.setBounds(135, 135, 225, 25);
        resumen.add(valorFuncion);

        JLabel lblEstado = crearEtiquetaResumen("Estado:");
        lblEstado.setBounds(35, 175, 100, 25);
        resumen.add(lblEstado);

        JLabel valorEstado = crearValorResumen("Selección temporal");
        valorEstado.setForeground(AMARILLO);
        valorEstado.setBounds(135, 175, 200, 25);
        resumen.add(valorEstado);

        JLabel lblAsientos = crearEtiquetaResumen("Asientos:");
        lblAsientos.setBounds(35, 230, 120, 25);
        resumen.add(lblAsientos);

        resumenAsientosPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        resumenAsientosPanel.setOpaque(false);
        resumenAsientosPanel.setBounds(35, 260, 295, 125);
        resumen.add(resumenAsientosPanel);

        lblValidacionTipos = new JLabel("Seleccione asientos para continuar.");
        lblValidacionTipos.setForeground(AMARILLO);
        lblValidacionTipos.setFont(new Font("Arial", Font.BOLD, 13));
        lblValidacionTipos.setBounds(35, 405, 295, 25);
        resumen.add(lblValidacionTipos);

        JSeparator separador = new JSeparator();
        separador.setForeground(new Color(70, 100, 145));
        separador.setBounds(35, 452, 295, 1);
        resumen.add(separador);

        JLabel lblTotalText = new JLabel("Seleccionados:");
        lblTotalText.setForeground(BLANCO);
        lblTotalText.setFont(new Font("Arial", Font.BOLD, 22));
        lblTotalText.setBounds(35, 475, 165, 35);
        resumen.add(lblTotalText);

        lblTotal = new JLabel("0");
        lblTotal.setForeground(BLANCO);
        lblTotal.setFont(new Font("Arial", Font.BOLD, 25));
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        lblTotal.setBounds(205, 475, 115, 35);
        resumen.add(lblTotal);

        actualizarResumen();
        return resumen;
    }

    private JLabel crearEtiquetaResumen(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(GRIS);
        lbl.setFont(new Font("Arial", Font.BOLD, 16));
        return lbl;
    }

    private JLabel crearValorResumen(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(BLANCO);
        lbl.setFont(new Font("Arial", Font.BOLD, 16));
        return lbl;
    }

    private JLabel crearValorPrecioTipo(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(AMARILLO);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        return lbl;
    }

    private JPanel crearFilaTipoEntrada(PrecioCINEX precio) {
        JPanel fila = new JPanel(null);
        fila.setOpaque(false);
        fila.setMaximumSize(new Dimension(295, 25));
        fila.setPreferredSize(new Dimension(295, 25));

        String tipo = precio.getTipoEntrada();
        JLabel lblTipo = crearEtiquetaResumen(tipo);
        lblTipo.setFont(new Font("Arial", Font.BOLD, 12));
        lblTipo.setBounds(0, 0, 145, 24);
        fila.add(lblTipo);

        JSpinner spinner = crearSpinnerTipoEntrada();
        spinner.setBounds(150, 0, 48, 24);
        spinner.addChangeListener(e -> ajustarTiposEntradaDinamicos(spinner));
        fila.add(spinner);
        spinnersTipoEntrada.put(tipo, spinner);

        JLabel lblPrecio = crearValorPrecioTipo("S/ " + String.format("%.2f", precio.getMonto()));
        lblPrecio.setBounds(205, 0, 90, 24);
        fila.add(lblPrecio);

        return fila;
    }

    private JSpinner crearSpinnerTipoEntrada() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
        spinner.setFont(new Font("Arial", Font.BOLD, 12));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField campo = ((JSpinner.DefaultEditor) editor).getTextField();
            campo.setHorizontalAlignment(SwingConstants.CENTER);
            campo.setEditable(false);
            campo.setBackground(AZUL_PANEL);
            campo.setForeground(BLANCO);
        }
        return spinner;
    }

    private void ajustarTiposEntradaDinamicos(JSpinner spinnerCambiado) {
        if (actualizandoTiposEntrada || spinnersTipoEntrada.isEmpty()) return;

        actualizandoTiposEntrada = true;
        int totalAsientosSeleccionados = asientosSeleccionados.size();

        for (JSpinner spinner : spinnersTipoEntrada.values()) {
            int valor = Math.min(obtenerValorSpinner(spinner), totalAsientosSeleccionados);
            setSpinnerSinEvento(spinner, valor, totalAsientosSeleccionados);
        }

        int suma = sumarSpinnersTipoEntrada();

        if (suma > totalAsientosSeleccionados) {
            int excedente = suma - totalAsientosSeleccionados;
            for (JSpinner spinner : spinnersTipoEntrada.values()) {
                if (spinner == spinnerCambiado) continue;
                int valor = obtenerValorSpinner(spinner);
                int quitar = Math.min(valor, excedente);
                if (quitar > 0) {
                    setSpinnerSinEvento(spinner, valor - quitar, totalAsientosSeleccionados);
                    excedente -= quitar;
                }
                if (excedente == 0) break;
            }
        }

        suma = sumarSpinnersTipoEntrada();
        if (suma < totalAsientosSeleccionados) {
            JSpinner spinnerPrincipal = obtenerSpinnerPrincipal();
            if (spinnerPrincipal != null) {
                setSpinnerSinEvento(
                        spinnerPrincipal,
                        obtenerValorSpinner(spinnerPrincipal) + (totalAsientosSeleccionados - suma),
                        totalAsientosSeleccionados
                );
            }
        }

        actualizandoTiposEntrada = false;
        actualizarTextoTiposEntrada();
        lblTotal.setText("S/ " + String.format("%.2f", calcularTotal()));
    }

    private void actualizarSpinnersTipoEntrada() {
        if (spinnersTipoEntrada.isEmpty()) return;

        actualizandoTiposEntrada = true;
        int totalAsientosSeleccionados = asientosSeleccionados.size();

        for (JSpinner spinner : spinnersTipoEntrada.values()) {
            int valor = Math.min(obtenerValorSpinner(spinner), totalAsientosSeleccionados);
            setSpinnerSinEvento(spinner, valor, totalAsientosSeleccionados);
        }

        int suma = sumarSpinnersTipoEntrada();
        JSpinner spinnerPrincipal = obtenerSpinnerPrincipal();

        if (totalAsientosSeleccionados == 0) {
            for (JSpinner spinner : spinnersTipoEntrada.values()) {
                setSpinnerSinEvento(spinner, 0, 0);
            }
        } else if (suma == 0 && spinnerPrincipal != null) {
            setSpinnerSinEvento(spinnerPrincipal, totalAsientosSeleccionados, totalAsientosSeleccionados);
        } else if (suma < totalAsientosSeleccionados && spinnerPrincipal != null) {
            setSpinnerSinEvento(spinnerPrincipal, obtenerValorSpinner(spinnerPrincipal) + (totalAsientosSeleccionados - suma), totalAsientosSeleccionados);
        } else if (suma > totalAsientosSeleccionados && spinnerPrincipal != null) {
            setSpinnerSinEvento(spinnerPrincipal, Math.max(0, obtenerValorSpinner(spinnerPrincipal) - (suma - totalAsientosSeleccionados)), totalAsientosSeleccionados);
        }

        actualizandoTiposEntrada = false;
        actualizarTextoTiposEntrada();
    }

    private JSpinner obtenerSpinnerPrincipal() {
        return spinnersTipoEntrada.isEmpty() ? null : spinnersTipoEntrada.values().iterator().next();
    }

    private int sumarSpinnersTipoEntrada() {
        int suma = 0;
        for (JSpinner spinner : spinnersTipoEntrada.values()) {
            suma += obtenerValorSpinner(spinner);
        }
        return suma;
    }

    private void actualizarTextoTiposEntrada() {
        if (lblValidacionTipos == null) return;
        int total = asientosSeleccionados.size();
        lblValidacionTipos.setText(total == 0 ? "Seleccione asientos." : "Tipos asignados: " + sumarSpinnersTipoEntrada() + " / " + total);
    }

    private void setSpinnerSinEvento(JSpinner spinner, int valor, int maximo) {
        spinner.setModel(new SpinnerNumberModel(Math.max(0, valor), 0, Math.max(0, maximo), 1));
    }

    private int obtenerValorSpinner(JSpinner spinner) {
        Object valor = spinner.getValue();
        return valor instanceof Number ? ((Number) valor).intValue() : 0;
    }

    private ArrayList<String> obtenerTiposEntradaSeleccionados() {
        ArrayList<String> tipos = new ArrayList<>();

        for (Map.Entry<String, JSpinner> entry : spinnersTipoEntrada.entrySet()) {
            String tipo = entry.getKey();
            int cantidad = obtenerValorSpinner(entry.getValue());
            for (int i = 0; i < cantidad; i++) {
                tipos.add(tipo);
            }
        }

        while (tipos.size() < asientosSeleccionados.size()) {
            tipos.add(ControlGestionarPagoCINEX.obtenerTipoEntradaPrincipal());
        }

        if (tipos.size() > asientosSeleccionados.size()) {
            return new ArrayList<>(tipos.subList(0, asientosSeleccionados.size()));
        }

        return tipos;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 25, 25, 25));

        JButton btnAtras = crearBotonSecundario("ATRÁS");
        btnConfirmar = crearBotonPrincipal("CONFIRMAR SELECCIÓN");

        btnAtras.addActionListener(e -> regresarSeleccionFuncion());
        btnConfirmar.addActionListener(e -> confirmarSeleccion());
        actualizarEstadoBotonConfirmar();

        footer.add(btnAtras, BorderLayout.WEST);
        footer.add(btnConfirmar, BorderLayout.EAST);

        return footer;
    }

    private void regresarSeleccionFuncion() {
        CINEXTransiciones.cambiar(this, new SeleccionFuncionCINEXGUI(usuarioActual, peliculaSeleccionada));
    }

    private void confirmarSeleccion() {
        if (contarDisponibles() <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "No existen asientos disponibles para esta función",
                    "Función agotada",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (asientosSeleccionados.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Seleccione al menos un asiento para continuar.",
                    "Selección requerida",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        btnConfirmar.setEnabled(false);
        btnConfirmar.setText("VERIFICANDO...");

        SwingWorker<Set<String>, Void> worker = new SwingWorker<Set<String>, Void>() {
            @Override
            protected Set<String> doInBackground() {
                return new LinkedHashSet<>(ControlVerificarDisponibilidadCINEX.consultarAsientosOcupados(peliculaSeleccionada, funcionSeleccionada));
            }

            @Override
            protected void done() {
                try {
                    asientosOcupados.clear();
                    asientosOcupados.addAll(get());
                    aplicarEstadoAsientosAlMapa();

                    for (String asiento : new ArrayList<>(asientosSeleccionados)) {
                        if (asientosOcupados.contains(asiento)) {
                            asientosSeleccionados.remove(asiento);
                            SeatButton boton = botonesAsientos.get(asiento);
                            if (boton != null) {
                                boton.setOcupado(true);
                                boton.setSeleccionado(false);
                            }
                            actualizarResumen();
                            mostrarMensajeAsientoOcupado();
                            btnConfirmar.setText("CONFIRMAR SELECCIÓN");
                            actualizarEstadoBotonConfirmar();
                            return;
                        }
                    }

                    ArrayList<String> seleccionFinal = new ArrayList<>(asientosSeleccionados);
                    boolean registroCorrecto = ControlVerificarDisponibilidadCINEX.guardarSeleccionTemporal(
                            usuarioActual,
                            peliculaSeleccionada,
                            funcionSeleccionada,
                            seleccionFinal
                    );

                    if (!registroCorrecto) {
                        JOptionPane.showMessageDialog(
                                SeleccionAsientosCINEXGUI.this,
                                "No se pudo registrar la selección de asientos. Intente nuevamente.",
                                "Error de registro",
                                JOptionPane.ERROR_MESSAGE
                        );
                        btnConfirmar.setText("CONFIRMAR SELECCIÓN");
                        actualizarEstadoBotonConfirmar();
                        return;
                    }

                    CINEXTransiciones.cambiar(SeleccionAsientosCINEXGUI.this, new SeleccionTipoEntradaCINEXGUI(
                            usuarioActual,
                            peliculaSeleccionada,
                            funcionSeleccionada,
                            tipoSalaFuncion,
                            seleccionFinal
                    ));

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            SeleccionAsientosCINEXGUI.this,
                            "No se pudo verificar la disponibilidad de los asientos.",
                            "Error de verificación",
                            JOptionPane.ERROR_MESSAGE
                    );
                    btnConfirmar.setText("CONFIRMAR SELECCIÓN");
                    actualizarEstadoBotonConfirmar();
                }
            }
        };

        worker.execute();
    }

    private void alternarAsiento(SeatButton seat) {
        String codigo = seat.getCodigo();

        if (asientosSeleccionados.contains(codigo)) {
            asientosSeleccionados.remove(codigo);
            seat.setSeleccionado(false);
        } else {
            asientosSeleccionados.add(codigo);
            seat.setSeleccionado(true);
        }

        actualizarResumen();
    }

    private void actualizarResumen() {
        if (resumenAsientosPanel == null || lblTotal == null) {
            return;
        }

        resumenAsientosPanel.removeAll();

        for (String asiento : asientosSeleccionados) {
            JLabel etiqueta = new JLabel(asiento, SwingConstants.CENTER);
            etiqueta.setOpaque(true);
            etiqueta.setBackground(AMARILLO);
            etiqueta.setForeground(Color.BLACK);
            etiqueta.setFont(new Font("Arial", Font.BOLD, 14));
            etiqueta.setPreferredSize(new Dimension(54, 34));
            resumenAsientosPanel.add(etiqueta);
        }

        lblTotal.setText(String.valueOf(asientosSeleccionados.size()));
        if (lblValidacionTipos != null) {
            lblValidacionTipos.setText(asientosSeleccionados.isEmpty()
                    ? "Seleccione asientos para continuar."
                    : "Continúe para asignar tipo de entrada y precio.");
        }
        actualizarEstadoBotonConfirmar();
        resumenAsientosPanel.revalidate();
        resumenAsientosPanel.repaint();
    }

    private void actualizarEstadoBotonConfirmar() {
        if (btnConfirmar != null) {
            boolean puedeContinuar = contarDisponibles() > 0 && !asientosSeleccionados.isEmpty();
            btnConfirmar.setEnabled(puedeContinuar);
            btnConfirmar.setText("CONFIRMAR SELECCIÓN");
        }
    }

    private void actualizarDisponibilidad() {
        if (lblDisponibles != null) {
            lblDisponibles.setText("Asientos disponibles: " + contarDisponibles() + " / " + totalAsientos);
        }

        actualizarEstadoBotonConfirmar();
    }

    private double calcularTotal() {
        double total = 0.0;

        for (PrecioCINEX precio : preciosEntrada) {
            JSpinner spinner = spinnersTipoEntrada.get(precio.getTipoEntrada());
            int cantidad = spinner == null ? 0 : obtenerValorSpinner(spinner);
            total += cantidad * precio.getMonto();
        }

        return total;
    }

    private JButton crearBotonPrincipal(String texto) {
        JButton btn = new JButton(texto);
        btn.setPreferredSize(new Dimension(270, 64));
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
        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("hh:mm a");
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        lblHora.setText(ahora.format(formatoHora));
        lblFecha.setText(ahora.format(formatoFecha));
    }

    private ImageIcon cargarImagen(String nombre, int ancho, int alto) {
        try {
            File archivo = new File(nombre);

            if (!archivo.exists()) {
                System.out.println("No se encontró la imagen: " + nombre);
                return new ImageIcon();
            }

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
            System.out.println("Error al cargar imagen: " + nombre + " -> " + e.getMessage());
            return new ImageIcon();
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
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (activo) {
                g2.setColor(AMARILLO);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                Polygon punta = new Polygon();
                punta.addPoint(getWidth() - 1, 0);
                punta.addPoint(getWidth() + 22, getHeight() / 2);
                punta.addPoint(getWidth() - 1, getHeight());
                g2.fillPolygon(punta);

                g2.setColor(new Color(5, 20, 55));
            } else {
                g2.setColor(new Color(10, 28, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(160, 170, 185));
            }

            g2.setFont(new Font("Arial", Font.BOLD, 15));
            g2.drawString(texto, 22, 35);
            g2.dispose();
        }
    }

    class PantallaPanel extends JPanel {

        public PantallaPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            GradientPaint gp = new GradientPaint(0, 0, new Color(150, 160, 170), getWidth(), 0, new Color(35, 45, 60));
            g2.setPaint(gp);

            int[] x = {50, getWidth() - 50, getWidth() - 90, 90};
            int[] y = {22, 22, 39, 39};
            g2.fillPolygon(x, y, 4);

            g2.setColor(BLANCO);
            g2.setFont(new Font("Arial", Font.BOLD, 17));
            FontMetrics fm = g2.getFontMetrics();

            String texto = "PANTALLA";
            g2.drawString(texto, (getWidth() - fm.stringWidth(texto)) / 2, 18);
            g2.dispose();
        }
    }

    class SeatButton extends JPanel {

        private final String codigo;
        private boolean ocupado;
        private boolean seleccionado = false;
        private boolean hover = false;

        public SeatButton(String codigo, boolean ocupado) {
            this.codigo = codigo;
            this.ocupado = ocupado;
            setOpaque(false);
            setPreferredSize(new Dimension(58, 32));
            setMinimumSize(new Dimension(58, 32));
            setMaximumSize(new Dimension(58, 32));
            actualizarCursor();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!ocupado) {
                        hover = true;
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        public String getCodigo() {
            return codigo;
        }

        public boolean isOcupado() {
            return ocupado;
        }

        public void setOcupado(boolean ocupado) {
            this.ocupado = ocupado;
            if (ocupado) {
                this.seleccionado = false;
                this.hover = false;
            }
            actualizarCursor();
            repaint();
        }

        public void setSeleccionado(boolean seleccionado) {
            this.seleccionado = seleccionado;
            repaint();
        }

        private void actualizarCursor() {
            if (ocupado) {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            } else {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color color;

            if (ocupado) {
                color = GRIS_OCUPADO;
            } else if (seleccionado) {
                color = AMARILLO;
            } else if (hover) {
                color = new Color(45, 130, 220);
            } else {
                color = AZUL_DISPONIBLE;
            }

            int w = getWidth();
            int h = getHeight();
            int x = 5;
            int y = 5;
            int asientoW = w - 10;
            int asientoH = h - 10;

            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillRoundRect(x + 2, y + 3, asientoW, asientoH, 10, 10);

            g2.setColor(color.darker());
            g2.fillRoundRect(x + 5, y, asientoW - 10, asientoH / 2 + 4, 8, 8);

            g2.setColor(color);
            g2.fillRoundRect(x, y + asientoH / 3, asientoW, asientoH / 2 + 6, 8, 8);

            g2.setColor(color.darker());
            g2.fillRoundRect(x, y + asientoH / 3 + 2, 6, asientoH / 2, 5, 5);
            g2.fillRoundRect(x + asientoW - 6, y + asientoH / 3 + 2, 6, asientoH / 2, 5, 5);

            g2.setColor(new Color(255, 255, 255, 45));
            g2.drawRoundRect(x + 5, y + 2, asientoW - 11, asientoH - 5, 8, 8);

            g2.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(codigo)) / 2;
            int ty = y + (asientoH / 2) + (fm.getAscent() / 2) + 3;
            g2.setColor(seleccionado ? Color.BLACK : Color.WHITE);
            g2.drawString(codigo, tx, ty);

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
        CINEXResponsive.iniciar();
        SwingUtilities.invokeLater(() -> {
            new SeleccionAsientosCINEXGUI("taquillero", "Dune: Parte Dos", "2:30 PM").setVisible(true);
        });
    }
}
