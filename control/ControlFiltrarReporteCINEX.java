package control;

import entidad.VentaCINEX;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;

public class ControlFiltrarReporteCINEX {

    public ArrayList<VentaCINEX> solicitarFiltros(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String sala,
            String metodoPago
    ) {
        return consultarVentas(
                fechaInicio,
                fechaFin,
                sala,
                metodoPago
        );
    }

    public ArrayList<VentaCINEX> consultarVentas(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String sala,
            String metodoPago
    ) {
        ArrayList<VentaCINEX> ventas = new ArrayList<>();

        if (fechaInicio == null
                || fechaFin == null
                || fechaInicio.isAfter(fechaFin)) {
            return ventas;
        }

        ControlModuloReembolsosCINEX.asegurarEstructura();

        try (Connection con = BDCINEX.conectar()) {
            String fechaCol = columnaExiste(
                    con,
                    "ventas",
                    "fecha_venta"
            ) ? "fecha_venta" : "fecha_hora";

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT v.id_venta, ");
            sql.append("IFNULL(v.numero_venta, CONCAT('VTA-', LPAD(v.id_venta, 6, '0'))) AS numero_venta, ");
            sql.append("v.").append(fechaCol).append(" AS fecha_hora, ");
            sql.append("IFNULL(MIN(pel.titulo), '-') AS pelicula, ");
            sql.append("IFNULL(MIN(s.nombre), '-') AS sala, ");
            sql.append("IFNULL(MAX(pg.metodo_pago), '-') AS metodo_pago, ");
            sql.append("IFNULL(v.total, 0) AS total_bruto, ");
            sql.append("COUNT(e.id_entrada) AS entradas_vendidas, ");
            sql.append("IFNULL(MAX(ref.monto_reembolsado), 0) AS monto_reembolsado, ");
            sql.append("IFNULL(MAX(ref.entradas_reembolsadas), 0) AS entradas_reembolsadas, ");
            sql.append("IFNULL(MAX(ref.asientos_reembolsados), '') AS asientos_reembolsados, ");
            sql.append("CASE ");
            sql.append("WHEN IFNULL(MAX(ref.entradas_reembolsadas), 0) = 0 THEN 'Sin reembolso' ");
            sql.append("WHEN IFNULL(MAX(ref.entradas_reembolsadas), 0) >= COUNT(e.id_entrada) THEN 'Reembolso total' ");
            sql.append("ELSE 'Reembolso parcial' END AS estado_reembolso ");
            sql.append("FROM ventas v ");
            sql.append("LEFT JOIN pagos pg ON pg.id_venta = v.id_venta ");
            sql.append("LEFT JOIN entradas e ON e.id_venta = v.id_venta ");
            sql.append("LEFT JOIN funciones f ON f.id_funcion = COALESCE(e.id_funcion, v.id_funcion) ");
            sql.append("LEFT JOIN peliculas pel ON pel.id_pelicula = f.id_pelicula ");
            sql.append("LEFT JOIN salas s ON s.id_sala = f.id_sala ");
            sql.append("LEFT JOIN ( ");
            sql.append("   SELECT r.id_venta, SUM(r.monto_total) AS monto_reembolsado, ");
            sql.append("          COUNT(DISTINCT dr.id_entrada) AS entradas_reembolsadas, ");
            sql.append("          GROUP_CONCAT(DISTINCT dr.asiento ORDER BY dr.asiento SEPARATOR ', ') AS asientos_reembolsados ");
            sql.append("   FROM reembolsos r ");
            sql.append("   LEFT JOIN detalle_reembolsos dr ON dr.id_reembolso = r.id_reembolso ");
            sql.append("   GROUP BY r.id_venta ");
            sql.append(") ref ON ref.id_venta = v.id_venta ");
            sql.append("WHERE v.").append(fechaCol).append(" >= ? AND v.").append(fechaCol).append(" < ? ");

            ArrayList<Object> parametros = new ArrayList<>();
            parametros.add(Timestamp.valueOf(fechaInicio.atStartOfDay()));
            parametros.add(Timestamp.valueOf(fechaFin.plusDays(1).atStartOfDay()));

            if (sala != null
                    && !sala.trim().isEmpty()
                    && !"Todas".equalsIgnoreCase(sala.trim())) {
                sql.append("AND s.nombre = ? ");
                parametros.add(sala.trim());
            }

            if (metodoPago != null
                    && !metodoPago.trim().isEmpty()
                    && !"Todos".equalsIgnoreCase(metodoPago.trim())) {
                sql.append("AND TRIM(pg.metodo_pago) = ? ");
                parametros.add(metodoPago.trim());
            }

            sql.append("GROUP BY v.id_venta, v.numero_venta, v.")
                    .append(fechaCol)
                    .append(", v.total ");
            sql.append("ORDER BY v.")
                    .append(fechaCol)
                    .append(" DESC, v.id_venta DESC");

            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                for (int i = 0; i < parametros.size(); i++) {
                    ps.setObject(i + 1, parametros.get(i));
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Timestamp fecha = rs.getTimestamp("fecha_hora");

                        VentaCINEX venta = new VentaCINEX(
                                rs.getInt("id_venta"),
                                rs.getString("numero_venta"),
                                fecha == null ? null : fecha.toLocalDateTime(),
                                rs.getString("pelicula"),
                                rs.getString("sala"),
                                rs.getString("metodo_pago"),
                                rs.getDouble("total_bruto"),
                                rs.getInt("entradas_vendidas")
                        );

                        venta.setMontoReembolsado(
                                rs.getDouble("monto_reembolsado")
                        );
                        venta.setEntradasReembolsadas(
                                rs.getInt("entradas_reembolsadas")
                        );
                        venta.setAsientosReembolsados(
                                rs.getString("asientos_reembolsados")
                        );
                        venta.setEstadoReembolso(
                                rs.getString("estado_reembolso")
                        );

                        ventas.add(venta);
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo consultar ventas para el reporte.",
                    e
            );
        }

        return ventas;
    }

    private boolean columnaExiste(
            Connection con,
            String tabla,
            String columna
    ) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();

        try (ResultSet rs = meta.getColumns(
                con.getCatalog(),
                null,
                tabla,
                columna
        )) {
            if (rs.next()) {
                return true;
            }
        }

        try (ResultSet rs = meta.getColumns(
                con.getCatalog(),
                null,
                tabla.toLowerCase(),
                columna
        )) {
            return rs.next();
        }
    }
}
