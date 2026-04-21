package mx.empenya.confiable.repository;

import mx.empenya.confiable.entity.Corte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CorteRepository extends JpaRepository<Corte, UUID> {

    List<Corte> findAllByOrderByFechaCorteDescCreatedAtDesc();

    @Query("SELECT COUNT(DISTINCT a.pago.prestamo.cliente.id) FROM Abono a WHERE a.corte.id = :corteId")
    long countClientesByCorteId(@Param("corteId") UUID corteId);

    @Query("SELECT COUNT(DISTINCT a.pago.prestamo.id) FROM Abono a WHERE a.corte.id = :corteId")
    long countPrestamosByCorteId(@Param("corteId") UUID corteId);
}
