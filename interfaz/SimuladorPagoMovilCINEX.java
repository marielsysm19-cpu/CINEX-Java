package interfaz;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import control.BDCINEX;


public class SimuladorPagoMovilCINEX extends JDialog {

    private final Color AZUL_FONDO = new Color(3, 18, 45);
    private final Color AZUL_PANEL = new Color(8, 24, 55);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color BLANCO = new Color(245, 245, 245);
    private final Color GRIS = new Color(185, 190, 200);
    private final Color VERDE = new Color(35, 180, 85);
    private final Color ROJO = new Color(190, 45, 45);

    private boolean pagoAprobado = false;

    private JLabel lblMensaje;
    private JLabel lblEstado;
    private JProgressBar barra;
    private JButton btnConfirmar;
    private JButton btnRechazar;

    public SimuladorPagoMovilCINEX(JFrame padre, double monto, String metodoPago, String nombreQR) {
        super(padre, "Pago móvil CINEX", true);

        setSize(500, 565);
        setLocationRelativeTo(padre);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(null);
        root.setBackground(AZUL_FONDO);
        setContentPane(root);

        JLabel lblTitulo = new JLabel(metodoPago + " CINEX", SwingConstants.CENTER);
        lblTitulo.setForeground(BLANCO);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 27));
        lblTitulo.setBounds(0, 20, 500, 35);
        root.add(lblTitulo);

        JLabel lblSubtitulo = new JLabel("Escanee el QR para realizar el pago", SwingConstants.CENTER);
        lblSubtitulo.setForeground(GRIS);
        lblSubtitulo.setFont(new Font("Arial", Font.PLAIN, 14));
        lblSubtitulo.setBounds(0, 56, 500, 24);
        root.add(lblSubtitulo);

        JPanel panelQR = new JPanel(null);
        panelQR.setBackground(AZUL_PANEL);
        panelQR.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));
        panelQR.setBounds(75, 95, 350, 310);
        root.add(panelQR);

        JLabel lblMonto = new JLabel("Monto: S/ " + String.format("%.2f", monto), SwingConstants.CENTER);
        lblMonto.setForeground(AMARILLO);
        lblMonto.setFont(new Font("Arial", Font.BOLD, 25));
        lblMonto.setBounds(0, 15, 350, 35);
        panelQR.add(lblMonto);

        JLabel lblQR = new JLabel("", SwingConstants.CENTER);
        lblQR.setBounds(80, 62, 190, 190);
        lblQR.setBorder(new LineBorder(Color.WHITE, 2, true));
        lblQR.setOpaque(true);
        lblQR.setBackground(Color.WHITE);

        ImageIcon qr = cargarQR(nombreQR, 180, 180);

        if (qr != null) {
            lblQR.setIcon(qr);
        } else {
            lblQR.setForeground(Color.BLACK);
            lblQR.setFont(new Font("Arial", Font.BOLD, 13));
            lblQR.setText("<html><center>QR NO<br>ENCONTRADO<br><br>" + nombreQR + "</center></html>");
        }

        panelQR.add(lblQR);

        lblMensaje = new JLabel("Esperando pago del cliente...", SwingConstants.CENTER);
        lblMensaje.setForeground(BLANCO);
        lblMensaje.setFont(new Font("Arial", Font.PLAIN, 14));
        lblMensaje.setBounds(0, 265, 350, 25);
        panelQR.add(lblMensaje);

        barra = new JProgressBar(0, 100);
        barra.setBounds(80, 292, 190, 12);
        barra.setValue(0);
        panelQR.add(barra);

        lblEstado = new JLabel("El cliente debe escanear el QR desde su aplicativo.", SwingConstants.CENTER);
        lblEstado.setForeground(GRIS);
        lblEstado.setFont(new Font("Arial", Font.PLAIN, 13));
        lblEstado.setBounds(0, 415, 500, 25);
        root.add(lblEstado);

        btnConfirmar = new JButton("CONFIRMAR PAGO");
        btnConfirmar.setBounds(75, 460, 165, 45);
        btnConfirmar.setBackground(AMARILLO);
        btnConfirmar.setForeground(new Color(3, 18, 45));
        btnConfirmar.setFont(new Font("Arial", Font.BOLD, 12));
        btnConfirmar.setFocusPainted(false);
        btnConfirmar.setBorderPainted(false);
        btnConfirmar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        root.add(btnConfirmar);

        btnRechazar = new JButton("RECHAZAR PAGO");
        btnRechazar.setBounds(260, 460, 165, 45);
        btnRechazar.setBackground(ROJO);
        btnRechazar.setForeground(BLANCO);
        btnRechazar.setFont(new Font("Arial", Font.BOLD, 12));
        btnRechazar.setFocusPainted(false);
        btnRechazar.setBorderPainted(false);
        btnRechazar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        root.add(btnRechazar);

        btnConfirmar.addActionListener(e -> iniciarValidacionMovil(metodoPago));
        btnRechazar.addActionListener(e -> rechazarPagoMovil());
    }

    private ImageIcon cargarQR(String nombre, int ancho, int alto) {
        String[] posiblesNombres = {
                nombre,
                "imagenes/qryapeplin.png",
                "Imagenes/qryapeplin.png",
                "qryapeplin.png"
        };

        for (String posible : posiblesNombres) {
            try {
                File archivo = new File(posible);

                if (!archivo.exists()) {
                    continue;
                }

                BufferedImage original = ImageIO.read(archivo);

                if (original == null) {
                    continue;
                }

                BufferedImage escalada = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = escalada.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.drawImage(original, 0, 0, ancho, alto, null);
                g2.dispose();

                return new ImageIcon(escalada);

            } catch (Exception e) {
                System.out.println("No se pudo cargar QR: " + posible);
            }
        }

        return null;
    }

    private void iniciarValidacionMovil(String metodoPago) {
        btnConfirmar.setEnabled(false);
        btnRechazar.setEnabled(false);

        Timer t1 = new Timer(700, e -> {
            lblMensaje.setText("Buscando operación " + metodoPago + "...");
            lblEstado.setForeground(GRIS);
            lblEstado.setText("Consultando pago móvil recibido.");
            barra.setValue(25);
        });

        Timer t2 = new Timer(1500, e -> {
            lblMensaje.setText("Operación encontrada...");
            lblEstado.setText("Validando código de operación.");
            barra.setValue(55);
        });

        Timer t3 = new Timer(2300, e -> {
            lblMensaje.setText("Confirmando pago...");
            lblEstado.setText("Registrando pago en el sistema CINEX.");
            barra.setValue(80);
        });

        Timer t4 = new Timer(3200, e -> {
            lblMensaje.setText("Pago móvil aprobado correctamente.");
            lblEstado.setForeground(VERDE);
            lblEstado.setText("Operación aprobada. Código: " + metodoPago.toUpperCase().replace(" ", "-") + "-" + System.currentTimeMillis());
            barra.setValue(100);
            pagoAprobado = true;

            Timer cerrar = new Timer(900, ev -> dispose());
            cerrar.setRepeats(false);
            cerrar.start();
        });

        t1.setRepeats(false);
        t2.setRepeats(false);
        t3.setRepeats(false);
        t4.setRepeats(false);

        t1.start();
        t2.start();
        t3.start();
        t4.start();
    }

    private void rechazarPagoMovil() {
        btnConfirmar.setEnabled(false);
        btnRechazar.setEnabled(false);

        lblMensaje.setText("Pago móvil rechazado.");
        lblEstado.setForeground(ROJO);
        lblEstado.setText("El pago no fue confirmado por el cliente.");
        barra.setValue(100);
        pagoAprobado = false;

        Timer cerrar = new Timer(900, e -> dispose());
        cerrar.setRepeats(false);
        cerrar.start();
    }

    public boolean isPagoAprobado() {
        return pagoAprobado;
    }
}
