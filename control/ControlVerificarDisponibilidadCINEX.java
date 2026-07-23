package control;

import entidad.AsientoCINEX;
import entidad.FuncionCINEX;
import entidad.ReferenciaFuncionCINEX;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class ControlVerificarDisponibilidadCINEX {

    public static Set<String> consultarAsientosOcupados(
            String pelicula,
            String referenciaFuncion
    ) {
        LinkedHashSet<String> ocupados = new LinkedHashSet<>();

        ArrayList<String> datos =
                BDCINEX.listarAsientosOcupados(
                        pelicula,
                        referenciaFuncion
                );

        if (datos == null) {
            return ocupados;
        }

        for (String asiento : datos) {
            if (asiento != null && !asiento.trim().isEmpty()) {
                ocupados.add(
                        asiento.trim().toUpperCase()
                );
            }
        }

        return ocupados;
    }

    public static int consultarCapacidadFuncion(
            String pelicula,
            String referenciaFuncion
    ) {
        int capacidad = BDCINEX.obtenerCapacidadSalaFuncion(
                pelicula,
                referenciaFuncion
        );

        return capacidad > 0 ? capacidad : 100;
    }

    public static boolean verificarDisponibilidad(
            String pelicula,
            String referenciaFuncion,
            String asiento
    ) {
        if (asiento == null || asiento.trim().isEmpty()) {
            return false;
        }

        String codigo = asiento.trim().toUpperCase();

        return !consultarAsientosOcupados(
                pelicula,
                referenciaFuncion
        ).contains(codigo);
    }

    public static boolean guardarSeleccionTemporal(
            String usuario,
            String pelicula,
            String referenciaFuncion,
            ArrayList<String> asientos
    ) {
        if (asientos == null || asientos.isEmpty()) {
            return false;
        }

        /*
         * La selección se conserva en memoria hasta el pago.
         * Antes de continuar se verifica nuevamente cada asiento
         * contra la función exacta identificada por su ID.
         */
        for (String asiento : asientos) {
            if (!verificarDisponibilidad(
                    pelicula,
                    referenciaFuncion,
                    asiento
            )) {
                return false;
            }
        }

        return true;
    }

    public static ArrayList<AsientoCINEX> consultarMapaAsientos(
            String pelicula,
            String referenciaFuncion
    ) {
        int capacidad = consultarCapacidadFuncion(
                pelicula,
                referenciaFuncion
        );

        Set<String> ocupados = consultarAsientosOcupados(
                pelicula,
                referenciaFuncion
        );

        ArrayList<AsientoCINEX> mapa = new ArrayList<>();

        int columnas;

        if (capacidad <= 100) {
            columnas = 10;
        } else if (capacidad <= 120) {
            columnas = 12;
        } else {
            columnas = 14;
        }

        int filas = (int) Math.ceil(
                capacidad / (double) columnas
        );

        int contador = 0;

        for (int f = 0; f < filas; f++) {
            String fila = String.valueOf((char) ('A' + f));

            for (int numero = 1; numero <= columnas; numero++) {
                contador++;

                if (contador > capacidad) {
                    break;
                }

                String codigo = fila + numero;

                mapa.add(
                        new AsientoCINEX(
                                codigo,
                                ocupados.contains(codigo)
                        )
                );
            }
        }

        return mapa;
    }

    public static FuncionCINEX consultarFuncionSeleccionada(
            String pelicula,
            String referenciaFuncion
    ) {
        int idFuncion =
                ReferenciaFuncionCINEX.obtenerId(
                        referenciaFuncion
                );

        if (idFuncion > 0) {
            FuncionCINEX funcion =
                    ControlConsultarFuncionesCINEX
                            .consultarDetalleFuncion(idFuncion);

            if (funcion != null) {
                return funcion;
            }
        }

        FuncionCINEX funcion = new FuncionCINEX();
        funcion.setPelicula(pelicula);
        funcion.setEstado("Activa");
        funcion.setCapacidad(
                consultarCapacidadFuncion(
                        pelicula,
                        referenciaFuncion
                )
        );
        funcion.setVendidos(
                consultarAsientosOcupados(
                        pelicula,
                        referenciaFuncion
                ).size()
        );

        return funcion;
    }
}
