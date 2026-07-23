package entidad;

public class PagoCINEX {

    private int idPago;
    private String metodoPago;
    private String estado;
    private double total;

    public PagoCINEX() {
    }

    public PagoCINEX(int idPago, String metodoPago, String estado) {
        this(idPago, metodoPago, estado, 0.0);
    }

    public PagoCINEX(int idPago, String metodoPago, String estado, double total) {
        this.idPago = idPago;
        this.metodoPago = valorSeguro(metodoPago);
        this.estado = valorSeguro(estado);
        this.total = total;
    }

    public int getIdPago() { return idPago; }
    public void setIdPago(int idPago) { this.idPago = idPago; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = valorSeguro(metodoPago); }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = valorSeguro(estado); }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public boolean estaPagado() {
        return "Pagado".equalsIgnoreCase(estado) || "Aprobado".equalsIgnoreCase(estado);
    }

    private static String valorSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
