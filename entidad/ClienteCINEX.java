package entidad;

public class ClienteCINEX {

    private int idCliente;
    private String tipoDocumento;
    private String numeroDocumento;
    private String dni;
    private String nombre;

    public ClienteCINEX() {
    }

    public ClienteCINEX(int idCliente, String dni, String nombre) {
        this.idCliente = idCliente;
        this.dni = valorSeguro(dni);
        this.numeroDocumento = this.dni;
        this.nombre = valorSeguro(nombre);
        this.tipoDocumento = obtenerTipoDesdeDocumento(this.numeroDocumento);
    }

    public ClienteCINEX(String tipoDocumento, String numeroDocumento, String nombre) {
        this.tipoDocumento = valorSeguro(tipoDocumento);
        this.numeroDocumento = valorSeguro(numeroDocumento);
        this.dni = this.numeroDocumento;
        this.nombre = valorSeguro(nombre);
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getTipoDocumento() {
        if (tipoDocumento == null || tipoDocumento.trim().isEmpty()) {
            return obtenerTipoDesdeDocumento(getNumeroDocumento());
        }
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = valorSeguro(tipoDocumento);
    }

    public String getNumeroDocumento() {
        if (numeroDocumento == null || numeroDocumento.trim().isEmpty()) {
            return valorSeguro(dni);
        }
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = valorSeguro(numeroDocumento);
        this.dni = this.numeroDocumento;
        this.tipoDocumento = obtenerTipoDesdeDocumento(this.numeroDocumento);
    }

    public String getDni() {
        if (dni == null || dni.trim().isEmpty()) {
            return valorSeguro(numeroDocumento);
        }
        return dni;
    }

    public void setDni(String dni) {
        this.dni = valorSeguro(dni);
        this.numeroDocumento = this.dni;
        this.tipoDocumento = obtenerTipoDesdeDocumento(this.dni);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = valorSeguro(nombre);
    }

    // Compatibilidad: no se muestran ni se guardan en BD.
    public String getCorreo() {
        return "";
    }

    public void setCorreo(String correo) {
        // Eliminado del CU-4. Se conserva para evitar errores en archivos antiguos.
    }

    public String getTelefono() {
        return "";
    }

    public void setTelefono(String telefono) {
        // Eliminado del CU-4. Se conserva para evitar errores en archivos antiguos.
    }

    private static String obtenerTipoDesdeDocumento(String numeroDocumento) {
        if (numeroDocumento == null) {
            return "DNI";
        }
        return numeroDocumento.trim().length() == 9 ? "C.E." : "DNI";
    }

    private static String valorSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
