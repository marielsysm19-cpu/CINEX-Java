package interfaz;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.KeyEvent;

/**
 * Cuadros de diálogo propios de CINEX.
 *
 * Esta clase tiene el mismo nombre simple que javax.swing.JOptionPane
 * dentro del paquete interfaz. De esta manera, todos los mensajes y
 * confirmaciones existentes utilizan automáticamente el diseño oscuro
 * sin tener que reescribir cada pantalla.
 */
public final class JOptionPane {

    public static final int ERROR_MESSAGE = 0;
    public static final int INFORMATION_MESSAGE = 1;
    public static final int WARNING_MESSAGE = 2;
    public static final int QUESTION_MESSAGE = 3;
    public static final int PLAIN_MESSAGE = -1;

    public static final int YES_NO_OPTION = 0;

    public static final int YES_OPTION = 0;
    public static final int NO_OPTION = 1;
    public static final int CLOSED_OPTION = -1;

    private static final Color FONDO = new Color(3, 15, 36);
    private static final Color PANEL = new Color(8, 28, 65);
    private static final Color HEADER = new Color(5, 18, 43);
    private static final Color BORDE = new Color(63, 96, 145);
    private static final Color BLANCO = new Color(245, 247, 252);
    private static final Color GRIS = new Color(190, 200, 215);
    private static final Color AMARILLO = new Color(245, 196, 0);
    private static final Color ROJO = new Color(225, 70, 70);

    private JOptionPane() {
    }

    public static void showMessageDialog(
            Component parentComponent,
            Object message
    ) {
        showMessageDialog(
                parentComponent,
                message,
                "CINEX",
                INFORMATION_MESSAGE
        );
    }

    public static void showMessageDialog(
            Component parentComponent,
            Object message,
            String title,
            int messageType
    ) {
        showMessageDialog(
                parentComponent,
                message,
                title,
                messageType,
                null
        );
    }

    public static void showMessageDialog(
            Component parentComponent,
            Object message,
            String title,
            int messageType,
            Icon icon
    ) {
        mostrarDialogo(
                parentComponent,
                message,
                title,
                messageType,
                icon,
                false
        );
    }

    public static int showConfirmDialog(
            Component parentComponent,
            Object message
    ) {
        return showConfirmDialog(
                parentComponent,
                message,
                "Confirmación",
                YES_NO_OPTION,
                QUESTION_MESSAGE
        );
    }

    public static int showConfirmDialog(
            Component parentComponent,
            Object message,
            String title,
            int optionType
    ) {
        return showConfirmDialog(
                parentComponent,
                message,
                title,
                optionType,
                QUESTION_MESSAGE
        );
    }

    public static int showConfirmDialog(
            Component parentComponent,
            Object message,
            String title,
            int optionType,
            int messageType
    ) {
        return showConfirmDialog(
                parentComponent,
                message,
                title,
                optionType,
                messageType,
                null
        );
    }

    public static int showConfirmDialog(
            Component parentComponent,
            Object message,
            String title,
            int optionType,
            int messageType,
            Icon icon
    ) {
        return mostrarDialogo(
                parentComponent,
                message,
                title,
                messageType,
                icon,
                true
        );
    }

