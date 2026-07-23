package control;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class DocumentoAPI {

    private static final String API_DNI_URL = "https://api.factiliza.com/v1/dni/info/{dni}";
    private static final String API_CE_URL = "https://api.factiliza.com/v1/cee/info/{cee}";

   
    private static final String TOKEN =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0MTMxMiIsImh0dHA6Ly9zY2hlbWFzLm1pY3Jvc29mdC5jb20vd3MvMjAwOC8wNi9pZGVudGl0eS9jbGFpbXMvcm9sZSI6ImNvbnN1bHRvciJ9.427WhaTcn0aiMNOJBQ8ZVHUAgsgpEKn5ffyvwqXLF6U";

    public static String consultarDocumento(String tipoDocumento, String numeroDocumento) {
        if (tipoDocumento == null || numeroDocumento == null) {
            return null;
        }

        tipoDocumento = tipoDocumento.trim();
        numeroDocumento = numeroDocumento.trim();

        if ("DNI".equalsIgnoreCase(tipoDocumento)) {
            if (!numeroDocumento.matches("\\d{8}")) {
                return null;
            }

            return consultarDNI(numeroDocumento);
        }

        if ("C.E.".equalsIgnoreCase(tipoDocumento) || "CE".equalsIgnoreCase(tipoDocumento)) {
            if (!numeroDocumento.matches("\\d{9}")) {
                return null;
            }

            return consultarCE(numeroDocumento);
        }

        return null;
    }

    private static String consultarDNI(String dni) {
        String endpoint = API_DNI_URL.replace("{dni}", dni);
        String json = ejecutarGET(endpoint);

        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        return obtenerNombreCortoDesdeJson(json);
    }

    private static String consultarCE(String ce) {
        String endpoint = API_CE_URL.replace("{cee}", ce);
        String json = ejecutarGET(endpoint);

        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        return obtenerNombreCortoDesdeJson(json);
    }

    private static String ejecutarGET(String endpoint) {
        if (TOKEN == null || TOKEN.trim().isEmpty()) {
            System.out.println(
                    "Configure CINEX_FACTILIZA_TOKEN para consultar documentos."
            );
            return null;
        }

        HttpURLConnection con = null;

        try {
            URL url = new URL(endpoint);
            con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("GET");
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Authorization", "Bearer " + TOKEN);

            int codigo = con.getResponseCode();
            BufferedReader br;

            if (codigo >= 200 && codigo < 300) {
                br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)
                );
            } else {
                br = new BufferedReader(
                        new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8)
                );

                String error = leerTodo(br);
                System.out.println("Error API HTTP " + codigo + ": " + error);
                return null;
            }

            return leerTodo(br);

        } catch (Exception e) {
            System.out.println("Error al consultar API de documento: " + e.getMessage());
            return null;

        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private static String leerTodo(BufferedReader br) throws Exception {
        StringBuilder sb = new StringBuilder();
        String linea;

        while ((linea = br.readLine()) != null) {
            sb.append(linea);
        }

        br.close();
        return sb.toString();
    }

    private static String obtenerNombreCortoDesdeJson(String json) {
        /*
         * Devuelve siempre:
         * PRIMER NOMBRE + PRIMER APELLIDO
         *
         * Ejemplo:
         * MARIELSYS PAOLA MARTINO LOPEZ -> Marielsys Martino
         */

        String nombres = extraerValor(json, "nombres");
        String apellidoPaterno = extraerValor(json, "apellido_paterno");

        if (apellidoPaterno.isEmpty()) {
            apellidoPaterno = extraerValor(json, "apellidoPaterno");
        }

        if (nombres.isEmpty()) {
            String primerNombre = extraerValor(json, "primer_nombre");
            String segundoNombre = extraerValor(json, "segundo_nombre");
            nombres = (primerNombre + " " + segundoNombre).trim();
        }

        if (!nombres.isEmpty() && !apellidoPaterno.isEmpty()) {
            return formatearNombreCorto(nombres, apellidoPaterno);
        }

        String nombreCompleto = extraerValor(json, "nombre_completo");

        if (!nombreCompleto.isEmpty()) {
            return formatearNombreCliente(nombreCompleto);
        }

        String nombre = extraerValor(json, "nombre");

        if (!nombre.isEmpty()) {
            return formatearNombreCliente(nombre);
        }

        return null;
    }

    public static String formatearNombreCliente(String nombreCompleto) {
        nombreCompleto = limpiarTexto(nombreCompleto);

        if (nombreCompleto.isEmpty()) {
            return "";
        }

        String[] partes = nombreCompleto.split("\\s+");

        if (partes.length == 1) {
            return capitalizar(partes[0]);
        }

        /*
         * Caso de API que devuelve:
         * APELLIDO_PATERNO APELLIDO_MATERNO NOMBRES
         * Ejemplo:
         * MARTINO LOPEZ MARIELSYS PAOLA
         * Resultado:
         * Marielsys Martino
         */
        if (partes.length >= 3 && estaEnMayusculas(nombreCompleto)) {
            String primerApellido = partes[0];
            String primerNombre = partes[2];
            return capitalizar(primerNombre + " " + primerApellido);
        }

        /*
         * Caso normal:
         * NOMBRES APELLIDOS
         * Ejemplo:
         * MARIELSYS PAOLA MARTINO LOPEZ
         * Resultado:
         * Marielsys Martino
         */
        String primerNombre = partes[0];
        String primerApellido = partes.length >= 3 ? partes[2] : partes[1];

        return capitalizar(primerNombre + " " + primerApellido);
    }

    private static String formatearNombreCorto(String nombres, String apellidoPaterno) {
        nombres = limpiarTexto(nombres);
        apellidoPaterno = limpiarTexto(apellidoPaterno);

        String primerNombre = "";

        if (!nombres.isEmpty()) {
            primerNombre = nombres.split("\\s+")[0];
        }

        String primerApellido = "";

        if (!apellidoPaterno.isEmpty()) {
            primerApellido = apellidoPaterno.split("\\s+")[0];
        }

        return capitalizar(primerNombre + " " + primerApellido);
    }

    private static boolean estaEnMayusculas(String texto) {
        return texto.equals(texto.toUpperCase());
    }

    private static String capitalizar(String texto) {
        texto = limpiarTexto(texto).toLowerCase();

        if (texto.isEmpty()) {
            return "";
        }

        String[] palabras = texto.split("\\s+");
        StringBuilder resultado = new StringBuilder();

        for (String palabra : palabras) {
            if (palabra.isEmpty()) {
                continue;
            }

            resultado.append(Character.toUpperCase(palabra.charAt(0)));

            if (palabra.length() > 1) {
                resultado.append(palabra.substring(1));
            }

            resultado.append(" ");
        }

        return resultado.toString().trim();
    }

    private static String extraerValor(String json, String clave) {
        try {
            String patron = "\"" + clave + "\"";
            int pos = json.indexOf(patron);

            if (pos == -1) {
                return "";
            }

            int dosPuntos = json.indexOf(":", pos);

            if (dosPuntos == -1) {
                return "";
            }

            int inicioComilla = json.indexOf("\"", dosPuntos + 1);

            if (inicioComilla == -1) {
                return "";
            }

            int finComilla = json.indexOf("\"", inicioComilla + 1);

            if (finComilla == -1) {
                return "";
            }

            return json.substring(inicioComilla + 1, finComilla).trim();

        } catch (Exception e) {
            return "";
        }
    }

    private static String limpiarTexto(String texto) {
        if (texto == null) {
            return "";
        }

        return texto
                .replace("\\/", "/")
                .replace("\\\"", "\"")
                .replace("\\n", " ")
                .replace("\\t", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
