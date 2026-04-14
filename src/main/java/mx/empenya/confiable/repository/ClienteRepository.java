package mx.empenya.confiable.repository;

import mx.empenya.confiable.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    boolean existsByNumero(String numero);

    Optional<Cliente> findByNumero(String numero);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(c.numero, 4) AS integer)), 0) FROM Cliente c")
    Integer findMaxNumeroSecuencia();
}
