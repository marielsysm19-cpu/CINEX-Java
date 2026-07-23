package interfaz;

import control.ControlCalcularIngresosCINEX;
import control.ControlConsultarVentasPeriodoCINEX;
import control.ControlFiltrarReporteCINEX;
import control.ControlGenerarIndicadoresCINEX;
import control.ControlOrdenarPeliculasCINEX;
import control.ControlProcesarConsultaCINEX;
import control.BDCINEX;
import entidad.PeliculaCINEX;
import entidad.ReporteCINEX;
import entidad.VentaCINEX;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class ReportesGerenteCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(1, 15, 42);
    private final Color AZUL_FONDO_2 = new Color(4, 35, 78);
    private final Color AZUL_SIDEBAR = new Color(3, 18, 45);
    private final Color AZUL_PANEL = new Color(5, 27, 62);
    private final Color AZUL_PANEL_2 = new Color(7, 34, 76);
    private final Color AZUL_BORDE = new Color(50, 82, 125);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = Color.WHITE;
    private final Color GRIS = new Color(185, 198, 215);
    private final Color ROJO = new Color(230, 65, 65);
    private final Color VERDE = new Color(35, 180, 85);

    private static final String REPORTE_VENTAS = "Consultar reporte de ventas";
    private static final String INGRESOS_PELICULA = "Consultar ingresos por película";
    private static final String PELICULAS_MAS_VISTAS = "Consultar películas más vistas";

    private JLabel lblHora;
    private JLabel lblFecha;
    private JLabel lblEstado;
    private JLabel lblCard1Titulo;
    private JLabel lblCard1Valor;
    private JLabel lblCard2Titulo;
    private JLabel lblCard2Valor;
    private JLabel lblCard3Titulo;
    private JLabel lblCard3Valor;
    private JLabel lblCard4Titulo;
    private JLabel lblCard4Valor;

    private JTextField txtInicio;
    private JTextField txtFin;
    private JComboBox<String> cbTipoReporte;
    private JComboBox<String> cbSala;
    private JComboBox<String> cbMetodo;
    private JComboBox<String> cbPelicula;
    private JLabel lblInicioFiltro;
    private JLabel lblFinFiltro;
    private JLabel lblSalaFiltro;
    private JLabel lblMetodoFiltro;
    private JLabel lblPeliculaFiltro;
    private JButton btnGenerar;
    private JButton btnExportarExcel;
    private JButton btnCalInicio;
    private JButton btnCalFin;
    private JTable tablaResultados;
    private DefaultTableModel modelo;
    private GraficoBarrasPanel graficoPanel;

    private JPanel rootReportes;
    private JPanel panelFiltrosReportes;
    private JPanel panelTablaReportes;
    private JPanel cardIndicador1;
    private JPanel cardIndicador2;
    private JPanel cardIndicador3;
    private JPanel cardIndicador4;

    private final String usuarioActual;

    private final ControlFiltrarReporteCINEX controlFiltrarReporte = new ControlFiltrarReporteCINEX();
    private final ControlGenerarIndicadoresCINEX controlGenerarIndicadores = new ControlGenerarIndicadoresCINEX();
    private final ControlProcesarConsultaCINEX controlProcesarConsulta = new ControlProcesarConsultaCINEX();
    private final ControlCalcularIngresosCINEX controlCalcularIngresos = new ControlCalcularIngresosCINEX();
    private final ControlConsultarVentasPeriodoCINEX controlConsultarVentasPeriodo = new ControlConsultarVentasPeriodoCINEX();
    private final ControlOrdenarPeliculasCINEX controlOrdenarPeliculas = new ControlOrdenarPeliculasCINEX();

    public ReportesGerenteCINEXGUI(String usuario) {
        this.usuarioActual = usuario == null || usuario.trim().isEmpty() ? "gerente" : usuario.trim();

        setTitle("CINEX - Reportes de ventas");
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        rootReportes = new FondoPanel();
        JPanel root = rootReportes;
        root.setLayout(null);
        setContentPane(root);

        crearSidebar(root);
        crearCabecera(root);
        crearFiltros(root);
        crearIndicadores(root);
        crearResultados(root);

        actualizarFechaHora();
        new Timer(1000, e -> actualizarFechaHora()).start();

        cargarSalas();
        cargarPeliculas();
        actualizarTipoReporte();
        mostrarDashboardInicial();
        instalarAjusteReportes(root);
}

    private void instalarAjusteReportes(JPanel root) {
        root.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ajustarDistribucionReportes();
            }
        });

        SwingUtilities.invokeLater(this::ajustarDistribucionReportes);
    }

    private void ajustarDistribucionReportes() {
        if (rootReportes == null
                || panelFiltrosReportes == null
                || panelTablaReportes == null
                || graficoPanel == null) {
            return;
        }

        int ancho = rootReportes.getWidth();
        int alto = rootReportes.getHeight();

        if (ancho <= 0 || alto <= 0) {
            return;
        }

        final int anchoSidebar = 215;
        final int xContenido = 255;
        final int margenDerecho = 40;
        final int separacion = 25;

        int anchoContenido = Math.max(
                980,
                ancho - xContenido - margenDerecho
        );

        for (Component componente : rootReportes.getComponents()) {
            if (componente instanceof SidebarPanel) {
                componente.setBounds(0, 0, anchoSidebar, alto);
                break;
            }
        }

        panelFiltrosReportes.setBounds(
                xContenido,
                155,
                anchoContenido,
                150
        );

        int anchoCard = Math.max(
                210,
                (anchoContenido - separacion * 3) / 4
        );

        JPanel[] cards = {
                cardIndicador1,
                cardIndicador2,
                cardIndicador3,
                cardIndicador4
        };

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null) {
                cards[i].setBounds(
                        xContenido + i * (anchoCard + separacion),
                        320,
                        anchoCard,
                        82
                );
            }
        }

        int yResultados = 420;
        int altoResultados = Math.max(
                255,
                alto - yResultados - 65
        );

        int anchoTabla = Math.max(
                570,
                (int) Math.round(
                        (anchoContenido - separacion) * 0.58
                )
        );

        int anchoGrafico = Math.max(
                390,
                anchoContenido - anchoTabla - separacion
        );

        panelTablaReportes.setBounds(
                xContenido,
                yResultados,
                anchoTabla,
                altoResultados
        );

        graficoPanel.setBounds(
                xContenido + anchoTabla + separacion,
                yResultados,
                anchoGrafico,
                altoResultados
        );

        ajustarFiltrosReportes();

        rootReportes.revalidate();
        rootReportes.repaint();
    }

    private void ajustarFiltrosReportes() {
        if (panelFiltrosReportes == null
                || cbTipoReporte == null) {
            return;
        }

        int anchoPanel = panelFiltrosReportes.getWidth();
        cbTipoReporte.setBounds(20, 43, 300, 38);

        String tipo = String.valueOf(
                cbTipoReporte.getSelectedItem()
        );

        if (REPORTE_VENTAS.equals(tipo)) {
            lblInicioFiltro.setBounds(345, 18, 120, 22);
            txtInicio.setBounds(345, 43, 112, 38);
            btnCalInicio.setBounds(460, 43, 38, 38);

            lblFinFiltro.setBounds(520, 18, 120, 22);
            txtFin.setBounds(520, 43, 112, 38);
            btnCalFin.setBounds(635, 43, 38, 38);

            lblSalaFiltro.setBounds(695, 18, 120, 22);
            cbSala.setBounds(695, 43, 125, 38);

            lblMetodoFiltro.setBounds(840, 18, 120, 22);
            cbMetodo.setBounds(840, 43, 160, 38);

            btnGenerar.setBounds(
                    Math.max(760, anchoPanel - 210),
                    88,
                    180,
                    38
            );

            lblEstado.setBounds(
                    330,
                    94,
                    Math.max(360, anchoPanel - 570),
                    25
            );

        } else if (INGRESOS_PELICULA.equals(tipo)) {
            lblPeliculaFiltro.setBounds(345, 18, 120, 22);
            cbPelicula.setBounds(
                    345,
                    43,
                    Math.min(430, Math.max(340, anchoPanel - 780)),
                    38
            );

            btnGenerar.setBounds(
                    Math.max(800, anchoPanel - 230),
                    43,
                    200,
                    38
            );

            lblEstado.setBounds(
                    345,
                    94,
                    Math.max(400, anchoPanel - 600),
                    25
            );

        } else {
            lblInicioFiltro.setBounds(345, 18, 120, 22);
            txtInicio.setBounds(345, 43, 112, 38);
            btnCalInicio.setBounds(460, 43, 38, 38);

            lblFinFiltro.setBounds(520, 18, 120, 22);
            txtFin.setBounds(520, 43, 112, 38);
            btnCalFin.setBounds(635, 43, 38, 38);

            btnGenerar.setBounds(
                    Math.max(720, anchoPanel - 230),
                    43,
                    200,
                    38
            );

            lblEstado.setBounds(
                    345,
                    94,
                    Math.max(400, anchoPanel - 600),
                    25
            );
        }

        panelFiltrosReportes.revalidate();
        panelFiltrosReportes.repaint();
    }

    private void crearSidebar(JPanel root) {
        JPanel sidebar = new SidebarPanel();
        sidebar.setLayout(null);
        sidebar.setBounds(0, 0, 215, 1000);
        root.add(sidebar);

        JLabel lblLogo = new JLabel();
        lblLogo.setBounds(25, 15, 160, 75);
        lblLogo.setIcon(loadScaledIcon("imagenes/logocinex.png", 150, 60));
        sidebar.add(lblLogo);

        SidebarButton btnReportesVentas = new SidebarButton("📄", "Reportes de ventas", true);
        btnReportesVentas.setBounds(18, 110, 178, 58);
        sidebar.add(btnReportesVentas);

        SidebarButton btnHistorialVentas = new SidebarButton("🎟", "Historial de ventas", false);
        btnHistorialVentas.setBounds(18, 178, 178, 58);
        sidebar.add(btnHistorialVentas);

        SidebarButton btnListaClientes = new SidebarButton("👥", "Lista de clientes", false);
        btnListaClientes.setBounds(18, 246, 178, 58);
        sidebar.add(btnListaClientes);

        SidebarButton btnNotificaciones =
                new SidebarButton(
                        "🔔",
                        "Notificaciones",
                        false
                );
        btnNotificaciones.setBounds(
                18,
                314,
                178,
                58
        );
        sidebar.add(btnNotificaciones);

        SidebarButton btnMenuPrincipal = new SidebarButton("≡", "Menú principal", false);
        btnMenuPrincipal.setBounds(18, 672, 178, 52);
        sidebar.add(btnMenuPrincipal);

        JSeparator sep = new JSeparator();
        sep.setBounds(20, 735, 175, 1);
        sep.setForeground(new Color(35, 65, 105));
        sidebar.add(sep);

        SidebarButton btnCerrar = new SidebarButton("\u21A9", "Cerrar sesión", false);
        btnCerrar.setBounds(18, 755, 178, 52);
        sidebar.add(btnCerrar);

        btnHistorialVentas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                new VentasGerenteCINEXGUI(usuarioActual).setVisible(true);
                dispose();
            }
        });

        btnListaClientes.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                new ClientesGerenteCINEXGUI(usuarioActual).setVisible(true);
                dispose();
            }
        });

        btnNotificaciones.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(
                            MouseEvent e
                    ) {
                        CINEXTransiciones.cambiar(
                                ReportesGerenteCINEXGUI.this,
                                new NotificacionesGerenteCINEXGUI(
                                        usuarioActual
                                )
                        );
                    }
                }
        );

        btnMenuPrincipal.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                new MenuGerenteCINEXGUI(usuarioActual).setVisible(true);
                dispose();
            }
        });

        btnCerrar.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int opcion = JOptionPane.showConfirmDialog(
                        ReportesGerenteCINEXGUI.this,
                        "¿Deseas cerrar sesión?",
                        "Confirmar cierre de sesión",
                        JOptionPane.YES_NO_OPTION
                );
                if (opcion == JOptionPane.YES_OPTION) {
                    new LoginCINEXGUI().setVisible(true);
                    dispose();
                }
            }
        });
    }

    private void crearCabecera(JPanel root) {
        Rectangle pantalla = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int topX = Math.max(880, pantalla.width - 650);

        JLabel lblUsuario = crearTextoSuperior("Usuario: " + usuarioActual);
        lblUsuario.setBounds(topX, 18, 220, 25);
        root.add(lblUsuario);

        JLabel lblTerminal = crearTextoSuperior("Terminal: 01");
        lblTerminal.setBounds(topX + 190, 18, 150, 25);
        root.add(lblTerminal);

        lblHora = crearTextoSuperior("");
        lblHora.setBounds(topX + 330, 18, 130, 25);
        root.add(lblHora);

        lblFecha = crearTextoSuperior("");
        lblFecha.setBounds(topX + 460, 18, 160, 25);
        root.add(lblFecha);

        JLabel lblTitulo = new JLabel("Reportes de ventas");
        lblTitulo.setForeground(BLANCO);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 34));
        lblTitulo.setBounds(255, 78, 380, 40);
        root.add(lblTitulo);

        JLabel lblSubtitulo = new JLabel("Consulte reportes de ventas, ingresos y películas más vistas");
        lblSubtitulo.setForeground(GRIS);
        lblSubtitulo.setFont(new Font("Arial", Font.PLAIN, 17));
        lblSubtitulo.setBounds(257, 118, 560, 25);
        root.add(lblSubtitulo);
    }

    private void crearFiltros(JPanel root) {
        PanelRedondeado panel = new PanelRedondeado();
        panelFiltrosReportes = panel;
        panel.setLayout(null);
        panel.setBounds(255, 155, 980, 150);
        root.add(panel);

        JLabel lblTipo = crearLabelFiltro("Tipo de reporte:");
        lblTipo.setBounds(20, 18, 150, 22);
        panel.add(lblTipo);

        cbTipoReporte = crearCombo(new String[]{REPORTE_VENTAS, INGRESOS_PELICULA, PELICULAS_MAS_VISTAS});
        cbTipoReporte.setBounds(20, 43, 260, 38);
        cbTipoReporte.addActionListener(e -> actualizarTipoReporte());
        panel.add(cbTipoReporte);

        lblInicioFiltro = crearLabelFiltro("Fecha inicio:");
        lblInicioFiltro.setBounds(305, 18, 120, 22);
        panel.add(lblInicioFiltro);

        txtInicio = crearCampoFiltro(LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        txtInicio.setBounds(305, 43, 112, 38);
        panel.add(txtInicio);

        btnCalInicio = crearBotonCalendario();
        btnCalInicio.setBounds(420, 43, 38, 38);
        btnCalInicio.addActionListener(e -> mostrarCalendario(txtInicio));
        panel.add(btnCalInicio);

        lblFinFiltro = crearLabelFiltro("Fecha fin:");
        lblFinFiltro.setBounds(475, 18, 120, 22);
        panel.add(lblFinFiltro);

        txtFin = crearCampoFiltro(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        txtFin.setBounds(475, 43, 112, 38);
        panel.add(txtFin);

        btnCalFin = crearBotonCalendario();
        btnCalFin.setBounds(590, 43, 38, 38);
        btnCalFin.addActionListener(e -> mostrarCalendario(txtFin));
        panel.add(btnCalFin);

        lblSalaFiltro = crearLabelFiltro("Sala:");
        lblSalaFiltro.setBounds(650, 18, 120, 22);
        panel.add(lblSalaFiltro);

        cbSala = crearCombo(new String[]{"Todas"});
        cbSala.setBounds(650, 43, 125, 38);
        panel.add(cbSala);

        lblMetodoFiltro = crearLabelFiltro("Método:");
        lblMetodoFiltro.setBounds(795, 18, 120, 22);
        panel.add(lblMetodoFiltro);

        cbMetodo = crearCombo(new String[]{"Todos", "Efectivo", "Tarjeta Crédito", "Tarjeta Débito", "Yape", "Plin"});
        cbMetodo.setBounds(795, 43, 145, 38);
        panel.add(cbMetodo);

        lblPeliculaFiltro = crearLabelFiltro("Película:");
        lblPeliculaFiltro.setBounds(20, 88, 120, 22);
        panel.add(lblPeliculaFiltro);

        cbPelicula = crearCombo(new String[]{"Seleccione una película"});
        cbPelicula.setBounds(90, 85, 340, 38);
        panel.add(cbPelicula);

        btnGenerar = new JButton("GENERAR REPORTE");
        btnGenerar.setBounds(760, 85, 180, 38);
        btnGenerar.setBackground(AMARILLO);
        btnGenerar.setForeground(AZUL_FONDO_1);
        btnGenerar.setFont(new Font("Arial", Font.BOLD, 14));
        btnGenerar.setFocusPainted(false);
        btnGenerar.setBorderPainted(false);
        btnGenerar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGenerar.addActionListener(e -> generarReporte());
        panel.add(btnGenerar);

        lblEstado = new JLabel("Seleccione filtros y genere el reporte.");
        lblEstado.setForeground(GRIS);
        lblEstado.setFont(new Font("Arial", Font.BOLD, 13));
        lblEstado.setHorizontalAlignment(SwingConstants.CENTER);
        lblEstado.setBounds(455, 92, 300, 25);
        panel.add(lblEstado);
    }

    private void crearIndicadores(JPanel root) {
        cardIndicador1 = crearCard(255, 320, 230, 82);
        JPanel card1 = cardIndicador1;
        lblCard1Titulo = crearCardTitulo("Ventas totales");
        lblCard1Valor = crearCardValor("S/ 0.00");
        card1.add(lblCard1Titulo);
        card1.add(lblCard1Valor);
        root.add(card1);

        cardIndicador2 = crearCard(505, 320, 230, 82);
        JPanel card2 = cardIndicador2;
        lblCard2Titulo = crearCardTitulo("Entradas vendidas");
        lblCard2Valor = crearCardValor("0");
        card2.add(lblCard2Titulo);
        card2.add(lblCard2Valor);
        root.add(card2);

        cardIndicador3 = crearCard(755, 320, 230, 82);
        JPanel card3 = cardIndicador3;
        lblCard3Titulo = crearCardTitulo("Transacciones");
        lblCard3Valor = crearCardValor("0");
        card3.add(lblCard3Titulo);
        card3.add(lblCard3Valor);
        root.add(card3);

        cardIndicador4 = crearCard(1005, 320, 230, 82);
        JPanel card4 = cardIndicador4;
        lblCard4Titulo = crearCardTitulo("Ocupación promedio");
        lblCard4Valor = crearCardValor("0%");
        card4.add(lblCard4Titulo);
        card4.add(lblCard4Valor);
        root.add(card4);
    }

    private void crearResultados(JPanel root) {
        PanelRedondeado panelTabla = new PanelRedondeado();
        panelTablaReportes = panelTabla;
        panelTabla.setLayout(new BorderLayout());
        panelTabla.setBounds(255, 420, 570, 255);
        panelTabla.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.add(panelTabla);

        modelo = new DefaultTableModel(new String[]{"Resultado", "Entradas", "Ingresos"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaResultados = new JTable(modelo);
        configurarTabla(tablaResultados);

        JScrollPane scrollTabla = new JScrollPane(tablaResultados);
        configurarScrollTabla(scrollTabla);
        panelTabla.add(scrollTabla, BorderLayout.CENTER);

        JPanel panelExportar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panelExportar.setOpaque(false);
        panelExportar.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        btnExportarExcel = crearBotonExcel();
        btnExportarExcel.addActionListener(e -> exportarExcel());
        panelExportar.add(btnExportarExcel);
        panelTabla.add(panelExportar, BorderLayout.SOUTH);

        actualizarBotonExportar();

        graficoPanel = new GraficoBarrasPanel();
        graficoPanel.setBounds(845, 420, 390, 255);
        root.add(graficoPanel);
    }

    private void generarReporte() {
        String tipo = String.valueOf(cbTipoReporte.getSelectedItem());

        try {
            if (REPORTE_VENTAS.equals(tipo)) {
                generarReporteVentas();
            } else if (INGRESOS_PELICULA.equals(tipo)) {
                generarIngresosPorPelicula();
            } else if (PELICULAS_MAS_VISTAS.equals(tipo)) {
                generarPeliculasMasVistas();
            }
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Ingrese fechas válidas con formato dd/MM/yyyy.", "Fechas inválidas", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo generar el reporte.\n" + e.getMessage(), "Reportes", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generarReporteVentas() {
        LocalDate inicio = parsearFecha(txtInicio.getText());
        LocalDate fin = parsearFecha(txtFin.getText());
        if (inicio.isAfter(fin)) {
            JOptionPane.showMessageDialog(this, "La fecha de inicio no puede ser mayor que la fecha fin.", "Rango inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!existenVentasRegistradas()) {
            limpiarResultados();
            mostrarMensaje("Deben existir ventas registradas.", ROJO);
            JOptionPane.showMessageDialog(this, "Deben existir ventas registradas.", "Reporte de ventas", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String sala = String.valueOf(cbSala.getSelectedItem());
        String metodo = String.valueOf(cbMetodo.getSelectedItem());

        ArrayList<VentaCINEX> ventas = controlFiltrarReporte.solicitarFiltros(inicio, fin, sala, metodo);
        ReporteCINEX reporte = controlGenerarIndicadores.generarIndicadores(ventas, inicio, fin);

        mostrarReporteVentas(reporte);

        if (ventas.isEmpty()) {
            mostrarMensaje("No existen registros para los filtros seleccionados.", ROJO);
            JOptionPane.showMessageDialog(this, "No existen registros para los filtros seleccionados.", "Reporte de ventas", JOptionPane.INFORMATION_MESSAGE);
        } else {
            mostrarMensaje("El sistema muestra gráficos e indicadores.", VERDE);
        }
    }

    private void generarIngresosPorPelicula() {
        String titulo = String.valueOf(cbPelicula.getSelectedItem());
        if (titulo == null || titulo.trim().isEmpty() || titulo.equals("Seleccione una película")) {
            JOptionPane.showMessageDialog(this, "Seleccione una película.", "Ingresos por película", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PeliculaCINEX pelicula = controlProcesarConsulta.procesarConsulta(titulo);
        if (!controlProcesarConsulta.existenFuncionesProgramadas(pelicula)) {
            limpiarResultados();
            mostrarMensaje("No existen funciones programadas para la película seleccionada.", ROJO);
            JOptionPane.showMessageDialog(this, "No existen funciones programadas para la película seleccionada.", "Ingresos por película", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ReporteCINEX reporte = controlCalcularIngresos.calcularIngresosGenerados(pelicula);
        mostrarReporteIngresos(pelicula, reporte);

        if (reporte.getTotalIngresos() <= 0) {
            mostrarMensaje("No existen ingresos registrados.", ROJO);
            JOptionPane.showMessageDialog(this, "No existen ingresos registrados.", "Ingresos por película", JOptionPane.INFORMATION_MESSAGE);
        } else {
            mostrarMensaje("Se muestran los ingresos correspondientes.", VERDE);
        }
    }

    private void generarPeliculasMasVistas() {
        LocalDate inicio = parsearFecha(txtInicio.getText());
        LocalDate fin = parsearFecha(txtFin.getText());
        if (inicio.isAfter(fin)) {
            JOptionPane.showMessageDialog(this, "La fecha de inicio no puede ser mayor que la fecha fin.", "Rango inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ArrayList<PeliculaCINEX> peliculas = controlConsultarVentasPeriodo.procesarVentasPeriodo(inicio, fin);
        ArrayList<PeliculaCINEX> ranking = controlOrdenarPeliculas.generarRankingPeliculas(peliculas);
        ranking = controlOrdenarPeliculas.enviarRankingPeliculas(ranking);

        mostrarRankingPeliculas(ranking);

        if (ranking.isEmpty()) {
            mostrarMensaje("No existen ventas registradas en el periodo seleccionado.", ROJO);
            JOptionPane.showMessageDialog(this, "No existen ventas registradas en el periodo seleccionado.", "Películas más vistas", JOptionPane.INFORMATION_MESSAGE);
        } else {
            mostrarMensaje("Se visualiza el ranking de películas más vistas.", VERDE);
        }
    }

    private void mostrarReporteVentas(ReporteCINEX reporte) {
        configurarColumnas(new String[]{
                "Película",
                "Entradas vigentes",
                "Reembolsadas",
                "Ventas netas"
        });

        lblCard1Titulo.setText("Ventas netas");
        lblCard1Valor.setText(
                "S/ " + String.format(
                        "%.2f",
                        reporte.getTotalIngresos()
                )
        );

        lblCard2Titulo.setText("Entradas vigentes");
        lblCard2Valor.setText(
                String.valueOf(reporte.getEntradasVigentes())
        );

        lblCard3Titulo.setText("Monto reembolsado");
        lblCard3Valor.setText(
                "S/ " + String.format(
                        "%.2f",
                        reporte.getMontoReembolsado()
                )
        );

        lblCard4Titulo.setText("Entradas reembolsadas");
        lblCard4Valor.setText(
                String.valueOf(
                        reporte.getEntradasReembolsadas()
                )
        );

        modelo.setRowCount(0);

        LinkedHashMap<String, Integer> vigentesPorPelicula =
                new LinkedHashMap<>();

        LinkedHashMap<String, Integer> reembolsadasPorPelicula =
                new LinkedHashMap<>();

        LinkedHashMap<String, Double> ingresosNetosPorPelicula =
                new LinkedHashMap<>();

        for (VentaCINEX venta : reporte.getVentas()) {
            String pelicula = venta.getPelicula().isEmpty()
                    ? "Sin película"
                    : venta.getPelicula();

            vigentesPorPelicula.put(
                    pelicula,
                    vigentesPorPelicula.getOrDefault(pelicula, 0)
                            + venta.getEntradasVigentes()
            );

            reembolsadasPorPelicula.put(
                    pelicula,
                    reembolsadasPorPelicula.getOrDefault(pelicula, 0)
                            + venta.getEntradasReembolsadas()
            );

            ingresosNetosPorPelicula.put(
                    pelicula,
                    ingresosNetosPorPelicula.getOrDefault(
                            pelicula,
                            0.0
                    ) + venta.getTotalNeto()
            );
        }

        for (String pelicula : ingresosNetosPorPelicula.keySet()) {
            modelo.addRow(new Object[]{
                    pelicula,
                    vigentesPorPelicula.getOrDefault(pelicula, 0),
                    reembolsadasPorPelicula.getOrDefault(pelicula, 0),
                    "S/ " + String.format(
                            "%.2f",
                            ingresosNetosPorPelicula.getOrDefault(
                                    pelicula,
                                    0.0
                            )
                    )
            });
        }

        graficoPanel.setDatos(
                ingresosNetosPorPelicula,
                "Ventas netas por película"
        );

        actualizarBotonExportar();
    }

    private void mostrarReporteIngresos(
            PeliculaCINEX pelicula,
            ReporteCINEX reporte
    ) {
        configurarColumnas(new String[]{
                "Película",
                "Ventas brutas",
                "Reembolsos",
                "Ingresos netos"
        });

        lblCard1Titulo.setText("Película");
        lblCard1Valor.setText(
                recortar(pelicula.getTitulo(), 18)
        );

        lblCard2Titulo.setText("Ingresos netos");
        lblCard2Valor.setText(
                "S/ " + String.format(
                        "%.2f",
                        reporte.getTotalIngresos()
                )
        );

        lblCard3Titulo.setText("Entradas vigentes");
        lblCard3Valor.setText(
                String.valueOf(reporte.getEntradasVigentes())
        );

        lblCard4Titulo.setText("Entradas reembolsadas");
        lblCard4Valor.setText(
                String.valueOf(
                        reporte.getEntradasReembolsadas()
                )
        );

        modelo.setRowCount(0);
        modelo.addRow(new Object[]{
                pelicula.getTitulo(),
                "S/ " + String.format(
                        "%.2f",
                        reporte.getVentasBrutas()
                ),
                "S/ " + String.format(
                        "%.2f",
                        reporte.getMontoReembolsado()
                ),
                "S/ " + String.format(
                        "%.2f",
                        reporte.getTotalIngresos()
                )
        });

        graficoPanel.setDatos(
                reporte.getGrupos(),
                "Ingresos netos por película"
        );

        actualizarBotonExportar();
    }

    private void mostrarRankingPeliculas(ArrayList<PeliculaCINEX> ranking) {
        configurarColumnas(new String[]{"Ranking", "Película", "Entradas vendidas"});
        int totalEntradas = 0;
        double totalIngresos = 0.0;
        for (PeliculaCINEX pelicula : ranking) {
            totalEntradas += pelicula.getEntradasVendidas();
            totalIngresos += pelicula.getIngresosGenerados();
        }

        lblCard1Titulo.setText("Películas listadas");
        lblCard1Valor.setText(String.valueOf(ranking.size()));
        lblCard2Titulo.setText("Entradas vendidas");
        lblCard2Valor.setText(String.valueOf(totalEntradas));
        lblCard3Titulo.setText("Ingresos asociados");
        lblCard3Valor.setText("S/ " + String.format("%.2f", totalIngresos));
        lblCard4Titulo.setText("Más vista");
        lblCard4Valor.setText(ranking.isEmpty() ? "-" : recortar(ranking.get(0).getTitulo(), 18));

        modelo.setRowCount(0);
        LinkedHashMap<String, Double> datos = new LinkedHashMap<>();
        int puesto = 1;
        for (PeliculaCINEX pelicula : ranking) {
            modelo.addRow(new Object[]{puesto, pelicula.getTitulo(), pelicula.getEntradasVendidas()});
            datos.put(pelicula.getTitulo(), (double) pelicula.getEntradasVendidas());
            puesto++;
        }
        graficoPanel.setDatos(datos, "Películas más vistas");
        actualizarBotonExportar();
    }

    private void mostrarDashboardInicial() {
        modelo.setRowCount(0);
        actualizarBotonExportar();
        graficoPanel.setDatos(new LinkedHashMap<>(), "Reporte de ventas");
        prepararCardsReporteVentas();
    }

    private void limpiarResultados() {
        String tipo = String.valueOf(cbTipoReporte.getSelectedItem());
        if (REPORTE_VENTAS.equals(tipo)) {
            prepararCardsReporteVentas();
        } else if (INGRESOS_PELICULA.equals(tipo)) {
            prepararCardsIngresosPelicula();
        } else if (PELICULAS_MAS_VISTAS.equals(tipo)) {
            prepararCardsPeliculasMasVistas();
        }
        modelo.setRowCount(0);
        actualizarBotonExportar();
        graficoPanel.setDatos(new LinkedHashMap<>(), "Sin resultados");
    }

    private void actualizarTipoReporte() {
        String tipo = String.valueOf(cbTipoReporte.getSelectedItem());

        boolean esReporteVentas = REPORTE_VENTAS.equals(tipo);
        boolean esIngresosPelicula = INGRESOS_PELICULA.equals(tipo);
        boolean esPeliculasMasVistas = PELICULAS_MAS_VISTAS.equals(tipo);

        // CU-12: filtros de ventas. CU-13: solo película. CU-15: solo periodo.
        setVisibleFecha(esReporteVentas || esPeliculasMasVistas);
        setVisibleSalaMetodo(esReporteVentas);
        setVisiblePelicula(esIngresosPelicula);

        if (esReporteVentas) {
            configurarLayoutCU12();
            configurarColumnas(new String[]{"Película", "Entradas", "Ingresos"});
            btnGenerar.setText("GENERAR REPORTE");
            graficoPanel.setDatos(new LinkedHashMap<>(), "Reporte de ventas");
            mostrarMensaje("Seleccione filtros y genere el reporte.", GRIS);
            prepararCardsReporteVentas();
        } else if (esIngresosPelicula) {
            configurarLayoutCU13();
            configurarColumnas(new String[]{"Película", "Entradas vendidas", "Ingresos"});
            btnGenerar.setText("CONSULTAR INGRESOS");
            graficoPanel.setDatos(new LinkedHashMap<>(), "Ingresos por película");
            mostrarMensaje("Seleccione una película y consulte los ingresos.", GRIS);
            prepararCardsIngresosPelicula();
        } else if (esPeliculasMasVistas) {
            configurarLayoutCU15();
            configurarColumnas(new String[]{"Ranking", "Película", "Entradas vendidas"});
            btnGenerar.setText("GENERAR CONSULTA");
            graficoPanel.setDatos(new LinkedHashMap<>(), "Películas más vistas");
            mostrarMensaje("Seleccione el periodo y genere la consulta.", GRIS);
            prepararCardsPeliculasMasVistas();
        }

        modelo.setRowCount(0);
        actualizarBotonExportar();
        SwingUtilities.invokeLater(this::ajustarFiltrosReportes);
    }


    private void setVisibleFecha(boolean visible) {
        lblInicioFiltro.setVisible(visible);
        lblFinFiltro.setVisible(visible);
        txtInicio.setVisible(visible);
        txtFin.setVisible(visible);
        btnCalInicio.setVisible(visible);
        btnCalFin.setVisible(visible);
    }

    private void setVisibleSalaMetodo(boolean visible) {
        lblSalaFiltro.setVisible(visible);
        lblMetodoFiltro.setVisible(visible);
        cbSala.setVisible(visible);
        cbMetodo.setVisible(visible);
    }

    private void setVisiblePelicula(boolean visible) {
        lblPeliculaFiltro.setVisible(visible);
        cbPelicula.setVisible(visible);
    }

    private void configurarLayoutCU12() {
        lblInicioFiltro.setBounds(305, 18, 120, 22);
        txtInicio.setBounds(305, 43, 112, 38);
        btnCalInicio.setBounds(420, 43, 38, 38);
        lblFinFiltro.setBounds(475, 18, 120, 22);
        txtFin.setBounds(475, 43, 112, 38);
        btnCalFin.setBounds(590, 43, 38, 38);
        lblSalaFiltro.setBounds(650, 18, 120, 22);
        cbSala.setBounds(650, 43, 125, 38);
        lblMetodoFiltro.setBounds(795, 18, 120, 22);
        cbMetodo.setBounds(795, 43, 145, 38);
        lblEstado.setBounds(250, 92, 500, 25);
        btnGenerar.setBounds(760, 85, 180, 38);
    }

    private void configurarLayoutCU13() {
        lblPeliculaFiltro.setBounds(305, 18, 120, 22);
        cbPelicula.setBounds(305, 43, 360, 38);
        lblEstado.setBounds(280, 92, 500, 25);
        btnGenerar.setBounds(700, 43, 190, 38);
    }

    private void configurarLayoutCU15() {
        lblInicioFiltro.setBounds(305, 18, 120, 22);
        txtInicio.setBounds(305, 43, 112, 38);
        btnCalInicio.setBounds(420, 43, 38, 38);
        lblFinFiltro.setBounds(475, 18, 120, 22);
        txtFin.setBounds(475, 43, 112, 38);
        btnCalFin.setBounds(590, 43, 38, 38);
        lblEstado.setBounds(280, 92, 500, 25);
        btnGenerar.setBounds(650, 43, 190, 38);
    }

    private void configurarColumnas(String[] columnas) {
        modelo.setColumnIdentifiers(columnas);
        configurarTabla(tablaResultados);
    }

    private void prepararCardsReporteVentas() {
        lblCard1Titulo.setText("Ventas netas");
        lblCard1Valor.setText("S/ 0.00");
        lblCard2Titulo.setText("Entradas vigentes");
        lblCard2Valor.setText("0");
        lblCard3Titulo.setText("Monto reembolsado");
        lblCard3Valor.setText("S/ 0.00");
        lblCard4Titulo.setText("Entradas reembolsadas");
        lblCard4Valor.setText("0");
    }

    private void prepararCardsIngresosPelicula() {
        lblCard1Titulo.setText("Película");
        lblCard1Valor.setText("-");
        lblCard2Titulo.setText("Ingresos generados");
        lblCard2Valor.setText("S/ 0.00");
        lblCard3Titulo.setText("Entradas vendidas");
        lblCard3Valor.setText("0");
        lblCard4Titulo.setText("Funciones programadas");
        lblCard4Valor.setText("0");
    }

    private void prepararCardsPeliculasMasVistas() {
        lblCard1Titulo.setText("Películas listadas");
        lblCard1Valor.setText("0");
        lblCard2Titulo.setText("Entradas vendidas");
        lblCard2Valor.setText("0");
        lblCard3Titulo.setText("Ingresos asociados");
        lblCard3Valor.setText("S/ 0.00");
        lblCard4Titulo.setText("Más vista");
        lblCard4Valor.setText("-");
    }

    private boolean existenVentasRegistradas() {
        String sql = "SELECT 1 FROM ventas LIMIT 1";
        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo validar la precondición de ventas registradas.", e);
        }
    }

    private void cargarSalas() {
        cbSala.removeAllItems();
        cbSala.addItem("Todas");
        String sql = "SELECT nombre FROM salas ORDER BY nombre";
        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) cbSala.addItem(rs.getString("nombre"));
        } catch (SQLException e) {
            // Mantiene Todas.
        }
    }

    private void cargarPeliculas() {
        cbPelicula.removeAllItems();
        cbPelicula.addItem("Seleccione una película");
        for (String pelicula : controlProcesarConsulta.listarPeliculas()) {
            cbPelicula.addItem(pelicula);
        }
    }

    private LocalDate parsearFecha(String texto) {
        return LocalDate.parse(texto.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private void mostrarMensaje(String mensaje, Color color) {
        lblEstado.setText(mensaje);
        lblEstado.setForeground(color);
        lblEstado.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private String recortar(String texto, int max) {
        if (texto == null) return "-";
        return texto.length() <= max ? texto : texto.substring(0, max - 3) + "...";
    }


    private JButton crearBotonExcel() {
        JButton boton = new JButton("EXPORTAR EXCEL");
        boton.setPreferredSize(new Dimension(165, 32));
        boton.setBackground(new Color(34, 145, 80));
        boton.setForeground(BLANCO);
        boton.setFont(new Font("Arial", Font.BOLD, 12));
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setToolTipText("Exportar los resultados visibles a Excel");
        return boton;
    }

    private void actualizarBotonExportar() {
        if (btnExportarExcel != null && modelo != null) {
            btnExportarExcel.setEnabled(modelo.getRowCount() > 0);
        }
    }

    private void exportarExcel() {
        if (modelo == null || modelo.getRowCount() == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "No hay datos para exportar.",
                    "Exportar Excel",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar reporte en Excel");
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo CSV compatible con Excel (*.csv)", "csv"));
        chooser.setSelectedFile(new File(nombreArchivoReporte()));

        int opcion = chooser.showSaveDialog(this);
        if (opcion != JFileChooser.APPROVE_OPTION) return;

        File archivo = chooser.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".csv")) {
            archivo = new File(archivo.getAbsolutePath() + ".csv");
        }

        if (archivo.exists()) {
            int confirmar = JOptionPane.showConfirmDialog(
                    this,
                    "El archivo ya existe. ¿Deseas reemplazarlo?",
                    "Confirmar reemplazo",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirmar != JOptionPane.YES_OPTION) return;
        }

        try {
            escribirReporteCsv(archivo);
            JOptionPane.showMessageDialog(
                    this,
                    "Reporte exportado correctamente.\n" + archivo.getAbsolutePath(),
                    "Exportar Excel",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo exportar el reporte.\n" + e.getMessage(),
                    "Exportar Excel",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String nombreArchivoReporte() {
        String tipo = String.valueOf(cbTipoReporte.getSelectedItem())
                .toLowerCase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ñ", "n")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "reporte_cinex_" + tipo + "_" + fecha + ".csv";
    }

    private void escribirReporteCsv(File archivo) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(archivo), StandardCharsets.UTF_8))) {

            writer.write('\ufeff');
            escribirLineaCsv(writer, "CINEX - Reportes");
            escribirLineaCsv(writer, "Tipo de reporte", String.valueOf(cbTipoReporte.getSelectedItem()));

            if (txtInicio.isVisible() && txtFin.isVisible()) {
                escribirLineaCsv(writer, "Fecha inicio", txtInicio.getText());
                escribirLineaCsv(writer, "Fecha fin", txtFin.getText());
            }
            if (cbSala.isVisible()) {
                escribirLineaCsv(writer, "Sala", String.valueOf(cbSala.getSelectedItem()));
            }
            if (cbMetodo.isVisible()) {
                escribirLineaCsv(writer, "Método", String.valueOf(cbMetodo.getSelectedItem()));
            }
            if (cbPelicula.isVisible()) {
                escribirLineaCsv(writer, "Película", String.valueOf(cbPelicula.getSelectedItem()));
            }

            writer.newLine();
            escribirLineaCsv(writer, "Indicador", "Valor");
            escribirLineaCsv(writer, lblCard1Titulo.getText(), lblCard1Valor.getText());
            escribirLineaCsv(writer, lblCard2Titulo.getText(), lblCard2Valor.getText());
            escribirLineaCsv(writer, lblCard3Titulo.getText(), lblCard3Valor.getText());
            escribirLineaCsv(writer, lblCard4Titulo.getText(), lblCard4Valor.getText());

            writer.newLine();
            ArrayList<String> cabeceras = new ArrayList<>();
            for (int col = 0; col < modelo.getColumnCount(); col++) {
                cabeceras.add(modelo.getColumnName(col));
            }
            escribirLineaCsv(writer, cabeceras.toArray(new String[0]));

            for (int fila = 0; fila < modelo.getRowCount(); fila++) {
                ArrayList<String> valores = new ArrayList<>();
                for (int col = 0; col < modelo.getColumnCount(); col++) {
                    Object valor = modelo.getValueAt(fila, col);
                    valores.add(valor == null ? "" : String.valueOf(valor));
                }
                escribirLineaCsv(writer, valores.toArray(new String[0]));
            }
        }
    }

    private void escribirLineaCsv(BufferedWriter writer, String... valores) throws IOException {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) writer.write(";");
            writer.write(escaparCsv(valores[i]));
        }
        writer.newLine();
    }

    private String escaparCsv(String valor) {
        if (valor == null) return "";
        String limpio = valor.replace("\r", " ").replace("\n", " ");
        if (limpio.contains(";") || limpio.contains("\"") || limpio.contains(",")) {
            limpio = limpio.replace("\"", "\"\"");
            return "\"" + limpio + "\"";
        }
        return limpio;
    }

    private JPanel crearCard(int x, int y, int w, int h) {
        PanelRedondeado card = new PanelRedondeado();
        card.setLayout(null);
        card.setBounds(x, y, w, h);
        return card;
    }

    private JLabel crearCardTitulo(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(GRIS);
        lbl.setFont(new Font("Arial", Font.BOLD, 13));
        lbl.setBounds(16, 10, 205, 22);
        return lbl;
    }

    private JLabel crearCardValor(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(AMARILLO);
        lbl.setFont(new Font("Arial", Font.BOLD, 22));
        lbl.setBounds(16, 38, 205, 32);
        return lbl;
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

    private JComboBox<String> crearCombo(String[] opciones) {
        JComboBox<String> combo = new JComboBox<String>(opciones) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                g2.setColor(AZUL_PANEL_2);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 4, 4);
                g2.setColor(AZUL_BORDE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 4, 4);

                Object seleccionado = getSelectedItem();
                String texto = seleccionado == null ? "" : seleccionado.toString();

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int anchoDisponible = Math.max(20, w - 55);
                if (fm.stringWidth(texto) > anchoDisponible) {
                    while (texto.length() > 3 && fm.stringWidth(texto + "...") > anchoDisponible) {
                        texto = texto.substring(0, texto.length() - 1);
                    }
                    texto = texto + "...";
                }

                int y = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(BLANCO);
                g2.drawString(texto, 12, y);
                g2.dispose();
            }
        };

        combo.setBackground(AZUL_PANEL_2);
        combo.setForeground(BLANCO);
        combo.setFont(new Font("Arial", Font.BOLD, 13));
        combo.setFocusable(false);
        combo.setRequestFocusEnabled(false);
        combo.setOpaque(false);
        combo.setBorder(new LineBorder(AZUL_BORDE, 1, true));

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setOpaque(true);
                lbl.setFont(new Font("Arial", Font.BOLD, 13));
                lbl.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                lbl.setBackground(isSelected ? new Color(15, 55, 100) : AZUL_PANEL_2);
                lbl.setForeground(BLANCO);
                list.setBackground(AZUL_PANEL_2);
                list.setForeground(BLANCO);
                list.setSelectionBackground(new Color(15, 55, 100));
                list.setSelectionForeground(BLANCO);
                return lbl;
            }
        });

        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                return crearBotonFlechaCombo();
            }
        });

        return combo;
    }

    private JButton crearBotonFlechaCombo() {
        JButton boton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                g2.setColor(AZUL_PANEL_2);
                g2.fillRect(0, 0, w, h);
                g2.setColor(AZUL_BORDE);
                g2.drawLine(0, 0, 0, h);

                int cx = w / 2;
                int cy = h / 2 + 2;
                Path2D flecha = new Path2D.Double();
                flecha.moveTo(cx - 8, cy - 5);
                flecha.lineTo(cx + 8, cy - 5);
                flecha.lineTo(cx, cy + 5);
                flecha.closePath();

                g2.setColor(BLANCO);
                g2.fill(flecha);
                g2.dispose();
            }
        };

        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setContentAreaFilled(false);
        boton.setOpaque(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return boton;
    }

    private JButton crearBotonCalendario() {
        JButton boton = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(AZUL_PANEL_2);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 6, 6);
                g2.setColor(AZUL_BORDE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);

                int x = (w - 18) / 2;
                int y = (h - 20) / 2;
                g2.setColor(BLANCO);
                g2.drawRoundRect(x, y + 2, 18, 18, 4, 4);
                g2.setColor(AMARILLO);
                g2.fillRoundRect(x + 1, y + 3, 17, 6, 3, 3);
                g2.setColor(BLANCO);
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
            fechaBase = parsearFecha(campoFecha.getText());
        } catch (Exception e) {
            fechaBase = LocalDate.now();
        }

        JDialog dialogo = new JDialog(this, "Seleccionar fecha", true);
        dialogo.setResizable(false);
        dialogo.setSize(330, 340);
        dialogo.setLocationRelativeTo(campoFecha);

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
            String nombreMes = mesActual[0].getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
            lblMes.setText(nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1) + " " + mesActual[0].getYear());

            for (String diaSemana : new String[]{"L", "M", "M", "J", "V", "S", "D"}) {
                JLabel lblDia = new JLabel(diaSemana, SwingConstants.CENTER);
                lblDia.setForeground(AMARILLO);
                lblDia.setFont(new Font("Arial", Font.BOLD, 12));
                panelDias.add(lblDia);
            }

            int espacios = mesActual[0].atDay(1).getDayOfWeek().getValue() - 1;
            for (int i = 0; i < espacios; i++) {
                JLabel vacio = new JLabel("");
                vacio.setOpaque(false);
                panelDias.add(vacio);
            }

            for (int dia = 1; dia <= mesActual[0].lengthOfMonth(); dia++) {
                LocalDate fecha = mesActual[0].atDay(dia);
                JButton btnDia = crearBotonDia(String.valueOf(dia), fecha.equals(LocalDate.now()));
                btnDia.addActionListener(e -> {
                    campoFecha.setText(fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    dialogo.dispose();
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

    private JButton crearBotonMes(String texto) {
        JButton boton = new JButton(texto);
        boton.setBackground(AMARILLO);
        boton.setForeground(AZUL_FONDO_1);
        boton.setFont(new Font("Arial", Font.BOLD, 12));
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setPreferredSize(new Dimension(45, 30));
        return boton;
    }

    private JButton crearBotonDia(String texto, boolean hoy) {
        JButton boton = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color fondo = hoy ? AMARILLO : new Color(8, 38, 82);
                Color borde = hoy ? AMARILLO : AZUL_BORDE;
                Color textoColor = hoy ? Color.BLACK : BLANCO;

                if (getModel().isPressed()) {
                    fondo = new Color(245, 196, 0);
                    textoColor = Color.BLACK;
                } else if (getModel().isRollover() && !hoy) {
                    fondo = new Color(16, 55, 105);
                }

                g2.setColor(fondo);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 7, 7);

                g2.setColor(borde);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 7, 7);

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

                g2.setColor(textoColor);
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };

        boton.setFont(new Font("Arial", Font.BOLD, 12));
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setContentAreaFilled(false);
        boton.setOpaque(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setMargin(new Insets(0, 0, 0, 0));
        return boton;
    }

    private void configurarTabla(JTable tabla) {
        tabla.setBackground(AZUL_PANEL);
        tabla.setForeground(BLANCO);
        tabla.setFont(new Font("Arial", Font.PLAIN, 13));
        tabla.setRowHeight(30);
        tabla.setShowGrid(true);
        tabla.setGridColor(AZUL_BORDE);
        tabla.setSelectionBackground(new Color(15, 55, 100));
        tabla.setSelectionForeground(BLANCO);
        tabla.setFillsViewportHeight(true);
        tabla.setOpaque(true);

        JTableHeader header = tabla.getTableHeader();
        header.setBackground(new Color(6, 31, 70));
        header.setForeground(BLANCO);
        header.setFont(new Font("Arial", Font.BOLD, 13));
        header.setBorder(new LineBorder(AZUL_BORDE, 1));
        header.setOpaque(true);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 34));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setOpaque(true);
                lbl.setBackground(new Color(6, 31, 70));
                lbl.setForeground(BLANCO);
                lbl.setFont(new Font("Arial", Font.BOLD, 13));
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setBorder(new LineBorder(AZUL_BORDE, 1));
                return lbl;
            }
        };
        header.setDefaultRenderer(headerRenderer);

        DefaultTableCellRenderer render = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setOpaque(true);
                lbl.setFont(new Font("Arial", Font.PLAIN, 13));
                lbl.setForeground(BLANCO);
                lbl.setBackground(isSelected ? new Color(15, 55, 100) : AZUL_PANEL);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return lbl;
            }
        };

        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(render);
            tabla.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
    }

    private void configurarScrollTabla(JScrollPane scroll) {
        scroll.setBorder(null);
        scroll.setBackground(AZUL_PANEL);
        scroll.getViewport().setBackground(AZUL_PANEL);
        scroll.getViewport().setOpaque(true);
        scroll.setColumnHeaderView(tablaResultados.getTableHeader());
        if (scroll.getColumnHeader() != null) {
            scroll.getColumnHeader().setBackground(new Color(6, 31, 70));
            scroll.getColumnHeader().setOpaque(true);
        }
        scroll.getVerticalScrollBar().setBackground(AZUL_PANEL);
        scroll.getHorizontalScrollBar().setBackground(AZUL_PANEL);
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
            return new ImageIcon();
        }
    }

    private void actualizarFechaHora() {
        LocalDateTime ahora = LocalDateTime.now();
        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String horaTexto = ahora.format(formatoHora).replace("AM", "a. m.").replace("PM", "p. m.");
        lblHora.setText(horaTexto);
        lblFecha.setText(ahora.format(formatoFecha));
    }

    class FondoPanel extends JPanel {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            GradientPaint fondo = new GradientPaint(0, 0, AZUL_FONDO_1, getWidth(), getHeight(), AZUL_FONDO_2);
            g2.setPaint(fondo);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 255, 255, 4));
            g2.fillOval(1220, 120, 250, 250);
            g2.dispose();
        }
    }

    class SidebarPanel extends JPanel {
        public SidebarPanel() { setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
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
            if ("\u21A9".equals(emoji) || "↩".equals(emoji) || "≡".equals(emoji)) {
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
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
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
        public PanelRedondeado() { setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
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

    class GraficoBarrasPanel extends JPanel {
        private LinkedHashMap<String, Double> datos = new LinkedHashMap<>();
        private String titulo = "Resultados";

        public GraficoBarrasPanel() { setOpaque(false); }

        public void setDatos(Map<String, Double> nuevosDatos, String titulo) {
            this.datos = new LinkedHashMap<>();
            if (nuevosDatos != null) this.datos.putAll(nuevosDatos);
            this.titulo = titulo == null ? "Resultados" : titulo;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(AZUL_PANEL);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.setColor(AZUL_BORDE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

            g2.setColor(BLANCO);
            g2.setFont(new Font("Arial", Font.BOLD, 15));
            g2.drawString(titulo, 18, 28);

            if (datos.isEmpty()) {
                g2.setColor(GRIS);
                g2.setFont(new Font("Arial", Font.BOLD, 14));
                g2.drawString("Sin datos para mostrar", 115, 130);
                g2.dispose();
                return;
            }

            double max = 1.0;
            for (double valor : datos.values()) max = Math.max(max, valor);

            int y = 58;
            int contador = 0;
            for (Map.Entry<String, Double> item : datos.entrySet()) {
                if (contador >= 5) break;
                String nombre = recortar(item.getKey(), 24);
                double valor = item.getValue();
                int ancho = (int) Math.max(6, (valor / max) * 190);

                g2.setColor(BLANCO);
                g2.setFont(new Font("Arial", Font.PLAIN, 12));
                g2.drawString(nombre, 18, y);

                g2.setColor(new Color(20, 55, 95));
                g2.fillRoundRect(18, y + 8, 230, 10, 8, 8);
                g2.setColor(AMARILLO);
                g2.fillRoundRect(18, y + 8, ancho, 10, 8, 8);

                g2.setColor(GRIS);
                g2.drawString(String.format("%.0f", valor), 260, y + 18);
                y += 37;
                contador++;
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ReportesGerenteCINEXGUI("gerente01").setVisible(true));
    }
}
