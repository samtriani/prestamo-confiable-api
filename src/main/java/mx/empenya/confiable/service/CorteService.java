package mx.empenya.confiable.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.empenya.confiable.dto.request.CorteRequest;
import mx.empenya.confiable.dto.response.CorteAbonoItem;
import mx.empenya.confiable.dto.response.CorteDetalleResponse;
import mx.empenya.confiable.entity.Corte;
import mx.empenya.confiable.entity.Pago;
import mx.empenya.confiable.enums.EstadoPago;
import mx.empenya.confiable.exception.BusinessException;
import mx.empenya.confiable.repository.AbonoRepository;
import mx.empenya.confiable.repository.CorteRepository;
import mx.empenya.confiable.repository.PagoRepository;
import mx.empenya.confiable.repository.PrestamoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorteService {

    private final CorteRepository    corteRepository;
    private final AbonoRepository    abonoRepository;
    private final PagoRepository     pagoRepository;
    private final PrestamoRepository prestamoRepository;
    private final AbonoService       abonoService;

    /**
     * Realiza el corte semanal:
     * 1. Suma todos los abonos pendientes (corte_id IS NULL)
     * 2. Crea el registro de Corte con ese total
     * 3. Asigna corte_id a todos esos abonos → pasan de naranja a verde
     * 4. Actualiza estado de pagos: PAGADO_SIN_CORTE → PAGADO
     * 5. El acumulado semanal queda en 0 (histórico conservado en la tabla cortes)
     */
    @Transactional
    public Corte realizarCorte(CorteRequest request) {
        long abonosesPendientes = abonoRepository.countByCorteIdIsNull();
        if (abonosesPendientes == 0) {
            throw BusinessException.badRequest("No hay abonos pendientes para incluir en el corte.");
        }

        BigDecimal totalSemanal = abonoRepository.sumTotalSemanalActual();
        LocalDate fechaCorte = request.getFechaCorte() != null ? request.getFechaCorte() : LocalDate.now();

        // Crear registro de corte
        Corte corte = Corte.builder()
            .fechaCorte(fechaCorte)
            .totalSemanal(totalSemanal)
            .numAbonos((int) abonosesPendientes)
            .descripcion(request.getDescripcion())
            .build();
        corte = corteRepository.save(corte);

        // Asignar corte_id a todos los abonos pendientes
        int abonosActualizados = abonoRepository.asignarCorteATodosLosPendientes(corte.getId());
        log.info("Corte {} realizado: {} abonos por ${}", corte.getId(), abonosActualizados, totalSemanal);

        // Actualizar estado de pagos: PAGADO_SIN_CORTE → PAGADO (verde)
        List<Pago> pagosNaranja = pagoRepository.findByEstadoOrderByFechaProgramadaAsc(EstadoPago.PAGADO_SIN_CORTE);
        pagosNaranja.forEach(pago -> {
            pago.setEstado(EstadoPago.PAGADO);
            pagoRepository.save(pago);
        });
        log.info("{} pagos marcados como PAGADO tras el corte", pagosNaranja.size());

        // Marcar como inactivos los préstamos donde todos los pagos quedaron PAGADO
        Set<UUID> prestamosAfectados = pagosNaranja.stream()
                .map(p -> p.getPrestamo().getId())
                .collect(Collectors.toSet());

        prestamosAfectados.forEach(prestamoId -> {
            List<Pago> todos = pagoRepository.findByPrestamoIdOrderByNumeroPagoAsc(prestamoId);
            boolean todosPagado = todos.stream().allMatch(p -> p.getEstado() == EstadoPago.PAGADO);
            if (todosPagado) {
                prestamoRepository.findById(prestamoId).ifPresent(prestamo -> {
                    prestamo.setActivo(false);
                    prestamoRepository.save(prestamo);
                    log.info("Préstamo {} liquidado completamente tras corte.", prestamo.getNumero());
                });
            }
        });

        return corte;
    }

    @Transactional(readOnly = true)
    public List<CorteAbonoItem> findAbonosDetalle(UUID corteId) {
        if (!corteRepository.existsById(corteId)) {
            throw BusinessException.notFound("Corte", corteId.toString());
        }
        return abonoRepository.findByCorteIdConDetalle(corteId)
                .stream()
                .map(a -> {
                    var pago     = a.getPago();
                    var prestamo = pago.getPrestamo();
                    var cliente  = prestamo.getCliente();
                    return CorteAbonoItem.builder()
                            .clienteNumero(cliente.getNumero())
                            .clienteNombre(cliente.getNombre())
                            .clienteTelefono(cliente.getTelefono())
                            .prestamoNumero(prestamo.getNumero())
                            .prestaMonto(prestamo.getMonto())
                            .numeroPago(pago.getNumeroPago())
                            .montoAbono(a.getMontoAbono())
                            .fechaAbono(a.getFechaAbono())
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CorteDetalleResponse> findHistorico() {
        return corteRepository.findAllByOrderByFechaCorteDescCreatedAtDesc()
            .stream()
            .map(this::toDetalleResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public CorteDetalleResponse findById(java.util.UUID id) {
        return toDetalleResponse(corteRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Corte", id.toString())));
    }

    private CorteDetalleResponse toDetalleResponse(Corte c) {
        return CorteDetalleResponse.builder()
            .id(c.getId())
            .fechaCorte(c.getFechaCorte())
            .totalSemanal(c.getTotalSemanal())
            .numAbonos(c.getNumAbonos())
            .descripcion(c.getDescripcion())
            .createdAt(c.getCreatedAt())
            .numClientes(corteRepository.countClientesByCorteId(c.getId()))
            .numPrestamos(corteRepository.countPrestamosByCorteId(c.getId()))
            .build();
    }

    /**
     * Job automático que corre cada día a las 00:05 para marcar como
     * ATRASADO los pagos cuya fecha venció sin estar cubiertos.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "America/Mexico_City")
    @Transactional
    public void actualizarPagosAtrasados() {
        List<Pago> vencidos = pagoRepository.findPagosVencidosSinCubrir(LocalDate.now());
        if (vencidos.isEmpty()) return;

        vencidos.forEach(pago -> {
            if (pago.getEstado() != EstadoPago.ATRASADO) {
                pago.setEstado(EstadoPago.ATRASADO);
                pagoRepository.save(pago);
            }
        });
        log.info("Job estados: {} pagos marcados como ATRASADO", vencidos.size());
    }
}
