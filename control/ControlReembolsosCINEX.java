package control;

import entidad.EntradaReembolsoCINEX;
import entidad.FuncionCINEX;
import entidad.ReembolsoCINEX;
import entidad.ResumenReembolsosCINEX;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ControlReembolsosCINEX {

    private final ControlNotificacionesCINEX controlNotificaciones =
            new ControlNotificacionesCINEX();

    public ControlReembolsosCINEX() {
        ControlModuloReembolsosCINEX.asegurarEstructura();
    }

    public ArrayList<FuncionCINEX> listarFuncionesConVentas(
            String filtro
    ) {
        ControlModuloReembolsosCINEX.asegurarEstructura();

        ArrayList<FuncionCINEX> lista = new ArrayList<>();
        String texto = filtro == null ? "" : filtro.trim();

        String sql =
                "SELECT f.id_funcion, p.titulo, "
                        + "DATE_FORMAT(f.fecha, '%d/%m/%Y') AS fecha, "
                        + "TIME_FORMAT(f.hora, '%h:%i %p') AS hora, "
                        + "s.nombre AS sala, f.estado, "
                        + "COUNT(e.id_entrada) AS entradas_historicas "
                        + "FROM funciones f "
                        + "INNER JOIN peliculas p "
                        + "ON p.id_pelicula = f.id_pelicula "
                        + "INNER JOIN salas s "
                        + "ON s.id_sala = f.id_sala "
                        + "INNER JOIN entradas e "
                        + "ON e.id_funcion = f.id_funcion "
                        + "WHERE (? = '' "
                        + "OR LOWER(p.titulo) LIKE LOWER(?) "
                        + "OR DATE_FORMAT(f.fecha, '%d/%m/%Y') LIKE ? "
                        + "OR TIME_FORMAT(f.hora, '%h:%i %p') LIKE ? "
                        + "OR LOWER(s.nombre) LIKE LOWER(?)) "
                        + "GROUP BY f.id_funcion, p.titulo, "
                        + "f.fecha, f.hora, s.nombre, f.estado "
                        + "ORDER BY f.fecha DESC, f.hora DESC, "
                        + "p.titulo ASC";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String like = "%" + texto + "%";
            ps.setString(1, texto);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idFuncion = rs.getInt("id_funcion");
                    ControlNotificacionesCINEX.EstadoAutorizacion autorizacion =
                            controlNotificaciones
                                    .obtenerAutorizacionFuncion(
                                            idFuncion
                                    );

                    FuncionCINEX funcion = new FuncionCINEX();
                    funcion.setIdFuncion(idFuncion);
                    funcion.setPelicula(rs.getString("titulo"));
                    funcion.setFechaTexto(rs.getString("fecha"));
                    funcion.setHoraBD(rs.getString("hora"));
                    funcion.setSala(rs.getString("sala"));
                    funcion.setEstado(rs.getString("estado"));
                    funcion.setVendidos(rs.getInt("entradas_historicas"));
                    funcion.setAutorizacionReembolso(autorizacion.getEstado());
                    lista.add(funcion);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron consultar las funciones con ventas.",
                    e
            );
        }

        return lista;
    }

    public ArrayList<EntradaReembolsoCINEX> buscarEntradasCliente(
            String documento,
            int idFuncion
    ) {
        ControlModuloReembolsosCINEX.asegurarEstructura();

        ArrayList<EntradaReembolsoCINEX> lista =
                new ArrayList<>();

        if (documento == null
                || documento.trim().isEmpty()
                || idFuncion <= 0) {
            return lista;
        }

        ControlNotificacionesCINEX.EstadoAutorizacion autorizacion =
                controlNotificaciones
                        .obtenerAutorizacionFuncion(idFuncion);

        /*
         * Solo son elegibles las entradas compradas antes de
         * la modificación que originó la autorización.
         */
        if (!autorizacion.estaPermitido()
                || autorizacion.getFechaCambio() == null) {
            return lista;
        }

        try (Connection con = BDCINEX.conectar()) {
            String fechaVenta =
                    columnaExiste(con, "ventas", "fecha_venta")
                            ? "fecha_venta"
                            : "fecha_hora";

            boolean tienePrecioUnitario =
                    columnaExiste(
                            con,
                            "entradas",
                            "precio_unitario"
                    );

            boolean tieneTipoEntrada =
                    columnaExiste(
                            con,
                            "entradas",
                            "tipo_entrada"
                    );

            String precioExpr = tienePrecioUnitario
                    ? "IFNULL(e.precio_unitario, "
                            + "v.total / NULLIF(("
                            + "SELECT COUNT(*) FROM entradas e2 "
                            + "WHERE e2.id_venta = v.id_venta"
                            + "), 0))"
                    : "v.total / NULLIF(("
                            + "SELECT COUNT(*) FROM entradas e2 "
                            + "WHERE e2.id_venta = v.id_venta"
                            + "), 0)";

            String tipoExpr = tieneTipoEntrada
                    ? "IFNULL(e.tipo_entrada, 'Entrada General')"
                    : "'Entrada General'";

            String sql =
                    "SELECT e.id_entrada, v.id_venta, "
                            + "IFNULL(v.numero_venta, "
                            + "CONCAT('VTA-', v.id_venta)) "
                            + "AS numero_venta, "
                            + "c.nombre AS cliente, c.dni, "
                            + "f.id_funcion, p.titulo, "
                            + "CONCAT(DATE_FORMAT(f.fecha, '%d/%m/%Y'), "
                            + "' - ', TIME_FORMAT(f.hora, '%h:%i %p')) "
                            + "AS funcion, "
                            + "s.nombre AS sala, "
                            + "CONCAT(a.fila, a.numero) AS asiento, "
                            + tipoExpr + " AS tipo_entrada, "
                            + "IFNULL(" + precioExpr + ", 0) "
                            + "AS precio_original, "
                            + "CASE "
                            + "WHEN dr.id_entrada IS NOT NULL "
                            + "THEN 'Reembolsada' "
                            + "ELSE IFNULL(e.estado, 'Emitida') "
                            + "END AS estado_entrada, "
                            + "IFNULL(pg.metodo_pago, '-') "
                            + "AS metodo_pago, "
                            + "IFNULL(v.total, 0) AS total_venta, "
                            + "DATE_FORMAT(v." + fechaVenta
                            + ", '%d/%m/%Y %h:%i %p') AS fecha_venta "
                            + "FROM entradas e "
                            + "INNER JOIN ventas v "
                            + "ON v.id_venta = e.id_venta "
                            + "INNER JOIN clientes c "
                            + "ON c.id_cliente = v.id_cliente "
                            + "INNER JOIN funciones f "
                            + "ON f.id_funcion = e.id_funcion "
                            + "INNER JOIN peliculas p "
                            + "ON p.id_pelicula = f.id_pelicula "
                            + "INNER JOIN salas s "
                            + "ON s.id_sala = f.id_sala "
                            + "INNER JOIN asientos a "
                            + "ON a.id_asiento = e.id_asiento "
                            + "LEFT JOIN detalle_reembolsos dr "
                            + "ON dr.id_entrada = e.id_entrada "
                            + "LEFT JOIN pagos pg "
                            + "ON pg.id_venta = v.id_venta "
                            + "WHERE c.dni = ? "
                            + "AND f.id_funcion = ? "
                            + "AND v." + fechaVenta + " < ? "
                            + "ORDER BY v.id_venta DESC, "
                            + "a.fila ASC, a.numero ASC";

            try (PreparedStatement ps =
                         con.prepareStatement(sql)) {
                ps.setString(1, documento.trim());
                ps.setInt(2, idFuncion);
                ps.setTimestamp(
                        3,
                        autorizacion.getFechaCambio()
                );

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        EntradaReembolsoCINEX item =
                                new EntradaReembolsoCINEX();

                        item.setIdEntrada(
                                rs.getInt("id_entrada")
                        );
                        item.setIdVenta(
                                rs.getInt("id_venta")
                        );
                        item.setNumeroVenta(
                                rs.getString("numero_venta")
                        );
                        item.setCliente(
                                rs.getString("cliente")
                        );
                        item.setDocumento(
                                rs.getString("dni")
                        );
                        item.setIdFuncion(
                                rs.getInt("id_funcion")
                        );
                        item.setPelicula(
                                rs.getString("titulo")
                        );
                        item.setFuncion(
                                rs.getString("funcion")
                        );
                        item.setSala(
                                rs.getString("sala")
                        );
                        item.setAsiento(
                                rs.getString("asiento")
                        );
                        item.setTipoEntrada(
                                rs.getString("tipo_entrada")
                        );
                        item.setPrecioOriginal(
                                rs.getDouble("precio_original")
                        );
                        item.setEstadoEntrada(
                                rs.getString("estado_entrada")
                        );
                        item.setMetodoPagoOriginal(
                                rs.getString("metodo_pago")
                        );
                        item.setTotalVenta(
                                rs.getDouble("total_venta")
                        );
                        item.setFechaVenta(
                                rs.getString("fecha_venta")
                        );

                        lista.add(item);
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron consultar las entradas del cliente.",
                    e
            );
        }

        return lista;
    }

    public ResultadoReembolso aplicarReembolso(
            String documento,
            int idFuncion,
            List<Integer> idsEntradas,
            String usuarioTaquillero
    ) {
        ControlModuloReembolsosCINEX.asegurarEstructura();

        if (documento == null
                || documento.trim().isEmpty()
                || idFuncion <= 0
                || idsEntradas == null
                || idsEntradas.isEmpty()) {
            return ResultadoReembolso.error(
                    "Seleccione el cliente y las entradas que desea reembolsar."
            );
        }

        ControlNotificacionesCINEX.EstadoAutorizacion autorizacion =
                controlNotificaciones
                        .obtenerAutorizacionFuncion(idFuncion);

        if (!autorizacion.estaPermitido()) {
            return ResultadoReembolso.error(
                    "El gerente no ha permitido el reembolso para esta función. "
                            + "Estado actual: "
                            + autorizacion.getEstado()
            );
        }

        if (autorizacion.getFechaCambio() == null) {
            return ResultadoReembolso.error(
                    "No se pudo determinar la fecha de la modificación "
                            + "que autorizó el reembolso."
            );
        }

        Connection con = null;

        try {
            con = BDCINEX.conectar();
            con.setAutoCommit(false);

            ArrayList<EntradaBloqueada> entradas =
                    bloquearEntradas(
                            con,
                            documento.trim(),
                            idFuncion,
                            idsEntradas,
                            autorizacion.getFechaCambio()
                    );

            if (entradas.size() != idsEntradas.size()) {
                con.rollback();
                return ResultadoReembolso.error(
                        "Una o más entradas no son reembolsables. "
                                + "Solo se aceptan entradas compradas antes "
                                + "de la modificación autorizada y que aún "
                                + "no hayan sido reembolsadas."
                );
            }

            int idVenta = entradas.get(0).idVenta;
            int idCliente = entradas.get(0).idCliente;

            for (EntradaBloqueada entrada : entradas) {
                if (entrada.idVenta != idVenta) {
                    con.rollback();
                    return ResultadoReembolso.error(
                            "Seleccione entradas pertenecientes "
                                    + "a una sola venta."
                    );
                }
            }

            double monto = 0.0;
            for (EntradaBloqueada entrada : entradas) {
                monto += entrada.precio;
            }

            int idReembolso = insertarReembolso(
                    con,
                    idVenta,
                    idCliente,
                    idFuncion,
                    autorizacion.getIdNotificacion(),
                    usuarioTaquillero,
                    monto
            );

            if (idReembolso <= 0) {
                throw new SQLException(
                        "No se pudo generar el identificador del reembolso."
                );
            }

            for (EntradaBloqueada entrada : entradas) {
                insertarDetalle(
                        con,
                        idReembolso,
                        entrada
                );
                marcarEntradaReembolsada(
                        con,
                        entrada.idEntrada
                );
            }

            int totalEntradasVenta =
                    contarEntradasVenta(con, idVenta);

            int totalReembolsadas =
                    contarEntradasReembolsadasVenta(
                            con,
                            idVenta
                    );

            String estado =
                    totalReembolsadas >= totalEntradasVenta
                            ? "Total"
                            : "Parcial";

            try (PreparedStatement ps =
                         con.prepareStatement(
                                 "UPDATE reembolsos "
                                         + "SET estado_reembolso = ?, "
                                         + "monto_total = ? "
                                         + "WHERE id_reembolso = ?"
                         )) {
                ps.setString(1, estado);
                ps.setDouble(2, monto);
                ps.setInt(3, idReembolso);
                ps.executeUpdate();
            }

            con.commit();

            return ResultadoReembolso.exito(
                    idReembolso,
                    monto,
                    estado,
                    entradas.size(),
                    "Reembolso "
                            + estado.toLowerCase()
                            + " registrado correctamente. "
                            + "Entregue S/ "
                            + String.format("%.2f", monto)
                            + " en efectivo."
            );

        } catch (Exception e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ignored) {
            }

            return ResultadoReembolso.error(
                    "No se pudo registrar el reembolso: "
                            + e.getMessage()
            );

        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    private ArrayList<EntradaBloqueada> bloquearEntradas(
            Connection con,
            String documento,
            int idFuncion,
            List<Integer> idsEntradas,
            Timestamp fechaCambio
    ) throws SQLException {
        ArrayList<EntradaBloqueada> lista =
                new ArrayList<>();

        String fechaVenta =
                columnaExiste(con, "ventas", "fecha_venta")
                        ? "fecha_venta"
                        : "fecha_hora";

        boolean tienePrecioUnitario =
                columnaExiste(
                        con,
                        "entradas",
                        "precio_unitario"
                );

        boolean tieneTipoEntrada =
                columnaExiste(
                        con,
                        "entradas",
                        "tipo_entrada"
                );

        String precioExpr = tienePrecioUnitario
                ? "IFNULL(e.precio_unitario, "
                        + "v.total / NULLIF(("
                        + "SELECT COUNT(*) FROM entradas e2 "
                        + "WHERE e2.id_venta = v.id_venta"
                        + "), 0))"
                : "v.total / NULLIF(("
                        + "SELECT COUNT(*) FROM entradas e2 "
                        + "WHERE e2.id_venta = v.id_venta"
                        + "), 0)";

        String tipoExpr = tieneTipoEntrada
                ? "IFNULL(e.tipo_entrada, 'Entrada General')"
                : "'Entrada General'";

        StringBuilder marcas = new StringBuilder();

        for (int i = 0; i < idsEntradas.size(); i++) {
            if (i > 0) {
                marcas.append(',');
            }
            marcas.append('?');
        }

        String sql =
                "SELECT e.id_entrada, e.id_venta, "
                        + "v.id_cliente, e.id_asiento, "
                        + "CONCAT(a.fila, a.numero) AS asiento, "
                        + tipoExpr + " AS tipo_entrada, "
                        + "IFNULL(" + precioExpr + ", 0) AS precio "
                        + "FROM entradas e "
                        + "INNER JOIN ventas v "
                        + "ON v.id_venta = e.id_venta "
                        + "INNER JOIN clientes c "
                        + "ON c.id_cliente = v.id_cliente "
                        + "INNER JOIN asientos a "
                        + "ON a.id_asiento = e.id_asiento "
                        + "LEFT JOIN detalle_reembolsos dr "
                        + "ON dr.id_entrada = e.id_entrada "
                        + "WHERE c.dni = ? "
                        + "AND e.id_funcion = ? "
                        + "AND v." + fechaVenta + " < ? "
                        + "AND e.id_entrada IN (" + marcas + ") "
                        + "AND dr.id_entrada IS NULL "
                        + "AND (e.estado IS NULL "
                        + "OR e.estado NOT IN "
                        + "('Reembolsada', 'Anulada')) "
                        + "FOR UPDATE";

        try (PreparedStatement ps =
                     con.prepareStatement(sql)) {
            int indice = 1;
            ps.setString(indice++, documento);
            ps.setInt(indice++, idFuncion);
            ps.setTimestamp(indice++, fechaCambio);

            for (Integer idEntrada : idsEntradas) {
                ps.setInt(
                        indice++,
                        idEntrada == null ? 0 : idEntrada
                );
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EntradaBloqueada entrada =
                            new EntradaBloqueada();

                    entrada.idEntrada =
                            rs.getInt("id_entrada");
                    entrada.idVenta =
                            rs.getInt("id_venta");
                    entrada.idCliente =
                            rs.getInt("id_cliente");
                    entrada.idAsiento =
                            rs.getInt("id_asiento");
                    entrada.asiento =
                            rs.getString("asiento");
                    entrada.tipoEntrada =
                            rs.getString("tipo_entrada");
                    entrada.precio =
                            rs.getDouble("precio");

                    lista.add(entrada);
                }
            }
        }

        return lista;
    }

    private int insertarReembolso(
            Connection con,
            int idVenta,
            int idCliente,
            int idFuncion,
            int idNotificacion,
            String usuarioTaquillero,
            double monto
    ) throws SQLException {
        String sql =
                "INSERT INTO reembolsos("
                        + "id_venta, id_cliente, id_funcion, "
                        + "id_notificacion, usuario_taquillero, "
                        + "metodo_reembolso, monto_total, "
                        + "estado_reembolso, motivo) "
                        + "VALUES (?, ?, ?, ?, ?, 'Efectivo', ?, "
                        + "'Parcial', ?)";

        try (PreparedStatement ps =
                     con.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS
                     )) {
            ps.setInt(1, idVenta);
            ps.setInt(2, idCliente);
            ps.setInt(3, idFuncion);

            if (idNotificacion > 0) {
                ps.setInt(4, idNotificacion);
            } else {
                ps.setNull(4, Types.INTEGER);
            }

            ps.setString(
                    5,
                    usuarioTaquillero == null
                            || usuarioTaquillero.trim().isEmpty()
                            ? "taquillero"
                            : usuarioTaquillero.trim()
            );
            ps.setDouble(6, monto);
            ps.setString(
                    7,
                    "Reembolso autorizado por el gerente "
                            + "debido a una modificación "
                            + "de película o función."
            );

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private void insertarDetalle(
            Connection con,
            int idReembolso,
            EntradaBloqueada entrada
    ) throws SQLException {
        String sql =
                "INSERT INTO detalle_reembolsos("
                        + "id_reembolso, id_entrada, id_asiento, "
                        + "asiento, tipo_entrada, precio_original, "
                        + "monto_reembolsado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps =
                     con.prepareStatement(sql)) {
            ps.setInt(1, idReembolso);
            ps.setInt(2, entrada.idEntrada);
            ps.setInt(3, entrada.idAsiento);
            ps.setString(4, entrada.asiento);
            ps.setString(5, entrada.tipoEntrada);
            ps.setDouble(6, entrada.precio);
            ps.setDouble(7, entrada.precio);
            ps.executeUpdate();
        }
    }

    private void marcarEntradaReembolsada(
            Connection con,
            int idEntrada
    ) throws SQLException {
        if (columnaExiste(con, "entradas", "estado")) {
            try (PreparedStatement ps =
                         con.prepareStatement(
                                 "UPDATE entradas "
                                         + "SET estado = 'Reembolsada' "
                                         + "WHERE id_entrada = ?"
                         )) {
                ps.setInt(1, idEntrada);
                ps.executeUpdate();
            }
        }
    }

    private int contarEntradasVenta(
            Connection con,
            int idVenta
    ) throws SQLException {
        try (PreparedStatement ps =
                     con.prepareStatement(
                             "SELECT COUNT(*) "
                                     + "FROM entradas "
                                     + "WHERE id_venta = ?"
                     )) {
            ps.setInt(1, idVenta);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int contarEntradasReembolsadasVenta(
            Connection con,
            int idVenta
    ) throws SQLException {
        try (PreparedStatement ps =
                     con.prepareStatement(
                             "SELECT COUNT(DISTINCT dr.id_entrada) "
                                     + "FROM detalle_reembolsos dr "
                                     + "INNER JOIN entradas e "
                                     + "ON e.id_entrada = dr.id_entrada "
                                     + "WHERE e.id_venta = ?"
                     )) {
            ps.setInt(1, idVenta);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public ResumenReembolsosCINEX obtenerResumenGeneral() {
        ControlModuloReembolsosCINEX.asegurarEstructura();

        ResumenReembolsosCINEX resumen =
                new ResumenReembolsosCINEX();

        try (Connection con = BDCINEX.conectar()) {
            consultarDecimal(
                    con,
                    "SELECT IFNULL(SUM(total), 0) FROM ventas",
                    resumen::setVentasBrutas
            );

            consultarDecimal(
                    con,
                    "SELECT IFNULL(SUM(monto_total), 0) "
                            + "FROM reembolsos",
                    resumen::setMontoReembolsado
            );

            consultarEntero(
                    con,
                    "SELECT COUNT(*) FROM entradas",
                    resumen::setEntradasVendidas
            );

            consultarEntero(
                    con,
                    "SELECT COUNT(DISTINCT id_entrada) "
                            + "FROM detalle_reembolsos",
                    resumen::setEntradasReembolsadas
            );

            consultarEntero(
                    con,
                    "SELECT COUNT(*) FROM reembolsos "
                            + "WHERE estado_reembolso = 'Parcial'",
                    resumen::setReembolsosParciales
            );

            consultarEntero(
                    con,
                    "SELECT COUNT(*) FROM reembolsos "
                            + "WHERE estado_reembolso = 'Total'",
                    resumen::setReembolsosTotales
            );

            consultarEntero(
                    con,
                    "SELECT COUNT(*) "
                            + "FROM notificaciones_cambios "
                            + "WHERE estado_reembolso = 'Pendiente'",
                    resumen::setNotificacionesPendientes
            );

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo generar el resumen de reembolsos.",
                    e
            );
        }

        return resumen;
    }

    public ArrayList<ReembolsoCINEX> listarUltimosReembolsos(
            int limite
    ) {
        ControlModuloReembolsosCINEX.asegurarEstructura();

        ArrayList<ReembolsoCINEX> lista = new ArrayList<>();

        String sql =
                "SELECT r.id_reembolso, "
                        + "IFNULL(v.numero_venta, "
                        + "CONCAT('VTA-', v.id_venta)) "
                        + "AS numero_venta, "
                        + "p.titulo, c.nombre, c.dni, "
                        + "GROUP_CONCAT(dr.asiento "
                        + "ORDER BY dr.asiento SEPARATOR ', ') "
                        + "AS asientos, "
                        + "COUNT(dr.id_detalle) AS entradas, "
                        + "r.monto_total, "
                        + "r.estado_reembolso, "
                        + "r.usuario_taquillero, "
                        + "DATE_FORMAT(r.fecha_reembolso, "
                        + "'%d/%m/%Y %h:%i %p') AS fecha "
                        + "FROM reembolsos r "
                        + "INNER JOIN ventas v "
                        + "ON v.id_venta = r.id_venta "
                        + "INNER JOIN clientes c "
                        + "ON c.id_cliente = r.id_cliente "
                        + "INNER JOIN funciones f "
                        + "ON f.id_funcion = r.id_funcion "
                        + "INNER JOIN peliculas p "
                        + "ON p.id_pelicula = f.id_pelicula "
                        + "LEFT JOIN detalle_reembolsos dr "
                        + "ON dr.id_reembolso = r.id_reembolso "
                        + "GROUP BY r.id_reembolso, v.numero_venta, "
                        + "v.id_venta, p.titulo, c.nombre, c.dni, "
                        + "r.monto_total, r.estado_reembolso, "
                        + "r.usuario_taquillero, r.fecha_reembolso "
                        + "ORDER BY r.fecha_reembolso DESC "
                        + "LIMIT ?";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limite));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReembolsoCINEX reembolso = new ReembolsoCINEX();
                    reembolso.setIdReembolso(rs.getInt("id_reembolso"));
                    reembolso.setNumeroVenta(rs.getString("numero_venta"));
                    reembolso.setPelicula(rs.getString("titulo"));
                    reembolso.setCliente(rs.getString("nombre"));
                    reembolso.setDocumento(rs.getString("dni"));
                    reembolso.setAsientos(rs.getString("asientos"));
                    reembolso.setEntradas(rs.getInt("entradas"));
                    reembolso.setMontoTotal(rs.getDouble("monto_total"));
                    reembolso.setEstado(rs.getString("estado_reembolso"));
                    reembolso.setUsuarioTaquillero(rs.getString("usuario_taquillero"));
                    reembolso.setFecha(rs.getString("fecha"));
                    lista.add(reembolso);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron consultar los reembolsos.",
                    e
            );
        }

        return lista;
    }

    private boolean columnaExiste(
            Connection con,
            String tabla,
            String columna
    ) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();
        String catalogo = con.getCatalog();

        try (ResultSet rs =
                     meta.getColumns(
                             catalogo,
                             null,
                             tabla,
                             columna
                     )) {
            if (rs.next()) {
                return true;
            }
        }

        try (ResultSet rs =
                     meta.getColumns(
                             catalogo,
                             null,
                             tabla.toLowerCase(),
                             columna
                     )) {
            return rs.next();
        }
    }

    private void consultarDecimal(
            Connection con,
            String sql,
            DoubleSetter setter
    ) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                setter.set(rs.getDouble(1));
            }
        }
    }

    private void consultarEntero(
            Connection con,
            String sql,
            IntSetter setter
    ) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                setter.set(rs.getInt(1));
            }
        }
    }

    private interface DoubleSetter {
        void set(double value);
    }

    private interface IntSetter {
        void set(int value);
    }

    private static class EntradaBloqueada {
        int idEntrada;
        int idVenta;
        int idCliente;
        int idAsiento;
        String asiento;
        String tipoEntrada;
        double precio;
    }

    public static class ResultadoReembolso {

        private final boolean exito;
        private final int idReembolso;
        private final double monto;
        private final String estado;
        private final int entradas;
        private final String mensaje;

        private ResultadoReembolso(
                boolean exito,
                int idReembolso,
                double monto,
                String estado,
                int entradas,
                String mensaje
        ) {
            this.exito = exito;
            this.idReembolso = idReembolso;
            this.monto = monto;
            this.estado = estado == null ? "" : estado;
            this.entradas = entradas;
            this.mensaje = mensaje == null ? "" : mensaje;
        }

        public static ResultadoReembolso exito(
                int idReembolso,
                double monto,
                String estado,
                int entradas,
                String mensaje
        ) {
            return new ResultadoReembolso(
                    true,
                    idReembolso,
                    monto,
                    estado,
                    entradas,
                    mensaje
            );
        }

        public static ResultadoReembolso error(
                String mensaje
        ) {
            return new ResultadoReembolso(
                    false,
                    0,
                    0.0,
                    "",
                    0,
                    mensaje
            );
        }

        public boolean isExito() { return exito; }
        public int getIdReembolso() { return idReembolso; }
        public double getMonto() { return monto; }
        public String getEstado() { return estado; }
        public int getEntradas() { return entradas; }
        public String getMensaje() { return mensaje; }
    }
}
