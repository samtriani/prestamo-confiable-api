package mx.empenya.confiable.repository;

import mx.empenya.confiable.entity.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, UUID> {

    /** Historial completo del cliente con pagos precargados. Abonos se cargan en batch por @BatchSize. */
    @Query("SELECT DISTINCT p FROM Prestamo p JOIN FETCH p.cliente JOIN FETCH p.pagos WHERE p.cliente.id = :clienteId ORDER BY p.createdAt DESC")
    List<Prestamo> findByClienteIdOrderByCreatedAtDesc(@Param("clienteId") UUID clienteId);

    /** Préstamos activos con cliente y pagos en una sola query. Abonos se cargan en batch por @BatchSize. */
    @Query("SELECT DISTINCT p FROM Prestamo p JOIN FETCH p.cliente JOIN FETCH p.pagos WHERE p.activo = true ORDER BY p.createdAt DESC")
    List<Prestamo> findPrestamosActivosConDetalle();

    long countByActivoTrue();

    /**
     * Regla de negocio: un cliente no puede tener más de un préstamo activo.
     * Usada en ClienteService antes de crear un préstamo nuevo.
     */
    boolean existsByClienteIdAndActivoTrue(UUID clienteId);

    /** Para generar el siguiente número PR-XXX. */
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.numero, 4) AS integer)), 0) FROM Prestamo p")
    Integer findMaxNumeroSecuencia();

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Prestamo p")
    BigDecimal sumMontoHistorico();
}
