package control;

import entidad.FuncionCINEX;
import entidad.SalaCINEX;

/**
 * Control exclusivo para consultar la sala y construir su plano.
 * La consulta de películas y funciones pertenece a
 * ControlConsultarFuncionesCINEX.
 */
public class ControlConsultarPlanoAsientosCINEX {

    public static SalaCINEX obtenerPlanoSala(FuncionCINEX funcion) {
        SalaCINEX sala = obtenerSalaAsociada(funcion);
        return obtenerDisenoSala(sala);
    }

    public static SalaCINEX obtenerSalaAsociada(FuncionCINEX funcion) {
        if (funcion == null) {
            return new SalaCINEX(0, "Sala", "", 100);
        }

        int idFuncion = funcion.getIdFuncion();

        String nombreSala = idFuncion > 0
                ? BDCINEX.obtenerNombreSalaFuncion(idFuncion)
                : "";

        if (nombreSala == null || nombreSala.trim().isEmpty()) {
            nombreSala = funcion.getSala();
        }

        int capacidad = idFuncion > 0
                ? BDCINEX.obtenerCapacidadSalaFuncion(idFuncion)
                : 0;

        if (capacidad <= 0) {
            capacidad = funcion.getCapacidad();
        }

        if (capacidad <= 0) {
            capacidad = capacidadPorDefecto(nombreSala);
        }

        String tipo = funcion.getTipoSala();
        SalaCINEX sala = new SalaCINEX(
                funcion.getIdSala(),
                nombreSala,
                tipo,
                capacidad
        );

        funcion.setSalaEntidad(sala);
        return sala;
    }

    public static SalaCINEX obtenerDisenoSala(SalaCINEX sala) {
        if (sala == null) {
            return new SalaCINEX(0, "Sala", "", 100);
        }

        if (sala.getCapacidad() <= 0) {
            sala.setCapacidad(
                    capacidadPorDefecto(sala.getNombre())
            );
        }

        return sala;
    }

    private static int capacidadPorDefecto(String sala) {
        String texto = sala == null ? "" : sala.toLowerCase();

        if (texto.contains("1")) {
            return 80;
        }

        if (texto.contains("2")) {
            return 100;
        }

        if (texto.contains("3")) {
            return 120;
        }

        return 100;
    }
}
