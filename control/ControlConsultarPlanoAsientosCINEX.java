package control;

import entidad.FuncionCINEX;
import entidad.SalaCINEX;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Control exclusivo para consultar la sala y construir su plano.
 * La consulta de películas y funciones pertenece a
 * ControlConsultarFuncionesCINEX.
 */
public class ControlConsultarPlanoAsientosCINEX {

    public static SalaCINEX obtenerPlanoSala(FuncionCINEX funcion) {
        SalaCINEX sala = obtenerSalaAsociada(funcion);
        return obtenerDisenoSala(sala);
    }

    public static SalaCINEX obtenerSalaAsociada(FuncionCINEX funcion) {
        if (funcion == null) {
            return new SalaCINEX(0, "Sala", "", 100);
        }

        if (funcion.getIdFuncion() > 0) {
            SalaCINEX salaBD = consultarSalaPorFuncion(funcion.getIdFuncion());
            if (salaBD != null) {
                funcion.setSalaEntidad(salaBD);
                funcion.setIdSala(salaBD.getIdSala());
                funcion.setSala(salaBD.getNombre());
                funcion.setTipoSala(salaBD.getTipo());
                funcion.setCapacidad(salaBD.getCapacidad());
                return salaBD;
            }
        }

        String nombre = funcion.getSala();
        int capacidad = funcion.getCapacidad();
        if (capacidad <= 0) {
            capacidad = capacidadPorDefecto(nombre);
        }

        SalaCINEX sala = new SalaCINEX(
                funcion.getIdSala(),
                nombre == null || nombre.trim().isEmpty() ? "Sala" : nombre,
                funcion.getTipoSala(),
                capacidad
        );
        funcion.setSalaEntidad(sala);
        return sala;
    }

    public static SalaCINEX consultarSalaPorFuncion(int idFuncion) {
        if (idFuncion <= 0) {
            return null;
        }

        String sql = "SELECT s.id_sala, s.nombre, s.tipo, s.capacidad, s.estado "
                + "FROM funciones f INNER JOIN salas s ON f.id_sala = s.id_sala "
                + "WHERE f.id_funcion = ? LIMIT 1";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idFuncion);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SalaCINEX sala = new SalaCINEX(
                            rs.getInt("id_sala"),
                            rs.getString("nombre"),
                            rs.getString("tipo"),
                            rs.getInt("capacidad")
                    );
                    sala.setEstado(rs.getString("estado"));
                    return sala;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al consultar sala de la función: " + e.getMessage());
        }

        return null;
    }

    public static SalaCINEX obtenerDisenoSala(SalaCINEX sala) {
        if (sala == null) {
            return new SalaCINEX(0, "Sala", "", 100);
        }

        if (sala.getCapacidad() <= 0) {
            sala.setCapacidad(capacidadPorDefecto(sala.getNombre()));
        }

        return sala;
    }

    private static int capacidadPorDefecto(String sala) {
        String texto = sala == null ? "" : sala.toLowerCase();

        if (texto.contains("1")) {
            return 80;
        }
        if (texto.contains("2")) {
            return 100;
        }
        if (texto.contains("3")) {
            return 120;
        }
        return 100;
    }
}
