package entidad;

public class AsientoCINEX {

    private int idAsiento;
    private String codigo;
    private String fila;
    private int numero;
    private String estado;
    private boolean ocupado;
    private boolean seleccionadoTemporalmente;

    public AsientoCINEX() {
    }

    public AsientoCINEX(String codigo, boolean ocupado) {
        setCodigo(codigo);
        this.ocupado = ocupado;
        this.estado = ocupado ? "Ocupado" : "Disponible";
    }

    public int getIdAsiento() {
        return idAsiento;
    }

    public void setIdAsiento(int idAsiento) {
        this.idAsiento = idAsiento;
    }

    public String getCodigo() {
        return codigo == null ? "" : codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo == null ? "" : codigo.trim().toUpperCase();
        extraerFilaNumeroDesdeCodigo();
    }

    public String getFila() {
        return fila == null ? "" : fila;
    }

    public void setFila(String fila) {
        this.fila = fila == null ? "" : fila.trim().toUpperCase();
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = Math.max(0, numero);
    }

    public String getEstado() {
        if (estado == null || estado.trim().isEmpty()) {
            return ocupado ? "Ocupado" : "Disponible";
        }
        return estado.trim();
    }

    public void setEstado(String estado) {
        this.estado = estado == null ? "" : estado.trim();
        this.ocupado = "Ocupado".equalsIgnoreCase(this.estado) || "Reservado".equalsIgnoreCase(this.estado);
    }

    public boolean isOcupado() {
        return ocupado;
    }

    public void setOcupado(boolean ocupado) {
        this.ocupado = ocupado;
        this.estado = ocupado ? "Ocupado" : "Disponible";
    }

    public boolean isSeleccionadoTemporalmente() {
        return seleccionadoTemporalmente;
    }

    public void setSeleccionadoTemporalmente(boolean seleccionadoTemporalmente) {
        this.seleccionadoTemporalmente = seleccionadoTemporalmente;
    }

    public boolean estaDisponible() {
        return !ocupado && !seleccionadoTemporalmente;
    }

    private void extraerFilaNumeroDesdeCodigo() {
        if (codigo == null || codigo.length() < 2) {
            fila = "";
            numero = 0;
            return;
        }

        fila = String.valueOf(codigo.charAt(0));

        try {
            numero = Integer.parseInt(codigo.substring(1));
        } catch (NumberFormatException e) {
            numero = 0;
        }
    }
}
