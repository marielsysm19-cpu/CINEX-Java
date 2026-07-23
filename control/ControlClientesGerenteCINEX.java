package control;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ControlClientesGerenteCINEX {

    public ArrayList<Object[]> listarClientesCompradores() {
        ArrayList<Object[]> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT c.dni, c.nombre " +
                "FROM clientes c INNER JOIN ventas v ON c.id_cliente = v.id_cliente " +
                "WHERE c.dni IS NOT NULL AND c.dni <> '' AND c.dni <> '00000000' " +
                "AND c.nombre IS NOT NULL AND c.nombre <> '' ORDER BY c.nombre ASC";
        try (Connection con = BDCINEX.conectar(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(new Object[]{rs.getString("dni"), rs.getString("nombre")});
        } catch (SQLException e) {
            System.out.println("[CLIENTES GERENTE] " + e.getMessage());
        }
        return lista;
    }
}
