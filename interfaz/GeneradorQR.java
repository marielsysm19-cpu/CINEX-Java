package interfaz;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import javax.imageio.ImageIO;
import control.BDCINEX;


public class GeneradorQR {

    private static final Path CARPETA_QR = Paths.get("qr_ventas");
    private static final Random RANDOM = new Random();

    public static String generarNumeroVenta() {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int aleatorio = RANDOM.nextInt(900) + 100;
        return "VTA-" + fecha + "-" + aleatorio;
    }

    public static String crearTextoQR(String numeroVenta, String pelicula, String funcion, String sala,
                                      String asientos, double total, String metodoPago, String vendedor, String fecha) {
        return "CINEX - TICKET DIGITAL\n" +
                "Venta: " + valorSeguro(numeroVenta) + "\n" +
                "Pelicula: " + valorSeguro(pelicula) + "\n" +
                "Funcion: " + valorSeguro(funcion) + "\n" +
                "Sala: " + valorSeguro(sala) + "\n" +
                "Asientos: " + valorSeguro(asientos) + "\n" +
                "Total: S/ " + String.format("%.2f", total) + "\n" +
                "Metodo de pago: " + valorSeguro(metodoPago) + "\n" +
                "Vendedor: " + valorSeguro(vendedor) + "\n" +
                "Fecha: " + valorSeguro(fecha) + "\n" +
                "Ticket valido para ingreso.";
    }

    public static BufferedImage generarQRVentaComoImagen(String numeroVenta, String pelicula, String funcion, String sala,
                                                         String asientos, double total, String metodoPago,
                                                         String vendedor, String fecha, int tamanio) {
        int size = Math.max(120, tamanio);
        String texto = crearTextoQR(numeroVenta, pelicula, funcion, sala, asientos, total, metodoPago, vendedor, fecha);

        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = img.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.setColor(Color.BLACK);

        int celdas = 29;
        int margen = Math.max(8, size / 18);
        int area = size - margen * 2;
        int celda = Math.max(2, area / celdas);
        int offset = (size - (celda * celdas)) / 2;
        int hash = texto.hashCode();

        dibujarMarca(g, offset, offset, celda);
        dibujarMarca(g, offset + celda * 22, offset, celda);
        dibujarMarca(g, offset, offset + celda * 22, celda);

        for (int y = 0; y < celdas; y++) {
            for (int x = 0; x < celdas; x++) {
                if (enMarca(x, y)) {
                    continue;
                }

                int v = mezcla(hash, x, y);
                if ((v & 1) == 0 || v % 11 == 0) {
                    g.fillRect(offset + x * celda, offset + y * celda, celda, celda);
                }
            }
        }

        g.dispose();
        return img;
    }

    private static int mezcla(int hash, int x, int y) {
        int v = hash;
        v ^= x * 0x45d9f3b;
        v ^= y * 0x119de1f3;
        v ^= (x * y * 31);
        v ^= (v >>> 16);
        return Math.abs(v);
    }

    private static void dibujarMarca(Graphics2D g, int x, int y, int celda) {
        g.setColor(Color.BLACK);
        g.fillRect(x, y, celda * 7, celda * 7);

        g.setColor(Color.WHITE);
        g.fillRect(x + celda, y + celda, celda * 5, celda * 5);

        g.setColor(Color.BLACK);
        g.fillRect(x + celda * 2, y + celda * 2, celda * 3, celda * 3);
    }

    private static boolean enMarca(int x, int y) {
        return (x < 8 && y < 8) || (x > 20 && y < 8) || (x < 8 && y > 20);
    }

    public static String guardarQRVentaPNG(String numeroVenta, BufferedImage imagenQR) {
        try {
            if (!Files.exists(CARPETA_QR)) {
                Files.createDirectories(CARPETA_QR);
            }

            String nombreSeguro = limpiarNombreArchivo(numeroVenta);
            Path ruta = CARPETA_QR.resolve("qr_" + nombreSeguro + ".png");

            if (imagenQR == null) {
                return null;
            }

            ImageIO.write(imagenQR, "PNG", ruta.toFile());
            return ruta.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String guardarQRVentaPNG(String numeroVenta, String pelicula, String funcion, String sala,
                                           String asientos, double total, String metodoPago, String vendedor,
                                           String fecha, int tamanio) {
        BufferedImage imagen = generarQRVentaComoImagen(
                numeroVenta,
                pelicula,
                funcion,
                sala,
                asientos,
                total,
                metodoPago,
                vendedor,
                fecha,
                tamanio
        );

        return guardarQRVentaPNG(numeroVenta, imagen);
    }

    private static String limpiarNombreArchivo(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "venta";
        }

        return texto.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private static String valorSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
