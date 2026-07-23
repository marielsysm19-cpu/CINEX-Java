package entidad;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mantiene el identificador real de la función durante todo el flujo
 * sin mostrarlo en la interfaz.
 *
 * Formato interno: ID:123||08:00 PM
 */
public final class ReferenciaFuncionCINEX {

    private static final String SEPARADOR = "||";
    private static final Pattern PATRON_ID =
            Pattern.compile("(?i)^\\s*ID\\s*:\\s*(\\d+)");

    private ReferenciaFuncionCINEX() {
    }

    public static String crear(int idFuncion, String textoVisible) {
        String visible = limpiar(textoVisible);

        if (idFuncion <= 0) {
            return visible;
        }

        return "ID:" + idFuncion + SEPARADOR + visible;
    }

    public static String crear(FuncionCINEX funcion) {
        if (funcion == null) {
            return "";
        }

        return crear(funcion.getIdFuncion(), funcion.getHoraBD());
    }

    public static int obtenerId(String referencia) {
        String texto = limpiar(referencia);

        if (texto.isEmpty()) {
            return -1;
        }

        Matcher matcher = PATRON_ID.matcher(texto);

        if (!matcher.find()) {
            return -1;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String mostrar(String referencia) {
        String texto = limpiar(referencia);

        if (texto.isEmpty()) {
            return "";
        }

        int separador = texto.indexOf(SEPARADOR);

        if (separador >= 0) {
            return limpiar(texto.substring(separador + SEPARADOR.length()));
        }

        if (PATRON_ID.matcher(texto).matches()) {
            return "Función";
        }

        return texto;
    }

    public static boolean tieneId(String referencia) {
        return obtenerId(referencia) > 0;
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
