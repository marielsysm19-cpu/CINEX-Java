package control;

import entidad.NotificacionCambioCINEX;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class ControlNotificacionesCINEX {

    public static final String PENDIENTE = "Pendiente";
    public static final String PERMITIDO = "Permitido";
    public static final String NO_PERMITIDO = "No permitido";

    public ControlNotificacionesCINEX() {
        ControlModuloReembolsosCINEX.asegurarEstructura();
    }

    public boolean registrarCambioPelicula(
            int idPelicula,
            String titulo,
            String descripcion,
            String datosAnteriores,
            String datosNuevos,
            String usuarioAdmin
    ) {
        return registrar(
                "Película",
                idPelicula,
                0,
                titulo,
                descripcion,
                datosAnteriores,
                datosNuevos,
                usuarioAdmin
        );
    }

    public boolean registrarCambioFuncion(
            int idFuncion,
            String descripcion,
            String datosAnteriores,
            String datosNuevos,
            String usuarioAdmin
    ) {
        ControlModuloReembolsosCINEX.asegurarEstructura();

        String sqlDatos =
                "SELECT f.id_pelicula, "
                        + "CONCAT(p.titulo, ' - ', "
                        + "DATE_FORMAT(f.fecha, '%d/%m/%Y'), ' ', "
                        + "TIME_FORMAT(f.hora, '%h:%i %p'), ' - ', "
                        + "s.nombre) AS titulo "
                        + "FROM funciones f "
                        + "INNER JOIN peliculas p "
                        + "ON p.id_pelicula = f.id_pelicula "
                        + "INNER JOIN salas s "
                        + "ON s.id_sala = f.id_sala "
                        + "WHERE f.id_funcion = ?";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sqlDatos)) {

            ps.setInt(1, idFuncion);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }

                return registrar(
                        "Función",
                        rs.getInt("id_pelicula"),
                        idFuncion,
                        rs.getString("titulo"),
                        descripcion,
                        datosAnteriores,
                        datosNuevos,
                        usuarioAdmin
                );
            }

        } catch (SQLException e) {
            System.out.println(
                    "[Notificaciones] No se pudo registrar cambio de función: "
                            + e.getMessage()
            );
            return false;
        }
    }

    private boolean registrar(
            String tipo,
            int idPelicula,
            int idFuncion,
            String titulo,
            String descripcion,
            String datosAnteriores,
            String datosNuevos,
            String usuarioAdmin
    ) {
        ControlModuloReembolsosCINEX.asegurarEstructura();

        String sql =
                "INSERT INTO notificaciones_cambios("
                        + "tipo_elemento, id_pelicula, id_funcion, "
                        + "titulo_elemento, descripcion, "
                        + "datos_anteriores, datos_nuevos, usuario_admin, "
                        + "estado_reembolso) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Pendiente')";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, seguro(tipo));

            if (idPelicula > 0) {
                ps.setInt(2, idPelicula);
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }

            if (idFuncion > 0) {
                ps.setInt(3, idFuncion);
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }

            ps.setString(4, seguro(titulo));
            ps.setString(5, seguro(descripcion));
            ps.setString(6, seguro(datosAnteriores));
            ps.setString(7, seguro(datosNuevos));
            ps.setString(
                    8,
                    seguro(usuarioAdmin).isEmpty()
                            ? "admin"
                            : seguro(usuarioAdmin)
            );

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println(
                    "[Notificaciones] No se pudo guardar: "
                            + e.getMessage()
            );
            return false;
        }
    }

    /**
     * La bandeja del gerente muestra solamente decisiones pendientes.
     * Las notificaciones resueltas permanecen guardadas en la base de
     * datos para la trazabilidad, pero dejan de aparecer en esta lista.
     */
    public ArrayList<NotificacionCambioCINEX> listarNotificaciones(
            String filtroEstado
    ) {
        ControlModuloReembolsosCINEX.asegurarEstructura();

        ArrayList<NotificacionCambioCINEX> lista =
                new ArrayList<>();

        String sql =
                "SELECT id_notificacion, tipo_elemento, "
                        + "IFNULL(id_pelicula, 0) AS id_pelicula, "
                        + "IFNULL(id_funcion, 0) AS id_funcion, "
                        + "titulo_elemento, descripcion, "
                        + "IFNULL(datos_anteriores, '') AS datos_anteriores, "
                        + "IFNULL(datos_nuevos, '') AS datos_nuevos, "
                        + "usuario_admin, "
                        + "DATE_FORMAT(fecha_cambio, '%d/%m/%Y %h:%i %p') "
                        + "AS fecha_cambio, "
                        + "estado_reembolso, "
                        + "IFNULL(usuario_gerente, '') AS usuario_gerente, "
                        + "IFNULL(DATE_FORMAT(fecha_decision, "
                        + "'%d/%m/%Y %h:%i %p'), '') AS fecha_decision "
                        + "FROM notificaciones_cambios "
                        + "WHERE estado_reembolso = 'Pendiente' "
                        + "ORDER BY fecha_cambio DESC, "
                        + "id_notificacion DESC";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                NotificacionCambioCINEX item =
                        new NotificacionCambioCINEX();

                item.setIdNotificacion(
                        rs.getInt("id_notificacion")
                );
                item.setTipoElemento(
                        rs.getString("tipo_elemento")
                );
                item.setIdPelicula(
                        rs.getInt("id_pelicula")
                );
                item.setIdFuncion(
                        rs.getInt("id_funcion")
                );
                item.setTituloElemento(
                        rs.getString("titulo_elemento")
                );
                item.setDescripcion(
                        rs.getString("descripcion")
                );
                item.setDatosAnteriores(
                        rs.getString("datos_anteriores")
                );
                item.setDatosNuevos(
                        rs.getString("datos_nuevos")
                );
                item.setUsuarioAdmin(
                        rs.getString("usuario_admin")
                );
                item.setFechaCambio(
                        rs.getString("fecha_cambio")
                );
                item.setEstadoReembolso(
                        rs.getString("estado_reembolso")
                );
                item.setUsuarioGerente(
                        rs.getString("usuario_gerente")
                );
                item.setFechaDecision(
                        rs.getString("fecha_decision")
                );

                lista.add(item);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron consultar las notificaciones pendientes.",
                    e
            );
        }

        return lista;
    }

    public boolean resolverNotificacion(
            int idNotificacion,
            boolean permitir,
            String usuarioGerente
    ) {
        ControlModuloReembolsosCINEX.asegurarEstructura();

        String sql =
                "UPDATE notificaciones_cambios "
                        + "SET estado_reembolso = ?, "
                        + "usuario_gerente = ?, "
                        + "fecha_decision = NOW() "
                        + "WHERE id_notificacion = ?";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(
                    1,
                    permitir ? PERMITIDO : NO_PERMITIDO
            );
            ps.setString(
                    2,
                    seguro(usuarioGerente).isEmpty()
                            ? "gerente"
                            : seguro(usuarioGerente)
            );
            ps.setInt(3, idNotificacion);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo guardar la decisión del gerente.",
                    e
            );
        }
    }

    public EstadoAutorizacion obtenerAutorizacionFuncion(
            int idFuncion
    ) {
        ControlModuloReembolsosCINEX.asegurarEstructura();

        String sql =
                "SELECT n.id_notificacion, n.estado_reembolso, "
                        + "n.descripcion, n.titulo_elemento, "
                        + "n.fecha_cambio "
                        + "FROM funciones f "
                        + "INNER JOIN notificaciones_cambios n "
                        + "ON (n.id_funcion = f.id_funcion "
                        + "OR (n.id_funcion IS NULL "
                        + "AND n.id_pelicula = f.id_pelicula)) "
                        + "WHERE f.id_funcion = ? "
                        + "ORDER BY "
                        + "n.fecha_cambio DESC, "
                        + "n.id_notificacion DESC, "
                        + "CASE WHEN n.id_funcion = f.id_funcion "
                        + "THEN 0 ELSE 1 END "
                        + "LIMIT 1";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idFuncion);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new EstadoAutorizacion(
                            rs.getInt("id_notificacion"),
                            rs.getString("estado_reembolso"),
                            rs.getString("descripcion"),
                            rs.getString("titulo_elemento"),
                            rs.getTimestamp("fecha_cambio")
                    );
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo verificar la autorización de reembolso.",
                    e
            );
        }

        return new EstadoAutorizacion(
                0,
                "Sin solicitud",
                "No existe una modificación autorizada para reembolso.",
                "",
                null
        );
    }

    public int contarPendientes() {
        ControlModuloReembolsosCINEX.asegurarEstructura();

        String sql =
                "SELECT COUNT(*) "
                        + "FROM notificaciones_cambios "
                        + "WHERE estado_reembolso = 'Pendiente'";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            return 0;
        }
    }

    private static String seguro(String valor) {
        return valor == null ? "" : valor.trim();
    }

    public static class EstadoAutorizacion {

        private final int idNotificacion;
        private final String estado;
        private final String descripcion;
        private final String elemento;
        private final Timestamp fechaCambio;

        public EstadoAutorizacion(
                int idNotificacion,
                String estado,
                String descripcion,
                String elemento,
                Timestamp fechaCambio
        ) {
            this.idNotificacion = idNotificacion;
            this.estado = seguro(estado);
            this.descripcion = seguro(descripcion);
            this.elemento = seguro(elemento);
            this.fechaCambio = fechaCambio;
        }

        public int getIdNotificacion() {
            return idNotificacion;
        }

        public String getEstado() {
            return estado;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public String getElemento() {
            return elemento;
        }

        public Timestamp getFechaCambio() {
            return fechaCambio;
        }

        public boolean estaPermitido() {
            return PERMITIDO.equalsIgnoreCase(estado);
        }
    }

}
