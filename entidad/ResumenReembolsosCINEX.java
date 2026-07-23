package entidad;

public class ResumenReembolsosCINEX {

    private double ventasBrutas;
    private double montoReembolsado;
    private int entradasVendidas;
    private int entradasReembolsadas;
    private int reembolsosParciales;
    private int reembolsosTotales;
    private int notificacionesPendientes;

    public double getVentasBrutas() { return ventasBrutas; }
    public void setVentasBrutas(double ventasBrutas) { this.ventasBrutas = Math.max(0.0, ventasBrutas); }

    public double getMontoReembolsado() { return montoReembolsado; }
    public void setMontoReembolsado(double montoReembolsado) { this.montoReembolsado = Math.max(0.0, montoReembolsado); }

    public double getVentasNetas() { return Math.max(0.0, ventasBrutas - montoReembolsado); }

    public int getEntradasVendidas() { return entradasVendidas; }
    public void setEntradasVendidas(int entradasVendidas) { this.entradasVendidas = Math.max(0, entradasVendidas); }

    public int getEntradasReembolsadas() { return entradasReembolsadas; }
    public void setEntradasReembolsadas(int entradasReembolsadas) { this.entradasReembolsadas = Math.max(0, entradasReembolsadas); }

    public int getEntradasVigentes() { return Math.max(0, entradasVendidas - entradasReembolsadas); }

    public int getReembolsosParciales() { return reembolsosParciales; }
    public void setReembolsosParciales(int reembolsosParciales) { this.reembolsosParciales = Math.max(0, reembolsosParciales); }

    public int getReembolsosTotales() { return reembolsosTotales; }
    public void setReembolsosTotales(int reembolsosTotales) { this.reembolsosTotales = Math.max(0, reembolsosTotales); }

    public int getNotificacionesPendientes() { return notificacionesPendientes; }
    public void setNotificacionesPendientes(int notificacionesPendientes) { this.notificacionesPendientes = Math.max(0, notificacionesPendientes); }
}
