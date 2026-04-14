package mx.empenya.confiable.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CorteDetalleResponse {
    private UUID        id;
    private LocalDate   fechaCorte;
    private BigDecimal  totalSemanal;
    private Integer     numAbonos;
    private String      descripcion;
    private LocalDateTime createdAt;
    private long        numClientes;
    private long        numPrestamos;
}
