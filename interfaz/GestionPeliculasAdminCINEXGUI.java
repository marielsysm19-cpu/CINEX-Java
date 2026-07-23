package interfaz;

import control.ControlGestionarPeliculasCINEX;
import control.ControlNotificacionesCINEX;
import control.ControlGestionarPeliculasCINEX.RespuestaRegistro;
import control.ControlGestionarPeliculasCINEX.ResultadoRegistro;
import control.ControlGestionarPeliculasCINEX.RespuestaModificacion;
import control.ControlGestionarPeliculasCINEX.ResultadoModificacion;
import control.ControlGestionarPeliculasCINEX.RespuestaEliminacion;
import control.ControlGestionarPeliculasCINEX.ResultadoEliminacion;
import entidad.PeliculaCINEX;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.text.SimpleDateFormat;

public class GestionPeliculasAdminCINEXGUI extends JFrame {

    private final Color AZUL_FONDO_1 = new Color(3, 12, 30);
    private final Color AZUL_FONDO_2 = new Color(6, 25, 60);
    private final Color AZUL_PANEL = new Color(7, 24, 56);
    private final Color AZUL_INPUT = new Color(8, 28, 65);
    private final Color AZUL_BORDE = new Color(63, 96, 145);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color AZUL_BOTON = new Color(0, 80, 160);
    private final Color BLANCO = new Color(245, 247, 252);
    private final Color GRIS = new Color(185, 195, 210);
    private final Color VERDE = new Color(35, 180, 85);
    private final Color ROJO = new Color(230, 65, 65);

    private final String usuarioActual;
    private final ControlGestionarPeliculasCINEX controlPeliculas;
    private final ControlNotificacionesCINEX controlNotificaciones =
            new ControlNotificacionesCINEX();

    private JTextField txtTitulo;
    private JComboBox<String> cbGenero;
    private JTextField txtDuracion;
    private JComboBox<String> cbClasificacion;
    private JTextField txtImagen;
    private JLabel lblPreviewImagen;
    private JComboBox<String> cbEstado;
    private JLabel lblMensaje;
    private JPanel panelMensajeEstado;
    private JLabel lblTituloEstado;
    private JLabel lblTextoEstado;

    private JLabel lblTituloFormulario;
    private JLabel lblDescripcionFormulario;
    private JButton btnModificar;
    private JButton btnEliminar;
    private JButton btnGuardar;

    private int idPeliculaEnEdicion = -1;
    private String datosOriginalesEdicion = "";

    public GestionPeliculasAdminCINEXGUI(String usuario) {
        this.usuarioActual = usuario == null || usuario.trim().isEmpty() ? "administrador" : usuario.trim();
        this.controlPeliculas =
                new ControlGestionarPeliculasCINEX();

        setTitle("CINEX - Registrar película");
        setSize(1180, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel fondo = new FondoPanel();
        fondo.setLayout(new BorderLayout());
        setContentPane(fondo);

        fondo.add(crearHeader(), BorderLayout.NORTH);
        fondo.add(crearContenido(), BorderLayout.CENTER);
        fondo.add(crearFooter(), BorderLayout.SOUTH);
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(30, 55, 18, 55));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Registrar película");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 34));
        textos.add(titulo);

        JLabel subtitulo = new JLabel("Registro, modificación y eliminación de películas");
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 17));
        subtitulo.setBorder(new EmptyBorder(7, 0, 0, 0));
        textos.add(subtitulo);

        JLabel usuario = new JLabel("Administrador: " + usuarioActual);
        usuario.setForeground(BLANCO);
        usuario.setFont(new Font("Arial", Font.BOLD, 15));

        header.add(textos, BorderLayout.WEST);
        header.add(usuario, BorderLayout.EAST);
        return header;
    }

    private JPanel crearContenido() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 55, 45, 55));

        JPanel panel = new JPanel(null);
        panel.setPreferredSize(new Dimension(1180, 520));
        panel.setOpaque(true);
        panel.setBackground(new Color(4, 18, 45));
        panel.setBorder(new LineBorder(new Color(55, 84, 128), 1, true));

        lblTituloFormulario = new JLabel("Registrar película");
        lblTituloFormulario.setForeground(BLANCO);
        lblTituloFormulario.setFont(new Font("Arial", Font.BOLD, 27));
        lblTituloFormulario.setBounds(38, 28, 420, 35);
        panel.add(lblTituloFormulario);

        lblDescripcionFormulario = new JLabel(
                "Ingrese los datos de la película y seleccione guardar."
        );
        lblDescripcionFormulario.setForeground(GRIS);
        lblDescripcionFormulario.setFont(
                new Font("Arial", Font.PLAIN, 15)
        );
        lblDescripcionFormulario.setBounds(40, 65, 700, 25);
        panel.add(lblDescripcionFormulario);

        int y = 118;
        txtTitulo = crearCampo(panel, "Título *", 45, y, 410);
        cbGenero = crearCombo(panel, "Género *", 520, y, 300, new String[]{
                "Seleccione género",
                "Acción",
                "Aventura",
                "Comedia",
                "Drama",
                "Terror",
                "Suspenso",
                "Ciencia ficción",
                "Romance",
                "Animación",
                "Familiar",
                "Documental"
        });

        y += 82;
        txtDuracion = crearCampo(panel, "Duración (min) *", 45, y, 220);
        ((AbstractDocument) txtDuracion.getDocument()).setDocumentFilter(new SoloNumerosFilter(3));
        cbClasificacion = crearCombo(panel, "Clasificación *", 320, y, 220, new String[]{
                "Seleccione clasificación",
                "APT",
                "+7",
                "+14",
                "+18"
        });

        y += 82;
        txtImagen = crearCampo(panel, "Imagen / póster", 45, y, 600);
        txtImagen.setEditable(false);

        lblPreviewImagen = crearPreviewImagen();
        lblPreviewImagen.setBounds(665, y, 70, 70);
        panel.add(lblPreviewImagen);

        JButton btnBuscarImagen = crearBotonSecundario("BUSCAR");
        btnBuscarImagen.setBounds(745, y + 24, 90, 39);
        btnBuscarImagen.addActionListener(e -> seleccionarImagen());
        panel.add(btnBuscarImagen);

        y += 82;
        cbEstado = crearCombo(panel, "Estado", 45, y, 220, new String[]{"Activa", "Inactiva", "Proximamente"});

        lblMensaje = new JLabel("Complete la información de la película.", SwingConstants.CENTER);
        lblMensaje.setForeground(GRIS);
        lblMensaje.setFont(new Font("Arial", Font.BOLD, 15));
        lblMensaje.setBounds(320, y + 30, 560, 30);
        panel.add(lblMensaje);

        btnModificar = crearBotonSecundario("MODIFICAR PELÍCULA");
        btnModificar.setBounds(310, 445, 170, 45);
        btnModificar.addActionListener(e -> {
            if (idPeliculaEnEdicion > 0) {
                cancelarEdicion();
            } else {
                mostrarVentanaSeleccionarPelicula();
            }
        });
        panel.add(btnModificar);

        btnEliminar = crearBotonPeligro("ELIMINAR PELÍCULA");
        btnEliminar.setBounds(490, 445, 170, 45);
        btnEliminar.addActionListener(
                e -> mostrarVentanaEliminarPelicula()
        );
        panel.add(btnEliminar);

        btnGuardar = crearBotonPrincipal("GUARDAR");
        btnGuardar.setBounds(670, 445, 170, 45);
        btnGuardar.addActionListener(e -> solicitarGuardar());
        panel.add(btnGuardar);

        JPanel panelInfo = crearPanelInfo();
        panelInfo.setBounds(850, 28, 285, 462);
        panel.add(panelInfo);
wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }


    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(0, 55, 22, 55));

        JLabel nota = new JLabel(
                "Complete los datos obligatorios antes de guardar la película."
        );
        nota.setForeground(new Color(145, 160, 185));
        nota.setFont(new Font("Arial", Font.PLAIN, 12));

        JButton btnMenuPrincipal = crearBotonMenuPrincipal();
        btnMenuPrincipal.addActionListener(e -> volverMenu());

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        acciones.setOpaque(false);
        acciones.add(btnMenuPrincipal);

        footer.add(nota, BorderLayout.WEST);
        footer.add(acciones, BorderLayout.EAST);

        return footer;
    }

    private JPanel crearPanelInfo() {
        JPanel panel = new JPanel(null);
        panel.setOpaque(false);
        panel.setBorder(new LineBorder(AZUL_BORDE, 1, true));

        JLabel titulo = new JLabel("Validación del sistema");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        titulo.setBounds(22, 22, 240, 28);
        panel.add(titulo);

        JLabel descripcion = new JLabel("<html>El sistema mostrará aquí solo el resultado actual de la validación.</html>");
        descripcion.setForeground(GRIS);
        descripcion.setFont(new Font("Arial", Font.PLAIN, 12));
        descripcion.setBounds(22, 58, 240, 45);
        panel.add(descripcion);

        panelMensajeEstado = new JPanel(null);
        panelMensajeEstado.setBackground(new Color(8, 28, 65));
        panelMensajeEstado.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        panelMensajeEstado.setBounds(20, 135, 245, 150);

        lblTituloEstado = new JLabel("Formulario listo");
        lblTituloEstado.setForeground(GRIS);
        lblTituloEstado.setFont(new Font("Arial", Font.BOLD, 15));
        lblTituloEstado.setBounds(18, 18, 210, 24);
        panelMensajeEstado.add(lblTituloEstado);

        lblTextoEstado = new JLabel("<html>Ingrese los datos de la película y seleccione guardar.</html>");
        lblTextoEstado.setForeground(BLANCO);
        lblTextoEstado.setFont(new Font("Arial", Font.PLAIN, 13));
        lblTextoEstado.setBounds(18, 50, 210, 80);
        panelMensajeEstado.add(lblTextoEstado);

        panel.add(panelMensajeEstado);

        JLabel nota = new JLabel("<html>Mensajes del sistema:<br>Datos incompletos, registro, eliminación o confirmación.</html>");
        nota.setForeground(GRIS);
        nota.setFont(new Font("Arial", Font.PLAIN, 12));
        nota.setBounds(22, 318, 240, 90);
        panel.add(nota);

        return panel;
    }

    private JTextField crearCampo(JPanel panel, String etiqueta, int x, int y, int w) {
        JLabel lbl = crearLabel(etiqueta);
        lbl.setBounds(x, y, w, 20);
        panel.add(lbl);

        JTextField txt = new JTextField();
        txt.setBounds(x, y + 24, w, 39);
        txt.setBackground(AZUL_INPUT);
        txt.setForeground(BLANCO);
        txt.setCaretColor(BLANCO);
        txt.setFont(new Font("Arial", Font.BOLD, 14));
        txt.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AZUL_BORDE, 1, true),
                new EmptyBorder(0, 10, 0, 10)
        ));
        panel.add(txt);
        return txt;
    }


    private JLabel crearPreviewImagen() {
        JLabel preview = new JLabel("Sin imagen", SwingConstants.CENTER);
        preview.setOpaque(true);
        preview.setBackground(AZUL_INPUT);
        preview.setForeground(GRIS);
        preview.setFont(new Font("Arial", Font.BOLD, 10));
        preview.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        return preview;
    }

    private void mostrarPreviewImagen(File archivo) {
        if (archivo == null || !archivo.exists()) {
            lblPreviewImagen.setIcon(null);
            lblPreviewImagen.setText("Sin imagen");
            return;
        }

        try {
            BufferedImage original = ImageIO.read(archivo);
            if (original == null) {
                lblPreviewImagen.setIcon(null);
                lblPreviewImagen.setText("Sin imagen");
                return;
            }

            int ancho = lblPreviewImagen.getWidth() > 0 ? lblPreviewImagen.getWidth() : 70;
            int alto = lblPreviewImagen.getHeight() > 0 ? lblPreviewImagen.getHeight() : 70;

            BufferedImage recortada = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = recortada.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double escala = Math.max((double) ancho / original.getWidth(), (double) alto / original.getHeight());
            int nuevoAncho = (int) Math.round(original.getWidth() * escala);
            int nuevoAlto = (int) Math.round(original.getHeight() * escala);
            int x = (ancho - nuevoAncho) / 2;
            int y = (alto - nuevoAlto) / 2;

            g2.drawImage(original, x, y, nuevoAncho, nuevoAlto, null);
            g2.dispose();

            lblPreviewImagen.setText("");
            lblPreviewImagen.setIcon(new ImageIcon(recortada));
        } catch (IOException e) {
            lblPreviewImagen.setIcon(null);
            lblPreviewImagen.setText("Sin imagen");
        }
    }

    private JComboBox<String> crearCombo(JPanel panel, String etiqueta, int x, int y, int w, String[] opciones) {
        JLabel lbl = crearLabel(etiqueta);
        lbl.setBounds(x, y, w, 20);
        panel.add(lbl);

        JComboBox<String> combo = new JComboBox<String>(opciones) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int ancho = getWidth();
                int alto = getHeight();

                g2.setColor(AZUL_INPUT);
                g2.fillRect(0, 0, ancho, alto);
                g2.setColor(AZUL_BORDE);
                g2.drawRect(0, 0, ancho - 1, alto - 1);

                Object seleccionado = getSelectedItem();
                String texto = seleccionado == null ? "" : seleccionado.toString();

                boolean esPlaceholder = texto.toLowerCase().startsWith("seleccione");
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();

                int anchoDisponible = Math.max(10, ancho - 58);
                String textoMostrar = texto;
                if (fm.stringWidth(textoMostrar) > anchoDisponible) {
                    while (textoMostrar.length() > 3 && fm.stringWidth(textoMostrar + "...") > anchoDisponible) {
                        textoMostrar = textoMostrar.substring(0, textoMostrar.length() - 1);
                    }
                    textoMostrar += "...";
                }

                int yTexto = (alto - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(esPlaceholder ? new Color(170, 182, 200) : BLANCO);
                g2.drawString(textoMostrar, 12, yTexto);
                g2.dispose();
            }
        };

        combo.setBounds(x, y + 24, w, 39);
        combo.setBackground(AZUL_INPUT);
        combo.setForeground(BLANCO);
        combo.setFont(new Font("Arial", Font.BOLD, 14));
        combo.setFocusable(false);
        combo.setOpaque(false);
        combo.setBorder(new LineBorder(AZUL_BORDE, 1, true));
        combo.setLightWeightPopupEnabled(false);

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setOpaque(true);
                lbl.setFont(new Font("Arial", Font.BOLD, 14));
                lbl.setBorder(new EmptyBorder(8, 10, 8, 10));
                lbl.setBackground(isSelected ? new Color(20, 60, 115) : AZUL_INPUT);
                lbl.setForeground(BLANCO);
                list.setBackground(AZUL_INPUT);
                list.setSelectionBackground(new Color(20, 60, 115));
                list.setSelectionForeground(BLANCO);
                return lbl;
            }
        });

        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton boton = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        int ancho = getWidth();
                        int alto = getHeight();

                        g2.setColor(AZUL_INPUT);
                        g2.fillRect(0, 0, ancho, alto);
                        g2.setColor(AZUL_BORDE);
                        g2.drawLine(0, 0, 0, alto);

                        int centroX = ancho / 2;
                        int centroY = alto / 2 + 2;
                        Polygon flecha = new Polygon();
                        flecha.addPoint(centroX - 6, centroY - 4);
                        flecha.addPoint(centroX + 6, centroY - 4);
                        flecha.addPoint(centroX, centroY + 5);

                        g2.setColor(AMARILLO);
                        g2.fillPolygon(flecha);
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
        });

        panel.add(combo);
        return combo;
    }

    private String obtenerSeleccionValida(JComboBox<String> combo, String textoInicial) {
        Object seleccionado = combo.getSelectedItem();
        if (seleccionado == null) return "";
        String valor = seleccionado.toString().trim();
        return valor.equalsIgnoreCase(textoInicial) ? "" : valor;
    }

    private JLabel crearLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(GRIS);
        lbl.setFont(new Font("Arial", Font.BOLD, 14));
        return lbl;
    }


    private JButton crearBotonMenuPrincipal() {
        JButton boton = new JButton("MENÚ PRINCIPAL") {
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
                    fondo = AZUL_BOTON.darker();
                } else if (getModel().isRollover()) {
                    fondo = new Color(18, 98, 184);
                } else {
                    fondo = AZUL_BOTON;
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
                super.paintComponent(g);
            }
        };

        boton.setPreferredSize(new Dimension(185, 46));
        boton.setForeground(BLANCO);
        boton.setFont(new Font("Arial", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setContentAreaFilled(false);
        boton.setOpaque(false);
        boton.setRolloverEnabled(true);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return boton;
    }

    private JButton crearBotonPrincipal(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(AMARILLO);
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
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
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                Color fondoNormal = new Color(4, 18, 45);
                Color fondoHover = new Color(12, 42, 85);
                Color fondoPresionado = new Color(2, 12, 32);

                if (!isEnabled()) {
                    g2.setColor(new Color(30, 45, 70));
                } else if (getModel().isPressed()) {
                    g2.setColor(fondoPresionado);
                } else if (getModel().isRollover()) {
                    g2.setColor(fondoHover);
                } else {
                    g2.setColor(fondoNormal);
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                g2.setColor(isEnabled() ? AZUL_BORDE : new Color(70, 80, 100));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();

                super.paintComponent(g);
            }
        };

        btn.setForeground(BLANCO);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setRolloverEnabled(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return btn;
    }


    private JButton crearBotonPeligro(String texto) {
        JButton boton = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                Color fondo;

                if (!isEnabled()) {
                    fondo = new Color(65, 55, 65);
                } else if (getModel().isPressed()) {
                    fondo = new Color(145, 35, 45);
                } else if (getModel().isRollover()) {
                    fondo = new Color(215, 70, 80);
                } else {
                    fondo = ROJO;
                }

                g2.setColor(fondo);
                g2.fillRoundRect(
                        0,
                        0,
                        getWidth(),
                        getHeight(),
                        8,
                        8
                );

                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawRoundRect(
                        0,
                        0,
                        getWidth() - 1,
                        getHeight() - 1,
                        8,
                        8
                );

                g2.dispose();
                super.paintComponent(g);
            }
        };

        boton.setForeground(Color.WHITE);
        boton.setFont(new Font("Arial", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setContentAreaFilled(false);
        boton.setOpaque(false);
        boton.setRolloverEnabled(true);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return boton;
    }


    private void mostrarVentanaSeleccionarPelicula() {
        JDialog dialogo = new JDialog(
                this,
                "Modificar película",
                true
        );

        dialogo.setDefaultCloseOperation(
                JDialog.DISPOSE_ON_CLOSE
        );
        dialogo.setSize(800, 520);
        dialogo.setMinimumSize(new Dimension(710, 470));
        dialogo.setLocationRelativeTo(this);

        JPanel fondo = new JPanel(new BorderLayout(0, 14));
        fondo.setBackground(AZUL_FONDO_1);
        fondo.setBorder(new EmptyBorder(22, 22, 22, 22));
        dialogo.setContentPane(fondo);

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel(
                "Seleccionar película para modificar"
        );
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 24));

        JLabel descripcion = new JLabel(
                "Seleccione un registro para cargar sus datos en el formulario."
        );
        descripcion.setForeground(GRIS);
        descripcion.setFont(new Font("Arial", Font.PLAIN, 13));

        textos.add(titulo);
        textos.add(Box.createVerticalStrut(5));
        textos.add(descripcion);

        JTextField txtBuscarPelicula = new JTextField();
        txtBuscarPelicula.setPreferredSize(
                new Dimension(220, 38)
        );
        txtBuscarPelicula.setBackground(AZUL_INPUT);
        txtBuscarPelicula.setForeground(BLANCO);
        txtBuscarPelicula.setCaretColor(BLANCO);
        txtBuscarPelicula.setFont(
                new Font("Arial", Font.BOLD, 13)
        );
        txtBuscarPelicula.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(AZUL_BORDE, 1, true),
                        new EmptyBorder(0, 10, 0, 10)
                )
        );

        JPanel buscador = new JPanel(new FlowLayout(
                FlowLayout.RIGHT,
                8,
                0
        ));
        buscador.setOpaque(false);

        JLabel lblBuscar = new JLabel("Buscar:");
        lblBuscar.setForeground(GRIS);
        lblBuscar.setFont(new Font("Arial", Font.BOLD, 13));

        buscador.add(lblBuscar);
        buscador.add(txtBuscarPelicula);

        cabecera.add(textos, BorderLayout.WEST);
        cabecera.add(buscador, BorderLayout.EAST);

        DefaultTableModel modeloPeliculas =
                new DefaultTableModel(
                        new String[]{
                                "ID",
                                "Título",
                                "Género",
                                "Duración",
                                "Clasificación",
                                "Estado"
                        },
                        0
                ) {
                    @Override
                    public boolean isCellEditable(
                            int fila,
                            int columna
                    ) {
                        return false;
                    }
                };

        JTable tablaPeliculas = new JTable(modeloPeliculas);
        tablaPeliculas.setRowHeight(38);
        tablaPeliculas.setBackground(AZUL_INPUT);
        tablaPeliculas.setForeground(BLANCO);
        tablaPeliculas.setSelectionBackground(AMARILLO);
        tablaPeliculas.setSelectionForeground(Color.BLACK);
        tablaPeliculas.setGridColor(
                new Color(55, 95, 150)
        );
        tablaPeliculas.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        tablaPeliculas.setFillsViewportHeight(true);
        tablaPeliculas.setShowGrid(true);

        JTableHeader encabezadoTabla =
                tablaPeliculas.getTableHeader();
        encabezadoTabla.setBackground(
                new Color(10, 38, 83)
        );
        encabezadoTabla.setForeground(BLANCO);
        encabezadoTabla.setFont(
                new Font("Arial", Font.BOLD, 13)
        );
        encabezadoTabla.setPreferredSize(
                new Dimension(0, 38)
        );
        encabezadoTabla.setReorderingAllowed(false);
        encabezadoTabla.setOpaque(true);
        encabezadoTabla.setDefaultRenderer(
                new RendererEncabezadoDialogo()
        );

        tablaPeliculas.setDefaultRenderer(
                Object.class,
                new RendererCeldaDialogo()
        );

        tablaPeliculas.getColumnModel()
                .getColumn(0)
                .setPreferredWidth(45);
        tablaPeliculas.getColumnModel()
                .getColumn(0)
                .setMaxWidth(65);
        tablaPeliculas.getColumnModel()
                .getColumn(1)
                .setPreferredWidth(240);

        TableRowSorter<DefaultTableModel> orden =
                new TableRowSorter<>(modeloPeliculas);
        tablaPeliculas.setRowSorter(orden);

        JScrollPane scroll = new JScrollPane(tablaPeliculas);
        scroll.setBorder(
                new LineBorder(AZUL_BORDE, 1, true)
        );
        scroll.getViewport().setBackground(AZUL_INPUT);
        estilizarScrollDialogo(scroll);

        JLabel lblEstadoCarga = new JLabel(
                "Consultando películas registradas..."
        );
        lblEstadoCarga.setForeground(GRIS);
        lblEstadoCarga.setFont(
                new Font("Arial", Font.BOLD, 13)
        );

        JButton btnCancelar =
                crearBotonSecundario("CANCELAR");
        btnCancelar.setPreferredSize(
                new Dimension(145, 43)
        );
        btnCancelar.addActionListener(
                e -> dialogo.dispose()
        );

        JButton btnEditarSeleccionada =
                crearBotonPrincipal("EDITAR SELECCIONADA");
        btnEditarSeleccionada.setPreferredSize(
                new Dimension(205, 43)
        );
        btnEditarSeleccionada.setEnabled(false);

        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setOpaque(false);

        JPanel acciones = new JPanel(new FlowLayout(
                FlowLayout.RIGHT,
                12,
                0
        ));
        acciones.setOpaque(false);
        acciones.add(btnCancelar);
        acciones.add(btnEditarSeleccionada);

        inferior.add(lblEstadoCarga, BorderLayout.WEST);
        inferior.add(acciones, BorderLayout.EAST);

        fondo.add(cabecera, BorderLayout.NORTH);
        fondo.add(scroll, BorderLayout.CENTER);
        fondo.add(inferior, BorderLayout.SOUTH);

        Map<Integer, PeliculaCINEX> peliculasPorId =
                new HashMap<>();

        tablaPeliculas.getSelectionModel()
                .addListSelectionListener(e -> {
                    btnEditarSeleccionada.setEnabled(
                            tablaPeliculas.getSelectedRow() >= 0
                    );
                });

        txtBuscarPelicula.getDocument()
                .addDocumentListener(
                        new javax.swing.event.DocumentListener() {
                            private void filtrar() {
                                String texto = txtBuscarPelicula
                                        .getText()
                                        .trim();

                                if (texto.isEmpty()) {
                                    orden.setRowFilter(null);
                                } else {
                                    orden.setRowFilter(
                                            RowFilter.regexFilter(
                                                    "(?i)"
                                                            + Pattern.quote(texto),
                                                    1,
                                                    2,
                                                    4,
                                                    5
                                            )
                                    );
                                }
                            }

                            @Override
                            public void insertUpdate(
                                    javax.swing.event.DocumentEvent e
                            ) {
                                filtrar();
                            }

                            @Override
                            public void removeUpdate(
                                    javax.swing.event.DocumentEvent e
                            ) {
                                filtrar();
                            }

                            @Override
                            public void changedUpdate(
                                    javax.swing.event.DocumentEvent e
                            ) {
                                filtrar();
                            }
                        }
                );

        btnEditarSeleccionada.addActionListener(e -> {
            int filaVista = tablaPeliculas.getSelectedRow();

            if (filaVista < 0) {
                return;
            }

            int filaModelo = tablaPeliculas
                    .convertRowIndexToModel(filaVista);

            int idPelicula = Integer.parseInt(
                    modeloPeliculas
                            .getValueAt(filaModelo, 0)
                            .toString()
            );

            PeliculaCINEX pelicula =
                    peliculasPorId.get(idPelicula);

            if (pelicula == null) {
                JOptionPane.showMessageDialog(
                        dialogo,
                        "No se pudo recuperar la película seleccionada.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            dialogo.setVisible(false);
            dialogo.dispose();

            SwingUtilities.invokeLater(() -> {
                cargarPeliculaParaEdicion(pelicula);
                getContentPane().revalidate();
                getContentPane().repaint();
                repaint();
            });
        });

        SwingWorker<ArrayList<PeliculaCINEX>, Void> carga =
                new SwingWorker<ArrayList<PeliculaCINEX>, Void>() {
                    @Override
                    protected ArrayList<PeliculaCINEX> doInBackground() {
                        return controlPeliculas
                                .listarPeliculasRegistradas();
                    }

                    @Override
                    protected void done() {
                        try {
                            ArrayList<PeliculaCINEX> peliculas = get();
                            modeloPeliculas.setRowCount(0);
                            peliculasPorId.clear();

                            for (PeliculaCINEX pelicula : peliculas) {
                                peliculasPorId.put(
                                        pelicula.getIdPelicula(),
                                        pelicula
                                );

                                modeloPeliculas.addRow(new Object[]{
                                        pelicula.getIdPelicula(),
                                        pelicula.getTitulo(),
                                        pelicula.getGenero(),
                                        pelicula.getDuracion() + " min",
                                        pelicula.getClasificacion(),
                                        pelicula.getEstado()
                                });
                            }

                            if (peliculas.isEmpty()) {
                                lblEstadoCarga.setText(
                                        "No existen películas registradas."
                                );
                                lblEstadoCarga.setForeground(AMARILLO);
                            } else {
                                lblEstadoCarga.setText(
                                        "Películas encontradas: "
                                                + peliculas.size()
                                );
                                lblEstadoCarga.setForeground(VERDE);
                            }

                        } catch (Exception ex) {
                            lblEstadoCarga.setText(
                                    "No se pudieron cargar las películas."
                            );
                            lblEstadoCarga.setForeground(ROJO);

                            JOptionPane.showMessageDialog(
                                    dialogo,
                                    "No se pudieron consultar las películas.",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                };

        carga.execute();
        dialogo.setVisible(true);
    }

    private void cargarPeliculaParaEdicion(
            PeliculaCINEX pelicula
    ) {
        idPeliculaEnEdicion = pelicula.getIdPelicula();
        datosOriginalesEdicion = describirPelicula(pelicula);

        txtTitulo.setText(pelicula.getTitulo());
        seleccionarValorCombo(
                cbGenero,
                pelicula.getGenero()
        );
        txtDuracion.setText(
                String.valueOf(pelicula.getDuracion())
        );
        seleccionarValorCombo(
                cbClasificacion,
                pelicula.getClasificacion()
        );
        txtImagen.setText(pelicula.getImagen());
        seleccionarValorCombo(
                cbEstado,
                pelicula.getEstado()
        );

        File imagen = resolverArchivoImagen(
                pelicula.getImagen()
        );
        mostrarPreviewImagen(imagen);

        lblTituloFormulario.setText("Modificar película");
        lblDescripcionFormulario.setText(
                "Edite los datos y seleccione actualizar."
        );

        btnGuardar.setText("ACTUALIZAR");
        btnModificar.setText("CANCELAR EDICIÓN");
        btnEliminar.setEnabled(false);

        lblMensaje.setText(
                "Editando: " + pelicula.getTitulo()
        );
        lblMensaje.setForeground(AMARILLO);

        actualizarPanelValidacion(
                "Modo edición",
                "Modifique los datos y seleccione actualizar.",
                AMARILLO
        );

        txtTitulo.requestFocusInWindow();

        revalidate();
        repaint();
    }

    private void cancelarEdicion() {
        idPeliculaEnEdicion = -1;
        datosOriginalesEdicion = "";

        lblTituloFormulario.setText("Registrar película");
        lblDescripcionFormulario.setText(
                "Ingrese los datos de la película y seleccione guardar."
        );

        btnGuardar.setText("GUARDAR");
        btnModificar.setText("MODIFICAR PELÍCULA");
        btnEliminar.setEnabled(true);

        limpiarFormulario();

        lblMensaje.setText(
                "Complete la información de la película."
        );
        lblMensaje.setForeground(GRIS);

        actualizarPanelValidacion(
                "Formulario listo",
                "Ingrese los datos de la película y seleccione guardar.",
                GRIS
        );

        revalidate();
        repaint();
    }

    private void seleccionarValorCombo(
            JComboBox<String> combo,
            String valor
    ) {
        String buscado = valor == null ? "" : valor.trim();

        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i);

            if (item != null
                    && item.equalsIgnoreCase(buscado)) {
                combo.setSelectedIndex(i);
                return;
            }
        }

        if (!buscado.isEmpty()) {
            combo.addItem(buscado);
            combo.setSelectedItem(buscado);
        }
    }

    private File resolverArchivoImagen(String ruta) {
        if (ruta == null || ruta.trim().isEmpty()) {
            return null;
        }

        File archivo = new File(ruta.trim());

        if (!archivo.exists()) {
            archivo = new File(
                    "Imagenes/" + new File(ruta).getName()
            );
        }

        return archivo.exists() ? archivo : null;
    }

    private void mostrarVentanaEliminarPelicula() {
        JDialog dialogo = new JDialog(
                this,
                "Eliminar película",
                true
        );

        dialogo.setDefaultCloseOperation(
                JDialog.DISPOSE_ON_CLOSE
        );
        dialogo.setSize(790, 520);
        dialogo.setMinimumSize(new Dimension(700, 470));
        dialogo.setLocationRelativeTo(this);

        JPanel fondo = new JPanel(new BorderLayout(0, 14));
        fondo.setBackground(AZUL_FONDO_1);
        fondo.setBorder(new EmptyBorder(22, 22, 22, 22));
        dialogo.setContentPane(fondo);

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Eliminar película registrada");
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 25));

        JLabel descripcion = new JLabel(
                "Seleccione una película. No se eliminará si tiene funciones asociadas."
        );
        descripcion.setForeground(GRIS);
        descripcion.setFont(new Font("Arial", Font.PLAIN, 13));

        textos.add(titulo);
        textos.add(Box.createVerticalStrut(5));
        textos.add(descripcion);

        JTextField txtBuscarPelicula = new JTextField();
        txtBuscarPelicula.setPreferredSize(
                new Dimension(220, 38)
        );
        txtBuscarPelicula.setBackground(AZUL_INPUT);
        txtBuscarPelicula.setForeground(BLANCO);
        txtBuscarPelicula.setCaretColor(BLANCO);
        txtBuscarPelicula.setFont(
                new Font("Arial", Font.BOLD, 13)
        );
        txtBuscarPelicula.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(AZUL_BORDE, 1, true),
                        new EmptyBorder(0, 10, 0, 10)
                )
        );

        JPanel buscador = new JPanel(new FlowLayout(
                FlowLayout.RIGHT,
                8,
                0
        ));
        buscador.setOpaque(false);

        JLabel lblBuscar = new JLabel("Buscar:");
        lblBuscar.setForeground(GRIS);
        lblBuscar.setFont(new Font("Arial", Font.BOLD, 13));

        buscador.add(lblBuscar);
        buscador.add(txtBuscarPelicula);

        cabecera.add(textos, BorderLayout.WEST);
        cabecera.add(buscador, BorderLayout.EAST);

        DefaultTableModel modeloPeliculas =
                new DefaultTableModel(
                        new String[]{
                                "ID",
                                "Título",
                                "Género",
                                "Duración",
                                "Estado"
                        },
                        0
                ) {
                    @Override
                    public boolean isCellEditable(
                            int fila,
                            int columna
                    ) {
                        return false;
                    }
                };

        JTable tablaPeliculas = new JTable(modeloPeliculas);
        tablaPeliculas.setRowHeight(38);
        tablaPeliculas.setBackground(AZUL_INPUT);
        tablaPeliculas.setForeground(BLANCO);
        tablaPeliculas.setSelectionBackground(AMARILLO);
        tablaPeliculas.setSelectionForeground(Color.BLACK);
        tablaPeliculas.setGridColor(
                new Color(55, 95, 150)
        );
        tablaPeliculas.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        tablaPeliculas.setFillsViewportHeight(true);
        tablaPeliculas.setShowGrid(true);

        JTableHeader encabezadoTabla =
                tablaPeliculas.getTableHeader();
        encabezadoTabla.setBackground(
                new Color(10, 38, 83)
        );
        encabezadoTabla.setForeground(BLANCO);
        encabezadoTabla.setFont(
                new Font("Arial", Font.BOLD, 13)
        );
        encabezadoTabla.setPreferredSize(
                new Dimension(0, 38)
        );
        encabezadoTabla.setReorderingAllowed(false);
        encabezadoTabla.setOpaque(true);
        encabezadoTabla.setDefaultRenderer(
                new RendererEncabezadoDialogo()
        );

        tablaPeliculas.setDefaultRenderer(
                Object.class,
                new RendererCeldaDialogo()
        );

        tablaPeliculas.getColumnModel()
                .getColumn(0)
                .setPreferredWidth(45);
        tablaPeliculas.getColumnModel()
                .getColumn(0)
                .setMaxWidth(65);
        tablaPeliculas.getColumnModel()
                .getColumn(1)
                .setPreferredWidth(260);

        TableRowSorter<DefaultTableModel> orden =
                new TableRowSorter<>(modeloPeliculas);
        tablaPeliculas.setRowSorter(orden);

        JScrollPane scroll = new JScrollPane(tablaPeliculas);
        scroll.setBorder(
                new LineBorder(AZUL_BORDE, 1, true)
        );
        scroll.getViewport().setBackground(AZUL_INPUT);
        estilizarScrollDialogo(scroll);

        JLabel lblEstadoCarga = new JLabel(
                "Consultando películas registradas..."
        );
        lblEstadoCarga.setForeground(GRIS);
        lblEstadoCarga.setFont(
                new Font("Arial", Font.BOLD, 13)
        );

        JButton btnCancelar =
                crearBotonSecundario("CANCELAR");
        btnCancelar.setPreferredSize(
                new Dimension(150, 43)
        );
        btnCancelar.addActionListener(
                e -> dialogo.dispose()
        );

        JButton btnConfirmarEliminar =
                crearBotonPeligro("ELIMINAR SELECCIONADA");
        btnConfirmarEliminar.setPreferredSize(
                new Dimension(210, 43)
        );
        btnConfirmarEliminar.setEnabled(false);

        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setOpaque(false);

        JPanel acciones = new JPanel(new FlowLayout(
                FlowLayout.RIGHT,
                12,
                0
        ));
        acciones.setOpaque(false);
        acciones.add(btnCancelar);
        acciones.add(btnConfirmarEliminar);

        inferior.add(lblEstadoCarga, BorderLayout.WEST);
        inferior.add(acciones, BorderLayout.EAST);

        fondo.add(cabecera, BorderLayout.NORTH);
        fondo.add(scroll, BorderLayout.CENTER);
        fondo.add(inferior, BorderLayout.SOUTH);

        tablaPeliculas.getSelectionModel()
                .addListSelectionListener(e -> {
                    btnConfirmarEliminar.setEnabled(
                            tablaPeliculas.getSelectedRow() >= 0
                    );
                });

        txtBuscarPelicula.getDocument()
                .addDocumentListener(
                        new javax.swing.event.DocumentListener() {
                            private void filtrar() {
                                String texto = txtBuscarPelicula
                                        .getText()
                                        .trim();

                                if (texto.isEmpty()) {
                                    orden.setRowFilter(null);
                                } else {
                                    orden.setRowFilter(
                                            RowFilter.regexFilter(
                                                    "(?i)"
                                                            + Pattern.quote(texto),
                                                    1,
                                                    2,
                                                    4
                                            )
                                    );
                                }
                            }

                            @Override
                            public void insertUpdate(
                                    javax.swing.event.DocumentEvent e
                            ) {
                                filtrar();
                            }

                            @Override
                            public void removeUpdate(
                                    javax.swing.event.DocumentEvent e
                            ) {
                                filtrar();
                            }

                            @Override
                            public void changedUpdate(
                                    javax.swing.event.DocumentEvent e
                            ) {
                                filtrar();
                            }
                        }
                );

        btnConfirmarEliminar.addActionListener(e -> {
            int filaVista = tablaPeliculas.getSelectedRow();

            if (filaVista < 0) {
                JOptionPane.showMessageDialog(
                        dialogo,
                        "Seleccione una película.",
                        "Selección requerida",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            int filaModelo = tablaPeliculas
                    .convertRowIndexToModel(filaVista);

            int idPelicula = Integer.parseInt(
                    modeloPeliculas
                            .getValueAt(filaModelo, 0)
                            .toString()
            );

            String tituloPelicula = modeloPeliculas
                    .getValueAt(filaModelo, 1)
                    .toString();

            int confirmar = JOptionPane.showConfirmDialog(
                    dialogo,
                    "¿Desea eliminar definitivamente la película?\n\n"
                            + tituloPelicula,
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirmar != JOptionPane.YES_OPTION) {
                return;
            }

            btnConfirmarEliminar.setEnabled(false);
            lblEstadoCarga.setText("Eliminando película...");
            lblEstadoCarga.setForeground(AMARILLO);

            SwingWorker<RespuestaEliminacion, Void> worker =
                    new SwingWorker<RespuestaEliminacion, Void>() {
                        @Override
                        protected RespuestaEliminacion doInBackground() {
                            return controlPeliculas.eliminarPelicula(
                                    idPelicula
                            );
                        }

                        @Override
                        protected void done() {
                            try {
                                RespuestaEliminacion respuesta = get();

                                if (respuesta.fueExitosa()) {
                                    modeloPeliculas.removeRow(filaModelo);

                                    lblEstadoCarga.setText(
                                            respuesta.getMensaje()
                                    );
                                    lblEstadoCarga.setForeground(VERDE);

                                    lblMensaje.setText(
                                            respuesta.getMensaje()
                                    );
                                    lblMensaje.setForeground(VERDE);

                                    actualizarPanelValidacion(
                                            "Película eliminada",
                                            respuesta.getMensaje(),
                                            VERDE
                                    );

                                    JOptionPane.showMessageDialog(
                                            dialogo,
                                            respuesta.getMensaje(),
                                            "Eliminación exitosa",
                                            JOptionPane.INFORMATION_MESSAGE
                                    );

                                } else {
                                    boolean relacionada =
                                            respuesta.getResultado()
                                                    == ResultadoEliminacion
                                                    .FUNCIONES_ASOCIADAS;

                                    Color color = relacionada
                                            ? AMARILLO
                                            : ROJO;

                                    lblEstadoCarga.setText(
                                            respuesta.getMensaje()
                                    );
                                    lblEstadoCarga.setForeground(color);

                                    actualizarPanelValidacion(
                                            "No se pudo eliminar",
                                            respuesta.getMensaje(),
                                            color
                                    );

                                    JOptionPane.showMessageDialog(
                                            dialogo,
                                            respuesta.getMensaje(),
                                            "No se pudo eliminar",
                                            relacionada
                                                    ? JOptionPane
                                                    .WARNING_MESSAGE
                                                    : JOptionPane
                                                    .ERROR_MESSAGE
                                    );
                                }

                            } catch (Exception ex) {
                                lblEstadoCarga.setText(
                                        "No se pudo eliminar la película."
                                );
                                lblEstadoCarga.setForeground(ROJO);

                                JOptionPane.showMessageDialog(
                                        dialogo,
                                        "Ocurrió un error al eliminar "
                                                + "la película.",
                                        "Error",
                                        JOptionPane.ERROR_MESSAGE
                                );

                            } finally {
                                btnConfirmarEliminar.setEnabled(
                                        tablaPeliculas.getSelectedRow() >= 0
                                );
                            }
                        }
                    };

            worker.execute();
        });

        SwingWorker<ArrayList<PeliculaCINEX>, Void> carga =
                new SwingWorker<ArrayList<PeliculaCINEX>, Void>() {
                    @Override
                    protected ArrayList<PeliculaCINEX> doInBackground() {
                        return controlPeliculas
                                .listarPeliculasRegistradas();
                    }

                    @Override
                    protected void done() {
                        try {
                            ArrayList<PeliculaCINEX> peliculas = get();
                            modeloPeliculas.setRowCount(0);

                            for (PeliculaCINEX pelicula : peliculas) {
                                modeloPeliculas.addRow(new Object[]{
                                        pelicula.getIdPelicula(),
                                        pelicula.getTitulo(),
                                        pelicula.getGenero(),
                                        pelicula.getDuracion() + " min",
                                        pelicula.getEstado()
                                });
                            }

                            if (peliculas.isEmpty()) {
                                lblEstadoCarga.setText(
                                        "No existen películas registradas."
                                );
                                lblEstadoCarga.setForeground(AMARILLO);
                            } else {
                                lblEstadoCarga.setText(
                                        "Películas encontradas: "
                                                + peliculas.size()
                                );
                                lblEstadoCarga.setForeground(VERDE);
                            }

                        } catch (Exception ex) {
                            lblEstadoCarga.setText(
                                    "No se pudieron cargar las películas."
                            );
                            lblEstadoCarga.setForeground(ROJO);

                            JOptionPane.showMessageDialog(
                                    dialogo,
                                    "No se pudieron consultar las películas "
                                            + "registradas.",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                };

        carga.execute();
        dialogo.setVisible(true);
    }

    // 5. El Administrador selecciona guardar o actualizar.
    private void solicitarGuardar() {
        PeliculaCINEX pelicula =
                obtenerPeliculaDesdeFormulario();

        if (idPeliculaEnEdicion > 0) {
            pelicula.setIdPelicula(idPeliculaEnEdicion);
            solicitarModificacion(pelicula);
            return;
        }

        RespuestaRegistro respuesta =
                controlPeliculas.gestionarRegistroPelicula(
                        pelicula
                );

        if (respuesta.getResultado()
                == ResultadoRegistro.DATOS_INCOMPLETOS) {
            mostrarMensaje(
                    respuesta.getMensaje(),
                    AMARILLO,
                    JOptionPane.WARNING_MESSAGE,
                    "Datos incompletos"
            );
            return;
        }

        if (respuesta.getResultado()
                == ResultadoRegistro.PELICULA_YA_REGISTRADA) {
            mostrarMensaje(
                    respuesta.getMensaje(),
                    ROJO,
                    JOptionPane.WARNING_MESSAGE,
                    "Película ya registrada"
            );
            return;
        }

        if (respuesta.fueExitoso()) {
            mostrarMensaje(
                    respuesta.getMensaje(),
                    VERDE,
                    JOptionPane.INFORMATION_MESSAGE,
                    "Confirmación"
            );
            limpiarFormulario();
            return;
        }

        mostrarMensaje(
                respuesta.getMensaje(),
                ROJO,
                JOptionPane.ERROR_MESSAGE,
                "Error"
        );
    }

    private void solicitarModificacion(
            PeliculaCINEX pelicula
    ) {
        int confirmar = JOptionPane.showConfirmDialog(
                this,
                "¿Desea modificar los datos de la película?\n\n"
                        + pelicula.getTitulo(),
                "Confirmar modificación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }

        RespuestaModificacion respuesta =
                controlPeliculas.modificarPelicula(pelicula);

        if (respuesta.getResultado()
                == ResultadoModificacion.DATOS_INCOMPLETOS) {
            mostrarMensaje(
                    respuesta.getMensaje(),
                    AMARILLO,
                    JOptionPane.WARNING_MESSAGE,
                    "Datos incompletos"
            );
            return;
        }

        if (respuesta.getResultado()
                == ResultadoModificacion
                .PELICULA_YA_REGISTRADA) {
            mostrarMensaje(
                    respuesta.getMensaje(),
                    ROJO,
                    JOptionPane.WARNING_MESSAGE,
                    "Título ya registrado"
            );
            return;
        }

        if (respuesta.getResultado()
                == ResultadoModificacion.CRUCE_HORARIOS) {
            mostrarMensaje(
                    respuesta.getMensaje(),
                    AMARILLO,
                    JOptionPane.WARNING_MESSAGE,
                    "Cruce de horarios"
            );
            return;
        }

        if (respuesta.fueExitosa()) {
            String mensaje = respuesta.getMensaje();

            controlNotificaciones.registrarCambioPelicula(
                    pelicula.getIdPelicula(),
                    pelicula.getTitulo(),
                    "El administrador modificó los datos de la película.",
                    datosOriginalesEdicion,
                    describirPelicula(pelicula),
                    usuarioActual
            );

            datosOriginalesEdicion = "";
            idPeliculaEnEdicion = -1;
            lblTituloFormulario.setText(
                    "Registrar película"
            );
            lblDescripcionFormulario.setText(
                    "Ingrese los datos de la película "
                            + "y seleccione guardar."
            );
            btnGuardar.setText("GUARDAR");
            btnModificar.setText("MODIFICAR PELÍCULA");
            btnEliminar.setEnabled(true);
            limpiarFormulario();

            mostrarMensaje(
                    mensaje,
                    VERDE,
                    JOptionPane.INFORMATION_MESSAGE,
                    "Película modificada"
            );
            return;
        }

        mostrarMensaje(
                respuesta.getMensaje(),
                ROJO,
                JOptionPane.ERROR_MESSAGE,
                "No se pudo modificar"
        );
    }

    private String describirPelicula(PeliculaCINEX pelicula) {
        if (pelicula == null) {
            return "Sin datos";
        }

        return "Título: " + pelicula.getTitulo()
                + "\nGénero: " + pelicula.getGenero()
                + "\nDuración: " + pelicula.getDuracion() + " min"
                + "\nClasificación: " + pelicula.getClasificacion()
                + "\nEstado: " + pelicula.getEstado()
                + "\nImagen: " + pelicula.getImagen();
    }

    // 3. El Administrador ingresa los datos de la película.
    private PeliculaCINEX obtenerPeliculaDesdeFormulario() {
        int duracion = 0;
        String duracionTexto = txtDuracion.getText().trim();
        if (!duracionTexto.isEmpty()) {
            try {
                duracion = Integer.parseInt(duracionTexto);
            } catch (NumberFormatException ignored) {
                duracion = 0;
            }
        }

        String imagen = txtImagen.getText().trim();
        if (imagen.isEmpty()) {
            imagen = "Imagenes/default.png";
        }

        return new PeliculaCINEX(
                txtTitulo.getText().trim(),
                obtenerSeleccionValida(cbGenero, "Seleccione género"),
                duracion,
                obtenerSeleccionValida(cbClasificacion, "Seleccione clasificación"),
                imagen,
                String.valueOf(cbEstado.getSelectedItem()),
                1
        );
    }

    // 6. El sistema registra y muestra mensaje de confirmación.
    // 7. El Administrador visualiza el mensaje.
    private void mostrarMensaje(String mensaje, Color color, int tipo, String titulo) {
        lblMensaje.setText(mensaje);
        lblMensaje.setForeground(color);
        actualizarPanelValidacion(titulo, mensaje, color);
        JOptionPane.showMessageDialog(this, mensaje, titulo, tipo);
    }

    private void actualizarPanelValidacion(String titulo, String mensaje, Color color) {
        if (panelMensajeEstado == null || lblTituloEstado == null || lblTextoEstado == null) {
            return;
        }
        panelMensajeEstado.setBorder(new LineBorder(color, 1, true));
        lblTituloEstado.setText(titulo);
        lblTituloEstado.setForeground(color);
        lblTextoEstado.setText("<html>" + mensaje + "</html>");
        panelMensajeEstado.repaint();
    }

    private void seleccionarImagen() {
        File archivoSeleccionado =
                mostrarSelectorImagenesRecientes();

        if (archivoSeleccionado == null) {
            return;
        }

        if (!esImagenPermitida(archivoSeleccionado)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Solo se permiten imágenes PNG, JPG o JPEG.",
                    "Archivo no permitido",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        File archivoParaMostrar = archivoSeleccionado;
        String rutaParaGuardar =
                archivoSeleccionado.getAbsolutePath();

        try {
            File carpetaImagenes = new File("imagenes");

            if (!carpetaImagenes.exists()
                    && !carpetaImagenes.mkdirs()) {
                throw new IOException(
                        "No se pudo crear la carpeta de imágenes."
                );
            }

            File destino = new File(
                    carpetaImagenes,
                    archivoSeleccionado.getName()
            );

            Files.copy(
                    archivoSeleccionado.toPath(),
                    destino.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

            rutaParaGuardar = "imagenes/" + destino.getName();
            archivoParaMostrar = destino;

        } catch (Exception ex) {
            // Si no se puede copiar, se conserva la ruta original.
            rutaParaGuardar =
                    archivoSeleccionado.getAbsolutePath();
        }

        txtImagen.setText(rutaParaGuardar);
        mostrarPreviewImagen(archivoParaMostrar);
    }

    /**
     * Muestra únicamente imágenes PNG, JPG y JPEG.
     * Las ordena de la más reciente a la más antigua.
     */
    private File mostrarSelectorImagenesRecientes() {
        JDialog dialogo = new JDialog(
                this,
                "Seleccionar imagen de película",
                true
        );

        dialogo.setDefaultCloseOperation(
                JDialog.DISPOSE_ON_CLOSE
        );
        dialogo.setSize(760, 540);
        dialogo.setMinimumSize(new Dimension(680, 470));
        dialogo.setLocationRelativeTo(this);

        JPanel fondo = new JPanel(new BorderLayout(0, 14));
        fondo.setBackground(AZUL_FONDO_1);
        fondo.setBorder(new EmptyBorder(20, 20, 20, 20));
        dialogo.setContentPane(fondo);

        JLabel titulo = new JLabel(
                "Imágenes disponibles"
        );
        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 24));

        JLabel subtitulo = new JLabel(
                "Ordenadas de la más reciente a la más antigua"
        );
        subtitulo.setForeground(GRIS);
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 13));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(
                new BoxLayout(textos, BoxLayout.Y_AXIS)
        );
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(4));
        textos.add(subtitulo);

        JLabel lblCarpeta = new JLabel();
        lblCarpeta.setForeground(AMARILLO);
        lblCarpeta.setFont(
                new Font("Arial", Font.BOLD, 12)
        );

        JButton btnCambiarCarpeta =
                crearBotonSecundario("CAMBIAR CARPETA");
        btnCambiarCarpeta.setPreferredSize(
                new Dimension(170, 39)
        );

        JPanel cabeceraSuperior =
                new JPanel(new BorderLayout(12, 0));
        cabeceraSuperior.setOpaque(false);
        cabeceraSuperior.add(textos, BorderLayout.WEST);
        cabeceraSuperior.add(
                btnCambiarCarpeta,
                BorderLayout.EAST
        );

        JPanel cabecera = new JPanel(new BorderLayout(0, 10));
        cabecera.setOpaque(false);
        cabecera.add(
                cabeceraSuperior,
                BorderLayout.NORTH
        );
        cabecera.add(lblCarpeta, BorderLayout.SOUTH);

        DefaultListModel<File> modelo =
                new DefaultListModel<>();

        JList<File> listaImagenes = new JList<>(modelo);
        listaImagenes.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        listaImagenes.setBackground(AZUL_INPUT);
        listaImagenes.setForeground(BLANCO);
        listaImagenes.setSelectionBackground(
                new Color(20, 60, 115)
        );
        listaImagenes.setSelectionForeground(BLANCO);
        listaImagenes.setFixedCellHeight(52);
        listaImagenes.setFont(
                new Font("Arial", Font.PLAIN, 13)
        );

        SimpleDateFormat formatoFecha =
                new SimpleDateFormat("dd/MM/yyyy  HH:mm");

        listaImagenes.setCellRenderer(
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

                        File archivo = (File) value;
                        String fecha = formatoFecha.format(
                                new Date(archivo.lastModified())
                        );

                        label.setText(
                                "<html><b>"
                                        + archivo.getName()
                                        + "</b><br>"
                                        + "<span style='color:#AEBBD0'>"
                                        + "Modificada: "
                                        + fecha
                                        + "</span></html>"
                        );

                        label.setBorder(
                                new EmptyBorder(5, 12, 5, 12)
                        );

                        return label;
                    }
                }
        );

        JScrollPane scroll =
                new JScrollPane(listaImagenes);
        scroll.setBorder(
                new LineBorder(AZUL_BORDE, 1, true)
        );
        scroll.getViewport().setBackground(AZUL_INPUT);
        estilizarScrollDialogo(scroll);

        JLabel lblResultado = new JLabel(
                "Buscando imágenes..."
        );
        lblResultado.setForeground(GRIS);
        lblResultado.setFont(
                new Font("Arial", Font.BOLD, 13)
        );

        JButton btnCancelar =
                crearBotonSecundario("CANCELAR");
        btnCancelar.setPreferredSize(
                new Dimension(140, 42)
        );

        JButton btnSeleccionar =
                crearBotonPrincipal("SELECCIONAR");
        btnSeleccionar.setPreferredSize(
                new Dimension(165, 42)
        );
        btnSeleccionar.setEnabled(false);

        JPanel acciones = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 0)
        );
        acciones.setOpaque(false);
        acciones.add(btnCancelar);
        acciones.add(btnSeleccionar);

        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setOpaque(false);
        inferior.add(lblResultado, BorderLayout.WEST);
        inferior.add(acciones, BorderLayout.EAST);

        fondo.add(cabecera, BorderLayout.NORTH);
        fondo.add(scroll, BorderLayout.CENTER);
        fondo.add(inferior, BorderLayout.SOUTH);

        File[] carpetaActual = {
                obtenerCarpetaInicialImagenes()
        };

        File[] imagenSeleccionada = {null};

        Runnable cargarImagenes = () -> {
            modelo.clear();

            File carpeta = carpetaActual[0];

            lblCarpeta.setText(
                    "Carpeta: " + carpeta.getAbsolutePath()
            );

            File[] imagenes = carpeta.listFiles(
                    this::esImagenPermitida
            );

            if (imagenes == null) {
                imagenes = new File[0];
            }

            Arrays.sort(
                    imagenes,
                    Comparator.comparingLong(
                            File::lastModified
                    ).reversed()
            );

            for (File imagen : imagenes) {
                modelo.addElement(imagen);
            }

            if (imagenes.length == 0) {
                lblResultado.setText(
                        "No hay imágenes PNG, JPG o JPEG "
                                + "en esta carpeta."
                );
                lblResultado.setForeground(AMARILLO);
            } else {
                lblResultado.setText(
                        "Imágenes encontradas: "
                                + imagenes.length
                );
                lblResultado.setForeground(VERDE);
                listaImagenes.setSelectedIndex(0);
            }
        };

        listaImagenes.addListSelectionListener(e -> {
            btnSeleccionar.setEnabled(
                    listaImagenes.getSelectedValue() != null
            );
        });

        btnCambiarCarpeta.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(
                    carpetaActual[0]
            );

            chooser.setDialogTitle(
                    "Seleccione una carpeta de imágenes"
            );
            chooser.setFileSelectionMode(
                    JFileChooser.DIRECTORIES_ONLY
            );
            chooser.setMultiSelectionEnabled(false);
            chooser.setAcceptAllFileFilterUsed(false);

            int resultado = chooser.showOpenDialog(dialogo);

            if (resultado == JFileChooser.APPROVE_OPTION) {
                carpetaActual[0] =
                        chooser.getSelectedFile();
                cargarImagenes.run();
            }
        });

        btnCancelar.addActionListener(
                e -> dialogo.dispose()
        );

        btnSeleccionar.addActionListener(e -> {
            imagenSeleccionada[0] =
                    listaImagenes.getSelectedValue();

            if (imagenSeleccionada[0] != null) {
                dialogo.dispose();
            }
        });

        listaImagenes.addMouseListener(
                new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(
                            java.awt.event.MouseEvent e
                    ) {
                        if (e.getClickCount() == 2
                                && listaImagenes
                                .getSelectedValue() != null) {
                            imagenSeleccionada[0] =
                                    listaImagenes
                                            .getSelectedValue();
                            dialogo.dispose();
                        }
                    }
                }
        );

        cargarImagenes.run();
        dialogo.setVisible(true);

        return imagenSeleccionada[0];
    }

    private File obtenerCarpetaInicialImagenes() {
        File descargas = new File(
                System.getProperty("user.home"),
                "Downloads"
        );

        if (descargas.exists() && descargas.isDirectory()) {
            return descargas;
        }

        File imagenes = new File(
                System.getProperty("user.home"),
                "Pictures"
        );

        if (imagenes.exists() && imagenes.isDirectory()) {
            return imagenes;
        }

        return new File(System.getProperty("user.home"));
    }

    private boolean esImagenPermitida(File archivo) {
        if (archivo == null || !archivo.isFile()) {
            return false;
        }

        String nombre =
                archivo.getName().toLowerCase();

        return nombre.endsWith(".png")
                || nombre.endsWith(".jpg")
                || nombre.endsWith(".jpeg");
    }

    private void limpiarFormulario() {
        txtTitulo.setText("");
        cbGenero.setSelectedIndex(0);
        txtDuracion.setText("");
        cbClasificacion.setSelectedIndex(0);
        txtImagen.setText("");
        mostrarPreviewImagen(null);
        cbEstado.setSelectedIndex(0);
        txtTitulo.requestFocus();
    }

    private void volverMenu() {
        try {
            new MenuAdministradorCINEXGUI(usuarioActual).setVisible(true);
            dispose();
        } catch (Exception e) {
            dispose();
        }
    }

    private void estilizarScrollDialogo(
            JScrollPane scroll
    ) {
        if (scroll == null) {
            return;
        }

        scroll.setOpaque(true);
        scroll.setBackground(AZUL_INPUT);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(AZUL_INPUT);

        JPanel esquina = new JPanel();
        esquina.setBackground(new Color(10, 38, 83));
        scroll.setCorner(
                JScrollPane.UPPER_RIGHT_CORNER,
                esquina
        );

        scroll.getVerticalScrollBar().setOpaque(true);
        scroll.getVerticalScrollBar().setBackground(AZUL_INPUT);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setUI(
                new ScrollBarCINEXUI()
        );

        scroll.getHorizontalScrollBar().setOpaque(true);
        scroll.getHorizontalScrollBar().setBackground(AZUL_INPUT);
        scroll.getHorizontalScrollBar().setUI(
                new ScrollBarCINEXUI()
        );
    }

    private class RendererEncabezadoDialogo
            extends DefaultTableCellRenderer {

        RendererEncabezadoDialogo() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBackground(new Color(10, 38, 83));
            setForeground(BLANCO);
            setFont(new Font("Arial", Font.BOLD, 13));
            setBorder(
                    new LineBorder(
                            new Color(55, 95, 150),
                            1
                    )
            );
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
                    false,
                    false,
                    row,
                    column
            );

            setText(value == null ? "" : value.toString());
            setOpaque(true);
            setBackground(new Color(10, 38, 83));
            setForeground(BLANCO);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Arial", Font.BOLD, 13));
            setBorder(
                    new LineBorder(
                            new Color(55, 95, 150),
                            1
                    )
            );

            return this;
        }
    }

    private class RendererCeldaDialogo
            extends DefaultTableCellRenderer {

        RendererCeldaDialogo() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(
                    new EmptyBorder(0, 8, 0, 8)
            );
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

            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(
                    new Font(
                            "Arial",
                            column == 1
                                    ? Font.BOLD
                                    : Font.PLAIN,
                            13
                    )
            );

            if (isSelected) {
                setBackground(AMARILLO);
                setForeground(Color.BLACK);
            } else {
                setBackground(
                        row % 2 == 0
                                ? AZUL_INPUT
                                : new Color(10, 34, 75)
                );
                setForeground(BLANCO);
            }

            setBorder(
                    new LineBorder(
                            new Color(55, 95, 150),
                            1
                    )
            );

            return this;
        }
    }

    private class ScrollBarCINEXUI
            extends BasicScrollBarUI {

        @Override
        protected void configureScrollBarColors() {
            trackColor = AZUL_INPUT;
            thumbColor = new Color(70, 105, 155);
            thumbDarkShadowColor = AZUL_BORDE;
            thumbHighlightColor = new Color(95, 130, 180);
            thumbLightShadowColor = AZUL_BORDE;
        }

        @Override
        protected JButton createDecreaseButton(
                int orientation
        ) {
            return crearBotonInvisibleScroll();
        }

        @Override
        protected JButton createIncreaseButton(
                int orientation
        ) {
            return crearBotonInvisibleScroll();
        }

        private JButton crearBotonInvisibleScroll() {
            JButton boton = new JButton();
            Dimension cero = new Dimension(0, 0);

            boton.setPreferredSize(cero);
            boton.setMinimumSize(cero);
            boton.setMaximumSize(cero);
            boton.setBorder(null);
            boton.setContentAreaFilled(false);
            boton.setOpaque(false);

            return boton;
        }

        @Override
        protected void paintTrack(
                Graphics g,
                JComponent componente,
                Rectangle limites
        ) {
            g.setColor(AZUL_INPUT);
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

    class FondoPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            GradientPaint gp = new GradientPaint(0, 0, AZUL_FONDO_1, getWidth(), getHeight(), AZUL_FONDO_2);
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 255, 255, 7));
            g2.fillOval(-180, -90, 520, 520);
            g2.fillOval(getWidth() - 380, 260, 360, 360);

            g2.dispose();
        }
    }

    static class SoloNumerosFilter extends DocumentFilter {
        private final int max;

        public SoloNumerosFilter(int max) {
            this.max = max;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, string, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null) return;

            String actual = fb.getDocument().getText(0, fb.getDocument().getLength());
            String nuevo = actual.substring(0, offset) + text + actual.substring(offset + length);

            if (nuevo.matches("\\d{0," + max + "}")) {
                fb.replace(offset, length, text, attrs);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GestionPeliculasAdminCINEXGUI("admin").setVisible(true));
    }
}
