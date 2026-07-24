package interfaz;
import javax.swing.*;
import java.awt.*;


public final class CINEXTransiciones {

    private CINEXTransiciones() {}

    public static void cambiar(JFrame actual, JFrame siguiente) {
        if (siguiente == null) return;

        if (actual != null) {
            try {
                siguiente.setBounds(actual.getBounds());
                siguiente.setExtendedState(actual.getExtendedState());
            } catch (Exception ignored) {}
        }

        SwingUtilities.invokeLater(() -> {
            CINEXResponsive.aplicarTemaOscuroArbol(
                    siguiente.getContentPane()
            );
            siguiente.setVisible(true);

            SwingUtilities.invokeLater(() ->
                    CINEXResponsive.aplicarTemaOscuroArbol(
                            siguiente.getContentPane()
                    )
            );

            if (actual != null) {
                actual.dispose();
            }
        });
    }

    public static void abrir(JFrame siguiente) {
        cambiar(null, siguiente);
    }
}
