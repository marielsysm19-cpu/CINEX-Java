package control;

import entidad.PeliculaCINEX;
import java.sql.*;
import java.util.ArrayList;

public class ControlProcesarConsultaCINEX {

    public PeliculaCINEX procesarConsulta(String tituloPelicula) {
        return consultarPelicula(tituloPelicula);
    }

    public PeliculaCINEX consultarPelicula(String tituloPelicula) {
        if (tituloPelicula == null || tituloPelicula.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT p.id_pelicula, p.titulo, COUNT(f.id_funcion) AS funciones " +
                "FROM peliculas p " +
                "LEFT JOIN funciones f ON f.id_pelicula = p.id_pelicula " +
                "WHERE p.titulo = ? " +
                "GROUP BY p.id_pelicula, p.titulo LIMIT 1";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, tituloPelicula.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PeliculaCINEX pelicula = new PeliculaCINEX(rs.getInt("id_pelicula"), rs.getString("titulo"));
                    pelicula.setFuncionesProgramadas(rs.getInt("funciones"));
                    return pelicula;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo procesar la consulta de película.", e);
        }

        return null;
    }

    public boolean existenFuncionesProgramadas(PeliculaCINEX pelicula) {
        return pelicula != null && pelicula.getFuncionesProgramadas() > 0;
    }

    public ArrayList<String> listarPeliculas() {
        ArrayList<String> lista = new ArrayList<>();
        String sql = "SELECT titulo FROM peliculas ORDER BY titulo ASC";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(rs.getString("titulo"));
            }

        } catch (SQLException e) {
            // Si falla, la interfaz mantiene el combo vacío.
        }

        return lista;
    }
}
