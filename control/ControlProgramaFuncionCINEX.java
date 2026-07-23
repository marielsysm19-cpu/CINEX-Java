package control;

import entidad.FuncionCINEX;
import entidad.HorarioCINEX;
import entidad.PeliculaCINEX;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * Control del CU-10: Gestionar programación de funciones.
 *
 * Flujo cubierto:
 *  5.1 verificarPelicula(idPelicula)
 *  5.2 validarBloqueHorario(fecha, horarioInicio)
 *  5.3 comprobarDisponibilidadSala(idSala, fecha, horarioInicio, duracion)
 *  5.4 informar cruce de horarios
 *  5.5 crearNuevaFuncion(...)
 */
public class ControlProgramaFuncionCINEX {

    private static final int DURACION_PREDETERMINADA_MIN = 120;
    public static final int TIEMPO_LIMPIEZA_MIN = 30;

    public static class SalaItem {
        private final int idSala;
        private final String nombre;
        private final String tipo;
        private final int capacidad;
        private final String estado;

        public SalaItem(int idSala, String nombre) {
            this(idSala, nombre, "", 0, "Activa");
        }

        public SalaItem(int idSala, String nombre, String tipo, int capacidad, String estado) {
            this.idSala = idSala;
            this.nombre = limpiar(nombre, "Sala");
            this.tipo = limpiar(tipo, "Estándar");
            this.capacidad = Math.max(0, capacidad);
            this.estado = limpiar(estado, "Activa");
        }

        public int getIdSala() {
            return idSala;
        }

        public String getNombre() {
            return nombre;
        }

        public String getTipo() {
            return tipo;
        }

        public int getCapacidad() {
            return capacidad;
        }

        public String getEstado() {
            return estado;
        }

        public String getDescripcion() {
            StringBuilder texto = new StringBuilder(nombre);
            if (!tipo.isEmpty()) {
                texto.append(" - ").append(tipo);
            }
            if (capacidad > 0) {
                texto.append(" (").append(capacidad).append(" asientos)");
            }
            return texto.toString();
        }

        @Override
        public String toString() {
            if (tipo == null || tipo.trim().isEmpty()) {
                return nombre;
            }

            return nombre + " - " + tipo;
        }
    }

    public enum TipoResultado {
        EXITO,
        DATOS_INCOMPLETOS,
        PELICULA_NO_EXISTE,
        FECHA_HORA_INVALIDA,
        HORARIO_DUPLICADO,
        FUNCION_REACTIVADA,
        ERROR
    }

    public static class ResultadoProgramacion {
        private final boolean exito;
        private final TipoResultado tipo;
        private final String mensaje;
        private final FuncionCINEX funcion;

        public ResultadoProgramacion(boolean exito, String mensaje, FuncionCINEX funcion) {
            this(exito, exito ? TipoResultado.EXITO : TipoResultado.ERROR, mensaje, funcion);
        }

        public ResultadoProgramacion(
                boolean exito,
                TipoResultado tipo,
                String mensaje,
                FuncionCINEX funcion
        ) {
            this.exito = exito;
            this.tipo = tipo == null ? TipoResultado.ERROR : tipo;
            this.mensaje = mensaje == null ? "" : mensaje.trim();
            this.funcion = funcion;
        }

        public boolean isExito() {
            return exito;
        }

        public TipoResultado getTipo() {
            return tipo;
        }

        public String getMensaje() {
            return mensaje;
        }

        public FuncionCINEX getFuncion() {
            return funcion;
        }
    }

    public ArrayList<PeliculaCINEX> listarPeliculasRegistradas() {
        ArrayList<PeliculaCINEX> lista = new ArrayList<>();

        String sql = "SELECT id_pelicula, titulo, genero, duracion, clasificacion, "
                + "imagen, estado FROM peliculas "
                + "WHERE estado IS NULL OR LOWER(estado) NOT IN ('inactiva', 'inactivo') "
                + "ORDER BY titulo ASC";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setQueryTimeout(12);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new PeliculaCINEX(
                            rs.getInt("id_pelicula"),
                            rs.getString("titulo"),
                            rs.getString("genero"),
                            rs.getInt("duracion"),
                            rs.getString("clasificacion"),
                            rs.getString("imagen"),
                            rs.getString("estado"),
                            1
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("No se pudieron cargar las películas registradas.", e);
        }

        return lista;
    }

    public ArrayList<SalaItem> listarSalasRegistradas() {
        ArrayList<SalaItem> lista = new ArrayList<>();

        String sql = "SELECT id_sala, nombre, tipo, capacidad, estado FROM salas "
                + "WHERE estado IS NULL OR LOWER(estado) NOT IN ('inactiva', 'inactivo') "
                + "ORDER BY nombre ASC";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setQueryTimeout(12);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new SalaItem(
                            rs.getInt("id_sala"),
                            rs.getString("nombre"),
                            rs.getString("tipo"),
                            rs.getInt("capacidad"),
                            rs.getString("estado")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("No se pudieron cargar las salas registradas.", e);
        }

        return lista;
    }

