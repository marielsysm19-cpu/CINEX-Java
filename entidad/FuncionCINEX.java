package entidad;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FuncionCINEX {

    private int idFuncion;
    private int idPelicula;
    private int idSala;
    private String pelicula;
    private PeliculaCINEX peliculaEntidad;
    private String genero;
    private int duracionMinutos;
    private String clasificacion;
    private String imagen;
    private String sala;
    private SalaCINEX salaEntidad;
    private String tipoSala;
    private LocalDate fecha;
    private LocalTime hora;
    private String fechaTexto;
    private String horaBD;
    private String estado;
    private int capacidad;
    private int vendidos;
    private int disponibles;

    public FuncionCINEX() {
    }

    public FuncionCINEX(int idFuncion, int idPelicula, int idSala, Object fecha, Object hora) {
        this.idFuncion = idFuncion;
        this.idPelicula = idPelicula;
        this.idSala = idSala;
        setFechaDesdeObjeto(fecha);
        setHoraDesdeObjeto(hora);
    }

    public FuncionCINEX(int idFuncion, int idPelicula, int idSala, java.time.LocalDate fecha, java.time.LocalTime hora) {
        this(idFuncion, idPelicula, idSala, (Object) fecha, (Object) hora);
    }

    public FuncionCINEX(int idFuncion, int idPelicula, int idSala, java.time.LocalDate fecha, String hora) {
        this(idFuncion, idPelicula, idSala, (Object) fecha, (Object) hora);
    }

    public FuncionCINEX(int idFuncion, int idPelicula, int idSala, java.sql.Date fecha, String hora) {
        this(idFuncion, idPelicula, idSala, (Object) fecha, (Object) hora);
    }

    public FuncionCINEX(int idFuncion, int idPelicula, int idSala, String fecha, String hora) {
        this(idFuncion, idPelicula, idSala, (Object) fecha, (Object) hora);
    }

    public int getIdFuncion() {
        return idFuncion;
    }

    public void setIdFuncion(int idFuncion) {
        this.idFuncion = idFuncion;
    }


    public int getIdPelicula() {
        return idPelicula;
    }

    public void setIdPelicula(int idPelicula) {
        this.idPelicula = idPelicula;
    }

    public int getIdSala() {
        return idSala;
    }

    public void setIdSala(int idSala) {
        this.idSala = idSala;
    }

    public String getPelicula() {
        if (pelicula != null && !pelicula.trim().isEmpty()) {
            return pelicula.trim();
        }
        return peliculaEntidad == null ? "" : peliculaEntidad.getTitulo();
    }

    public String getPeliculaTitulo() {
        return getPelicula();
    }

    public void setPelicula(String pelicula) {
        this.pelicula = valorSeguro(pelicula);
    }

    public PeliculaCINEX getPeliculaEntidad() {
        return peliculaEntidad;
    }

    public void setPeliculaEntidad(PeliculaCINEX peliculaEntidad) {
        this.peliculaEntidad = peliculaEntidad;
        if (peliculaEntidad != null) {
            this.pelicula = peliculaEntidad.getTitulo();
        }
    }

    public String getGenero() {
        return valorSeguro(genero);
    }

    public void setGenero(String genero) {
        this.genero = valorSeguro(genero);
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(int duracionMinutos) {
        this.duracionMinutos = Math.max(0, duracionMinutos);
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

    public String getSala() {
        if (sala != null && !sala.trim().isEmpty()) {
            return sala.trim();
        }
        return salaEntidad == null ? "Sala" : salaEntidad.getNombre();
    }

    public void setSala(String sala) {
        this.sala = valorSeguro(sala);
    }

    public SalaCINEX getSalaEntidad() {
        return salaEntidad;
    }

    public void setSalaEntidad(SalaCINEX salaEntidad) {
        this.salaEntidad = salaEntidad;
        if (salaEntidad != null) {
            this.sala = salaEntidad.getNombre();
            this.tipoSala = salaEntidad.getTipo();
            if (salaEntidad.getCapacidad() > 0) {
                setCapacidad(salaEntidad.getCapacidad());
            }
        }
    }

    public String getTipoSala() {
        if (tipoSala != null && !tipoSala.trim().isEmpty()) {
            return tipoSala.trim();
        }
        return salaEntidad == null ? "" : salaEntidad.getTipo();
    }

    public void setTipoSala(String tipoSala) {
        this.tipoSala = valorSeguro(tipoSala);
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public String getFechaTexto() {
        if (fechaTexto != null && !fechaTexto.trim().isEmpty()) {
            return fechaTexto.trim();
        }
        return fechaFormateada();
    }

    public void setFechaTexto(String fechaTexto) {
        this.fechaTexto = valorSeguro(fechaTexto);
    }

    public String getHoraBD() {
        if (horaBD != null && !horaBD.trim().isEmpty()) {
            return horaBD.trim();
        }
        return horaFormateada();
    }

    public void setHoraBD(String horaBD) {
        this.horaBD = valorSeguro(horaBD);
    }

    public String getEstado() {
        return valorSeguro(estado);
    }

    public void setEstado(String estado) {
        this.estado = valorSeguro(estado);
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = Math.max(0, capacidad);
        recalcularDisponibles();
    }

    public int getVendidos() {
        return vendidos;
    }

    public void setVendidos(int vendidos) {
        this.vendidos = Math.max(0, vendidos);
        recalcularDisponibles();
    }

    public int getDisponibles() {
        return disponibles;
    }

    public void setDisponibles(int disponibles) {
        this.disponibles = Math.max(0, disponibles);
    }

    private void recalcularDisponibles() {
        this.disponibles = Math.max(0, capacidad - vendidos);
    }

    public String fechaFormateada() {
        if (fecha == null) {
            return "-";
        }
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String horaFormateada() {
        if (hora == null) {
            return "-";
        }
        return hora.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US));
    }

    public String duracionFormateada() {
        if (duracionMinutos <= 0) {
            return "No especificada";
        }

        int horas = duracionMinutos / 60;
        int minutos = duracionMinutos % 60;

        if (horas > 0 && minutos > 0) {
            return horas + "h " + minutos + "m";
        }

        if (horas > 0) {
            return horas + "h";
        }

        return duracionMinutos + "m";
    }

    public String estadoDisponibilidad() {
        if (capacidad <= 0) {
            return "Disponible";
        }

        if (disponibles <= 0) {
            return "Sin disponibilidad";
        }

        double porcentaje = (disponibles * 100.0) / capacidad;
        if (porcentaje <= 30) {
            return "Alta demanda";
        }

        return "Disponible";
    }

    public String resumenCorto() {
        StringBuilder sb = new StringBuilder();

        if (!getHoraBD().isEmpty() && !"-".equals(getHoraBD())) {
            sb.append(getHoraBD());
        }

        if (!getSala().isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(getSala());
        }

        if (!getTipoSala().isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(getTipoSala());
        }

        if (!getFechaTexto().isEmpty() && !"-".equals(getFechaTexto())) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(getFechaTexto());
        }

        return sb.length() == 0 ? "Función" : sb.toString();
    }

    @Override
    public String toString() {
        return resumenCorto();
    }


    private void setFechaDesdeObjeto(Object valor) {
        if (valor == null) {
            this.fecha = null;
            this.fechaTexto = "";
            return;
        }

        if (valor instanceof LocalDate) {
            this.fecha = (LocalDate) valor;
            this.fechaTexto = this.fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return;
        }

        if (valor instanceof java.sql.Date) {
            this.fecha = ((java.sql.Date) valor).toLocalDate();
            this.fechaTexto = this.fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return;
        }

        String texto = valor.toString().trim();
        this.fechaTexto = texto;
        try {
            this.fecha = LocalDate.parse(texto);
        } catch (Exception e) {
            try {
                this.fecha = LocalDate.parse(texto, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception ignored) {
                this.fecha = null;
            }
        }
    }

    private void setHoraDesdeObjeto(Object valor) {
        if (valor == null) {
            this.hora = null;
            this.horaBD = "";
            return;
        }

        if (valor instanceof LocalTime) {
            this.hora = (LocalTime) valor;
            this.horaBD = this.hora.toString();
            return;
        }

        if (valor instanceof java.sql.Time) {
            this.hora = ((java.sql.Time) valor).toLocalTime();
            this.horaBD = this.hora.toString();
            return;
        }

        String texto = valor.toString().trim();
        this.horaBD = texto;
        try {
            this.hora = LocalTime.parse(texto.length() >= 5 ? texto.substring(0, 5) : texto);
        } catch (Exception ignored) {
            this.hora = null;
        }
    }

    private static String valorSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
