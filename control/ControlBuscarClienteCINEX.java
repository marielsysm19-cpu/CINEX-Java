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

    public static ArrayList<ClienteCINEX> buscarCliente(String tipoBusqueda, String busqueda) {
        ArrayList<ClienteCINEX> clientes = new ArrayList<>();
        String dato = busqueda == null ? "" : busqueda.trim();

        if (dato.isEmpty()) {
            return clientes;
        }

        boolean buscarPorDocumento = "Documento".equalsIgnoreCase(tipoBusqueda)
                || "DNI".equalsIgnoreCase(tipoBusqueda)
                || "C.E.".equalsIgnoreCase(tipoBusqueda)
                || "CE".equalsIgnoreCase(tipoBusqueda);
        boolean buscarPorNombre = "Nombre".equalsIgnoreCase(tipoBusqueda);

        if (!buscarPorDocumento && !buscarPorNombre) {
            return clientes;
        }

        String sql = buscarPorDocumento
                ? "SELECT id_cliente, dni, nombre FROM clientes WHERE dni = ? ORDER BY nombre ASC"
                : "SELECT id_cliente, dni, nombre FROM clientes WHERE nombre LIKE ? ORDER BY nombre ASC";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, buscarPorDocumento ? dato : "%" + dato + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clientes.add(mapearCliente(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo buscar el cliente.", e);
        }

        return clientes;
    }

    public static ClienteCINEX consultarCliente(String documento) {
        if (documento == null || documento.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT id_cliente, dni, nombre FROM clientes WHERE dni = ? LIMIT 1";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, documento.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCliente(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar el cliente.", e);
        }

        return null;
    }

    private static ClienteCINEX mapearCliente(ResultSet rs) throws SQLException {
        ClienteCINEX cliente = new ClienteCINEX();
        cliente.setIdCliente(rs.getInt("id_cliente"));
        cliente.setNumeroDocumento(rs.getString("dni"));
        cliente.setNombre(rs.getString("nombre"));
        return cliente;
    }
}
