package mx.empenya.confiable.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PrestamoActivoResponse {
    private UUID     id;
    private UUID     clienteId;
    private String   clienteNumero;
    private String   clienteNombre;
    private String   clienteTelefono;
    private String   numero;
    private BigDecimal monto;
    private BigDecimal pagoSemanal;
    private LocalDate  fechaInicio;
    private LocalDate  fechaPrimerPago;
    private Boolean    activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int        totalPagos;
    private int        pagosCubiertos;
    private int        pagosSinCorte;
    private int        pagosAtrasados;
    private BigDecimal totalAbonado;
    private BigDecimal saldoPendiente;
    private BigDecimal semanalSinCorte;
    private BigDecimal totalARecuperar;
}
