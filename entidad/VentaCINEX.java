package entidad;

import java.time.LocalDateTime;

public class VentaCINEX {

    private int idVenta;
    private String numeroVenta;
    private LocalDateTime fechaHora;
    private String pelicula;
    private String sala;
    private String metodoPago;
    private double total;
    private int entradasVendidas;
    private String asientos;
    private String vendedor;

    private double montoReembolsado;
    private int entradasReembolsadas;
    private String asientosReembolsados;
    private String estadoReembolso;

    public VentaCINEX() {
    }

    public VentaCINEX(
            int idVenta,
            String numeroVenta,
            LocalDateTime fechaHora,
            String pelicula,
            String sala,
            String metodoPago,
            double total,
            int entradasVendidas
    ) {
        this.idVenta = idVenta;
        this.numeroVenta = valorSeguro(numeroVenta);
        this.fechaHora = fechaHora;
        this.pelicula = valorSeguro(pelicula);
        this.sala = valorSeguro(sala);
        this.metodoPago = valorSeguro(metodoPago);
        this.total = Math.max(0.0, total);
        this.entradasVendidas = Math.max(0, entradasVendidas);
        this.asientosReembolsados = "";
        this.estadoReembolso = "Sin reembolso";
    }

    public int getIdVenta() { return idVenta; }
    public void setIdVenta(int idVenta) { this.idVenta = idVenta; }

    public String getNumeroVenta() { return valorSeguro(numeroVenta); }
    public void setNumeroVenta(String numeroVenta) { this.numeroVenta = valorSeguro(numeroVenta); }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public String getPelicula() { return valorSeguro(pelicula); }
    public void setPelicula(String pelicula) { this.pelicula = valorSeguro(pelicula); }

    public String getSala() { return valorSeguro(sala); }
    public void setSala(String sala) { this.sala = valorSeguro(sala); }

    public String getMetodoPago() { return valorSeguro(metodoPago); }
    public void setMetodoPago(String metodoPago) { this.metodoPago = valorSeguro(metodoPago); }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = Math.max(0.0, total); }

    public int getEntradasVendidas() { return entradasVendidas; }
    public void setEntradasVendidas(int entradasVendidas) {
        this.entradasVendidas = Math.max(0, entradasVendidas);
    }

    public String getAsientos() { return valorSeguro(asientos); }
    public void setAsientos(String asientos) { this.asientos = valorSeguro(asientos); }

    public String getVendedor() { return valorSeguro(vendedor); }
    public void setVendedor(String vendedor) { this.vendedor = valorSeguro(vendedor); }

    public double getMontoReembolsado() { return montoReembolsado; }
    public void setMontoReembolsado(double montoReembolsado) {
        this.montoReembolsado = Math.max(0.0, montoReembolsado);
    }

    public double getTotalNeto() {
        return Math.max(0.0, total - montoReembolsado);
    }

    public int getEntradasReembolsadas() { return entradasReembolsadas; }
    public void setEntradasReembolsadas(int entradasReembolsadas) {
        this.entradasReembolsadas = Math.max(0, entradasReembolsadas);
    }

    public int getEntradasVigentes() {
        return Math.max(0, entradasVendidas - entradasReembolsadas);
    }

    public String getAsientosReembolsados() {
        return valorSeguro(asientosReembolsados);
    }

    public void setAsientosReembolsados(String asientosReembolsados) {
        this.asientosReembolsados = valorSeguro(asientosReembolsados);
    }

    public String getEstadoReembolso() {
        String valor = valorSeguro(estadoReembolso);
        return valor.isEmpty() ? "Sin reembolso" : valor;
    }

    public void setEstadoReembolso(String estadoReembolso) {
        this.estadoReembolso = valorSeguro(estadoReembolso);
    }

    private static String valorSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
