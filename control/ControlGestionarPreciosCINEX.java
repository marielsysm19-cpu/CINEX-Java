package control;

import entidad.PrecioCINEX;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ControlGestionarPreciosCINEX {

    private String ultimoError = "";

    public ArrayList<PrecioCINEX> listarPrecios() {
        ArrayList<PrecioCINEX> lista = new ArrayList<>();
        ultimoError = "";

        String sql = "SELECT id_precio, tipo_entrada, monto "
                + "FROM precios ORDER BY id_precio ASC";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setQueryTimeout(15);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new PrecioCINEX(
                            rs.getInt("id_precio"),
                            rs.getString("tipo_entrada"),
                            rs.getDouble("monto"),
                            "Activo"
                    ));
                }
            }

        } catch (SQLException e) {
            ultimoError = e.getMessage() == null
                    ? "No fue posible consultar la tabla precios."
                    : e.getMessage();

            System.out.println(
                    "[ControlGestionarPreciosCINEX] Error al listar precios: "
                            + ultimoError
            );
        }

        return lista;
    }

    public boolean actualizarPrecio(int idPrecio, double monto) {
        ultimoError = "";

        if (idPrecio <= 0 || monto <= 0) {
            ultimoError = "El identificador y el monto deben ser válidos.";
            return false;
        }

        String sql = "UPDATE precios SET monto = ? WHERE id_precio = ?";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setQueryTimeout(15);
            ps.setDouble(1, monto);
            ps.setInt(2, idPrecio);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            ultimoError = e.getMessage() == null
                    ? "No fue posible actualizar el precio."
                    : e.getMessage();

            System.out.println(
                    "[ControlGestionarPreciosCINEX] Error al actualizar precio: "
                            + ultimoError
            );
            return false;
        }
    }

    /* Compatibilidad con llamadas antiguas: el estado ya no se modifica. */
    public boolean actualizarPrecio(int idPrecio, double monto, String estadoIgnorado) {
        return actualizarPrecio(idPrecio, monto);
    }

    public boolean montoValido(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return false;
        }

        try {
            double monto = Double.parseDouble(
                    texto.trim().replace(",", ".")
            );
            return monto > 0 && monto <= 9999.99;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public double convertirMonto(String texto) {
        return Double.parseDouble(
                texto.trim().replace(",", ".")
        );
    }

    public String getUltimoError() {
        return ultimoError;
    }
}
