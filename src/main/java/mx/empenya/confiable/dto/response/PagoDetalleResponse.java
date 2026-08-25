package mx.empenya.confiable.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import mx.empenya.confiable.entity.Abono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Pago enriquecido con el detalle de sus abonos.
 *
 * La entidad {@link mx.empenya.confiable.entity.Pago} no expone los abonos
 * (van con @JsonIgnore para evitar ciclos), así que el front nunca recibía
 * cuánto se había abonado ni cuánto faltaba. Este DTO cierra ese hueco.
 */
@Data
@Builder
public class PagoDetalleResponse {

    // ── Pago ──────────────────────────────────────────────────────
    private UUID       id;
    private UUID       prestamoId;
    private int        numeroPago;
    private LocalDate  fechaProgramada;
    private BigDecimal montoProgramado;
    private String     estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Detalle de abonos ─────────────────────────────────────────
    private BigDecimal totalAbonado;
    private BigDecimal saldoPendiente;
    private int        numAbonos;

    /** Indica si el pago se cubrió en más de una exhibición. */
    private boolean    tieneAbonoParcial;

    /** True si alguno de sus abonos sigue sin entrar a corte. */
    private boolean    tienePendienteCorte;

    /**
     * Desglose abono por abono. Solo se llena en los endpoints que lo
     * necesitan de un jalón (mi-credito); en el resto va null para no
     * inflar la respuesta.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Abono> abonos;
}
