package interfaz;

import control.ControlNotificacionesCINEX;
import control.ControlReembolsosCINEX;
import control.ControlReembolsosCINEX.ResultadoReembolso;
import entidad.EntradaReembolsoCINEX;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReembolsoEntradasCINEXGUI extends JFrame {

    private static final Color FONDO = new Color(3, 15, 36);
    private static final Color PANEL = new Color(7, 28, 65);
    private static final Color PANEL_2 = new Color(10, 40, 85);
    private static final Color BORDE = new Color(63, 96, 145);
    private static final Color AMARILLO = new Color(245, 196, 0);
    private static final Color BLANCO = new Color(245, 247, 252);
    private static final Color GRIS = new Color(185, 195, 210);
    private static final Color VERDE = new Color(35, 180, 85);
    private static final Color ROJO = new Color(225, 70, 70);
    private static final Color AZUL_BOTON = new Color(0, 80, 160);

    private final String usuarioActual;
    private final ControlReembolsosCINEX control =
            new ControlReembolsosCINEX();
    private final ControlNotificacionesCINEX controlNotificaciones =
            new ControlNotificacionesCINEX();

    private JTextField txtBuscarFuncion;
    private JTable tablaFunciones;
    private DefaultTableModel modeloFunciones;
    private JLabel lblAutorizacion;

    private JComboBox<String> cbTipoDocumento;
    private JTextField txtDocumento;
    private JButton btnBuscarCliente;
    private JTable tablaEntradas;
    private DefaultTableModel modeloEntradas;
    private JLabel lblCompra;
    private JLabel lblSeleccion;
    private JButton btnReembolsar;

    private int idFuncionSeleccionada = -1;
    private boolean reembolsoPermitido = false;

    private final Map<Integer, EntradaReembolsoCINEX> entradasPorId =
            new HashMap<>();

    public ReembolsoEntradasCINEXGUI(String usuario) {
        usuarioActual = usuario == null || usuario.trim().isEmpty()
                ? "taquillero"
                : usuario.trim();

        setTitle("CINEX - Reembolso de entradas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1440, 850);
        setMinimumSize(new Dimension(1180, 720));
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBackground(FONDO);
        root.setBorder(new EmptyBorder(18, 24, 18, 24));
        setContentPane(root);

        root.add(crearCabecera(), BorderLayout.NORTH);
        root.add(crearCentro(), BorderLayout.CENTER);
        root.add(crearFooter(), BorderLayout.SOUTH);

        configurarEventos();
        cargarFunciones();
        actualizarSeleccion();
    }

    private JPanel crearCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Reembolso de entradas");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 34));

        JLabel subtitulo = new JLabel(
                "Solo se reembolsan compras anteriores a la modificación y el pago se entrega en efectivo."
        );
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 16));

        textos.add(titulo);
        textos.add(Box.createVerticalStrut(4));
        textos.add(subtitulo);

        JLabel usuario = new JLabel("Taquillero: " + usuarioActual);
        usuario.setForeground(BLANCO);
        usuario.setFont(new Font("Arial", Font.BOLD, 15));

        cabecera.add(textos, BorderLayout.WEST);
        cabecera.add(usuario, BorderLayout.EAST);
        return cabecera;
    }

    private JPanel crearCentro() {
        JPanel centro = new JPanel(new GridLayout(2, 1, 0, 16));
        centro.setOpaque(false);

        centro.add(crearPanelFunciones());
        centro.add(crearPanelCompras());
        return centro;
    }

    private JPanel crearPanelFunciones() {
        JPanel panel = crearPanelBase();
        panel.setLayout(new BorderLayout(0, 12));

        JPanel cabecera = new JPanel(new BorderLayout(15, 0));
        cabecera.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("1. Buscar película y función");
        titulo.setForeground(AMARILLO);
        titulo.setFont(new Font("Arial", Font.BOLD, 21));

        JLabel ayuda = new JLabel(
                "Seleccione una función y verifique la decisión del gerente; las compras posteriores al cambio no son elegibles."
        );
        ayuda.setForeground(GRIS);
        ayuda.setFont(new Font("Arial", Font.PLAIN, 13));

        textos.add(titulo);
        textos.add(Box.createVerticalStrut(3));
        textos.add(ayuda);

        JPanel buscar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buscar.setOpaque(false);

        txtBuscarFuncion = crearCampoTexto();
        txtBuscarFuncion.setPreferredSize(new Dimension(310, 40));
        txtBuscarFuncion.setToolTipText(
                "Película, fecha, hora o sala"
        );

        JButton btnBuscar = crearBoton(
                "BUSCAR",
                AZUL_BOTON,
                BLANCO,
                130
        );

        buscar.add(txtBuscarFuncion);
        buscar.add(btnBuscar);

        cabecera.add(textos, BorderLayout.WEST);
        cabecera.add(buscar, BorderLayout.EAST);

        modeloFunciones = new DefaultTableModel(
                new Object[]{
                        "ID",
                        "Película",
                        "Fecha",
                        "Hora",
                        "Sala",
                        "Estado",
                        "Entradas",
                        "Autorización"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaFunciones = new JTable(modeloFunciones);
        configurarTabla(tablaFunciones);
        tablaFunciones.setRowHeight(38);
        tablaFunciones.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tablaFunciones.getColumnModel()
                .getColumn(0)
                .setMaxWidth(65);
        tablaFunciones.getColumnModel()
                .getColumn(1)
                .setPreferredWidth(280);
        tablaFunciones.getColumnModel()
                .getColumn(7)
                .setPreferredWidth(140);

        JScrollPane scroll = crearScroll(tablaFunciones);

        lblAutorizacion = new JLabel(
                "Seleccione una función para verificar su autorización."
        );
        lblAutorizacion.setForeground(GRIS);
        lblAutorizacion.setFont(
                new Font("Arial", Font.BOLD, 13)
        );

        JButton btnVerificar = crearBoton(
                "VERIFICAR Y CONTINUAR",
                AMARILLO,
                Color.BLACK,
                215
        );

        JPanel pie = new JPanel(new BorderLayout(12, 0));
        pie.setOpaque(false);
        pie.add(lblAutorizacion, BorderLayout.CENTER);
        pie.add(btnVerificar, BorderLayout.EAST);

        panel.add(cabecera, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(pie, BorderLayout.SOUTH);

        btnBuscar.addActionListener(e -> cargarFunciones());
        txtBuscarFuncion.addActionListener(e -> cargarFunciones());
        btnVerificar.addActionListener(e -> verificarAutorizacion());

        return panel;
    }

    private JPanel crearPanelCompras() {
        JPanel panel = crearPanelBase();
        panel.setLayout(new BorderLayout(0, 12));

        JPanel cabecera = new JPanel(new BorderLayout(15, 0));
        cabecera.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("2. Buscar cliente y seleccionar entradas");
        titulo.setForeground(AMARILLO);
        titulo.setFont(new Font("Arial", Font.BOLD, 21));

        lblCompra = new JLabel(
                "Primero debe seleccionar una función con reembolso permitido."
        );
        lblCompra.setForeground(GRIS);
        lblCompra.setFont(new Font("Arial", Font.PLAIN, 13));

        textos.add(titulo);
        textos.add(Box.createVerticalStrut(3));
        textos.add(lblCompra);

        JPanel buscar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buscar.setOpaque(false);

        cbTipoDocumento = new JComboBox<>(
                new String[]{"DNI", "C.E."}
        );
        cbTipoDocumento.setPreferredSize(
                new Dimension(90, 40)
        );
        configurarComboDocumento(
                cbTipoDocumento
        );

        txtDocumento = crearCampoTexto();
        txtDocumento.setPreferredSize(
                new Dimension(205, 40)
        );

        btnBuscarCliente = crearBoton(
                "BUSCAR CLIENTE",
                AZUL_BOTON,
                BLANCO,
                165
        );
        btnBuscarCliente.setEnabled(false);

        buscar.add(cbTipoDocumento);
        buscar.add(txtDocumento);
        buscar.add(btnBuscarCliente);

        cabecera.add(textos, BorderLayout.WEST);
        cabecera.add(buscar, BorderLayout.EAST);

        modeloEntradas = new DefaultTableModel(
                new Object[]{
                        "Seleccionar",
                        "ID",
                        "Venta",
                        "Cliente",
                        "Documento",
                        "Asiento",
                        "Tipo",
                        "Precio",
                        "Estado",
                        "Pago original",
                        "Fecha compra"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (column != 0) {
                    return false;
                }

                Object estado = getValueAt(row, 8);
                return estado == null
                        || !"Reembolsada".equalsIgnoreCase(
                                estado.toString()
                        );
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Boolean.class;
                }
                if (columnIndex == 1) {
                    return Integer.class;
                }
                if (columnIndex == 7) {
                    return Double.class;
                }
                return String.class;
            }
        };

        tablaEntradas = new JTable(modeloEntradas);
        configurarTabla(tablaEntradas);
        tablaEntradas.setRowHeight(38);
        tablaEntradas.setAutoResizeMode(
                JTable.AUTO_RESIZE_OFF
        );

        int[] anchos = {
                90, 55, 120, 190, 110,
                85, 140, 90, 115, 120, 160
        };

        for (int i = 0; i < anchos.length; i++) {
            tablaEntradas.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(anchos[i]);
        }

        tablaEntradas.getColumnModel()
                .getColumn(1)
                .setMinWidth(0);
        tablaEntradas.getColumnModel()
                .getColumn(1)
                .setMaxWidth(0);
        tablaEntradas.getColumnModel()
                .getColumn(1)
                .setPreferredWidth(0);

        JScrollPane scroll = crearScroll(tablaEntradas);

        lblSeleccion = new JLabel(
                "Entradas seleccionadas: 0 | Monto: S/ 0.00"
        );
        lblSeleccion.setForeground(GRIS);
        lblSeleccion.setFont(
                new Font("Arial", Font.BOLD, 14)
        );

        btnReembolsar = crearBoton(
                "REEMBOLSAR EN EFECTIVO",
                new Color(35, 145, 85),
                BLANCO,
                235
        );
        btnReembolsar.setEnabled(false);

        JPanel pie = new JPanel(new BorderLayout(12, 0));
        pie.setOpaque(false);
        pie.add(lblSeleccion, BorderLayout.CENTER);
        pie.add(btnReembolsar, BorderLayout.EAST);

        panel.add(cabecera, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(pie, BorderLayout.SOUTH);

        btnBuscarCliente.addActionListener(e -> buscarCliente());
        txtDocumento.addActionListener(e -> buscarCliente());
        btnReembolsar.addActionListener(e -> aplicarReembolso());

        return panel;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        JLabel regla = new JLabel(
                "La venta original permanece registrada. "
                        + "Las entradas reembolsadas liberan sus asientos."
        );
        regla.setForeground(GRIS);
        regla.setFont(new Font("Arial", Font.PLAIN, 13));

        JButton btnMenu = crearBoton(
                "MENÚ PRINCIPAL",
                AZUL_BOTON,
                BLANCO,
                185
        );
        btnMenu.addActionListener(e ->
                CINEXTransiciones.cambiar(
                        this,
                        new MenuTaquilleroCINEXGUI(usuarioActual)
                )
        );

        footer.add(regla, BorderLayout.WEST);
        footer.add(btnMenu, BorderLayout.EAST);
        return footer;
    }

    private void configurarEventos() {
        tablaFunciones.getSelectionModel()
                .addListSelectionListener(e -> {
                    if (!e.getValueIsAdjusting()) {
                        idFuncionSeleccionada = obtenerIdFuncionTabla();
                        reembolsoPermitido = false;
                        btnBuscarCliente.setEnabled(false);
                        modeloEntradas.setRowCount(0);
                        entradasPorId.clear();
                        actualizarSeleccion();

                        if (idFuncionSeleccionada > 0) {
                            lblAutorizacion.setText(
                                    "Presione VERIFICAR Y CONTINUAR."
                            );
                            lblAutorizacion.setForeground(AMARILLO);
                        }
                    }
                });

        modeloEntradas.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                actualizarSeleccion();
            }
        });
    }

    private void cargarFunciones() {
        try {
            ArrayList<Object[]> datos =
                    control.listarFuncionesConVentas(
                            txtBuscarFuncion == null
                                    ? ""
                                    : txtBuscarFuncion.getText()
                    );

            modeloFunciones.setRowCount(0);

            for (Object[] fila : datos) {
                modeloFunciones.addRow(fila);
            }

            idFuncionSeleccionada = -1;
            reembolsoPermitido = false;
            btnBuscarCliente.setEnabled(false);
            modeloEntradas.setRowCount(0);
            entradasPorId.clear();

            lblAutorizacion.setText(
                    datos.isEmpty()
                            ? "No existen funciones con ventas para el filtro."
                            : "Funciones encontradas: " + datos.size()
            );
            lblAutorizacion.setForeground(
                    datos.isEmpty()
                            ? AMARILLO
                            : VERDE
            );

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Reembolsos",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private int obtenerIdFuncionTabla() {
        int fila = tablaFunciones.getSelectedRow();

        if (fila < 0) {
            return -1;
        }

        int modeloFila =
                tablaFunciones.convertRowIndexToModel(fila);

        Object valor =
                modeloFunciones.getValueAt(modeloFila, 0);

        try {
            return Integer.parseInt(valor.toString());
        } catch (Exception e) {
            return -1;
        }
    }

    private void verificarAutorizacion() {
        idFuncionSeleccionada = obtenerIdFuncionTabla();

        if (idFuncionSeleccionada <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Seleccione una función.",
                    "Función requerida",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        ControlNotificacionesCINEX.EstadoAutorizacion estado =
                controlNotificaciones
                        .obtenerAutorizacionFuncion(
                                idFuncionSeleccionada
                        );

        reembolsoPermitido = estado.estaPermitido();
        btnBuscarCliente.setEnabled(reembolsoPermitido);

        if (reembolsoPermitido) {
            lblAutorizacion.setText(
                    "Reembolso permitido por el gerente. "
                            + "Busque al cliente por DNI o C.E."
            );
            lblAutorizacion.setForeground(VERDE);
            lblCompra.setText(
                    "Autorización aprobada para: "
                            + estado.getElemento()
            );
            lblCompra.setForeground(VERDE);
            txtDocumento.requestFocusInWindow();

        } else if (ControlNotificacionesCINEX.PENDIENTE
                .equalsIgnoreCase(estado.getEstado())) {
            lblAutorizacion.setText(
                    "La decisión del gerente todavía está pendiente."
            );
            lblAutorizacion.setForeground(AMARILLO);

            JOptionPane.showMessageDialog(
                    this,
                    "La solicitud de reembolso está pendiente "
                            + "de revisión por el gerente.",
                    "Autorización pendiente",
                    JOptionPane.WARNING_MESSAGE
            );

        } else {
            lblAutorizacion.setText(
                    "Reembolso no habilitado. Estado: "
                            + estado.getEstado()
            );
            lblAutorizacion.setForeground(ROJO);

            JOptionPane.showMessageDialog(
                    this,
                    "El gerente no ha autorizado reembolsos "
                            + "para esta película o función.",
                    "Reembolso no permitido",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void buscarCliente() {
        if (!reembolsoPermitido
                || idFuncionSeleccionada <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Primero verifique que el gerente haya "
                            + "permitido el reembolso.",
                    "Autorización requerida",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String documento = txtDocumento.getText().trim();

        if (documento.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ingrese el DNI o C.E. del cliente.",
                    "Documento requerido",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            ArrayList<EntradaReembolsoCINEX> entradas =
                    control.buscarEntradasCliente(
                            documento,
                            idFuncionSeleccionada
                    );

            modeloEntradas.setRowCount(0);
            entradasPorId.clear();

            for (EntradaReembolsoCINEX entrada : entradas) {
                entradasPorId.put(
                        entrada.getIdEntrada(),
                        entrada
                );

                modeloEntradas.addRow(new Object[]{
                        Boolean.FALSE,
                        entrada.getIdEntrada(),
                        entrada.getNumeroVenta(),
                        entrada.getCliente(),
                        entrada.getDocumento(),
                        entrada.getAsiento(),
                        entrada.getTipoEntrada(),
                        entrada.getPrecioOriginal(),
                        entrada.getEstadoEntrada(),
                        entrada.getMetodoPagoOriginal(),
                        entrada.getFechaVenta()
                });
            }

            if (entradas.isEmpty()) {
                lblCompra.setText(
                        "No hay entradas reembolsables del cliente. "
                                + "Solo califican las compradas antes "
                                + "de la modificación."
                );
                lblCompra.setForeground(AMARILLO);

                JOptionPane.showMessageDialog(
                        this,
                        "No se encontraron entradas elegibles.\n\n"
                                + "Solo se pueden reembolsar las entradas "
                                + "compradas antes de la fecha y hora "
                                + "de la modificación autorizada.",
                        "Compra no encontrada",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else {
                EntradaReembolsoCINEX primera = entradas.get(0);

                lblCompra.setText(
                        "Cliente: " + primera.getCliente()
                                + " | Compra: "
                                + primera.getNumeroVenta()
                                + " | Total original: S/ "
                                + String.format(
                                        "%.2f",
                                        primera.getTotalVenta()
                                )
                                + " | Seleccione asientos de una sola venta."
                );
                lblCompra.setForeground(VERDE);
            }

            actualizarSeleccion();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "No se pudo consultar la compra",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void actualizarSeleccion() {
        int cantidad = 0;
        double monto = 0.0;

        if (modeloEntradas != null) {
            for (int fila = 0;
                 fila < modeloEntradas.getRowCount();
                 fila++) {

                if (Boolean.TRUE.equals(
                        modeloEntradas.getValueAt(fila, 0)
                )) {
                    cantidad++;

                    Object valor =
                            modeloEntradas.getValueAt(fila, 7);

                    if (valor instanceof Number) {
                        monto += ((Number) valor).doubleValue();
                    }
                }
            }
        }

        if (lblSeleccion != null) {
            lblSeleccion.setText(
                    "Entradas seleccionadas: " + cantidad
                            + " | Monto a devolver: S/ "
                            + String.format("%.2f", monto)
                            + " | Método: EFECTIVO"
            );
        }

        if (btnReembolsar != null) {
            btnReembolsar.setEnabled(
                    reembolsoPermitido
                            && cantidad > 0
            );
        }
    }

    private List<Integer> obtenerEntradasSeleccionadas() {
        ArrayList<Integer> ids = new ArrayList<>();

        for (int fila = 0;
             fila < modeloEntradas.getRowCount();
             fila++) {

            if (Boolean.TRUE.equals(
                    modeloEntradas.getValueAt(fila, 0)
            )) {
                Object id = modeloEntradas.getValueAt(fila, 1);

                if (id instanceof Number) {
                    ids.add(((Number) id).intValue());
                } else {
                    try {
                        ids.add(Integer.parseInt(id.toString()));
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return ids;
    }

    private void aplicarReembolso() {
        List<Integer> ids = obtenerEntradasSeleccionadas();

        if (ids.isEmpty()) {
            return;
        }

        double monto = 0.0;
        StringBuilder asientos = new StringBuilder();
        String numeroVenta = "";

        for (Integer id : ids) {
            EntradaReembolsoCINEX entrada =
                    entradasPorId.get(id);

            if (entrada == null) {
                continue;
            }

            monto += entrada.getPrecioOriginal();

            if (asientos.length() > 0) {
                asientos.append(", ");
            }

            asientos.append(entrada.getAsiento());

            if (numeroVenta.isEmpty()) {
                numeroVenta = entrada.getNumeroVenta();
            } else if (!numeroVenta.equals(
                    entrada.getNumeroVenta()
            )) {
                JOptionPane.showMessageDialog(
                        this,
                        "Seleccione entradas pertenecientes "
                                + "a una sola venta.",
                        "Ventas diferentes",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
        }

        int confirmar = JOptionPane.showConfirmDialog(
                this,
                "¿Desea registrar el reembolso?\n\n"
                        + "Venta: " + numeroVenta + "\n"
                        + "Asientos: " + asientos + "\n"
                        + "Entradas: " + ids.size() + "\n"
                        + "Monto: S/ "
                        + String.format("%.2f", monto)
                        + "\nMétodo de devolución: EFECTIVO",
                "Confirmar reembolso",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }

        btnReembolsar.setEnabled(false);

        SwingWorker<ResultadoReembolso, Void> worker =
                new SwingWorker<ResultadoReembolso, Void>() {
                    @Override
                    protected ResultadoReembolso doInBackground() {
                        return control.aplicarReembolso(
                                txtDocumento.getText().trim(),
                                idFuncionSeleccionada,
                                ids,
                                usuarioActual
                        );
                    }

                    @Override
                    protected void done() {
                        try {
                            ResultadoReembolso resultado = get();

                            if (!resultado.isExito()) {
                                JOptionPane.showMessageDialog(
                                        ReembolsoEntradasCINEXGUI.this,
                                        resultado.getMensaje(),
                                        "No se pudo reembolsar",
                                        JOptionPane.ERROR_MESSAGE
                                );
                                actualizarSeleccion();
                                return;
                            }

                            JOptionPane.showMessageDialog(
                                    ReembolsoEntradasCINEXGUI.this,
                                    resultado.getMensaje(),
                                    "Reembolso registrado",
                                    JOptionPane.INFORMATION_MESSAGE
                            );

                            buscarCliente();
                            cargarFunciones();

                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(
                                    ReembolsoEntradasCINEXGUI.this,
                                    ex.getMessage(),
                                    "Error de reembolso",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        } finally {
                            actualizarSeleccion();
                        }
                    }
                };

        worker.execute();
    }

    private JPanel crearPanelBase() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDE, 1, true),
                new EmptyBorder(15, 17, 15, 17)
        ));
        return panel;
    }

    private JTextField crearCampoTexto() {
        JTextField campo = new JTextField();
        campo.setBackground(PANEL_2);
        campo.setForeground(BLANCO);
        campo.setCaretColor(BLANCO);
        campo.setFont(new Font("Arial", Font.BOLD, 14));
        campo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDE, 1, true),
                new EmptyBorder(0, 10, 0, 10)
        ));
        return campo;
    }

    private JButton crearBoton(
            String texto,
            Color fondo,
            Color colorTexto,
            int ancho
    ) {
        JButton boton = new JButton(texto);
        boton.setPreferredSize(new Dimension(ancho, 42));
        boton.setBackground(fondo);
        boton.setForeground(colorTexto);
        boton.setFont(new Font("Arial", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return boton;
    }

    private void configurarTabla(JTable tabla) {
        tabla.setBackground(PANEL);
        tabla.setForeground(BLANCO);
        tabla.setSelectionBackground(AMARILLO);
        tabla.setSelectionForeground(Color.BLACK);
        tabla.setGridColor(BORDE);
        tabla.setFillsViewportHeight(true);
        tabla.setShowGrid(true);
        tabla.setFont(new Font("Arial", Font.PLAIN, 13));

        JTableHeader header = tabla.getTableHeader();
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());

        tabla.setDefaultRenderer(
                Object.class,
                new CellRenderer()
        );
        tabla.setDefaultRenderer(
                Double.class,
                new CellRenderer()
        );
        tabla.setDefaultRenderer(
                Boolean.class,
                tabla.getDefaultRenderer(Boolean.class)
        );
    }

    private JScrollPane crearScroll(JTable tabla) {
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new LineBorder(BORDE, 1));
        scroll.setOpaque(true);
        scroll.setBackground(PANEL);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(PANEL);

        scroll.getVerticalScrollBar().setOpaque(true);
        scroll.getVerticalScrollBar().setBackground(PANEL);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setUI(
                new ScrollBarReembolsoUI()
        );

        scroll.getHorizontalScrollBar().setOpaque(true);
        scroll.getHorizontalScrollBar().setBackground(PANEL);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUI(
                new ScrollBarReembolsoUI()
        );

        JPanel esquinaSuperior = new JPanel();
        esquinaSuperior.setBackground(PANEL_2);

        JPanel esquinaInferior = new JPanel();
        esquinaInferior.setBackground(PANEL);

        scroll.setCorner(
                JScrollPane.UPPER_RIGHT_CORNER,
                esquinaSuperior
        );
        scroll.setCorner(
                JScrollPane.LOWER_RIGHT_CORNER,
                esquinaInferior
        );
        scroll.setCorner(
                JScrollPane.LOWER_LEFT_CORNER,
                esquinaInferior
        );

        return scroll;
    }

    private void configurarComboDocumento(
            JComboBox<String> combo
    ) {
        combo.setBackground(PANEL_2);
        combo.setForeground(BLANCO);
        combo.setFont(
                new Font("Arial", Font.BOLD, 13)
        );
        combo.setFocusable(false);
        combo.setOpaque(true);
        combo.setBorder(
                new LineBorder(BORDE, 1, true)
        );

        combo.setRenderer(
                new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(
                            JList<?> list,
                            Object value,
                            int index,
                            boolean isSelected,
                            boolean cellHasFocus
                    ) {
                        JLabel label =
                                (JLabel) super
                                        .getListCellRendererComponent(
                                                list,
                                                value,
                                                index,
                                                isSelected,
                                                cellHasFocus
                                        );

                        label.setOpaque(true);
                        label.setBorder(
                                new EmptyBorder(
                                        8,
                                        10,
                                        8,
                                        10
                                )
                        );
                        label.setFont(
                                new Font(
                                        "Arial",
                                        Font.BOLD,
                                        13
                                )
                        );

                        if (!combo.isEnabled()) {
                            label.setBackground(PANEL_2);
                            label.setForeground(GRIS);
                        } else if (isSelected) {
                            label.setBackground(AMARILLO);
                            label.setForeground(Color.BLACK);
                        } else {
                            label.setBackground(PANEL_2);
                            label.setForeground(BLANCO);
                        }

                        list.setBackground(PANEL_2);
                        list.setForeground(BLANCO);
                        list.setSelectionBackground(AMARILLO);
                        list.setSelectionForeground(Color.BLACK);

                        return label;
                    }
                }
        );

        combo.setUI(
                new BasicComboBoxUI() {
                    @Override
                    protected JButton createArrowButton() {
                        JButton boton = new JButton() {
                            @Override
                            protected void paintComponent(
                                    Graphics g
                            ) {
                                Graphics2D g2 =
                                        (Graphics2D) g.create();

                                g2.setRenderingHint(
                                        RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON
                                );

                                g2.setColor(PANEL_2);
                                g2.fillRect(
                                        0,
                                        0,
                                        getWidth(),
                                        getHeight()
                                );

                                g2.setColor(BORDE);
                                g2.drawLine(
                                        0,
                                        0,
                                        0,
                                        getHeight()
                                );

                                int cx = getWidth() / 2;
                                int cy = getHeight() / 2 + 1;

                                Polygon flecha = new Polygon();
                                flecha.addPoint(
                                        cx - 6,
                                        cy - 4
                                );
                                flecha.addPoint(
                                        cx + 6,
                                        cy - 4
                                );
                                flecha.addPoint(
                                        cx,
                                        cy + 5
                                );

                                g2.setColor(
                                        combo.isEnabled()
                                                ? AMARILLO
                                                : GRIS
                                );
                                g2.fillPolygon(flecha);
                                g2.dispose();
                            }
                        };

                        boton.setPreferredSize(
                                new Dimension(32, 40)
                        );
                        boton.setBorderPainted(false);
                        boton.setContentAreaFilled(false);
                        boton.setFocusPainted(false);
                        boton.setOpaque(false);

                        return boton;
                    }
                }
        );

        combo.addPropertyChangeListener(
                "enabled",
                e -> {
                    combo.setBackground(PANEL_2);
                    combo.setForeground(
                            combo.isEnabled()
                                    ? BLANCO
                                    : GRIS
                    );
                    combo.repaint();
                }
        );
    }

    private class ScrollBarReembolsoUI
            extends BasicScrollBarUI {

        @Override
        protected void configureScrollBarColors() {
            trackColor = PANEL;
            thumbColor = new Color(70, 105, 155);
            thumbDarkShadowColor = BORDE;
            thumbHighlightColor = new Color(95, 130, 180);
            thumbLightShadowColor = BORDE;
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
            boton.setOpaque(false);
            boton.setBorder(null);
            boton.setContentAreaFilled(false);

            return boton;
        }

        @Override
        protected void paintTrack(
                Graphics g,
                JComponent component,
                Rectangle bounds
        ) {
            g.setColor(PANEL);
            g.fillRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height
            );
        }

        @Override
        protected void paintThumb(
                Graphics g,
                JComponent component,
                Rectangle bounds
        ) {
            if (!component.isEnabled()
                    || bounds.width <= 0
                    || bounds.height <= 0) {
                return;
            }

            Graphics2D g2 =
                    (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(
                    new Color(70, 105, 155)
            );

            g2.fillRoundRect(
                    bounds.x + 2,
                    bounds.y + 2,
                    Math.max(1, bounds.width - 4),
                    Math.max(1, bounds.height - 4),
                    8,
                    8
            );

            g2.dispose();
        }
    }

    private class HeaderRenderer
            extends DefaultTableCellRenderer {
        HeaderRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBackground(PANEL_2);
            setForeground(BLANCO);
            setFont(new Font("Arial", Font.BOLD, 13));
            setBorder(new LineBorder(BORDE, 1));
        }
    }

    private class CellRenderer
            extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            JLabel label =
                    (JLabel) super
                            .getTableCellRendererComponent(
                                    table,
                                    value,
                                    isSelected,
                                    hasFocus,
                                    row,
                                    column
                            );

            label.setOpaque(true);
            label.setBorder(new EmptyBorder(0, 7, 0, 7));
            label.setHorizontalAlignment(
                    column == 1
                            || column == 6
                            || column == 7
                            ? SwingConstants.LEFT
                            : SwingConstants.CENTER
            );

            if (isSelected) {
                label.setBackground(AMARILLO);
                label.setForeground(Color.BLACK);
            } else {
                label.setBackground(
                        row % 2 == 0
                                ? PANEL
                                : new Color(9, 34, 76)
                );

                String texto = value == null
                        ? ""
                        : value.toString();

                if ("Permitido".equalsIgnoreCase(texto)) {
                    label.setForeground(VERDE);
                } else if ("No permitido".equalsIgnoreCase(texto)
                        || "Reembolsada".equalsIgnoreCase(texto)) {
                    label.setForeground(ROJO);
                } else if ("Pendiente".equalsIgnoreCase(texto)) {
                    label.setForeground(AMARILLO);
                } else {
                    label.setForeground(BLANCO);
                }
            }

            if (value instanceof Double) {
                label.setText(
                        "S/ "
                                + String.format(
                                        "%.2f",
                                        (Double) value
                                )
                );
            }

            return label;
        }
    }
}
