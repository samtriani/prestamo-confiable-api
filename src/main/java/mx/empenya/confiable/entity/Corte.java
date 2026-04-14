package mx.empenya.confiable.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cortes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Corte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "fecha_corte", nullable = false)
    private LocalDate fechaCorte;

    @Column(name = "total_semanal", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSemanal = BigDecimal.ZERO;

    @Column(name = "num_abonos", nullable = false)
    @Builder.Default
    private Integer numAbonos = 0;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "corte", fetch = FetchType.LAZY)
    private List<Abono> abonos;
}
