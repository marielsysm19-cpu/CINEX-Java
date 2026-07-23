package control;

import entidad.PeliculaCINEX;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.Time;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Control único para gestionar películas en CINEX.
 *
 * Responsabilidades:
 * - Registrar películas.
 * - Listar películas registradas.
 * - Modificar películas existentes sin cambiar su ID.
 * - Eliminar películas que no tengan funciones asociadas.
 */
public class ControlGestionarPeliculasCINEX {

    private static final int DURACION_PREDETERMINADA_MIN = 120;
    private static final int TIEMPO_LIMPIEZA_MIN =
            ControlProgramaFuncionCINEX.TIEMPO_LIMPIEZA_MIN;

    public static final String MENSAJE_DATOS_INCOMPLETOS =
            "Complete la información obligatoria de la película";

    public static final String MENSAJE_PELICULA_EXISTE =
            "La película ya existe en el sistema";

    public static final String MENSAJE_REGISTRADA =
            "Película registrada correctamente";

    public static final String MENSAJE_MODIFICADA =
            "Película modificada correctamente";

    public static final String MENSAJE_ELIMINADA =
            "Película eliminada correctamente";

    public static final String MENSAJE_CON_FUNCIONES =
            "No se puede eliminar la película porque tiene funciones asociadas";

    // ========================================================
    // RESULTADOS DE REGISTRO
    // ========================================================
    public enum ResultadoRegistro {
        REGISTRADA,
        DATOS_INCOMPLETOS,
        PELICULA_YA_REGISTRADA,
        ERROR
    }

    public static class RespuestaRegistro {
        private final ResultadoRegistro resultado;
        private final String mensaje;

        public RespuestaRegistro(
                ResultadoRegistro resultado,
                String mensaje
        ) {
            this.resultado = resultado;
            this.mensaje = mensaje;
        }

        public ResultadoRegistro getResultado() {
            return resultado;
        }

        public String getMensaje() {
            return mensaje;
        }

        public boolean fueExitoso() {
            return resultado == ResultadoRegistro.REGISTRADA;
        }
    }

    // ========================================================
    // RESULTADOS DE MODIFICACIÓN
    // ========================================================
    public enum ResultadoModificacion {
        MODIFICADA,
        DATOS_INCOMPLETOS,
        PELICULA_YA_REGISTRADA,
        CRUCE_HORARIOS,
        NO_ENCONTRADA,
        ERROR
    }

    public static class RespuestaModificacion {
        private final ResultadoModificacion resultado;
        private final String mensaje;

        public RespuestaModificacion(
                ResultadoModificacion resultado,
                String mensaje
        ) {
            this.resultado = resultado;
            this.mensaje = mensaje;
        }

        public ResultadoModificacion getResultado() {
            return resultado;
        }

        public String getMensaje() {
            return mensaje;
        }

        public boolean fueExitosa() {
            return resultado == ResultadoModificacion.MODIFICADA;
        }
    }

    // ========================================================
    // RESULTADOS DE ELIMINACIÓN
    // ========================================================
    public enum ResultadoEliminacion {
        ELIMINADA,
        NO_ENCONTRADA,
        FUNCIONES_ASOCIADAS,
        ERROR
    }

    public static class RespuestaEliminacion {
        private final ResultadoEliminacion resultado;
        private final String mensaje;

        public RespuestaEliminacion(
                ResultadoEliminacion resultado,
                String mensaje
        ) {
            this.resultado = resultado;
            this.mensaje = mensaje;
        }

        public ResultadoEliminacion getResultado() {
            return resultado;
        }

        public String getMensaje() {
            return mensaje;
        }

        public boolean fueExitosa() {
            return resultado == ResultadoEliminacion.ELIMINADA;
        }
    }

