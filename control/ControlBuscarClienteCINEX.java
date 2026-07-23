package control;

import entidad.ClienteCINEX;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ControlBuscarClienteCINEX {

    private ControlBuscarClienteCINEX() {
    }

    public static ArrayList<ClienteCINEX> buscarCliente(String tipoBusqueda, String busqueda) throws SQLException {
        ArrayList<ClienteCINEX> clientes = new ArrayList<>();
        String dato = busqueda == null ? "" : busqueda.trim();

        if (dato.isEmpty()) {
            return clientes;
        }

        boolean buscarPorDocumento = "Documento".equalsIgnoreCase(tipoBusqueda);
        String sql;

        if (buscarPorDocumento) {
            sql = "SELECT id_cliente, dni, nombre " +
                    "FROM clientes " +
                    "WHERE dni = ? " +
                    "ORDER BY nombre ASC";
        } else {
            sql = "SELECT id_cliente, dni, nombre " +
                    "FROM clientes " +
                    "WHERE nombre LIKE ? OR dni LIKE ? " +
                    "ORDER BY nombre ASC";
        }

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (buscarPorDocumento) {
                ps.setString(1, dato);
            } else {
                ps.setString(1, "%" + dato + "%");
                ps.setString(2, "%" + dato + "%");
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clientes.add(mapearCliente(rs));
                }
            }
        }

        return clientes;
    }

    public static ClienteCINEX consultarCliente(String documento) throws SQLException {
        if (documento == null || documento.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT id_cliente, dni, nombre " +
                "FROM clientes " +
                "WHERE dni = ? " +
                "LIMIT 1";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, documento.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCliente(rs);
                }
            }
        }

        return null;
    }

    private static ClienteCINEX mapearCliente(ResultSet rs) throws SQLException {
        ClienteCINEX cliente = new ClienteCINEX();
        cliente.setIdCliente(rs.getInt("id_cliente"));
        cliente.setDni(rs.getString("dni"));
        cliente.setNumeroDocumento(rs.getString("dni"));
        cliente.setNombre(rs.getString("nombre"));
        return cliente;
    }
}