    /**
     * Valida el CU antes de mostrar la confirmación al administrador.
     * No inserta datos.
     */
    public ResultadoProgramacion validarProgramacion(
            int idPelicula,
            int idSala,
            LocalDate fecha,
            LocalTime horarioInicio
    ) {
        if (idPelicula <= 0 || idSala <= 0 || fecha == null || horarioInicio == null) {
            return resultadoError(
                    TipoResultado.DATOS_INCOMPLETOS,
                    "Complete la película, sala, fecha y horario."
            );
        }

        if (!validarBloqueHorario(fecha, horarioInicio)) {
            return resultadoError(
                    TipoResultado.FECHA_HORA_INVALIDA,
                    "La fecha y el horario deben ser actuales o futuros."
            );
        }

        try (Connection con = BDCINEX.conectar()) {
            if (!verificarPelicula(con, idPelicula)) {
                return resultadoError(
                        TipoResultado.PELICULA_NO_EXISTE,
                        "La película seleccionada no está registrada o se encuentra inactiva."
                );
            }

            int duracion = obtenerDuracionPelicula(con, idPelicula);

            if (!comprobarDisponibilidadSala(con, idSala, fecha, horarioInicio, duracion)) {
                return resultadoError(
                        TipoResultado.HORARIO_DUPLICADO,
                        "Cruce de horarios detectado: la sala está ocupada por otra función o por el periodo de limpieza. Cada programación reserva la duración completa de la película más 30 minutos para limpieza."
                );
            }

            int idFuncionCancelada =
                    buscarFuncionCanceladaExacta(
                            con,
                            idPelicula,
                            idSala,
                            fecha,
                            horarioInicio
                    );

            if (idFuncionCancelada > 0) {
                return new ResultadoProgramacion(
                        true,
                        TipoResultado.FUNCION_REACTIVADA,
                        "Existe una función cancelada con los mismos datos. "
                                + "Al confirmar se reactivará esa misma función, "
                                + "sin crear un registro duplicado.",
                        null
                );
            }

            return new ResultadoProgramacion(
                    true,
                    TipoResultado.EXITO,
                    "La información es válida. Puede confirmar el registro.",
                    null
            );

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo validar la programación de la función.", e);
        }
    }

