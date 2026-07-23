package interfaz;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.DeflaterOutputStream;
import control.ControlGestionarPagoCINEX;
import control.ControlEmitirComprobanteCINEX;
import entidad.ComprobanteCINEX;
import entidad.ReferenciaFuncionCINEX;


public class ConfirmacionCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_PANEL = new Color(8, 24, 55);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color AZUL_BOTON = new Color(0, 80, 160);
    private final Color VERDE = new Color(35, 180, 85);
    private final Color ROJO = new Color(210, 65, 65);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS = new Color(185, 190, 200);

    private JLabel lblHora;
    private JLabel lblFecha;
    private JLabel lblEstadoOperacion;
    private JLabel lblMensajeTicket;

    private JButton btnGenerarComprobante;
    private JButton btnImprimir;
    private JButton btnNuevaVenta;
    private JButton btnMenuPrincipal;

    private String usuarioActual;
    private String peliculaSeleccionada;
    private String funcionSeleccionada;
    private List<String> asientosSeleccionados;
    private List<String> tiposEntradaSeleccionados;
    private double totalPagado;
    private String metodoPago;

    private String numeroVenta;
    private String fechaVenta;
    private String salaFuncion = "Sala";
    private BufferedImage qrVenta;
    private boolean ventaPagadaValida = false;
    private boolean comprobanteGenerado = false;

    private TicketPanel ticketPanel;
    private ComprobanteCINEX comprobanteActual;

    private static final int ESCALA_PDF = 3;
    private final Map<String, ImageIcon> cacheImagenes = new HashMap<>();

    public ConfirmacionCINEXGUI() {
        this(
                "taquillero",
                "Dune: Parte Dos",
                "2:30 PM",
                crearAsientosPrueba(),
                20.00,
                "Efectivo"
        );
    }

    private static ArrayList<String> crearAsientosPrueba() {
        ArrayList<String> asientos = new ArrayList<>();
        asientos.add("C5");
        asientos.add("C6");
        return asientos;
    }

    public ConfirmacionCINEXGUI(
            String usuario,
            String pelicula,
            String funcion,
            List<String> asientos,
            double total,
            String metodo
    ) {
        this(usuario, pelicula, funcion, asientos, null, total, metodo);
    }

    public ConfirmacionCINEXGUI(
            String usuario,
            String pelicula,
            String funcion,
            List<String> asientos,
            List<String> tiposEntrada,
            double total,
            String metodo
    ) {
        this(
                "",
                usuario,
                pelicula,
                funcion,
                asientos,
                tiposEntrada,
                total,
                metodo
        );
    }

    public ConfirmacionCINEXGUI(
            String numeroVentaRegistrada,
            String usuario,
            String pelicula,
            String funcion,
            List<String> asientos,
            List<String> tiposEntrada,
            double total,
            String metodo
    ) {
        this.numeroVenta = numeroVentaRegistrada == null ? "" : numeroVentaRegistrada.trim();
        this.usuarioActual = usuario;
        this.peliculaSeleccionada = pelicula;
        this.funcionSeleccionada = funcion;
        this.asientosSeleccionados = asientos == null ? new ArrayList<>() : new ArrayList<>(asientos);
        this.tiposEntradaSeleccionados = normalizarTiposEntrada(tiposEntrada, this.asientosSeleccionados.size());
        this.totalPagado = total;
        this.metodoPago = metodo;

        recuperarInformacionVenta();
        validarVentaPagada();

        setTitle("CINEX - Confirmación");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        CINEXResponsive.configurarVentana(this, 1366, 768, 1000, 650);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!comprobanteGenerado) {
                    mostrarErrorComprobanteObligatorio();
                    return;
                }
                dispose();
            }
        });

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
        Timer timer = new Timer(1000, e -> actualizarFechaHora());
        timer.start();

        SwingUtilities.invokeLater(() -> {
            if (!ventaPagadaValida) {
                mostrarMensajeVentaNoPagada();
            }
        });
        }

    private void recuperarInformacionVenta() {
        comprobanteActual = ControlEmitirComprobanteCINEX.recuperarInformacionVenta(
                numeroVenta,
                usuarioActual,
                peliculaSeleccionada,
                funcionSeleccionada,
                asientosSeleccionados,
                tiposEntradaSeleccionados,
                totalPagado,
                metodoPago
        );

        this.numeroVenta = comprobanteActual.getNumeroVenta();
        this.fechaVenta = comprobanteActual.getFechaEmision();
        this.salaFuncion = comprobanteActual.getSala();
        this.peliculaSeleccionada = comprobanteActual.getPelicula();
        this.funcionSeleccionada = comprobanteActual.getFuncion();
        this.asientosSeleccionados = comprobanteActual.obtenerCodigosAsientos();
        this.tiposEntradaSeleccionados = comprobanteActual.obtenerTiposEntrada();
        this.totalPagado = comprobanteActual.getPago() == null ? totalPagado : comprobanteActual.getPago().getTotal();
        this.metodoPago = comprobanteActual.getPago() == null ? metodoPago : comprobanteActual.getPago().getMetodoPago();
    }

    private void validarVentaPagada() {
        ventaPagadaValida = ControlEmitirComprobanteCINEX.ventaEstaPagada(comprobanteActual);
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(6, 25, 2, 25));

        JLabel logo = new JLabel();
        logo.setIcon(cargarImagen("imagenes/logocinex.png", 235, 82));

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 4));
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

        contenido.add(new SidebarCINEX(5), BorderLayout.WEST);

        /*
         * Ajuste visual:
         * El contenido ya no se agrega directamente al CENTER con BorderLayout,
         * porque eso estiraba el comprobante verticalmente y dejaba demasiado espacio vacío.
         * Ahora el bloque principal se mantiene con un tamaño controlado y centrado.
         */
        JPanel mainWrapper = new JPanel(new GridBagLayout());
        mainWrapper.setOpaque(false);

        JPanel main = new JPanel(new BorderLayout(42, 0));
        main.setOpaque(false);

        int anchoMain = CINEXResponsive.pantallaPequena() ? 980 : 1035;
        int altoMain = CINEXResponsive.pantallaPequena() ? 520 : 545;
        main.setPreferredSize(new Dimension(anchoMain, altoMain));
        main.setMinimumSize(new Dimension(950, 500));
        main.setMaximumSize(new Dimension(1060, 560));
        main.setBorder(new EmptyBorder(6, 10, 8, 10));

        JPanel panelIzquierdo = crearPanelVentaComprobante();
        panelIzquierdo.setPreferredSize(new Dimension(640, altoMain));
        panelIzquierdo.setMaximumSize(new Dimension(660, altoMain));

        ticketPanel = new TicketPanel();

        JPanel ticketWrapper = new JPanel(new GridBagLayout());
        ticketWrapper.setOpaque(false);
        ticketWrapper.setPreferredSize(new Dimension(350, altoMain));
        ticketWrapper.setMaximumSize(new Dimension(350, altoMain));

        GridBagConstraints ticketGbc = new GridBagConstraints();
        ticketGbc.gridx = 0;
        ticketGbc.gridy = 0;
        ticketGbc.anchor = GridBagConstraints.CENTER;
        ticketWrapper.add(ticketPanel, ticketGbc);

        main.add(panelIzquierdo, BorderLayout.CENTER);
        main.add(ticketWrapper, BorderLayout.EAST);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 8, 0, 8);
        mainWrapper.add(main, gbc);

        contenido.add(mainWrapper, BorderLayout.CENTER);
        contenido.add(crearFooter(), BorderLayout.SOUTH);

        return contenido;
    }

    private JPanel crearPanelVentaComprobante() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel encabezado = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        encabezado.setOpaque(false);
        encabezado.setAlignmentX(Component.LEFT_ALIGNMENT);

        CheckPanel check = new CheckPanel();
        check.setPreferredSize(new Dimension(76, 76));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Confirmación");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 31));

        JLabel subtitulo = new JLabel("Recupere la venta pagada y genere el comprobante para el cliente.");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 17));

        textos.add(Box.createVerticalStrut(6));
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(7));
        textos.add(subtitulo);

        encabezado.add(check);
        encabezado.add(textos);

        panel.add(encabezado);
        panel.add(Box.createVerticalStrut(18));

        RoundedPanel resumen = new RoundedPanel(18, new Color(5, 18, 43, 175));
        resumen.setLayout(null);
        resumen.setPreferredSize(new Dimension(610, 405));
        resumen.setMaximumSize(new Dimension(610, 405));
        resumen.setAlignmentX(Component.LEFT_ALIGNMENT);
        resumen.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));

        JLabel lblResumen = new JLabel("Resumen compra");
        lblResumen.setForeground(BLANCO);
        lblResumen.setFont(new Font("Arial", Font.BOLD, 22));
        lblResumen.setBounds(35, 25, 380, 30);
        resumen.add(lblResumen);

        agregarFilaResumen(resumen, "Venta:", numeroVenta, 75);
        agregarFilaResumen(resumen, "Película:", peliculaSeleccionada, 113);
        agregarFilaResumen(
                resumen,
                "Función:",
                ReferenciaFuncionCINEX.mostrar(funcionSeleccionada),
                151
        );
        agregarFilaResumen(resumen, "Sala:", salaFuncion, 189);
        agregarFilaResumen(resumen, "Entradas:", String.join(", ", asientosSeleccionados), 227);
        agregarFilaResumen(resumen, "Tipos:", resumenTiposEntrada(), 265);
        agregarFilaResumen(resumen, "Método:", metodoPago, 303);
        agregarFilaResumen(resumen, "Monto pagado:", "S/ " + String.format("%.2f", totalPagado), 331);

        lblEstadoOperacion = new JLabel();
        lblEstadoOperacion.setFont(new Font("Arial", Font.BOLD, 15));
        lblEstadoOperacion.setBounds(35, 368, 540, 25);
        resumen.add(lblEstadoOperacion);

        if (ventaPagadaValida) {
            lblEstadoOperacion.setText("Venta pagada recuperada correctamente.");
            lblEstadoOperacion.setForeground(VERDE);
        } else {
            lblEstadoOperacion.setText("No se puede generar el comprobante porque la venta aún no ha sido pagada.");
            lblEstadoOperacion.setForeground(ROJO);
        }

        panel.add(resumen);

        return panel;
    }

    private void agregarFilaResumen(JPanel panel, String campo, String valor, int y) {
        JLabel lblCampo = new JLabel(campo);
        lblCampo.setForeground(GRIS);
        lblCampo.setFont(new Font("Arial", Font.BOLD, 16));
        lblCampo.setBounds(35, y, 135, 25);
        panel.add(lblCampo);

        JLabel lblValor = new JLabel(valor == null ? "" : valor);
        lblValor.setForeground(BLANCO);
        lblValor.setFont(new Font("Arial", Font.BOLD, 16));
        lblValor.setBounds(175, y, 390, 25);
        panel.add(lblValor);
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 25, 25, 25));

        btnMenuPrincipal = crearBotonAzul("MENÚ PRINCIPAL");
        btnGenerarComprobante = crearBotonPrincipal("EMITIR COMPROBANTE");
        btnImprimir = crearBotonAzul("IMPRIMIR");

        btnMenuPrincipal.addActionListener(e ->
                intentarSalirDespuesDelComprobante(
                        new MenuTaquilleroCINEXGUI(usuarioActual)
                )
        );

        btnGenerarComprobante.addActionListener(e -> generarComprobante());
        btnImprimir.addActionListener(e -> imprimirTicket());

        btnGenerarComprobante.setEnabled(ventaPagadaValida);
        btnImprimir.setEnabled(false);

        JPanel botonesIzquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        botonesIzquierda.setOpaque(false);
        botonesIzquierda.add(btnMenuPrincipal);

        JPanel botonesDerecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 22, 0));
        botonesDerecha.setOpaque(false);
        botonesDerecha.add(btnGenerarComprobante);
        botonesDerecha.add(btnImprimir);

        footer.add(botonesIzquierda, BorderLayout.WEST);
        footer.add(botonesDerecha, BorderLayout.EAST);

        return footer;
    }

    private void intentarSalirDespuesDelComprobante(JFrame destino) {
        if (!comprobanteGenerado) {
            mostrarErrorComprobanteObligatorio();
            return;
        }

        CINEXTransiciones.cambiar(this, destino);
    }

    private void mostrarErrorComprobanteObligatorio() {
        JOptionPane.showMessageDialog(
                this,
                "Debe generar el comprobante antes de volver al menú principal.",
                "Comprobante obligatorio",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void generarComprobante() {
        if (!ventaPagadaValida) {
            mostrarMensajeVentaNoPagada();
            return;
        }

        btnGenerarComprobante.setEnabled(false);
        btnGenerarComprobante.setText("EMITIDO");

        final String asientosTexto = String.join(", ", asientosSeleccionados);

        this.qrVenta = GeneradorQR.generarQRVentaComoImagen(
                numeroVenta,
                peliculaSeleccionada,
                ReferenciaFuncionCINEX.mostrar(funcionSeleccionada),
                salaFuncion,
                asientosTexto,
                totalPagado,
                metodoPago,
                usuarioActual,
                fechaVenta,
                220
        );

        comprobanteGenerado = true;
        lblEstadoOperacion.setText("Comprobante generado.");
        lblEstadoOperacion.setForeground(VERDE);
        lblMensajeTicket.setText("Comprobante listo para imprimir.");
        lblMensajeTicket.setForeground(VERDE);
        ticketPanel.repaint();

        btnImprimir.setEnabled(true);

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String rutaQR = "";

            @Override
            protected Boolean doInBackground() {
                try {
                    rutaQR = GeneradorQR.guardarQRVentaPNG(numeroVenta, qrVenta);

                    if (rutaQR == null) {
                        rutaQR = "";
                    }

                    comprobanteActual.setRutaQR(rutaQR);
                    comprobanteActual.setQrGenerado(true);

                    return ControlEmitirComprobanteCINEX.registrarComprobante(
                            comprobanteActual,
                            rutaQR
                    );

                } catch (Exception e) {
                    System.out.println("Registro BD pendiente: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean comprobanteRegistrado = get();

                    if (comprobanteRegistrado) {
                        lblEstadoOperacion.setText("Comprobante registrado correctamente. La venta ya estaba registrada desde el pago.");
                        lblEstadoOperacion.setForeground(VERDE);
                        lblMensajeTicket.setText("Comprobante generado correctamente.");
                        lblMensajeTicket.setForeground(VERDE);
                    } else {
                        lblEstadoOperacion.setText("Comprobante generado.");
                        lblEstadoOperacion.setForeground(AMARILLO);
                        lblMensajeTicket.setText("Comprobante listo para imprimir.");
                        lblMensajeTicket.setForeground(VERDE);
                    }

                    ticketPanel.repaint();

                } catch (Exception e) {
                    lblEstadoOperacion.setText("Comprobante generado.");
                    lblEstadoOperacion.setForeground(AMARILLO);
                    lblMensajeTicket.setText("Comprobante listo para imprimir.");
                    lblMensajeTicket.setForeground(VERDE);
                    ticketPanel.repaint();
                    System.out.println(" " + e.getMessage());
                }
            }
        };

        worker.execute();

        JOptionPane.showMessageDialog(
                this,
                "Comprobante generado. Ya puede imprimirlo.",
                "Comprobante listo",
                JOptionPane.INFORMATION_MESSAGE
        );
    }


    private void mostrarMensajeVentaNoPagada() {
        JOptionPane.showMessageDialog(
                this,
                "No se puede generar el comprobante porque la venta aún no ha sido pagada.",
                "Venta no pagada",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private JButton crearBotonPrincipal(String texto) {
        JButton btn = new JButton(texto);
        btn.setPreferredSize(new Dimension(285, 62));
        btn.setFont(new Font("Arial", Font.BOLD, 15));
        CINEXResponsive.estabilizarBoton(btn, AMARILLO, Color.BLACK, new Color(60, 76, 105), new Color(235, 240, 248));
        return btn;
    }

    private JButton crearBotonAzul(String texto) {
        JButton btn = new JButton(texto);
        btn.setPreferredSize(new Dimension(220, 62));
        btn.setFont(new Font("Arial", Font.BOLD, 15));
        CINEXResponsive.estabilizarBoton(btn, AZUL_BOTON, BLANCO, new Color(60, 76, 105), new Color(235, 240, 248));
        return btn;
    }

    private void imprimirTicket() {
        if (!comprobanteGenerado) {
            JOptionPane.showMessageDialog(
                    this,
                    "Primero debe generar el comprobante.",
                    "Comprobante requerido",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar comprobante CINEX");
        chooser.setSelectedFile(new File("comprobante_" + limpiarNombreArchivo(numeroVenta) + ".pdf"));

        int opcion = chooser.showSaveDialog(this);

        if (opcion != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File archivo = chooser.getSelectedFile();

        if (!archivo.getName().toLowerCase().endsWith(".pdf")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".pdf");
        }

        final File archivoFinal = archivo;
        final BufferedImage imagenTicket = renderizarTicketComoImagen();

        btnImprimir.setEnabled(false);
        btnImprimir.setText("GUARDANDO...");

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                escribirPDFConImagen(archivoFinal, imagenTicket);
                return null;
            }

            @Override
            protected void done() {
                btnImprimir.setEnabled(true);
                btnImprimir.setText("IMPRIMIR");

                try {
                    get();
                    JOptionPane.showMessageDialog(
                            ConfirmacionCINEXGUI.this,
                            "Comprobante PDF guardado correctamente.",
                            "PDF generado",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            ConfirmacionCINEXGUI.this,
                            "No se pudo guardar el comprobante PDF.",
                            "Error al guardar",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    private BufferedImage renderizarTicketComoImagen() {
        int anchoBase = Math.max(ticketPanel.getWidth(), ticketPanel.getPreferredSize().width);
        int altoBase = Math.max(ticketPanel.getHeight(), ticketPanel.getPreferredSize().height);

        int ancho = anchoBase * ESCALA_PDF;
        int alto = altoBase * ESCALA_PDF;

        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = imagen.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, ancho, alto);

        Dimension anterior = ticketPanel.getSize();
        ticketPanel.setSize(anchoBase, altoBase);
        ticketPanel.doLayout();

        g2.scale(ESCALA_PDF, ESCALA_PDF);
        ticketPanel.printAll(g2);

        ticketPanel.setSize(anterior);
        g2.dispose();
        return imagen;
    }

    private void escribirPDFConImagen(File archivo, BufferedImage imagen) throws IOException {
        BufferedImage rgb = new BufferedImage(imagen.getWidth(), imagen.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = rgb.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        g2.drawImage(imagen, 0, 0, null);
        g2.dispose();

        ByteArrayOutputStream rawBytes = new ByteArrayOutputStream(rgb.getWidth() * rgb.getHeight() * 3);

        for (int y = 0; y < rgb.getHeight(); y++) {
            for (int x = 0; x < rgb.getWidth(); x++) {
                int color = rgb.getRGB(x, y);
                rawBytes.write((color >> 16) & 0xFF);
                rawBytes.write((color >> 8) & 0xFF);
                rawBytes.write(color & 0xFF);
            }
        }

        ByteArrayOutputStream imagenComprimida = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(imagenComprimida)) {
            rawBytes.writeTo(deflater);
        }

        byte[] imagenPDF = imagenComprimida.toByteArray();

        int anchoPagina = Math.max(1, rgb.getWidth() / ESCALA_PDF);
        int altoPagina = Math.max(1, rgb.getHeight() / ESCALA_PDF);

        String contenido = "q\n" + anchoPagina + " 0 0 " + altoPagina + " 0 0 cm\n" +
                "/Im0 Do\n" +
                "Q\n";
        byte[] contenidoBytes = contenido.getBytes(StandardCharsets.ISO_8859_1);

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        ArrayList<Integer> offsets = new ArrayList<>();
        offsets.add(0);

        escribirAscii(pdf, "%PDF-1.4\n%âãÏÓ\n");

        escribirObjeto(pdf, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>\n");
        escribirObjeto(pdf, offsets, 2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n");
        escribirObjeto(pdf, offsets, 3,
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + anchoPagina + " " + altoPagina + "] " +
                        "/Resources << /XObject << /Im0 4 0 R >> >> /Contents 5 0 R >>\n");

        offsets.add(pdf.size());
        escribirAscii(pdf, "4 0 obj\n");
        escribirAscii(pdf,
                "<< /Type /XObject /Subtype /Image /Width " + rgb.getWidth() +
                        " /Height " + rgb.getHeight() +
                        " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length " + imagenPDF.length + " >>\nstream\n");
        pdf.write(imagenPDF);
        escribirAscii(pdf, "endstream\nendobj\n");

        offsets.add(pdf.size());
        escribirAscii(pdf, "5 0 obj\n");
        escribirAscii(pdf, "<< /Length " + contenidoBytes.length + " >>\nstream\n");
        pdf.write(contenidoBytes);
        escribirAscii(pdf, "endstream\nendobj\n");

        int inicioXref = pdf.size();
        escribirAscii(pdf, "xref\n0 6\n");
        escribirAscii(pdf, "0000000000 65535 f\n");
        for (int i = 1; i <= 5; i++) {
            escribirAscii(pdf, String.format("%010d 00000 n\n", offsets.get(i)));
        }
        escribirAscii(pdf, "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + inicioXref + "\n%%EOF");

        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            pdf.writeTo(fos);
        }
    }

    private void escribirObjeto(ByteArrayOutputStream out, ArrayList<Integer> offsets, int numero, String contenido) throws IOException {
        offsets.add(out.size());
        escribirAscii(out, numero + " 0 obj\n");
        escribirAscii(out, contenido);
        escribirAscii(out, "endobj\n");
    }

    private void escribirAscii(ByteArrayOutputStream out, String texto) throws IOException {
        out.write(texto.getBytes(StandardCharsets.ISO_8859_1));
    }

    private String limpiarNombreArchivo(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "comprobante";
        }

        return texto.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private void actualizarFechaHora() {
        LocalDateTime ahora = LocalDateTime.now();

        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("hh:mm a");
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        lblHora.setText(ahora.format(formatoHora));
        lblFecha.setText(ahora.format(formatoFecha));
    }

    private ImageIcon cargarImagen(String nombre, int ancho, int alto) {
        String clave = nombre + "|" + ancho + "x" + alto;

        if (cacheImagenes.containsKey(clave)) {
            return cacheImagenes.get(clave);
        }

        try {
            File archivo = new File(nombre);

            if (!archivo.exists()) {
                File alternativo = new File("Imagenes/" + new File(nombre).getName());
                if (alternativo.exists()) {
                    archivo = alternativo;
                }
            }

            if (!archivo.exists()) {
                ImageIcon vacio = new ImageIcon();
                cacheImagenes.put(clave, vacio);
                return vacio;
            }

            BufferedImage original = ImageIO.read(archivo);

            if (original == null) {
                ImageIcon vacio = new ImageIcon();
                cacheImagenes.put(clave, vacio);
                return vacio;
            }

            BufferedImage escalada = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g2 = escalada.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(original, 0, 0, ancho, alto, null);
            g2.dispose();

            ImageIcon icono = new ImageIcon(escalada);
            cacheImagenes.put(clave, icono);
            return icono;

        } catch (Exception e) {
            ImageIcon vacio = new ImageIcon();
            cacheImagenes.put(clave, vacio);
            return vacio;
        }
    }



    class TicketPanel extends JPanel {

        public TicketPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(330, 530));
            setMinimumSize(new Dimension(330, 530));
            setMaximumSize(new Dimension(330, 530));
            setLayout(null);

            lblMensajeTicket = new JLabel("Comprobante pendiente de emisión", SwingConstants.CENTER);
            lblMensajeTicket.setForeground(GRIS);
            lblMensajeTicket.setFont(new Font("Arial", Font.BOLD, 13));
            lblMensajeTicket.setBounds(30, 500, 270, 24);
            add(lblMensajeTicket);

        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int x = 15;
            int y = 8;
            int w = getWidth() - 30;
            int h = getHeight() - 48;

            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRoundRect(x + 8, y + 8, w, h, 12, 12);

            g2.setColor(new Color(250, 250, 250));
            g2.fillRoundRect(x, y, w, h, 12, 12);

            g2.setColor(new Color(220, 220, 220));
            g2.drawRoundRect(x, y, w, h, 12, 12);

            ImageIcon logo = cargarImagen("imagenes/logocinex2.png", 145, 70);
            logo.paintIcon(this, g2, x + 80, y + 18);

            if (comprobanteGenerado) {
                dibujarQRReal(g2, x + 80, y + 78, 145, 145);

                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.BOLD, 13));
                dibujarTextoCentrado(g2, numeroVenta, x, y + 255, w);

                g2.setFont(new Font("Arial", Font.BOLD, 17));
                dibujarTextoCentrado(g2, peliculaSeleccionada, x, y + 284, w);

                g2.setFont(new Font("Arial", Font.PLAIN, 15));
                dibujarTextoCentrado(
                        g2,
                        ReferenciaFuncionCINEX.mostrar(funcionSeleccionada)
                                + "     "
                                + fechaVenta.substring(0, 10),
                        x,
                        y + 314,
                        w
                );

                g2.setFont(new Font("Arial", Font.PLAIN, 15));
                dibujarTextoCentrado(g2, salaFuncion, x, y + 342, w);
                dibujarTextoCentrado(g2, "Asientos: " + String.join(", ", asientosSeleccionados), x, y + 370, w);
                dibujarTextoCentrado(g2, resumenTiposEntrada(), x, y + 398, w);
                dibujarTextoCentrado(g2, "Total: S/ " + String.format("%.2f", totalPagado), x, y + 426, w);

                g2.setColor(new Color(190, 190, 190));
                g2.drawLine(x + 40, y + 440, x + w - 40, y + 440);

                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.BOLD, 17));
                dibujarTextoCentrado(g2, "¡Disfrute su función!", x, y + 468, w);
            } else {
                g2.setColor(new Color(240, 240, 240));
                g2.fillRoundRect(x + 70, y + 95, 160, 150, 12, 12);

                g2.setColor(new Color(120, 120, 120));
                g2.setFont(new Font("Arial", Font.BOLD, 15));
                dibujarTextoCentrado(g2, "COMPROBANTE", x, y + 160, w);
                dibujarTextoCentrado(g2, "PENDIENTE", x, y + 184, w);

                g2.setColor(new Color(190, 190, 190));
                g2.drawLine(x + 40, y + 310, x + w - 40, y + 310);

                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.BOLD, 15));
                dibujarTextoCentrado(g2, "Solicite generar comprobante", x, y + 350, w);
            }

            g2.dispose();
        }

        private void dibujarQRReal(Graphics2D g2, int x, int y, int ancho, int alto) {
            if (qrVenta != null) {
                g2.drawImage(qrVenta, x, y, ancho, alto, null);
            } else {
                g2.setColor(Color.WHITE);
                g2.fillRect(x, y, ancho, alto);
                g2.setColor(Color.BLACK);
                g2.drawRect(x, y, ancho, alto);
                g2.setFont(new Font("Arial", Font.BOLD, 13));
                g2.drawString("QR NO GENERADO", x + 15, y + alto / 2);
            }
        }

        private void dibujarTextoCentrado(Graphics2D g2, String texto, int x, int y, int ancho) {
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (ancho - fm.stringWidth(texto == null ? "" : texto)) / 2;
            g2.drawString(texto == null ? "" : texto, tx, y);
        }
    }

    class CheckPanel extends JPanel {

        public CheckPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(ventaPagadaValida ? VERDE : ROJO);
            g2.fillOval(5, 5, getWidth() - 10, getHeight() - 10);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            if (ventaPagadaValida) {
                int[] xs = {22, 36, 58};
                int[] ys = {40, 54, 26};
                g2.drawPolyline(xs, ys, 3);
            } else {
                g2.drawLine(25, 25, 55, 55);
                g2.drawLine(55, 25, 25, 55);
            }

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

            RoundRectangle2D round = new RoundRectangle2D.Double(
                    0, 0, getWidth() - 1, getHeight() - 1, radio, radio
            );

            g2.setColor(color);
            g2.fill(round);

            g2.dispose();
        }
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

    public static void main(String[] args) {
        ArrayList<String> asientos = new ArrayList<>();
        asientos.add("C71");
        asientos.add("C80");

        SwingUtilities.invokeLater(() -> {
            new ConfirmacionCINEXGUI(
                    "taquillero",
                    "Dune: Parte Dos",
                    "2:30 PM",
                    asientos,
                    20.00,
                    "Efectivo"
            ).setVisible(true);
        });
    }
}
