package entidad;

public class ReembolsoCINEX {

    private int idReembolso;
    private String numeroVenta;
    private String pelicula;
    private String cliente;
    private String documento;
    private String asientos;
    private int entradas;
    private double montoTotal;
    private String estado;
    private String usuarioTaquillero;
    private String fecha;

    public int getIdReembolso() { return idReembolso; }
    public void setIdReembolso(int idReembolso) { this.idReembolso = idReembolso; }

    public String getNumeroVenta() { return seguro(numeroVenta); }
    public void setNumeroVenta(String numeroVenta) { this.numeroVenta = seguro(numeroVenta); }

    public String getPelicula() { return seguro(pelicula); }
    public void setPelicula(String pelicula) { this.pelicula = seguro(pelicula); }

    public String getCliente() { return seguro(cliente); }
    public void setCliente(String cliente) { this.cliente = seguro(cliente); }

    public String getDocumento() { return seguro(documento); }
    public void setDocumento(String documento) { this.documento = seguro(documento); }

    public String getAsientos() { return seguro(asientos); }
    public void setAsientos(String asientos) { this.asientos = seguro(asientos); }

    public int getEntradas() { return entradas; }
    public void setEntradas(int entradas) { this.entradas = Math.max(0, entradas); }

    public double getMontoTotal() { return montoTotal; }
    public void setMontoTotal(double montoTotal) { this.montoTotal = Math.max(0.0, montoTotal); }

    public String getEstado() { return seguro(estado); }
    public void setEstado(String estado) { this.estado = seguro(estado); }

    public String getUsuarioTaquillero() { return seguro(usuarioTaquillero); }
    public void setUsuarioTaquillero(String usuarioTaquillero) { this.usuarioTaquillero = seguro(usuarioTaquillero); }

    public String getFecha() { return seguro(fecha); }
    public void setFecha(String fecha) { this.fecha = seguro(fecha); }

    private static String seguro(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
