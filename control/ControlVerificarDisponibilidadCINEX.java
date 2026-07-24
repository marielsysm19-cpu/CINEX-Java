package control;

import entidad.AsientoCINEX;
import entidad.FuncionCINEX;
import entidad.ReferenciaFuncionCINEX;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class ControlVerificarDisponibilidadCINEX {

    public static Set<String> consultarAsientosOcupados(
            String pelicula,
            String referenciaFuncion
    ) {
        LinkedHashSet<String> ocupados = new LinkedHashSet<>();

        ArrayList<String> datos = consultarCodigosOcupados(
                pelicula,
                referenciaFuncion
        );

        if (datos == null) {
            return ocupados;
        }

        for (String asiento : datos) {
            if (asiento != null && !asiento.trim().isEmpty()) {
                ocupados.add(
                        asiento.trim().toUpperCase()
                );
            }
        }

        return ocupados;
    }

    public static int consultarCapacidadFuncion(
            String pelicula,
            String referenciaFuncion
    ) {
        int idFuncion = resolverIdFuncion(pelicula, referenciaFuncion);
        if (idFuncion <= 0) {
            return 100;
        }

        String sql = "SELECT s.capacidad FROM funciones f "
                + "INNER JOIN salas s ON f.id_sala = s.id_sala "
                + "WHERE f.id_funcion = ? LIMIT 1";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idFuncion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int capacidad = rs.getInt("capacidad");
                    return capacidad > 0 ? capacidad : 100;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al consultar capacidad de la función: " + e.getMessage());
        }

        return 100;
    }

    public static boolean verificarDisponibilidad(
            String pelicula,
            String referenciaFuncion,
            String asiento
    ) {
        if (asiento == null || asiento.trim().isEmpty()) {
            return false;
        }

        String codigo = asiento.trim().toUpperCase();

        return !consultarAsientosOcupados(
                pelicula,
                referenciaFuncion
        ).contains(codigo);
    }

    public static boolean guardarSeleccionTemporal(
            String usuario,
            String pelicula,
            String referenciaFuncion,
            ArrayList<String> asientos
    ) {
        if (asientos == null || asientos.isEmpty()) {
            return false;
        }

        /*
         * La selección se conserva en memoria hasta el pago.
         * Antes de continuar se verifica nuevamente cada asiento
         * contra la función exacta identificada por su ID.
         */
        for (String asiento : asientos) {
            if (!verificarDisponibilidad(
                    pelicula,
                    referenciaFuncion,
                    asiento
            )) {
                return false;
            }
        }

        return true;
    }

    public static ArrayList<AsientoCINEX> consultarMapaAsientos(
            String pelicula,
            String referenciaFuncion
    ) {
        int capacidad = consultarCapacidadFuncion(
                pelicula,
                referenciaFuncion
        );

        Set<String> ocupados = consultarAsientosOcupados(
                pelicula,
                referenciaFuncion
        );

        ArrayList<AsientoCINEX> mapa = new ArrayList<>();

        int columnas;

        if (capacidad <= 100) {
            columnas = 10;
        } else if (capacidad <= 120) {
            columnas = 12;
        } else {
            columnas = 14;
        }

        int filas = (int) Math.ceil(
                capacidad / (double) columnas
        );

        int contador = 0;

        for (int f = 0; f < filas; f++) {
            String fila = String.valueOf((char) ('A' + f));

            for (int numero = 1; numero <= columnas; numero++) {
                contador++;

                if (contador > capacidad) {
                    break;
                }

                String codigo = fila + numero;

                mapa.add(
                        new AsientoCINEX(
                                codigo,
                                ocupados.contains(codigo)
                        )
                );
            }
        }

        return mapa;
    }

    public static FuncionCINEX consultarFuncionSeleccionada(
            String pelicula,
            String referenciaFuncion
    ) {
        int idFuncion =
                ReferenciaFuncionCINEX.obtenerId(
                        referenciaFuncion
                );

        if (idFuncion > 0) {
            FuncionCINEX funcion =
                    ControlConsultarFuncionesCINEX
                            .consultarDetalleFuncion(idFuncion);

            if (funcion != null) {
                return funcion;
            }
        }

        FuncionCINEX funcion = new FuncionCINEX();
        funcion.setPelicula(pelicula);
        funcion.setEstado("Activa");
        funcion.setCapacidad(
                consultarCapacidadFuncion(
                        pelicula,
                        referenciaFuncion
                )
        );
        funcion.setVendidos(
                consultarAsientosOcupados(
                        pelicula,
                        referenciaFuncion
                ).size()
        );

        return funcion;
    }

    private static ArrayList<String> consultarCodigosOcupados(
            String pelicula,
            String referenciaFuncion
    ) {
        ArrayList<String> ocupados = new ArrayList<>();
        int idFuncion = resolverIdFuncion(pelicula, referenciaFuncion);

        if (idFuncion <= 0) {
            return ocupados;
        }

        String sql = "SELECT DISTINCT CONCAT(a.fila, a.numero) AS asiento "
                + "FROM entradas e INNER JOIN asientos a ON e.id_asiento = a.id_asiento "
                + "WHERE e.id_funcion = ? "
                + "AND (e.estado IS NULL OR e.estado NOT IN ('Anulada', 'Reembolsada'))";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idFuncion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ocupados.add(rs.getString("asiento"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al consultar asientos ocupados: " + e.getMessage());
        }

        return ocupados;
    }

    private static int resolverIdFuncion(String pelicula, String referenciaFuncion) {
        int idDirecto = ReferenciaFuncionCINEX.obtenerId(referenciaFuncion);
        if (idDirecto > 0) {
            return idDirecto;
        }

        String titulo = pelicula == null ? "" : pelicula.trim();
        String visible = ReferenciaFuncionCINEX.mostrar(referenciaFuncion);
        if (titulo.isEmpty() || visible.isEmpty()) {
            return -1;
        }

        String hora24 = normalizarHora(visible);
        String sql = "SELECT f.id_funcion FROM funciones f "
                + "INNER JOIN peliculas p ON f.id_pelicula = p.id_pelicula "
                + "WHERE LOWER(TRIM(p.titulo)) = LOWER(TRIM(?)) "
                + "AND f.estado = 'Activa' "
                + "AND f.fecha >= CURDATE() AND f.fecha < DATE_ADD(CURDATE(), INTERVAL 2 DAY) "
                + "AND (TIME_FORMAT(f.hora, '%H:%i') = ? "
                + "OR UPPER(TIME_FORMAT(f.hora, '%h:%i %p')) = ? "
                + "OR UPPER(TIME_FORMAT(f.hora, '%l:%i %p')) = ?) "
                + "ORDER BY f.fecha, f.hora LIMIT 1";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, titulo);
            ps.setString(2, hora24);
            ps.setString(3, visible.toUpperCase());
            ps.setString(4, visible.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id_funcion") : -1;
            }
        } catch (SQLException e) {
            System.out.println("Error al resolver función: " + e.getMessage());
            return -1;
        }
    }

    private static String normalizarHora(String texto) {
        if (texto == null) {
            return "";
        }
        String hora = texto.trim().toUpperCase()
                .replace("A. M.", "AM")
                .replace("P. M.", "PM")
                .replace("A.M.", "AM")
                .replace("P.M.", "PM");
        try {
            LocalTime valor = LocalTime.parse(
                    hora,
                    DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
            );
            return valor.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return hora;
        }
    }

}
