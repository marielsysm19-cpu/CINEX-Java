package interfaz;

import control.ControlNotificacionesCINEX;
import entidad.NotificacionCambioCINEX;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;

public class NotificacionesGerenteCINEXGUI extends JFrame {

    private static final Color FONDO = new Color(3, 15, 36);
    private static final Color PANEL = new Color(7, 28, 65);
    private static final Color PANEL_2 = new Color(10, 40, 85);
    private static final Color BORDE = new Color(63, 96, 145);
    private static final Color AMARILLO = new Color(245, 196, 0);
    private static final Color BLANCO = new Color(245, 247, 252);
    private static final Color GRIS = new Color(185, 195, 210);
    private static final Color VERDE = new Color(35, 180, 85);
    private static final Color ROJO = new Color(225, 70, 70);

    private final String usuarioActual;
    private final ControlNotificacionesCINEX control =
            new ControlNotificacionesCINEX();

    private JTable tabla;
    private DefaultTableModel modelo;
    private JTextArea txtDetalle;
    private JLabel lblEstado;
    private JButton btnPermitir;
    private JButton btnNoPermitir;

    private final ArrayList<NotificacionCambioCINEX> notificaciones =
            new ArrayList<>();

    public NotificacionesGerenteCINEXGUI(String usuario) {
        usuarioActual = usuario == null || usuario.trim().isEmpty()
                ? "gerente"
                : usuario.trim();

        setTitle("CINEX - Visualizar notificaciones de reembolso");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1366, 768);
        setMinimumSize(new Dimension(1100, 680));
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel root = new JPanel(new BorderLayout(18, 18));
        root.setBackground(FONDO);
        root.setBorder(new EmptyBorder(22, 28, 22, 28));
        setContentPane(root);

        root.add(crearCabecera(), BorderLayout.NORTH);
        root.add(crearCentro(), BorderLayout.CENTER);
        root.add(crearFooter(), BorderLayout.SOUTH);

