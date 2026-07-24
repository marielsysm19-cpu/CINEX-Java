package interfaz;
import javax.swing.*;
import java.awt.*;


public class ClientesCINEXGUI extends JFrame {
    public ClientesCINEXGUI(String usuario) {
        setTitle("CINEX - Clientes");
        setSize(600,300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(30,30,30,30));
        JLabel msg = new JLabel("<html><center>En CINEX los clientes se registran automáticamente cuando compran una entrada.<br>Este módulo de consulta está en el menú del gerente.</center></html>", SwingConstants.CENTER);
        msg.setFont(new Font("Arial", Font.BOLD, 16));
        p.add(msg, BorderLayout.CENTER);
        JButton volver = new JButton("VOLVER");
        volver.addActionListener(e -> { new MenuTaquilleroCINEXGUI(usuario).setVisible(true); dispose(); });
        p.add(volver, BorderLayout.SOUTH);
        setContentPane(p);
    }
}
