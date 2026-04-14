package mx.empenya.confiable.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String nombre;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;

    private String domicilio;

    @NotNull(message = "El monto del préstamo es obligatorio")
    @DecimalMin(value = "100.00", message = "El monto mínimo es $100")
    private BigDecimal monto;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha del primer pago es obligatoria")
    private LocalDate fechaPrimerPago;
}
