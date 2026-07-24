package interfaz;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.imageio.ImageIO;
import javax.swing.table.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Locale;
import control.ControlConsultarHistorialVentasCINEX;
import entidad.SalaCINEX;
import entidad.VentaCINEX;


public class VentasGerenteCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(1, 15, 42);
    private final Color AZUL_FONDO_2 = new Color(4, 35, 78);
    private final Color AZUL_SIDEBAR = new Color(3, 18, 45);
    private final Color AZUL_PANEL = new Color(5, 27, 62);
    private final Color AZUL_BORDE = new Color(50, 82, 125);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = Color.WHITE;
    private final Color GRIS = new Color(185, 198, 215);
    private final Color ROJO = new Color(220, 70, 70);
    private final Color VERDE = new Color(35, 180, 85);

    private JLabel lblHora;
    private JLabel lblFecha;
    private JLabel lblEstadoConsulta;
    private JLabel lblMonto;
    private JLabel lblCantidad;

    private JTextField txtInicio;
    private JTextField txtFin;
    private JButton btnCalendarioInicio;
    private JButton btnCalendarioFin;
    private JComboBox<String> cbSala;
    private JComboBox<String> cbMetodo;
    private JButton btnBuscar;

    private JTable tabla;
    private DefaultTableModel modelo;

    private String usuarioActual;
    private final ControlConsultarHistorialVentasCINEX controlHistorialVentas = new ControlConsultarHistorialVentasCINEX();

    public VentasGerenteCINEXGUI(String usuario) {
        this.usuarioActual = usuario == null || usuario.trim().isEmpty() ? "gerente" : usuario.trim();

        setTitle("CINEX - Visualizar historial de ventas");
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel root = new FondoPanel();
        root.setLayout(null);
        setContentPane(root);

        Rectangle pantalla = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();

        int anchoPantalla = pantalla.width;

        int topX = anchoPantalla - 575;

        JButton btnMenuGerente = new JButton("MENÚ GERENTE");
        btnMenuGerente.setBounds(40, 820, 180, 44);
        btnMenuGerente.setBackground(new Color(0, 80, 160));
        btnMenuGerente.setForeground(BLANCO);
        btnMenuGerente.setFont(new Font("Arial", Font.BOLD, 13));
        btnMenuGerente.setFocusPainted(false);
        btnMenuGerente.setBorderPainted(false);
        btnMenuGerente.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMenuGerente.addActionListener(e -> abrirMenuPrincipal());
        root.add(btnMenuGerente);

        JLabel lblUsuario = crearTextoSuperior("Usuario: " + usuarioActual);
        lblUsuario.setBounds(topX, 18, 210, 25);
        root.add(lblUsuario);

        JLabel lblTerminal = crearTextoSuperior("Terminal: 01");
        lblTerminal.setBounds(topX + 190, 18, 140, 25);
        root.add(lblTerminal);

        lblHora = crearTextoSuperior("");
        lblHora.setBounds(topX + 325, 18, 120, 25);
        root.add(lblHora);

        lblFecha = crearTextoSuperior("");
        lblFecha.setBounds(topX + 455, 18, 130, 25);
        root.add(lblFecha);

        actualizarFechaHora();
        Timer timer = new Timer(1000, e -> actualizarFechaHora());
        timer.start();

        JLabel lblTitulo = new JLabel("Visualizar historial de ventas");
        lblTitulo.setForeground(BLANCO);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 34));
        lblTitulo.setBounds(40, 55, 650, 44);
        root.add(lblTitulo);

        JLabel lblSubtitulo = new JLabel("Consulte las ventas realizadas en periodos anteriores");
        lblSubtitulo.setForeground(GRIS);
        lblSubtitulo.setFont(new Font("Arial", Font.PLAIN, 17));
        lblSubtitulo.setBounds(42, 102, 720, 25);
        root.add(lblSubtitulo);

        JLabel lblFechaInicio = crearLabelFiltro("Fecha inicio:");
        lblFechaInicio.setBounds(40, 140, 120, 22);
        root.add(lblFechaInicio);

        txtInicio = crearCampoFiltro(LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        txtInicio.setBounds(40, 165, 125, 38);
        root.add(txtInicio);

        btnCalendarioInicio = crearBotonCalendario();
        btnCalendarioInicio.setBounds(168, 165, 38, 38);
        btnCalendarioInicio.addActionListener(e -> mostrarCalendario(txtInicio));
        root.add(btnCalendarioInicio);

        JLabel lblFechaFin = crearLabelFiltro("Fecha fin:");
        lblFechaFin.setBounds(220, 140, 120, 22);
        root.add(lblFechaFin);

        txtFin = crearCampoFiltro(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        txtFin.setBounds(220, 165, 125, 38);
        root.add(txtFin);

        btnCalendarioFin = crearBotonCalendario();
        btnCalendarioFin.setBounds(348, 165, 38, 38);
        btnCalendarioFin.addActionListener(e -> mostrarCalendario(txtFin));
        root.add(btnCalendarioFin);

        JLabel lblSala = crearLabelFiltro("Sala:");
        lblSala.setBounds(400, 140, 120, 22);
        root.add(lblSala);

        cbSala = crearCombo(new String[]{"Todas"});
        cbSala.setBounds(400, 165, 140, 38);
        root.add(cbSala);

        JLabel lblMetodo = crearLabelFiltro("Método de pago:");
        lblMetodo.setBounds(565, 140, 140, 22);
        root.add(lblMetodo);

        cbMetodo = crearCombo(new String[]{
                "Todos",
                "Efectivo",
                "Tarjeta Crédito",
                "Tarjeta Débito",
                "Yape",
                "Plin"
        });
        cbMetodo.setBounds(565, 165, 220, 38);
        root.add(cbMetodo);

        btnBuscar = new JButton("BUSCAR");
        btnBuscar.setBounds(815, 165, 130, 38);
        btnBuscar.setBackground(AMARILLO);
        btnBuscar.setForeground(AZUL_FONDO_1);
        btnBuscar.setFont(new Font("Arial", Font.BOLD, 14));
        btnBuscar.setFocusPainted(false);
        btnBuscar.setBorderPainted(false);
        btnBuscar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        root.add(btnBuscar);

        lblEstadoConsulta = new JLabel("Mostrando filtros de búsqueda.");
        lblEstadoConsulta.setForeground(GRIS);
        lblEstadoConsulta.setFont(new Font("Arial", Font.BOLD, 13));
        lblEstadoConsulta.setVerticalAlignment(SwingConstants.CENTER);
        lblEstadoConsulta.setBounds(40, 207, 800, 18);
        root.add(lblEstadoConsulta);

        JPanel tablaPanel = new PanelRedondeado();
        tablaPanel.setLayout(new BorderLayout());
        tablaPanel.setBounds(40, 235, 1195, 375);
        tablaPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.add(tablaPanel);

        String[] columnas = {
                "N° Venta",
                "Fecha",
                "Hora",
                "Película",
                "Sala",
                "Asientos vendidos",
                "Total bruto",
                "Método de pago",
                "Vendedor",
                "Estado reembolso",
                "Entradas reemb.",
                "Asientos reemb.",
                "Monto reemb.",
                "Total neto"
        };

        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 6
                        || columnIndex == 12
                        || columnIndex == 13) {
                    return Double.class;
                }

                if (columnIndex == 10) {
                    return Integer.class;
                }

                return String.class;
            }
        };

        tabla = new JTable(modelo);
        configurarTabla();

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(null);
        scroll.setBackground(AZUL_PANEL);
        scroll.setOpaque(true);

        scroll.getViewport().setBackground(AZUL_PANEL);
        scroll.getViewport().setOpaque(true);

        // Evita NullPointerException: en algunas versiones de Swing el encabezado
        // del JScrollPane aún no existe hasta asignarlo explícitamente.
        scroll.setColumnHeaderView(tabla.getTableHeader());
        if (scroll.getColumnHeader() != null) {
            scroll.getColumnHeader().setBackground(new Color(6, 31, 70));
            scroll.getColumnHeader().setOpaque(true);
        }

        tablaPanel.add(scroll, BorderLayout.CENTER);

        JPanel bottomPanel = new PanelRedondeado();
        bottomPanel.setLayout(null);
        bottomPanel.setBounds(40, 615, 1195, 70);
        root.add(bottomPanel);

        JLabel lblTotal = new JLabel("Ventas netas:");
        lblTotal.setForeground(BLANCO);
        lblTotal.setFont(new Font("Arial", Font.BOLD, 16));
        lblTotal.setBounds(20, 20, 120, 30);
        bottomPanel.add(lblTotal);

        lblMonto = new JLabel("S/ 0.00");
        lblMonto.setForeground(AMARILLO);
        lblMonto.setFont(new Font("Arial", Font.BOLD, 20));
        lblMonto.setBounds(140, 20, 180, 30);
        bottomPanel.add(lblMonto);

        JLabel lblRegistros = new JLabel("Registros:");
        lblRegistros.setForeground(BLANCO);
        lblRegistros.setFont(new Font("Arial", Font.BOLD, 16));
        lblRegistros.setBounds(370, 20, 100, 30);
        bottomPanel.add(lblRegistros);

        lblCantidad = new JLabel("0");
        lblCantidad.setForeground(AMARILLO);
        lblCantidad.setFont(new Font("Arial", Font.BOLD, 20));
        lblCantidad.setBounds(470, 20, 80, 30);
        bottomPanel.add(lblCantidad);

        JLabel lblFormato = new JLabel("Formato de fecha: dd/MM/yyyy");
        lblFormato.setForeground(GRIS);
        lblFormato.setFont(new Font("Arial", Font.BOLD, 13));
        lblFormato.setBounds(690, 20, 250, 30);
        bottomPanel.add(lblFormato);

        btnBuscar.addActionListener(e -> verificarPrecondicionYConsultar(true));

        cargarSalas();
        verificarPrecondicionInicial();

        CINEXResponsive.adaptarLayoutAbsoluto(
                this,
                root,
                1280,
                900,
                0
        );
    }

    private void prepararPantallaInicial() {
        modelo.setRowCount(0);
        actualizarResumen(0, 0.0);
        lblEstadoConsulta.setText("Seleccione filtros y presione BUSCAR.");
        lblEstadoConsulta.setForeground(GRIS);
    }

    private void verificarPrecondicionInicial() {
        btnBuscar.setEnabled(false);
        modelo.setRowCount(0);
        actualizarResumen(0, 0.0);
        lblEstadoConsulta.setText("Validando ventas registradas...");
        lblEstadoConsulta.setForeground(AMARILLO);

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return controlHistorialVentas.existenVentasRegistradas();
            }

            @Override
            protected void done() {
                try {
                    boolean existen = get();

                    modelo.setRowCount(0);
                    actualizarResumen(0, 0.0);

                    if (!existen) {
                        btnBuscar.setEnabled(false);
                        lblEstadoConsulta.setText("Deben existir ventas registradas.");
                        lblEstadoConsulta.setForeground(ROJO);

                        JOptionPane.showMessageDialog(
                                VentasGerenteCINEXGUI.this,
                                "Deben existir ventas registradas.",
                                "Precondición no cumplida",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }

                    btnBuscar.setEnabled(true);
                    prepararPantallaInicial();

                } catch (Exception e) {
                    btnBuscar.setEnabled(false);
                    mostrarError("No se pudo verificar la existencia de ventas registradas.", e);
                }
            }
        };

        worker.execute();
    }

    private void abrirMenuPrincipal() {
        String[] posiblesClases = {
                "interfaz.MenuGerenteCINEXGUI",
                "interfaz.MenuPrincipalCINEXGUI",
                "interfaz.MenuPrincipalGUI"
        };

        for (String nombreClase : posiblesClases) {
            try {
                Class<?> clase = Class.forName(nombreClase);
                JFrame ventana;

                try {
                    ventana = (JFrame) clase.getConstructor(String.class).newInstance(usuarioActual);
                } catch (NoSuchMethodException ex) {
                    ventana = (JFrame) clase.getConstructor().newInstance();
                }

                ventana.setVisible(true);
                dispose();
                return;

            } catch (ClassNotFoundException e) {
                // Intenta con el siguiente nombre de clase.
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        this,
                        "No se pudo abrir el menú principal.\n" + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        }

        JOptionPane.showMessageDialog(
                this,
                "No se encontró la clase del menú principal.",
                "Menú principal",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void verificarPrecondicionYConsultar(boolean mostrarMensajeSiVacio) {
        btnBuscar.setEnabled(false);
        btnBuscar.setText("VALIDANDO...");
        lblEstadoConsulta.setText("Validando ventas...");
        lblEstadoConsulta.setForeground(AMARILLO);

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return controlHistorialVentas.existenVentasRegistradas();
            }

            @Override
            protected void done() {
                try {
                    boolean existen = get();

                    btnBuscar.setEnabled(true);
                    btnBuscar.setText("BUSCAR");

                    if (!existen) {
                        modelo.setRowCount(0);
                        actualizarResumen(0, 0.0);
                        lblEstadoConsulta.setText("Deben existir ventas registradas.");
                        lblEstadoConsulta.setForeground(ROJO);
                        JOptionPane.showMessageDialog(
                                VentasGerenteCINEXGUI.this,
                                "Deben existir ventas registradas.",
                                "Precondición no cumplida",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }

                    procesarConsultaHistorial(mostrarMensajeSiVacio);

                } catch (Exception e) {
                    btnBuscar.setEnabled(true);
                    btnBuscar.setText("BUSCAR");
                    mostrarError("No se pudo verificar la existencia de ventas registradas.", e);
                }
            }
        };

        worker.execute();
    }

    private void cargarSalas() {
        cbSala.removeAllItems();
        cbSala.addItem("Todas");

        for (SalaCINEX sala : controlHistorialVentas.listarSalas()) {
            cbSala.addItem(sala.getNombre());
        }
    }

    private void procesarConsultaHistorial(boolean mostrarMensajeSiVacio) {
        LocalDate fechaInicio;
        LocalDate fechaFin;

        try {
            fechaInicio = parsearFecha(txtInicio.getText().trim());
            fechaFin = parsearFecha(txtFin.getText().trim());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ingrese fechas válidas con el formato dd/MM/yyyy.",
                    "Fechas inválidas",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (fechaInicio.isAfter(fechaFin)) {
            JOptionPane.showMessageDialog(
                    this,
                    "La fecha de inicio no puede ser mayor que la fecha fin.",
                    "Rango inválido",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String sala = String.valueOf(cbSala.getSelectedItem());
        String metodo = String.valueOf(cbMetodo.getSelectedItem());

        btnBuscar.setEnabled(false);
        btnBuscar.setText("BUSCANDO...");
        lblEstadoConsulta.setText("Procesando consulta...");
        lblEstadoConsulta.setForeground(AMARILLO);

        SwingWorker<ArrayList<VentaCINEX>, Void> worker = new SwingWorker<ArrayList<VentaCINEX>, Void>() {
            @Override
            protected ArrayList<VentaCINEX> doInBackground() {
                return controlHistorialVentas.consultarVentas(fechaInicio, fechaFin, sala, metodo);
            }

            @Override
            protected void done() {
                btnBuscar.setEnabled(true);
                btnBuscar.setText("BUSCAR");

                try {
                    ArrayList<VentaCINEX> ventas = get();
                    mostrarVentas(ventas);

                    if (ventas.isEmpty()) {
                        lblEstadoConsulta.setText("No se encontraron ventas registradas.");
                        lblEstadoConsulta.setForeground(ROJO);

                        if (mostrarMensajeSiVacio) {
                            JOptionPane.showMessageDialog(
                                    VentasGerenteCINEXGUI.this,
                                    "No se encontraron ventas registradas.",
                                    "Historial de ventas",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                        }
                    } else {
                        lblEstadoConsulta.setText("Historial de ventas encontrado.");
                        lblEstadoConsulta.setForeground(VERDE);
                    }

                } catch (Exception e) {
                    mostrarError("No se pudo consultar el historial de ventas.", e);
                }
            }
        };

        worker.execute();
    }

    private LocalDate parsearFecha(String texto) {
        return LocalDate.parse(texto, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private void mostrarVentas(ArrayList<VentaCINEX> ventas) {
        modelo.setRowCount(0);

        double totalNeto = 0.0;
        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

        for (VentaCINEX v : ventas) {
            totalNeto += v.getTotalNeto();

            LocalDateTime fechaHora = v.getFechaHora();
            String fecha = fechaHora == null ? "-" : fechaHora.toLocalDate().format(fechaFmt);
            String hora = fechaHora == null ? "-" : fechaHora.toLocalTime().format(horaFmt)
                    .replace("AM", "a. m.")
                    .replace("PM", "p. m.");

            modelo.addRow(new Object[]{
                    v.getNumeroVenta(),
                    fecha,
                    hora,
                    v.getPelicula(),
                    v.getSala(),
                    v.getAsientos(),
                    v.getTotal(),
                    v.getMetodoPago(),
                    v.getVendedor(),
                    v.getEstadoReembolso(),
                    v.getEntradasReembolsadas(),
                    v.getAsientosReembolsados(),
                    v.getMontoReembolsado(),
                    v.getTotalNeto()
            });
        }

        actualizarResumen(ventas.size(), totalNeto);
    }

    private void actualizarResumen(int cantidad, double total) {
        lblCantidad.setText(String.valueOf(cantidad));
        lblMonto.setText("S/ " + String.format("%.2f", total));
    }

    private void mostrarError(String mensaje, Exception e) {
        lblEstadoConsulta.setText("Error de consulta.");
        lblEstadoConsulta.setForeground(ROJO);
        JOptionPane.showMessageDialog(
                this,
                mensaje + "\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void configurarTabla() {
        tabla.setBackground(AZUL_PANEL);
        tabla.setForeground(BLANCO);
        tabla.setFont(new Font("Arial", Font.PLAIN, 13));
        tabla.setRowHeight(36);
        tabla.setShowGrid(false);
        tabla.setSelectionBackground(new Color(15, 55, 100));
        tabla.setSelectionForeground(Color.WHITE);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setAutoCreateRowSorter(true);
        tabla.setOpaque(true);
        tabla.setFillsViewportHeight(true);

        JTableHeader header = tabla.getTableHeader();
        header.setBackground(new Color(6, 31, 70));
        header.setForeground(BLANCO);
        header.setFont(new Font("Arial", Font.BOLD, 13));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        header.setOpaque(true);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column
                );

                lbl.setBackground(new Color(6, 31, 70));
                lbl.setForeground(BLANCO);
                lbl.setFont(new Font("Arial", Font.BOLD, 13));
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setOpaque(true);
                lbl.setBorder(new LineBorder(AZUL_BORDE, 1));

                return lbl;
            }
        };

        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        DefaultTableCellRenderer centro = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                if (value instanceof Double) {
                    setText("S/ " + String.format("%.2f", (Double) value));
                } else {
                    setText(value == null ? "" : value.toString());
                }
            }
        };

        centro.setHorizontalAlignment(SwingConstants.CENTER);
        centro.setBackground(AZUL_PANEL);
        centro.setForeground(BLANCO);
        centro.setOpaque(true);

        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(centro);
        }

        tabla.getColumnModel().getColumn(0).setPreferredWidth(105);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(160);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(125);
        tabla.getColumnModel().getColumn(7).setPreferredWidth(135);
        tabla.getColumnModel().getColumn(8).setPreferredWidth(120);
    }

    private void actualizarFechaHora() {
        LocalDateTime ahora = LocalDateTime.now();

        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String horaTexto = ahora.format(formatoHora)
                .replace("AM", "a. m.")
                .replace("PM", "p. m.");

        lblHora.setText(horaTexto);
        lblFecha.setText(ahora.format(formatoFecha));
    }

    private JLabel crearTextoSuperior(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("Arial", Font.BOLD, 15));
        return lbl;
    }

    private JLabel crearLabelFiltro(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(BLANCO);
        lbl.setFont(new Font("Arial", Font.BOLD, 13));
        return lbl;
    }

    private JTextField crearCampoFiltro(String texto) {
        JTextField campo = new JTextField(texto);
        campo.setBackground(AZUL_PANEL);
        campo.setForeground(BLANCO);
        campo.setCaretColor(BLANCO);
        campo.setFont(new Font("Arial", Font.PLAIN, 13));
        campo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1, true),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        return campo;
    }

    private JButton crearBotonCalendario() {
        JButton boton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Fondo del botón
                g2.setColor(AZUL_PANEL);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 5, 5);

                // Borde del botón
                g2.setColor(AZUL_BORDE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 5, 5);

                // Ícono de calendario dibujado manualmente.
                // Así se ve igual aunque Windows/Java no muestre emojis.
                int iconW = 18;
                int iconH = 20;
                int x = (w - iconW) / 2;
                int y = (h - iconH) / 2;

                g2.setColor(BLANCO);
                g2.drawRoundRect(x, y + 2, iconW, iconH - 2, 4, 4);

                g2.setColor(AMARILLO);
                g2.fillRoundRect(x + 1, y + 3, iconW - 1, 6, 3, 3);

                g2.setColor(BLANCO);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(x + 5, y, x + 5, y + 5);
                g2.drawLine(x + iconW - 5, y, x + iconW - 5, y + 5);

                g2.setStroke(new BasicStroke(1f));
                g2.fillRect(x + 5, y + 12, 2, 2);
                g2.fillRect(x + 10, y + 12, 2, 2);
                g2.fillRect(x + 5, y + 16, 2, 2);
                g2.fillRect(x + 10, y + 16, 2, 2);

                g2.dispose();
            }
        };

        boton.setToolTipText("Seleccionar fecha");
        boton.setFocusPainted(false);
        boton.setContentAreaFilled(false);
        boton.setOpaque(false);
        boton.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return boton;
    }

    private void mostrarCalendario(JTextField campoFecha) {
        LocalDate fechaBase;

        try {
            fechaBase = parsearFecha(campoFecha.getText().trim());
        } catch (Exception e) {
            fechaBase = LocalDate.now();
        }

        JDialog dialogo = new JDialog(this, "Seleccionar fecha", true);
        dialogo.setResizable(false);
        dialogo.setSize(330, 340);
        dialogo.setLocationRelativeTo(campoFecha);
        dialogo.getContentPane().setBackground(AZUL_PANEL);

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(AZUL_PANEL);
        contenedor.setBorder(new LineBorder(AZUL_BORDE, 1, true));

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(AZUL_PANEL);
        cabecera.setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 10));

        JButton btnAnterior = crearBotonMes("<");
        JButton btnSiguiente = crearBotonMes(">");

        JLabel lblMes = new JLabel("", SwingConstants.CENTER);
        lblMes.setForeground(BLANCO);
        lblMes.setFont(new Font("Arial", Font.BOLD, 15));

        cabecera.add(btnAnterior, BorderLayout.WEST);
        cabecera.add(lblMes, BorderLayout.CENTER);
        cabecera.add(btnSiguiente, BorderLayout.EAST);
        contenedor.add(cabecera, BorderLayout.NORTH);

        JPanel panelDias = new JPanel(new GridLayout(0, 7, 4, 4));
        panelDias.setBackground(AZUL_PANEL);
        panelDias.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        contenedor.add(panelDias, BorderLayout.CENTER);

        YearMonth[] mesActual = {YearMonth.from(fechaBase)};

        Runnable[] pintarCalendario = new Runnable[1];
        pintarCalendario[0] = () -> {
            panelDias.removeAll();

            String nombreMes = mesActual[0].getMonth()
                    .getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
            nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);
            lblMes.setText(nombreMes + " " + mesActual[0].getYear());

            String[] diasSemana = {"L", "M", "M", "J", "V", "S", "D"};
            for (String dia : diasSemana) {
                JLabel lblDia = new JLabel(dia, SwingConstants.CENTER);
                lblDia.setForeground(AMARILLO);
                lblDia.setFont(new Font("Arial", Font.BOLD, 12));
                panelDias.add(lblDia);
            }

            LocalDate primerDia = mesActual[0].atDay(1);
            int espaciosAntes = primerDia.getDayOfWeek().getValue() - 1;

            for (int i = 0; i < espaciosAntes; i++) {
                JPanel vacio = new JPanel();
                vacio.setOpaque(false);
                panelDias.add(vacio);
            }

            int totalDias = mesActual[0].lengthOfMonth();
            LocalDate fechaSeleccionadaActual = null;

            try {
                fechaSeleccionadaActual = parsearFecha(campoFecha.getText().trim());
            } catch (Exception ignored) {
            }

            for (int dia = 1; dia <= totalDias; dia++) {
                LocalDate fecha = mesActual[0].atDay(dia);
                boolean esHoy = fecha.equals(LocalDate.now());
                boolean esSeleccionada = fechaSeleccionadaActual != null && fecha.equals(fechaSeleccionadaActual);

                JComponent btnDia = crearBotonDiaCalendario(String.valueOf(dia), esHoy, esSeleccionada);

                btnDia.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        campoFecha.setText(fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        dialogo.dispose();
                    }
                });

                panelDias.add(btnDia);
            }

            panelDias.revalidate();
            panelDias.repaint();
        };

        btnAnterior.addActionListener(e -> {
            mesActual[0] = mesActual[0].minusMonths(1);
            pintarCalendario[0].run();
        });

        btnSiguiente.addActionListener(e -> {
            mesActual[0] = mesActual[0].plusMonths(1);
            pintarCalendario[0].run();
        });

        pintarCalendario[0].run();

        dialogo.setContentPane(contenedor);
        dialogo.setVisible(true);
    }


    private JComponent crearBotonDiaCalendario(String texto, boolean esHoy, boolean esSeleccionada) {
        JPanel diaPanel = new JPanel() {
            private boolean hover = false;

            {
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

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color fondo;
                Color textoColor;

                if (esSeleccionada) {
                    fondo = AMARILLO;
                    textoColor = AZUL_FONDO_1;
                } else if (esHoy) {
                    fondo = new Color(245, 196, 0, 190);
                    textoColor = AZUL_FONDO_1;
                } else if (hover) {
                    fondo = new Color(18, 62, 115);
                    textoColor = BLANCO;
                } else {
                    fondo = new Color(8, 38, 82);
                    textoColor = BLANCO;
                }

                g2.setColor(fondo);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

                g2.setColor(AZUL_BORDE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

                g2.setColor(textoColor);
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(texto)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(texto, x, y);

                g2.dispose();
            }
        };

        return diaPanel;
    }

    private JButton crearBotonMes(String texto) {
        JButton boton = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(AMARILLO);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 5, 5);

                g2.setColor(AZUL_FONDO_1);
                g2.setFont(new Font("Arial", Font.BOLD, 15));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(texto)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(texto, x, y);

                g2.dispose();
            }
        };

        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setContentAreaFilled(false);
        boton.setOpaque(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setPreferredSize(new Dimension(45, 30));
        return boton;
    }

    private JComboBox<String> crearCombo(String[] opciones) {
        JComboBox<String> combo = new JComboBox<>(opciones);

        combo.setBackground(AZUL_PANEL);
        combo.setForeground(BLANCO);
        combo.setFont(new Font("Arial", Font.BOLD, 13));
        combo.setFocusable(false);
        combo.setOpaque(true);
        combo.setEditable(false);
        combo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1, true),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)
        ));

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus
                );

                lbl.setText(value == null ? "" : value.toString());
                lbl.setFont(new Font("Arial", Font.BOLD, 13));
                lbl.setOpaque(true);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

                if (isSelected) {
                    lbl.setBackground(new Color(15, 55, 100));
                    lbl.setForeground(BLANCO);
                } else {
                    lbl.setBackground(AZUL_PANEL);
                    lbl.setForeground(BLANCO);
                }

                return lbl;
            }
        });

        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton boton = new JButton("v") {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(AZUL_PANEL);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        g2.setColor(BLANCO);
                        g2.setFont(new Font("Arial", Font.BOLD, 10));
                        FontMetrics fm = g2.getFontMetrics();
                        String texto = getText();
                        int x = (getWidth() - fm.stringWidth(texto)) / 2;
                        int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                        g2.drawString(texto, x, y);
                        g2.dispose();
                    }
                };
                boton.setContentAreaFilled(false);
                boton.setOpaque(false);
                boton.setBorder(new LineBorder(AZUL_BORDE, 1));
                boton.setFocusable(false);
                return boton;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(AZUL_PANEL);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });

        combo.setSelectedIndex(0);
        return combo;
    }

    private ImageIcon loadScaledIcon(String path, int width, int height) {
        try {
            BufferedImage img = ImageIO.read(new File(path));
            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(img, 0, 0, width, height, null);
            g2.dispose();

            return new ImageIcon(scaled);

        } catch (Exception e) {
            System.out.println("No se pudo cargar el logo: " + path);
            return new ImageIcon();
        }
    }

    class FondoPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            GradientPaint fondo = new GradientPaint(
                    0, 0, AZUL_FONDO_1,
                    getWidth(), getHeight(), AZUL_FONDO_2
            );

            g2.setPaint(fondo);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 255, 255, 4));
            g2.fillOval(1220, 120, 250, 250);

            g2.dispose();
        }
    }

    class SidebarPanel extends JPanel {
        public SidebarPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setColor(AZUL_SIDEBAR);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(45, 75, 115));
            g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());

            g2.dispose();
            super.paintComponent(g);
        }
    }

    class SidebarButton extends JPanel {

        private final String emoji;
        private final String texto;
        private final boolean activo;
        private boolean hover = false;

        public SidebarButton(String emoji, String texto, boolean activo) {
            this.emoji = emoji;
            this.texto = texto;
            this.activo = activo;

            setOpaque(false);
            setLayout(null);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            JLabel lblEmoji = new JLabel(emoji, SwingConstants.CENTER);

if ("\u2261".equals(emoji) || "\u21A9".equals(emoji) || "≡".equals(emoji) || "↩".equals(emoji)) {
    lblEmoji.setFont(new Font("Segoe UI Symbol", Font.BOLD, 22));
} else {
    lblEmoji.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
}

lblEmoji.setBounds(14, 12, 32, 28);
lblEmoji.setForeground(activo ? AZUL_FONDO_1 : BLANCO);
add(lblEmoji);

            JLabel lblTexto = new JLabel(texto);
            lblTexto.setFont(new Font("Arial", Font.BOLD, texto.length() > 14 ? 12 : 14));
            lblTexto.setForeground(activo ? AZUL_FONDO_1 : BLANCO);
            lblTexto.setBounds(52, 10, 124, 34);
            add(lblTexto);

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

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (activo) {
                g2.setColor(AMARILLO);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            } else if (hover) {
                g2.setColor(new Color(15, 40, 80));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    class PanelRedondeado extends JPanel {
        public PanelRedondeado() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(AZUL_PANEL);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

            g2.setColor(AZUL_BORDE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentasGerenteCINEXGUI("gerente01").setVisible(true));
    }
}
