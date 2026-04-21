package mx.empenya.confiable.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AbonoRequest {

    @NotNull(message = "El ID del pago es obligatorio")
    private UUID pagoId;

    @NotNull(message = "El monto del abono es obligatorio")
    @DecimalMin(value = "1.00", message = "El monto mínimo es $1")
    private BigDecimal montoAbono;

    private LocalDateTime fechaAbono; // opcional, default = now
}
