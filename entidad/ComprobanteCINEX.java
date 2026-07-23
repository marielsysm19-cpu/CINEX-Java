package entidad;

import java.util.ArrayList;
import java.util.List;

public class ComprobanteCINEX {

    private int idComprobante;
    private String numeroComprobante;
    private String numeroVenta;
    private String fechaEmision;
    private String usuario;
    private String pelicula;
    private String funcion;
    private String sala;
    private ClienteCINEX cliente;
    private PagoCINEX pago;
    private List<EntradaCINEX> entradas = new ArrayList<>();
    private String rutaQR;
    private boolean qrGenerado;
    private boolean registrado;

    public ComprobanteCINEX() {
    }

    public int getIdComprobante() { return idComprobante; }
    public void setIdComprobante(int idComprobante) { this.idComprobante = idComprobante; }

    public String getNumeroComprobante() { return numeroComprobante; }
    public void setNumeroComprobante(String numeroComprobante) { this.numeroComprobante = valorSeguro(numeroComprobante); }

    public String getNumeroVenta() { return numeroVenta; }
    public void setNumeroVenta(String numeroVenta) { this.numeroVenta = valorSeguro(numeroVenta); }

    public String getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(String fechaEmision) { this.fechaEmision = valorSeguro(fechaEmision); }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = valorSeguro(usuario); }

    public String getPelicula() { return pelicula; }
    public void setPelicula(String pelicula) { this.pelicula = valorSeguro(pelicula); }

    public String getFuncion() { return funcion; }
    public void setFuncion(String funcion) { this.funcion = valorSeguro(funcion); }

    public String getSala() { return sala; }
    public void setSala(String sala) { this.sala = valorSeguro(sala); }

    public ClienteCINEX getCliente() { return cliente; }
    public void setCliente(ClienteCINEX cliente) { this.cliente = cliente; }

    public PagoCINEX getPago() { return pago; }
    public void setPago(PagoCINEX pago) { this.pago = pago; }

    public List<EntradaCINEX> getEntradas() { return entradas; }
    public void setEntradas(List<EntradaCINEX> entradas) {
        this.entradas = entradas == null ? new ArrayList<>() : new ArrayList<>(entradas);
    }

    public String getRutaQR() { return rutaQR; }
    public void setRutaQR(String rutaQR) { this.rutaQR = valorSeguro(rutaQR); }

    public boolean isQrGenerado() { return qrGenerado; }
    public void setQrGenerado(boolean qrGenerado) { this.qrGenerado = qrGenerado; }

    public boolean isRegistrado() { return registrado; }
    public void setRegistrado(boolean registrado) { this.registrado = registrado; }

    public double getMontoPagado() {
        return pago == null ? 0.0 : pago.getTotal();
    }

    public String getMetodoPago() {
        return pago == null ? "" : pago.getMetodoPago();
    }

    public ArrayList<String> obtenerCodigosAsientos() {
        ArrayList<String> codigos = new ArrayList<>();

        for (EntradaCINEX entrada : entradas) {
            if (entrada == null || entrada.getAsientos() == null) {
                continue;
            }

            String[] partes = entrada.getAsientos().split(",");
            for (String parte : partes) {
                String asiento = parte.trim();
                if (!asiento.isEmpty()) {
                    codigos.add(asiento);
                }
            }
        }

        return codigos;
    }

    public ArrayList<String> obtenerTiposEntrada() {
        ArrayList<String> tipos = new ArrayList<>();

        for (EntradaCINEX entrada : entradas) {
            if (entrada == null) {
                continue;
            }

            String tipo = entrada.getTipoEntrada();
            tipos.add(tipo == null || tipo.trim().isEmpty() ? "Entrada General" : tipo.trim());
        }

        return tipos;
    }

    public ArrayList<Double> obtenerPreciosUnitarios() {
        ArrayList<Double> precios = new ArrayList<>();

        for (EntradaCINEX entrada : entradas) {
            if (entrada == null) {
                continue;
            }

            precios.add(entrada.getPrecioUnitario());
        }

        return precios;
    }

    private static String valorSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
