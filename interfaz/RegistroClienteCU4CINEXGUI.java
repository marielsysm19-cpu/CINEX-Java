package interfaz;

import control.ControlValidarClienteCINEX;
import control.DocumentoAPI;
import entidad.ClienteCINEX;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RegistroClienteCU4CINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_PANEL = new Color(8, 24, 55);
    private final Color AZUL_CARD = new Color(5, 18, 43);
    private final Color AZUL_BORDE = new Color(80, 105, 145);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS = new Color(185, 190, 200);
    private final Color VERDE = new Color(35, 180, 85);
    private final Color ROJO = new Color(210, 65, 65);

    private JLabel lblHora;
    private JLabel lblFecha;
    private JLabel lblMensaje;
    private JLabel lblEstadoFlujo;

    private JComboBox<String> cboTipoDocumento;
    private JTextField txtNumeroDocumento;
    private JTextField txtNombreCliente;

    private JButton btnValidar;
    private JButton btnGuardar;
    private JButton btnLimpiar;
    private JButton btnMenu;

    private String usuarioActual;
    private boolean datosValidados = false;
    private boolean clienteExistente = false;
    private String documentoValidado = "";

    public RegistroClienteCU4CINEXGUI() {
        this("taquillero");
    }

    public RegistroClienteCU4CINEXGUI(String usuario) {
        this.usuarioActual = usuario == null || usuario.trim().isEmpty() ? "taquillero" : usuario.trim();

        setTitle("CINEX - Registro de cliente");
        setSize(1366, 768);
        setMinimumSize(new Dimension(1180, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

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

        configurarAcciones();
        configurarFiltros();
        mostrarMensaje("Ingrese los datos del cliente y presione VALIDAR.", GRIS);
        mostrarEstado("Complete los datos del cliente para iniciar el registro.");

        actualizarFechaHora();
        new Timer(1000, e -> actualizarFechaHora()).start();
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(8, 25, 4, 25));

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

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setPreferredSize(new Dimension(760, 455));
        main.setMaximumSize(new Dimension(800, 470));
        main.add(crearPanelFormulario(), BorderLayout.CENTER);

        wrapper.add(main, new GridBagConstraints());
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

        JLabel subtitulo = new JLabel("Ingrese la información del cliente para registrarlo en el sistema.");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 17));
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(titulo);
        panel.add(Box.createVerticalStrut(6));
        panel.add(subtitulo);
        panel.add(Box.createVerticalStrut(20));

        RoundedPanel formulario = new RoundedPanel(18, new Color(5, 18, 43, 185));
        formulario.setLayout(null);
        formulario.setPreferredSize(new Dimension(700, 340));
        formulario.setMaximumSize(new Dimension(700, 340));
        formulario.setAlignmentX(Component.LEFT_ALIGNMENT);
        formulario.setBorder(new LineBorder(AZUL_BORDE, 1, true));

        JLabel lblTipo = crearLabelCampo("Tipo de documento:");
        lblTipo.setBounds(36, 32, 190, 25);
        formulario.add(lblTipo);

        cboTipoDocumento = new JComboBox<>(new String[]{"DNI", "C.E."});
        cboTipoDocumento.setBounds(36, 62, 175, 44);
        configurarComboDocumento(cboTipoDocumento);
        formulario.add(cboTipoDocumento);

        JLabel lblNumero = crearLabelCampo("Número de documento:");
        lblNumero.setBounds(245, 32, 230, 25);
        formulario.add(lblNumero);

        txtNumeroDocumento = crearTextField();
        txtNumeroDocumento.setBounds(245, 62, 230, 44);
        formulario.add(txtNumeroDocumento);

        btnValidar = crearBotonAzul("VALIDAR");
        btnValidar.setBounds(500, 62, 150, 44);
        formulario.add(btnValidar);

        JLabel lblNombre = crearLabelCampo("Nombre completo del cliente:");
        lblNombre.setBounds(36, 128, 280, 25);
        formulario.add(lblNombre);

        txtNombreCliente = crearTextField();
        txtNombreCliente.setBounds(36, 158, 614, 44);
        txtNombreCliente.setEditable(false);
        txtNombreCliente.setFocusable(false);
        txtNombreCliente.setToolTipText(
                "El nombre solo se completa al encontrar el documento en la base de datos o RENIEC/API."
        );
        formulario.add(txtNombreCliente);

        lblMensaje = new JLabel();
        lblMensaje.setFont(new Font("Arial", Font.BOLD, 14));
        lblMensaje.setBounds(36, 220, 614, 24);
        formulario.add(lblMensaje);

        lblEstadoFlujo = new JLabel("Complete los datos del cliente para iniciar el registro.");
        lblEstadoFlujo.setForeground(GRIS);
        lblEstadoFlujo.setFont(new Font("Arial", Font.BOLD, 13));
        lblEstadoFlujo.setBounds(36, 247, 614, 24);
        formulario.add(lblEstadoFlujo);

        btnLimpiar = crearBotonSecundario("LIMPIAR");
        btnLimpiar.setBounds(36, 285, 190, 50);
        formulario.add(btnLimpiar);

        btnGuardar = crearBotonPrincipal("GUARDAR CLIENTE");
        btnGuardar.setBounds(360, 285, 290, 50);
        formulario.add(btnGuardar);

        panel.add(formulario);
        return panel;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(8, 25, 22, 25));

        btnMenu = crearBotonSecundario("MENÚ PRINCIPAL");
        btnMenu.setPreferredSize(new Dimension(220, 56));
        btnMenu.addActionListener(e -> volverMenu());
        footer.add(btnMenu, BorderLayout.WEST);
        return footer;
    }

    private void configurarAcciones() {
        btnValidar.addActionListener(this::validarCliente);
        btnGuardar.addActionListener(this::guardarCliente);
        btnLimpiar.addActionListener(e -> limpiarFormulario());
        txtNumeroDocumento.addActionListener(this::validarCliente);
        txtNombreCliente.addActionListener(this::guardarCliente);

        cboTipoDocumento.addActionListener(e -> {
            ajustarLongitudDocumentoActual();
            reiniciarValidacion();
            mostrarEstado("Complete los datos del cliente para iniciar el registro.");
            txtNumeroDocumento.requestFocus();
        });

        KeyAdapter reiniciar = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                reiniciarValidacion();
                mostrarEstado("Complete los datos del cliente para iniciar el registro.");
            }
        };

        txtNumeroDocumento.addKeyListener(reiniciar);
        txtNombreCliente.addKeyListener(reiniciar);
    }

    private void configurarFiltros() {
        aplicarFiltroNumerico(txtNumeroDocumento, () -> ControlValidarClienteCINEX.obtenerLongitudDocumento(String.valueOf(cboTipoDocumento.getSelectedItem())));
        aplicarFiltroSoloLetras(txtNombreCliente);
    }

    private void aplicarFiltroNumerico(JTextField campo, LongitudMaxima proveedorLongitud) {
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String texto, AttributeSet attr) throws BadLocationException {
                replace(fb, offset, 0, texto, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String texto, AttributeSet attrs) throws BadLocationException {
                if (texto == null) return;
                String numeros = texto.replaceAll("\\D", "");
                if (numeros.isEmpty() && !texto.isEmpty()) return;
                String actual = fb.getDocument().getText(0, fb.getDocument().getLength());
                int max = proveedorLongitud.obtener();
                int espacio = max - (actual.length() - length);
                if (espacio <= 0 && !numeros.isEmpty()) return;
                if (numeros.length() > espacio) numeros = numeros.substring(0, espacio);
                super.replace(fb, offset, length, numeros, attrs);
            }
        });
    }

    private void aplicarFiltroSoloLetras(JTextField campo) {
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String texto, AttributeSet attr) throws BadLocationException {
                replace(fb, offset, 0, texto, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String texto, AttributeSet attrs) throws BadLocationException {
                if (texto == null) return;
                String letras = texto.replaceAll("[^A-Za-zÁÉÍÓÚáéíóúÑñÜü\\s]", "");
                if (letras.isEmpty() && !texto.isEmpty()) return;
                super.replace(fb, offset, length, letras, attrs);
            }
        });
    }

    private void validarCliente(ActionEvent e) {
        String tipo = String.valueOf(cboTipoDocumento.getSelectedItem());
        String numero = txtNumeroDocumento.getText().trim();

        String validacionConsulta = ControlValidarClienteCINEX.validarDatosParaConsulta(tipo, numero);
        if (!validacionConsulta.isEmpty()) {
            datosValidados = false;
            clienteExistente = false;
            documentoValidado = "";
            mostrarMensaje("Complete la información obligatoria", ROJO);
            mostrarEstado("Corrija la información para continuar.");
            JOptionPane.showMessageDialog(this, "Complete la información obligatoria", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnValidar.setEnabled(false);
        btnValidar.setText("...");
        mostrarMensaje("Validando información...", AMARILLO);
        mostrarEstado("Espere mientras se verifica el cliente.");

        SwingWorker<ClienteCINEX, Void> worker = new SwingWorker<ClienteCINEX, Void>() {
            @Override
            protected ClienteCINEX doInBackground() throws Exception {
                return ControlValidarClienteCINEX.verificarCliente(numero);
            }

            @Override
            protected void done() {
                btnValidar.setEnabled(true);
                btnValidar.setText("VALIDAR");

                try {
                    ClienteCINEX existente = get();
                    documentoValidado = numero;

                    if (existente != null) {
                        datosValidados = true;
                        clienteExistente = true;
                        txtNombreCliente.setText(valorSeguro(existente.getNombre()));
                        mostrarMensaje("El cliente ya existe.", AMARILLO);
                        mostrarEstado("El cliente ya se encuentra registrado.");
                        JOptionPane.showMessageDialog(RegistroClienteCU4CINEXGUI.this, "El cliente ya existe.", "Cliente ya registrado", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        clienteExistente = false;
                        buscarNombreReniec(tipo, numero);
                    }
                } catch (Exception ex) {
                    datosValidados = false;
                    clienteExistente = false;
                    documentoValidado = "";
                    mostrarMensaje("No se pudo validar contra la base de datos.", ROJO);
                    mostrarEstado("Revise la conexión e inténtelo nuevamente.");
                    JOptionPane.showMessageDialog(RegistroClienteCU4CINEXGUI.this, "No se pudo verificar el cliente:\n" + obtenerMensajeError(ex), "Error de BD", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void buscarNombreReniec(String tipo, String numero) {
        txtNombreCliente.setText("");
        datosValidados = false;
        documentoValidado = "";

        btnValidar.setEnabled(false);
        btnValidar.setText("RENIEC...");
        mostrarMensaje(
                "Cliente no registrado. Consultando RENIEC/API...",
                AMARILLO
        );
        mostrarEstado(
                "Buscando el nombre del cliente mediante su documento."
        );

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return DocumentoAPI.consultarDocumento(
                        ControlValidarClienteCINEX
                                .normalizarTipoDocumentoAPI(tipo),
                        numero
                );
            }

            @Override
            protected void done() {
                btnValidar.setEnabled(true);
                btnValidar.setText("VALIDAR");

                try {
                    String nombre = get();

                    if (nombre == null || nombre.trim().isEmpty()) {
                        documentoNoEncontrado(tipo, numero);
                        return;
                    }

                    String nombreFormateado =
                            DocumentoAPI.formatearNombreCliente(
                                    nombre.trim()
                            );

                    if (nombreFormateado.isEmpty()) {
                        documentoNoEncontrado(tipo, numero);
                        return;
                    }

                    txtNombreCliente.setText(nombreFormateado);
                    datosValidados = true;
                    clienteExistente = false;
                    documentoValidado = numero;
                    mostrarMensaje(
                            "Información válida. Puede guardar el cliente.",
                            VERDE
                    );
                    mostrarEstado(
                            "Cliente encontrado en RENIEC/API. Puede registrarse."
                    );

                } catch (Exception ex) {
                    documentoNoEncontrado(tipo, numero);
                }
            }
        };

        worker.execute();
    }

    private void documentoNoEncontrado(String tipo, String numero) {
        txtNombreCliente.setText("");
        datosValidados = false;
        clienteExistente = false;
        documentoValidado = "";

        mostrarMensaje(
                "Documento no encontrado. No se permite escribir el nombre manualmente.",
                ROJO
        );
        mostrarEstado(
                "Verifique el documento y vuelva a realizar la consulta."
        );

        JOptionPane.showMessageDialog(
                this,
                "No se encontró el " + tipo + " " + numero
                        + " en RENIEC/API.\n"
                        + "El nombre del cliente no puede ingresarse manualmente.",
                "Documento no encontrado",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void guardarCliente(ActionEvent e) {
        String tipo = String.valueOf(cboTipoDocumento.getSelectedItem());
        String numero = txtNumeroDocumento.getText().trim();
        String nombre = txtNombreCliente.getText().trim();

        String validacion = ControlValidarClienteCINEX.validarInformacion(tipo, numero, nombre);
        if (!validacion.isEmpty()) {
            mostrarMensaje("Complete la información obligatoria", ROJO);
            mostrarEstado("Corrija la información para continuar.");
            JOptionPane.showMessageDialog(this, "Complete la información obligatoria", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!datosValidados || !numero.equals(documentoValidado)) {
            mostrarMensaje("Primero valide la información antes de guardar.", ROJO);
            mostrarEstado("Presione VALIDAR antes de guardar el cliente.");
            JOptionPane.showMessageDialog(this, "Primero valide la información antes de guardar.", "Validación requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (clienteExistente) {
            mostrarMensaje("El cliente ya existe.", AMARILLO);
            mostrarEstado("El cliente ya se encuentra registrado.");
            JOptionPane.showMessageDialog(this, "El cliente ya existe.", "Cliente ya registrado", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ClienteCINEX cliente = new ClienteCINEX();
        cliente.setTipoDocumento(tipo);
        cliente.setNumeroDocumento(numero);
        cliente.setNombre(formatearNombre(nombre));

        btnGuardar.setEnabled(false);
        btnGuardar.setText("GUARDANDO...");
        mostrarMensaje("Registrando cliente en la base de datos...", AMARILLO);
        mostrarEstado("Espere mientras se guarda el registro.");

        SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return ControlValidarClienteCINEX.registrarCliente(cliente);
            }

            @Override
            protected void done() {
                btnGuardar.setEnabled(true);
                btnGuardar.setText("GUARDAR CLIENTE");

                try {
                    int id = get();

                    if (id == -1) {
                        clienteExistente = true;
                        mostrarMensaje("El cliente ya existe.", AMARILLO);
                        mostrarEstado("El cliente ya se encuentra registrado.");
                        JOptionPane.showMessageDialog(RegistroClienteCU4CINEXGUI.this, "El cliente ya existe.", "Cliente ya registrado", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    if (id > 0) {
                        datosValidados = false;
                        clienteExistente = false;
                        documentoValidado = "";
                        mostrarMensaje("Cliente registrado correctamente.", VERDE);
                        mostrarEstado("Cliente registrado correctamente.");
                        JOptionPane.showMessageDialog(RegistroClienteCU4CINEXGUI.this, "Cliente registrado correctamente.", "Registro confirmado", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    mostrarMensaje("No se pudo registrar el cliente.", ROJO);
                    mostrarEstado("No se registró el cliente. Intente nuevamente.");
                } catch (Exception ex) {
                    mostrarMensaje("Error al registrar cliente.", ROJO);
                    mostrarEstado("Error al registrar cliente en la base de datos.");
                    JOptionPane.showMessageDialog(RegistroClienteCU4CINEXGUI.this, "No se pudo registrar el cliente:\n" + obtenerMensajeError(ex), "Error de registro", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void limpiarFormulario() {
        cboTipoDocumento.setSelectedIndex(0);
        txtNumeroDocumento.setText("");
        txtNombreCliente.setText("");
        reiniciarValidacion();
        mostrarMensaje("Ingrese los datos del cliente y presione VALIDAR.", GRIS);
        mostrarEstado("Complete los datos del cliente para iniciar el registro.");
        txtNumeroDocumento.requestFocus();
    }

    private void reiniciarValidacion() {
        datosValidados = false;
        clienteExistente = false;
        documentoValidado = "";
    }

    private String formatearNombre(String nombre) {
        nombre = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (nombre.isEmpty()) return nombre;
        StringBuilder sb = new StringBuilder();
        for (String p : nombre.toLowerCase().split(" ")) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private String valorSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private String obtenerMensajeError(Exception e) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() == null ? e.toString() : t.getMessage();
    }

    private void ajustarLongitudDocumentoActual() {
        String numero = txtNumeroDocumento.getText().replaceAll("\\D", "");
        int max = ControlValidarClienteCINEX.obtenerLongitudDocumento(String.valueOf(cboTipoDocumento.getSelectedItem()));
        if (numero.length() > max) numero = numero.substring(0, max);
        if (!numero.equals(txtNumeroDocumento.getText())) txtNumeroDocumento.setText(numero);
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
        txt.setBorder(BorderFactory.createCompoundBorder(new LineBorder(AZUL_BORDE, 1, true), new EmptyBorder(0, 12, 0, 12)));
        return txt;
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
                btn.setFont(new Font("Arial", Font.BOLD, 11));
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
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setOpaque(true);
                label.setFont(new Font("Arial", Font.BOLD, 15));
                label.setBorder(new EmptyBorder(0, 10, 0, 10));
                if (isSelected && index >= 0) {
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

    private JButton crearBotonPrincipal(String texto) {
        JButton btn = crearBotonBase(texto);
        aplicarColoresBoton(btn, AMARILLO, Color.BLACK, new Color(78, 70, 35), new Color(225, 225, 225));
        return btn;
    }

    private JButton crearBotonAzul(String texto) {
        JButton btn = crearBotonBase(texto);
        aplicarColoresBoton(btn, AZUL_PANEL, BLANCO, new Color(45, 58, 82), new Color(190, 200, 215));
        btn.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        btn.setBorderPainted(true);
        return btn;
    }

    private JButton crearBotonSecundario(String texto) {
        JButton btn = crearBotonBase(texto);
        aplicarColoresBoton(btn, AZUL_CARD, BLANCO, new Color(45, 58, 82), new Color(190, 200, 215));
        btn.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        btn.setBorderPainted(true);
        return btn;
    }

    private JButton crearBotonBase(String texto) {
        JButton btn = new JButton(texto);
        btn.setUI(new BasicButtonUI());
        btn.setFont(new Font("Arial", Font.BOLD, 15));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void aplicarColoresBoton(JButton btn, Color fondoActivo, Color textoActivo, Color fondoInactivo, Color textoInactivo) {
        btn.putClientProperty("fondoActivo", fondoActivo);
        btn.putClientProperty("textoActivo", textoActivo);
        btn.putClientProperty("fondoInactivo", fondoInactivo);
        btn.putClientProperty("textoInactivo", textoInactivo);
        actualizarColoresBoton(btn);
        btn.addPropertyChangeListener("enabled", e -> actualizarColoresBoton(btn));
    }

    private void actualizarColoresBoton(JButton btn) {
        Color fondo = (Color) btn.getClientProperty(btn.isEnabled() ? "fondoActivo" : "fondoInactivo");
        Color texto = (Color) btn.getClientProperty(btn.isEnabled() ? "textoActivo" : "textoInactivo");
        if (fondo != null) btn.setBackground(fondo);
        if (texto != null) btn.setForeground(texto);
        btn.setCursor(new Cursor(btn.isEnabled() ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void mostrarMensaje(String mensaje, Color color) {
        if (lblMensaje != null) {
            lblMensaje.setText(mensaje);
            lblMensaje.setForeground(color);
        }
    }

    private void mostrarEstado(String mensaje) {
        if (lblEstadoFlujo != null) {
            lblEstadoFlujo.setText(mensaje);
        }
    }

    private void volverMenu() {
        dispose();
        new MenuTaquilleroCINEXGUI(usuarioActual).setVisible(true);
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

    private interface LongitudMaxima {
        int obtener();
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
            g2.setColor(color);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, radio, radio));
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegistroClienteCU4CINEXGUI("taquillero").setVisible(true));
    }
}
