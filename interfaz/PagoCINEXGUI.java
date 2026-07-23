package interfaz;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import control.ControlGestionarPagoCINEX;
import entidad.PagoCINEX;
import entidad.EntradaCINEX;
import entidad.ReferenciaFuncionCINEX;


public class PagoCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_PANEL = new Color(8, 24, 55);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color VERDE = new Color(0, 190, 75);
    private final Color ROJO = new Color(210, 65, 65);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS = new Color(185, 190, 200);

    private JLabel lblHora;
    private JLabel lblFecha;

    private String usuarioActual;
    private String peliculaSeleccionada;
    private String funcionSeleccionada;
    private List<String> asientosSeleccionados;
    private List<String> tiposEntradaSeleccionados;
    private String tipoSalaFuncion;
    private double totalPagar;

    private String metodoSeleccionado = "Efectivo";

    private MetodoButton btnEfectivo;
    private MetodoButton btnDebito;
    private MetodoButton btnCredito;
    private MetodoButton btnYape;
    private MetodoButton btnPlin;

    private JLabel lblTituloDato;
    private JTextField txtDatoPago;
    private JLabel lblCambioTitulo;
    private JLabel lblCambio;
    private JLabel lblMensajeMetodo;
    private JButton btnPagar;

    private PagoCINEX pagoGenerado;
    private String numeroVentaRegistrada = "";
    private ArrayList<EntradaCINEX> entradasResumen = new ArrayList<>();

    public PagoCINEXGUI() {
        this(
                "taquillero",
                "Dune: Parte Dos",
                "2:30 PM",
                crearAsientosPrueba(),
                20.00
        );
    }

    private static ArrayList<String> crearAsientosPrueba() {
        ArrayList<String> asientos = new ArrayList<>();
        asientos.add("C5");
        asientos.add("C6");
        return asientos;
    }

    public PagoCINEXGUI(String usuario, String pelicula, String funcion, List<String> asientos, double total) {
        this(usuario, pelicula, funcion, asientos, null, total);
    }

    public PagoCINEXGUI(String usuario, String pelicula, String funcion, List<String> asientos, List<String> tiposEntrada, double total) {
        this(usuario, pelicula, funcion, asientos, tiposEntrada, "", total);
    }

    public PagoCINEXGUI(String usuario, String pelicula, String funcion, List<String> asientos, List<String> tiposEntrada, String tipoSalaFuncion, double total) {
        this.usuarioActual = usuario;
        this.peliculaSeleccionada = pelicula;
        this.funcionSeleccionada = funcion;
        this.tipoSalaFuncion = tipoSalaFuncion == null ? "" : tipoSalaFuncion.trim();
        this.asientosSeleccionados = asientos != null ? asientos : new ArrayList<>();
        this.tiposEntradaSeleccionados = normalizarTiposEntrada(tiposEntrada, this.asientosSeleccionados.size());
        this.totalPagar = total;
        this.entradasResumen = ControlGestionarPagoCINEX.solicitarResumenCompra(
                peliculaSeleccionada,
                funcionSeleccionada,
                asientosSeleccionados,
                tiposEntradaSeleccionados,
                this.tipoSalaFuncion,
                totalPagar,
                metodoSeleccionado
        );
        this.pagoGenerado = ControlGestionarPagoCINEX.registrarMetodoPago(metodoSeleccionado, totalPagar);

        setTitle("CINEX - Pago de entrada");
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
                g2.fillOval(getWidth() - 430, 220, 360, 360);
                g2.dispose();
            }
        };

        setContentPane(fondo);
        fondo.add(crearHeader(), BorderLayout.NORTH);
        fondo.add(crearContenido(), BorderLayout.CENTER);

        actualizarFechaHora();
        new Timer(1000, e -> actualizarFechaHora()).start();

        SwingUtilities.invokeLater(this::validarVentaGeneradaAlAbrir);
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

        contenido.add(new SidebarCINEX(4), BorderLayout.WEST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel main = new JPanel(new BorderLayout(28, 0));
        main.setOpaque(false);
        main.setPreferredSize(CINEXResponsive.pantallaPequena() ? new Dimension(1120, 560) : new Dimension(1220, 590));
        main.setBorder(new EmptyBorder(0, 20, 10, 20));

        // Primero se crea el panel de métodos para inicializar btnEfectivo, btnDebito, etc.
        // Luego recién se crea el panel de pago, porque seleccionarMetodo() usa esos botones.
        JPanel panelMetodos = crearPanelMetodos();

        JPanel panelCentral = new JPanel();
        panelCentral.setOpaque(false);
        panelCentral.setLayout(new BoxLayout(panelCentral, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Pago de entrada");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 32));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitulo = new JLabel("Revise el resumen de compra, seleccione el medio de pago y confirme la operación");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 17));
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelCentral.add(titulo);
        panelCentral.add(Box.createVerticalStrut(5));
        panelCentral.add(subtitulo);
        panelCentral.add(Box.createVerticalStrut(18));
        panelCentral.add(crearPanelResumenCompra());
        panelCentral.add(Box.createVerticalStrut(18));
        panelCentral.add(crearPanelPago());

        main.add(panelMetodos, BorderLayout.WEST);
        main.add(panelCentral, BorderLayout.CENTER);

        seleccionarMetodo("Efectivo");

        wrapper.add(CINEXResponsive.envolverConScroll(main), BorderLayout.CENTER);
        contenido.add(wrapper, BorderLayout.CENTER);
        contenido.add(crearFooter(), BorderLayout.SOUTH);

        return contenido;
    }

    private JPanel crearPanelMetodos() {
        RoundedPanel panel = new RoundedPanel(14, new Color(5, 18, 43, 175));
        panel.setLayout(null);
        panel.setPreferredSize(new Dimension(330, 450));
        panel.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));

        JLabel titulo = new JLabel("Medio de pago");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 22));
        titulo.setBounds(28, 24, 220, 30);
        panel.add(titulo);

        btnEfectivo = new MetodoButton("💵", "Efectivo", true);
        btnDebito = new MetodoButton("💳", "Tarjeta Débito", false);
        btnCredito = new MetodoButton("💳", "Tarjeta Crédito", false);
        btnYape = new MetodoButton("📱", "Yape", false);
        btnPlin = new MetodoButton("📲", "Plin", false);

        btnEfectivo.setBounds(24, 76, 282, 62);
        btnDebito.setBounds(24, 148, 282, 62);
        btnCredito.setBounds(24, 220, 282, 62);
        btnYape.setBounds(24, 292, 282, 62);
        btnPlin.setBounds(24, 364, 282, 62);

        panel.add(btnEfectivo);
        panel.add(btnDebito);
        panel.add(btnCredito);
        panel.add(btnYape);
        panel.add(btnPlin);

        btnEfectivo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                seleccionarMetodo("Efectivo");
            }
        });

        btnDebito.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                seleccionarMetodo("Tarjeta Débito");
            }
        });

        btnCredito.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                seleccionarMetodo("Tarjeta Crédito");
            }
        });

        btnYape.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                seleccionarMetodo("Yape");
            }
        });

        btnPlin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                seleccionarMetodo("Plin");
            }
        });

        return panel;
    }

    private JPanel crearPanelResumenCompra() {
        RoundedPanel panel = new RoundedPanel(18, new Color(5, 18, 43, 170));
        panel.setLayout(null);
        panel.setPreferredSize(new Dimension(820, 180));
        panel.setMaximumSize(new Dimension(820, 180));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));

        JLabel titulo = new JLabel("Resumen compra");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 23));
        titulo.setBounds(32, 22, 300, 30);
        panel.add(titulo);

        JLabel pelicula = crearLabelResumen("Película:", peliculaSeleccionada);
        pelicula.setBounds(32, 68, 420, 24);
        panel.add(pelicula);

        JLabel funcion = crearLabelResumen(
                "Función:",
                ReferenciaFuncionCINEX.mostrar(funcionSeleccionada)
        );
        funcion.setBounds(32, 102, 420, 24);
        panel.add(funcion);

        JLabel asientos = crearLabelResumen("Asientos:", String.join(", ", asientosSeleccionados));
        asientos.setBounds(32, 128, 430, 24);
        panel.add(asientos);

        JLabel tipos = crearLabelResumen("Tipos:", resumenTiposEntrada());
        tipos.setBounds(32, 150, 430, 24);
        panel.add(tipos);

        JLabel total = new JLabel("S/ " + String.format("%.2f", totalPagar), SwingConstants.RIGHT);
        total.setForeground(AMARILLO);
        total.setFont(new Font("Arial", Font.BOLD, 38));
        total.setBounds(470, 56, 245, 55);
        panel.add(total);

        JLabel totalTexto = new JLabel("Total a pagar", SwingConstants.RIGHT);
        totalTexto.setForeground(GRIS);
        totalTexto.setFont(new Font("Arial", Font.BOLD, 15));
        totalTexto.setBounds(470, 112, 245, 25);
        panel.add(totalTexto);

        return panel;
    }

    private JLabel crearLabelResumen(String etiqueta, String valor) {
        String textoValor = valor == null || valor.trim().isEmpty() ? "No registrado" : valor;
        JLabel lbl = new JLabel("<html><b>" + etiqueta + "</b> " + textoValor + "</html>");
        lbl.setForeground(GRIS);
        lbl.setFont(new Font("Arial", Font.PLAIN, 15));
        return lbl;
    }

    private JPanel crearPanelPago() {
        RoundedPanel panel = new RoundedPanel(18, new Color(5, 18, 43, 160));
        panel.setLayout(null);
        panel.setPreferredSize(new Dimension(820, 310));
        panel.setMaximumSize(new Dimension(820, 310));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));

        lblTituloDato = new JLabel("Recibido");
        lblTituloDato.setForeground(BLANCO);
        lblTituloDato.setFont(new Font("Arial", Font.BOLD, 20));
        lblTituloDato.setBounds(40, 35, 580, 30);
        panel.add(lblTituloDato);

        txtDatoPago = new JTextField();
        txtDatoPago.setBounds(40, 78, 700, 54);
        txtDatoPago.setBackground(AZUL_PANEL);
        txtDatoPago.setForeground(BLANCO);
        txtDatoPago.setCaretColor(BLANCO);
        txtDatoPago.setFont(new Font("Arial", Font.BOLD, 18));
        txtDatoPago.setHorizontalAlignment(SwingConstants.CENTER);
        txtDatoPago.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(80, 105, 145), 1, true),
                new EmptyBorder(0, 12, 0, 12)
        ));
        panel.add(txtDatoPago);

        lblCambioTitulo = new JLabel("Cambio");
        lblCambioTitulo.setForeground(BLANCO);
        lblCambioTitulo.setFont(new Font("Arial", Font.BOLD, 20));
        lblCambioTitulo.setBounds(40, 160, 130, 30);
        panel.add(lblCambioTitulo);

        lblCambio = new JLabel("S/ 0.00");
        lblCambio.setForeground(VERDE);
        lblCambio.setFont(new Font("Arial", Font.BOLD, 34));
        lblCambio.setBounds(175, 151, 360, 48);
        panel.add(lblCambio);

        lblMensajeMetodo = new JLabel("Ingrese el monto recibido en efectivo.");
        lblMensajeMetodo.setForeground(GRIS);
        lblMensajeMetodo.setFont(new Font("Arial", Font.PLAIN, 15));
        lblMensajeMetodo.setBounds(40, 225, 700, 25);
        panel.add(lblMensajeMetodo);

        txtDatoPago.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (!metodoSeleccionado.equals("Efectivo")) {
                    return;
                }

                char c = e.getKeyChar();
                String texto = txtDatoPago.getText();

                if (Character.isDigit(c)) {
                    return;
                }

                if (c == '.' && !texto.contains(".")) {
                    return;
                }

                if (c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE) {
                    return;
                }

                e.consume();
            }
        });

        txtDatoPago.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizarCambio();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizarCambio();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizarCambio();
            }
        });

        return panel;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 25, 25, 25));

        JButton btnAtras = crearBotonSecundario("ATRÁS");
        btnPagar = crearBotonPrincipal("PAGAR");

        btnAtras.addActionListener(e -> {
            CINEXTransiciones.cambiar(this, new SeleccionTipoEntradaCINEXGUI(
                    usuarioActual,
                    peliculaSeleccionada,
                    funcionSeleccionada,
                    tipoSalaFuncion,
                    asientosSeleccionados
            ));
        });

        btnPagar.addActionListener(e -> procesarPago());

        footer.add(btnAtras, BorderLayout.WEST);
        footer.add(btnPagar, BorderLayout.EAST);
        return footer;
    }

    private void validarVentaGeneradaAlAbrir() {
        if (!existeVentaGenerada()) {
            if (btnPagar != null) {
                btnPagar.setEnabled(false);
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Venta no encontrada. Debe seleccionar película, función y asientos antes de realizar el pago.",
                    "Venta no encontrada",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private boolean existeVentaGenerada() {
        return ControlGestionarPagoCINEX.existeVentaGenerada(
                peliculaSeleccionada,
                funcionSeleccionada,
                asientosSeleccionados,
                totalPagar
        );
    }



    private ArrayList<String> normalizarTiposEntrada(List<String> tiposEntrada, int cantidad) {
        ArrayList<String> tipos = new ArrayList<>();
        String tipoPrincipal = ControlGestionarPagoCINEX.obtenerTipoEntradaPrincipal();

        for (int i = 0; i < cantidad; i++) {
            String tipo = tiposEntrada != null && i < tiposEntrada.size() ? tiposEntrada.get(i) : tipoPrincipal;
            tipos.add(tipo == null || tipo.trim().isEmpty() ? tipoPrincipal : tipo.trim());
        }
        return tipos;
    }

    private String resumenTiposEntrada() {
        Map<String, Integer> resumen = new LinkedHashMap<>();

        for (String tipo : tiposEntradaSeleccionados) {
            String clave = tipo == null || tipo.trim().isEmpty() ? ControlGestionarPagoCINEX.obtenerTipoEntradaPrincipal() : tipo.trim();
            resumen.put(clave, resumen.getOrDefault(clave, 0) + 1);
        }

        StringBuilder texto = new StringBuilder();
        for (Map.Entry<String, Integer> entry : resumen.entrySet()) {
            if (texto.length() > 0) texto.append(" | ");
            texto.append(entry.getKey()).append(": ").append(entry.getValue());
        }

        return texto.length() == 0 ? "Sin entradas" : texto.toString();
    }

    private String normalizarMetodoPago(String metodo) {
        if (metodo == null) {
            return "Efectivo";
        }

        String valor = metodo.trim();

        if (valor.equalsIgnoreCase("Efectivo")) {
            return "Efectivo";
        }
        if (valor.equalsIgnoreCase("Tarjeta")) {
            return "Tarjeta";
        }
        if (valor.equalsIgnoreCase("Tarjeta Débito") || valor.equalsIgnoreCase("Tarjeta Debito")) {
            return "Tarjeta Débito";
        }
        if (valor.equalsIgnoreCase("Tarjeta Crédito") || valor.equalsIgnoreCase("Tarjeta Credito")) {
            return "Tarjeta Crédito";
        }
        if (valor.equalsIgnoreCase("Yape")) {
            return "Yape";
        }
        if (valor.equalsIgnoreCase("Plin")) {
            return "Plin";
        }

        return "Efectivo";
    }

    private void seleccionarMetodo(String metodo) {
        metodoSeleccionado = normalizarMetodoPago(metodo);
        pagoGenerado = ControlGestionarPagoCINEX.registrarMetodoPago(metodoSeleccionado, totalPagar);


        btnEfectivo.setSeleccionado(metodo.equals("Efectivo"));
        btnDebito.setSeleccionado(metodo.equals("Tarjeta Débito"));
        btnCredito.setSeleccionado(metodo.equals("Tarjeta Crédito"));
        btnYape.setSeleccionado(metodo.equals("Yape"));
        btnPlin.setSeleccionado(metodo.equals("Plin"));

        txtDatoPago.setText("");
        txtDatoPago.setHorizontalAlignment(SwingConstants.CENTER);
        txtDatoPago.setForeground(BLANCO);

        if (metodo.equals("Efectivo")) {
            lblTituloDato.setText("Monto recibido");
            txtDatoPago.setEditable(true);
            txtDatoPago.setText("");
            lblCambioTitulo.setVisible(true);
            lblCambio.setVisible(true);
            lblCambio.setForeground(VERDE);
            lblCambio.setText("S/ 0.00");
            lblMensajeMetodo.setText("Ingrese el monto recibido. El sistema validará si el pago está completo.");

        } else if (metodo.equals("Tarjeta Débito") || metodo.equals("Tarjeta Crédito")) {
            lblTituloDato.setText("Operación POS");
            txtDatoPago.setEditable(false);
            txtDatoPago.setText("Presione PAGAR para abrir POS CINEX");
            lblCambioTitulo.setVisible(false);
            lblCambio.setVisible(false);
            lblMensajeMetodo.setText("El sistema validará la transacción de tarjeta antes de confirmar la operación.");

        } else if (metodo.equals("Yape") || metodo.equals("Plin")) {
            lblTituloDato.setText("Pago móvil con " + metodo);
            txtDatoPago.setEditable(false);
            txtDatoPago.setText("Presione PAGAR para mostrar el QR de pago");
            lblCambioTitulo.setVisible(false);
            lblCambio.setVisible(false);
            lblMensajeMetodo.setText("Se mostrará el QR y luego se validará el pago móvil.");
        }

        repaint();
    }

    private void actualizarCambio() {
        if (!metodoSeleccionado.equals("Efectivo")) {
            return;
        }

        try {
            String texto = txtDatoPago.getText().trim();

            if (texto.isEmpty()) {
                lblCambio.setForeground(VERDE);
                lblCambio.setText("S/ 0.00");
                return;
            }

            double recibido = Double.parseDouble(texto);
            double cambio = recibido - totalPagar;

            if (cambio < 0) {
                lblCambio.setForeground(ROJO);
                lblCambio.setText("Falta S/ " + String.format("%.2f", Math.abs(cambio)));
            } else {
                lblCambio.setForeground(VERDE);
                lblCambio.setText("S/ " + String.format("%.2f", cambio));
            }

        } catch (NumberFormatException e) {
            lblCambio.setForeground(ROJO);
            lblCambio.setText("Monto inválido");
        }
    }

    private void procesarPago() {
        if (!existeVentaGenerada()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Venta no encontrada.",
                    "Venta no encontrada",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (metodoSeleccionado.equals("Efectivo")) {
            procesarPagoEfectivo();
            return;
        }

        if (metodoSeleccionado.equals("Tarjeta Débito") || metodoSeleccionado.equals("Tarjeta Crédito")) {
            procesarPagoPOS();
            return;
        }

        if (metodoSeleccionado.equals("Yape") || metodoSeleccionado.equals("Plin")) {
            procesarPagoMovil();
        }
    }

    private void procesarPagoEfectivo() {
        String validacion = ControlGestionarPagoCINEX.validarPagoEfectivo(
                totalPagar,
                txtDatoPago.getText().trim()
        );

        if (!validacion.isEmpty()) {
            String titulo = validacion.toLowerCase().contains("insuficiente")
                    ? "Pago insuficiente"
                    : "Pago rechazado";

            JOptionPane.showMessageDialog(
                    this,
                    validacion,
                    titulo,
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        confirmarOperacion();
    }


    private void procesarPagoPOS() {
        SimuladorPOSCINEX pos = new SimuladorPOSCINEX(this, totalPagar, metodoSeleccionado);
        pos.setVisible(true);

        if (pos.isPagoAprobado()) {
            confirmarOperacion();
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Pago rechazado o cancelado por POS.",
                    "Pago rechazado",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void procesarPagoMovil() {
        SimuladorPagoMovilCINEX pagoMovil = new SimuladorPagoMovilCINEX(
                this,
                totalPagar,
                metodoSeleccionado,
                "imagenes/qryapeplin.png"
        );

        pagoMovil.setVisible(true);

        if (pagoMovil.isPagoAprobado()) {
            confirmarOperacion();
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Pago móvil rechazado o no confirmado.",
                    "Pago rechazado",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void confirmarOperacion() {
        registrarVentaComoPagada();
    }

    private void registrarVentaComoPagada() {
        metodoSeleccionado = normalizarMetodoPago(metodoSeleccionado);

        PagoCINEX pago = ControlGestionarPagoCINEX.registrarPago(
                metodoSeleccionado,
                totalPagar
        );

        if (!ControlGestionarPagoCINEX.confirmarPago(pago)) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo confirmar el pago.",
                    "Pago rechazado",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        ArrayList<EntradaCINEX> entradasPagadas =
                ControlGestionarPagoCINEX.marcarEntradasPagadas(
                        peliculaSeleccionada,
                        funcionSeleccionada,
                        asientosSeleccionados,
                        tiposEntradaSeleccionados,
                        tipoSalaFuncion,
                        pago
                );

        if (entradasPagadas.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudieron preparar las entradas pagadas.",
                    "Error de pago",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        final String numeroVenta =
                ControlGestionarPagoCINEX.generarNumeroVenta();

        btnPagar.setEnabled(false);
        btnPagar.setText("REGISTRANDO...");

        SwingWorker<Boolean, Void> worker =
                new SwingWorker<Boolean, Void>() {

            @Override
            protected Boolean doInBackground() {
                return ControlGestionarPagoCINEX.registrarVentaPagada(
                        numeroVenta,
                        usuarioActual,
                        peliculaSeleccionada,
                        funcionSeleccionada,
                        asientosSeleccionados,
                        tiposEntradaSeleccionados,
                        pago,
                        entradasPagadas
                );
            }

            @Override
            protected void done() {
                try {
                    boolean registrada = get();

                    if (!registrada) {
                        btnPagar.setEnabled(true);
                        btnPagar.setText("PAGAR");

                        JOptionPane.showMessageDialog(
                                PagoCINEXGUI.this,
                                "El pago fue validado, pero no se pudo registrar la venta. No se generó ningún comprobante.",
                                "Venta no registrada",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }

                    pagoGenerado = pago;
                    entradasResumen = entradasPagadas;
                    numeroVentaRegistrada = numeroVenta;

                    abrirConfirmacion();

                } catch (Exception e) {
                    btnPagar.setEnabled(true);
                    btnPagar.setText("PAGAR");

                    JOptionPane.showMessageDialog(
                            PagoCINEXGUI.this,
                            "No se pudo registrar la venta: " + e.getMessage(),
                            "Error de registro",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    private void abrirConfirmacion() {
        CINEXTransiciones.cambiar(
                this,
                new ConfirmacionCINEXGUI(
                        numeroVentaRegistrada,
                        usuarioActual,
                        peliculaSeleccionada,
                        funcionSeleccionada,
                        asientosSeleccionados,
                        tiposEntradaSeleccionados,
                        totalPagar,
                        metodoSeleccionado
                )
        );
    }

    private JButton crearBotonPrincipal(String texto) {
        JButton btn = new JButton(texto);
        btn.setPreferredSize(new Dimension(245, 62));
        btn.setFont(new Font("Arial", Font.BOLD, 17));
        CINEXResponsive.estabilizarBoton(btn, AMARILLO, Color.BLACK);
        return btn;
    }

    private JButton crearBotonSecundario(String texto) {
        JButton btn = CINEXResponsive.botonSecundario(texto, 190, 58);
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
                String base = new File(nombre).getName();
                archivo = new File("Imagenes/" + base);
            }

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
            System.out.println("Error al cargar imagen: " + nombre);
            return new ImageIcon();
        }
    }

    class MetodoButton extends JPanel {

        private final String icono;
        private final String texto;
        private boolean seleccionado;
        private boolean hover = false;

        public MetodoButton(String icono, String texto, boolean seleccionado) {
            this.icono = icono;
            this.texto = texto;
            this.seleccionado = seleccionado;

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

        public void setSeleccionado(boolean seleccionado) {
            this.seleccionado = seleccionado;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color fondo;
            Color textoColor;

            if (seleccionado) {
                fondo = AMARILLO;
                textoColor = Color.BLACK;
            } else if (hover) {
                fondo = new Color(35, 60, 100);
                textoColor = BLANCO;
            } else {
                fondo = AZUL_PANEL;
                textoColor = BLANCO;
            }

            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

            g2.setColor(new Color(80, 105, 145));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

            g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
            g2.setColor(textoColor);
            g2.drawString(icono, 22, 38);

            g2.setFont(new Font("Arial", Font.BOLD, 15));
            g2.drawString(texto, 72, 36);

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
        ArrayList<String> asientos = new ArrayList<>();
        asientos.add("C5");
        asientos.add("C6");

        SwingUtilities.invokeLater(() -> {
            new PagoCINEXGUI(
                    "taquillero",
                    "Dune: Parte Dos",
                    "2:30 PM",
                    asientos,
                    20.00
            ).setVisible(true);
        });
    }
}
