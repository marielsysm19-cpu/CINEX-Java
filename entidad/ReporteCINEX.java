package entidad;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReporteCINEX {

    private String tipoReporte;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private double totalIngresos;
    private int entradasVendidas;
    private int transacciones;
    private double ocupacionPromedio;
    private String mensaje;

    private double ventasBrutas;
    private double montoReembolsado;
    private int entradasReembolsadas;
    private int reembolsosParciales;
    private int reembolsosTotales;

    private final ArrayList<VentaCINEX> ventas = new ArrayList<>();
    private final LinkedHashMap<String, Double> grupos = new LinkedHashMap<>();

    public String getTipoReporte() { return valorSeguro(tipoReporte); }
    public void setTipoReporte(String tipoReporte) { this.tipoReporte = valorSeguro(tipoReporte); }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public double getTotalIngresos() { return totalIngresos; }
    public void setTotalIngresos(double totalIngresos) { this.totalIngresos = Math.max(0.0, totalIngresos); }

    public int getEntradasVendidas() { return entradasVendidas; }
    public void setEntradasVendidas(int entradasVendidas) { this.entradasVendidas = Math.max(0, entradasVendidas); }

    public int getTransacciones() { return transacciones; }
    public void setTransacciones(int transacciones) { this.transacciones = Math.max(0, transacciones); }

    public double getOcupacionPromedio() { return ocupacionPromedio; }
    public void setOcupacionPromedio(double ocupacionPromedio) { this.ocupacionPromedio = Math.max(0.0, ocupacionPromedio); }

    public String getMensaje() { return valorSeguro(mensaje); }
    public void setMensaje(String mensaje) { this.mensaje = valorSeguro(mensaje); }

    public double getVentasBrutas() { return ventasBrutas; }
    public void setVentasBrutas(double ventasBrutas) { this.ventasBrutas = Math.max(0.0, ventasBrutas); }

    public double getMontoReembolsado() { return montoReembolsado; }
    public void setMontoReembolsado(double montoReembolsado) { this.montoReembolsado = Math.max(0.0, montoReembolsado); }

    public double getVentasNetas() { return Math.max(0.0, ventasBrutas - montoReembolsado); }

    public int getEntradasReembolsadas() { return entradasReembolsadas; }
    public void setEntradasReembolsadas(int entradasReembolsadas) { this.entradasReembolsadas = Math.max(0, entradasReembolsadas); }

    public int getEntradasVigentes() { return Math.max(0, entradasVendidas - entradasReembolsadas); }

    public int getReembolsosParciales() { return reembolsosParciales; }
    public void setReembolsosParciales(int reembolsosParciales) { this.reembolsosParciales = Math.max(0, reembolsosParciales); }

    public int getReembolsosTotales() { return reembolsosTotales; }
    public void setReembolsosTotales(int reembolsosTotales) { this.reembolsosTotales = Math.max(0, reembolsosTotales); }

    public List<VentaCINEX> getVentas() { return new ArrayList<>(ventas); }
    public void setVentas(List<VentaCINEX> lista) {
        ventas.clear();
        if (lista != null) {
            ventas.addAll(lista);
        }
    }

    public void agregarVenta(VentaCINEX venta) {
        if (venta != null) {
            ventas.add(venta);
        }
    }

    public Map<String, Double> getGrupos() { return new LinkedHashMap<>(grupos); }
    public void agregarGrupo(String nombre, double valor) {
        grupos.put(valorSeguro(nombre), Math.max(0.0, valor));
    }

    public void limpiarGrupos() { grupos.clear(); }

    private static String valorSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
