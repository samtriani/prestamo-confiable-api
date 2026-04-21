package mx.empenya.confiable.repository;

import mx.empenya.confiable.entity.Pago;
import mx.empenya.confiable.enums.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PagoRepository extends JpaRepository<Pago, UUID> {

    List<Pago> findByPrestamoIdOrderByNumeroPagoAsc(UUID prestamoId);

    List<Pago> findByEstadoOrderByFechaProgramadaAsc(EstadoPago estado);

    @Query("""
        SELECT p FROM Pago p
        WHERE p.estado IN ('PENDIENTE', 'PROXIMO', 'ATRASADO', 'PAGADO_SIN_CORTE')
        AND p.fechaProgramada < :hoy
        AND p.id NOT IN (
            SELECT a.pago.id FROM Abono a
            GROUP BY a.pago.id
            HAVING COALESCE(SUM(a.montoAbono), 0) >= p.montoProgramado
        )
        """)
    List<Pago> findPagosVencidosSinCubrir(@Param("hoy") LocalDate hoy);

    @Query("""
        SELECT p FROM Pago p
        JOIN FETCH p.prestamo pr
        JOIN FETCH pr.cliente c
        WHERE p.estado IN :estados
        ORDER BY p.fechaProgramada ASC
        """)
    List<Pago> findByEstadosConDetalle(@Param("estados") List<EstadoPago> estados);

    @Modifying
    @Query("UPDATE Pago p SET p.estado = :estado WHERE p.id = :id")
    void actualizarEstado(@Param("id") UUID id, @Param("estado") EstadoPago estado);

    long countByEstado(EstadoPago estado);
}
