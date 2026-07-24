package interfaz;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
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
import java.util.ArrayList;

import control.ControlBuscarClienteCINEX;
import control.ControlConsultarHistorialCINEX;
import entidad.ClienteCINEX;
import entidad.EntradaCINEX;


public class HistorialClientesCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_PANEL = new Color(8, 24, 55);
    private final Color AZUL_PANEL_2 = new Color(5, 18, 43);
    private final Color AZUL_BOTON = new Color(0, 80, 160);
    private final Color AZUL_DESHABILITADO = new Color(39, 51, 78);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS = new Color(185, 190, 200);
    private final Color VERDE = new Color(35, 180, 85);
    private final Color ROJO = new Color(210, 65, 65);

    private JLabel lblHora;
    private JLabel lblFecha;

    private JTextField txtBusqueda;
    private JComboBox<String> cboTipoBusqueda;
    private JButton btnBuscar;
    private JButton btnLimpiar;
    private JButton btnVerHistorial;
    private JButton btnAtras;

    private JTable tablaClientes;
    private JTable tablaHistorial;
    private DefaultTableModel modeloClientes;
    private DefaultTableModel modeloHistorial;

    private JLabel lblEstadoBusqueda;
    private JLabel lblNombreCliente;
    private JLabel lblDocumentoCliente;
    private JLabel lblTotalCompras;
    private JLabel lblTotalGastado;
    private JLabel lblUltimaCompra;

    private String usuarioActual;
    private ClienteCINEX clienteSeleccionado;

    public HistorialClientesCINEXGUI() {
        this("taquillero");
    }

    public HistorialClientesCINEXGUI(String usuario) {
        this.usuarioActual = usuario == null || usuario.trim().isEmpty() ? "taquillero" : usuario.trim();

        setTitle("CINEX - Historial de compras");
        setSize(1366, 768);
        setMinimumSize(new Dimension(1280, 720));
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
                g2.fillOval(getWidth() - 450, 240, 380, 380);

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

        SwingUtilities.invokeLater(() -> txtBusqueda.requestFocus());
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(8, 25, 4, 25));

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
        contenido.setBorder(new EmptyBorder(8, 25, 8, 25));

        JPanel main = new JPanel(new BorderLayout(24, 0));
        main.setOpaque(false);

        main.add(crearPanelBusqueda(), BorderLayout.WEST);
        main.add(crearPanelHistorial(), BorderLayout.CENTER);

        contenido.add(main, BorderLayout.CENTER);

        return contenido;
    }

    private JPanel crearPanelBusqueda() {
        RoundedPanel panel = new RoundedPanel(18, new Color(5, 18, 43, 190));
        panel.setLayout(null);
        panel.setPreferredSize(new Dimension(355, 0));
        panel.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));

        JLabel titulo = new JLabel("Buscar clientes");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 26));
        titulo.setBounds(25, 28, 290, 35);
        panel.add(titulo);

        JLabel subtitulo = new JLabel("Busque por DNI, C.E. o nombre del cliente");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitulo.setBounds(25, 63, 310, 24);
        panel.add(subtitulo);

        JLabel lblTipo = new JLabel("Tipo de búsqueda");
        lblTipo.setForeground(BLANCO);
        lblTipo.setFont(new Font("Arial", Font.BOLD, 15));
        lblTipo.setBounds(25, 112, 230, 22);
        panel.add(lblTipo);

        cboTipoBusqueda = new JComboBox<>(new String[]{"DNI", "C.E.", "Nombre"});
        cboTipoBusqueda.setBounds(25, 140, 305, 44);
        configurarComboBusqueda(cboTipoBusqueda);
        panel.add(cboTipoBusqueda);

        JLabel lblDato = new JLabel("Dato del cliente");
        lblDato.setForeground(BLANCO);
        lblDato.setFont(new Font("Arial", Font.BOLD, 15));
        lblDato.setBounds(25, 198, 230, 22);
        panel.add(lblDato);

        txtBusqueda = new JTextField();
        txtBusqueda.setBounds(25, 226, 305, 44);
        txtBusqueda.setFont(new Font("Arial", Font.BOLD, 15));
        txtBusqueda.setBackground(Color.WHITE);
        txtBusqueda.setForeground(Color.BLACK);
        txtBusqueda.setCaretColor(Color.BLACK);
        txtBusqueda.setBorder(new EmptyBorder(0, 12, 0, 12));
        panel.add(txtBusqueda);

        aplicarFiltroBusqueda();

        btnBuscar = crearBotonPrincipal("BUSCAR CLIENTE");
        btnBuscar.setBounds(25, 292, 305, 50);
        panel.add(btnBuscar);

        btnVerHistorial = crearBotonAzul("VER HISTORIAL");
        btnVerHistorial.setBounds(25, 352, 305, 50);
        actualizarBotonVerHistorial(false);
        panel.add(btnVerHistorial);

        btnLimpiar = crearBotonSecundario("LIMPIAR");
        btnLimpiar.setBounds(25, 412, 305, 50);
        panel.add(btnLimpiar);

        JLabel lblEncontrados = new JLabel("Clientes encontrados");
        lblEncontrados.setForeground(BLANCO);
        lblEncontrados.setFont(new Font("Arial", Font.BOLD, 16));
        lblEncontrados.setBounds(25, 490, 250, 25);
        panel.add(lblEncontrados);

        modeloClientes = new DefaultTableModel(new Object[]{"Documento", "Cliente"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaClientes = new JTable(modeloClientes);
        configurarTabla(tablaClientes, false);
        tablaClientes.setRowHeight(34);
        tablaClientes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollClientes = new JScrollPane(tablaClientes);
        scrollClientes.setBounds(25, 520, 305, 120);
        scrollClientes.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));
        scrollClientes.getViewport().setBackground(AZUL_PANEL);
        panel.add(scrollClientes);

        lblEstadoBusqueda = new JLabel("Ingrese un dato para consultar.", SwingConstants.LEFT);
        lblEstadoBusqueda.setForeground(GRIS);
        lblEstadoBusqueda.setFont(new Font("Arial", Font.BOLD, 13));
        lblEstadoBusqueda.setBounds(25, 660, 305, 25);
        panel.add(lblEstadoBusqueda);

        btnBuscar.addActionListener(e -> buscarCliente());
        btnVerHistorial.addActionListener(e -> cargarHistorialClienteSeleccionado());
        btnLimpiar.addActionListener(e -> limpiarPantalla());

        txtBusqueda.addActionListener(e -> buscarCliente());

        cboTipoBusqueda.addActionListener(e -> {
            txtBusqueda.setText("");
            aplicarFiltroBusqueda();
            mostrarEstado("Ingrese un dato para consultar.", GRIS);
            txtBusqueda.requestFocus();
        });

        tablaClientes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                seleccionarClienteDesdeTabla();

                if (e.getClickCount() == 2) {
                    cargarHistorialClienteSeleccionado();
                }
            }
        });

        tablaClientes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                seleccionarClienteDesdeTabla();
            }
        });

        return panel;
    }

    private JPanel crearPanelHistorial() {
        RoundedPanel panel = new RoundedPanel(18, new Color(5, 18, 43, 150));
        panel.setLayout(new BorderLayout(0, 14));
        panel.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(24, 28, 0, 28));

        JPanel textoTitulo = new JPanel();
        textoTitulo.setOpaque(false);
        textoTitulo.setLayout(new BoxLayout(textoTitulo, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Listar compras del cliente");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 30));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitulo = new JLabel("Visualice las compras realizadas por el cliente seleccionado.");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 15));
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        textoTitulo.add(titulo);
        textoTitulo.add(Box.createVerticalStrut(4));
        textoTitulo.add(subtitulo);

        top.add(textoTitulo, BorderLayout.WEST);
        top.add(crearPanelMetricas(), BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);

        modeloHistorial = new DefaultTableModel(
                new Object[]{
                        "N° Venta",
                        "Fecha compra",
                        "Película",
                        "Función",
                        "Sala",
                        "Asientos vendidos",
                        "Entradas",
                        "Método",
                        "Total bruto",
                        "Estado venta",
                        "Estado reembolso",
                        "Entradas reemb.",
                        "Asientos reemb.",
                        "Monto reemb.",
                        "Total neto"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 6 || columnIndex == 11) {
                    return Integer.class;
                }

                if (columnIndex == 8
                        || columnIndex == 13
                        || columnIndex == 14) {
                    return Double.class;
                }

                return String.class;
            }
        };

        tablaHistorial = new JTable(modeloHistorial);
        configurarTabla(tablaHistorial, true);
        tablaHistorial.setRowHeight(42);
        tablaHistorial.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        ajustarColumnasHistorial();

        JScrollPane scroll = new JScrollPane(tablaHistorial);
        scroll.setBorder(new EmptyBorder(0, 28, 0, 28));
        scroll.getViewport().setBackground(new Color(6, 18, 43));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(true);

        panel.add(scroll, BorderLayout.CENTER);

        JPanel detalle = crearPanelDetalle();
        panel.add(detalle, BorderLayout.SOUTH);

        tablaHistorial.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarDetalleCompraSeleccionada();
            }
        });

        return panel;
    }

    private JPanel crearPanelMetricas() {
        JPanel metricas = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        metricas.setOpaque(false);

        lblTotalCompras = crearValorMetrica("0");
        lblTotalGastado = crearValorMetrica("S/ 0.00");
        lblUltimaCompra = crearValorMetrica("-");

        metricas.add(crearCardMetrica("Compras", lblTotalCompras));
        metricas.add(crearCardMetrica("Total gastado", lblTotalGastado));
        metricas.add(crearCardMetrica("Última compra", lblUltimaCompra));

        return metricas;
    }

    private JPanel crearCardMetrica(String titulo, JLabel valor) {
        RoundedPanel card = new RoundedPanel(14, new Color(8, 24, 55, 220));
        card.setLayout(null);
        card.setPreferredSize(new Dimension(142, 70));
        card.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setForeground(GRIS);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 12));
        lblTitulo.setBounds(12, 10, 120, 18);
        card.add(lblTitulo);

        valor.setBounds(12, 32, 125, 24);
        card.add(valor);

        return card;
    }

    private JLabel crearValorMetrica(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(AMARILLO);
        lbl.setFont(new Font("Arial", Font.BOLD, 18));
        return lbl;
    }

    private JPanel crearPanelDetalle() {
        RoundedPanel panel = new RoundedPanel(16, new Color(8, 24, 55, 190));
        panel.setLayout(null);
        panel.setPreferredSize(new Dimension(0, 105));
        panel.setBorder(new EmptyBorder(0, 28, 20, 28));

        lblNombreCliente = new JLabel("Cliente: -");
        lblNombreCliente.setForeground(BLANCO);
        lblNombreCliente.setFont(new Font("Arial", Font.BOLD, 18));
        lblNombreCliente.setBounds(28, 14, 520, 28);
        panel.add(lblNombreCliente);

        lblDocumentoCliente = new JLabel("Documento: -");
        lblDocumentoCliente.setForeground(GRIS);
        lblDocumentoCliente.setFont(new Font("Arial", Font.BOLD, 15));
        lblDocumentoCliente.setBounds(28, 48, 520, 24);
        panel.add(lblDocumentoCliente);

        JLabel ayuda = new JLabel("Seleccione una compra de la tabla para revisar el detalle.");
        ayuda.setForeground(GRIS);
        ayuda.setFont(new Font("Arial", Font.PLAIN, 14));
        ayuda.setBounds(610, 20, 520, 24);
        panel.add(ayuda);

        JLabel estado = new JLabel("Consulta lista");
        estado.setForeground(VERDE);
        estado.setFont(new Font("Arial", Font.BOLD, 15));
        estado.setBounds(610, 52, 520, 24);
        panel.add(estado);

        return panel;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(8, 25, 20, 25));

        btnAtras = crearBotonSecundario("ATRÁS");
        btnAtras.setPreferredSize(new Dimension(190, 58));
        btnAtras.addActionListener(e -> {
            dispose();
            new MenuTaquilleroCINEXGUI(usuarioActual).setVisible(true);
        });

        footer.add(btnAtras, BorderLayout.WEST);

        return footer;
    }

    private void buscarCliente() {
        String busqueda = txtBusqueda.getText().trim();
        String tipo = String.valueOf(cboTipoBusqueda.getSelectedItem());

        String validacion = validarBusqueda(tipo, busqueda);

        if (!validacion.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    validacion,
                    "Dato inválido",
                    JOptionPane.WARNING_MESSAGE
            );
            mostrarEstado(validacion, ROJO);
            txtBusqueda.requestFocus();
            return;
        }

        btnBuscar.setEnabled(false);
        btnBuscar.setText("BUSCANDO...");
        actualizarBotonVerHistorial(false);

        modeloClientes.setRowCount(0);
        modeloHistorial.setRowCount(0);
        limpiarResumenCliente();

        mostrarEstado("Buscando información registrada...", AMARILLO);

        SwingWorker<ArrayList<ClienteCINEX>, Void> worker = new SwingWorker<ArrayList<ClienteCINEX>, Void>() {
            @Override
            protected ArrayList<ClienteCINEX> doInBackground() {
                return ControlBuscarClienteCINEX.buscarCliente(tipo, busqueda);
            }

            @Override
            protected void done() {
                btnBuscar.setEnabled(true);
                btnBuscar.setText("BUSCAR CLIENTE");

                try {
                    ArrayList<ClienteCINEX> clientes = get();

                    if (clientes.isEmpty()) {
                        clienteSeleccionado = null;
                        modeloHistorial.setRowCount(0);
                        mostrarEstado("Cliente no registrado.", ROJO);

                        JOptionPane.showMessageDialog(
                                HistorialClientesCINEXGUI.this,
                                "Cliente no registrado.",
                                "Cliente no encontrado",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }

                    for (ClienteCINEX c : clientes) {
                        modeloClientes.addRow(new Object[]{c.getNumeroDocumento(), c.getNombre()});
                    }

                    tablaClientes.setRowSelectionInterval(0, 0);
                    seleccionarClienteDesdeTabla();

                    if (clientes.size() == 1) {
                        cargarHistorialClienteSeleccionado();
                    } else {
                        mostrarEstado("Seleccione el cliente y presione VER HISTORIAL.", VERDE);
                    }

                } catch (Exception e) {
                    mostrarEstado("Error al consultar clientes.", ROJO);

                    JOptionPane.showMessageDialog(
                            HistorialClientesCINEXGUI.this,
                            "No se pudo consultar la información del cliente.",
                            "Error de consulta",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    private String validarBusqueda(String tipo, String busqueda) {
        if (busqueda == null || busqueda.trim().isEmpty()) {
            return "Ingrese un dato para consultar.";
        }

        busqueda = busqueda.trim();

        if ("DNI".equalsIgnoreCase(tipo)) {
            if (!busqueda.matches("\\d{8}")) {
                return "El DNI debe tener exactamente 8 dígitos.";
            }
        }

        if ("C.E.".equalsIgnoreCase(tipo) || "CE".equalsIgnoreCase(tipo)) {
            if (!busqueda.matches("\\d{9}")) {
                return "El C.E. debe tener exactamente 9 dígitos.";
            }
        }

        if ("Nombre".equalsIgnoreCase(tipo)) {
            if (!busqueda.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+")) {
                return "El nombre solo debe contener letras.";
            }

            if (busqueda.trim().length() < 2) {
                return "Ingrese al menos 2 letras del nombre.";
            }
        }

        return "";
    }

    private void seleccionarClienteDesdeTabla() {
        int fila = tablaClientes.getSelectedRow();

        if (fila < 0) {
            clienteSeleccionado = null;
            actualizarBotonVerHistorial(false);
            return;
        }

        String dni = String.valueOf(modeloClientes.getValueAt(fila, 0));
        String nombre = String.valueOf(modeloClientes.getValueAt(fila, 1));

        clienteSeleccionado = ControlBuscarClienteCINEX.consultarCliente(dni);

        if (clienteSeleccionado == null) {
            clienteSeleccionado = new ClienteCINEX(-1, dni, nombre);
        }

        lblNombreCliente.setText("Cliente: " + clienteSeleccionado.getNombre());
        lblDocumentoCliente.setText("Documento: " + clienteSeleccionado.getNumeroDocumento());

        actualizarBotonVerHistorial(clienteSeleccionado.getIdCliente() > 0);
    }

    private void cargarHistorialClienteSeleccionado() {
        if (clienteSeleccionado == null || clienteSeleccionado.getIdCliente() <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Seleccione un cliente registrado.",
                    "Cliente requerido",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        actualizarBotonVerHistorial(false);
        btnBuscar.setEnabled(false);
        btnVerHistorial.setText("CARGANDO...");

        mostrarEstado("Consultando historial de compras...", AMARILLO);

        SwingWorker<ArrayList<EntradaCINEX>, Void> worker = new SwingWorker<ArrayList<EntradaCINEX>, Void>() {
            @Override
            protected ArrayList<EntradaCINEX> doInBackground() {
                try {
                    return ControlConsultarHistorialCINEX.consultarHistorialCompras(clienteSeleccionado.getIdCliente());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void done() {
                btnBuscar.setEnabled(true);
                btnVerHistorial.setText("VER HISTORIAL");
                actualizarBotonVerHistorial(clienteSeleccionado != null && clienteSeleccionado.getIdCliente() > 0);

                try {
                    ArrayList<EntradaCINEX> compras = get();
                    mostrarHistorial(compras);

                    if (compras.isEmpty()) {
                        mostrarEstado("Cliente registrado, pero sin compras realizadas.", ROJO);
                    } else {
                        mostrarEstado("Historial de compras cargado correctamente.", VERDE);
                    }

                } catch (Exception e) {
                    mostrarEstado("Error al cargar historial.", ROJO);

                    JOptionPane.showMessageDialog(
                            HistorialClientesCINEXGUI.this,
                            "No se pudo cargar el historial de compras.",
                            "Error de historial",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    private void mostrarHistorial(ArrayList<EntradaCINEX> compras) {
        modeloHistorial.setRowCount(0);

        double totalGastado = 0;
        String ultimaCompra = "-";

        for (int i = 0; i < compras.size(); i++) {
            EntradaCINEX c = compras.get(i);
            totalGastado += c.getTotalNeto();

            if (i == 0) {
                ultimaCompra = c.getFechaCompraCorta();
            }

            modeloHistorial.addRow(new Object[]{
                    c.getNumeroVenta(),
                    c.getFechaCompra(),
                    c.getPelicula(),
                    c.getFuncion(),
                    c.getSala(),
                    c.getAsientos(),
                    c.getCantidadEntradas(),
                    c.getMetodoPago(),
                    c.getTotal(),
                    c.getEstado(),
                    c.getEstadoReembolso(),
                    c.getEntradasReembolsadas(),
                    c.getAsientosReembolsados(),
                    c.getMontoReembolsado(),
                    c.getTotalNeto()
            });
        }

        lblTotalCompras.setText(String.valueOf(compras.size()));
        lblTotalGastado.setText("S/ " + String.format("%.2f", totalGastado));
        lblUltimaCompra.setText(ultimaCompra);

        if (!compras.isEmpty()) {
            tablaHistorial.setRowSelectionInterval(0, 0);
        }
    }

    private void actualizarDetalleCompraSeleccionada() {
        int fila = tablaHistorial.getSelectedRow();

        if (fila < 0) {
            return;
        }

        int modelo = tablaHistorial.convertRowIndexToModel(fila);

        String venta = String.valueOf(modeloHistorial.getValueAt(modelo, 0));
        String pelicula = String.valueOf(modeloHistorial.getValueAt(modelo, 2));
        String asientos = String.valueOf(modeloHistorial.getValueAt(modelo, 5));
        String total = String.valueOf(modeloHistorial.getValueAt(modelo, 14));

        lblNombreCliente.setText("Compra: " + venta + " | " + pelicula);
        lblDocumentoCliente.setText("Asientos: " + asientos + " | Total: S/ " + total);
    }

    private void limpiarPantalla() {
        txtBusqueda.setText("");
        modeloClientes.setRowCount(0);
        modeloHistorial.setRowCount(0);
        clienteSeleccionado = null;

        actualizarBotonVerHistorial(false);
        limpiarResumenCliente();

        mostrarEstado("Ingrese un dato para consultar.", GRIS);
        txtBusqueda.requestFocus();
    }

    private void limpiarResumenCliente() {
        lblNombreCliente.setText("Cliente: -");
        lblDocumentoCliente.setText("Documento: -");
        lblTotalCompras.setText("0");
        lblTotalGastado.setText("S/ 0.00");
        lblUltimaCompra.setText("-");
    }

    private void mostrarEstado(String texto, Color color) {
        lblEstadoBusqueda.setText(texto);
        lblEstadoBusqueda.setForeground(color);
    }

    private void aplicarFiltroBusqueda() {
        if (txtBusqueda == null || cboTipoBusqueda == null) {
            return;
        }

        AbstractDocument documento = (AbstractDocument) txtBusqueda.getDocument();

        documento.setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String texto, AttributeSet attr)
                    throws BadLocationException {
                replace(fb, offset, 0, texto, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String texto, AttributeSet attrs)
                    throws BadLocationException {

                if (texto == null) {
                    return;
                }

                String tipo = String.valueOf(cboTipoBusqueda.getSelectedItem());

                if ("DNI".equalsIgnoreCase(tipo)) {
                    insertarSoloNumeros(fb, offset, length, texto, attrs, 8);
                    return;
                }

                if ("C.E.".equalsIgnoreCase(tipo) || "CE".equalsIgnoreCase(tipo)) {
                    insertarSoloNumeros(fb, offset, length, texto, attrs, 9);
                    return;
                }

                if ("Nombre".equalsIgnoreCase(tipo)) {
                    insertarSoloLetras(fb, offset, length, texto, attrs);
                }
            }
        });
    }

    private void insertarSoloNumeros(
            DocumentFilter.FilterBypass fb,
            int offset,
            int length,
            String texto,
            AttributeSet attrs,
            int maximo
    ) throws BadLocationException {

        String soloNumeros = texto.replaceAll("\\D", "");

        if (soloNumeros.isEmpty() && !texto.isEmpty()) {
            return;
        }

        String actual = fb.getDocument().getText(0, fb.getDocument().getLength());
        int longitudActualSinReemplazo = actual.length() - length;
        int espacioDisponible = maximo - longitudActualSinReemplazo;

        if (espacioDisponible <= 0 && !soloNumeros.isEmpty()) {
            return;
        }

        if (soloNumeros.length() > espacioDisponible) {
            soloNumeros = soloNumeros.substring(0, espacioDisponible);
        }

        fb.replace(offset, length, soloNumeros, attrs);
    }

    private void insertarSoloLetras(
            DocumentFilter.FilterBypass fb,
            int offset,
            int length,
            String texto,
            AttributeSet attrs
    ) throws BadLocationException {

        String soloLetras = texto.replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]", "");

        if (soloLetras.isEmpty() && !texto.isEmpty()) {
            return;
        }

        fb.replace(offset, length, soloLetras, attrs);
    }

    private void configurarComboBusqueda(JComboBox<String> combo) {
        combo.setFont(new Font("Arial", Font.BOLD, 14));
        combo.setBackground(Color.WHITE);
        combo.setForeground(Color.BLACK);
        combo.setFocusable(false);
        combo.setOpaque(true);

        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton("▼");
                btn.setBorder(null);
                btn.setFocusPainted(false);
                btn.setContentAreaFilled(true);
                btn.setOpaque(true);
                btn.setBackground(Color.WHITE);
                btn.setForeground(Color.BLACK);
                btn.setFont(new Font("Arial", Font.BOLD, 10));
                return btn;
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
                label.setFont(new Font("Arial", Font.BOLD, 14));
                label.setBorder(new EmptyBorder(0, 10, 0, 10));

                if (isSelected) {
                    label.setBackground(AMARILLO);
                    label.setForeground(Color.BLACK);
                } else {
                    label.setBackground(Color.WHITE);
                    label.setForeground(Color.BLACK);
                }

                return label;
            }
        });
    }

    private JButton crearBotonPrincipal(String texto) {
        JButton btn = new JButton(texto);
        estabilizarBoton(btn);
        btn.setBackground(AMARILLO);
        btn.setForeground(new Color(5, 20, 55));
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        return btn;
    }

    private JButton crearBotonAzul(String texto) {
        JButton btn = new JButton(texto);
        estabilizarBoton(btn);
        btn.setBackground(AZUL_BOTON);
        btn.setForeground(BLANCO);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        return btn;
    }

    private JButton crearBotonSecundario(String texto) {
        JButton btn = new JButton(texto);
        estabilizarBoton(btn);
        btn.setBackground(AZUL_PANEL_2);
        btn.setForeground(BLANCO);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));
        btn.setBorderPainted(true);
        return btn;
    }

    private void estabilizarBoton(JButton btn) {
        btn.setUI(new BasicButtonUI());
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void actualizarBotonVerHistorial(boolean habilitado) {
        btnVerHistorial.setEnabled(habilitado);

        if (habilitado) {
            btnVerHistorial.setBackground(AZUL_BOTON);
            btnVerHistorial.setForeground(BLANCO);
            btnVerHistorial.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            btnVerHistorial.setBackground(AZUL_DESHABILITADO);
            btnVerHistorial.setForeground(new Color(170, 180, 200));
            btnVerHistorial.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void configurarTabla(JTable tabla, boolean historial) {
        tabla.setBackground(new Color(6, 18, 43));
        tabla.setForeground(BLANCO);
        tabla.setGridColor(new Color(50, 70, 105));
        tabla.setSelectionBackground(AMARILLO);
        tabla.setSelectionForeground(new Color(5, 20, 55));
        tabla.setFont(new Font("Arial", Font.BOLD, historial ? 13 : 12));
        tabla.setShowVerticalLines(true);
        tabla.setShowHorizontalLines(true);
        tabla.setFocusable(false);
        tabla.setOpaque(true);

        JTableHeader header = tabla.getTableHeader();
        header.setOpaque(true);
        header.setBackground(AZUL_PANEL_2);
        header.setForeground(BLANCO);
        header.setFont(new Font("Arial", Font.BOLD, 13));
        header.setBorder(new LineBorder(new Color(80, 105, 145)));
        header.setReorderingAllowed(false);

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, false, false, row, column
                );

                label.setOpaque(true);
                label.setBackground(AZUL_PANEL_2);
                label.setForeground(BLANCO);
                label.setFont(new Font("Arial", Font.BOLD, historial ? 13 : 12));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(new LineBorder(new Color(80, 105, 145), 1));

                return label;
            }
        });

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column
                );

                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(new Font("Arial", Font.BOLD, historial ? 13 : 12));

                if (isSelected) {
                    label.setBackground(AMARILLO);
                    label.setForeground(new Color(5, 20, 55));
                } else {
                    label.setBackground(new Color(6, 18, 43));
                    label.setForeground(BLANCO);
                }

                return label;
            }
        };

        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private void ajustarColumnasHistorial() {
        int[] anchos = {
                145, 145, 190, 150, 105,
                160, 80, 105, 95, 105,
                135, 95, 150, 105, 95
        };

        for (int i = 0; i < anchos.length; i++) {
            tablaHistorial.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
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
                File alternativo = new File("Imagenes/" + new File(nombre).getName());

                if (alternativo.exists()) {
                    archivo = alternativo;
                }
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
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HistorialClientesCINEXGUI("taquillero").setVisible(true));
    }
}