    // ========================================================
    // LISTADO
    // ========================================================
    public ArrayList<PeliculaCINEX> listarPeliculasRegistradas() {
        ArrayList<PeliculaCINEX> peliculas = new ArrayList<>();

        try (Connection con = BDCINEX.conectar()) {
            Set<String> columnas = obtenerColumnas(con, "peliculas");

            if (!columnas.contains("id_pelicula")
                    || !columnas.contains("titulo")) {
                throw new SQLException(
                        "La tabla peliculas no tiene las columnas requeridas."
                );
            }

            String genero = columnas.contains("genero")
                    ? "genero"
                    : "'' AS genero";

            String duracion = columnas.contains("duracion")
                    ? "duracion"
                    : "0 AS duracion";

            String clasificacion = columnas.contains("clasificacion")
                    ? "clasificacion"
                    : "'' AS clasificacion";

            String imagen = columnas.contains("imagen")
                    ? "imagen"
                    : "'' AS imagen";

            String estado = columnas.contains("estado")
                    ? "estado"
                    : "'Activa' AS estado";

            String cartelera;

            if (columnas.contains("cartelera")) {
                cartelera = "cartelera";
            } else if (columnas.contains("en_cartelera")) {
                cartelera = "en_cartelera AS cartelera";
            } else {
                cartelera = "1 AS cartelera";
            }

            String sql = "SELECT id_pelicula, titulo, "
                    + genero + ", "
                    + duracion + ", "
                    + clasificacion + ", "
                    + imagen + ", "
                    + estado + ", "
                    + cartelera
                    + " FROM peliculas ORDER BY titulo";

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    peliculas.add(new PeliculaCINEX(
                            rs.getInt("id_pelicula"),
                            textoSeguro(rs.getString("titulo")),
                            textoSeguro(rs.getString("genero")),
                            rs.getInt("duracion"),
                            textoSeguro(rs.getString("clasificacion")),
                            textoSeguro(rs.getString("imagen")),
                            textoSeguro(rs.getString("estado")),
                            rs.getInt("cartelera")
                    ));
                }
            }

