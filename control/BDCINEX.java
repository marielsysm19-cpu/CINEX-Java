package control;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Única responsabilidad: abrir la conexión física con la base de datos CINEX.
 *
 * No contiene consultas de negocio, CRUD, login, ventas, usuarios, películas,
 * funciones, precios, salas, asientos, reportes ni comprobantes.
 * Esas operaciones pertenecen a sus clases Control correspondientes.
 */
public final class BDCINEX {

    // Cambiar a true únicamente para la demostración de BD no disponible.
    private static final boolean SIMULAR_BD_APAGADA = false;

    private static final String URL =
            "jdbc:mysql://tokaido.proxy.rlwy.net:25706/railway"
                    + "?useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=America/Lima"
                    + "&useUnicode=true"
                    + "&characterEncoding=UTF-8";

    private static final String USUARIO = "root";
    private static final String CLAVE = "EFTxbLnITmSGuFRadKWWiCAyPoaSrmJU";

    private static volatile boolean driverCargado = false;

    private BDCINEX() {
    }

    public static Connection conectar() throws SQLException {
        if (SIMULAR_BD_APAGADA) {
            throw new SQLException(
                    "Simulación activa: la base de datos CINEX no está disponible."
            );
        }

        cargarDriverMySQL();
        Connection con = DriverManager.getConnection(URL, USUARIO, CLAVE);
        configurarZonaHoraria(con);
        return con;
    }

    private static void cargarDriverMySQL() throws SQLException {
        if (driverCargado) {
            return;
        }

        synchronized (BDCINEX.class) {
            if (driverCargado) {
                return;
            }

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                driverCargado = true;
            } catch (ClassNotFoundException e1) {
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                    driverCargado = true;
                } catch (ClassNotFoundException e2) {
                    throw new SQLException(
                            "No se encontró el driver MySQL Connector/J en la librería del proyecto.",
                            e2
                    );
                }
            }
        }
    }

    private static void configurarZonaHoraria(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("SET time_zone = '-05:00'");
        }
    }
}