    /**
     * 5. procesarProgramacion(...)
     * Vuelve a validar y registra la función en una transacción.
     */
    public ResultadoProgramacion procesarProgramacion(
            int idPelicula,
            int idSala,
            LocalDate fecha,
            LocalTime horarioInicio
    ) {
        if (idPelicula <= 0 || idSala <= 0 || fecha == null || horarioInicio == null) {
            return resultadoError(
                    TipoResultado.DATOS_INCOMPLETOS,
                    "Complete la película, sala, fecha y horario."
            );
        }

        if (!validarBloqueHorario(fecha, horarioInicio)) {
            return resultadoError(
                    TipoResultado.FECHA_HORA_INVALIDA,
                    "La fecha y el horario deben ser actuales o futuros."
            );
        }

        Connection con = null;

        try {
            con = BDCINEX.conectar();
            con.setAutoCommit(false);

            if (!verificarPelicula(con, idPelicula)) {
                con.rollback();
                return resultadoError(
                        TipoResultado.PELICULA_NO_EXISTE,
                        "La película seleccionada no está registrada o se encuentra inactiva."
                );
            }

            int duracion = obtenerDuracionPelicula(con, idPelicula);

            if (!comprobarDisponibilidadSala(con, idSala, fecha, horarioInicio, duracion)) {
                con.rollback();
                return resultadoError(
                        TipoResultado.HORARIO_DUPLICADO,
                        "Cruce de horarios detectado: la sala está ocupada por otra función o por el periodo de limpieza. Cada programación reserva la duración completa de la película más 30 minutos para limpieza."
                );
            }

            HorarioCINEX horario =
                    new HorarioCINEX(
                            fecha,
                            horarioInicio
                    );

            int idFuncionCancelada =
                    buscarFuncionCanceladaExacta(
                            con,
                            idPelicula,
                            idSala,
                            fecha,
                            horarioInicio
                    );

            FuncionCINEX funcion;
            TipoResultado tipoResultado;
            String mensaje;

            if (idFuncionCancelada > 0) {
                funcion = reactivarFuncionCancelada(
                        con,
                        idFuncionCancelada,
                        idPelicula,
                        idSala,
                        horario
                );

                tipoResultado =
                        TipoResultado.FUNCION_REACTIVADA;

                mensaje =
                        "La función cancelada fue reactivada correctamente.";
            } else {
                funcion = crearNuevaFuncion(
                        con,
                        idPelicula,
                        idSala,
                        horario
                );

                tipoResultado = TipoResultado.EXITO;
                mensaje = "Función programada correctamente.";
            }

            con.commit();

            return new ResultadoProgramacion(
                    true,
                    tipoResultado,
                    mensaje,
                    funcion
            );

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw new RuntimeException("No se pudo crear la nueva función.", e);

        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    // 5.1 verificarPelicula(idPelicula)
    public boolean verificarPelicula(int idPelicula) {
        try (Connection con = BDCINEX.conectar()) {
            return verificarPelicula(con, idPelicula);
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo verificar la película.", e);
        }
    }

    private boolean verificarPelicula(Connection con, int idPelicula) throws SQLException {
        String sql = "SELECT 1 FROM peliculas WHERE id_pelicula = ? "
                + "AND (estado IS NULL OR LOWER(estado) NOT IN ('inactiva', 'inactivo')) LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPelicula);
            ps.setQueryTimeout(10);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // 5.2 validarBloqueHorario(fecha, horarioInicio)
    public boolean validarBloqueHorario(LocalDate fecha, LocalTime horarioInicio) {
        if (fecha == null || horarioInicio == null) {
            return false;
        }

        LocalDateTime fechaHora = LocalDateTime.of(fecha, horarioInicio);
        return !fechaHora.isBefore(LocalDateTime.now().withSecond(0).withNano(0));
    }

    // 5.3 comprobarDisponibilidadSala(...)
    public boolean comprobarDisponibilidadSala(
            int idSala,
            LocalDate fecha,
            LocalTime horarioInicio,
            int duracionNuevaPelicula
    ) {
        try (Connection con = BDCINEX.conectar()) {
            return comprobarDisponibilidadSala(
                    con,
                    idSala,
                    fecha,
                    horarioInicio,
                    duracionNuevaPelicula
            );
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo comprobar la disponibilidad de la sala.", e);
        }
    }

    /**
     * Compatibilidad con el código anterior.
     */
    public boolean comprobarDisponibilidadSala(
            int idSala,
            LocalDate fecha,
            LocalTime horarioInicio
    ) {
        return comprobarDisponibilidadSala(
                idSala,
                fecha,
                horarioInicio,
                DURACION_PREDETERMINADA_MIN
        );
    }

    private boolean comprobarDisponibilidadSala(
            Connection con,
            int idSala,
            LocalDate fecha,
            LocalTime horarioInicio,
            int duracionNuevaPelicula
    ) throws SQLException {
        int duracionNueva = normalizarDuracion(
                duracionNuevaPelicula
        );

        LocalDateTime inicioNueva =
                LocalDateTime.of(fecha, horarioInicio);
        LocalDateTime finNueva =
                inicioNueva.plusMinutes(
                        duracionNueva
                                + TIEMPO_LIMPIEZA_MIN
                );

        /*
         * Se consultan el día anterior, el mismo día y el siguiente.
         * De esta manera también se detectan películas que comienzan
         * antes de medianoche y terminan al día siguiente.
         */
        String sql = "SELECT f.fecha, f.hora, p.duracion "
                + "FROM funciones f "
                + "INNER JOIN peliculas p "
                + "ON p.id_pelicula = f.id_pelicula "
                + "WHERE f.id_sala = ? "
                + "AND f.fecha BETWEEN ? AND ? "
                + "AND (f.estado IS NULL "
                + "OR LOWER(f.estado) NOT IN "
                + "('cancelada', 'inactiva', 'inactivo'))";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idSala);
            ps.setDate(
                    2,
                    Date.valueOf(fecha.minusDays(1))
            );
            ps.setDate(
                    3,
                    Date.valueOf(fecha.plusDays(1))
            );
            ps.setQueryTimeout(10);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date fechaBD = rs.getDate("fecha");
                    Time horaBD = rs.getTime("hora");

                    if (fechaBD == null || horaBD == null) {
                        continue;
                    }

                    LocalDateTime inicioExistente =
                            LocalDateTime.of(
                                    fechaBD.toLocalDate(),
                                    horaBD.toLocalTime()
                            );

                    int duracionExistente =
                            normalizarDuracion(
                                    rs.getInt("duracion")
                            );

                    LocalDateTime finExistente =
                            inicioExistente.plusMinutes(
                                    duracionExistente
                                            + TIEMPO_LIMPIEZA_MIN
                            );

                    /*
                     * Hay cruce cuando cada función comienza antes de
                     * que la otra termine.
                     *
                     * El final operativo incluye 30 minutos de limpieza.
                     * La siguiente función puede comenzar exactamente
                     * cuando termina ese periodo de limpieza.
                     */
                    boolean seCruzan =
                            inicioNueva.isBefore(finExistente)
                                    && inicioExistente.isBefore(
                                            finNueva
                                    );

                    if (seCruzan) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Busca una función cancelada con exactamente la misma
     * película, sala, fecha y hora.
     */
    private int buscarFuncionCanceladaExacta(
            Connection con,
            int idPelicula,
            int idSala,
            LocalDate fecha,
            LocalTime horarioInicio
    ) throws SQLException {
        String sql =
                "SELECT id_funcion "
                        + "FROM funciones "
                        + "WHERE id_pelicula = ? "
                        + "AND id_sala = ? "
                        + "AND fecha = ? "
                        + "AND hora = ? "
                        + "AND LOWER(TRIM(estado)) = 'cancelada' "
                        + "ORDER BY id_funcion DESC "
                        + "LIMIT 1 "
                        + "FOR UPDATE";

        try (PreparedStatement ps =
                     con.prepareStatement(sql)) {
            ps.setInt(1, idPelicula);
            ps.setInt(2, idSala);
            ps.setDate(3, Date.valueOf(fecha));
            ps.setTime(4, Time.valueOf(horarioInicio));
            ps.setQueryTimeout(10);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_funcion");
                }
            }
        }

        return 0;
    }

    /**
     * Reactiva la misma fila cancelada. No inserta otra función.
     */
    private FuncionCINEX reactivarFuncionCancelada(
            Connection con,
            int idFuncion,
            int idPelicula,
            int idSala,
            HorarioCINEX horario
    ) throws SQLException {
        String sql =
                "UPDATE funciones "
                        + "SET estado = 'Activa' "
                        + "WHERE id_funcion = ? "
                        + "AND LOWER(TRIM(estado)) = 'cancelada'";

        try (PreparedStatement ps =
                     con.prepareStatement(sql)) {
            ps.setInt(1, idFuncion);
            ps.setQueryTimeout(10);

            if (ps.executeUpdate() <= 0) {
                throw new SQLException(
                        "La función cancelada no pudo reactivarse."
                );
            }
        }

        FuncionCINEX funcion =
                new FuncionCINEX(
                        idFuncion,
                        idPelicula,
                        idSala,
                        horario.getFecha(),
                        horario.getHorarioInicio()
                );

        funcion.setEstado("Activa");
        return funcion;
    }

    // 5.5 crearNuevaFuncion(...)
    public FuncionCINEX crearNuevaFuncion(
            int idPelicula,
            int idSala,
            HorarioCINEX horario
    ) {
        try (Connection con = BDCINEX.conectar()) {
            return crearNuevaFuncion(con, idPelicula, idSala, horario);
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo crear la nueva función.", e);
        }
    }

    private FuncionCINEX crearNuevaFuncion(
            Connection con,
            int idPelicula,
            int idSala,
            HorarioCINEX horario
    ) throws SQLException {
        String sql = "INSERT INTO funciones "
                + "(id_pelicula, id_sala, fecha, hora, estado) "
                + "VALUES (?, ?, ?, ?, 'Activa')";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idPelicula);
            ps.setInt(2, idSala);
            ps.setDate(3, Date.valueOf(horario.getFecha()));
            ps.setTime(4, Time.valueOf(horario.getHorarioInicio()));
            ps.setQueryTimeout(12);

            int filas = ps.executeUpdate();
            if (filas <= 0) {
                throw new SQLException("La función no pudo ser registrada.");
            }

            int idFuncion = 0;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    idFuncion = keys.getInt(1);
                }
            }

            FuncionCINEX funcion = new FuncionCINEX(
                    idFuncion,
                    idPelicula,
                    idSala,
                    horario.getFecha(),
                    horario.getHorarioInicio()
            );
            funcion.setEstado("Activa");
            return funcion;
        }
    }

    public int obtenerDuracionPelicula(int idPelicula) {
        try (Connection con = BDCINEX.conectar()) {
            return obtenerDuracionPelicula(con, idPelicula);
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo obtener la duración de la película.", e);
        }
    }

    private int obtenerDuracionPelicula(Connection con, int idPelicula) throws SQLException {
        String sql = "SELECT duracion FROM peliculas WHERE id_pelicula = ? LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPelicula);
            ps.setQueryTimeout(10);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return normalizarDuracion(rs.getInt("duracion"));
                }
            }
        }

        return DURACION_PREDETERMINADA_MIN;
    }

    private ResultadoProgramacion resultadoError(TipoResultado tipo, String mensaje) {
        return new ResultadoProgramacion(false, tipo, mensaje, null);
    }

    private int normalizarDuracion(int duracion) {
        return duracion > 0 ? duracion : DURACION_PREDETERMINADA_MIN;
    }

    private static String limpiar(String texto, String defecto) {
        if (texto == null || texto.trim().isEmpty()) {
            return defecto == null ? "" : defecto;
        }
        return texto.trim();
    }
}
