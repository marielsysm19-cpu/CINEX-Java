package interfaz;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import control.BDCINEX;
import control.DocumentoAPI;

public class RegistroClienteCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_PANEL = new Color(8, 24, 55);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS = new Color(185, 190, 200);
    private final Color VERDE = new Color(35, 180, 85);
    private final Color ROJO = new Color(210, 65, 65);
    private final Color AZUL_BORDE = new Color(80, 105, 145);

    private JLabel lblHora;
    private JLabel lblFecha;
    private JLabel lblMensaje;

    private JComboBox<String> cboTipoDocumento;
    private JTextField txtNumeroDocumento;
    private JTextField txtNombreCliente;

    private JButton btnConsultar;
    private JButton btnSiguiente;
    private JButton btnLimpiar;

    private String usuarioActual;
    private boolean documentoValidado = false;
    private boolean clienteYaExiste = false;
    private boolean clienteNuevoAceptado = false;
    private String ultimoTipoDocumento = "";
    private String ultimoNumeroDocumento = "";

    public RegistroClienteCINEXGUI() {
        this("taquillero");
    }

    public RegistroClienteCINEXGUI(String usuario) {
        this.usuarioActual = usuario;

        setTitle("CINEX - Registro de cliente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        CINEXResponsive.configurarVentana(this, 1366, 768, 1120, 680);

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

        actualizarFechaHora();
        Timer timer = new Timer(1000, e -> actualizarFechaHora());
        timer.start();
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

        contenido.add(new SidebarCINEX(0), BorderLayout.WEST);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setPreferredSize(new Dimension(670, 520));
        main.setMinimumSize(new Dimension(650, 500));
        main.setMaximumSize(new Dimension(700, 540));
        main.setBorder(new EmptyBorder(8, 18, 8, 18));

        main.add(crearPanelFormulario(), BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 10, 0, 10);
        wrapper.add(main, gbc);

        contenido.add(wrapper, BorderLayout.CENTER);

        return contenido;
    }

    private JPanel crearPanelFormulario() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Registro de cliente");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 34));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitulo = new JLabel("Ingrese el documento del cliente para continuar con la venta");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 17));
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(titulo);
        panel.add(Box.createVerticalStrut(6));
        panel.add(subtitulo);
        panel.add(Box.createVerticalStrut(22));

        RoundedPanel formulario = new RoundedPanel(18, new Color(5, 18, 43, 190));
        formulario.setLayout(null);
        formulario.setPreferredSize(new Dimension(620, 382));
        formulario.setMaximumSize(new Dimension(620, 382));
        formulario.setAlignmentX(Component.LEFT_ALIGNMENT);
        formulario.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));

        JLabel lblTipo = crearLabelCampo("Tipo de documento:");
        lblTipo.setBounds(36, 34, 180, 25);
        formulario.add(lblTipo);

        cboTipoDocumento = new JComboBox<>(new String[]{"DNI", "C.E."});
        cboTipoDocumento.setBounds(36, 64, 168, 42);
        configurarComboDocumento(cboTipoDocumento);
        formulario.add(cboTipoDocumento);

        JLabel lblNumero = crearLabelCampo("Número de documento:");
        lblNumero.setBounds(236, 34, 220, 25);
        formulario.add(lblNumero);

        txtNumeroDocumento = crearTextField();
        txtNumeroDocumento.setBounds(236, 64, 212, 42);
        formulario.add(txtNumeroDocumento);

        btnConsultar = crearBotonPequeno("CONSULTAR");
        btnConsultar.setBounds(470, 64, 104, 42);
        formulario.add(btnConsultar);

        JLabel lblNombre = crearLabelCampo("Nombre del cliente:");
        lblNombre.setBounds(36, 132, 220, 25);
        formulario.add(lblNombre);

        txtNombreCliente = crearTextField();
        txtNombreCliente.setBounds(36, 162, 538, 42);
        txtNombreCliente.setEditable(false);
        txtNombreCliente.setFocusable(false);
        txtNombreCliente.setToolTipText(
                "El nombre solo se completa desde la base de datos o la consulta de documento."
        );
        formulario.add(txtNombreCliente);

        lblMensaje = new JLabel("Primero consulte el documento del cliente.");
        lblMensaje.setForeground(GRIS);
        lblMensaje.setFont(new Font("Arial", Font.BOLD, 14));
        lblMensaje.setBounds(36, 222, 538, 25);
        formulario.add(lblMensaje);

        btnLimpiar = crearBotonSecundario("LIMPIAR");
        btnLimpiar.setBounds(36, 286, 170, 50);
        formulario.add(btnLimpiar);

        btnSiguiente = crearBotonPrincipal("SIGUIENTE");
        btnSiguiente.setBounds(304, 286, 270, 50);
        formulario.add(btnSiguiente);

        btnConsultar.addActionListener(e -> consultarDocumento());
        btnLimpiar.addActionListener(e -> limpiarFormulario());
        btnSiguiente.addActionListener(e -> continuarAPeliculas());

        aplicarFiltroDocumento();

        cboTipoDocumento.addActionListener(e -> {
            ajustarLongitudDocumentoActual();
            reiniciarEstadoDocumento(false);
            txtNumeroDocumento.requestFocus();
        });

        txtNumeroDocumento.addActionListener(e -> consultarDocumento());

        panel.add(formulario);

        return panel;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 25, 25, 25));

        JButton btnAtras = crearBotonSecundario("ATRÁS");
        btnAtras.setPreferredSize(new Dimension(190, 58));
        btnAtras.addActionListener(e -> {
            dispose();
            new MenuTaquilleroCINEXGUI(usuarioActual).setVisible(true);
        });

        footer.add(btnAtras, BorderLayout.WEST);
        return footer;
    }

    private void consultarDocumento() {
        String tipo = String.valueOf(cboTipoDocumento.getSelectedItem());
        String numero = txtNumeroDocumento.getText().trim();

        reiniciarEstadoDocumento(true);

        if (!validarDocumento(tipo, numero)) {
            mostrarErrorDocumento(tipo, numero);
            return;
        }

        String clienteExistente =
                BDCINEX.buscarClientePorDocumento(tipo, numero);

        if (clienteExistente != null
                && !clienteExistente.trim().isEmpty()) {
            clienteExistente = DocumentoAPI.formatearNombreCliente(
                    clienteExistente
            );
            txtNombreCliente.setText(clienteExistente);

            documentoValidado = true;
            clienteYaExiste = true;
            clienteNuevoAceptado = false;
            ultimoTipoDocumento = tipo;
            ultimoNumeroDocumento = numero;
            mostrarMensaje(
                    "Cliente encontrado. Puede continuar con la venta.",
                    VERDE
            );

            JOptionPane.showMessageDialog(
                    this,
                    "El cliente ya está registrado.",
                    "Cliente registrado",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        consultarNombreEnAPI(tipo, numero);
    }

    private void consultarNombreEnAPI(String tipo, String numero) {
        btnConsultar.setEnabled(false);
        btnConsultar.setText("BUSCANDO...");
        mostrarMensaje(
                "Consultando el documento en RENIEC/API...",
                AMARILLO
        );

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return DocumentoAPI.consultarDocumento(
                        normalizarTipoDocumentoAPI(tipo),
                        numero
                );
            }

            @Override
            protected void done() {
                btnConsultar.setEnabled(true);
                btnConsultar.setText("CONSULTAR");

                try {
                    String nombre = get();

                    if (nombre == null || nombre.trim().isEmpty()) {
                        bloquearDocumentoNoEncontrado(tipo, numero);
                        return;
                    }

                    nombre = DocumentoAPI.formatearNombreCliente(
                            nombre.trim()
                    );

                    if (nombre.isEmpty()) {
                        bloquearDocumentoNoEncontrado(tipo, numero);
                        return;
                    }

                    txtNombreCliente.setText(nombre);
                    documentoValidado = true;
                    clienteYaExiste = false;
                    clienteNuevoAceptado = true;
                    ultimoTipoDocumento = tipo;
                    ultimoNumeroDocumento = numero;

                    mostrarMensaje(
                            "Documento encontrado. El cliente se guardará al confirmar el pago.",
                            VERDE
                    );

                } catch (Exception e) {
                    bloquearDocumentoNoEncontrado(tipo, numero);
                }
            }
        };

        worker.execute();
    }

    private void bloquearDocumentoNoEncontrado(
            String tipo,
            String numero
    ) {
        txtNombreCliente.setText("");
        documentoValidado = false;
        clienteYaExiste = false;
        clienteNuevoAceptado = false;
        ultimoTipoDocumento = "";
        ultimoNumeroDocumento = "";

        mostrarMensaje(
                "Documento no encontrado. No se permite ingresar el nombre manualmente.",
                ROJO
        );

        JOptionPane.showMessageDialog(
                this,
                "No se encontró el " + tipo + " " + numero
                        + " en la consulta de RENIEC/API.\n"
                        + "Verifique el documento antes de continuar.",
                "Documento no encontrado",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void continuarAPeliculas() {
        String tipo = String.valueOf(cboTipoDocumento.getSelectedItem());
        String numero = txtNumeroDocumento.getText().trim();
        String nombre = txtNombreCliente.getText().trim();

        if (!validarDocumento(tipo, numero)) {
            mostrarErrorDocumento(tipo, numero);
            return;
        }

        if (!documentoValidado
                || !tipo.equals(ultimoTipoDocumento)
                || !numero.equals(ultimoNumeroDocumento)) {
            mostrarMensaje(
                    "Consulte y valide el documento antes de continuar.",
                    ROJO
            );
            JOptionPane.showMessageDialog(
                    this,
                    "El documento debe existir en la base de datos o haber sido encontrado por RENIEC/API.",
                    "Validación requerida",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (nombre.isEmpty()) {
            mostrarMensaje(
                    "No se encontró un nombre válido para el documento.",
                    ROJO
            );
            JOptionPane.showMessageDialog(
                    this,
                    "No se permite continuar sin un nombre obtenido de la consulta.",
                    "Cliente no validado",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!clienteYaExiste && !clienteNuevoAceptado) {
            mostrarMensaje(
                    "El cliente nuevo todavía no ha sido validado.",
                    ROJO
            );
            return;
        }

        BDCINEX.prepararClienteParaVenta(tipo, numero, nombre);

        dispose();
        new VentaEntradasCINEXGUI(usuarioActual).setVisible(true);
    }

    private void aplicarFiltroDocumento() {
        AbstractDocument documento = (AbstractDocument) txtNumeroDocumento.getDocument();

        documento.setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String texto, AttributeSet attr) throws BadLocationException {
                replace(fb, offset, 0, texto, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String texto, AttributeSet attrs) throws BadLocationException {
                if (texto == null) {
                    return;
                }

                String soloNumeros = texto.replaceAll("\\D", "");

                if (soloNumeros.isEmpty() && !texto.isEmpty()) {
                    return;
                }

                String actual = fb.getDocument().getText(0, fb.getDocument().getLength());
                int maximo = obtenerLongitudDocumentoEsperada();
                int longitudSinTextoReemplazado = actual.length() - length;
                int espacioDisponible = maximo - longitudSinTextoReemplazado;

                if (espacioDisponible <= 0 && !soloNumeros.isEmpty()) {
                    return;
                }

                if (soloNumeros.length() > espacioDisponible) {
                    soloNumeros = soloNumeros.substring(0, espacioDisponible);
                }

                super.replace(fb, offset, length, soloNumeros, attrs);
            }
        });

        txtNumeroDocumento.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                reiniciarEstadoDocumento(false);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                reiniciarEstadoDocumento(false);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                reiniciarEstadoDocumento(false);
            }
        });
    }

    private void ajustarLongitudDocumentoActual() {
        if (txtNumeroDocumento == null) {
            return;
        }

        String numero = txtNumeroDocumento.getText().replaceAll("\\D", "");
        int maximo = obtenerLongitudDocumentoEsperada();

        if (numero.length() > maximo) {
            numero = numero.substring(0, maximo);
        }

        if (!numero.equals(txtNumeroDocumento.getText())) {
            txtNumeroDocumento.setText(numero);
        }
    }

    private int obtenerLongitudDocumentoEsperada() {
        String tipo = String.valueOf(cboTipoDocumento.getSelectedItem());

        if ("DNI".equalsIgnoreCase(tipo)) {
            return 8;
        }

        return 9;
    }

    private String obtenerNombreTipoDocumento(String tipo) {
        if ("DNI".equalsIgnoreCase(tipo)) {
            return "DNI";
        }

        return "C.E.";
    }

    private String obtenerMensajeDocumentoInvalido(String tipo, String numero) {
        String nombreTipo = obtenerNombreTipoDocumento(tipo);
        int longitudEsperada = obtenerLongitudDocumentoEsperada();

        if (numero == null || numero.trim().isEmpty()) {
            return "Ingrese el número de documento.";
        }

        numero = numero.trim();

        if (!numero.matches("\\d+")) {
            return "El " + nombreTipo + " solo debe contener números.";
        }

        if (numero.length() < longitudEsperada) {
            int faltan = longitudEsperada - numero.length();
            return "El " + nombreTipo + " debe tener " + longitudEsperada + " dígitos. Faltan " + faltan + ".";
        }

        if (numero.length() > longitudEsperada) {
            return "El " + nombreTipo + " no puede tener más de " + longitudEsperada + " dígitos.";
        }

        return "Documento inválido.";
    }

    private void mostrarErrorDocumento(String tipo, String numero) {
        String mensaje = obtenerMensajeDocumentoInvalido(tipo, numero);
        mostrarMensaje(mensaje, ROJO);

        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Documento inválido",
                JOptionPane.WARNING_MESSAGE
        );

        txtNumeroDocumento.requestFocus();
    }

    private boolean validarDocumento(String tipo, String numero) {
        if (tipo == null || numero == null || numero.trim().isEmpty()) {
            return false;
        }

        numero = numero.trim();
        int longitudEsperada = "DNI".equalsIgnoreCase(tipo) ? 8 : 9;

        if ("DNI".equalsIgnoreCase(tipo)) {
            return numero.matches("\\d{" + longitudEsperada + "}");
        }

        if ("C.E.".equalsIgnoreCase(tipo) || "CE".equalsIgnoreCase(tipo)) {
            return numero.matches("\\d{" + longitudEsperada + "}");
        }

        return false;
    }

    private void limpiarFormulario() {
        cboTipoDocumento.setSelectedIndex(0);
        txtNumeroDocumento.setText("");
        txtNombreCliente.setText("");
        reiniciarEstadoDocumento(true);
        mostrarMensaje("Primero consulte el documento del cliente.", GRIS);
        txtNumeroDocumento.requestFocus();
    }

    private void reiniciarEstadoDocumento(boolean limpiarNombre) {
        documentoValidado = false;
        clienteYaExiste = false;
        clienteNuevoAceptado = false;
        ultimoTipoDocumento = "";
        ultimoNumeroDocumento = "";

        if (limpiarNombre && txtNombreCliente != null) {
            txtNombreCliente.setText("");
        }
    }

    private void mostrarMensaje(String mensaje, Color color) {
        lblMensaje.setText(mensaje);
        lblMensaje.setForeground(color);
    }


    private void configurarComboDocumento(JComboBox<String> combo) {
        combo.setFont(new Font("Arial", Font.BOLD, 15));
        combo.setBackground(AZUL_PANEL);
        combo.setForeground(BLANCO);
        combo.setFocusable(false);
        combo.setOpaque(true);
        combo.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        combo.setMaximumRowCount(6);

        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton("▼");
                btn.setBorder(null);
                btn.setFocusPainted(false);
                btn.setContentAreaFilled(true);
                btn.setOpaque(true);
                btn.setBackground(AZUL_PANEL);
                btn.setForeground(BLANCO);
                btn.setFont(new Font("Arial", Font.BOLD, 10));
                return btn;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(AZUL_PANEL);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list,
                        value,
                        index,
                        isSelected,
                        cellHasFocus
                );

                label.setOpaque(true);
                label.setFont(new Font("Arial", Font.BOLD, 15));
                label.setBorder(new EmptyBorder(0, 10, 0, 10));

                if (isSelected) {
                    label.setBackground(AMARILLO);
                    label.setForeground(Color.BLACK);
                } else {
                    label.setBackground(AZUL_PANEL);
                    label.setForeground(BLANCO);
                }

                if (index == -1) {
                    label.setBackground(AZUL_PANEL);
                    label.setForeground(BLANCO);
                }

                return label;
            }
        });
    }

    private String normalizarTipoDocumentoAPI(String tipo) {
        if (tipo == null) {
            return "DNI";
        }

        tipo = tipo.trim().toUpperCase();

        if (tipo.equals("C.E.") || tipo.equals("C.E") || tipo.equals("CE") || tipo.contains("EXTRANJER")) {
            return "CE";
        }

        return "DNI";
    }

    private JLabel crearLabelCampo(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(BLANCO);
        lbl.setFont(new Font("Arial", Font.BOLD, 15));
        return lbl;
    }

    private JTextField crearTextField() {
        JTextField txt = new JTextField();
        txt.setFont(new Font("Arial", Font.BOLD, 15));
        txt.setForeground(BLANCO);
        txt.setBackground(AZUL_PANEL);
        txt.setCaretColor(BLANCO);
        txt.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(80, 105, 145), 1, true),
                new EmptyBorder(0, 12, 0, 12)
        ));
        return txt;
    }

    private JButton crearBotonPrincipal(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(AMARILLO);
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("Arial", Font.BOLD, 15));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        CINEXResponsive.estabilizarBoton(btn, AMARILLO, Color.BLACK);
        return btn;
    }

    private JButton crearBotonSecundario(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(new Color(5, 18, 43));
        btn.setForeground(BLANCO);
        btn.setFont(new Font("Arial", Font.BOLD, 15));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        CINEXResponsive.estabilizarBoton(btn, new Color(5, 18, 43), BLANCO, new Color(38, 48, 70), new Color(180, 190, 205));
        btn.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));
        btn.setBorderPainted(true);
        return btn;
    }

    private JButton crearBotonPequeno(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(AZUL_PANEL);
        btn.setForeground(BLANCO);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        CINEXResponsive.estabilizarBoton(btn, AZUL_PANEL, BLANCO, new Color(38, 48, 70), new Color(180, 190, 205));
        btn.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));
        btn.setBorderPainted(true);
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
        SwingUtilities.invokeLater(() -> {
            new RegistroClienteCINEXGUI("taquillero").setVisible(true);
        });
    }
}
