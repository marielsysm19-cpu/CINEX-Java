package entidad;

public class EntradaCINEX {

    private int idEntrada;
    private String numeroVenta;
    private String fechaCompra;
    private String fechaCompraCorta;
    private String pelicula;
    private String funcion;
    private String sala;
    private String asientos;
    private int cantidadEntradas;
    private String tipoEntrada = "Entrada General";
    private double precioUnitario;
    private String estado;
    private PagoCINEX pago;
    private int entradasReembolsadas;
    private String asientosReembolsados = "";
    private double montoReembolsado;
    private String estadoReembolso = "Sin reembolso";

    public EntradaCINEX() {
    }

    public EntradaCINEX(int idEntrada, String asientos, int cantidadEntradas, String estado) {
        this.idEntrada = idEntrada;
        this.asientos = valorSeguro(asientos);
        this.cantidadEntradas = cantidadEntradas;
        this.estado = valorSeguro(estado);
    }

    public EntradaCINEX(int idEntrada, String numeroVenta, String fechaCompra, String fechaCompraCorta,
                       String pelicula, String funcion, String sala, String asientos,
                       int cantidadEntradas, String estado, PagoCINEX pago) {
        this.idEntrada = idEntrada;
        this.numeroVenta = valorSeguro(numeroVenta);
        this.fechaCompra = valorSeguro(fechaCompra);
        this.fechaCompraCorta = valorSeguro(fechaCompraCorta);
        this.pelicula = valorSeguro(pelicula);
        this.funcion = valorSeguro(funcion);
        this.sala = valorSeguro(sala);
        this.asientos = valorSeguro(asientos);
        this.cantidadEntradas = cantidadEntradas;
        this.estado = valorSeguro(estado);
        this.pago = pago;
    }

    public int getIdEntrada() { return idEntrada; }
    public void setIdEntrada(int idEntrada) { this.idEntrada = idEntrada; }

    public String getNumeroVenta() { return numeroVenta; }
    public void setNumeroVenta(String numeroVenta) { this.numeroVenta = valorSeguro(numeroVenta); }

    public String getFechaCompra() { return fechaCompra; }
    public void setFechaCompra(String fechaCompra) { this.fechaCompra = valorSeguro(fechaCompra); }

    public String getFechaCompraCorta() { return fechaCompraCorta; }
    public void setFechaCompraCorta(String fechaCompraCorta) { this.fechaCompraCorta = valorSeguro(fechaCompraCorta); }

    public String getPelicula() { return pelicula; }
    public void setPelicula(String pelicula) { this.pelicula = valorSeguro(pelicula); }

    public String getFuncion() { return funcion; }
    public void setFuncion(String funcion) { this.funcion = valorSeguro(funcion); }

    public String getSala() { return sala; }
    public void setSala(String sala) { this.sala = valorSeguro(sala); }

    public String getAsientos() { return asientos; }
    public void setAsientos(String asientos) { this.asientos = valorSeguro(asientos); }

    public int getCantidadEntradas() { return cantidadEntradas; }
    public void setCantidadEntradas(int cantidadEntradas) { this.cantidadEntradas = cantidadEntradas; }

    public String getTipoEntrada() { return tipoEntrada; }
    public void setTipoEntrada(String tipoEntrada) {
        String valor = valorSeguro(tipoEntrada);
        this.tipoEntrada = valor.isEmpty() ? "Entrada General" : valor;
    }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = Math.max(0, precioUnitario); }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = valorSeguro(estado); }

    public PagoCINEX getPago() { return pago; }
    public void setPago(PagoCINEX pago) { this.pago = pago; }

    public String getMetodoPago() { return pago == null ? "-" : pago.getMetodoPago(); }
    public double getTotal() { return pago == null ? 0.0 : pago.getTotal(); }

    public int getEntradasReembolsadas() { return entradasReembolsadas; }
    public void setEntradasReembolsadas(int entradasReembolsadas) {
        this.entradasReembolsadas = Math.max(0, entradasReembolsadas);
    }

    public String getAsientosReembolsados() {
        return valorSeguro(asientosReembolsados);
    }

    public void setAsientosReembolsados(String asientosReembolsados) {
        this.asientosReembolsados = valorSeguro(asientosReembolsados);
    }

    public double getMontoReembolsado() { return montoReembolsado; }
    public void setMontoReembolsado(double montoReembolsado) {
        this.montoReembolsado = Math.max(0.0, montoReembolsado);
    }

    public double getTotalNeto() {
        return Math.max(0.0, getTotal() - montoReembolsado);
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
