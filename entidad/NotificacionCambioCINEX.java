package entidad;

public class NotificacionCambioCINEX {

    private int idNotificacion;
    private String tipoElemento;
    private int idPelicula;
    private int idFuncion;
    private String tituloElemento;
    private String descripcion;
    private String datosAnteriores;
    private String datosNuevos;
    private String usuarioAdmin;
    private String fechaCambio;
    private String estadoReembolso;
    private String usuarioGerente;
    private String fechaDecision;

    public int getIdNotificacion() { return idNotificacion; }
    public void setIdNotificacion(int idNotificacion) { this.idNotificacion = idNotificacion; }

    public String getTipoElemento() { return seguro(tipoElemento); }
    public void setTipoElemento(String tipoElemento) { this.tipoElemento = seguro(tipoElemento); }

    public int getIdPelicula() { return idPelicula; }
    public void setIdPelicula(int idPelicula) { this.idPelicula = idPelicula; }

    public int getIdFuncion() { return idFuncion; }
    public void setIdFuncion(int idFuncion) { this.idFuncion = idFuncion; }

    public String getTituloElemento() { return seguro(tituloElemento); }
    public void setTituloElemento(String tituloElemento) { this.tituloElemento = seguro(tituloElemento); }

    public String getDescripcion() { return seguro(descripcion); }
    public void setDescripcion(String descripcion) { this.descripcion = seguro(descripcion); }

    public String getDatosAnteriores() { return seguro(datosAnteriores); }
    public void setDatosAnteriores(String datosAnteriores) { this.datosAnteriores = seguro(datosAnteriores); }

    public String getDatosNuevos() { return seguro(datosNuevos); }
    public void setDatosNuevos(String datosNuevos) { this.datosNuevos = seguro(datosNuevos); }

    public String getUsuarioAdmin() { return seguro(usuarioAdmin); }
    public void setUsuarioAdmin(String usuarioAdmin) { this.usuarioAdmin = seguro(usuarioAdmin); }

    public String getFechaCambio() { return seguro(fechaCambio); }
    public void setFechaCambio(String fechaCambio) { this.fechaCambio = seguro(fechaCambio); }

    public String getEstadoReembolso() { return seguro(estadoReembolso); }
    public void setEstadoReembolso(String estadoReembolso) { this.estadoReembolso = seguro(estadoReembolso); }

    public String getUsuarioGerente() { return seguro(usuarioGerente); }
    public void setUsuarioGerente(String usuarioGerente) { this.usuarioGerente = seguro(usuarioGerente); }

    public String getFechaDecision() { return seguro(fechaDecision); }
    public void setFechaDecision(String fechaDecision) { this.fechaDecision = seguro(fechaDecision); }

    private static String seguro(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
