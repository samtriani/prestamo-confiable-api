package mx.empenya.confiable.repository;

import mx.empenya.confiable.entity.Abono;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface AbonoRepository extends JpaRepository<Abono, UUID> {

    List<Abono> findByPagoIdOrderByFechaAbonoAsc(UUID pagoId);

    // Abonos pendientes de corte (naranja)
    List<Abono> findByCorteIdIsNullOrderByFechaAbonoAsc();

    @Query("SELECT COALESCE(SUM(a.montoAbono), 0) FROM Abono a WHERE a.pago.id = :pagoId")
    BigDecimal sumMontoByPagoId(@Param("pagoId") UUID pagoId);

    @Query("SELECT COALESCE(SUM(a.montoAbono), 0) FROM Abono a WHERE a.corte.id IS NULL")
    BigDecimal sumTotalSemanalActual();

    @Query("SELECT COALESCE(SUM(a.montoAbono), 0) FROM Abono a")
    BigDecimal sumTotalRecuperado();

    @Modifying
    @Query("UPDATE Abono a SET a.corte = (SELECT c FROM Corte c WHERE c.id = :corteId) WHERE a.corte IS NULL")
    int asignarCorteATodosLosPendientes(@Param("corteId") UUID corteId);

    long countByCorteIdIsNull();
}
