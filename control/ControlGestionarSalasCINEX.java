package control;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ControlGestionarSalasCINEX {

    public ArrayList<Object[]> listarSalas() {
        ArrayList<Object[]> lista = new ArrayList<>();

        String sql = "SELECT id_sala, nombre, capacidad, tipo, estado "
                + "FROM salas ORDER BY id_sala ASC";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new Object[]{
                        rs.getInt("id_sala"),
                        rs.getString("nombre"),
                        rs.getInt("capacidad"),
                        rs.getString("tipo"),
                        rs.getString("estado")
                });
            }

        } catch (SQLException e) {
            System.out.println("[ControlGestionarSalasCINEX] Error al listar salas: " + e.getMessage());
        }

        return lista;
    }
}
