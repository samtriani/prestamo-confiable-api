package mx.empenya.confiable.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CorteRequest {
    private LocalDate fechaCorte;   // opcional, default = hoy
    private String descripcion;
}