    private static int mostrarDialogo(
            Component propietario,
            Object mensaje,
            String titulo,
            int tipo,
            Icon iconoPersonalizado,
            boolean confirmacion
    ) {
        final int[] resultado = {
                confirmacion
                        ? CLOSED_OPTION
                        : YES_OPTION
        };

        Window owner = resolverVentana(propietario);

        JDialog dialogo = owner == null
                ? new JDialog(
                        (Frame) null,
                        tituloSeguro(titulo),
                        true
                )
                : new JDialog(
                        owner,
                        tituloSeguro(titulo),
                        Dialog.ModalityType.APPLICATION_MODAL
                );

        dialogo.setUndecorated(true);
        dialogo.setDefaultCloseOperation(
                JDialog.DISPOSE_ON_CLOSE
        );
        dialogo.setResizable(false);

        JPanel raiz = new JPanel(
                new BorderLayout(0, 0)
        );
        raiz.setBackground(FONDO);
        raiz.setBorder(
                new LineBorder(BORDE, 1, true)
        );

        JPanel cabecera = new JPanel(
                new BorderLayout(12, 0)
        );
        cabecera.setBackground(HEADER);
        cabecera.setBorder(
                new EmptyBorder(11, 16, 11, 10)
        );

        JLabel lblTitulo = new JLabel(
                tituloSeguro(titulo)
        );
        lblTitulo.setForeground(BLANCO);
        lblTitulo.setFont(
                new Font("Arial", Font.BOLD, 15)
        );

        JButton cerrar = new JButton("×");
        cerrar.setPreferredSize(
                new Dimension(34, 28)
        );
        cerrar.setFont(
                new Font("Arial", Font.BOLD, 21)
        );
        cerrar.setToolTipText("Cerrar");
        CINEXResponsive.estabilizarBoton(
                cerrar,
                HEADER,
                BLANCO,
                HEADER,
                BLANCO
        );
        cerrar.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        cerrar.addActionListener(e -> {
            resultado[0] = confirmacion
                    ? CLOSED_OPTION
                    : YES_OPTION;
            dialogo.dispose();
        });

        cabecera.add(lblTitulo, BorderLayout.CENTER);
        cabecera.add(cerrar, BorderLayout.EAST);

        JPanel cuerpo = new JPanel(
                new BorderLayout(16, 0)
        );
        cuerpo.setBackground(PANEL);
        cuerpo.setBorder(
                new EmptyBorder(22, 24, 17, 24)
        );

        JLabel lblIcono = new JLabel(
                iconoPersonalizado == null
                        ? iconoPorTipo(tipo)
                        : iconoPersonalizado
        );
        lblIcono.setVerticalAlignment(
                SwingConstants.TOP
        );

        JComponent contenidoMensaje =
                construirMensaje(mensaje);

        cuerpo.add(lblIcono, BorderLayout.WEST);
        cuerpo.add(contenidoMensaje, BorderLayout.CENTER);

        JPanel acciones = new JPanel(
                new FlowLayout(
                        FlowLayout.CENTER,
                        12,
                        0
                )
        );
        acciones.setBackground(PANEL);
        acciones.setBorder(
                new EmptyBorder(0, 20, 18, 20)
        );

        if (confirmacion) {
            JButton si = CINEXResponsive.botonAmarillo(
                    "SÍ",
                    125,
                    42
            );

            JButton no = CINEXResponsive.botonSecundario(
                    "NO",
                    125,
                    42
            );

            si.addActionListener(e -> {
                resultado[0] = YES_OPTION;
                dialogo.dispose();
            });

            no.addActionListener(e -> {
                resultado[0] = NO_OPTION;
                dialogo.dispose();
            });

            acciones.add(si);
            acciones.add(no);

            dialogo.getRootPane().setDefaultButton(no);
        } else {
            JButton aceptar = CINEXResponsive.botonAmarillo(
                    "ACEPTAR",
                    145,
                    42
            );

            aceptar.addActionListener(e -> {
                resultado[0] = YES_OPTION;
                dialogo.dispose();
            });

            acciones.add(aceptar);
            dialogo.getRootPane().setDefaultButton(
                    aceptar
            );
        }

        raiz.add(cabecera, BorderLayout.NORTH);
        raiz.add(cuerpo, BorderLayout.CENTER);
        raiz.add(acciones, BorderLayout.SOUTH);

        dialogo.setContentPane(raiz);

        dialogo.getRootPane().registerKeyboardAction(
                e -> {
                    resultado[0] = confirmacion
                            ? CLOSED_OPTION
                            : YES_OPTION;
                    dialogo.dispose();
                },
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_ESCAPE,
                        0
                ),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        dialogo.pack();

        int ancho = Math.max(
                440,
                Math.min(720, dialogo.getWidth())
        );

        int alto = Math.max(
                185,
                Math.min(560, dialogo.getHeight())
        );

        dialogo.setSize(ancho, alto);
        dialogo.setLocationRelativeTo(propietario);
        dialogo.setVisible(true);

        return resultado[0];
    }

    private static JComponent construirMensaje(
            Object mensaje
    ) {
        if (mensaje instanceof Component) {
            Component componente = (Component) mensaje;

            if (componente instanceof JComponent) {
                CINEXResponsive.aplicarTemaOscuroArbol(
                        componente
                );
                return (JComponent) componente;
            }

            JPanel panel = new JPanel(
                    new BorderLayout()
            );
            panel.setOpaque(false);
            panel.add(componente, BorderLayout.CENTER);
            return panel;
        }

        String texto = mensaje == null
                ? ""
                : String.valueOf(mensaje);

        if (texto.trim().toLowerCase().startsWith(
                "<html"
        )) {
            JLabel label = new JLabel(texto);
            label.setForeground(BLANCO);
            label.setFont(
                    new Font("Arial", Font.PLAIN, 14)
            );
            label.setVerticalAlignment(
                    SwingConstants.TOP
            );
            label.setPreferredSize(
                    new Dimension(520, 90)
            );
            return label;
        }

        JTextArea area = new JTextArea(texto);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(true);
        area.setBackground(PANEL);
        area.setForeground(BLANCO);
        area.setCaretColor(BLANCO);
        area.setFont(
                new Font("Arial", Font.PLAIN, 14)
        );
        area.setBorder(null);

        int lineasExplicitas =
                Math.max(
                        1,
                        texto.split("\\R", -1).length
                );

        int filasCalculadas =
                Math.max(
                        lineasExplicitas,
                        Math.min(
                                11,
                                Math.max(
                                        2,
                                        texto.length() / 66 + 1
                                )
                        )
                );

        area.setRows(filasCalculadas);
        area.setColumns(48);

        if (filasCalculadas > 9) {
            JScrollPane scroll = new JScrollPane(area);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setBackground(PANEL);
            scroll.setPreferredSize(
                    new Dimension(535, 210)
            );
            return scroll;
        }

        return area;
    }

    private static Window resolverVentana(
            Component propietario
    ) {
        if (propietario instanceof Window) {
            return (Window) propietario;
        }

        return propietario == null
                ? null
                : SwingUtilities.getWindowAncestor(
                        propietario
                );
    }

    private static String tituloSeguro(String titulo) {
        return titulo == null || titulo.trim().isEmpty()
                ? "CINEX"
                : titulo.trim();
    }

    private static Icon iconoPorTipo(int tipo) {
        switch (tipo) {
            case ERROR_MESSAGE:
                return UIManager.getIcon(
                        "OptionPane.errorIcon"
                );

            case WARNING_MESSAGE:
                return UIManager.getIcon(
                        "OptionPane.warningIcon"
                );

            case QUESTION_MESSAGE:
                return UIManager.getIcon(
                        "OptionPane.questionIcon"
                );

            case INFORMATION_MESSAGE:
            default:
                return UIManager.getIcon(
                        "OptionPane.informationIcon"
                );
        }
    }
}
