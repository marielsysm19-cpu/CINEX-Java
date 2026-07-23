package entidad;

public class EntradaReembolsoCINEX {

    private int idEntrada;
    private int idVenta;
    private String numeroVenta;
    private String cliente;
    private String documento;
    private int idFuncion;
    private String pelicula;
    private String funcion;
    private String sala;
    private String asiento;
    private String tipoEntrada;
    private double precioOriginal;
    private String estadoEntrada;
    private String metodoPagoOriginal;
    private double totalVenta;
    private String fechaVenta;

    public int getIdEntrada() { return idEntrada; }
    public void setIdEntrada(int idEntrada) { this.idEntrada = idEntrada; }

    public int getIdVenta() { return idVenta; }
    public void setIdVenta(int idVenta) { this.idVenta = idVenta; }

    public String getNumeroVenta() { return seguro(numeroVenta); }
    public void setNumeroVenta(String numeroVenta) { this.numeroVenta = seguro(numeroVenta); }

    public String getCliente() { return seguro(cliente); }
    public void setCliente(String cliente) { this.cliente = seguro(cliente); }

    public String getDocumento() { return seguro(documento); }
    public void setDocumento(String documento) { this.documento = seguro(documento); }

    public int getIdFuncion() { return idFuncion; }
    public void setIdFuncion(int idFuncion) { this.idFuncion = idFuncion; }

    public String getPelicula() { return seguro(pelicula); }
    public void setPelicula(String pelicula) { this.pelicula = seguro(pelicula); }

    public String getFuncion() { return seguro(funcion); }
    public void setFuncion(String funcion) { this.funcion = seguro(funcion); }

    public String getSala() { return seguro(sala); }
    public void setSala(String sala) { this.sala = seguro(sala); }

    public String getAsiento() { return seguro(asiento); }
    public void setAsiento(String asiento) { this.asiento = seguro(asiento); }

    public String getTipoEntrada() { return seguro(tipoEntrada); }
    public void setTipoEntrada(String tipoEntrada) { this.tipoEntrada = seguro(tipoEntrada); }

    public double getPrecioOriginal() { return precioOriginal; }
    public void setPrecioOriginal(double precioOriginal) { this.precioOriginal = Math.max(0.0, precioOriginal); }

    public String getEstadoEntrada() { return seguro(estadoEntrada); }
    public void setEstadoEntrada(String estadoEntrada) { this.estadoEntrada = seguro(estadoEntrada); }

    public String getMetodoPagoOriginal() { return seguro(metodoPagoOriginal); }
    public void setMetodoPagoOriginal(String metodoPagoOriginal) { this.metodoPagoOriginal = seguro(metodoPagoOriginal); }

    public double getTotalVenta() { return totalVenta; }
    public void setTotalVenta(double totalVenta) { this.totalVenta = Math.max(0.0, totalVenta); }

    public String getFechaVenta() { return seguro(fechaVenta); }
    public void setFechaVenta(String fechaVenta) { this.fechaVenta = seguro(fechaVenta); }

    public boolean estaReembolsada() {
        return "Reembolsada".equalsIgnoreCase(estadoEntrada);
    }

    private static String seguro(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
