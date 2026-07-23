package interfaz;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.Map;
import control.BDCINEX;


public final class CINEXResponsive {

    private static boolean iniciado = false;

    private CINEXResponsive() {}

    public static void iniciar() {
        if (iniciado) return;
        iniciado = true;

        // No forzamos sun.java2d.uiScale=1.0. Dejar que Java respete el escalado real de Windows
        // evita que los botones, letras y asientos se vean demasiado pequeños en laptops con 125% o 150%.
        System.setProperty("sun.java2d.dpiaware", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        aplicarTemaOscuroGlobal();
        ajustarFuenteGlobal();
    }

    public static void configurarVentana(JFrame frame, int anchoBase, int altoBase, int anchoMinimo, int altoMinimo) {
        iniciar();

        Rectangle area = obtenerAreaPantalla();
        int margen = 35;

        int ancho = Math.min(anchoBase, Math.max(1024, area.width - margen));
        int alto = Math.min(altoBase, Math.max(680, area.height - margen));

        int minW = Math.min(Math.max(1120, anchoMinimo), Math.max(1024, area.width - margen));
        int minH = Math.min(Math.max(680, altoMinimo), Math.max(640, area.height - margen));

        frame.setMinimumSize(new Dimension(minW, minH));
        frame.setSize(new Dimension(ancho, alto));
        frame.setLocationRelativeTo(null);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        SwingUtilities.invokeLater(() ->
                aplicarTemaOscuroArbol(
                        frame.getContentPane()
                )
        );
    }

    public static Rectangle obtenerAreaPantalla() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            Rectangle b = gc.getBounds();
            return new Rectangle(
                    b.x + insets.left,
                    b.y + insets.top,
                    b.width - insets.left - insets.right,
                    b.height - insets.top - insets.bottom
            );
        } catch (Exception e) {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            return new Rectangle(0, 0, screen.width, screen.height);
        }
    }

    public static boolean pantallaPequena() {
        Rectangle a = obtenerAreaPantalla();
        return a.width < 1366 || a.height < 768;
    }

    public static int escalar(int valor) {
        Rectangle a = obtenerAreaPantalla();
        double factor = 1.0;
        if (a.width >= 1600 && a.height >= 900) factor = 1.06;
        if (a.width >= 1900 && a.height >= 1000) factor = 1.12;
        if (a.width <= 1280 || a.height <= 720) factor = 0.96;
        return Math.max(1, (int) Math.round(valor * factor));
    }

    public static JPanel centrarConLimite(JComponent contenido, int anchoMaximo, int altoMaximo) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        Dimension pref = contenido.getPreferredSize();
        int ancho = Math.min(anchoMaximo, Math.max(pref == null ? anchoMaximo : pref.width, Math.min(900, anchoMaximo)));
        int alto = Math.min(altoMaximo, pref == null ? altoMaximo : pref.height);

        contenido.setPreferredSize(new Dimension(ancho, alto));
        contenido.setMaximumSize(new Dimension(anchoMaximo, altoMaximo));

        wrapper.add(contenido);
        return wrapper;
    }

    public static JScrollPane envolverConScroll(Component componente) {
        JScrollPane scroll = new JScrollPane(componente);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        // Se dejan ocultos para que no aparezcan barras blancas en las interfaces.
        // Las pantallas se ajustan con tamaños controlados en vez de depender del scroll.
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getHorizontalScrollBar().setUnitIncrement(20);
        return scroll;
    }


    public static void adaptarLayoutAbsoluto(
            JFrame frame,
            JComponent raiz,
            int anchoBase,
            int altoBase,
            int anchoSidebar
    ) {
        if (frame == null || raiz == null) return;

        SwingUtilities.invokeLater(() -> {
            AdaptadorAbsoluto adaptador =
                    new AdaptadorAbsoluto(
                            raiz,
                            anchoBase,
                            altoBase,
                            anchoSidebar
                    );

            adaptador.capturar();

            raiz.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    adaptador.aplicar();
                }
            });

            adaptador.aplicar();
        });
    }

    public static void adaptarPanelAbsoluto(
            JComponent panel,
            int anchoBase,
            int altoBase
    ) {
        if (panel == null) return;

        SwingUtilities.invokeLater(() -> {
            AdaptadorAbsoluto adaptador =
                    new AdaptadorAbsoluto(
                            panel,
                            anchoBase,
                            altoBase,
                            0
                    );

            adaptador.capturar();

            panel.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    adaptador.aplicar();
                }
            });

            adaptador.aplicar();
        });
    }

    private static final class AdaptadorAbsoluto {

        private final JComponent raiz;
        private final int anchoBaseRaiz;
        private final int altoBaseRaiz;
        private final int anchoSidebar;

        private final Map<Container, Dimension> tamanosBase =
                new IdentityHashMap<>();

        private final Map<Container, Map<Component, Rectangle>>
                limitesBase = new IdentityHashMap<>();

        private AdaptadorAbsoluto(
                JComponent raiz,
                int anchoBaseRaiz,
                int altoBaseRaiz,
                int anchoSidebar
        ) {
            this.raiz = raiz;
            this.anchoBaseRaiz = Math.max(1, anchoBaseRaiz);
            this.altoBaseRaiz = Math.max(1, altoBaseRaiz);
            this.anchoSidebar = Math.max(0, anchoSidebar);
        }

        private void capturar() {
            capturarContenedor(
                    raiz,
                    new Dimension(
                            anchoBaseRaiz,
                            altoBaseRaiz
                    )
            );
        }

        private void capturarContenedor(
                Container contenedor,
                Dimension baseForzada
        ) {
            Dimension base = baseForzada;

            if (base == null) {
                int ancho = contenedor.getWidth();
                int alto = contenedor.getHeight();

                Dimension preferido =
                        contenedor.getPreferredSize();

                if (ancho <= 0) {
                    ancho = preferido == null
                            ? 1
                            : Math.max(1, preferido.width);
                }

                if (alto <= 0) {
                    alto = preferido == null
                            ? 1
                            : Math.max(1, preferido.height);
                }

                base = new Dimension(ancho, alto);
            }

            tamanosBase.put(
                    contenedor,
                    new Dimension(base)
            );

            if (contenedor.getLayout() == null) {
                Map<Component, Rectangle> mapa =
                        new IdentityHashMap<>();

                for (Component componente
                        : contenedor.getComponents()) {
                    mapa.put(
                            componente,
                            new Rectangle(
                                    componente.getBounds()
                            )
                    );
                }

                limitesBase.put(contenedor, mapa);
            }

            for (Component componente
                    : contenedor.getComponents()) {
                if (componente instanceof Container) {
                    capturarContenedor(
                            (Container) componente,
                            null
                    );
                }
            }
        }

        private void aplicar() {
            aplicarContenedor(raiz);
            raiz.revalidate();
            raiz.repaint();
        }

        private void aplicarContenedor(Container contenedor) {
            Map<Component, Rectangle> mapa =
                    limitesBase.get(contenedor);

            Dimension base =
                    tamanosBase.get(contenedor);

            if (mapa != null
                    && base != null
                    && contenedor.getWidth() > 0
                    && contenedor.getHeight() > 0) {

                double escalaY =
                        contenedor.getHeight()
                                / (double) Math.max(1, base.height);

                boolean esRaiz = contenedor == raiz;

                double escalaX;

                if (esRaiz && anchoSidebar > 0) {
                    int baseContenido =
                            Math.max(
                                    1,
                                    base.width - anchoSidebar
                            );

                    int anchoContenido =
                            Math.max(
                                    1,
                                    contenedor.getWidth()
                                            - anchoSidebar
                            );

                    escalaX =
                            anchoContenido
                                    / (double) baseContenido;
                } else {
                    escalaX =
                            contenedor.getWidth()
                                    / (double) Math.max(1, base.width);
                }

                for (Map.Entry<Component, Rectangle> entry
                        : mapa.entrySet()) {

                    Component componente = entry.getKey();
                    Rectangle original = entry.getValue();

                    boolean esSidebar =
                            esRaiz
                                    && anchoSidebar > 0
                                    && original.x == 0
                                    && original.width
                                            <= anchoSidebar + 20;

                    int x;
                    int ancho;

                    if (esSidebar) {
                        x = 0;
                        ancho = anchoSidebar;
                    } else if (esRaiz && anchoSidebar > 0) {
                        x = anchoSidebar
                                + (int) Math.round(
                                        (original.x - anchoSidebar)
                                                * escalaX
                                );

                        ancho = Math.max(
                                1,
                                (int) Math.round(
                                        original.width * escalaX
                                )
                        );
                    } else {
                        x = (int) Math.round(
                                original.x * escalaX
                        );

                        ancho = Math.max(
                                1,
                                (int) Math.round(
                                        original.width * escalaX
                                )
                        );
                    }

                    int y = esSidebar
                            ? 0
                            : (int) Math.round(
                                    original.y * escalaY
                            );

                    int alto = esSidebar
                            ? contenedor.getHeight()
                            : Math.max(
                                    1,
                                    (int) Math.round(
                                            original.height * escalaY
                                    )
                            );

                    componente.setBounds(
                            x,
                            y,
                            ancho,
                            alto
                    );
                }
            }

            for (Component componente
                    : contenedor.getComponents()) {
                if (componente instanceof Container) {
                    aplicarContenedor(
                            (Container) componente
                    );
                }
            }
        }
    }

    public static void estabilizarBoton(AbstractButton btn, Color fondo, Color texto) {
        estabilizarBoton(btn, fondo, texto, new Color(35, 55, 90), new Color(235, 240, 248));
    }

    public static void estabilizarBoton(AbstractButton btn, Color fondo, Color texto, Color fondoDeshabilitado, Color textoDeshabilitado) {
        if (btn == null) return;

        btn.setUI(new BasicButtonUI());
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setRolloverEnabled(true);
        btn.setCursor(btn.isEnabled() ? new Cursor(Cursor.HAND_CURSOR) : new Cursor(Cursor.DEFAULT_CURSOR));
        btn.setBackground(btn.isEnabled() ? fondo : fondoDeshabilitado);
        btn.setForeground(btn.isEnabled() ? texto : textoDeshabilitado);

        for (PropertyChangeListener pcl : btn.getPropertyChangeListeners("enabled")) {
            btn.removePropertyChangeListener("enabled", pcl);
        }

        btn.addPropertyChangeListener("enabled", e -> {
            boolean habilitado = Boolean.TRUE.equals(e.getNewValue());
            btn.setBackground(habilitado ? fondo : fondoDeshabilitado);
            btn.setForeground(habilitado ? texto : textoDeshabilitado);
            btn.setCursor(habilitado ? new Cursor(Cursor.HAND_CURSOR) : new Cursor(Cursor.DEFAULT_CURSOR));
            btn.repaint();
        });
    }

    public static JButton botonAmarillo(String texto, int ancho, int alto) {
        JButton btn = new JButton(texto);
        btn.setPreferredSize(new Dimension(ancho, alto));
        btn.setFont(new Font("Arial", Font.BOLD, 15));
        estabilizarBoton(btn, new Color(245, 196, 0), Color.BLACK);
        return btn;
    }

    public static JButton botonAzul(String texto, int ancho, int alto) {
        JButton btn = new JButton(texto);
        btn.setPreferredSize(new Dimension(ancho, alto));
        btn.setFont(new Font("Arial", Font.BOLD, 15));
        estabilizarBoton(btn, new Color(0, 80, 160), Color.WHITE);
        return btn;
    }

    public static JButton botonSecundario(String texto, int ancho, int alto) {
        JButton btn = new JButton(texto);
        btn.setPreferredSize(new Dimension(ancho, alto));
        btn.setFont(new Font("Arial", Font.BOLD, 15));
        estabilizarBoton(btn, new Color(5, 18, 43), Color.WHITE, new Color(38, 48, 70), new Color(180, 190, 205));
        btn.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));
        btn.setBorderPainted(true);
        return btn;
    }

    public static void prepararCombo(JComboBox<?> combo) {
        if (combo == null) return;
        combo.setFont(new Font("Arial", Font.BOLD, 15));
        combo.setBackground(new Color(8, 24, 55));
        combo.setForeground(Color.WHITE);
        combo.setOpaque(true);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setFont(new Font("Arial", Font.BOLD, 15));
                label.setOpaque(true);
                if (isSelected) {
                    label.setBackground(new Color(245, 196, 0));
                    label.setForeground(Color.BLACK);
                } else {
                    label.setBackground(new Color(8, 24, 55));
                    label.setForeground(Color.WHITE);
                }
                return label;
            }
        });
    }

    /**
     * Muestra una confirmación de cierre de sesión con el estilo CINEX.
     * Evita los botones blancos del JOptionPane nativo de Windows.
     */
    public static boolean confirmarCerrarSesion(
            Component propietario
    ) {
        final boolean[] confirmado = {false};

        Window ventanaPropietaria =
                propietario == null
                        ? null
                        : SwingUtilities.getWindowAncestor(
                                propietario
                        );

        JDialog dialogo = new JDialog(
                ventanaPropietaria,
                "Cerrar sesión",
                Dialog.ModalityType.APPLICATION_MODAL
        );

        dialogo.setDefaultCloseOperation(
                JDialog.DISPOSE_ON_CLOSE
        );
        dialogo.setResizable(false);

        JPanel fondo = new JPanel(
                new BorderLayout(18, 16)
        );
        fondo.setBackground(new Color(8, 28, 65));
        fondo.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(
                                new Color(63, 96, 145),
                                1,
                                true
                        ),
                        BorderFactory.createEmptyBorder(
                                22,
                                24,
                                20,
                                24
                        )
                )
        );

        JLabel icono = new JLabel(
                UIManager.getIcon(
                        "OptionPane.questionIcon"
                )
        );
        icono.setVerticalAlignment(
                SwingConstants.TOP
        );

        JLabel mensaje = new JLabel(
                "¿Desea cerrar sesión?"
        );
        mensaje.setForeground(
                new Color(245, 247, 252)
        );
        mensaje.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        16
                )
        );

        JPanel contenido = new JPanel(
                new BorderLayout(14, 0)
        );
        contenido.setOpaque(false);
        contenido.add(icono, BorderLayout.WEST);
        contenido.add(mensaje, BorderLayout.CENTER);

        JButton btnSi = botonAmarillo(
                "SÍ",
                118,
                42
        );

        JButton btnNo = botonSecundario(
                "NO",
                118,
                42
        );

        JPanel acciones = new JPanel(
                new FlowLayout(
                        FlowLayout.CENTER,
                        12,
                        0
                )
        );
        acciones.setOpaque(false);
        acciones.add(btnSi);
        acciones.add(btnNo);

        btnSi.addActionListener(e -> {
            confirmado[0] = true;
            dialogo.dispose();
        });

        btnNo.addActionListener(e -> {
            confirmado[0] = false;
            dialogo.dispose();
        });

        dialogo.getRootPane().setDefaultButton(
                btnNo
        );

        dialogo.getRootPane().registerKeyboardAction(
                e -> dialogo.dispose(),
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_ESCAPE,
                        0
                ),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        fondo.add(contenido, BorderLayout.CENTER);
        fondo.add(acciones, BorderLayout.SOUTH);

        dialogo.setContentPane(fondo);
        dialogo.pack();
        dialogo.setMinimumSize(
                new Dimension(390, 185)
        );
        dialogo.setLocationRelativeTo(
                propietario
        );
        dialogo.setVisible(true);

        return confirmado[0];
    }

    public static void hacerBotonAccesible(AbstractButton boton, String textoAccesible) {
        if (boton == null) return;
        boton.getAccessibleContext().setAccessibleName(textoAccesible);
    }

    private static void aplicarTemaOscuroGlobal() {
        Color fondo = new Color(8, 28, 65);
        Color fondoAlterno = new Color(10, 40, 85);
        Color texto = new Color(245, 247, 252);
        Color gris = new Color(185, 195, 210);
        Color amarillo = new Color(245, 196, 0);
        Color borde = new Color(63, 96, 145);

        UIManager.put("Panel.background", fondo);
        UIManager.put("Label.foreground", texto);
        UIManager.put("OptionPane.background", fondo);
        UIManager.put("OptionPane.messageForeground", texto);

        UIManager.put("TextField.background", fondo);
        UIManager.put("TextField.foreground", texto);
        UIManager.put("TextField.caretForeground", texto);
        UIManager.put("TextField.selectionBackground", amarillo);
        UIManager.put("TextField.selectionForeground", Color.BLACK);
        UIManager.put("TextField.inactiveBackground", new Color(18, 35, 68));
        UIManager.put("TextField.inactiveForeground", gris);

        UIManager.put("PasswordField.background", fondo);
        UIManager.put("PasswordField.foreground", texto);
        UIManager.put("PasswordField.caretForeground", texto);
        UIManager.put("PasswordField.selectionBackground", amarillo);
        UIManager.put("PasswordField.selectionForeground", Color.BLACK);
        UIManager.put("PasswordField.inactiveBackground", new Color(18, 35, 68));
        UIManager.put("PasswordField.inactiveForeground", gris);

        UIManager.put("FormattedTextField.background", fondo);
        UIManager.put("FormattedTextField.foreground", texto);
        UIManager.put("FormattedTextField.caretForeground", texto);
        UIManager.put("FormattedTextField.selectionBackground", amarillo);
        UIManager.put("FormattedTextField.selectionForeground", Color.BLACK);

        UIManager.put("TextArea.background", fondo);
        UIManager.put("TextArea.foreground", texto);
        UIManager.put("TextArea.caretForeground", texto);
        UIManager.put("TextPane.background", fondo);
        UIManager.put("TextPane.foreground", texto);
        UIManager.put("EditorPane.background", fondo);
        UIManager.put("EditorPane.foreground", texto);

        UIManager.put("ComboBox.background", fondo);
        UIManager.put("ComboBox.foreground", texto);
        UIManager.put("ComboBox.buttonBackground", fondoAlterno);
        UIManager.put("ComboBox.selectionBackground", amarillo);
        UIManager.put("ComboBox.selectionForeground", Color.BLACK);
        UIManager.put("ComboBox.disabledForeground", gris);
        UIManager.put("ComboBox.disabledBackground", new Color(18, 35, 68));

        UIManager.put("List.background", fondo);
        UIManager.put("List.foreground", texto);
        UIManager.put("List.selectionBackground", amarillo);
        UIManager.put("List.selectionForeground", Color.BLACK);

        UIManager.put("Table.background", fondo);
        UIManager.put("Table.foreground", texto);
        UIManager.put("Table.selectionBackground", amarillo);
        UIManager.put("Table.selectionForeground", Color.BLACK);
        UIManager.put("Table.gridColor", borde);
        UIManager.put("TableHeader.background", fondoAlterno);
        UIManager.put("TableHeader.foreground", texto);

        UIManager.put("ScrollPane.background", fondo);
        UIManager.put("Viewport.background", fondo);
        UIManager.put("ScrollBar.background", fondo);
        UIManager.put("ScrollBar.track", fondo);
        UIManager.put("ScrollBar.thumb", new Color(70, 105, 155));

        UIManager.put("Spinner.background", fondo);
        UIManager.put("Spinner.foreground", texto);
        UIManager.put("CheckBox.background", fondo);
        UIManager.put("CheckBox.foreground", texto);
        UIManager.put("RadioButton.background", fondo);
        UIManager.put("RadioButton.foreground", texto);

        UIManager.put("Button.background", fondoAlterno);
        UIManager.put("Button.foreground", texto);
        UIManager.put("Button.disabledText", gris);
        UIManager.put("Button.select", amarillo);
        UIManager.put("ToggleButton.background", fondoAlterno);
        UIManager.put("ToggleButton.foreground", texto);
        UIManager.put("ToggleButton.select", new Color(20, 60, 115));

        UIManager.put("TabbedPane.background", fondo);
        UIManager.put("TabbedPane.foreground", texto);
        UIManager.put("TabbedPane.selected", fondoAlterno);
        UIManager.put("TitledBorder.titleColor", texto);
    }

    /**
     * Corrige controles que Windows puede pintar en blanco aun cuando
     * la interfaz utiliza texto blanco. Solo cambia componentes con
     * colores claros o renderizadores predeterminados.
     */
    public static void aplicarTemaOscuroArbol(
            Component raiz
    ) {
        if (raiz == null) {
            return;
        }

        Color fondo = new Color(8, 28, 65);
        Color fondoAlterno = new Color(10, 40, 85);
        Color texto = new Color(245, 247, 252);
        Color amarillo = new Color(245, 196, 0);
        Color borde = new Color(63, 96, 145);

        if (raiz instanceof JComboBox<?>) {
            JComboBox<?> combo = (JComboBox<?>) raiz;
            combo.setBackground(fondo);
            combo.setForeground(texto);
            combo.setOpaque(true);

            ListCellRenderer<?> renderer = combo.getRenderer();
            if (renderer == null
                    || renderer instanceof javax.swing.plaf.UIResource) {
                prepararCombo(combo);
            }
        } else if (raiz instanceof JPasswordField
                || raiz instanceof JFormattedTextField
                || raiz instanceof JTextField) {
            JTextField campo = (JTextField) raiz;

            boolean fondoEraClaro =
                    esColorClaro(campo.getBackground());

            if (fondoEraClaro) {
                campo.setBackground(fondo);
                campo.setForeground(texto);
            }

            campo.setCaretColor(texto);
            campo.setSelectionColor(amarillo);
            campo.setSelectedTextColor(Color.BLACK);

            if (campo.getBorder() == null) {
                campo.setBorder(
                        new LineBorder(borde, 1, true)
                );
            }
        } else if (raiz instanceof JTextArea) {
            JTextArea area = (JTextArea) raiz;

            if (esColorClaro(area.getBackground())) {
                area.setBackground(fondo);
            }

            area.setForeground(texto);
            area.setCaretColor(texto);
        } else if (raiz instanceof JList<?>) {
            JList<?> lista = (JList<?>) raiz;

            if (esColorClaro(lista.getBackground())) {
                lista.setBackground(fondo);
            }

            lista.setForeground(texto);
            lista.setSelectionBackground(amarillo);
            lista.setSelectionForeground(Color.BLACK);
        } else if (raiz instanceof JTable) {
            JTable tabla = (JTable) raiz;

            if (esColorClaro(tabla.getBackground())) {
                tabla.setBackground(fondo);
            }

            tabla.setForeground(texto);
            tabla.setGridColor(borde);

            if (tabla.getTableHeader() != null
                    && esColorClaro(
                            tabla.getTableHeader().getBackground()
                    )) {
                tabla.getTableHeader().setBackground(fondoAlterno);
                tabla.getTableHeader().setForeground(texto);
            }
        } else if (raiz instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) raiz;

            if (esColorClaro(scroll.getBackground())) {
                scroll.setBackground(fondo);
            }

            scroll.getViewport().setBackground(fondo);

            JPanel esquina = new JPanel();
            esquina.setBackground(fondoAlterno);
            scroll.setCorner(
                    JScrollPane.UPPER_RIGHT_CORNER,
                    esquina
            );
            scroll.setCorner(
                    JScrollPane.LOWER_RIGHT_CORNER,
                    esquina
            );
        } else if (raiz instanceof JSpinner) {
            JSpinner spinner = (JSpinner) raiz;
            spinner.setBackground(fondo);
            spinner.setForeground(texto);
        } else if (raiz instanceof AbstractButton) {
            AbstractButton boton = (AbstractButton) raiz;

            if (esColorClaro(boton.getBackground())) {
                boton.setBackground(fondoAlterno);
                boton.setForeground(texto);
            }
        }

        if (raiz instanceof Container) {
            for (Component hijo
                    : ((Container) raiz).getComponents()) {
                aplicarTemaOscuroArbol(hijo);
            }
        }
    }

    private static boolean esColorClaro(Color color) {
        if (color == null) {
            return true;
        }

        return color.getRed() > 215
                && color.getGreen() > 215
                && color.getBlue() > 215;
    }

    private static void ajustarFuenteGlobal() {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                FontUIResource font = (FontUIResource) value;
                int size = font.getSize();
                if (size < 13) size = 13;
                UIManager.put(key, new FontUIResource(font.getName(), font.getStyle(), size));
            }
        }
    }
}
