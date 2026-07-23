package interfaz;

import control.ControlReembolsosCINEX;
import entidad.ResumenReembolsosCINEX;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;

public class DashboardGerenteCINEXGUI extends JFrame {

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
    private final ControlReembolsosCINEX control =
            new ControlReembolsosCINEX();

    private final JLabel lblVentasBrutas = crearValor();
    private final JLabel lblReembolsos = crearValor();
    private final JLabel lblVentasNetas = crearValor();
    private final JLabel lblEntradasVendidas = crearValor();
    private final JLabel lblEntradasReembolsadas = crearValor();
    private final JLabel lblEntradasVigentes = crearValor();
    private final JLabel lblParciales = crearValor();
    private final JLabel lblTotales = crearValor();
    private final JLabel lblPendientes = crearValor();

    private DefaultTableModel modelo;

    public DashboardGerenteCINEXGUI(String usuario) {
        usuarioActual = usuario == null || usuario.trim().isEmpty()
                ? "gerente"
                : usuario.trim();

        setTitle("CINEX - Visualizar dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1366, 768);
        setMinimumSize(new Dimension(1100, 680));
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel root = new JPanel(new BorderLayout(18, 18));
        root.setBackground(FONDO);
        root.setBorder(new EmptyBorder(22, 28, 22, 28));
        setContentPane(root);

        root.add(crearCabecera(), BorderLayout.NORTH);
        root.add(crearContenido(), BorderLayout.CENTER);
        root.add(crearFooter(), BorderLayout.SOUTH);

        cargarDashboard();
    }

    private JPanel crearCabecera() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Visualizar dashboard");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 34));

        JLabel subtitulo = new JLabel(
                "Resumen real de ventas, reembolsos y entradas vigentes."
        );
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 16));

        textos.add(titulo);
        textos.add(Box.createVerticalStrut(5));
        textos.add(subtitulo);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        acciones.setOpaque(false);

        JLabel usuario = new JLabel("Usuario: " + usuarioActual);
        usuario.setForeground(BLANCO);
        usuario.setFont(new Font("Arial", Font.BOLD, 14));

        acciones.add(usuario);

        panel.add(textos, BorderLayout.WEST);
        panel.add(acciones, BorderLayout.EAST);
        return panel;
    }

    private JPanel crearContenido() {
        JPanel contenido = new JPanel(new BorderLayout(0, 18));
        contenido.setOpaque(false);

        JPanel tarjetas = new JPanel(new GridLayout(3, 3, 14, 14));
        tarjetas.setOpaque(false);
        tarjetas.setPreferredSize(new Dimension(0, 330));

        tarjetas.add(crearTarjeta(
                "Ventas brutas",
                lblVentasBrutas,
                "Total histórico antes de devoluciones",
                AMARILLO
        ));
        tarjetas.add(crearTarjeta(
                "Monto reembolsado",
                lblReembolsos,
                "Dinero devuelto únicamente en efectivo",
                ROJO
        ));
        tarjetas.add(crearTarjeta(
                "Ventas netas",
                lblVentasNetas,
                "Ventas brutas menos reembolsos",
                VERDE
        ));
        tarjetas.add(crearTarjeta(
                "Entradas vendidas",
                lblEntradasVendidas,
                "Entradas emitidas históricamente",
                AMARILLO
        ));
        tarjetas.add(crearTarjeta(
                "Entradas reembolsadas",
                lblEntradasReembolsadas,
                "Asientos devueltos por clientes",
                ROJO
        ));
        tarjetas.add(crearTarjeta(
                "Entradas vigentes",
                lblEntradasVigentes,
                "Entradas válidas después de reembolsos",
                VERDE
        ));
        tarjetas.add(crearTarjeta(
                "Reembolsos parciales",
                lblParciales,
                "Solo parte de la compra fue devuelta",
                AMARILLO
        ));
        tarjetas.add(crearTarjeta(
                "Reembolsos totales",
                lblTotales,
                "Toda la compra fue devuelta",
                ROJO
        ));

        contenido.add(tarjetas, BorderLayout.NORTH);
        contenido.add(crearTablaReembolsos(), BorderLayout.CENTER);
        return contenido;
    }

    private JPanel crearTarjeta(
            String titulo,
            JLabel valor,
            String detalle,
            Color colorValor
    ) {
        JPanel tarjeta = new JPanel();
        tarjeta.setBackground(PANEL);
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDE, 1, true),
                new EmptyBorder(15, 18, 15, 18)
        ));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setForeground(GRIS);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 13));
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        valor.setForeground(colorValor);
        valor.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDetalle = new JLabel(detalle);
        lblDetalle.setForeground(new Color(150, 165, 190));
        lblDetalle.setFont(new Font("Arial", Font.PLAIN, 12));
        lblDetalle.setAlignmentX(Component.LEFT_ALIGNMENT);

        tarjeta.add(lblTitulo);
        tarjeta.add(Box.createVerticalStrut(7));
        tarjeta.add(valor);
        tarjeta.add(Box.createVerticalStrut(5));
        tarjeta.add(lblDetalle);

        return tarjeta;
    }

    private JPanel crearTablaReembolsos() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDE, 1, true),
                new EmptyBorder(15, 18, 15, 18)
        ));

        JLabel titulo = new JLabel("Últimos reembolsos registrados");
        titulo.setForeground(AMARILLO);
        titulo.setFont(new Font("Arial", Font.BOLD, 21));

        modelo = new DefaultTableModel(
                new Object[]{
                        "ID",
                        "Venta",
                        "Película",
                        "Cliente",
                        "Documento",
                        "Asientos",
                        "Entradas",
                        "Monto",
                        "Estado",
                        "Taquillero",
                        "Fecha"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 6) {
                    return Integer.class;
                }
                if (columnIndex == 7) {
                    return Double.class;
                }
                return String.class;
            }
        };

        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(38);
        tabla.setBackground(PANEL);
        tabla.setForeground(BLANCO);
        tabla.setSelectionBackground(AMARILLO);
        tabla.setSelectionForeground(Color.BLACK);
        tabla.setGridColor(BORDE);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JTableHeader header = tabla.getTableHeader();
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());

        tabla.setDefaultRenderer(Object.class, new CellRenderer());
        tabla.setDefaultRenderer(Double.class, new CellRenderer());
        tabla.setDefaultRenderer(Integer.class, new CellRenderer());

        int[] anchos = {
                55, 115, 210, 180, 105,
                150, 80, 100, 90, 110, 155
        };

        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i)
                    .setPreferredWidth(anchos[i]);
        }

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new LineBorder(BORDE, 1));
        scroll.getViewport().setBackground(PANEL);

        panel.add(titulo, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        JButton menu = crearBoton(
                "MENÚ GERENTE",
                new Color(0, 80, 160),
                BLANCO,
                180
        );
        menu.setPreferredSize(new Dimension(180, 44));
        menu.addActionListener(e ->
                CINEXTransiciones.cambiar(
                        this,
                        new MenuGerenteCINEXGUI(usuarioActual)
                )
        );

        JLabel nota = new JLabel(
                "Ventas netas = ventas brutas - monto reembolsado."
        );
        nota.setForeground(GRIS);
        nota.setFont(new Font("Arial", Font.PLAIN, 13));

        footer.add(menu, BorderLayout.WEST);
        footer.add(nota, BorderLayout.EAST);
        return footer;
    }

    private void cargarDashboard() {
        try {
            ResumenReembolsosCINEX resumen =
                    control.obtenerResumenGeneral();

            lblVentasBrutas.setText(
                    "S/ " + String.format(
                            "%.2f",
                            resumen.getVentasBrutas()
                    )
            );
            lblReembolsos.setText(
                    "S/ " + String.format(
                            "%.2f",
                            resumen.getMontoReembolsado()
                    )
            );
            lblVentasNetas.setText(
                    "S/ " + String.format(
                            "%.2f",
                            resumen.getVentasNetas()
                    )
            );
            lblEntradasVendidas.setText(
                    String.valueOf(
                            resumen.getEntradasVendidas()
                    )
            );
            lblEntradasReembolsadas.setText(
                    String.valueOf(
                            resumen.getEntradasReembolsadas()
                    )
            );
            lblEntradasVigentes.setText(
                    String.valueOf(
                            resumen.getEntradasVigentes()
                    )
            );
            lblParciales.setText(
                    String.valueOf(
                            resumen.getReembolsosParciales()
                    )
            );
            lblTotales.setText(
                    String.valueOf(
                            resumen.getReembolsosTotales()
                    )
            );

            modelo.setRowCount(0);
            ArrayList<Object[]> reembolsos =
                    control.listarUltimosReembolsos(100);

            for (Object[] fila : reembolsos) {
                modelo.addRow(fila);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Dashboard",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private static JLabel crearValor() {
        JLabel label = new JLabel("0");
        label.setFont(new Font("Arial", Font.BOLD, 25));
        return label;
    }

    private JButton crearBoton(
            String texto,
            Color fondo,
            Color colorTexto,
            int ancho
    ) {
        JButton boton = new JButton(texto);
        boton.setPreferredSize(new Dimension(ancho, 44));
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
            setBackground(PANEL_2);
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
            label.setBorder(new EmptyBorder(0, 7, 0, 7));
            label.setHorizontalAlignment(SwingConstants.CENTER);

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

                if ("Total".equalsIgnoreCase(texto)) {
                    label.setForeground(ROJO);
                } else if ("Parcial".equalsIgnoreCase(texto)) {
                    label.setForeground(AMARILLO);
                } else {
                    label.setForeground(BLANCO);
                }
            }

            if (value instanceof Double) {
                label.setText(
                        "S/ " + String.format(
                                "%.2f",
                                (Double) value
                        )
                );
            }

            return label;
        }
    }
}
