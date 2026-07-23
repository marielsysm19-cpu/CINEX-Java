package control;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ControlGestionarFuncionesCINEX {

    public ArrayList<Object[]> listarFunciones() {
        ArrayList<Object[]> lista = new ArrayList<>();

        String sql = "SELECT f.id_funcion, p.titulo, "
                + "s.nombre AS sala, s.tipo AS tipo_sala, "
                + "DATE_FORMAT(f.fecha, '%d/%m/%Y') AS fecha, "
                + "TIME_FORMAT(f.hora, '%H:%i') AS hora, "
                + "f.estado, p.duracion "
                + "FROM funciones f "
                + "INNER JOIN peliculas p "
                + "ON f.id_pelicula = p.id_pelicula "
                + "INNER JOIN salas s "
                + "ON f.id_sala = s.id_sala "
                + "ORDER BY f.fecha DESC, f.hora DESC";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String nombreSala = rs.getString("sala");
                String tipoSala = rs.getString("tipo_sala");

                String salaCompleta = nombreSala == null
                        ? "Sala"
                        : nombreSala.trim();

                if (tipoSala != null
                        && !tipoSala.trim().isEmpty()) {
                    salaCompleta += " - " + tipoSala.trim();
                }

                lista.add(new Object[]{
                        rs.getInt("id_funcion"),
                        rs.getString("titulo"),
                        salaCompleta,
                        rs.getString("fecha"),
                        rs.getString("hora"),
                        rs.getString("estado"),
                        rs.getInt("duracion")
                });
            }

        } catch (SQLException e) {
            System.out.println("[ControlGestionarFuncionesCINEX] Error al listar funciones: " + e.getMessage());
        }

        return lista;
    }

    public boolean cancelarFuncion(int idFuncion) {
        String sql = "UPDATE funciones SET estado = 'Cancelada' WHERE id_funcion = ?";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idFuncion);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[ControlGestionarFuncionesCINEX] Error al cancelar función: " + e.getMessage());
            return false;
        }
    }

    public boolean hayFuncionSeleccionada(int filaSeleccionada) {
        return filaSeleccionada >= 0;
    }
}
