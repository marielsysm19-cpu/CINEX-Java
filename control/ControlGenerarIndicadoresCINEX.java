package control;

import entidad.ReporteCINEX;
import entidad.VentaCINEX;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class ControlGenerarIndicadoresCINEX {

    public ReporteCINEX generarIndicadores(
            ArrayList<VentaCINEX> ventas,
            LocalDate inicio,
            LocalDate fin
    ) {
        ReporteCINEX reporte = new ReporteCINEX();
        reporte.setTipoReporte("Reporte de ventas");
        reporte.setFechaInicio(inicio);
        reporte.setFechaFin(fin);
        reporte.setVentas(ventas);

        double ventasBrutas = sumarVentasBrutas(ventas);
        double reembolsos = sumarReembolsos(ventas);
        int entradasHistoricas = contarEntradasHistoricas(ventas);
        int entradasReembolsadas = contarEntradasReembolsadas(ventas);

        reporte.setVentasBrutas(ventasBrutas);
        reporte.setMontoReembolsado(reembolsos);
        reporte.setTotalIngresos(Math.max(0.0, ventasBrutas - reembolsos));
        reporte.setEntradasVendidas(entradasHistoricas);
        reporte.setEntradasReembolsadas(entradasReembolsadas);
        reporte.setTransacciones(ventas == null ? 0 : ventas.size());
        reporte.setOcupacionPromedio(
                calcularOcupacionReferencial(ventas)
        );

        int parciales = 0;
        int totales = 0;

        if (ventas != null) {
            for (VentaCINEX venta : ventas) {
                if ("Reembolso parcial".equalsIgnoreCase(
                        venta.getEstadoReembolso()
                )) {
                    parciales++;
                } else if ("Reembolso total".equalsIgnoreCase(
                        venta.getEstadoReembolso()
                )) {
                    totales++;
                }
            }
        }

        reporte.setReembolsosParciales(parciales);
        reporte.setReembolsosTotales(totales);
        reporte.setMensaje(
                reporte.getTransacciones() > 0
                        ? "El sistema muestra ventas brutas, reembolsos y ventas netas."
                        : "No existen registros para los filtros seleccionados."
        );

        LinkedHashMap<String, Double> grupos =
                agruparIngresosNetosPorPelicula(ventas);

        for (Map.Entry<String, Double> item : grupos.entrySet()) {
            reporte.agregarGrupo(
                    item.getKey(),
                    item.getValue()
            );
        }

        registrarConsulta(reporte);
        return enviarGraficosIndicadores(reporte);
    }

    public double sumarPagos(ArrayList<VentaCINEX> ventas) {
        return sumarVentasNetas(ventas);
    }

    public double sumarVentasBrutas(ArrayList<VentaCINEX> ventas) {
        double total = 0.0;

        if (ventas != null) {
            for (VentaCINEX venta : ventas) {
                total += venta.getTotal();
            }
        }

        return total;
    }

    public double sumarReembolsos(ArrayList<VentaCINEX> ventas) {
        double total = 0.0;

        if (ventas != null) {
            for (VentaCINEX venta : ventas) {
                total += venta.getMontoReembolsado();
            }
        }

        return total;
    }

    public double sumarVentasNetas(ArrayList<VentaCINEX> ventas) {
        return Math.max(
                0.0,
                sumarVentasBrutas(ventas) - sumarReembolsos(ventas)
        );
    }

    public int contarEntradas(ArrayList<VentaCINEX> ventas) {
        return contarEntradasVigentes(ventas);
    }

    public int contarEntradasHistoricas(ArrayList<VentaCINEX> ventas) {
        int total = 0;

        if (ventas != null) {
            for (VentaCINEX venta : ventas) {
                total += venta.getEntradasVendidas();
            }
        }

        return total;
    }

    public int contarEntradasReembolsadas(
            ArrayList<VentaCINEX> ventas
    ) {
        int total = 0;

        if (ventas != null) {
            for (VentaCINEX venta : ventas) {
                total += venta.getEntradasReembolsadas();
            }
        }

        return total;
    }

    public int contarEntradasVigentes(ArrayList<VentaCINEX> ventas) {
        return Math.max(
                0,
                contarEntradasHistoricas(ventas)
                        - contarEntradasReembolsadas(ventas)
        );
    }

    public void registrarConsulta(ReporteCINEX reporte) {
        // Operación interna del caso de uso. No altera la base de datos.
    }

    public ReporteCINEX enviarGraficosIndicadores(
            ReporteCINEX reporte
    ) {
        return reporte;
    }

    private double calcularOcupacionReferencial(
            ArrayList<VentaCINEX> ventas
    ) {
        if (ventas == null || ventas.isEmpty()) {
            return 0.0;
        }

        int entradasVigentes = contarEntradasVigentes(ventas);
        int capacidadReferencial = Math.max(1, ventas.size() * 10);

        return Math.min(
                100.0,
                (entradasVigentes * 100.0) / capacidadReferencial
        );
    }

    private LinkedHashMap<String, Double>
    agruparIngresosNetosPorPelicula(
            ArrayList<VentaCINEX> ventas
    ) {
        LinkedHashMap<String, Double> grupos =
                new LinkedHashMap<>();

        if (ventas != null) {
            for (VentaCINEX venta : ventas) {
                String pelicula = venta.getPelicula().isEmpty()
                        ? "Sin película"
                        : venta.getPelicula();

                grupos.put(
                        pelicula,
                        grupos.getOrDefault(pelicula, 0.0)
                                + venta.getTotalNeto()
                );
            }
        }

        return grupos;
    }
}