            return peliculas;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron consultar las películas registradas.",
                    e
            );
        }
    }

    // ========================================================
    // REGISTRO
    // ========================================================
    public RespuestaRegistro gestionarRegistroPelicula(
            PeliculaCINEX pelicula
    ) {
        if (!validarInformacionIngresada(pelicula)) {
            return new RespuestaRegistro(
                    ResultadoRegistro.DATOS_INCOMPLETOS,
                    MENSAJE_DATOS_INCOMPLETOS
            );
        }

        if (peliculaYaRegistrada(pelicula.getTitulo())) {
            return new RespuestaRegistro(
                    ResultadoRegistro.PELICULA_YA_REGISTRADA,
                    MENSAJE_PELICULA_EXISTE
            );
        }

        try {
            boolean registrada = registrarPelicula(pelicula);

            if (registrada) {
                return new RespuestaRegistro(
                        ResultadoRegistro.REGISTRADA,
                        MENSAJE_REGISTRADA
                );
            }

            return new RespuestaRegistro(
                    ResultadoRegistro.ERROR,
                    "No se pudo registrar la película"
            );

        } catch (RuntimeException e) {
            return new RespuestaRegistro(
                    ResultadoRegistro.ERROR,
                    mensajeSeguro(
                            e,
                            "No se pudo registrar la película"
                    )
            );
        }
    }

    public boolean validarInformacionIngresada(
            PeliculaCINEX pelicula
    ) {
        return pelicula != null
                && pelicula.datosObligatoriosCompletos();
    }

    public boolean peliculaYaRegistrada(String titulo) {
        if (titulo == null || titulo.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM peliculas "
                + "WHERE LOWER(TRIM(titulo)) = LOWER(TRIM(?))";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, titulo.trim());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo verificar si la película ya existe.",
                    e
            );
        }
    }

    private boolean registrarPelicula(
            PeliculaCINEX pelicula
    ) {
        try (Connection con = BDCINEX.conectar()) {
            Set<String> columnas = obtenerColumnas(con, "peliculas");

            if (columnas.isEmpty()) {
                throw new SQLException(
                        "No se encontró la tabla peliculas."
                );
            }

            ArrayList<String> nombresColumnas = new ArrayList<>();
            ArrayList<Object> valores = new ArrayList<>();

            agregarInsercionSiExiste(
                    columnas,
                    nombresColumnas,
                    valores,
                    "titulo",
                    pelicula.getTitulo()
            );

            agregarInsercionSiExiste(
                    columnas,
                    nombresColumnas,
                    valores,
                    "genero",
                    pelicula.getGenero()
            );

            agregarInsercionSiExiste(
                    columnas,
                    nombresColumnas,
                    valores,
                    "duracion",
                    pelicula.getDuracion()
            );

            agregarInsercionSiExiste(
                    columnas,
                    nombresColumnas,
                    valores,
                    "clasificacion",
                    pelicula.getClasificacion()
            );

            agregarInsercionSiExiste(
                    columnas,
                    nombresColumnas,
                    valores,
                    "imagen",
                    pelicula.getImagen()
            );

            agregarInsercionSiExiste(
                    columnas,
                    nombresColumnas,
                    valores,
                    "estado",
                    pelicula.getEstado()
            );

            if (columnas.contains("cartelera")) {
                agregarInsercionSiExiste(
                        columnas,
                        nombresColumnas,
                        valores,
                        "cartelera",
                        pelicula.getCartelera()
                );
            } else if (columnas.contains("en_cartelera")) {
                agregarInsercionSiExiste(
                        columnas,
                        nombresColumnas,
                        valores,
                        "en_cartelera",
                        pelicula.getCartelera()
                );
            }

            if (nombresColumnas.isEmpty()) {
                throw new SQLException(
                        "La tabla peliculas no tiene columnas compatibles."
                );
            }

            StringBuilder sql = new StringBuilder(
                    "INSERT INTO peliculas ("
            );

            for (int i = 0; i < nombresColumnas.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }

                sql.append(nombresColumnas.get(i));
            }

            sql.append(") VALUES (");

            for (int i = 0; i < nombresColumnas.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }

                sql.append("?");
            }

            sql.append(")");

            try (PreparedStatement ps =
                         con.prepareStatement(sql.toString())) {

                for (int i = 0; i < valores.size(); i++) {
                    ps.setObject(i + 1, valores.get(i));
                }

                return ps.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo registrar la película.",
                    e
            );
        }
    }

    // ========================================================
    // MODIFICACIÓN
    // ========================================================
    public RespuestaModificacion modificarPelicula(
            PeliculaCINEX pelicula
    ) {
        if (pelicula == null || pelicula.getIdPelicula() <= 0) {
            return new RespuestaModificacion(
                    ResultadoModificacion.NO_ENCONTRADA,
                    "Seleccione una película válida"
            );
        }

        if (!validarInformacionIngresada(pelicula)) {
            return new RespuestaModificacion(
                    ResultadoModificacion.DATOS_INCOMPLETOS,
                    MENSAJE_DATOS_INCOMPLETOS
            );
        }

        if (peliculaYaRegistradaExceptoId(
                pelicula.getTitulo(),
                pelicula.getIdPelicula()
        )) {
            return new RespuestaModificacion(
                    ResultadoModificacion.PELICULA_YA_REGISTRADA,
                    MENSAJE_PELICULA_EXISTE
            );
        }

        try (Connection con = BDCINEX.conectar()) {
            if (!peliculaExiste(con, pelicula.getIdPelicula())) {
                return new RespuestaModificacion(
                        ResultadoModificacion.NO_ENCONTRADA,
                        "La película seleccionada ya no existe"
                );
            }

            int duracionActual = obtenerDuracionActual(
                    con,
                    pelicula.getIdPelicula()
            );

            if (normalizarDuracion(pelicula.getDuracion())
                    != normalizarDuracion(duracionActual)) {

                String conflicto = detectarCrucePorCambioDuracion(
                        con,
                        pelicula.getIdPelicula(),
                        pelicula.getDuracion()
                );

                if (conflicto != null) {
                    return new RespuestaModificacion(
                            ResultadoModificacion.CRUCE_HORARIOS,
                            conflicto
                    );
                }
            }

            Set<String> columnas = obtenerColumnas(con, "peliculas");
            ArrayList<String> asignaciones = new ArrayList<>();
            ArrayList<Object> valores = new ArrayList<>();

            agregarActualizacionSiExiste(
                    columnas,
                    asignaciones,
                    valores,
                    "titulo",
                    pelicula.getTitulo()
            );

            agregarActualizacionSiExiste(
                    columnas,
                    asignaciones,
                    valores,
                    "genero",
                    pelicula.getGenero()
            );

            agregarActualizacionSiExiste(
                    columnas,
                    asignaciones,
                    valores,
                    "duracion",
                    pelicula.getDuracion()
            );

            agregarActualizacionSiExiste(
                    columnas,
                    asignaciones,
                    valores,
                    "clasificacion",
                    pelicula.getClasificacion()
            );

            agregarActualizacionSiExiste(
                    columnas,
                    asignaciones,
                    valores,
                    "imagen",
                    pelicula.getImagen()
            );

            agregarActualizacionSiExiste(
                    columnas,
                    asignaciones,
                    valores,
                    "estado",
                    pelicula.getEstado()
            );

            if (columnas.contains("cartelera")) {
                agregarActualizacionSiExiste(
                        columnas,
                        asignaciones,
                        valores,
                        "cartelera",
                        pelicula.getCartelera()
                );
            } else if (columnas.contains("en_cartelera")) {
                agregarActualizacionSiExiste(
                        columnas,
                        asignaciones,
                        valores,
                        "en_cartelera",
                        pelicula.getCartelera()
                );
            }

            if (asignaciones.isEmpty()) {
                return new RespuestaModificacion(
                        ResultadoModificacion.ERROR,
                        "No existen columnas compatibles para modificar"
                );
            }

            String sql = "UPDATE peliculas SET "
                    + String.join(", ", asignaciones)
                    + " WHERE id_pelicula = ?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (int i = 0; i < valores.size(); i++) {
                    ps.setObject(i + 1, valores.get(i));
                }

                ps.setInt(
                        valores.size() + 1,
                        pelicula.getIdPelicula()
                );

                int filas = ps.executeUpdate();

                if (filas > 0) {
                    return new RespuestaModificacion(
                            ResultadoModificacion.MODIFICADA,
                            MENSAJE_MODIFICADA
                    );
                }

                return new RespuestaModificacion(
                        ResultadoModificacion.NO_ENCONTRADA,
                        "La película seleccionada ya no existe"
                );
            }

        } catch (SQLException e) {
            return new RespuestaModificacion(
                    ResultadoModificacion.ERROR,
                    "No se pudo modificar la película"
            );
        }
    }

    /**
     * Comprueba si la nueva duración de una película altera las
     * programaciones existentes. Cada función ocupa la sala durante
     * la película y 30 minutos adicionales para limpieza.
     */
    private String detectarCrucePorCambioDuracion(
            Connection con,
            int idPelicula,
            int nuevaDuracion
    ) throws SQLException {
        ArrayList<FuncionProgramada> funciones =
                listarFuncionesNoCanceladas(con);

        int duracionPropuesta =
                normalizarDuracion(nuevaDuracion);

        LocalDateTime ahora = LocalDateTime.now();

        for (FuncionProgramada objetivo : funciones) {
            if (objetivo.idPelicula != idPelicula) {
                continue;
            }

            LocalDateTime finOperativoObjetivo =
                    objetivo.inicio.plusMinutes(
                            duracionPropuesta
                                    + TIEMPO_LIMPIEZA_MIN
                    );

            // Las funciones completamente pasadas son históricas y
            // no impiden modificar la duración actual de la película.
            if (!finOperativoObjetivo.isAfter(ahora)) {
                continue;
            }

            for (FuncionProgramada otra : funciones) {
                if (otra.idFuncion == objetivo.idFuncion
                        || otra.idSala != objetivo.idSala) {
                    continue;
                }

                int duracionOtra =
                        otra.idPelicula == idPelicula
                                ? duracionPropuesta
                                : normalizarDuracion(
                                        otra.duracion
                                );

                LocalDateTime finOperativoOtra =
                        otra.inicio.plusMinutes(
                                duracionOtra
                                        + TIEMPO_LIMPIEZA_MIN
                        );

                boolean seCruzan =
                        objetivo.inicio.isBefore(
                                finOperativoOtra
                        )
                                && otra.inicio.isBefore(
                                        finOperativoObjetivo
                                );

                if (seCruzan) {
                    return crearMensajeCruceDuracion(
                            objetivo,
                            otra,
                            duracionPropuesta,
                            finOperativoObjetivo
                    );
                }
            }
        }

        return null;
    }

    private ArrayList<FuncionProgramada>
    listarFuncionesNoCanceladas(
            Connection con
    ) throws SQLException {
        ArrayList<FuncionProgramada> funciones =
                new ArrayList<>();

        String sql =
                "SELECT f.id_funcion, f.id_pelicula, "
                        + "f.id_sala, f.fecha, f.hora, "
                        + "p.titulo, p.duracion, "
                        + "COALESCE(NULLIF(TRIM(s.nombre), ''), "
                        + "CONCAT('Sala ', f.id_sala)) "
                        + "AS sala_nombre "
                        + "FROM funciones f "
                        + "INNER JOIN peliculas p "
                        + "ON p.id_pelicula = f.id_pelicula "
                        + "LEFT JOIN salas s "
                        + "ON s.id_sala = f.id_sala "
                        + "WHERE f.estado IS NULL "
                        + "OR LOWER(TRIM(f.estado)) NOT IN "
                        + "('cancelada', 'inactiva', 'inactivo') "
                        + "ORDER BY f.fecha, f.hora, f.id_funcion";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setQueryTimeout(12);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date fecha = rs.getDate("fecha");
                    Time hora = rs.getTime("hora");

                    if (fecha == null || hora == null) {
                        continue;
                    }

                    funciones.add(
                            new FuncionProgramada(
                                    rs.getInt("id_funcion"),
                                    rs.getInt("id_pelicula"),
                                    rs.getInt("id_sala"),
                                    textoSeguro(
                                            rs.getString("titulo")
                                    ),
                                    textoSeguro(
                                            rs.getString("sala_nombre")
                                    ),
                                    LocalDateTime.of(
                                            fecha.toLocalDate(),
                                            hora.toLocalTime()
                                    ),
                                    rs.getInt("duracion")
                            )
                    );
                }
            }
        }

        return funciones;
    }

    private String crearMensajeCruceDuracion(
            FuncionProgramada objetivo,
            FuncionProgramada otra,
            int nuevaDuracion,
            LocalDateTime finOperativoObjetivo
    ) {
        DateTimeFormatter fechaFormato =
                DateTimeFormatter.ofPattern("dd/MM/yyyy");

        DateTimeFormatter horaFormato =
                DateTimeFormatter.ofPattern("hh:mm a");

        return "No se puede cambiar la duración a "
                + nuevaDuracion
                + " minutos porque produciría un cruce de horarios. "
                + objetivo.titulo
                + " está programada en "
                + objetivo.sala
                + " el "
                + objetivo.inicio.format(fechaFormato)
                + " a las "
                + objetivo.inicio.format(horaFormato)
                + " y reservaría la sala hasta las "
                + finOperativoObjetivo.format(horaFormato)
                + " incluyendo 30 minutos de limpieza. "
                + "Se cruza con "
                + otra.titulo
                + " a las "
                + otra.inicio.format(horaFormato)
                + ". Cambie primero el horario o cancele una de las funciones.";
    }

    private int obtenerDuracionActual(
            Connection con,
            int idPelicula
    ) throws SQLException {
        String sql =
                "SELECT duracion FROM peliculas "
                        + "WHERE id_pelicula = ? LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPelicula);
            ps.setQueryTimeout(10);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("duracion");
                }
            }
        }

        return DURACION_PREDETERMINADA_MIN;
    }

    private int normalizarDuracion(int duracion) {
        return duracion > 0
                ? duracion
                : DURACION_PREDETERMINADA_MIN;
    }

    private static final class FuncionProgramada {
        private final int idFuncion;
        private final int idPelicula;
        private final int idSala;
        private final String titulo;
        private final String sala;
        private final LocalDateTime inicio;
        private final int duracion;

        private FuncionProgramada(
                int idFuncion,
                int idPelicula,
                int idSala,
                String titulo,
                String sala,
                LocalDateTime inicio,
                int duracion
        ) {
            this.idFuncion = idFuncion;
            this.idPelicula = idPelicula;
            this.idSala = idSala;
            this.titulo = titulo;
            this.sala = sala;
            this.inicio = inicio;
            this.duracion = duracion;
        }
    }

    private boolean peliculaYaRegistradaExceptoId(
            String titulo,
            int idExcluir
    ) {
        if (titulo == null || titulo.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM peliculas "
                + "WHERE LOWER(TRIM(titulo)) = LOWER(TRIM(?)) "
                + "AND id_pelicula <> ?";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, titulo.trim());
            ps.setInt(2, idExcluir);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo verificar el título de la película.",
                    e
            );
        }
    }

    // ========================================================
    // ELIMINACIÓN
    // ========================================================
    public RespuestaEliminacion eliminarPelicula(
            int idPelicula
    ) {
        if (idPelicula <= 0) {
            return new RespuestaEliminacion(
                    ResultadoEliminacion.NO_ENCONTRADA,
                    "Seleccione una película válida"
            );
        }

        Connection con = null;

        try {
            con = BDCINEX.conectar();
            con.setAutoCommit(false);

            if (!peliculaExiste(con, idPelicula)) {
                con.rollback();

                return new RespuestaEliminacion(
                        ResultadoEliminacion.NO_ENCONTRADA,
                        "La película seleccionada ya no existe"
                );
            }

            int funciones = contarFuncionesAsociadas(
                    con,
                    idPelicula
            );

            if (funciones > 0) {
                con.rollback();

                return new RespuestaEliminacion(
                        ResultadoEliminacion.FUNCIONES_ASOCIADAS,
                        MENSAJE_CON_FUNCIONES
                                + " (" + funciones + "). "
                                + "Se conserva para proteger las ventas, "
                                + "entradas y comprobantes históricos."
                );
            }

            String sql = "DELETE FROM peliculas "
                    + "WHERE id_pelicula = ?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idPelicula);

                if (ps.executeUpdate() == 0) {
                    con.rollback();

                    return new RespuestaEliminacion(
                            ResultadoEliminacion.NO_ENCONTRADA,
                            "La película seleccionada ya no existe"
                    );
                }
            }

            con.commit();

            return new RespuestaEliminacion(
                    ResultadoEliminacion.ELIMINADA,
                    MENSAJE_ELIMINADA
            );

        } catch (SQLIntegrityConstraintViolationException e) {
            rollbackSeguro(con);

            return new RespuestaEliminacion(
                    ResultadoEliminacion.FUNCIONES_ASOCIADAS,
                    "La película no puede eliminarse porque está "
                            + "relacionada con información del sistema"
            );

        } catch (SQLException e) {
            rollbackSeguro(con);

            if (esErrorIntegridad(e)) {
                return new RespuestaEliminacion(
                        ResultadoEliminacion.FUNCIONES_ASOCIADAS,
                        "La película no puede eliminarse porque está "
                                + "relacionada con funciones, ventas o entradas"
                );
            }

            return new RespuestaEliminacion(
                    ResultadoEliminacion.ERROR,
                    "No se pudo eliminar la película"
            );

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

    private int contarFuncionesAsociadas(
            Connection con,
            int idPelicula
    ) throws SQLException {
        Set<String> columnasFunciones =
                obtenerColumnas(con, "funciones");

        if (!columnasFunciones.contains("id_pelicula")) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM funciones "
                + "WHERE id_pelicula = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPelicula);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ========================================================
    // MÉTODOS DE APOYO
    // ========================================================
    private boolean peliculaExiste(
            Connection con,
            int idPelicula
    ) throws SQLException {
        String sql = "SELECT 1 FROM peliculas "
                + "WHERE id_pelicula = ? LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPelicula);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void agregarInsercionSiExiste(
            Set<String> columnas,
            ArrayList<String> nombresColumnas,
            ArrayList<Object> valores,
            String columna,
            Object valor
    ) {
        if (columnas.contains(columna.toLowerCase(Locale.ROOT))) {
            nombresColumnas.add(columna);
            valores.add(valor);
        }
    }

    private void agregarActualizacionSiExiste(
            Set<String> columnas,
            ArrayList<String> asignaciones,
            ArrayList<Object> valores,
            String columna,
            Object valor
    ) {
        if (columnas.contains(columna.toLowerCase(Locale.ROOT))) {
            asignaciones.add(columna + " = ?");
            valores.add(valor);
        }
    }

    private Set<String> obtenerColumnas(
            Connection con,
            String tabla
    ) throws SQLException {
        Set<String> columnas = new HashSet<>();
        DatabaseMetaData meta = con.getMetaData();

        try (ResultSet rs = meta.getColumns(
                con.getCatalog(),
                null,
                tabla,
                null
        )) {
            while (rs.next()) {
                columnas.add(
                        rs.getString("COLUMN_NAME")
                                .toLowerCase(Locale.ROOT)
                );
            }
        }

        if (columnas.isEmpty()) {
            try (ResultSet rs = meta.getColumns(
                    null,
                    null,
                    tabla,
                    null
            )) {
                while (rs.next()) {
                    columnas.add(
                            rs.getString("COLUMN_NAME")
                                    .toLowerCase(Locale.ROOT)
                    );
                }
            }
        }

        return columnas;
    }

    private void rollbackSeguro(Connection con) {
        if (con == null) {
            return;
        }

        try {
            con.rollback();
        } catch (SQLException ignored) {
        }
    }

    private boolean esErrorIntegridad(SQLException e) {
        String sqlState = e.getSQLState();
        return sqlState != null && sqlState.startsWith("23");
    }

    private String textoSeguro(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String mensajeSeguro(
            RuntimeException excepcion,
            String mensajePredeterminado
    ) {
        String mensaje = excepcion.getMessage();

        if (mensaje == null || mensaje.trim().isEmpty()) {
            return mensajePredeterminado;
        }

        return mensaje;
    }
}
