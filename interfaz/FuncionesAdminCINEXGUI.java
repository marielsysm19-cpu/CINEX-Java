package interfaz;

import control.ControlGestionarFuncionesCINEX;
import control.ControlGestionarFuncionesCINEX.ResultadoModificacion;
import control.ControlNotificacionesCINEX;
import control.ControlProgramaFuncionCINEX;
import control.ControlProgramaFuncionCINEX.ResultadoProgramacion;
import control.ControlProgramaFuncionCINEX.SalaItem;
import control.ControlProgramaFuncionCINEX.TipoResultado;
import entidad.PeliculaCINEX;
import entidad.FuncionCINEX;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Interfaz del CU-10: Gestionar programación de funciones.
 *
 * Flujo implementado:
 * 1. El Administrador accede a la programación de funciones.
 * 2. El sistema muestra el formulario correspondiente.
 * 3. El Administrador selecciona película/sala y registra fecha/horario.
 * 4. El sistema valida la información y detecta cruces de horario.
 * 5. El Administrador confirma el registro.
 * 6. El sistema guarda la programación exitosamente.
 *
 * Flujo alterno 4.1:
 * - Si existe cruce, se informa el conflicto y no se registra la función.
 */
public class FuncionesAdminCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_PANEL = new Color(5, 18, 43);
    private final Color AZUL_PANEL_2 = new Color(8, 28, 65);
    private final Color AZUL_TABLA = new Color(7, 28, 65);
    private final Color AZUL_TABLA_ALTERNA = new Color(9, 34, 76);
    private final Color AZUL_HEADER = new Color(12, 43, 91);
    private final Color AZUL_BORDE = new Color(63, 96, 145);
    private final Color AZUL_BOTON = new Color(0, 80, 160);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 247, 252);
    private final Color GRIS = new Color(185, 195, 210);
    private final Color VERDE = new Color(35, 180, 85);
    private final Color ROJO = new Color(225, 70, 70);

    private final String usuarioActual;
    private final ControlGestionarFuncionesCINEX controlFunciones;
    private final ControlProgramaFuncionCINEX controlProgramacion;
    private final ControlNotificacionesCINEX controlNotificaciones =
            new ControlNotificacionesCINEX();

    private JTable tabla;
    private DefaultTableModel modelo;
    private TableRowSorter<DefaultTableModel> sorter;

    private JComboBox<PeliculaCINEX> cbPelicula;
    private JComboBox<SalaItem> cbSala;
    private JSpinner spFecha;
    private JSpinner spHora;
    private JTextField txtBuscar;

    private JLabel lblMensaje;
    private JLabel lblTituloValidacion;
    private JLabel lblDetalleValidacion;
    private JLabel lblPeliculaResumen;
    private JLabel lblSalaResumen;
    private JLabel lblFechaResumen;
    private JLabel lblDuracionResumen;
    private JLabel lblTotalFunciones;
    private JLabel lblActivas;
    private JLabel lblCanceladas;
    private JLabel lblFinalizadas;
    private JLabel lblHoraSistema;
    private JLabel lblFechaSistema;

    private JButton btnProgramar;
    private JButton btnCancelarFuncion;
    private JButton btnModificarFuncion;

    private boolean cargandoCatalogos;

    private final Map<Integer, Integer> duracionPorFuncion =
            new HashMap<>();

    private int totalRegistradasBD;
    private int totalActivasBD;
    private int totalFinalizadasBD;
    private int totalCanceladasBD;

    public FuncionesAdminCINEXGUI() {
        this("admin");
    }

    public FuncionesAdminCINEXGUI(String usuario) {
        this.usuarioActual = usuario == null || usuario.trim().isEmpty()
                ? "admin"
                : usuario.trim();
        this.controlFunciones = new ControlGestionarFuncionesCINEX();
        this.controlProgramacion = new ControlProgramaFuncionCINEX();

        configurarVentana();
        construirInterfaz();
        configurarEventos();
        iniciarReloj();
        establecerFechaHoraInicial();
        cargarTodo();
    }

    private void configurarVentana() {
        setTitle("CINEX - Programar funciones");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1380, 820);
        setMinimumSize(new Dimension(1180, 720));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void construirInterfaz() {
        FondoPanel fondo = new FondoPanel();
        fondo.setLayout(new BorderLayout());
        setContentPane(fondo);

        fondo.add(crearHeader(), BorderLayout.NORTH);
        fondo.add(crearContenido(), BorderLayout.CENTER);
        fondo.add(crearFooter(), BorderLayout.SOUTH);
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout(30, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(14, 32, 10, 32));

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        izquierda.setOpaque(false);

        JLabel logo = new JLabel();
        ImageIcon icono = cargarImagen("imagenes/logocinex.png", 184, 68);
        if (icono.getIconWidth() > 0) {
            logo.setIcon(icono);
            izquierda.add(logo);
        }

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Programar funciones");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 31));

        JLabel subtitulo = new JLabel(
                "Registre una película, sala, fecha y horario considerando 30 minutos de limpieza."
        );
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 15));

        textos.add(Box.createVerticalStrut(5));
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(5));
        textos.add(subtitulo);
        izquierda.add(textos);

        JPanel info = new JPanel(new FlowLayout(FlowLayout.RIGHT, 24, 8));
        info.setOpaque(false);

        JLabel lblUsuario = crearTextoHeader("Administrador: " + usuarioActual);
        lblHoraSistema = crearTextoHeader("");
        lblFechaSistema = crearTextoHeader("");

        info.add(lblUsuario);
        info.add(lblHoraSistema);
        info.add(lblFechaSistema);

        header.add(izquierda, BorderLayout.WEST);
        header.add(info, BorderLayout.EAST);
        return header;
    }

    private JPanel crearContenido() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 14));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(4, 32, 12, 32));

        wrapper.add(crearEstadisticas(), BorderLayout.NORTH);

        JPanel superior = new JPanel(new BorderLayout(20, 0));
        superior.setOpaque(false);
        superior.setPreferredSize(new Dimension(0, 345));
        superior.add(crearFormulario(), BorderLayout.CENTER);
        superior.add(crearPanelValidacion(), BorderLayout.EAST);

        JPanel centro = new JPanel(new BorderLayout(0, 16));
        centro.setOpaque(false);
        centro.add(superior, BorderLayout.NORTH);
        centro.add(crearPanelTabla(), BorderLayout.CENTER);

        wrapper.add(centro, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel crearEstadisticas() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 14, 0));
        panel.setOpaque(false);

        lblTotalFunciones = new JLabel("0");
        lblActivas = new JLabel("0");
        lblFinalizadas = new JLabel("0");
        lblCanceladas = new JLabel("0");

        panel.add(crearTarjetaEstadistica(
                "FUNCIONES REGISTRADAS",
                lblTotalFunciones,
                "Total de programaciones del cine"
        ));
        panel.add(crearTarjetaEstadistica(
                "FUNCIONES ACTIVAS",
                lblActivas,
                "Disponibles para la venta de entradas"
        ));
        panel.add(crearTarjetaEstadistica(
                "FUNCIONES FINALIZADAS",
                lblFinalizadas,
                "Funciones cuya fecha y hora ya pasaron"
        ));
        panel.add(crearTarjetaEstadistica(
                "FUNCIONES CANCELADAS",
                lblCanceladas,
                "Programaciones que fueron anuladas"
        ));

        return panel;
    }

    private JPanel crearTarjetaEstadistica(String titulo, JLabel valor, String detalle) {
        RoundedPanel tarjeta = new RoundedPanel(18, new Color(7, 24, 56, 225));
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1, true),
                new EmptyBorder(11, 18, 11, 18)
        ));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setForeground(GRIS);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 12));
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        valor.setForeground(AMARILLO);
        valor.setFont(new Font("Arial", Font.BOLD, 24));
        valor.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDetalle = new JLabel(detalle);
        lblDetalle.setForeground(new Color(150, 165, 190));
        lblDetalle.setFont(new Font("Arial", Font.PLAIN, 12));
        lblDetalle.setAlignmentX(Component.LEFT_ALIGNMENT);

        tarjeta.add(lblTitulo);
        tarjeta.add(Box.createVerticalStrut(2));
        tarjeta.add(valor);
        tarjeta.add(Box.createVerticalStrut(1));
        tarjeta.add(lblDetalle);
        return tarjeta;
    }

    private JPanel crearFormulario() {
        RoundedPanel panel = new RoundedPanel(18, new Color(5, 18, 43, 235));
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1, true),
                new EmptyBorder(18, 22, 18, 22)
        ));

        JPanel cabecera = new JPanel();
        cabecera.setOpaque(false);
        cabecera.setLayout(new BoxLayout(cabecera, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Programar funciones");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 22));

        JLabel descripcion = new JLabel(
                "Seleccione la película y la sala; luego registre la fecha y el horario."
        );
        descripcion.setForeground(GRIS);
        descripcion.setFont(new Font("Arial", Font.PLAIN, 13));

        cabecera.add(titulo);
        cabecera.add(Box.createVerticalStrut(4));
        cabecera.add(descripcion);

        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(9, 0, 9, 18);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        cbPelicula = crearCombo();
        cbSala = crearCombo();
        spFecha = crearSpinnerFecha();
        spHora = crearSpinnerHora();

        agregarCampo(campos, "Película registrada *", cbPelicula, 0, 0, gbc);
        agregarCampo(campos, "Sala *", cbSala, 1, 0, gbc);
        agregarCampo(campos, "Fecha de función *", spFecha, 0, 1, gbc);
        agregarCampo(campos, "Horario de inicio *", spHora, 1, 1, gbc);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        acciones.setOpaque(false);

        btnProgramar = crearBoton(
                "PROGRAMAR FUNCIÓN",
                AMARILLO,
                Color.BLACK,
                44
        );

        btnProgramar.setPreferredSize(
                new Dimension(210, 44)
        );

        acciones.add(btnProgramar);

        panel.add(cabecera, BorderLayout.NORTH);
        panel.add(campos, BorderLayout.CENTER);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelValidacion() {
        RoundedPanel panel = new RoundedPanel(18, new Color(7, 24, 56, 235));
        panel.setPreferredSize(new Dimension(390, 0));
        panel.setLayout(new BorderLayout(0, 13));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1, true),
                new EmptyBorder(18, 20, 18, 20)
        ));

        JPanel encabezado = new JPanel();
        encabezado.setOpaque(false);
        encabezado.setLayout(new BoxLayout(encabezado, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Validación de la programación");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 19));

        JLabel descripcion = new JLabel(
                "El sistema comprobará la película, la fecha y la disponibilidad de la sala."
        );
        descripcion.setForeground(GRIS);
        descripcion.setFont(new Font("Arial", Font.PLAIN, 12));

        encabezado.add(titulo);
        encabezado.add(Box.createVerticalStrut(5));
        encabezado.add(descripcion);

        RoundedPanel estado = new RoundedPanel(14, new Color(8, 28, 65));
        estado.setLayout(new BoxLayout(estado, BoxLayout.Y_AXIS));
        estado.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1, true),
                new EmptyBorder(13, 14, 13, 14)
        ));

        lblTituloValidacion = new JLabel("Formulario listo");
        lblTituloValidacion.setForeground(AMARILLO);
        lblTituloValidacion.setFont(new Font("Arial", Font.BOLD, 14));

        lblDetalleValidacion = new JLabel(
                "<html>Seleccione los datos para programar una función.</html>"
        );
        lblDetalleValidacion.setForeground(BLANCO);
        lblDetalleValidacion.setFont(new Font("Arial", Font.PLAIN, 12));

        estado.add(lblTituloValidacion);
        estado.add(Box.createVerticalStrut(6));
        estado.add(lblDetalleValidacion);

        JPanel resumen = new JPanel(new GridLayout(4, 1, 0, 5));
        resumen.setOpaque(false);

        lblPeliculaResumen = crearLineaResumen("Película: —");
        lblSalaResumen = crearLineaResumen("Sala: —");
        lblFechaResumen = crearLineaResumen("Fecha y hora: —");
        lblDuracionResumen = crearLineaResumen("Duración estimada: —");

        resumen.add(lblPeliculaResumen);
        resumen.add(lblSalaResumen);
        resumen.add(lblFechaResumen);
        resumen.add(lblDuracionResumen);

        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(estado, BorderLayout.CENTER);
        panel.add(resumen, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelTabla() {
        RoundedPanel panel = new RoundedPanel(18, new Color(5, 18, 43, 235));
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1, true),
                new EmptyBorder(15, 18, 15, 18)
        ));

        JPanel cabecera = new JPanel(new BorderLayout(20, 0));
        cabecera.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Listar funciones programadas");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 20));

        JLabel descripcion = new JLabel(
                "Seleccione una función activa para modificarla o cancelarla cuando sea necesario."
        );
        descripcion.setForeground(GRIS);
        descripcion.setFont(new Font("Arial", Font.PLAIN, 12));

        textos.add(titulo);
        textos.add(Box.createVerticalStrut(3));
        textos.add(descripcion);

        txtBuscar = crearCampoTexto();
        txtBuscar.setPreferredSize(new Dimension(315, 39));
        txtBuscar.setToolTipText("Buscar por película, sala, fecha, hora o estado");

        JPanel buscador = new JPanel(new BorderLayout(8, 0));
        buscador.setOpaque(false);
        JLabel lblBuscar = new JLabel("Buscar funciones programadas:");
        lblBuscar.setForeground(GRIS);
        lblBuscar.setFont(new Font("Arial", Font.BOLD, 13));
        buscador.add(lblBuscar, BorderLayout.WEST);
        buscador.add(txtBuscar, BorderLayout.CENTER);

        cabecera.add(textos, BorderLayout.WEST);
        cabecera.add(buscador, BorderLayout.EAST);

        modelo = new DefaultTableModel(
                new String[]{"ID", "Película", "Sala / Tipo", "Fecha", "Hora", "Estado"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Integer.class : String.class;
            }
        };

        tabla = new JTable(modelo);
        estilizarTabla();
        sorter = new TableRowSorter<>(modelo);
        tabla.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        scroll.setOpaque(true);
        scroll.setBackground(AZUL_TABLA);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(AZUL_TABLA);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setOpaque(true);
        scroll.getVerticalScrollBar().setBackground(AZUL_TABLA);
        scroll.getVerticalScrollBar().setUI(
                new ScrollBarFuncionesUI()
        );

        JPanel esquinaScroll = new JPanel();
        esquinaScroll.setBackground(AZUL_HEADER);
        scroll.setCorner(
                JScrollPane.UPPER_RIGHT_CORNER,
                esquinaScroll
        );

        lblMensaje = new JLabel("Cargando funciones...");
        lblMensaje.setForeground(GRIS);
        lblMensaje.setFont(new Font("Arial", Font.BOLD, 12));

        JLabel ayuda = new JLabel("Seleccione una fila para habilitar la modificación o cancelación.");
        ayuda.setForeground(new Color(145, 160, 185));
        ayuda.setFont(new Font("Arial", Font.PLAIN, 12));

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(lblMensaje, BorderLayout.WEST);
        pie.add(ayuda, BorderLayout.EAST);

        panel.add(cabecera, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(pie, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(0, 32, 20, 32));

        JLabel nota = new JLabel(
                "Cada función reserva la sala durante la película y 30 minutos adicionales para limpieza."
        );
        nota.setForeground(new Color(145, 160, 185));
        nota.setFont(new Font("Arial", Font.PLAIN, 12));

        btnModificarFuncion = crearBoton(
                "MODIFICAR FUNCIÓN",
                AMARILLO,
                Color.BLACK,
                46
        );
        btnCancelarFuncion = crearBoton(
                "CANCELAR FUNCIÓN",
                new Color(150, 48, 55),
                BLANCO,
                46
        );
        JButton btnMenu = crearBoton("MENÚ PRINCIPAL", AZUL_BOTON, BLANCO, 46);

        btnModificarFuncion.setPreferredSize(new Dimension(205, 46));
        btnCancelarFuncion.setPreferredSize(new Dimension(195, 46));
        btnMenu.setPreferredSize(new Dimension(185, 46));
        btnModificarFuncion.setEnabled(false);
        btnCancelarFuncion.setEnabled(false);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        acciones.setOpaque(false);
        acciones.add(btnModificarFuncion);
        acciones.add(btnCancelarFuncion);
        acciones.add(btnMenu);

        btnMenu.addActionListener(e -> volverMenu());

        footer.add(nota, BorderLayout.WEST);
        footer.add(acciones, BorderLayout.EAST);
        return footer;
    }

    private void configurarEventos() {
        cbPelicula.addActionListener(e -> actualizarResumen());
        cbSala.addActionListener(e -> actualizarResumen());
        spFecha.addChangeListener(e -> actualizarResumen());
        spHora.addChangeListener(e -> actualizarResumen());

        btnProgramar.addActionListener(e -> solicitarProgramacion());
        btnModificarFuncion.addActionListener(e -> modificarFuncionSeleccionada());
        btnCancelarFuncion.addActionListener(e -> cancelarFuncionSeleccionada());

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean activa = funcionSeleccionadaEstaActiva();
                btnModificarFuncion.setEnabled(activa);
                btnCancelarFuncion.setEnabled(activa);
                if (activa) {
                    cargarFuncionSeleccionadaEnFormulario();
                }
            }
        });

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.getSelectedRow() >= 0) {
                    cargarFuncionSeleccionadaEnFormulario();
                }
            }
        });


        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrarTabla();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrarTabla();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrarTabla();
            }
        });

        getRootPane().registerKeyboardAction(
                e -> volverMenu(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void cargarTodo() {
        cargarCatalogos();
        cargarFunciones();
    }

    private void cargarCatalogos() {
        cargandoCatalogos = true;
        bloquearFormulario(true);
        mostrarValidacion(
                "Cargando información",
                "Recuperando películas y salas registradas...",
                AMARILLO
        );

        SwingWorker<Object[], Void> worker = new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                ArrayList<PeliculaCINEX> peliculas = controlProgramacion.listarPeliculasRegistradas();
                ArrayList<SalaItem> salas = controlProgramacion.listarSalasRegistradas();
                return new Object[]{peliculas, salas};
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void done() {
                try {
                    Object[] resultado = get();
                    ArrayList<PeliculaCINEX> peliculas = (ArrayList<PeliculaCINEX>) resultado[0];
                    ArrayList<SalaItem> salas = (ArrayList<SalaItem>) resultado[1];

                    DefaultComboBoxModel<PeliculaCINEX> modeloPeliculas = new DefaultComboBoxModel<>();
                    for (PeliculaCINEX pelicula : peliculas) {
                        modeloPeliculas.addElement(pelicula);
                    }
                    cbPelicula.setModel(modeloPeliculas);

                    DefaultComboBoxModel<SalaItem> modeloSalas = new DefaultComboBoxModel<>();
                    for (SalaItem sala : salas) {
                        modeloSalas.addElement(sala);
                    }
                    cbSala.setModel(modeloSalas);

                    if (!peliculas.isEmpty()) {
                        cbPelicula.setSelectedIndex(0);
                    }
                    if (!salas.isEmpty()) {
                        cbSala.setSelectedIndex(0);
                    }

                    boolean disponibles = !peliculas.isEmpty() && !salas.isEmpty();
                    bloquearFormulario(!disponibles);

                    if (peliculas.isEmpty()) {
                        mostrarValidacion(
                                "No hay películas disponibles",
                                "Debe existir una película registrada y activa antes de programar una función.",
                                ROJO
                        );
                    } else if (salas.isEmpty()) {
                        mostrarValidacion(
                                "No hay salas disponibles",
                                "No se encontraron salas activas para la programación.",
                                ROJO
                        );
                    } else {
                        mostrarValidacion(
                                "Formulario listo",
                                "Seleccione los datos y presione PROGRAMAR FUNCIÓN.",
                                VERDE
                        );
                    }

                } catch (Exception ex) {
                    cbPelicula.setModel(new DefaultComboBoxModel<>());
                    cbSala.setModel(new DefaultComboBoxModel<>());
                    bloquearFormulario(true);
                    mostrarValidacion(
                            "No se pudieron cargar los datos",
                            "No fue posible cargar la información necesaria.",
                            ROJO
                    );

                    System.err.println(
                            "[Funciones] No se pudieron recuperar películas o salas: "
                                    + ex.getMessage()
                    );
                } finally {
                    cargandoCatalogos = false;
                    actualizarResumen();
                }
            }
        };

        worker.execute();
    }

    private void cargarFunciones() {
        lblMensaje.setText("Consultando funciones...");
        lblMensaje.setForeground(GRIS);
        btnCancelarFuncion.setEnabled(false);

        SwingWorker<ArrayList<FuncionCINEX>, Void> worker = new SwingWorker<ArrayList<FuncionCINEX>, Void>() {
            @Override
            protected ArrayList<FuncionCINEX> doInBackground() {
                return controlFunciones.listarFunciones();
            }

            @Override
            protected void done() {
                try {
                    ArrayList<FuncionCINEX> datos = get();
                    modelo.setRowCount(0);
                    duracionPorFuncion.clear();

                    totalRegistradasBD = 0;
                    totalActivasBD = 0;
                    totalFinalizadasBD = 0;
                    totalCanceladasBD = 0;

                    for (FuncionCINEX funcion : datos) {
                        Object[] normalizada =
                                normalizarFilaFuncion(funcion);

                        int idFuncion = funcion.getIdFuncion();

                        int duracion = funcion.getDuracionMinutos();

                        duracionPorFuncion.put(
                                idFuncion,
                                Math.max(1, duracion)
                        );

                        totalRegistradasBD++;

                        String estado =
                                valorSeguro(normalizada[5]);

                        if ("Cancelada".equalsIgnoreCase(
                                estado
                        )) {
                            totalCanceladasBD++;
                            continue;
                        }

                        if ("Finalizada".equalsIgnoreCase(
                                estado
                        )) {
                            totalFinalizadasBD++;
                            continue;
                        }

                        totalActivasBD++;
                        modelo.addRow(normalizada);
                    }

                    actualizarContadoresEstados();

                    int visibles = modelo.getRowCount();

                    if (visibles == 0) {
                        lblMensaje.setText(
                                "No existen funciones activas pendientes."
                        );
                        lblMensaje.setForeground(AMARILLO);
                    } else {
                        lblMensaje.setText(
                                "Funciones activas encontradas: "
                                        + visibles
                        );
                        lblMensaje.setForeground(VERDE);
                    }

                } catch (Exception ex) {
                    modelo.setRowCount(0);
                    lblTotalFunciones.setText("0");
                    lblActivas.setText("0");
                    lblFinalizadas.setText("0");
                    lblCanceladas.setText("0");
                    lblMensaje.setText("No se pudieron cargar las funciones.");
                    lblMensaje.setForeground(ROJO);
                } finally {
                    btnModificarFuncion.setEnabled(false);
                    btnCancelarFuncion.setEnabled(false);
                }
            }
        };

        worker.execute();
    }

    private Object[] normalizarFilaFuncion(FuncionCINEX funcion) {
        Object[] salida = new Object[6];
        salida[0] = funcion.getIdFuncion();
        salida[1] = funcion.getPelicula();
        salida[2] = funcion.getTipoSala().isEmpty()
                ? funcion.getSala()
                : funcion.getSala() + " - " + funcion.getTipoSala();
        salida[3] = funcion.fechaFormateada();
        salida[4] = funcion.getHora() == null
                ? "-"
                : funcion.getHora().format(DateTimeFormatter.ofPattern("HH:mm"));
        salida[5] = determinarEstadoFuncion(
                salida[3],
                salida[4],
                funcion.getDuracionMinutos(),
                funcion.getEstado()
        );
        return salida;
    }

    /**
     * Una función cancelada conserva ese estado.
     * Una función cuya fecha y hora de inicio ya pasaron
     * se muestra como Finalizada.
     */
    private String determinarEstadoFuncion(
            Object fechaValor,
            Object horaValor,
            int duracionMinutos,
            String estadoBD
    ) {
        String estado = valorSeguro(estadoBD);

        if ("Cancelada".equalsIgnoreCase(estado)) {
            return "Cancelada";
        }

        LocalDate fecha = convertirFechaFuncion(
                valorSeguro(fechaValor)
        );

        LocalTime hora = convertirHoraFuncion(
                valorSeguro(horaValor)
        );

        if (fecha == null || hora == null) {
            return estado.isEmpty() ? "Activa" : estado;
        }

        LocalDateTime finFuncion =
                LocalDateTime.of(fecha, hora)
                        .plusMinutes(
                                Math.max(1, duracionMinutos)
                        );

        if (!finFuncion.isAfter(LocalDateTime.now())) {
            return "Finalizada";
        }

        return "Activa";
    }

    private LocalDate convertirFechaFuncion(String texto) {
        String valor = valorSeguro(texto);

        String[] formatos = {
                "dd/MM/yyyy",
                "yyyy-MM-dd",
                "d/M/yyyy",
                "dd-MM-yyyy"
        };

        for (String formato : formatos) {
            try {
                return LocalDate.parse(
                        valor,
                        DateTimeFormatter.ofPattern(formato)
                );
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private LocalTime convertirHoraFuncion(String texto) {
        String valor = valorSeguro(texto)
                .toUpperCase()
                .replace(".", "")
                .trim();

        String[] formatos = {
                "hh:mm a",
                "h:mm a",
                "HH:mm",
                "HH:mm:ss",
                "H:mm"
        };

        for (String formato : formatos) {
            try {
                return LocalTime.parse(
                        valor,
                        DateTimeFormatter.ofPattern(formato)
                );
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private void actualizarEstadosTemporales() {
        if (modelo == null) {
            return;
        }

        boolean cambio = false;

        for (int fila = modelo.getRowCount() - 1;
             fila >= 0;
             fila--) {

            int idFuncion =
                    convertirEntero(
                            modelo.getValueAt(fila, 0)
                    );

            int duracion =
                    duracionPorFuncion.getOrDefault(
                            idFuncion,
                            1
                    );

            String nuevoEstado =
                    determinarEstadoFuncion(
                            modelo.getValueAt(fila, 3),
                            modelo.getValueAt(fila, 4),
                            duracion,
                            valorSeguro(
                                    modelo.getValueAt(fila, 5)
                            )
                    );

            if ("Finalizada".equalsIgnoreCase(
                    nuevoEstado
            )) {
                modelo.removeRow(fila);
                totalActivasBD =
                        Math.max(0, totalActivasBD - 1);
                totalFinalizadasBD++;
                cambio = true;
            }
        }

        actualizarContadoresEstados();

        if (cambio) {
            int visibles = modelo.getRowCount();

            lblMensaje.setText(
                    visibles == 0
                            ? "No existen funciones activas pendientes."
                            : "Funciones activas encontradas: "
                                    + visibles
            );

            lblMensaje.setForeground(
                    visibles == 0
                            ? AMARILLO
                            : VERDE
            );
        }

        boolean activa = funcionSeleccionadaEstaActiva();
        btnModificarFuncion.setEnabled(activa);
        btnCancelarFuncion.setEnabled(activa);
    }

    private void actualizarContadoresEstados() {
        lblTotalFunciones.setText(
                String.valueOf(totalRegistradasBD)
        );
        lblActivas.setText(
                String.valueOf(totalActivasBD)
        );
        lblFinalizadas.setText(
                String.valueOf(totalFinalizadasBD)
        );
        lblCanceladas.setText(
                String.valueOf(totalCanceladasBD)
        );
    }

    private boolean funcionSeleccionadaEstaActiva() {
        if (tabla == null || modelo == null) {
            return false;
        }

        int filaVista = tabla.getSelectedRow();

        if (filaVista < 0) {
            return false;
        }

        int filaModelo =
                tabla.convertRowIndexToModel(filaVista);

        String estado =
                valorSeguro(
                        modelo.getValueAt(
                                filaModelo,
                                5
                        )
                );

        return "Activa".equalsIgnoreCase(estado);
    }

    private void solicitarProgramacion() {
        PeliculaCINEX pelicula = (PeliculaCINEX) cbPelicula.getSelectedItem();
        SalaItem sala = (SalaItem) cbSala.getSelectedItem();
        LocalDate fecha = obtenerFechaSeleccionada();
        LocalTime hora = obtenerHoraSeleccionada();

        if (pelicula == null || sala == null || fecha == null || hora == null) {
            mostrarValidacion(
                    "Datos incompletos",
                    "Complete la película, sala, fecha y horario.",
                    AMARILLO
            );
            JOptionPane.showMessageDialog(
                    this,
                    "Complete todos los datos obligatorios.",
                    "Datos incompletos",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        bloquearMientrasProcesa(true);
        mostrarValidacion(
                "Validando información",
                "Comprobando la película y la disponibilidad de la sala...",
                AMARILLO
        );

        SwingWorker<ResultadoProgramacion, Void> worker = new SwingWorker<ResultadoProgramacion, Void>() {
            @Override
            protected ResultadoProgramacion doInBackground() {
                return controlProgramacion.validarProgramacion(
                        pelicula.getIdPelicula(),
                        sala.getIdSala(),
                        fecha,
                        hora
                );
            }

            @Override
            protected void done() {
                try {
                    ResultadoProgramacion validacion = get();

                    if (!validacion.isExito()) {
                        gestionarResultadoNoExitoso(validacion);
                        return;
                    }

                    String tituloValidacion =
                            validacion.getTipo()
                                    == TipoResultado.FUNCION_REACTIVADA
                                    ? "Función cancelada encontrada"
                                    : "Información válida";

                    mostrarValidacion(
                            tituloValidacion,
                            validacion.getMensaje(),
                            VERDE
                    );

                    int confirmacion = JOptionPane.showConfirmDialog(
                            FuncionesAdminCINEXGUI.this,
                            crearMensajeConfirmacion(pelicula, sala, fecha, hora),
                            "Confirmar programación",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                    );

                    if (confirmacion != JOptionPane.YES_OPTION) {
                        mostrarValidacion(
                                "Registro no confirmado",
                                "La programación no fue guardada.",
                                AMARILLO
                        );
                        return;
                    }

                    registrarFuncion(pelicula, sala, fecha, hora);

                } catch (Exception ex) {
                    mostrarValidacion(
                            "Error de validación",
                            "No se pudo validar la programación. Intente nuevamente.",
                            ROJO
                    );
                    JOptionPane.showMessageDialog(
                            FuncionesAdminCINEXGUI.this,
                            "No se pudo validar la programación de la función.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    bloquearMientrasProcesa(false);
                }
            }
        };

        worker.execute();
    }

    private void registrarFuncion(
            PeliculaCINEX pelicula,
            SalaItem sala,
            LocalDate fecha,
            LocalTime hora
    ) {
        bloquearMientrasProcesa(true);
        mostrarValidacion(
                "Guardando programación",
                "Registrando la nueva función...",
                AMARILLO
        );

        SwingWorker<ResultadoProgramacion, Void> worker = new SwingWorker<ResultadoProgramacion, Void>() {
            @Override
            protected ResultadoProgramacion doInBackground() {
                return controlProgramacion.procesarProgramacion(
                        pelicula.getIdPelicula(),
                        sala.getIdSala(),
                        fecha,
                        hora
                );
            }

            @Override
            protected void done() {
                try {
                    ResultadoProgramacion resultado = get();

                    if (!resultado.isExito()) {
                        gestionarResultadoNoExitoso(resultado);
                        return;
                    }

                    boolean fueReactivada =
                            resultado.getTipo()
                                    == TipoResultado.FUNCION_REACTIVADA;

                    if (fueReactivada
                            && resultado.getFuncion() != null) {
                        controlNotificaciones.registrarCambioFuncion(
                                resultado.getFuncion().getIdFuncion(),
                                "El administrador reactivó una función cancelada.",
                                "Estado anterior: Cancelada",
                                "Estado nuevo: Activa",
                                usuarioActual
                        );
                    }

                    mostrarValidacion(
                            fueReactivada
                                    ? "Función reactivada"
                                    : "Función programada",
                            resultado.getMensaje(),
                            VERDE
                    );

                    JOptionPane.showMessageDialog(
                            FuncionesAdminCINEXGUI.this,
                            resultado.getMensaje(),
                            fueReactivada
                                    ? "Reactivación exitosa"
                                    : "Registro exitoso",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    limpiarFormulario();
                    cargarFunciones();

                } catch (Exception ex) {
                    mostrarValidacion(
                            "No se pudo guardar",
                            "No se pudo registrar la función.",
                            ROJO
                    );
                    JOptionPane.showMessageDialog(
                            FuncionesAdminCINEXGUI.this,
                            "No se pudo registrar la función.",
                            "Error de registro",
                            JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    bloquearMientrasProcesa(false);
                }
            }
        };

        worker.execute();
    }

    private void gestionarResultadoNoExitoso(ResultadoProgramacion resultado) {
        Color color = resultado.getTipo() == TipoResultado.HORARIO_DUPLICADO
                ? ROJO
                : AMARILLO;

        String titulo = resultado.getTipo() == TipoResultado.HORARIO_DUPLICADO
                ? "Cruce de horarios detectado"
                : "Validación no superada";

        mostrarValidacion(titulo, resultado.getMensaje(), color);

        JOptionPane.showMessageDialog(
                this,
                resultado.getMensaje(),
                titulo,
                resultado.getTipo() == TipoResultado.HORARIO_DUPLICADO
                        ? JOptionPane.WARNING_MESSAGE
                        : JOptionPane.WARNING_MESSAGE
        );
    }

    private void cargarFuncionSeleccionadaEnFormulario() {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            return;
        }

        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        int idFuncion = convertirEntero(modelo.getValueAt(filaModelo, 0));
        FuncionCINEX datos = controlFunciones.obtenerFuncion(idFuncion);

        if (datos == null) {
            return;
        }

        cargandoCatalogos = true;
        seleccionarPeliculaPorId(datos.getIdPelicula());
        seleccionarSalaPorId(datos.getIdSala());
        spFecha.setValue(Date.from(
                datos.getFecha().atStartOfDay(ZoneId.systemDefault()).toInstant()
        ));

        Calendar calendario = Calendar.getInstance();
        calendario.set(Calendar.HOUR_OF_DAY, datos.getHora().getHour());
        calendario.set(Calendar.MINUTE, datos.getHora().getMinute());
        calendario.set(Calendar.SECOND, 0);
        spHora.setValue(calendario.getTime());
        cargandoCatalogos = false;

        mostrarValidacion(
                "Función seleccionada",
                "Modifique los datos necesarios y presione MODIFICAR FUNCIÓN.",
                AMARILLO
        );
        actualizarResumen();
    }

    private void seleccionarPeliculaPorId(int idPelicula) {
        for (int i = 0; i < cbPelicula.getItemCount(); i++) {
            PeliculaCINEX item = cbPelicula.getItemAt(i);
            if (item != null && item.getIdPelicula() == idPelicula) {
                cbPelicula.setSelectedIndex(i);
                return;
            }
        }
    }

    private void seleccionarSalaPorId(int idSala) {
        for (int i = 0; i < cbSala.getItemCount(); i++) {
            SalaItem item = cbSala.getItemAt(i);
            if (item != null && item.getIdSala() == idSala) {
                cbSala.setSelectedIndex(i);
                return;
            }
        }
    }

    private void modificarFuncionSeleccionada() {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Seleccione una función activa de la tabla.",
                    "Función requerida",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        int idFuncion = convertirEntero(modelo.getValueAt(filaModelo, 0));
        String peliculaAnterior = valorSeguro(modelo.getValueAt(filaModelo, 1));
        String salaAnterior = valorSeguro(modelo.getValueAt(filaModelo, 2));
        String fechaAnterior = valorSeguro(modelo.getValueAt(filaModelo, 3));
        String horaAnterior = valorSeguro(modelo.getValueAt(filaModelo, 4));

        PeliculaCINEX pelicula = (PeliculaCINEX) cbPelicula.getSelectedItem();
        SalaItem sala = (SalaItem) cbSala.getSelectedItem();
        LocalDate fecha = obtenerFechaSeleccionada();
        LocalTime hora = obtenerHoraSeleccionada();

        if (pelicula == null || sala == null || fecha == null || hora == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Complete todos los datos de la función.",
                    "Datos incompletos",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int entradasCompradas = controlFunciones.contarEntradasCompradas(idFuncion);
        String avisoEntradas = entradasCompradas > 0
                ? "\n\nEsta función tiene " + entradasCompradas + " entrada(s) comprada(s)."
                        + "\nAl modificarla, se enviará una notificación al gerente"
                        + "\npara revisar el reembolso de las entradas afectadas."
                : "";

        int confirmar = JOptionPane.showConfirmDialog(
                this,
                "¿Desea modificar la función seleccionada?\n\n"
                        + "Película: " + pelicula.getTitulo() + "\n"
                        + "Sala: " + sala.getDescripcion() + "\n"
                        + "Fecha: " + fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n"
                        + "Hora: " + hora.format(DateTimeFormatter.ofPattern("hh:mm a"))
                        + avisoEntradas,
                "Confirmar modificación",
                JOptionPane.YES_NO_OPTION,
                entradasCompradas > 0
                        ? JOptionPane.WARNING_MESSAGE
                        : JOptionPane.QUESTION_MESSAGE
        );

        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }

        bloquearMientrasProcesa(true);
        lblMensaje.setText("Modificando función...");
        lblMensaje.setForeground(AMARILLO);

        SwingWorker<ResultadoModificacion, Void> worker =
                new SwingWorker<ResultadoModificacion, Void>() {
            @Override
            protected ResultadoModificacion doInBackground() {
                return controlFunciones.modificarFuncion(
                        idFuncion,
                        pelicula.getIdPelicula(),
                        sala.getIdSala(),
                        fecha,
                        hora
                );
            }

            @Override
            protected void done() {
                try {
                    ResultadoModificacion resultado = get();
                    if (!resultado.isExito()) {
                        JOptionPane.showMessageDialog(
                                FuncionesAdminCINEXGUI.this,
                                resultado.getMensaje(),
                                "No se pudo modificar",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }

                    if (resultado.getEntradasCompradas() > 0) {
                        controlNotificaciones.registrarCambioFuncion(
                                idFuncion,
                                "La función fue modificada y posee entradas compradas. Se requiere revisar el reembolso.",
                                "Película: " + peliculaAnterior
                                        + "\nSala: " + salaAnterior
                                        + "\nFecha: " + fechaAnterior
                                        + "\nHora: " + horaAnterior,
                                "Película: " + pelicula.getTitulo()
                                        + "\nSala: " + sala.getDescripcion()
                                        + "\nFecha: " + fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                        + "\nHora: " + hora.format(DateTimeFormatter.ofPattern("hh:mm a"))
                                        + "\nEntradas afectadas: " + resultado.getEntradasCompradas(),
                                usuarioActual
                        );
                    }

                    JOptionPane.showMessageDialog(
                            FuncionesAdminCINEXGUI.this,
                            resultado.getMensaje(),
                            "Modificación exitosa",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    tabla.clearSelection();
                    limpiarFormulario();
                    cargarFunciones();

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            FuncionesAdminCINEXGUI.this,
                            "Ocurrió un error al modificar la función.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    bloquearMientrasProcesa(false);
                }
            }
        };

        worker.execute();
    }

    private void cancelarFuncionSeleccionada() {
        int filaVista = tabla.getSelectedRow();

        if (!controlFunciones.hayFuncionSeleccionada(filaVista)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Seleccione una función de la tabla.",
                    "Función requerida",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        int idFuncion = convertirEntero(modelo.getValueAt(filaModelo, 0));
        String pelicula = valorSeguro(modelo.getValueAt(filaModelo, 1));
        String sala = valorSeguro(modelo.getValueAt(filaModelo, 2));
        String fecha = valorSeguro(modelo.getValueAt(filaModelo, 3));
        String hora = valorSeguro(modelo.getValueAt(filaModelo, 4));
        String estado = valorSeguro(modelo.getValueAt(filaModelo, 5));

        if ("Cancelada".equalsIgnoreCase(estado)) {
            JOptionPane.showMessageDialog(
                    this,
                    "La función seleccionada ya se encuentra cancelada.",
                    "Función cancelada",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        if ("Finalizada".equalsIgnoreCase(estado)) {
            JOptionPane.showMessageDialog(
                    this,
                    "La función seleccionada ya finalizó y no puede cancelarse.",
                    "Función finalizada",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Desea cancelar esta función?\n\n"
                        + "Película: " + pelicula + "\n"
                        + "Sala: " + sala + "\n"
                        + "Fecha: " + fecha + "\n"
                        + "Hora: " + hora,
                "Confirmar cancelación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        bloquearMientrasProcesa(true);
        lblMensaje.setText("Cancelando función...");
        lblMensaje.setForeground(AMARILLO);

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return controlFunciones.cancelarFuncion(idFuncion);
            }

            @Override
            protected void done() {
                try {
                    boolean cancelada = get();
                    if (cancelada) {
                        controlNotificaciones.registrarCambioFuncion(
                                idFuncion,
                                "El administrador canceló la función.",
                                "Película: " + pelicula
                                        + "\nSala: " + sala
                                        + "\nFecha: " + fecha
                                        + "\nHora: " + hora
                                        + "\nEstado anterior: " + estado,
                                "Estado nuevo: Cancelada",
                                usuarioActual
                        );

                        JOptionPane.showMessageDialog(
                                FuncionesAdminCINEXGUI.this,
                                "Función cancelada correctamente.",
                                "Cancelación exitosa",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        cargarFunciones();
                    } else {
                        JOptionPane.showMessageDialog(
                                FuncionesAdminCINEXGUI.this,
                                "No se pudo cancelar la función.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            FuncionesAdminCINEXGUI.this,
                            "Ocurrió un error al cancelar la función.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    bloquearMientrasProcesa(false);
                }
            }
        };

        worker.execute();
    }

    private void limpiarFormulario() {
        if (cbPelicula.getItemCount() > 0) {
            cbPelicula.setSelectedIndex(0);
        }
        if (cbSala.getItemCount() > 0) {
            cbSala.setSelectedIndex(0);
        }
        establecerFechaHoraInicial();
        mostrarValidacion(
                "Formulario listo",
                "Seleccione los datos y presione PROGRAMAR FUNCIÓN.",
                VERDE
        );
        actualizarResumen();
    }

    private void actualizarResumen() {
        if (lblPeliculaResumen == null || cargandoCatalogos) {
            return;
        }

        PeliculaCINEX pelicula = (PeliculaCINEX) cbPelicula.getSelectedItem();
        SalaItem sala = (SalaItem) cbSala.getSelectedItem();
        LocalDate fecha = obtenerFechaSeleccionada();
        LocalTime hora = obtenerHoraSeleccionada();

        lblPeliculaResumen.setText(
                "Película: " + (pelicula == null ? "—" : pelicula.getTitulo())
        );
        lblSalaResumen.setText(
                "Sala: " + (sala == null ? "—" : sala.getDescripcion())
        );

        if (fecha != null && hora != null) {
            lblFechaResumen.setText(
                    "Fecha y hora: "
                            + fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            + " - "
                            + hora.format(DateTimeFormatter.ofPattern("hh:mm a"))
            );
        } else {
            lblFechaResumen.setText("Fecha y hora: —");
        }

        int duracion = pelicula == null ? 0 : pelicula.getDuracion();
        if (duracion > 0 && hora != null) {
            LocalTime finPelicula =
                    hora.plusMinutes(duracion);

            LocalTime salaDisponible =
                    finPelicula.plusMinutes(
                            ControlProgramaFuncionCINEX
                                    .TIEMPO_LIMPIEZA_MIN
                    );

            lblDuracionResumen.setText(
                    "Película: "
                            + duracion
                            + " min · Finaliza "
                            + finPelicula.format(
                                    DateTimeFormatter
                                            .ofPattern("hh:mm a")
                            )
                            + " · Sala libre "
                            + salaDisponible.format(
                                    DateTimeFormatter
                                            .ofPattern("hh:mm a")
                            )
            );
        } else {
            lblDuracionResumen.setText("Duración estimada: —");
        }
    }

    private void bloquearFormulario(boolean bloquear) {
        cbPelicula.setEnabled(!bloquear);
        cbSala.setEnabled(!bloquear);
        spFecha.setEnabled(!bloquear);
        spHora.setEnabled(!bloquear);
        btnProgramar.setEnabled(!bloquear);
    }

    private void bloquearMientrasProcesa(boolean bloquear) {
        boolean puedeUsarFormulario = !bloquear
                && cbPelicula.getItemCount() > 0
                && cbSala.getItemCount() > 0;

        cbPelicula.setEnabled(puedeUsarFormulario);
        cbSala.setEnabled(puedeUsarFormulario);
        spFecha.setEnabled(puedeUsarFormulario);
        spHora.setEnabled(puedeUsarFormulario);
        btnProgramar.setEnabled(puedeUsarFormulario);
        tabla.setEnabled(!bloquear);
        if (txtBuscar != null) {
            txtBuscar.setEnabled(!bloquear);
        }
        boolean activa = !bloquear && funcionSeleccionadaEstaActiva();
        btnModificarFuncion.setEnabled(activa);
        btnCancelarFuncion.setEnabled(activa);
    }



    private void filtrarTabla() {
        if (txtBuscar == null || sorter == null) {
            return;
        }

        String texto = txtBuscar.getText().trim();
        if (texto.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter(
                    "(?i)" + Pattern.quote(texto),
                    1, 2, 3, 4, 5
            ));
        }

        tabla.clearSelection();
        btnModificarFuncion.setEnabled(false);
        btnCancelarFuncion.setEnabled(false);
    }

    private String crearMensajeConfirmacion(
            PeliculaCINEX pelicula,
            SalaItem sala,
            LocalDate fecha,
            LocalTime hora
    ) {
        int duracion = pelicula.getDuracion() > 0
                ? pelicula.getDuracion()
                : 120;

        LocalTime finalizacion =
                hora.plusMinutes(duracion);

        LocalTime salaDisponible =
                finalizacion.plusMinutes(
                        ControlProgramaFuncionCINEX
                                .TIEMPO_LIMPIEZA_MIN
                );

        return "¿Desea guardar esta programación?\n\n"
                + "Película: " + pelicula.getTitulo() + "\n"
                + "Sala: " + sala.getDescripcion() + "\n"
                + "Fecha: " + fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n"
                + "Inicio: " + hora.format(DateTimeFormatter.ofPattern("hh:mm a")) + "\n"
                + "Fin de película: " + finalizacion.format(DateTimeFormatter.ofPattern("hh:mm a")) + "\n"
                + "Limpieza: 30 minutos\n"
                + "Sala disponible: " + salaDisponible.format(DateTimeFormatter.ofPattern("hh:mm a"));
    }

    private void mostrarValidacion(String titulo, String detalle, Color color) {
        lblTituloValidacion.setText(titulo);
        lblTituloValidacion.setForeground(color);
        lblDetalleValidacion.setText(
                "<html><div style='width:290px;'>" + escaparHtml(detalle) + "</div></html>"
        );
    }

    private String escaparHtml(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private LocalDate obtenerFechaSeleccionada() {
        Object valor = spFecha.getValue();
        if (!(valor instanceof Date)) {
            return null;
        }
        return ((Date) valor).toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private LocalTime obtenerHoraSeleccionada() {
        Object valor = spHora.getValue();
        if (!(valor instanceof Date)) {
            return null;
        }
        return ((Date) valor).toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .withSecond(0)
                .withNano(0);
    }

    private void establecerFechaHoraInicial() {
        LocalDateTime siguiente = LocalDateTime.now()
                .plusMinutes(30)
                .withSecond(0)
                .withNano(0);
        int minutos = siguiente.getMinute();
        int ajuste = (15 - minutos % 15) % 15;
        siguiente = siguiente.plusMinutes(ajuste);

        Date fecha = Date.from(
                siguiente.atZone(ZoneId.systemDefault()).toInstant()
        );

        if (spFecha != null) {
            spFecha.setValue(fecha);
        }
        if (spHora != null) {
            spHora.setValue(fecha);
        }
    }

    private void iniciarReloj() {
        actualizarFechaHoraSistema();
        new Timer(1000, e -> actualizarFechaHoraSistema()).start();
    }

    private void actualizarFechaHoraSistema() {
        LocalDateTime ahora = LocalDateTime.now();

        lblHoraSistema.setText(
                ahora.format(
                        DateTimeFormatter.ofPattern("hh:mm a")
                )
        );

        lblFechaSistema.setText(
                ahora.format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                )
        );

        /*
         * Se reevalúa el estado con el reloj del sistema.
         * Así una función cambia de Activa a Finalizada
         * aunque la ventana permanezca abierta.
         */
        if (ahora.getSecond() == 0) {
            actualizarEstadosTemporales();
        }
    }

    private void volverMenu() {
        new MenuAdministradorCINEXGUI(usuarioActual).setVisible(true);
        dispose();
    }

    private void agregarCampo(
            JPanel panel,
            String etiqueta,
            JComponent componente,
            int columna,
            int fila,
            GridBagConstraints base
    ) {
        GridBagConstraints gbcEtiqueta = (GridBagConstraints) base.clone();
        gbcEtiqueta.gridx = columna;
        gbcEtiqueta.gridy = fila * 2;
        gbcEtiqueta.weightx = 1.0;
        gbcEtiqueta.insets = new Insets(fila == 0 ? 8 : 7, columna == 0 ? 0 : 8, 4, columna == 0 ? 8 : 0);

        JLabel lbl = new JLabel(etiqueta);
        lbl.setForeground(GRIS);
        lbl.setFont(new Font("Arial", Font.BOLD, 13));
        panel.add(lbl, gbcEtiqueta);

        GridBagConstraints gbcCampo = (GridBagConstraints) base.clone();
        gbcCampo.gridx = columna;
        gbcCampo.gridy = fila * 2 + 1;
        gbcCampo.weightx = 1.0;
        gbcCampo.insets = new Insets(0, columna == 0 ? 0 : 8, 7, columna == 0 ? 8 : 0);
        componente.setPreferredSize(new Dimension(300, 42));
        panel.add(componente, gbcCampo);
    }

    private <T> JComboBox<T> crearCombo() {
        JComboBox<T> combo = new JComboBox<>();
        combo.setBackground(AZUL_PANEL_2);
        combo.setForeground(BLANCO);
        combo.setFont(new Font("Arial", Font.BOLD, 13));
        combo.setFocusable(false);
        combo.setOpaque(false);
        combo.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        combo.setMaximumRowCount(10);

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
                        list, value, index, isSelected, cellHasFocus
                );
                label.setOpaque(true);
                label.setBorder(new EmptyBorder(9, 12, 9, 12));
                label.setFont(new Font("Arial", Font.BOLD, 13));
                label.setBackground(isSelected ? new Color(20, 60, 115) : AZUL_PANEL_2);
                label.setForeground(BLANCO);
                return label;
            }
        });

        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton boton = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(
                                RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON
                        );
                        g2.setColor(AZUL_PANEL_2);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        g2.setColor(AZUL_BORDE);
                        g2.drawLine(0, 0, 0, getHeight());

                        int cx = getWidth() / 2;
                        int cy = getHeight() / 2 + 2;
                        Polygon flecha = new Polygon();
                        flecha.addPoint(cx - 6, cy - 4);
                        flecha.addPoint(cx + 6, cy - 4);
                        flecha.addPoint(cx, cy + 5);
                        g2.setColor(AMARILLO);
                        g2.fillPolygon(flecha);
                        g2.dispose();
                    }
                };
                boton.setBorderPainted(false);
                boton.setContentAreaFilled(false);
                boton.setFocusPainted(false);
                boton.setOpaque(false);
                return boton;
            }
        });

        return combo;
    }

    private JSpinner crearSpinnerFecha() {
        SpinnerDateModel modeloFecha = new SpinnerDateModel();
        JSpinner spinner = new JSpinner(modeloFecha);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "dd/MM/yyyy");
        spinner.setEditor(editor);
        estilizarSpinner(spinner, editor.getTextField());
        return spinner;
    }

    private JSpinner crearSpinnerHora() {
        SpinnerDateModel modeloHora = new SpinnerDateModel(
                new Date(),
                null,
                null,
                Calendar.MINUTE
        );
        JSpinner spinner = new JSpinner(modeloHora);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "hh:mm a");
        spinner.setEditor(editor);
        estilizarSpinner(spinner, editor.getTextField());
        return spinner;
    }

    private void estilizarSpinner(JSpinner spinner, JFormattedTextField campo) {
        spinner.setBackground(AZUL_PANEL_2);
        spinner.setForeground(BLANCO);
        spinner.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        spinner.setOpaque(true);

        campo.setBackground(AZUL_PANEL_2);
        campo.setForeground(BLANCO);
        campo.setCaretColor(BLANCO);
        campo.setSelectionColor(AMARILLO);
        campo.setSelectedTextColor(Color.BLACK);
        campo.setFont(new Font("Arial", Font.BOLD, 13));
        campo.setHorizontalAlignment(SwingConstants.CENTER);
        campo.setBorder(new EmptyBorder(0, 10, 0, 10));

        for (Component componente : spinner.getComponents()) {
            if (componente instanceof JButton) {
                JButton boton = (JButton) componente;
                boton.setBackground(AZUL_PANEL_2);
                boton.setForeground(AMARILLO);
                boton.setFocusPainted(false);
                boton.setBorder(new LineBorder(AZUL_BORDE, 1));
            }
        }
    }

    private JTextField crearCampoTexto() {
        JTextField campo = new JTextField();
        campo.setBackground(AZUL_PANEL_2);
        campo.setForeground(BLANCO);
        campo.setCaretColor(BLANCO);
        campo.setSelectionColor(AMARILLO);
        campo.setSelectedTextColor(Color.BLACK);
        campo.setFont(new Font("Arial", Font.BOLD, 13));
        campo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1, true),
                new EmptyBorder(0, 11, 0, 11)
        ));
        return campo;
    }

    private JLabel crearLineaResumen(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(GRIS);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        return label;
    }

    private JButton crearBoton(String texto, Color fondo, Color colorTexto, int alto) {
        JButton boton = new BotonModerno(texto, fondo, colorTexto);
        boton.setPreferredSize(new Dimension(175, alto));
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return boton;
    }

    private JLabel crearTextoHeader(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(BLANCO);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        return label;
    }

    private void estilizarTabla() {
        tabla.setRowHeight(40);
        tabla.setFont(new Font("Arial", Font.PLAIN, 13));
        tabla.setBackground(AZUL_TABLA);
        tabla.setForeground(BLANCO);
        tabla.setSelectionBackground(AMARILLO);
        tabla.setSelectionForeground(new Color(5, 20, 55));
        tabla.setGridColor(new Color(42, 78, 132));
        tabla.setIntercellSpacing(new Dimension(1, 1));
        tabla.setFillsViewportHeight(true);
        tabla.setShowGrid(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFocusable(false);

        JTableHeader header = tabla.getTableHeader();
        header.setPreferredSize(new Dimension(0, 39));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());

        tabla.setDefaultRenderer(Object.class, new CeldaRenderer());

        tabla.getColumnModel().getColumn(0).setPreferredWidth(55);
        tabla.getColumnModel().getColumn(0).setMaxWidth(75);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(290);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(160);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(125);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(105);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(120);
    }

    private int convertirEntero(Object[] fila, int indice) {
        return fila != null && indice >= 0 && indice < fila.length
                ? convertirEntero(fila[indice])
                : 0;
    }

    private int convertirEntero(Object valor) {
        if (valor instanceof Number) {
            return ((Number) valor).intValue();
        }
        try {
            return Integer.parseInt(valorSeguro(valor));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String valorSeguro(Object[] fila, int indice) {
        return fila != null && indice >= 0 && indice < fila.length
                ? valorSeguro(fila[indice])
                : "";
    }

    private String valorSeguro(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }

    private ImageIcon cargarImagen(String ruta, int ancho, int alto) {
        try {
            File archivo = new File(ruta);
            if (!archivo.exists()) {
                archivo = new File("Imagenes/" + new File(ruta).getName());
            }
            if (!archivo.exists()) {
                return new ImageIcon();
            }

            BufferedImage original = ImageIO.read(archivo);
            if (original == null) {
                return new ImageIcon();
            }

            BufferedImage escalada = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = escalada.createGraphics();
            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            g2.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            g2.drawImage(original, 0, 0, ancho, alto, null);
            g2.dispose();
            return new ImageIcon(escalada);
        } catch (Exception e) {
            return new ImageIcon();
        }
    }

    private class ScrollBarFuncionesUI
            extends BasicScrollBarUI {

        @Override
        protected void configureScrollBarColors() {
            trackColor = AZUL_TABLA;
            thumbColor = new Color(70, 105, 155);
        }

        @Override
        protected JButton createDecreaseButton(
                int orientation
        ) {
            return crearBotonInvisible();
        }

        @Override
        protected JButton createIncreaseButton(
                int orientation
        ) {
            return crearBotonInvisible();
        }

        private JButton crearBotonInvisible() {
            JButton boton = new JButton();
            Dimension cero = new Dimension(0, 0);

            boton.setPreferredSize(cero);
            boton.setMinimumSize(cero);
            boton.setMaximumSize(cero);
            boton.setBorder(null);
            boton.setOpaque(false);
            boton.setContentAreaFilled(false);

            return boton;
        }

        @Override
        protected void paintTrack(
                Graphics g,
                JComponent componente,
                Rectangle limites
        ) {
            g.setColor(AZUL_TABLA);
            g.fillRect(
                    limites.x,
                    limites.y,
                    limites.width,
                    limites.height
            );
        }

        @Override
        protected void paintThumb(
                Graphics g,
                JComponent componente,
                Rectangle limites
        ) {
            if (!componente.isEnabled()
                    || limites.width <= 0
                    || limites.height <= 0) {
                return;
            }

            Graphics2D g2 =
                    (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(new Color(70, 105, 155));
            g2.fillRoundRect(
                    limites.x + 2,
                    limites.y + 2,
                    Math.max(1, limites.width - 4),
                    Math.max(1, limites.height - 4),
                    8,
                    8
            );

            g2.dispose();
        }
    }

    private class HeaderRenderer extends DefaultTableCellRenderer {
        public HeaderRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBackground(AZUL_HEADER);
            setForeground(BLANCO);
            setFont(new Font("Arial", Font.BOLD, 13));
            setBorder(new LineBorder(new Color(42, 78, 132), 1));
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
            );
            setBackground(AZUL_HEADER);
            setForeground(BLANCO);
            setHorizontalAlignment(SwingConstants.CENTER);
            return this;
        }
    }

    private class CeldaRenderer extends DefaultTableCellRenderer {
        public CeldaRenderer() {
            setOpaque(true);
            setBorder(new EmptyBorder(0, 10, 0, 10));
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
            );

            if (isSelected) {
                setBackground(AMARILLO);
                setForeground(new Color(5, 20, 55));
            } else {
                setBackground(row % 2 == 0 ? AZUL_TABLA : AZUL_TABLA_ALTERNA);
                setForeground(BLANCO);
            }

            setHorizontalAlignment(column == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);

            if (column == 5 && !isSelected) {
                String estado = valorSeguro(value);

                if ("Cancelada".equalsIgnoreCase(estado)) {
                    setForeground(ROJO);
                } else if ("Finalizada".equalsIgnoreCase(estado)) {
                    setForeground(GRIS);
                } else {
                    setForeground(VERDE);
                }

                setFont(new Font("Arial", Font.BOLD, 12));
            } else {
                setFont(new Font("Arial", column == 1 ? Font.BOLD : Font.PLAIN, 13));
            }

            return this;
        }
    }

    private class BotonModerno extends JButton {
        private final Color fondoBase;
        private final Color colorTexto;

        public BotonModerno(String texto, Color fondoBase, Color colorTexto) {
            super(texto);
            this.fondoBase = fondoBase;
            this.colorTexto = colorTexto;

            setForeground(colorTexto);
            setFont(new Font("Arial", Font.BOLD, 13));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Color fondo;
            if (!isEnabled()) {
                fondo = new Color(55, 67, 90);
            } else if (getModel().isPressed()) {
                fondo = fondoBase.darker();
            } else if (getModel().isRollover()) {
                fondo = aclarar(fondoBase, 18);
            } else {
                fondo = fondoBase;
            }

            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.setColor(new Color(255, 255, 255, 35));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();

            setForeground(isEnabled() ? colorTexto : new Color(170, 177, 190));
            super.paintComponent(g);
        }
    }

    private class RoundedPanel extends JPanel {
        private final int radio;
        private final Color colorFondo;

        public RoundedPanel(int radio, Color colorFondo) {
            this.radio = radio;
            this.colorFondo = colorFondo;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            Shape forma = new RoundRectangle2D.Double(
                    0, 0, getWidth() - 1, getHeight() - 1, radio, radio
            );
            g2.setColor(colorFondo);
            g2.fill(forma);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private class FondoPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            GradientPaint degradado = new GradientPaint(
                    0, 0, AZUL_FONDO_1,
                    getWidth(), getHeight(), AZUL_FONDO_2
            );
            g2.setPaint(degradado);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 255, 255, 7));
            g2.fillOval(-190, -110, 540, 540);
            g2.fillOval(getWidth() - 410, 250, 380, 380);
            g2.dispose();
        }
    }

    private static Color aclarar(Color color, int cantidad) {
        return new Color(
                Math.min(255, color.getRed() + cantidad),
                Math.min(255, color.getGreen() + cantidad),
                Math.min(255, color.getBlue() + cantidad)
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new FuncionesAdminCINEXGUI("admin").setVisible(true);
        });
    }
}
