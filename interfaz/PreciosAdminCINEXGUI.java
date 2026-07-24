package interfaz;

import control.ControlGestionarPreciosCINEX;
import entidad.PrecioCINEX;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class PreciosAdminCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_PANEL = new Color(7, 24, 56);
    private final Color AZUL_PANEL_2 = new Color(10, 34, 75);
    private final Color AZUL_TABLA = new Color(7, 28, 65);
    private final Color AZUL_TABLA_ALTERNA = new Color(9, 33, 74);
    private final Color AZUL_HEADER = new Color(12, 43, 91);
    private final Color AZUL_BORDE = new Color(63, 96, 145);
    private final Color AZUL_BOTON = new Color(0, 80, 160);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 247, 252);
    private final Color GRIS = new Color(185, 195, 210);
    private final Color VERDE = new Color(35, 180, 85);
    private final Color ROJO = new Color(225, 70, 70);

    private final String usuarioActual;
    private final ControlGestionarPreciosCINEX controlPrecios;
    private final DecimalFormat formatoMonto = new DecimalFormat("0.00");

    private JTable tabla;
    private DefaultTableModel modelo;

    private JTextField txtTipoEntrada;
    private JTextField txtMonto;

    private JLabel lblMensaje;
    private JLabel lblCantidad;
    private JLabel lblPromedio;
    private JLabel lblSeleccion;
    private JLabel lblHora;
    private JLabel lblFecha;

    private JButton btnGuardar;
    private JButton btnLimpiar;

    private int idSeleccionado = -1;
    private boolean cargandoFormulario = false;

    public PreciosAdminCINEXGUI() {
        this("admin");
    }

    public PreciosAdminCINEXGUI(String usuario) {
        this.usuarioActual = usuario == null || usuario.trim().isEmpty()
                ? "admin"
                : usuario.trim();
        this.controlPrecios = new ControlGestionarPreciosCINEX();

        configurarVentana();
        construirInterfaz();
        configurarEventosGenerales();
        iniciarReloj();
        cargarDatos();
    }

    private void configurarVentana() {
        setTitle("CINEX - Configurar precios");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 760);
        setMinimumSize(new Dimension(1100, 680));
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
        header.setBorder(new EmptyBorder(18, 34, 12, 34));

        JPanel bloqueIzquierdo = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        bloqueIzquierdo.setOpaque(false);

        JLabel logo = new JLabel();
        ImageIcon iconoLogo = cargarImagen("imagenes/logocinex.png", 190, 70);
        if (iconoLogo.getIconWidth() > 0) {
            logo.setIcon(iconoLogo);
            bloqueIzquierdo.add(logo);
        }

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Configurar precios");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 32));

        JLabel subtitulo = new JLabel("Administra el monto de cada tipo de entrada.");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 16));

        textos.add(Box.createVerticalStrut(5));
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(6));
        textos.add(subtitulo);

        bloqueIzquierdo.add(textos);

        JPanel info = new JPanel(new FlowLayout(FlowLayout.RIGHT, 25, 8));
        info.setOpaque(false);

        JLabel lblUsuario = crearTextoHeader("Administrador: " + usuarioActual);
        lblHora = crearTextoHeader("");
        lblFecha = crearTextoHeader("");

        info.add(lblUsuario);
        info.add(lblHora);
        info.add(lblFecha);

        header.add(bloqueIzquierdo, BorderLayout.WEST);
        header.add(info, BorderLayout.EAST);
        return header;
    }

    private JPanel crearContenido() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(8, 34, 14, 34));

        JPanel estadisticas = crearPanelEstadisticas();

        JPanel principal = new JPanel(new BorderLayout(22, 0));
        principal.setOpaque(false);
        principal.add(crearPanelTabla(), BorderLayout.CENTER);
        principal.add(crearPanelEdicion(), BorderLayout.EAST);

        wrapper.add(estadisticas, BorderLayout.NORTH);
        wrapper.add(principal, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel crearPanelEstadisticas() {
        JPanel contenedor = new JPanel(new GridLayout(1, 2, 16, 0));
        contenedor.setOpaque(false);
        contenedor.setBorder(new EmptyBorder(0, 0, 16, 0));

        lblCantidad = new JLabel("0");
        lblPromedio = new JLabel("S/ 0.00");

        contenedor.add(crearTarjetaEstadistica("TIPOS DE ENTRADA", lblCantidad,
                "Tarifas registradas en el sistema"));
        contenedor.add(crearTarjetaEstadistica("PRECIO PROMEDIO", lblPromedio,
                "Promedio de las tarifas registradas"));

        return contenedor;
    }

    private JPanel crearTarjetaEstadistica(String titulo, JLabel valor, String detalle) {
        RoundedPanel tarjeta = new RoundedPanel(18, new Color(7, 24, 56, 225));
        tarjeta.setLayout(new BorderLayout());
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(63, 96, 145), 1, true),
                new EmptyBorder(13, 18, 13, 18)
        ));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setForeground(GRIS);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 12));

        valor.setForeground(AMARILLO);
        valor.setFont(new Font("Arial", Font.BOLD, 25));

        JLabel lblDetalle = new JLabel(detalle);
        lblDetalle.setForeground(new Color(155, 168, 190));
        lblDetalle.setFont(new Font("Arial", Font.PLAIN, 12));

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.add(lblTitulo);
        centro.add(Box.createVerticalStrut(4));
        centro.add(valor);
        centro.add(Box.createVerticalStrut(2));
        centro.add(lblDetalle);

        tarjeta.add(centro, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel crearPanelTabla() {
        RoundedPanel panel = new RoundedPanel(18, new Color(5, 18, 43, 230));
        panel.setLayout(new BorderLayout(0, 14));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1, true),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JPanel encabezado = new JPanel(new BorderLayout(18, 0));
        encabezado.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Lista de tarifas registradas");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 21));

        JLabel subtitulo = new JLabel("Seleccione una fila para modificar su precio.");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 13));

        textos.add(titulo);
        textos.add(Box.createVerticalStrut(4));
        textos.add(subtitulo);

        encabezado.add(textos, BorderLayout.WEST);

        modelo = new DefaultTableModel(
                new String[]{"ID", "Tipo de entrada", "Monto"}, 0
        ) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columna) {
                if (columna == 0) {
                    return Integer.class;
                }
                return String.class;
            }
        };

        tabla = new JTable(modelo);
        estilizarTabla();

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        scroll.getViewport().setBackground(AZUL_TABLA);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        lblMensaje = new JLabel("Cargando precios...");
        lblMensaje.setForeground(GRIS);
        lblMensaje.setFont(new Font("Arial", Font.BOLD, 13));

        JLabel ayuda = new JLabel("Doble clic en una tarifa para editar el monto.");
        ayuda.setForeground(new Color(145, 160, 185));
        ayuda.setFont(new Font("Arial", Font.PLAIN, 12));

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(lblMensaje, BorderLayout.WEST);
        pie.add(ayuda, BorderLayout.EAST);

        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(pie, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearPanelEdicion() {
        RoundedPanel panel = new RoundedPanel(18, new Color(7, 24, 56, 235));
        panel.setPreferredSize(new Dimension(360, 0));
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1, true),
                new EmptyBorder(22, 22, 22, 22)
        ));

        JPanel contenido = new JPanel();
        contenido.setOpaque(false);
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Editar tarifa");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 23));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descripcion = new JLabel(
                "<html>Seleccione un registro de la tabla para actualizar su información.</html>"
        );
        descripcion.setForeground(GRIS);
        descripcion.setFont(new Font("Arial", Font.PLAIN, 13));
        descripcion.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblSeleccion = new JLabel("Ninguna tarifa seleccionada");
        lblSeleccion.setOpaque(true);
        lblSeleccion.setBackground(new Color(10, 34, 75));
        lblSeleccion.setForeground(GRIS);
        lblSeleccion.setFont(new Font("Arial", Font.BOLD, 13));
        lblSeleccion.setBorder(new EmptyBorder(10, 12, 10, 12));
        lblSeleccion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        lblSeleccion.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtTipoEntrada = crearCampoTexto();
        txtTipoEntrada.setEditable(false);
        txtTipoEntrada.setFocusable(false);

        txtMonto = crearCampoTexto();
        txtMonto.setToolTipText("Ingrese un monto mayor que cero");

        btnGuardar = crearBoton("GUARDAR CAMBIOS", AMARILLO, Color.BLACK, 48);
        btnLimpiar = crearBoton("CANCELAR EDICIÓN", AZUL_BOTON, BLANCO, 44);

        btnGuardar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLimpiar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnGuardar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btnLimpiar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        contenido.add(titulo);
        contenido.add(Box.createVerticalStrut(7));
        contenido.add(descripcion);
        contenido.add(Box.createVerticalStrut(20));
        contenido.add(lblSeleccion);
        contenido.add(Box.createVerticalStrut(24));
        contenido.add(crearEtiqueta("Tipo de entrada"));
        contenido.add(Box.createVerticalStrut(7));
        contenido.add(txtTipoEntrada);
        contenido.add(Box.createVerticalStrut(18));
        contenido.add(crearEtiqueta("Nuevo monto (S/)"));
        contenido.add(Box.createVerticalStrut(7));
        contenido.add(txtMonto);
        contenido.add(Box.createVerticalStrut(18));
        contenido.add(Box.createVerticalGlue());
        contenido.add(btnGuardar);
        contenido.add(Box.createVerticalStrut(10));
        contenido.add(btnLimpiar);

        panel.add(contenido, BorderLayout.CENTER);

        habilitarFormulario(false);
        return panel;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(0, 34, 22, 34));

        JLabel nota = new JLabel("Los cambios se guardan directamente en la tabla precios.");
        nota.setForeground(new Color(145, 160, 185));
        nota.setFont(new Font("Arial", Font.PLAIN, 12));

        JButton btnMenu = crearBoton("MENÚ PRINCIPAL", AZUL_BOTON, BLANCO, 48);
        btnMenu.setPreferredSize(new Dimension(190, 48));

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        acciones.setOpaque(false);
        acciones.add(btnMenu);

        btnMenu.addActionListener(e -> volverMenu());

        footer.add(nota, BorderLayout.WEST);
        footer.add(acciones, BorderLayout.EAST);
        return footer;
    }

    private void configurarEventosGenerales() {
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccionEnFormulario();
            }
        });

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.getSelectedRow() >= 0) {
                    txtMonto.requestFocusInWindow();
                    txtMonto.selectAll();
                }
            }
        });


        txtMonto.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && btnGuardar.isEnabled()) {
                    guardarCambios();
                }
            }
        });

        btnGuardar.addActionListener(e -> guardarCambios());
        btnLimpiar.addActionListener(e -> limpiarSeleccion());

        getRootPane().registerKeyboardAction(
                e -> volverMenu(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void cargarDatos() {
        limpiarSeleccion();
        mostrarEstado("Consultando precios...", GRIS);

        SwingWorker<ArrayList<PrecioCINEX>, Void> worker = new SwingWorker<ArrayList<PrecioCINEX>, Void>() {
            @Override
            protected ArrayList<PrecioCINEX> doInBackground() {
                return controlPrecios.listarPrecios();
            }

            @Override
            protected void done() {
                try {
                    ArrayList<PrecioCINEX> datos = get();
                    modelo.setRowCount(0);

                    double suma = 0;

                    for (PrecioCINEX precio : datos) {
                        int id = precio.getIdPrecio();
                        String tipo = precio.getTipoEntrada();
                        double monto = precio.getMonto();

                        modelo.addRow(new Object[]{
                                id,
                                tipo,
                                "S/ " + formatoMonto.format(monto)
                        });

                        suma += monto;
                    }

                    lblCantidad.setText(String.valueOf(datos.size()));
                    lblPromedio.setText(datos.isEmpty()
                            ? "S/ 0.00"
                            : "S/ " + formatoMonto.format(suma / datos.size()));

                    if (datos.isEmpty()) {
                        mostrarEstado("No existen precios registrados.", AMARILLO);
                    } else {
                        mostrarEstado("Precios cargados correctamente: " + datos.size(), VERDE);
                    }

                } catch (Exception ex) {
                    modelo.setRowCount(0);
                    lblCantidad.setText("0");
                    lblPromedio.setText("S/ 0.00");
                    mostrarEstado("No se pudieron cargar los precios.", ROJO);

                    System.err.println(
                            "[Precios] No se pudieron recuperar los precios: "
                                    + ex.getMessage()
                    );
                } finally {
                    tabla.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void cargarSeleccionEnFormulario() {
        if (cargandoFormulario) {
            return;
        }

        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            limpiarSeleccion();
            return;
        }

        int filaModelo = tabla.convertRowIndexToModel(filaVista);

        idSeleccionado = convertirEntero(modelo.getValueAt(filaModelo, 0));
        String tipo = valorSeguro(modelo.getValueAt(filaModelo, 1));
        String monto = valorSeguro(modelo.getValueAt(filaModelo, 2))
                .replace("S/", "")
                .trim();
        cargandoFormulario = true;
        txtTipoEntrada.setText(tipo);
        txtMonto.setText(monto);
        lblSeleccion.setText("Seleccionado: " + tipo);
        lblSeleccion.setForeground(AMARILLO);
        cargandoFormulario = false;

        habilitarFormulario(true);
    }

    private void guardarCambios() {
        if (idSeleccionado <= 0) {
            mostrarAdvertencia("Seleccione una tarifa de la tabla.");
            return;
        }

        String textoMonto = txtMonto.getText().trim().replace(",", ".");

        if (!controlPrecios.montoValido(textoMonto)) {
            mostrarAdvertencia("Ingrese un monto válido mayor que cero.");
            txtMonto.requestFocusInWindow();
            txtMonto.selectAll();
            return;
        }

        double nuevoMonto = controlPrecios.convertirMonto(textoMonto);

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Desea actualizar la tarifa \"" + txtTipoEntrada.getText() + "\"?\n\n"
                        + "Nuevo monto: S/ " + formatoMonto.format(nuevoMonto),
                "Confirmar actualización",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        final int idActualizar = idSeleccionado;
        bloquearMientrasGuarda(true);
        mostrarEstado("Actualizando tarifa...", AMARILLO);

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return controlPrecios.actualizarPrecio(
                        idActualizar,
                        nuevoMonto
                );
            }

            @Override
            protected void done() {
                try {
                    boolean actualizado = get();

                    if (actualizado) {
                        mostrarEstado("Precio actualizado correctamente.", VERDE);
                        JOptionPane.showMessageDialog(
                                PreciosAdminCINEXGUI.this,
                                "La tarifa fue actualizada correctamente.",
                                "Actualización exitosa",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        cargarDatos();
                    } else {
                        mostrarEstado("No se pudo actualizar la tarifa.", ROJO);
                        System.err.println(
                                "[Precios] La actualización no fue confirmada."
                        );
                    }

                } catch (Exception ex) {
                    mostrarEstado("Ocurrió un error al actualizar.", ROJO);
                    System.err.println(
                            "[Precios] Error al actualizar: " + ex.getMessage()
                    );
                } finally {
                    bloquearMientrasGuarda(false);
                }
            }
        };

        worker.execute();
    }

    private void limpiarSeleccion() {
        idSeleccionado = -1;

        if (tabla != null) {
            tabla.clearSelection();
        }

        if (txtTipoEntrada != null) {
            txtTipoEntrada.setText("");
        }

        if (txtMonto != null) {
            txtMonto.setText("");
        }

        if (lblSeleccion != null) {
            lblSeleccion.setText("Ninguna tarifa seleccionada");
            lblSeleccion.setForeground(GRIS);
        }

        habilitarFormulario(false);
    }

    private void habilitarFormulario(boolean habilitar) {
        if (txtMonto != null) {
            txtMonto.setEnabled(habilitar);
        }

        if (btnGuardar != null) {
            btnGuardar.setEnabled(habilitar);
        }

        if (btnLimpiar != null) {
            btnLimpiar.setEnabled(habilitar);
        }
    }

    private void bloquearMientrasGuarda(boolean bloquear) {
        tabla.setEnabled(!bloquear);
        txtMonto.setEnabled(!bloquear && idSeleccionado > 0);
        btnGuardar.setEnabled(!bloquear && idSeleccionado > 0);
        btnLimpiar.setEnabled(!bloquear && idSeleccionado > 0);
    }


    private void estilizarTabla() {
        tabla.setRowHeight(44);
        tabla.setFont(new Font("Arial", Font.PLAIN, 14));
        tabla.setBackground(AZUL_TABLA);
        tabla.setForeground(BLANCO);
        tabla.setSelectionBackground(AMARILLO);
        tabla.setSelectionForeground(new Color(5, 20, 55));
        tabla.setGridColor(new Color(42, 78, 132));
        tabla.setIntercellSpacing(new Dimension(1, 1));
        tabla.setFillsViewportHeight(true);
        tabla.setShowGrid(true);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFocusable(false);

        JTableHeader header = tabla.getTableHeader();
        header.setPreferredSize(new Dimension(0, 42));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());

        tabla.setDefaultRenderer(Object.class, new CeldaRenderer());

        tabla.getColumnModel().getColumn(0).setPreferredWidth(55);
        tabla.getColumnModel().getColumn(0).setMaxWidth(80);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(280);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(140);
    }

    private JTextField crearCampoTexto() {
        JTextField campo = new JTextField();
        campo.setBackground(new Color(8, 28, 65));
        campo.setForeground(BLANCO);
        campo.setCaretColor(BLANCO);
        campo.setSelectionColor(AMARILLO);
        campo.setSelectedTextColor(Color.BLACK);
        campo.setFont(new Font("Arial", Font.BOLD, 14));
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        campo.setPreferredSize(new Dimension(250, 42));
        campo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1, true),
                new EmptyBorder(0, 12, 0, 12)
        ));
        return campo;
    }

    private JLabel crearEtiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(GRIS);
        etiqueta.setFont(new Font("Arial", Font.BOLD, 13));
        etiqueta.setAlignmentX(Component.LEFT_ALIGNMENT);
        return etiqueta;
    }

    private JButton crearBoton(
            String texto,
            Color fondo,
            Color textoColor,
            int alto
    ) {
        JButton boton = new BotonModerno(texto, fondo, textoColor);
        boton.setPreferredSize(new Dimension(180, alto));
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return boton;
    }

    private JLabel crearTextoHeader(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(BLANCO);
        label.setFont(new Font("Arial", Font.BOLD, 15));
        return label;
    }

    private void mostrarEstado(String mensaje, Color color) {
        lblMensaje.setText(mensaje);
        lblMensaje.setForeground(color);
    }

    private void mostrarAdvertencia(String mensaje) {
        mostrarEstado(mensaje, AMARILLO);
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Validación",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void iniciarReloj() {
        actualizarFechaHora();
        Timer timer = new Timer(1000, e -> actualizarFechaHora());
        timer.start();
    }

    private void actualizarFechaHora() {
        LocalDateTime ahora = LocalDateTime.now();
        lblHora.setText(ahora.format(DateTimeFormatter.ofPattern("hh:mm a")));
        lblFecha.setText(ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private void volverMenu() {
        new MenuAdministradorCINEXGUI(usuarioActual).setVisible(true);
        dispose();
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

    private double convertirDouble(Object valor) {
        if (valor instanceof Number) {
            return ((Number) valor).doubleValue();
        }

        try {
            return Double.parseDouble(valorSeguro(valor).replace(",", "."));
        } catch (NumberFormatException e) {
            return 0;
        }
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

            BufferedImage escalada = new BufferedImage(
                    ancho,
                    alto,
                    BufferedImage.TYPE_INT_ARGB
            );

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

    private class HeaderRenderer extends DefaultTableCellRenderer {
        public HeaderRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBackground(AZUL_HEADER);
            setForeground(BLANCO);
            setFont(new Font("Arial", Font.BOLD, 14));
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
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column
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
            setBorder(new EmptyBorder(0, 12, 0, 12));
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
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column
            );

            if (isSelected) {
                setBackground(AMARILLO);
                setForeground(new Color(5, 20, 55));
            } else {
                setBackground(row % 2 == 0 ? AZUL_TABLA : AZUL_TABLA_ALTERNA);
                setForeground(BLANCO);
            }

            setHorizontalAlignment(column == 1
                    ? SwingConstants.LEFT
                    : SwingConstants.CENTER);

            setFont(new Font(
                    "Arial",
                    column == 1 ? Font.BOLD : Font.PLAIN,
                    14
            ));

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
            g2.drawRoundRect(
                    0,
                    0,
                    getWidth() - 1,
                    getHeight() - 1,
                    10,
                    10
            );

            g2.dispose();

            setForeground(isEnabled()
                    ? colorTexto
                    : new Color(170, 177, 190));

            super.paintComponent(g);
        }
    }

    private static Color aclarar(Color color, int cantidad) {
        return new Color(
                Math.min(255, color.getRed() + cantidad),
                Math.min(255, color.getGreen() + cantidad),
                Math.min(255, color.getBlue() + cantidad)
        );
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
                    0,
                    0,
                    getWidth() - 1,
                    getHeight() - 1,
                    radio,
                    radio
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
                    0,
                    0,
                    AZUL_FONDO_1,
                    getWidth(),
                    getHeight(),
                    AZUL_FONDO_2
            );

            g2.setPaint(degradado);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 255, 255, 7));
            g2.fillOval(-180, -110, 530, 530);
            g2.fillOval(getWidth() - 390, 240, 360, 360);

            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {
            }

            new PreciosAdminCINEXGUI("admin").setVisible(true);
        });
    }
}
