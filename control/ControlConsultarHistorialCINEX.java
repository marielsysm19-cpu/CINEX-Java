package control;

import entidad.EntradaCINEX;
import entidad.PagoCINEX;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ControlConsultarHistorialCINEX {

    private ControlConsultarHistorialCINEX() {
    }

    /**
     * Equivale al mensaje del diagrama: consultarHistorialCompras().
     * Devuelve datos de e_entrada enriquecidos con su e_pago asociado para llenar la tabla.
     */
    public static ArrayList<EntradaCINEX> consultarHistorialCompras(int idCliente) throws SQLException {
        return consultarEntradaClientes(idCliente);
    }

    /**
     * Equivale al mensaje del diagrama hacia e_entrada: consultarEntradaClientes().
     */
    public static ArrayList<EntradaCINEX> consultarEntradaClientes(int idCliente) throws SQLException {
        ArrayList<EntradaCINEX> entradas = new ArrayList<>();
        ControlModuloReembolsosCINEX.asegurarEstructura();

        try (Connection con = BDCINEX.conectar()) {
            String columnaFechaVenta = columnaExiste(con, "ventas", "fecha_venta") ? "fecha_venta" : "fecha_hora";
            boolean tieneIdFuncionVenta = columnaExiste(con, "ventas", "id_funcion");
            boolean tieneEstadoPago = columnaExiste(con, "pagos", "estado");

            String funcionJoin = tieneIdFuncionVenta
                    ? "LEFT JOIN funciones f ON f.id_funcion = COALESCE(v.id_funcion, ed.id_funcion) "
                    : "LEFT JOIN funciones f ON f.id_funcion = ed.id_funcion ";

            String estadoPagoSelect = tieneEstadoPago
                    ? "IFNULL(p.estado, '-') AS estado_pago, "
                    : "'-' AS estado_pago, ";

            String sql =
                    "SELECT v.id_venta, " +
                            "IFNULL(v.numero_venta, CONCAT('VTA-', v.id_venta)) AS numero_venta, " +
                            "v." + columnaFechaVenta + " AS fecha_compra, " +
                            "v.total, " +
                            "IFNULL(v.estado, 'Registrada') AS estado_venta, " +
                            "IFNULL(p.id_pago, 0) AS id_pago, " +
                            "IFNULL(p.metodo_pago, '-') AS metodo_pago, " +
                            estadoPagoSelect +
                            "IFNULL(pel.titulo, '-') AS pelicula, " +
                            "IFNULL(s.nombre, '-') AS sala, " +
                            "IFNULL(DATE_FORMAT(f.fecha, '%d/%m/%Y'), '-') AS fecha_funcion, " +
                            "IFNULL(TIME_FORMAT(f.hora, '%h:%i %p'), '-') AS hora_funcion, " +
                            "IFNULL(ed.asientos, '-') AS asientos, " +
                            "IFNULL(ed.cantidad_entradas, 0) AS cantidad_entradas, " +
                            "IFNULL(ref.entradas_reembolsadas, 0) AS entradas_reembolsadas, " +
                            "IFNULL(ref.asientos_reembolsados, '') AS asientos_reembolsados, " +
                            "IFNULL(ref.monto_reembolsado, 0) AS monto_reembolsado, " +
                            "CASE " +
                            "WHEN IFNULL(ref.entradas_reembolsadas, 0) = 0 THEN 'Sin reembolso' " +
                            "WHEN IFNULL(ref.entradas_reembolsadas, 0) >= IFNULL(ed.cantidad_entradas, 0) THEN 'Reembolso total' " +
                            "ELSE 'Reembolso parcial' END AS estado_reembolso " +
                    "FROM ventas v " +
                    "LEFT JOIN pagos p ON p.id_venta = v.id_venta " +
                    "LEFT JOIN ( " +
                    "   SELECT e.id_venta, MIN(e.id_entrada) AS id_entrada, MIN(e.id_funcion) AS id_funcion, COUNT(*) AS cantidad_entradas, " +
                    "          GROUP_CONCAT(CONCAT(a.fila, a.numero) ORDER BY a.fila, a.numero SEPARATOR ', ') AS asientos " +
                    "   FROM entradas e " +
                    "   LEFT JOIN asientos a ON a.id_asiento = e.id_asiento " +
                    "   WHERE e.estado IS NULL OR e.estado <> 'Anulada' " +
                    "   GROUP BY e.id_venta " +
                    ") ed ON ed.id_venta = v.id_venta " +
                    "LEFT JOIN ( " +
                    "   SELECT r.id_venta, SUM(r.monto_total) AS monto_reembolsado, " +
                    "          COUNT(DISTINCT dr.id_entrada) AS entradas_reembolsadas, " +
                    "          GROUP_CONCAT(DISTINCT dr.asiento ORDER BY dr.asiento SEPARATOR ', ') AS asientos_reembolsados " +
                    "   FROM reembolsos r " +
                    "   LEFT JOIN detalle_reembolsos dr ON dr.id_reembolso = r.id_reembolso " +
                    "   GROUP BY r.id_venta " +
                    ") ref ON ref.id_venta = v.id_venta " +
                    funcionJoin +
                    "LEFT JOIN peliculas pel ON pel.id_pelicula = f.id_pelicula " +
                    "LEFT JOIN salas s ON s.id_sala = f.id_sala " +
                    "WHERE v.id_cliente = ? " +
                    "ORDER BY v." + columnaFechaVenta + " DESC, v.id_venta DESC";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idCliente);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Timestamp fecha = rs.getTimestamp("fecha_compra");
                        String fechaCompra = formatearFechaHora(fecha);
                        String fechaCorta = formatearFechaCorta(fecha);
                        String funcion = rs.getString("fecha_funcion") + " - " + rs.getString("hora_funcion");
                        String estadoVenta = rs.getString("estado_venta");

                        PagoCINEX pago = new PagoCINEX(
                                rs.getInt("id_pago"),
                                rs.getString("metodo_pago"),
                                rs.getString("estado_pago"),
                                rs.getDouble("total")
                        );

                        EntradaCINEX entrada = new EntradaCINEX(
                                rs.getInt("id_venta"),
                                rs.getString("numero_venta"),
                                fechaCompra,
                                fechaCorta,
                                rs.getString("pelicula"),
                                funcion,
                                rs.getString("sala"),
                                rs.getString("asientos"),
                                rs.getInt("cantidad_entradas"),
                                estadoVenta,
                                pago
                        );

                        entrada.setEntradasReembolsadas(
                                rs.getInt("entradas_reembolsadas")
                        );
                        entrada.setAsientosReembolsados(
                                rs.getString("asientos_reembolsados")
                        );
                        entrada.setMontoReembolsado(
                                rs.getDouble("monto_reembolsado")
                        );
                        entrada.setEstadoReembolso(
                                rs.getString("estado_reembolso")
                        );

                        entradas.add(entrada);
                    }
                }
            }
        }

        return entradas;
    }

    /**
     * Equivale al mensaje del diagrama hacia e_pago: consultarPagoClientes().
     */
    public static ArrayList<PagoCINEX> consultarPagoClientes(int idCliente) throws SQLException {
        ArrayList<PagoCINEX> pagos = new ArrayList<>();

        String sql = "SELECT IFNULL(p.id_pago, 0) AS id_pago, " +
                "IFNULL(p.metodo_pago, '-') AS metodo_pago, " +
                "IFNULL(p.estado, '-') AS estado_pago, " +
                "IFNULL(v.total, 0) AS total " +
                "FROM ventas v " +
                "LEFT JOIN pagos p ON p.id_venta = v.id_venta " +
                "WHERE v.id_cliente = ? " +
                "ORDER BY v.id_venta DESC";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idCliente);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pagos.add(new PagoCINEX(
                            rs.getInt("id_pago"),
                            rs.getString("metodo_pago"),
                            rs.getString("estado_pago"),
                            rs.getDouble("total")
                    ));
                }
            }
        }

        return pagos;
    }

    private static boolean columnaExiste(Connection con, String tabla, String columna) {
        try {
            DatabaseMetaData meta = con.getMetaData();
            String catalogo = con.getCatalog();

            try (ResultSet rs = meta.getColumns(catalogo, null, tabla, columna)) {
                if (rs.next()) return true;
            }

            try (ResultSet rs = meta.getColumns(catalogo, null, tabla.toLowerCase(), columna)) {
                if (rs.next()) return true;
            }

            try (ResultSet rs = meta.getColumns(catalogo, null, tabla.toUpperCase(), columna)) {
                if (rs.next()) return true;
            }

            try (ResultSet rs = meta.getColumns(null, null, tabla, columna)) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static String formatearFechaHora(Timestamp fecha) {
        if (fecha == null) {
            return "-";
        }
        return new SimpleDateFormat("dd/MM/yyyy hh:mm a").format(fecha);
    }

    private static String formatearFechaCorta(Timestamp fecha) {
        if (fecha == null) {
            return "-";
        }
        return new SimpleDateFormat("dd/MM").format(fecha);
    }
}
