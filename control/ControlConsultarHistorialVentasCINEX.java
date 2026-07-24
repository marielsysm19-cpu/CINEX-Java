package control;

import entidad.SalaCINEX;
import entidad.VentaCINEX;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Control del CU Visualizar historial de ventas.
 * La interfaz solo conversa con este control y recibe entidades VentaCINEX/SalaCINEX.
 */
public class ControlConsultarHistorialVentasCINEX {

    private ConfigBD configBD;

    private static class ConfigBD {
        String fechaCol;
        boolean tieneNumeroVenta;
        boolean unirUsuarios;
        String vendedorExpr;
    }

    public boolean existenVentasRegistradas() {
        String sql = "SELECT 1 FROM ventas LIMIT 1";
        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo verificar si existen ventas registradas.", e);
        }
    }

    public ArrayList<SalaCINEX> listarSalas() {
        ArrayList<SalaCINEX> salas = new ArrayList<>();
        String sql = "SELECT id_sala, nombre, capacidad, tipo FROM salas ORDER BY nombre";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                salas.add(new SalaCINEX(
                        rs.getInt("id_sala"),
                        rs.getString("nombre"),
                        rs.getString("tipo"),
                        rs.getInt("capacidad")
                ));
            }
        } catch (SQLException e) {
            System.out.println("[Historial ventas] No se pudieron listar salas: " + e.getMessage());
        }
        return salas;
    }

    public ArrayList<VentaCINEX> consultarVentas(LocalDate inicio, LocalDate fin, String sala, String metodo) {
        ArrayList<VentaCINEX> ventas = new ArrayList<>();
        if (inicio == null || fin == null || inicio.isAfter(fin)) {
            return ventas;
        }

        ControlModuloReembolsosCINEX.asegurarEstructura();

        try (Connection con = BDCINEX.conectar()) {
            ConfigBD cfg = obtenerConfigBD(con);
            String numeroExpr = cfg.tieneNumeroVenta
                    ? "IFNULL(MAX(v.numero_venta), CONCAT('VTA-', LPAD(v.id_venta, 6, '0')))"
                    : "CONCAT('VTA-', LPAD(v.id_venta, 6, '0'))";

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT v.id_venta, ");
            sql.append(numeroExpr).append(" AS numero_venta, ");
            sql.append("v.").append(cfg.fechaCol).append(" AS fecha_hora, ");
            sql.append("IFNULL(MIN(pel.titulo), '-') AS pelicula, ");
            sql.append("IFNULL(MIN(s.nombre), '-') AS sala, ");
            sql.append("IFNULL(GROUP_CONCAT(DISTINCT CONCAT(a.fila, a.numero) ORDER BY a.fila, a.numero SEPARATOR ', '), '-') AS asientos, ");
            sql.append("IFNULL(v.total, 0) AS total, ");
            sql.append("IFNULL(TRIM(pg.metodo_pago), '-') AS metodo_pago, ");
            sql.append(cfg.vendedorExpr).append(" AS vendedor, ");
            sql.append("COUNT(e.id_entrada) AS entradas_vendidas, ");
            sql.append("IFNULL(MAX(ref.entradas_reembolsadas), 0) AS entradas_reembolsadas, ");
            sql.append("IFNULL(MAX(ref.asientos_reembolsados), '-') AS asientos_reembolsados, ");
            sql.append("IFNULL(MAX(ref.monto_reembolsado), 0) AS monto_reembolsado, ");
            sql.append("CASE ");
            sql.append("WHEN IFNULL(MAX(ref.entradas_reembolsadas), 0) = 0 THEN 'Sin reembolso' ");
            sql.append("WHEN IFNULL(MAX(ref.entradas_reembolsadas), 0) >= COUNT(e.id_entrada) THEN 'Reembolso total' ");
            sql.append("ELSE 'Reembolso parcial' END AS estado_reembolso ");
            sql.append("FROM ventas v ");
            sql.append("LEFT JOIN pagos pg ON pg.id_venta = v.id_venta ");
            sql.append("LEFT JOIN entradas e ON e.id_venta = v.id_venta ");
            sql.append("LEFT JOIN asientos a ON a.id_asiento = e.id_asiento ");
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
            if (cfg.unirUsuarios) {
                sql.append("LEFT JOIN usuarios u ON u.id_usuario = v.id_usuario ");
            }
            sql.append("WHERE v.").append(cfg.fechaCol).append(" >= ? AND v.").append(cfg.fechaCol).append(" < ? ");

            ArrayList<Object> params = new ArrayList<>();
            params.add(Timestamp.valueOf(inicio.atStartOfDay()));
            params.add(Timestamp.valueOf(fin.plusDays(1).atStartOfDay()));

            if (sala != null && !sala.trim().isEmpty() && !"Todas".equalsIgnoreCase(sala.trim())) {
                sql.append("AND s.nombre = ? ");
                params.add(sala.trim());
            }

            String metodoBD = normalizarMetodoPago(metodo);
            if (!"Todos".equalsIgnoreCase(metodoBD)) {
                sql.append("AND pg.metodo_pago = ? ");
                params.add(metodoBD);
            }

            sql.append("GROUP BY v.id_venta, v.").append(cfg.fechaCol).append(", v.total, pg.metodo_pago ");
            sql.append("ORDER BY v.").append(cfg.fechaCol).append(" DESC, v.id_venta DESC");

            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Timestamp fechaHora = rs.getTimestamp("fecha_hora");
                        VentaCINEX venta = new VentaCINEX(
                                rs.getInt("id_venta"),
                                rs.getString("numero_venta"),
                                fechaHora == null ? null : fechaHora.toLocalDateTime(),
                                rs.getString("pelicula"),
                                rs.getString("sala"),
                                rs.getString("metodo_pago"),
                                rs.getDouble("total"),
                                rs.getInt("entradas_vendidas")
                        );
                        venta.setAsientos(rs.getString("asientos"));
                        venta.setVendedor(rs.getString("vendedor"));
                        venta.setEntradasReembolsadas(rs.getInt("entradas_reembolsadas"));
                        venta.setAsientosReembolsados(rs.getString("asientos_reembolsados"));
                        venta.setMontoReembolsado(rs.getDouble("monto_reembolsado"));
                        venta.setEstadoReembolso(rs.getString("estado_reembolso"));
                        ventas.add(venta);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar el historial de ventas.", e);
        }

        return ventas;
    }

    private synchronized ConfigBD obtenerConfigBD(Connection con) throws SQLException {
        if (configBD == null) {
            ConfigBD cfg = new ConfigBD();
            cfg.fechaCol = columnaExiste(con, "ventas", "fecha_venta") ? "fecha_venta" : "fecha_hora";
            cfg.tieneNumeroVenta = columnaExiste(con, "ventas", "numero_venta");
            cfg.unirUsuarios = columnaExiste(con, "ventas", "id_usuario") && tablaExiste(con, "usuarios");
            cfg.vendedorExpr = obtenerVendedorSelectExpr(con);
            configBD = cfg;
        }
        return configBD;
    }

    private String normalizarMetodoPago(String metodo) {
        if (metodo == null || metodo.trim().isEmpty() || "Todos".equalsIgnoreCase(metodo.trim())) return "Todos";
        String valor = metodo.trim();
        if (valor.equalsIgnoreCase("Tarjeta Debito")) return "Tarjeta Débito";
        if (valor.equalsIgnoreCase("Tarjeta Credito")) return "Tarjeta Crédito";
        if (valor.equalsIgnoreCase("Efectivo") || valor.equalsIgnoreCase("Tarjeta Débito")
                || valor.equalsIgnoreCase("Tarjeta Crédito") || valor.equalsIgnoreCase("Yape")
                || valor.equalsIgnoreCase("Plin")) return valor;
        return "Todos";
    }

    private String obtenerVendedorSelectExpr(Connection con) throws SQLException {
        if (columnaExiste(con, "ventas", "usuario")) return "IFNULL(MAX(v.usuario), '-')";
        if (columnaExiste(con, "ventas", "vendedor")) return "IFNULL(MAX(v.vendedor), '-')";
        if (columnaExiste(con, "ventas", "taquillero")) return "IFNULL(MAX(v.taquillero), '-')";
        if (columnaExiste(con, "ventas", "id_usuario") && tablaExiste(con, "usuarios")) {
            if (columnaExiste(con, "usuarios", "usuario")) return "IFNULL(MAX(u.usuario), '-')";
            if (columnaExiste(con, "usuarios", "nombre")) return "IFNULL(MAX(u.nombre), '-')";
        }
        return "'-'";
    }

    private boolean tablaExiste(Connection con, String tabla) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();
        String catalogo = con.getCatalog();
        try (ResultSet rs = meta.getTables(catalogo, null, tabla, null)) { if (rs.next()) return true; }
        try (ResultSet rs = meta.getTables(catalogo, null, tabla.toLowerCase(), null)) { if (rs.next()) return true; }
        try (ResultSet rs = meta.getTables(catalogo, null, tabla.toUpperCase(), null)) { return rs.next(); }
    }

    private boolean columnaExiste(Connection con, String tabla, String columna) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();
        String catalogo = con.getCatalog();
        try (ResultSet rs = meta.getColumns(catalogo, null, tabla, columna)) { if (rs.next()) return true; }
        try (ResultSet rs = meta.getColumns(catalogo, null, tabla.toLowerCase(), columna)) { if (rs.next()) return true; }
        try (ResultSet rs = meta.getColumns(catalogo, null, tabla.toUpperCase(), columna)) { return rs.next(); }
    }
}