        configurarEventos();
        cargarNotificaciones();
    }

    private JPanel crearCabecera() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Visualizar notificaciones de reembolso");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 34));

        JLabel subtitulo = new JLabel(
                "Revise las modificaciones pendientes y decida si permiten reembolso."
        );
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 16));

        textos.add(titulo);
        textos.add(Box.createVerticalStrut(5));
        textos.add(subtitulo);

        JPanel filtros = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 8)
        );
        filtros.setOpaque(false);

        JLabel lbl = new JLabel(
                "Solo se muestran cambios pendientes"
        );
        lbl.setForeground(GRIS);
        lbl.setFont(new Font("Arial", Font.BOLD, 13));

        JButton btnRefrescar = crearBoton(
                "REFRESCAR",
                new Color(0, 80, 160),
                BLANCO,
                145
        );
        btnRefrescar.addActionListener(
                e -> cargarNotificaciones()
        );

        filtros.add(lbl);
        filtros.add(btnRefrescar);

        panel.add(textos, BorderLayout.WEST);
        panel.add(filtros, BorderLayout.EAST);
        return panel;
    }

    private JPanel crearCentro() {
        JPanel centro = new JPanel(new BorderLayout(18, 0));
        centro.setOpaque(false);

        JPanel izquierda = crearPanelTabla();
        izquierda.setPreferredSize(new Dimension(820, 0));

        JPanel derecha = crearPanelDetalle();
        derecha.setPreferredSize(new Dimension(420, 0));

        centro.add(izquierda, BorderLayout.CENTER);
        centro.add(derecha, BorderLayout.EAST);
        return centro;
    }

    private JPanel crearPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDE, 1, true),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JLabel titulo = new JLabel("Cambios pendientes");
        titulo.setForeground(AMARILLO);
        titulo.setFont(new Font("Arial", Font.BOLD, 21));

        modelo = new DefaultTableModel(
                new Object[]{
                        "ID",
                        "Tipo",
                        "Elemento",
                        "Administrador",
                        "Fecha",
                        "Reembolso"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabla = new JTable(modelo);
        tabla.setRowHeight(42);
        tabla.setBackground(PANEL);
        tabla.setForeground(BLANCO);
        tabla.setSelectionBackground(AMARILLO);
        tabla.setSelectionForeground(Color.BLACK);
        tabla.setGridColor(BORDE);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        JTableHeader header = tabla.getTableHeader();
        header.setPreferredSize(new Dimension(0, 40));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());

        tabla.setDefaultRenderer(Object.class, new CellRenderer());

        tabla.getColumnModel().getColumn(0).setPreferredWidth(50);
        tabla.getColumnModel().getColumn(0).setMaxWidth(70);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(300);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(155);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(130);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new LineBorder(BORDE, 1));
        scroll.getViewport().setBackground(PANEL);

        lblEstado = new JLabel("Cargando notificaciones...");
        lblEstado.setForeground(GRIS);
        lblEstado.setFont(new Font("Arial", Font.BOLD, 13));

        panel.add(titulo, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(lblEstado, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelDetalle() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDE, 1, true),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JLabel titulo = new JLabel("Detalle y autorización");
        titulo.setForeground(AMARILLO);
        titulo.setFont(new Font("Arial", Font.BOLD, 21));

        txtDetalle = new JTextArea();
        txtDetalle.setEditable(false);
        txtDetalle.setLineWrap(true);
        txtDetalle.setWrapStyleWord(true);
        txtDetalle.setBackground(PANEL_2);
        txtDetalle.setForeground(BLANCO);
        txtDetalle.setCaretColor(BLANCO);
        txtDetalle.setFont(new Font("Arial", Font.PLAIN, 14));
        txtDetalle.setBorder(new EmptyBorder(14, 14, 14, 14));
        txtDetalle.setText(
                "Seleccione una notificación para consultar "
                        + "el cambio realizado."
        );

        JScrollPane scroll = new JScrollPane(txtDetalle);
        scroll.setBorder(new LineBorder(BORDE, 1));
        scroll.getViewport().setBackground(PANEL_2);

        btnPermitir = crearBoton(
                "PERMITIR REEMBOLSO",
                VERDE,
                BLANCO,
                185
        );

        btnNoPermitir = crearBoton(
                "NO PERMITIR",
                new Color(155, 45, 55),
                BLANCO,
                170
        );

        btnPermitir.setEnabled(false);
        btnNoPermitir.setEnabled(false);

        JPanel acciones = new JPanel(new GridLayout(1, 2, 12, 0));
        acciones.setOpaque(false);
        acciones.add(btnNoPermitir);
        acciones.add(btnPermitir);

        panel.add(titulo, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        JButton btnMenu = crearBoton(
                "MENÚ GERENTE",
                new Color(0, 80, 160),
                BLANCO,
                180
        );
        btnMenu.setPreferredSize(new Dimension(180, 44));
        btnMenu.addActionListener(e ->
                CINEXTransiciones.cambiar(
                        this,
                        new MenuGerenteCINEXGUI(usuarioActual)
                )
        );

        JLabel regla = new JLabel(
                "La autorización solo habilita el proceso; el cliente debe acercarse a taquilla."
        );
        regla.setForeground(GRIS);
        regla.setFont(new Font("Arial", Font.PLAIN, 13));

        footer.add(btnMenu, BorderLayout.WEST);
        footer.add(regla, BorderLayout.EAST);
        return footer;
    }

    private void configurarEventos() {
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarSeleccion();
            }
        });

        btnPermitir.addActionListener(e -> resolver(true));
        btnNoPermitir.addActionListener(e -> resolver(false));
    }

    private void cargarNotificaciones() {
        try {
            notificaciones.clear();
            notificaciones.addAll(
                    control.listarNotificaciones(
                            ControlNotificacionesCINEX.PENDIENTE
                    )
            );

            modelo.setRowCount(0);

            for (NotificacionCambioCINEX item : notificaciones) {
                modelo.addRow(new Object[]{
                        item.getIdNotificacion(),
                        item.getTipoElemento(),
                        item.getTituloElemento(),
                        item.getUsuarioAdmin(),
                        item.getFechaCambio(),
                        item.getEstadoReembolso()
                });
            }

            lblEstado.setText(
                    notificaciones.isEmpty()
                            ? "No existen cambios pendientes de revisión."
                            : "Notificaciones encontradas: "
                                    + notificaciones.size()
            );
            lblEstado.setForeground(
                    notificaciones.isEmpty()
                            ? AMARILLO
                            : VERDE
            );

            txtDetalle.setText(
                    "Seleccione una notificación para consultar "
                            + "el cambio realizado."
            );
            btnPermitir.setEnabled(false);
            btnNoPermitir.setEnabled(false);

        } catch (Exception ex) {
            lblEstado.setText("No se pudieron cargar las notificaciones.");
            lblEstado.setForeground(ROJO);

            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Notificaciones",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void mostrarSeleccion() {
        int fila = tabla.getSelectedRow();

        if (fila < 0) {
            btnPermitir.setEnabled(false);
            btnNoPermitir.setEnabled(false);
            return;
        }

        int modeloFila = tabla.convertRowIndexToModel(fila);
        int id = Integer.parseInt(
                modelo.getValueAt(modeloFila, 0).toString()
        );

        NotificacionCambioCINEX seleccion = buscarPorId(id);

        if (seleccion == null) {
            return;
        }

        StringBuilder detalle = new StringBuilder();
        detalle.append("Cambio: ")
                .append(seleccion.getDescripcion())
                .append("\n\nElemento: ")
                .append(seleccion.getTituloElemento())
                .append("\nTipo: ")
                .append(seleccion.getTipoElemento())
                .append("\nAdministrador: ")
                .append(seleccion.getUsuarioAdmin())
                .append("\nFecha: ")
                .append(seleccion.getFechaCambio())
                .append("\n\nDATOS ANTERIORES\n")
                .append(seleccion.getDatosAnteriores().isEmpty()
                        ? "No registrados"
                        : seleccion.getDatosAnteriores())
                .append("\n\nDATOS NUEVOS\n")
                .append(seleccion.getDatosNuevos().isEmpty()
                        ? "No registrados"
                        : seleccion.getDatosNuevos())
                .append("\n\nEstado de reembolso: ")
                .append(seleccion.getEstadoReembolso());

        if (!seleccion.getUsuarioGerente().isEmpty()) {
            detalle.append("\nDecisión tomada por: ")
                    .append(seleccion.getUsuarioGerente())
                    .append("\nFecha de decisión: ")
                    .append(seleccion.getFechaDecision());
        }

        txtDetalle.setText(detalle.toString());
        txtDetalle.setCaretPosition(0);

        btnPermitir.setEnabled(true);
        btnNoPermitir.setEnabled(true);
    }

    private NotificacionCambioCINEX buscarPorId(int id) {
        for (NotificacionCambioCINEX item : notificaciones) {
            if (item.getIdNotificacion() == id) {
                return item;
            }
        }
        return null;
    }

    private void resolver(boolean permitir) {
        int fila = tabla.getSelectedRow();

        if (fila < 0) {
            return;
        }

        int modeloFila = tabla.convertRowIndexToModel(fila);
        int id = Integer.parseInt(
                modelo.getValueAt(modeloFila, 0).toString()
        );

        String accion = permitir
                ? "permitir el reembolso"
                : "no permitir el reembolso";

        int confirmar = JOptionPane.showConfirmDialog(
                this,
                "¿Desea " + accion + " para este cambio?",
                "Confirmar decisión",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            boolean actualizado = control.resolverNotificacion(
                    id,
                    permitir,
                    usuarioActual
            );

            if (!actualizado) {
                throw new RuntimeException(
                        "La notificación ya no existe."
                );
            }

            JOptionPane.showMessageDialog(
                    this,
                    permitir
                            ? "El reembolso fue permitido."
                            : "El reembolso no fue permitido.",
                    "Decisión registrada",
                    JOptionPane.INFORMATION_MESSAGE
            );

            cargarNotificaciones();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "No se pudo registrar la decisión",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private JButton crearBoton(
            String texto,
            Color fondo,
            Color colorTexto,
            int ancho
    ) {
        JButton boton = new JButton(texto);
        boton.setPreferredSize(new Dimension(ancho, 46));
        boton.setBackground(fondo);
        boton.setForeground(colorTexto);
        boton.setFont(new Font("Arial", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return boton;
    }

    private class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBackground(new Color(10, 40, 85));
            setForeground(BLANCO);
            setFont(new Font("Arial", Font.BOLD, 13));
            setBorder(new LineBorder(BORDE, 1));
        }
    }

    private class CellRenderer extends DefaultTableCellRenderer {
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
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column
            );

            label.setOpaque(true);
            label.setBorder(new EmptyBorder(0, 8, 0, 8));
            label.setHorizontalAlignment(
                    column == 2
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

                String estado = column == 5
                        ? String.valueOf(value)
                        : "";

                if (ControlNotificacionesCINEX.PERMITIDO
                        .equalsIgnoreCase(estado)) {
                    label.setForeground(VERDE);
                } else if (ControlNotificacionesCINEX.NO_PERMITIDO
                        .equalsIgnoreCase(estado)) {
                    label.setForeground(ROJO);
                } else if (ControlNotificacionesCINEX.PENDIENTE
                        .equalsIgnoreCase(estado)) {
                    label.setForeground(AMARILLO);
                } else {
                    label.setForeground(BLANCO);
                }
            }

            return label;
        }
    }
}
