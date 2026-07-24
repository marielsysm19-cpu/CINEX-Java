package control;

import entidad.FuncionCINEX;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

public class ControlGestionarFuncionesCINEX {

    public ArrayList<FuncionCINEX> listarFunciones() {
        ArrayList<FuncionCINEX> lista = new ArrayList<>();

        String sql = "SELECT f.id_funcion, f.id_pelicula, f.id_sala, p.titulo, "
                + "s.nombre AS sala, s.tipo AS tipo_sala, "
                + "f.fecha, f.hora, f.estado, p.duracion "
                + "FROM funciones f "
                + "INNER JOIN peliculas p ON f.id_pelicula = p.id_pelicula "
                + "INNER JOIN salas s ON f.id_sala = s.id_sala "
                + "ORDER BY f.fecha DESC, f.hora DESC";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                FuncionCINEX funcion = new FuncionCINEX();
                funcion.setIdFuncion(rs.getInt("id_funcion"));
                funcion.setIdPelicula(rs.getInt("id_pelicula"));
                funcion.setIdSala(rs.getInt("id_sala"));
                funcion.setPelicula(rs.getString("titulo"));
                funcion.setSala(rs.getString("sala"));
                funcion.setTipoSala(rs.getString("tipo_sala"));
                funcion.setFecha(rs.getDate("fecha") == null ? null : rs.getDate("fecha").toLocalDate());
                funcion.setHora(rs.getTime("hora") == null ? null : rs.getTime("hora").toLocalTime());
                funcion.setEstado(rs.getString("estado"));
                funcion.setDuracionMinutos(rs.getInt("duracion"));
                lista.add(funcion);
            }

        } catch (SQLException e) {
            System.out.println("[ControlGestionarFuncionesCINEX] Error al listar funciones: " + e.getMessage());
        }

        return lista;
    }

    public FuncionCINEX obtenerFuncion(int idFuncion) {
        String sql = "SELECT f.id_funcion, f.id_pelicula, f.id_sala, "
                + "f.fecha, f.hora, f.estado, p.titulo, "
                + "s.nombre, s.tipo "
                + "FROM funciones f "
                + "INNER JOIN peliculas p ON p.id_pelicula = f.id_pelicula "
                + "INNER JOIN salas s ON s.id_sala = f.id_sala "
                + "WHERE f.id_funcion = ?";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idFuncion);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                FuncionCINEX funcion = new FuncionCINEX();
                funcion.setIdFuncion(rs.getInt("id_funcion"));
                funcion.setIdPelicula(rs.getInt("id_pelicula"));
                funcion.setIdSala(rs.getInt("id_sala"));
                funcion.setFecha(rs.getDate("fecha") == null ? null : rs.getDate("fecha").toLocalDate());
                funcion.setHora(rs.getTime("hora") == null ? null : rs.getTime("hora").toLocalTime());
                funcion.setEstado(rs.getString("estado"));
                funcion.setPelicula(rs.getString("titulo"));
                funcion.setSala(rs.getString("nombre"));
                funcion.setTipoSala(rs.getString("tipo"));
                return funcion;
            }

        } catch (SQLException e) {
            System.out.println(
                    "[ControlGestionarFuncionesCINEX] Error al obtener función: "
                            + e.getMessage()
            );
            return null;
        }
    }

    public ResultadoModificacion modificarFuncion(
            int idFuncion,
            int idPelicula,
            int idSala,
            LocalDate fecha,
            LocalTime hora
    ) {
        if (idFuncion <= 0 || idPelicula <= 0 || idSala <= 0
                || fecha == null || hora == null) {
            return new ResultadoModificacion(
                    false,
                    "Complete todos los datos de la función.",
                    0
            );
        }

        LocalDateTime nuevoInicio = LocalDateTime.of(fecha, hora);
        if (nuevoInicio.isBefore(LocalDateTime.now())) {
            return new ResultadoModificacion(
                    false,
                    "La nueva fecha y hora no pueden estar en el pasado.",
                    0
            );
        }

        try (Connection con = BDCINEX.conectar()) {
            int duracionNueva = obtenerDuracionPelicula(con, idPelicula);
            if (duracionNueva <= 0) {
                duracionNueva = 120;
            }

            LocalDateTime nuevoFin = nuevoInicio
                    .plusMinutes(duracionNueva)
                    .plusMinutes(ControlProgramaFuncionCINEX.TIEMPO_LIMPIEZA_MIN);

            String sqlCruces = "SELECT f.id_funcion, f.hora, p.duracion "
                    + "FROM funciones f "
                    + "INNER JOIN peliculas p ON p.id_pelicula = f.id_pelicula "
                    + "WHERE f.id_sala = ? AND f.fecha = ? "
                    + "AND f.id_funcion <> ? "
                    + "AND (f.estado IS NULL OR LOWER(f.estado) <> 'cancelada')";

            try (PreparedStatement ps = con.prepareStatement(sqlCruces)) {
                ps.setInt(1, idSala);
                ps.setDate(2, Date.valueOf(fecha));
                ps.setInt(3, idFuncion);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalTime otraHora = rs.getTime("hora").toLocalTime();
                        int otraDuracion = Math.max(1, rs.getInt("duracion"));

                        LocalDateTime otroInicio = LocalDateTime.of(fecha, otraHora);
                        LocalDateTime otroFin = otroInicio
                                .plusMinutes(otraDuracion)
                                .plusMinutes(ControlProgramaFuncionCINEX.TIEMPO_LIMPIEZA_MIN);

                        boolean seCruzan = nuevoInicio.isBefore(otroFin)
                                && nuevoFin.isAfter(otroInicio);

                        if (seCruzan) {
                            return new ResultadoModificacion(
                                    false,
                                    "No se puede modificar la función porque el nuevo horario se cruza con otra programación de la misma sala.",
                                    0
                            );
                        }
                    }
                }
            }

            String sqlActualizar = "UPDATE funciones "
                    + "SET id_pelicula = ?, id_sala = ?, fecha = ?, hora = ? "
                    + "WHERE id_funcion = ? "
                    + "AND (estado IS NULL OR LOWER(estado) <> 'cancelada')";

            try (PreparedStatement ps = con.prepareStatement(sqlActualizar)) {
                ps.setInt(1, idPelicula);
                ps.setInt(2, idSala);
                ps.setDate(3, Date.valueOf(fecha));
                ps.setTime(4, Time.valueOf(hora));
                ps.setInt(5, idFuncion);

                if (ps.executeUpdate() <= 0) {
                    return new ResultadoModificacion(
                            false,
                            "La función no pudo modificarse o ya no está activa.",
                            0
                    );
                }
            }

            int entradasCompradas = contarEntradasCompradas(con, idFuncion);
            return new ResultadoModificacion(
                    true,
                    entradasCompradas > 0
                            ? "Función modificada. Se registró una solicitud para revisar el reembolso de las entradas compradas."
                            : "Función modificada correctamente.",
                    entradasCompradas
            );

        } catch (SQLException e) {
            System.out.println(
                    "[ControlGestionarFuncionesCINEX] Error al modificar función: "
                            + e.getMessage()
            );
            return new ResultadoModificacion(
                    false,
                    "Ocurrió un error al modificar la función.",
                    0
            );
        }
    }

    public int contarEntradasCompradas(int idFuncion) {
        try (Connection con = BDCINEX.conectar()) {
            return contarEntradasCompradas(con, idFuncion);
        } catch (SQLException e) {
            return 0;
        }
    }

    private int contarEntradasCompradas(Connection con, int idFuncion)
            throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM entradas "
                + "WHERE id_funcion = ? "
                + "AND (estado IS NULL OR estado NOT IN ('Anulada', 'Reembolsada'))";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idFuncion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        }
    }

    private int obtenerDuracionPelicula(Connection con, int idPelicula)
            throws SQLException {
        String sql = "SELECT duracion FROM peliculas WHERE id_pelicula = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPelicula);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("duracion") : 0;
            }
        }
    }

    public boolean cancelarFuncion(int idFuncion) {
        String sql = "UPDATE funciones SET estado = 'Cancelada' WHERE id_funcion = ?";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idFuncion);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println(
                    "[ControlGestionarFuncionesCINEX] Error al cancelar función: "
                            + e.getMessage()
            );
            return false;
        }
    }

    public boolean hayFuncionSeleccionada(int filaSeleccionada) {
        return filaSeleccionada >= 0;
    }

    public static class ResultadoModificacion {
        private final boolean exito;
        private final String mensaje;
        private final int entradasCompradas;

        public ResultadoModificacion(
                boolean exito,
                String mensaje,
                int entradasCompradas
        ) {
            this.exito = exito;
            this.mensaje = mensaje == null ? "" : mensaje;
            this.entradasCompradas = Math.max(0, entradasCompradas);
        }

        public boolean isExito() { return exito; }
        public String getMensaje() { return mensaje; }
        public int getEntradasCompradas() { return entradasCompradas; }
    }
}
