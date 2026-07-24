package interfaz;
import javax.swing.*;
import java.awt.*;


public class SidebarCINEX extends JPanel {

    private final Color AZUL_SIDEBAR = new Color(5, 18, 43);
    private final Color AZUL_ITEM = new Color(10, 28, 60);
    private final Color AMARILLO = new Color(245, 196, 0);
    private final Color GRIS_TEXTO = new Color(190, 198, 214);
    private final Color AZUL_TEXTO_ACTIVO = new Color(5, 20, 55);

    private static final int ANCHO_SIDEBAR = 255;
    private static final int ALTO_ITEM = 58;
    private static final int ANCHO_PUNTA = 22;

    private final int pasoActivo;

    public SidebarCINEX(int pasoActivo) {
        this.pasoActivo = pasoActivo;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(ANCHO_SIDEBAR, 0));
        setMinimumSize(new Dimension(240, 0));
        setBackground(AZUL_SIDEBAR);
        setOpaque(true);
        construir();
    }

    private void construir() {
        String[] pasos = {
                "1. Búsqueda o registro de cliente",
                "2. Elegir película",
                "3. Seleccionar función",
                "4. Seleccionar asientos",
                "5. Tipo de entrada y pago",
                "6. Confirmación"
        };

        JPanel contenedorPasos = new JPanel();
        contenedorPasos.setOpaque(false);
        contenedorPasos.setLayout(new BoxLayout(contenedorPasos, BoxLayout.Y_AXIS));
        contenedorPasos.setBorder(BorderFactory.createEmptyBorder(88, 14, 24, 14));

        for (int i = 0; i < pasos.length; i++) {
            ItemPaso item = new ItemPaso(pasos[i], i == pasoActivo);
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            item.setPreferredSize(new Dimension(220, ALTO_ITEM));
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, ALTO_ITEM));
            item.setMinimumSize(new Dimension(205, ALTO_ITEM));
            contenedorPasos.add(item);
            contenedorPasos.add(Box.createVerticalStrut(8));
        }

        add(contenedorPasos, BorderLayout.CENTER);
    }

    private class ItemPaso extends JPanel {
        private final String texto;
        private final boolean activo;

        public ItemPaso(String texto, boolean activo) {
            this.texto = texto;
            this.activo = activo;
            setOpaque(false);
            setFocusable(true);
            setToolTipText(texto);
            getAccessibleContext().setAccessibleName(texto);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int ancho = getWidth();
            int alto = getHeight();
            int cuerpoAncho = activo ? ancho - ANCHO_PUNTA : ancho;

            if (activo) {
                g2.setColor(AMARILLO);
                g2.fillRoundRect(0, 0, cuerpoAncho, alto, 9, 9);

                Polygon punta = new Polygon();
                punta.addPoint(cuerpoAncho - 1, 0);
                punta.addPoint(ancho - 1, alto / 2);
                punta.addPoint(cuerpoAncho - 1, alto);
                g2.fillPolygon(punta);

                g2.setColor(AZUL_TEXTO_ACTIVO);
            } else {
                g2.setColor(AZUL_ITEM);
                g2.fillRoundRect(0, 0, cuerpoAncho, alto, 9, 9);
                g2.setColor(GRIS_TEXTO);
            }

            int fontSize = texto.length() > 23 ? 11 : (activo ? 14 : 13);
            g2.setFont(new Font("Arial", Font.BOLD, fontSize));

            FontMetrics fm = g2.getFontMetrics();
            int xTexto = 22;
            int yTexto = (alto - fm.getHeight()) / 2 + fm.getAscent();
            String textoFinal = ajustarTexto(g2, texto, cuerpoAncho - 36);
            g2.drawString(textoFinal, xTexto, yTexto);
            g2.dispose();
        }

        private String ajustarTexto(Graphics2D g2, String texto, int anchoMaximo) {
            FontMetrics fm = g2.getFontMetrics();
            if (fm.stringWidth(texto) <= anchoMaximo) return texto;
            String puntos = "...";
            String resultado = texto;
            while (resultado.length() > 0 && fm.stringWidth(resultado + puntos) > anchoMaximo) {
                resultado = resultado.substring(0, resultado.length() - 1);
            }
            return resultado + puntos;
        }
    }
}
