package control;

import entidad.FuncionCINEX;
import entidad.PeliculaCINEX;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;

public class ControlConsultarFuncionesCINEX {

    public static ArrayList<FuncionCINEX> consultarFuncionesPorPelicula(
            PeliculaCINEX pelicula
    ) {
        ArrayList<FuncionCINEX> funciones = new ArrayList<>();

        if (pelicula == null
                || pelicula.getTitulo() == null
                || pelicula.getTitulo().trim().isEmpty()) {
            return funciones;
        }

        String sql = "SELECT f.id_funcion, p.titulo, p.genero, p.duracion, p.clasificacion, p.imagen, "
                + "s.id_sala, s.nombre AS sala, s.capacidad, s.tipo, f.fecha, f.hora, f.estado, "
                + "IFNULL(vendidos.total, 0) AS vendidos "
                + "FROM funciones f "
                + "INNER JOIN peliculas p ON f.id_pelicula = p.id_pelicula "
                + "INNER JOIN salas s ON f.id_sala = s.id_sala "
                + "LEFT JOIN (SELECT id_funcion, COUNT(*) AS total FROM entradas "
                + "WHERE estado IS NULL OR estado NOT IN ('Anulada', 'Reembolsada') GROUP BY id_funcion) vendidos "
                + "ON vendidos.id_funcion = f.id_funcion "
                + "WHERE LOWER(TRIM(p.titulo)) = LOWER(TRIM(?)) "
                + "AND f.estado = 'Activa' "
                + "AND f.fecha >= CURDATE() "
                + "AND f.fecha < DATE_ADD(CURDATE(), INTERVAL 2 DAY) "
                + "AND (f.fecha > CURDATE() OR TIMESTAMP(f.fecha, f.hora) >= DATE_SUB(NOW(), INTERVAL 10 MINUTE)) "
                + "ORDER BY f.fecha ASC, f.hora ASC";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, pelicula.getTitulo().trim());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FuncionCINEX funcion = mapearFuncion(rs);
                    funcion.setPeliculaEntidad(pelicula);
                    funcion.setIdSala(rs.getInt("id_sala"));
                    funciones.add(funcion);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al listar funciones de la película: " + e.getMessage());
        }

        return funciones;
    }

    public ArrayList<FuncionCINEX> verificarFunciones(
            String tituloPelicula
    ) {
        String titulo = limpiarTitulo(tituloPelicula);
        if (titulo.isEmpty()) {
            return new ArrayList<>();
        }

        PeliculaCINEX pelicula = obtenerPeliculaPorTitulo(titulo);
        if (pelicula == null) {
            pelicula = new PeliculaCINEX();
            pelicula.setTitulo(titulo);
        }

        return consultarFuncionesPorPelicula(pelicula);
    }

    public PeliculaCINEX obtenerPeliculaPorTitulo(
            String tituloPelicula
    ) {
        String titulo = limpiarTitulo(tituloPelicula);
        if (titulo.isEmpty()) {
            return null;
        }

        String sql = "SELECT id_pelicula, titulo, genero, duracion, clasificacion, imagen, estado, en_cartelera "
                + "FROM peliculas WHERE LOWER(TRIM(titulo)) = LOWER(TRIM(?)) LIMIT 1";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, titulo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PeliculaCINEX pelicula = new PeliculaCINEX();
                    pelicula.setIdPelicula(rs.getInt("id_pelicula"));
                    pelicula.setTitulo(rs.getString("titulo"));
                    pelicula.setGenero(rs.getString("genero"));
                    pelicula.setDuracion(rs.getInt("duracion"));
                    pelicula.setClasificacion(rs.getString("clasificacion"));
                    pelicula.setImagen(rs.getString("imagen"));
                    pelicula.setEstado(rs.getString("estado"));
                    pelicula.setEnCartelera(rs.getInt("en_cartelera"));
                    return pelicula;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al consultar película: " + e.getMessage());
        }

        return null;
    }

    public static ArrayList<FuncionCINEX> consultarFuncionesRegistradas(LocalDate fecha, String turno) throws SQLException {
        ArrayList<FuncionCINEX> lista = new ArrayList<>();

        if (fecha == null) {
            return lista;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT f.id_funcion, p.titulo, p.genero, p.duracion, p.clasificacion, p.imagen, ");
        sql.append("s.nombre AS sala, s.capacidad, s.tipo, f.fecha, f.hora, f.estado, ");
        sql.append("IFNULL(vendidos.total, 0) AS vendidos ");
        sql.append("FROM funciones f ");
        sql.append("INNER JOIN peliculas p ON f.id_pelicula = p.id_pelicula ");
        sql.append("INNER JOIN salas s ON f.id_sala = s.id_sala ");
        sql.append("LEFT JOIN ( ");
        sql.append("   SELECT id_funcion, COUNT(*) AS total ");
        sql.append("   FROM entradas ");
        sql.append("   WHERE estado IS NULL OR estado NOT IN ('Anulada', 'Reembolsada') ");
        sql.append("   GROUP BY id_funcion ");
        sql.append(") vendidos ON vendidos.id_funcion = f.id_funcion ");
        sql.append("WHERE f.estado = 'Activa' ");
        sql.append("AND p.estado = 'Activa' ");
        sql.append("AND p.en_cartelera = 1 ");
        sql.append("AND f.fecha = ? ");
        sql.append("AND (f.fecha > CURDATE() ");
        sql.append("OR TIMESTAMP(f.fecha, f.hora) ");
        sql.append(">= DATE_SUB(NOW(), INTERVAL 10 MINUTE)) ");

        String turnoNormalizado = turno == null ? "Todos" : turno.trim();

        if ("Día".equalsIgnoreCase(turnoNormalizado)) {
            sql.append("AND TIME(f.hora) >= '06:00:00' AND TIME(f.hora) < '12:00:00' ");
        } else if ("Tarde".equalsIgnoreCase(turnoNormalizado)) {
            sql.append("AND TIME(f.hora) >= '12:00:00' AND TIME(f.hora) < '18:00:00' ");
        } else if ("Noche".equalsIgnoreCase(turnoNormalizado)) {
            sql.append("AND TIME(f.hora) >= '18:00:00' ");
        }

        sql.append("ORDER BY f.fecha ASC, f.hora ASC, p.titulo ASC");

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            ps.setDate(1, java.sql.Date.valueOf(fecha));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearFuncion(rs));
                }
            }
        }

        return lista;
    }

    /**
     * Una función puede vender entradas desde antes de iniciar
     * y hasta diez minutos después de su hora de comienzo.
     * Las funciones canceladas, inactivas o fuera del margen
     * dejan de estar disponibles inmediatamente.
     */
    public static boolean funcionDisponibleParaVenta(
            int idFuncion
    ) {
        if (idFuncion <= 0) {
            return false;
        }

        String sql =
                "SELECT COUNT(*) "
                        + "FROM funciones f "
                        + "INNER JOIN peliculas p "
                        + "ON p.id_pelicula = f.id_pelicula "
                        + "WHERE f.id_funcion = ? "
                        + "AND f.estado = 'Activa' "
                        + "AND p.estado = 'Activa' "
                        + "AND p.en_cartelera = 1 "
                        + "AND TIMESTAMP(f.fecha, f.hora) "
                        + ">= DATE_SUB(NOW(), INTERVAL 10 MINUTE)";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idFuncion);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error al validar disponibilidad de función: "
                            + e.getMessage()
            );
            return false;
        }
    }

    public static FuncionCINEX consultarDetalleFuncion(int idFuncion) {
        if (idFuncion <= 0) {
            return null;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT f.id_funcion, p.titulo, p.genero, p.duracion, p.clasificacion, p.imagen, ");
        sql.append("s.nombre AS sala, s.capacidad, s.tipo, f.fecha, f.hora, f.estado, ");
        sql.append("IFNULL(vendidos.total, 0) AS vendidos ");
        sql.append("FROM funciones f ");
        sql.append("INNER JOIN peliculas p ON f.id_pelicula = p.id_pelicula ");
        sql.append("INNER JOIN salas s ON f.id_sala = s.id_sala ");
        sql.append("LEFT JOIN ( ");
        sql.append("   SELECT id_funcion, COUNT(*) AS total ");
        sql.append("   FROM entradas ");
        sql.append("   WHERE estado IS NULL OR estado NOT IN ('Anulada', 'Reembolsada') ");
        sql.append("   GROUP BY id_funcion ");
        sql.append(") vendidos ON vendidos.id_funcion = f.id_funcion ");
        sql.append("WHERE f.id_funcion = ? ");
        sql.append("LIMIT 1");

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            ps.setInt(1, idFuncion);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearFuncion(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al consultar detalle de función: " + e.getMessage());
        }

        return null;
    }

    private static FuncionCINEX convertirFuncionDesdeFila(
            Object[] fila,
            PeliculaCINEX pelicula
    ) {
        if (fila == null || fila.length == 0) {
            return null;
        }

        FuncionCINEX funcion = new FuncionCINEX();

        funcion.setIdFuncion(
                obtenerEnteroSeguro(fila, 0, 0)
        );
        funcion.setPeliculaEntidad(pelicula);
        funcion.setPelicula(pelicula.getTitulo());
        funcion.setGenero(
                obtenerTextoSeguro(fila, 2, pelicula.getGenero())
        );
        funcion.setDuracionMinutos(
                obtenerEnteroSeguro(
                        fila,
                        3,
                        pelicula.getDuracion()
                )
        );
        funcion.setClasificacion(
                obtenerTextoSeguro(
                        fila,
                        4,
                        pelicula.getClasificacion()
                )
        );
        funcion.setImagen(
                obtenerTextoSeguro(
                        fila,
                        5,
                        pelicula.getImagen()
                )
        );
        funcion.setSala(
                obtenerTextoSeguro(fila, 6, "Sala")
        );
        funcion.setCapacidad(
                obtenerEnteroSeguro(fila, 7, 0)
        );
        funcion.setTipoSala(
                obtenerTextoSeguro(fila, 8, "2D")
        );
        funcion.setFechaTexto(
                obtenerTextoSeguro(fila, 9, "")
        );
        funcion.setHoraBD(
                obtenerTextoSeguro(fila, 10, "")
        );

        int disponibles =
                obtenerEnteroSeguro(fila, 11, 0);
        funcion.setDisponibles(disponibles);

        return funcion;
    }

    private static String limpiarTitulo(String titulo) {
        return titulo == null
                ? ""
                : titulo.replace("\\n", " ")
                        .replace("\n", " ")
                        .trim();
    }

    private static int obtenerEnteroSeguro(
            Object[] fila,
            int indice,
            int defecto
    ) {
        try {
            if (fila == null
                    || fila.length <= indice
                    || fila[indice] == null) {
                return defecto;
            }

            if (fila[indice] instanceof Number) {
                return ((Number) fila[indice]).intValue();
            }

            return Integer.parseInt(
                    String.valueOf(fila[indice]).trim()
            );

        } catch (Exception e) {
            return defecto;
        }
    }

    private static String obtenerTextoSeguro(
            Object[] fila,
            int indice,
            String defecto
    ) {
        if (fila == null
                || fila.length <= indice
                || fila[indice] == null) {
            return defecto;
        }

        String texto =
                String.valueOf(fila[indice]).trim();

        return texto.isEmpty() ? defecto : texto;
    }

    private static FuncionCINEX mapearFuncion(ResultSet rs) throws SQLException {
        FuncionCINEX funcion = new FuncionCINEX();

        funcion.setIdFuncion(rs.getInt("id_funcion"));
        funcion.setPelicula(textoSeguro(rs.getString("titulo"), "Película sin nombre"));
        funcion.setGenero(textoSeguro(rs.getString("genero"), "Sin género"));
        funcion.setDuracionMinutos(Math.max(0, rs.getInt("duracion")));
        funcion.setClasificacion(textoSeguro(rs.getString("clasificacion"), "-"));
        funcion.setImagen(textoSeguro(rs.getString("imagen"), ""));
        funcion.setSala(textoSeguro(rs.getString("sala"), "Sala no especificada"));
        funcion.setTipoSala(textoSeguro(rs.getString("tipo"), "2D"));

        Date fecha = rs.getDate("fecha");
        Time hora = rs.getTime("hora");

        funcion.setFecha(fecha == null ? null : fecha.toLocalDate());
        funcion.setHora(hora == null ? null : hora.toLocalTime());
        funcion.setEstado(textoSeguro(rs.getString("estado"), "Activa"));
        funcion.setCapacidad(Math.max(0, rs.getInt("capacidad")));
        funcion.setVendidos(Math.max(0, rs.getInt("vendidos")));

        return funcion;
    }

    private static String textoSeguro(String texto, String defecto) {
        return texto == null || texto.trim().isEmpty() || "null".equalsIgnoreCase(texto.trim()) ? defecto : texto.trim();
    }
}
