package control;

import entidad.SalaCINEX;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ControlGestionarSalasCINEX {

    public ArrayList<SalaCINEX> listarSalas() {
        ArrayList<SalaCINEX> lista = new ArrayList<>();

        String sql = "SELECT id_sala, nombre, capacidad, tipo, estado "
                + "FROM salas ORDER BY id_sala ASC";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                SalaCINEX sala = new SalaCINEX(
                        rs.getInt("id_sala"),
                        rs.getString("nombre"),
                        rs.getString("tipo"),
                        rs.getInt("capacidad"),
                        rs.getString("estado")
                );
                lista.add(sala);
            }

        } catch (SQLException e) {
            System.out.println("[ControlGestionarSalasCINEX] Error al listar salas: " + e.getMessage());
        }

        return lista;
    }
}
