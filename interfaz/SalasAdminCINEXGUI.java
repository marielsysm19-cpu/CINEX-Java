package interfaz;

import control.ControlGestionarSalasCINEX;
import entidad.SalaCINEX;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;

public class SalasAdminCINEXGUI extends JFrame {

    private final Color AZUL_FONDO = new Color(3, 12, 30);
    private final Color AZUL_PANEL = new Color(5, 18, 43);
    private final Color AZUL_TABLA = new Color(7, 28, 65);
    private final Color AZUL_HEADER = new Color(10, 38, 83);
    private final Color AZUL_BORDE = new Color(80, 105, 145);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS = new Color(185, 190, 200);

    private String usuarioActual;
    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lblMensaje;

    private final ControlGestionarSalasCINEX controlSalas = new ControlGestionarSalasCINEX();

    public SalasAdminCINEXGUI() {
        this("admin");
    }

    public SalasAdminCINEXGUI(String usuario) {
        this.usuarioActual = usuario;

        setTitle("CINEX - Gestión de salas");
        setSize(1000, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel fondo = new JPanel(new BorderLayout(0, 18));
        fondo.setBackground(AZUL_FONDO);
        fondo.setBorder(new EmptyBorder(26, 34, 28, 34));
        setContentPane(fondo);

        fondo.add(crearHeader(), BorderLayout.NORTH);
        fondo.add(crearCentro(), BorderLayout.CENTER);
        fondo.add(crearBotonera(), BorderLayout.SOUTH);

        cargarDatos();
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Gestión de salas");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 34));

        JLabel subtitulo = new JLabel("Consulta las salas registradas en CINEX");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 16));

        textos.add(titulo);
        textos.add(Box.createVerticalStrut(6));
        textos.add(subtitulo);

        lblMensaje = new JLabel(" ");
        lblMensaje.setForeground(AMARILLO);
        lblMensaje.setFont(new Font("Arial", Font.BOLD, 15));
        lblMensaje.setHorizontalAlignment(SwingConstants.RIGHT);

        header.add(textos, BorderLayout.WEST);
        header.add(lblMensaje, BorderLayout.EAST);

        return header;
    }

    private JPanel crearCentro() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AZUL_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1, true),
                new EmptyBorder(18, 18, 18, 18)
        ));

        modelo = new DefaultTableModel(new String[]{"ID", "Nombre", "Capacidad", "Tipo", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        tabla = new JTable(modelo);
        estilizarTabla();

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        scroll.getViewport().setBackground(AZUL_TABLA);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearBotonera() {
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        botones.setOpaque(false);

        JButton btnActualizar = crearBotonSecundario("REFRESCAR LISTA");
        JButton btnMenu = crearBotonSecundario("MENÚ");

        botones.add(btnActualizar);
        botones.add(btnMenu);

        btnActualizar.addActionListener(e -> cargarDatos());
        btnMenu.addActionListener(e -> {
            new MenuAdministradorCINEXGUI(usuarioActual).setVisible(true);
            dispose();
        });

        return botones;
    }

    private void cargarDatos() {
        modelo.setRowCount(0);
        ArrayList<SalaCINEX> datos = controlSalas.listarSalas();

        for (SalaCINEX sala : datos) {
            modelo.addRow(new Object[]{
                    sala.getIdSala(),
                    sala.getNombre(),
                    sala.getCapacidad(),
                    sala.getTipo(),
                    sala.getEstado()
            });
        }

        lblMensaje.setText(datos.isEmpty() ? "No existen salas registradas." : "Salas encontradas: " + datos.size());
    }

    private void estilizarTabla() {
        tabla.setRowHeight(36);
        tabla.setFont(new Font("Arial", Font.PLAIN, 14));
        tabla.setBackground(AZUL_TABLA);
        tabla.setForeground(BLANCO);
        tabla.setSelectionBackground(AMARILLO);
        tabla.setSelectionForeground(Color.BLACK);
        tabla.setGridColor(new Color(55, 95, 150));
        tabla.setFillsViewportHeight(true);
        tabla.setShowGrid(true);
        tabla.setAutoCreateRowSorter(true);

        JTableHeader header = tabla.getTableHeader();
        header.setBackground(AZUL_HEADER);
        header.setForeground(BLANCO);
        header.setFont(new Font("Arial", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        tabla.setDefaultRenderer(Object.class, renderer);
    }

    private JButton crearBotonSecundario(String texto) {
        JButton btn = new JButton(texto);
        btn.setPreferredSize(new Dimension(170, 48));
        btn.setBackground(AZUL_PANEL);
        btn.setForeground(BLANCO);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        return btn;
    }
}
