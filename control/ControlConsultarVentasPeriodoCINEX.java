package control;

import entidad.PeliculaCINEX;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;

public class ControlConsultarVentasPeriodoCINEX {

    public ArrayList<PeliculaCINEX> procesarVentasPeriodo(
            LocalDate inicio,
            LocalDate fin
    ) {
        if (!verificarPagosRegistrados(inicio, fin)) {
            return new ArrayList<>();
        }

        return contarEntradasVendidas(inicio, fin);
    }

    public ArrayList<PeliculaCINEX> contarEntradasVendidas(
            LocalDate inicio,
            LocalDate fin
    ) {
        ArrayList<PeliculaCINEX> lista = new ArrayList<>();

        if (inicio == null || fin == null || inicio.isAfter(fin)) {
            return lista;
        }

        ControlModuloReembolsosCINEX.asegurarEstructura();

        try (Connection con = BDCINEX.conectar()) {
            String fechaCol = columnaExiste(
                    con,
                    "ventas",
                    "fecha_venta"
            ) ? "fecha_venta" : "fecha_hora";

            boolean tienePrecioUnitario = columnaExiste(
                    con,
                    "entradas",
                    "precio_unitario"
            );

            String precioExpr = tienePrecioUnitario
                    ? "IFNULL(e.precio_unitario, 0)"
                    : "IFNULL(v.total / NULLIF((SELECT COUNT(*) FROM entradas e2 WHERE e2.id_venta = v.id_venta), 0), 0)";

            String sql =
                    "SELECT p.id_pelicula, p.titulo, "
                            + "SUM(CASE WHEN e.estado IS NULL "
                            + "OR e.estado NOT IN ('Anulada', 'Reembolsada') "
                            + "THEN 1 ELSE 0 END) AS entradas, "
                            + "IFNULL(SUM(CASE WHEN e.estado IS NULL "
                            + "OR e.estado NOT IN ('Anulada', 'Reembolsada') "
                            + "THEN " + precioExpr + " ELSE 0 END), 0) AS ingresos "
                            + "FROM entradas e "
                            + "INNER JOIN ventas v ON v.id_venta = e.id_venta "
                            + "INNER JOIN funciones f ON f.id_funcion = e.id_funcion "
                            + "INNER JOIN peliculas p ON p.id_pelicula = f.id_pelicula "
                            + "WHERE v." + fechaCol + " >= ? AND v." + fechaCol + " < ? "
                            + "GROUP BY p.id_pelicula, p.titulo";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setTimestamp(
                        1,
                        Timestamp.valueOf(inicio.atStartOfDay())
                );
                ps.setTimestamp(
                        2,
                        Timestamp.valueOf(fin.plusDays(1).atStartOfDay())
                );

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PeliculaCINEX pelicula = new PeliculaCINEX(
                                rs.getInt("id_pelicula"),
                                rs.getString("titulo")
                        );

                        pelicula.setEntradasVendidas(
                                rs.getInt("entradas")
                        );
                        pelicula.setIngresosGenerados(
                                rs.getDouble("ingresos")
                        );
                        lista.add(pelicula);
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron contar las entradas vigentes.",
                    e
            );
        }

        return lista;
    }

    public boolean verificarPagosRegistrados(
            LocalDate inicio,
            LocalDate fin
    ) {
        if (inicio == null || fin == null || inicio.isAfter(fin)) {
            return false;
        }

        try (Connection con = BDCINEX.conectar()) {
            String fechaCol = columnaExiste(
                    con,
                    "ventas",
                    "fecha_venta"
            ) ? "fecha_venta" : "fecha_hora";

            String sql =
                    "SELECT 1 FROM ventas v "
                            + "INNER JOIN pagos p ON p.id_venta = v.id_venta "
                            + "WHERE v." + fechaCol + " >= ? "
                            + "AND v." + fechaCol + " < ? LIMIT 1";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setTimestamp(
                        1,
                        Timestamp.valueOf(inicio.atStartOfDay())
                );
                ps.setTimestamp(
                        2,
                        Timestamp.valueOf(fin.plusDays(1).atStartOfDay())
                );

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron verificar pagos registrados.",
                    e
            );
        }
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
