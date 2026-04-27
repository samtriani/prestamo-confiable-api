package mx.empenya.confiable.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CorteAbonoItem {
    private String        clienteNumero;
    private String        clienteNombre;
    private String        clienteTelefono;
    private String        prestamoNumero;
    private BigDecimal    prestaMonto;
    private Integer       numeroPago;
    private BigDecimal    montoAbono;
    private LocalDateTime fechaAbono;
}
