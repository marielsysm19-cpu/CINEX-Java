package control;

import entidad.PeliculaCINEX;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ControlConsultarPeliculasCINEX {

    public static ArrayList<PeliculaCINEX> solicitarPeliculasDisponibles() {
        return consultarPeliculasDisponibles();
    }

    private static ArrayList<PeliculaCINEX> consultarPeliculasDisponibles() {
        ArrayList<PeliculaCINEX> peliculas = new ArrayList<>();
        String sql = "SELECT id_pelicula, titulo, genero, duracion, clasificacion, imagen " +
                "FROM peliculas WHERE estado = 'Activa' AND en_cartelera = 1 ORDER BY id_pelicula DESC";
        try (Connection con = BDCINEX.conectar(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PeliculaCINEX p = new PeliculaCINEX();
                p.setIdPelicula(rs.getInt("id_pelicula"));
                p.setTitulo(rs.getString("titulo"));
                p.setGenero(rs.getString("genero"));
                try { p.setDuracion(rs.getInt("duracion")); } catch (Exception ignored) {}
                try { p.setDuracionMinutos(rs.getInt("duracion")); } catch (Exception ignored) {}
                p.setClasificacion(rs.getString("clasificacion"));
                p.setImagen(rs.getString("imagen"));
                peliculas.add(p);
            }
        } catch (SQLException e) {
            System.out.println("[PELÍCULAS CINEX] " + e.getMessage());
        }
        return peliculas;
    }
}
