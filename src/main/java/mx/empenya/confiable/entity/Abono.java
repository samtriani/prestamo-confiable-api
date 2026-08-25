package mx.empenya.confiable.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "abonos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Abono {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corte_id")
    private Corte corte;  // null = pendiente de corte (naranja)

    @Column(name = "monto_abono", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoAbono;

    @Column(name = "fecha_abono", nullable = false)
    private LocalDateTime fechaAbono;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Campos derivados para el front ────────────────────────────
    // pago y corte van con @JsonIgnore para no arrastrar el grafo completo,
    // pero el front necesita los identificadores y saber si el abono ya
    // entró a corte. Leer solo el id de un proxy lazy no dispara consulta.

    @JsonProperty("pagoId")
    public UUID getPagoId() {
        return pago != null ? pago.getId() : null;
    }

    @JsonProperty("corteId")
    public UUID getCorteId() {
        return corte != null ? corte.getId() : null;
    }

    /** false = pendiente de corte (naranja). */
    @JsonProperty("enCorte")
    public boolean isEnCorte() {
        return corte != null;
    }
}
