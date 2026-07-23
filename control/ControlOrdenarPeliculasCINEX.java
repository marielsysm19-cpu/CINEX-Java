package control;

import entidad.PeliculaCINEX;
import java.util.ArrayList;
import java.util.Comparator;

public class ControlOrdenarPeliculasCINEX {

    public ArrayList<PeliculaCINEX> generarRankingPeliculas(ArrayList<PeliculaCINEX> peliculas) {
        return ordenarPeliculasMasVistas(peliculas);
    }

    public ArrayList<PeliculaCINEX> ordenarPeliculasMasVistas(ArrayList<PeliculaCINEX> peliculas) {
        ArrayList<PeliculaCINEX> ranking = peliculas == null ? new ArrayList<>() : new ArrayList<>(peliculas);
        ranking.sort(Comparator.comparingInt(PeliculaCINEX::getEntradasVendidas).reversed());
        return ranking;
    }

    public ArrayList<PeliculaCINEX> enviarRankingPeliculas(ArrayList<PeliculaCINEX> peliculas) {
        return peliculas == null ? new ArrayList<>() : peliculas;
    }
}
