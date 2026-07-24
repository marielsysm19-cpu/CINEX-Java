package control;

import entidad.AsientoCINEX;
import entidad.FuncionCINEX;
import entidad.SalaCINEX;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ControlIdentificarDisponibilidadCINEX {

    public static ArrayList<AsientoCINEX>
            identificarDisponibilidadAsientos(FuncionCINEX funcion) {
        return obtenerEstadoAsientos(funcion);
    }

    public static ArrayList<AsientoCINEX> obtenerEstadoAsientos(
            FuncionCINEX funcion
    ) {
        ArrayList<AsientoCINEX> asientos = new ArrayList<>();

        if (funcion == null) {
            return asientos;
        }

        SalaCINEX sala =
                ControlConsultarPlanoAsientosCINEX
                        .obtenerSalaAsociada(funcion);

        int capacidad = sala.getCapacidad() > 0
                ? sala.getCapacidad()
                : 100;

        Set<String> ocupados = obtenerCodigosOcupados(funcion);

        int columnas = sala.getColumnas() > 0
                ? sala.getColumnas()
                : 10;

        int filas = (int) Math.ceil(
                capacidad / (double) columnas
        );

        int contador = 0;

        for (int f = 0; f < filas; f++) {
            String letra = String.valueOf((char) ('A' + f));

            for (int c = 1; c <= columnas; c++) {
                contador++;

                if (contador > capacidad) {
                    break;
                }

                String codigo = letra + c;
                AsientoCINEX asiento = new AsientoCINEX(
                        codigo,
                        ocupados.contains(codigo.toUpperCase())
                );

                asientos.add(asiento);
            }
        }

        return asientos;
    }

    public static Set<String> obtenerCodigosOcupados(
            FuncionCINEX funcion
    ) {
        Set<String> ocupados = new HashSet<>();

        if (funcion == null || funcion.getIdFuncion() <= 0) {
            return ocupados;
        }

        String sql = "SELECT DISTINCT CONCAT(a.fila, a.numero) AS asiento "
                + "FROM entradas e INNER JOIN asientos a ON e.id_asiento = a.id_asiento "
                + "WHERE e.id_funcion = ? "
                + "AND (e.estado IS NULL OR e.estado NOT IN ('Anulada', 'Reembolsada'))";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, funcion.getIdFuncion());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String asiento = rs.getString("asiento");
                    if (asiento != null && !asiento.trim().isEmpty()) {
                        ocupados.add(asiento.trim().toUpperCase());
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al consultar asientos ocupados: " + e.getMessage());
        }

        return ocupados;
    }

    public static int contarOcupados(
            ArrayList<AsientoCINEX> asientos
    ) {
        int total = 0;

        if (asientos == null) {
            return 0;
        }

        for (AsientoCINEX asiento : asientos) {
            if (asiento != null && asiento.isOcupado()) {
                total++;
            }
        }

        return total;
    }

    public static int contarDisponibles(
            ArrayList<AsientoCINEX> asientos
    ) {
        int total = 0;

        if (asientos == null) {
            return 0;
        }

        for (AsientoCINEX asiento : asientos) {
            if (asiento != null && asiento.estaDisponible()) {
                total++;
            }
        }

        return total;
    }
}
