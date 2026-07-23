package control;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Lee credenciales desde:
 * 1) Propiedades Java (-D...)
 * 2) Variables de entorno
 * 3) config/cinex.local.properties (archivo ignorado por Git)
 */
public final class ConfiguracionCINEX {

    private static final Properties LOCAL = cargarArchivoLocal();

    private ConfiguracionCINEX() {
    }

    public static String obtener(
            String propiedadJava,
            String variableEntorno,
            String claveArchivo,
            String valorPredeterminado
    ) {
        String valor = System.getProperty(propiedadJava);

        if (estaVacio(valor)) {
            valor = System.getenv(variableEntorno);
        }

        if (estaVacio(valor)) {
            valor = LOCAL.getProperty(claveArchivo);
        }

        if (estaVacio(valor)) {
            valor = valorPredeterminado;
        }

        return valor == null ? "" : valor.trim();
    }

    private static Properties cargarArchivoLocal() {
        Properties propiedades = new Properties();

        File archivo = new File("config", "cinex.local.properties");

        if (!archivo.exists() || !archivo.isFile()) {
            return propiedades;
        }

        try (InputStream entrada = new FileInputStream(archivo)) {
            propiedades.load(entrada);
        } catch (Exception e) {
            System.out.println(
                    "No se pudo leer config/cinex.local.properties: "
                            + e.getMessage()
            );
        }

        return propiedades;
    }

    private static boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
