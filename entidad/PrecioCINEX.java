package entidad;

public class PrecioCINEX {

    private int idPrecio;
    private String tipoEntrada;
    private double monto;
    private String estado;

    public PrecioCINEX() {
    }

    public PrecioCINEX(int idPrecio, String tipoEntrada, double monto, String estado) {
        this.idPrecio = idPrecio;
        this.tipoEntrada = valorSeguro(tipoEntrada);
        this.monto = Math.max(0, monto);
        this.estado = valorSeguro(estado);
    }

    public int getIdPrecio() { return idPrecio; }
    public void setIdPrecio(int idPrecio) { this.idPrecio = idPrecio; }

    public String getTipoEntrada() { return tipoEntrada; }
    public void setTipoEntrada(String tipoEntrada) { this.tipoEntrada = valorSeguro(tipoEntrada); }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = Math.max(0, monto); }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = valorSeguro(estado); }

    public boolean estaActivo() {
        return estado == null || estado.trim().isEmpty() || "Activo".equalsIgnoreCase(estado.trim());
    }

    @Override
    public String toString() {
        return tipoEntrada + " - S/ " + String.format("%.2f", monto);
    }

    private static String valorSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
