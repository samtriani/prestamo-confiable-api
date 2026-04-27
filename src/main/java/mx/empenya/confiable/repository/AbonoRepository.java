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

    /** Abonos de un corte con cliente, préstamo y pago precargados para el reporte PDF. */
    @Query("""
            SELECT a FROM Abono a
            JOIN FETCH a.pago p
            JOIN FETCH p.prestamo pr
            JOIN FETCH pr.cliente c
            WHERE a.corte.id = :corteId
            ORDER BY c.numero ASC, p.numeroPago ASC
            """)
    List<Abono> findByCorteIdConDetalle(@Param("corteId") UUID corteId);

    /** Total abonado sobre préstamos activos (para calcular saldo pendiente de cartera). */
    @Query(value = """
            SELECT COALESCE(SUM(a.monto_abono), 0)
            FROM abonos a
            JOIN pagos pag ON a.pago_id = pag.id
            JOIN prestamos p ON pag.prestamo_id = p.id
            WHERE p.activo = true
            """, nativeQuery = true)
    BigDecimal sumAbonadoEnActivos();

    /** Monto recuperado agrupado por mes (YYYY-MM). */
    @Query(value = """
            SELECT TO_CHAR(CAST(fecha_abono AS DATE), 'YYYY-MM') AS mes,
                   SUM(monto_abono)                               AS total
            FROM abonos
            GROUP BY TO_CHAR(CAST(fecha_abono AS DATE), 'YYYY-MM')
            ORDER BY mes
            """, nativeQuery = true)
    List<Object[]> findAbonosPorMes();
}
