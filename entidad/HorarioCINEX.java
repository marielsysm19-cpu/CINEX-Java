package entidad;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class HorarioCINEX {
    private LocalDate fecha;
    private LocalTime horarioInicio;

    public HorarioCINEX() {
    }

    public HorarioCINEX(LocalDate fecha, LocalTime horarioInicio) {
        this.fecha = fecha;
        this.horarioInicio = horarioInicio;
    }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHorarioInicio() { return horarioInicio; }
    public void setHorarioInicio(LocalTime horarioInicio) { this.horarioInicio = horarioInicio; }

    public String getFechaSQL() {
        return fecha == null ? "" : fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public String getHorarioInicioSQL() {
        return horarioInicio == null ? "" : horarioInicio.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
