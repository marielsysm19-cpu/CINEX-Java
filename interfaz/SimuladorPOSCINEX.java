package interfaz;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import control.BDCINEX;


public class SimuladorPOSCINEX extends JDialog {

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
    private JButton btnPagar;

    public SimuladorPOSCINEX(JFrame padre, double monto, String metodoPago) {
        super(padre, "POS CINEX", true);

        setSize(460, 410);
        setLocationRelativeTo(padre);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(null);
        root.setBackground(AZUL_FONDO);
        setContentPane(root);

        JLabel lblTitulo = new JLabel("POS CINEX", SwingConstants.CENTER);
        lblTitulo.setForeground(BLANCO);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 28));
        lblTitulo.setBounds(0, 22, 460, 35);
        root.add(lblTitulo);

        JLabel lblSubtitulo = new JLabel("Simulación de terminal de pago", SwingConstants.CENTER);
        lblSubtitulo.setForeground(GRIS);
        lblSubtitulo.setFont(new Font("Arial", Font.PLAIN, 14));
        lblSubtitulo.setBounds(0, 58, 460, 24);
        root.add(lblSubtitulo);

        JPanel tarjetaPOS = new JPanel(null);
        tarjetaPOS.setBackground(AZUL_PANEL);
        tarjetaPOS.setBorder(new LineBorder(new Color(80, 105, 145), 1, true));
        tarjetaPOS.setBounds(55, 95, 350, 190);
        root.add(tarjetaPOS);

        JLabel lblMetodo = new JLabel("Método: " + metodoPago);
        lblMetodo.setForeground(BLANCO);
        lblMetodo.setFont(new Font("Arial", Font.BOLD, 15));
        lblMetodo.setBounds(25, 20, 300, 25);
        tarjetaPOS.add(lblMetodo);

        JLabel lblMonto = new JLabel("S/ " + String.format("%.2f", monto), SwingConstants.CENTER);
        lblMonto.setForeground(AMARILLO);
        lblMonto.setFont(new Font("Arial", Font.BOLD, 34));
        lblMonto.setBounds(0, 58, 350, 45);
        tarjetaPOS.add(lblMonto);

        lblMensaje = new JLabel("Acerque, inserte o deslice la tarjeta", SwingConstants.CENTER);
        lblMensaje.setForeground(BLANCO);
        lblMensaje.setFont(new Font("Arial", Font.PLAIN, 14));
        lblMensaje.setBounds(0, 112, 350, 24);
        tarjetaPOS.add(lblMensaje);

        barra = new JProgressBar(0, 100);
        barra.setBounds(35, 148, 280, 18);
        barra.setStringPainted(false);
        barra.setValue(0);
        tarjetaPOS.add(barra);

        lblEstado = new JLabel("Esperando operación...", SwingConstants.CENTER);
        lblEstado.setForeground(GRIS);
        lblEstado.setFont(new Font("Arial", Font.PLAIN, 13));
        lblEstado.setBounds(0, 292, 460, 25);
        root.add(lblEstado);

        btnPagar = new JButton("PAGAR");
        btnPagar.setBounds(115, 325, 230, 45);
        btnPagar.setBackground(AMARILLO);
        btnPagar.setForeground(new Color(3, 18, 45));
        btnPagar.setFont(new Font("Arial", Font.BOLD, 14));
        btnPagar.setFocusPainted(false);
        btnPagar.setBorderPainted(false);
        btnPagar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        root.add(btnPagar);

        btnPagar.addActionListener(e -> iniciarProcesoPOS());
    }

    private void iniciarProcesoPOS() {
        btnPagar.setEnabled(false);

        Timer t1 = new Timer(700, e -> {
            lblMensaje.setText("Conectando con POS...");
            lblEstado.setForeground(GRIS);
            lblEstado.setText("Estableciendo comunicación con el terminal.");
            barra.setValue(20);
        });

        Timer t2 = new Timer(1400, e -> {
            lblMensaje.setText("Leyendo tarjeta...");
            lblEstado.setText("Tarjeta detectada correctamente.");
            barra.setValue(45);
        });

        Timer t3 = new Timer(2100, e -> {
            lblMensaje.setText("Validando operación...");
            lblEstado.setText("Consultando autorización bancaria.");
            barra.setValue(70);
        });

        Timer t4 = new Timer(3000, e -> {
            lblMensaje.setText("Pago aprobado correctamente.");
            lblEstado.setForeground(VERDE);
            lblEstado.setText("Operación aprobada. Código POS: POS-" + System.currentTimeMillis());
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


    public boolean isPagoAprobado() {
        return pagoAprobado;
    }
}
