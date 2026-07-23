package entidad;

public class UsuarioCINEX {

    private int idUsuario;
    private String nombre;
    private String usuario;
    private String contrasena;
    private String rol;
    private String estado;
    private boolean debeCambiarContrasena;

    public UsuarioCINEX() {
    }

    public UsuarioCINEX(int idUsuario, String usuario, String rol, String estado) {
        this.idUsuario = idUsuario;
        this.usuario = usuario;
        this.rol = rol;
        this.estado = estado;
    }

    public UsuarioCINEX(int idUsuario, String nombre, String usuario, String rol, String estado) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.usuario = usuario;
        this.rol = rol;
        this.estado = estado;
    }

    public UsuarioCINEX(String usuario, String contrasena, String rol, String estado) {
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.rol = rol;
        this.estado = estado;
    }

    public UsuarioCINEX(String nombre, String usuario, String contrasena, String rol, String estado) {
        this.nombre = nombre;
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.rol = rol;
        this.estado = estado;
    }

    public UsuarioCINEX(int idUsuario, String nombre, String usuario, String contrasena, String rol, String estado) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.rol = rol;
        this.estado = estado;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre == null ? "" : nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUsuario() {
        return usuario == null ? "" : usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getContrasena() {
        return contrasena == null ? "" : contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getRol() {
        return rol == null ? "" : rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getEstado() {
        return estado == null ? "" : estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }


    public boolean debeCambiarContrasena() {
        return debeCambiarContrasena;
    }

    public boolean isDebeCambiarContrasena() {
        return debeCambiarContrasena;
    }

    public void setDebeCambiarContrasena(boolean debeCambiarContrasena) {
        this.debeCambiarContrasena = debeCambiarContrasena;
    }

    public boolean estaActivo() {
        return "Activo".equalsIgnoreCase(getEstado());
    }

    public boolean esAdministrador() {
        return "admin".equalsIgnoreCase(getRol()) || "administrador".equalsIgnoreCase(getRol());
    }

    public boolean esGerente() {
        return "gerente".equalsIgnoreCase(getRol());
    }

    public boolean esTaquillero() {
        return "taquillero".equalsIgnoreCase(getRol());
    }

    public boolean datosLoginCompletos() {
        return !getUsuario().trim().isEmpty() && !getContrasena().trim().isEmpty();
    }

    public boolean datosObligatoriosCompletos() {
        return !getNombre().trim().isEmpty()
                && !getUsuario().trim().isEmpty()
                && !getRol().trim().isEmpty()
                && !getEstado().trim().isEmpty();
    }

    @Override
    public String toString() {
        return getNombre().trim().isEmpty()
                ? getUsuario() + " - " + getRol()
                : getNombre() + " (" + getUsuario() + ")";
    }
}
