package mx.empenya.confiable.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prestamos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "numero", nullable = false, unique = true, length = 8)
    private String numero;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "pago_semanal", nullable = false, precision = 12, scale = 2)
    private BigDecimal pagoSemanal;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_primer_pago", nullable = false)
    private LocalDate fechaPrimerPago;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "prestamo", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("numero_pago ASC")
    private List<Pago> pagos;
}
