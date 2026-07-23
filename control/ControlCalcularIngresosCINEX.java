package control;

import entidad.PeliculaCINEX;
import entidad.ReporteCINEX;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ControlCalcularIngresosCINEX {

    public ReporteCINEX calcularIngresosGenerados(
            PeliculaCINEX pelicula
    ) {
        ReporteCINEX reporte = new ReporteCINEX();
        reporte.setTipoReporte("Ingresos por película");

        if (pelicula == null || pelicula.getIdPelicula() <= 0) {
            reporte.setMensaje("No existen ingresos registrados.");
            return reporte;
        }

        ControlModuloReembolsosCINEX.asegurarEstructura();

        double ventasBrutas = sumarVentasBrutas(pelicula);
        double reembolsos = sumarReembolsos(pelicula);
        int entradasHistoricas = contarEntradasHistoricas(pelicula);
        int entradasReembolsadas = contarEntradasReembolsadas(pelicula);

        double ingresosNetos = Math.max(
                0.0,
                ventasBrutas - reembolsos
        );

        pelicula.setIngresosGenerados(ingresosNetos);
        pelicula.setEntradasVendidas(
                Math.max(
                        0,
                        entradasHistoricas - entradasReembolsadas
                )
        );

        reporte.setVentasBrutas(ventasBrutas);
        reporte.setMontoReembolsado(reembolsos);
        reporte.setTotalIngresos(ingresosNetos);
        reporte.setEntradasVendidas(entradasHistoricas);
        reporte.setEntradasReembolsadas(entradasReembolsadas);
        reporte.setTransacciones(entradasHistoricas);
        reporte.agregarGrupo(pelicula.getTitulo(), ingresosNetos);
        reporte.setMensaje(
                ventasBrutas > 0
                        ? "Se muestran ventas brutas, reembolsos e ingresos netos."
                        : "No existen ingresos registrados."
        );

        return enviarIngresosGenerados(reporte);
    }

    public double sumarPagos(PeliculaCINEX pelicula) {
        return Math.max(
                0.0,
                sumarVentasBrutas(pelicula) - sumarReembolsos(pelicula)
        );
    }

    private double sumarVentasBrutas(PeliculaCINEX pelicula) {
        String sql =
                "SELECT IFNULL(SUM(v.total), 0) AS ingresos "
                        + "FROM ventas v "
                        + "INNER JOIN funciones f "
                        + "ON f.id_funcion = v.id_funcion "
                        + "WHERE f.id_pelicula = ?";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, pelicula.getIdPelicula());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("ingresos") : 0.0;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron sumar las ventas de la película.",
                    e
            );
        }
    }

    private double sumarReembolsos(PeliculaCINEX pelicula) {
        String sql =
                "SELECT IFNULL(SUM(r.monto_total), 0) AS reembolsos "
                        + "FROM reembolsos r "
                        + "INNER JOIN funciones f "
                        + "ON f.id_funcion = r.id_funcion "
                        + "WHERE f.id_pelicula = ?";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, pelicula.getIdPelicula());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("reembolsos") : 0.0;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron sumar los reembolsos de la película.",
                    e
            );
        }
    }

    public int consultaVentasPorPelicula(PeliculaCINEX pelicula) {
        return Math.max(
                0,
                contarEntradasHistoricas(pelicula)
                        - contarEntradasReembolsadas(pelicula)
        );
    }

    private int contarEntradasHistoricas(PeliculaCINEX pelicula) {
        String sql =
                "SELECT COUNT(*) AS entradas "
                        + "FROM entradas e "
                        + "INNER JOIN funciones f "
                        + "ON f.id_funcion = e.id_funcion "
                        + "WHERE f.id_pelicula = ?";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, pelicula.getIdPelicula());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("entradas") : 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron consultar las entradas históricas.",
                    e
            );
        }
    }

    private int contarEntradasReembolsadas(PeliculaCINEX pelicula) {
        String sql =
                "SELECT COUNT(DISTINCT dr.id_entrada) AS entradas "
                        + "FROM detalle_reembolsos dr "
                        + "INNER JOIN entradas e "
                        + "ON e.id_entrada = dr.id_entrada "
                        + "INNER JOIN funciones f "
                        + "ON f.id_funcion = e.id_funcion "
                        + "WHERE f.id_pelicula = ?";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, pelicula.getIdPelicula());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("entradas") : 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron consultar las entradas reembolsadas.",
                    e
            );
        }
    }

    public ReporteCINEX enviarIngresosGenerados(
            ReporteCINEX reporte
    ) {
        return reporte;
    }
}
