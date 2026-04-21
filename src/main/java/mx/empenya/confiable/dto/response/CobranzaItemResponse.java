package mx.empenya.confiable.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CobranzaItemResponse {

    // ── Pago ──────────────────────────────────────────────────────
    private UUID    pagoId;
    private int     numeroPago;
    private LocalDate fechaProgramada;
    private BigDecimal montoProgramado;
    private String  estado;        // "PROXIMO" | "ATRASADO"
    private int     diasVencido;   // 0 si PROXIMO

    // ── Préstamo ──────────────────────────────────────────────────
    private UUID   prestamoId;
    private String prestamoNumero;

    // ── Cliente ───────────────────────────────────────────────────
    private UUID   clienteId;
    private String clienteNumero;
    private String clienteNombre;
    private String clienteTelefono;
}
