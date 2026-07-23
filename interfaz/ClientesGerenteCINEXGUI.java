package interfaz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.imageio.ImageIO;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import control.BDCINEX;

public class ClientesGerenteCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_SIDEBAR = new Color(3, 18, 45);
    private final Color AZUL_PANEL = new Color(8, 24, 55);
    private final Color AZUL_PANEL_2 = new Color(5, 18, 43);
    private final Color AZUL_TABLA = new Color(7, 28, 65);
    private final Color AZUL_HEADER = new Color(10, 38, 83);
    private final Color AZUL_BORDE = new Color(80, 105, 145);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS = new Color(185, 190, 200);

    private JLabel lblHora;
    private JLabel lblFecha;
    private JLabel lblMensaje;

    private String usuarioActual;

    private JTextField txtBuscar;
    private JTable tablaClientes;
    private DefaultTableModel modeloTabla;
    private JScrollPane scrollTabla;

    private ArrayList<Cliente> clientes = new ArrayList<>();

    public ClientesGerenteCINEXGUI() {
        this("gerente");
    }

    public ClientesGerenteCINEXGUI(String usuario) {
        this.usuarioActual = usuario;

        cargarClientesIniciales();

        setTitle("CINEX - Listar clientes");
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel fondo = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp = new GradientPaint(
                        0, 0, AZUL_FONDO_1,
                        getWidth(), getHeight(), AZUL_FONDO_2
                );

                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(new Color(255, 255, 255, 8));
                g2.fillOval(-160, -80, 500, 500);
                g2.fillOval(getWidth() - 430, 220, 360, 360);

                g2.dispose();
            }
        };

        setContentPane(fondo);

        JPanel cuerpo = new JPanel(new BorderLayout());
        cuerpo.setOpaque(false);
        cuerpo.add(crearHeader(), BorderLayout.NORTH);
        cuerpo.add(crearContenido(), BorderLayout.CENTER);
        cuerpo.add(crearFooter(), BorderLayout.SOUTH);

        fondo.add(cuerpo, BorderLayout.CENTER);

        actualizarMensajeEstado();
        actualizarFechaHora();

        Timer timer = new Timer(1000, e -> actualizarFechaHora());
        timer.start();
    }

    private void cargarClientesIniciales() {
        clientes.clear();

        try {
            for (Object[] fila : BDCINEX.listarClientesCompradores()) {
                String documento = fila[0] == null ? "" : fila[0].toString().trim();
                String nombre = fila[1] == null ? "" : fila[1].toString().trim();

                if (!documento.isEmpty() && !nombre.isEmpty()) {
                    clientes.add(new Cliente(obtenerTipoDocumento(documento), documento, nombre));
                }
            }
        } catch (Exception e) {
            System.out.println("[CINEX] Error al cargar clientes compradores: " + e.getMessage());
        }
    }


    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 58));
        header.setBorder(new EmptyBorder(12, 25, 4, 25));

        JPanel infoPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                30,
                                5
                        )
                );

        infoPanel.setOpaque(false);

        JLabel lblUsuario =
                new JLabel("Usuario: " + usuarioActual);

        lblUsuario.setForeground(BLANCO);
        lblUsuario.setFont(
                new Font("Arial", Font.BOLD, 15)
        );

        JLabel lblTerminal =
                new JLabel("Terminal: 01");

        lblTerminal.setForeground(BLANCO);
        lblTerminal.setFont(
                new Font("Arial", Font.BOLD, 15)
        );

        lblHora = new JLabel();
        lblHora.setForeground(BLANCO);
        lblHora.setFont(
                new Font("Arial", Font.BOLD, 15)
        );

        lblFecha = new JLabel();
        lblFecha.setForeground(BLANCO);
        lblFecha.setFont(
                new Font("Arial", Font.BOLD, 15)
        );

        infoPanel.add(lblUsuario);
        infoPanel.add(lblTerminal);
        infoPanel.add(lblHora);
        infoPanel.add(lblFecha);

        header.add(infoPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel crearContenido() {
        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setOpaque(false);
        contenido.setBorder(
                new EmptyBorder(15, 20, 15, 20)
        );

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(crearPanelClientes(), BorderLayout.CENTER);

        contenido.add(main, BorderLayout.CENTER);
        return contenido;
    }

    private JPanel crearPanelClientes() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Listar clientes");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 34));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitulo = new JLabel("Clientes compradores con compra confirmada");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 17));
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(titulo);
        panel.add(Box.createVerticalStrut(5));
        panel.add(subtitulo);
        panel.add(Box.createVerticalStrut(25));

        RoundedPanel buscarPanel = new RoundedPanel(14, new Color(5, 18, 43, 170));
        buscarPanel.setLayout(null);
        buscarPanel.setPreferredSize(new Dimension(800, 105));
        buscarPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        buscarPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buscarPanel.setBorder(new LineBorder(AZUL_BORDE, 1, true));

        JLabel lblBuscar = new JLabel("Buscar:");
        lblBuscar.setForeground(BLANCO);
        lblBuscar.setFont(new Font("Arial", Font.BOLD, 16));
        lblBuscar.setBounds(25, 22, 90, 35);
        buscarPanel.add(lblBuscar);

        txtBuscar = new JTextField();
        txtBuscar.setBounds(115, 18, 645, 42);
        txtBuscar.setBackground(AZUL_PANEL);
        txtBuscar.setForeground(BLANCO);
        txtBuscar.setCaretColor(BLANCO);
        txtBuscar.setFont(new Font("Arial", Font.PLAIN, 16));
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1, true),
                new EmptyBorder(0, 10, 0, 10)
        ));
        txtBuscar.setOpaque(true);
        buscarPanel.add(txtBuscar);

        lblMensaje = new JLabel(" ");
        lblMensaje.setForeground(AMARILLO);
        lblMensaje.setFont(new Font("Arial", Font.BOLD, 16));
        lblMensaje.setHorizontalAlignment(SwingConstants.CENTER);
        lblMensaje.setBounds(25, 66, 735, 25);
        buscarPanel.add(lblMensaje);

        buscarPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int ancho = buscarPanel.getWidth();

                txtBuscar.setBounds(
                        115,
                        18,
                        Math.max(260, ancho - 155),
                        42
                );

                lblMensaje.setBounds(
                        25,
                        66,
                        Math.max(260, ancho - 50),
                        25
                );
            }
        });

        panel.add(buscarPanel);
        panel.add(Box.createVerticalStrut(20));

        RoundedPanel tablaPanel = new RoundedPanel(18, new Color(5, 18, 43, 170));
        tablaPanel.setLayout(new BorderLayout());
        tablaPanel.setPreferredSize(new Dimension(800, 390));
        tablaPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        tablaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tablaPanel.setBorder(new EmptyBorder(18, 18, 18, 18));

        String[] columnas = {"Tipo Doc.", "DNI / C.E.", "Nombre y Apellido"};

        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaClientes = new JTable(modeloTabla);
        scrollTabla = new JScrollPane(tablaClientes);

        tablaClientes.getColumnModel().getColumn(0).setPreferredWidth(90);
        tablaClientes.getColumnModel().getColumn(1).setPreferredWidth(150);
        tablaClientes.getColumnModel().getColumn(2).setPreferredWidth(520);

        estilizarTablaClientes();

        tablaPanel.add(scrollTabla, BorderLayout.CENTER);
        panel.add(tablaPanel);

        cargarTabla(clientes);

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrarClientes();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrarClientes();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrarClientes();
            }
        });

        return panel;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 25, 25, 25));

        JButton btnAtras = new JButton("MENÚ GERENTE");
        btnAtras.setPreferredSize(new Dimension(180, 44));
        btnAtras.setBackground(new Color(0, 80, 160));
        btnAtras.setForeground(BLANCO);
        btnAtras.setFont(new Font("Arial", Font.BOLD, 13));
        btnAtras.setFocusPainted(false);
        btnAtras.setBorderPainted(false);
        btnAtras.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnAtras.addActionListener(e -> {
            dispose();
            new MenuGerenteCINEXGUI(usuarioActual).setVisible(true);
        });

        footer.add(btnAtras, BorderLayout.WEST);
        return footer;
    }

    private JButton crearBotonPrincipal(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(AMARILLO);
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("Arial", Font.BOLD, 15));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton crearBotonSecundario(String texto) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                Color fondo = getModel().isPressed()
                        ? new Color(12, 45, 95)
                        : getModel().isRollover() ? new Color(15, 55, 105) : AZUL_PANEL_2;

                g2.setColor(fondo);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);

                g2.setColor(AZUL_BORDE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (w - fm.stringWidth(getText())) / 2;
                int y = (h + fm.getAscent() - fm.getDescent()) / 2;

                g2.setColor(BLANCO);
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };

        btn.setPreferredSize(new Dimension(180, 44));
        btn.setForeground(BLANCO);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void estilizarTablaClientes() {
        tablaClientes.setOpaque(true);
        tablaClientes.setBackground(AZUL_TABLA);
        tablaClientes.setForeground(BLANCO);
        tablaClientes.setGridColor(new Color(55, 95, 150));
        tablaClientes.setSelectionBackground(AMARILLO);
        tablaClientes.setSelectionForeground(Color.BLACK);
        tablaClientes.setRowHeight(38);
        tablaClientes.setFont(new Font("Arial", Font.PLAIN, 15));
        tablaClientes.setFillsViewportHeight(true);
        tablaClientes.setShowHorizontalLines(true);
        tablaClientes.setShowVerticalLines(true);
        tablaClientes.setIntercellSpacing(new Dimension(1, 1));
        tablaClientes.setAutoCreateRowSorter(true);
        tablaClientes.setFocusable(false);

        JTableHeader header = tablaClientes.getTableHeader();
        header.setOpaque(true);
        header.setBackground(AZUL_HEADER);
        header.setForeground(BLANCO);
        header.setFont(new Font("Arial", Font.BOLD, 14));
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 36));
        header.setDefaultRenderer(new HeaderOscuroRenderer());

        DefaultTableCellRenderer rendererCentro = new CeldaOscuraRenderer(SwingConstants.CENTER);
        DefaultTableCellRenderer rendererIzquierda = new CeldaOscuraRenderer(SwingConstants.LEFT);

        tablaClientes.getColumnModel().getColumn(0).setCellRenderer(rendererCentro);
        tablaClientes.getColumnModel().getColumn(1).setCellRenderer(rendererCentro);
        tablaClientes.getColumnModel().getColumn(2).setCellRenderer(rendererIzquierda);

        scrollTabla.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        scrollTabla.setBackground(AZUL_TABLA);
        scrollTabla.getViewport().setBackground(AZUL_TABLA);
        scrollTabla.getViewport().setOpaque(true);
        scrollTabla.setColumnHeaderView(header);
        scrollTabla.getColumnHeader().setBackground(AZUL_HEADER);
        scrollTabla.getColumnHeader().setOpaque(true);
        scrollTabla.getVerticalScrollBar().setBackground(AZUL_PANEL_2);
        scrollTabla.getHorizontalScrollBar().setBackground(AZUL_PANEL_2);
    }

    private void cargarTabla(ArrayList<Cliente> lista) {
        modeloTabla.setRowCount(0);

        for (Cliente c : lista) {
            modeloTabla.addRow(new Object[]{
                    c.tipoDocumento,
                    c.documento,
                    c.nombre
            });
        }

        actualizarMensajeEstado(lista.size());
    }

    private void filtrarClientes() {
        String filtro = txtBuscar.getText().trim().toLowerCase();
        ArrayList<Cliente> resultado = new ArrayList<>();

        for (Cliente c : clientes) {
            if (c.tipoDocumento.toLowerCase().contains(filtro) ||
                    c.documento.toLowerCase().contains(filtro) ||
                    c.nombre.toLowerCase().contains(filtro)) {
                resultado.add(c);
            }
        }

        cargarTabla(resultado);
    }

    private void actualizarMensajeEstado() {
        actualizarMensajeEstado(clientes.size());
    }

    private void actualizarMensajeEstado(int cantidadVisible) {
        if (clientes.isEmpty()) {
            lblMensaje.setText("No existen clientes compradores registrados.");
            lblMensaje.setForeground(AMARILLO);
        } else if (cantidadVisible == 0) {
            lblMensaje.setText("No se encontraron clientes con ese criterio de búsqueda.");
            lblMensaje.setForeground(AMARILLO);
        } else {
            lblMensaje.setText("Clientes compradores encontrados: " + cantidadVisible);
            lblMensaje.setForeground(new Color(40, 220, 90));
        }
    }

    private String obtenerTipoDocumento(String documento) {
        if (documento == null) {
            return "DOC.";
        }

        documento = documento.trim();

        if (documento.length() == 8) {
            return "DNI";
        }

        if (documento.length() == 9) {
            return "C.E.";
        }

        return "DOC.";
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

    private class Cliente {
        String tipoDocumento;
        String documento;
        String nombre;

        public Cliente(String tipoDocumento, String documento, String nombre) {
            this.tipoDocumento = tipoDocumento;
            this.documento = documento;
            this.nombre = nombre;
        }
    }

    private class CeldaOscuraRenderer extends DefaultTableCellRenderer {
        private final int alineacion;

        public CeldaOscuraRenderer(int alineacion) {
            this.alineacion = alineacion;
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            lbl.setOpaque(true);
            lbl.setHorizontalAlignment(alineacion);
            lbl.setFont(new Font("Arial", Font.PLAIN, 15));
            lbl.setBorder(new EmptyBorder(0, 8, 0, 8));

            if (isSelected) {
                lbl.setBackground(AMARILLO);
                lbl.setForeground(Color.BLACK);
            } else {
                lbl.setBackground(AZUL_TABLA);
                lbl.setForeground(BLANCO);
            }

            return lbl;
        }
    }

    private class HeaderOscuroRenderer extends DefaultTableCellRenderer {
        public HeaderOscuroRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setOpaque(true);
            lbl.setBackground(AZUL_HEADER);
            lbl.setForeground(BLANCO);
            lbl.setFont(new Font("Arial", Font.BOLD, 14));
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setBorder(new LineBorder(AZUL_BORDE, 1));
            return lbl;
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
        private final boolean activo;
        private boolean hover = false;

        public SidebarButton(String emoji, String texto, boolean activo) {
            this.activo = activo;

            setOpaque(false);
            setLayout(null);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            JLabel lblEmoji = new JLabel(emoji, SwingConstants.CENTER);
            if ("\u2261".equals(emoji) || "≡".equals(emoji) || "\u21A9".equals(emoji) || "↩".equals(emoji)) {
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
                    0, 0, getWidth() - 1, getHeight() - 1, radio, radio
            );

            g2.setColor(color);
            g2.fill(round);

            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ClientesGerenteCINEXGUI("gerente").setVisible(true);
        });
    }
}
