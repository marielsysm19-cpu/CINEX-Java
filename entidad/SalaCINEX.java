package entidad;

public class SalaCINEX {

    private int idSala;
    private String nombre;
    private String tipo;
    private int capacidad;
    private int filas;
    private int columnas;
    private String estado = "Activo";

    public SalaCINEX() {
    }

    public SalaCINEX(int idSala, String nombre, String tipo, int capacidad) {
        this(idSala, nombre, tipo, capacidad, "Activo");
    }

    public SalaCINEX(int idSala, String nombre, String tipo, int capacidad, String estado) {
        this.idSala = idSala;
        this.nombre = valorSeguro(nombre);
        this.tipo = valorSeguro(tipo);
        setCapacidad(capacidad);
        setEstado(estado);
    }

    public int getIdSala() {
        return idSala;
    }

    public void setIdSala(int idSala) {
        this.idSala = idSala;
    }

    public String getNombre() {
        return nombre == null || nombre.trim().isEmpty() ? "Sala" : nombre.trim();
    }

    public void setNombre(String nombre) {
        this.nombre = valorSeguro(nombre);
    }

    public String getTipo() {
        return valorSeguro(tipo);
    }

    public void setTipo(String tipo) {
        this.tipo = valorSeguro(tipo);
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = Math.max(0, capacidad);
        calcularDisenoPorCapacidad();
    }

    public int getFilas() {
        return filas;
    }

    public void setFilas(int filas) {
        this.filas = Math.max(0, filas);
    }

    public int getColumnas() {
        return columnas;
    }

    public void setColumnas(int columnas) {
        this.columnas = Math.max(0, columnas);
    }

    public String getEstado() {
        String valor = valorSeguro(estado);
        return valor.isEmpty() ? "Activo" : valor;
    }

    public void setEstado(String estado) {
        String valor = valorSeguro(estado);
        this.estado = valor.isEmpty() ? "Activo" : valor;
    }

    public boolean estaActiva() {
        return "Activo".equalsIgnoreCase(getEstado());
    }

    public String getDescripcion() {
        if (getTipo().isEmpty()) {
            return getNombre();
        }
        return getNombre() + " - " + getTipo();
    }

    private void calcularDisenoPorCapacidad() {
        if (capacidad <= 0) {
            filas = 0;
            columnas = 0;
        } else if (capacidad <= 80) {
            filas = 8;
            columnas = 10;
        } else if (capacidad <= 100) {
            filas = 10;
            columnas = 10;
        } else if (capacidad <= 120) {
            filas = 10;
            columnas = 12;
        } else {
            columnas = 12;
            filas = (int) Math.ceil(capacidad / 12.0);
        }
    }

    private static String valorSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
