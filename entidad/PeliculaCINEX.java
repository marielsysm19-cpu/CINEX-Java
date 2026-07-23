package entidad;

public class PeliculaCINEX {

    private int idPelicula;
    private String titulo;
    private String genero;
    private int duracion;
    private String clasificacion;
    private String imagen;
    private String estado;
    private int cartelera;

    // Campos usados por reportes/consultas.
    private int funcionesProgramadas;
    private int entradasVendidas;
    private double ingresosGenerados;

    public PeliculaCINEX() {
        this.estado = "Activa";
        this.cartelera = 1;
    }

    public PeliculaCINEX(int idPelicula, String titulo) {
        this();
        this.idPelicula = idPelicula;
        this.titulo = valorSeguro(titulo);
    }

    public PeliculaCINEX(int idPelicula, String titulo, String genero) {
        this(idPelicula, titulo);
        this.genero = valorSeguro(genero);
    }

    public PeliculaCINEX(int idPelicula, String titulo, String genero, int duracion, String clasificacion, String imagen) {
        this(idPelicula, titulo, genero);
        this.duracion = Math.max(0, duracion);
        this.clasificacion = valorSeguro(clasificacion);
        this.imagen = valorSeguro(imagen);
    }

    public PeliculaCINEX(String titulo, String genero, int duracion, String clasificacion, String imagen, String estado, int cartelera) {
        this();
        this.titulo = valorSeguro(titulo);
        this.genero = valorSeguro(genero);
        this.duracion = Math.max(0, duracion);
        this.clasificacion = valorSeguro(clasificacion);
        this.imagen = valorSeguro(imagen);
        this.estado = valorSeguro(estado).isEmpty() ? "Activa" : valorSeguro(estado);
        this.cartelera = normalizarCartelera(cartelera);
    }

    public PeliculaCINEX(int idPelicula, String titulo, String genero, int duracion, String clasificacion, String imagen, String estado, int cartelera) {
        this(titulo, genero, duracion, clasificacion, imagen, estado, cartelera);
        this.idPelicula = idPelicula;
    }

    public int getIdPelicula() {
        return idPelicula;
    }

    public void setIdPelicula(int idPelicula) {
        this.idPelicula = idPelicula;
    }

    public String getTitulo() {
        return valorSeguro(titulo);
    }

    public void setTitulo(String titulo) {
        this.titulo = valorSeguro(titulo);
    }

    public String getGenero() {
        return valorSeguro(genero);
    }

    public void setGenero(String genero) {
        this.genero = valorSeguro(genero);
    }

    public int getDuracion() {
        return duracion;
    }

    public void setDuracion(int duracion) {
        this.duracion = Math.max(0, duracion);
    }

    // Alias de compatibilidad usado por algunos controladores e interfaces.
    public int getDuracionMinutos() {
        return getDuracion();
    }

    public void setDuracionMinutos(int duracionMinutos) {
        setDuracion(duracionMinutos);
    }

    public String getClasificacion() {
        return valorSeguro(clasificacion);
    }

    public void setClasificacion(String clasificacion) {
        this.clasificacion = valorSeguro(clasificacion);
    }

    public String getImagen() {
        return valorSeguro(imagen);
    }

    public void setImagen(String imagen) {
        this.imagen = valorSeguro(imagen);
    }

    public String getEstado() {
        return valorSeguro(estado).isEmpty() ? "Activa" : valorSeguro(estado);
    }

    public void setEstado(String estado) {
        this.estado = valorSeguro(estado).isEmpty() ? "Activa" : valorSeguro(estado);
    }

    public int getCartelera() {
        return normalizarCartelera(cartelera);
    }

    public void setCartelera(int cartelera) {
        this.cartelera = normalizarCartelera(cartelera);
    }

    public int getEnCartelera() {
        return getCartelera();
    }

    public void setEnCartelera(int enCartelera) {
        setCartelera(enCartelera);
    }

    public int getFuncionesProgramadas() {
        return funcionesProgramadas;
    }

    public void setFuncionesProgramadas(int funcionesProgramadas) {
        this.funcionesProgramadas = Math.max(0, funcionesProgramadas);
    }

    public int getEntradasVendidas() {
        return entradasVendidas;
    }

    public void setEntradasVendidas(int entradasVendidas) {
        this.entradasVendidas = Math.max(0, entradasVendidas);
    }

    public double getIngresosGenerados() {
        return ingresosGenerados;
    }

    public void setIngresosGenerados(double ingresosGenerados) {
        this.ingresosGenerados = Math.max(0.0, ingresosGenerados);
    }

    public boolean datosObligatoriosCompletos() {
        return !getTitulo().isEmpty()
                && !getGenero().isEmpty()
                && duracion > 0
                && !getClasificacion().isEmpty()
                && !getEstado().isEmpty();
    }

    public String duracionFormateada() {
        return duracion <= 0 ? "" : duracion + " min";
    }

    @Override
    public String toString() {
        return getTitulo().isEmpty() ? "Película" : getTitulo();
    }

    private int normalizarCartelera(int valor) {
        return valor == 1 ? 1 : 0;
    }

    private static String valorSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
