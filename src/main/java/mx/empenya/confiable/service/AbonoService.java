package mx.empenya.confiable.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.empenya.confiable.dto.request.AbonoRequest;
import mx.empenya.confiable.entity.Abono;
import mx.empenya.confiable.entity.Pago;
import mx.empenya.confiable.entity.Prestamo;
import mx.empenya.confiable.enums.EstadoPago;
import mx.empenya.confiable.exception.BusinessException;
import mx.empenya.confiable.repository.AbonoRepository;
import mx.empenya.confiable.repository.PagoRepository;
import mx.empenya.confiable.repository.PrestamoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbonoService {

    private final AbonoRepository abonoRepository;
    private final PagoRepository pagoRepository;
    private final PrestamoRepository prestamoRepository;

    /**
     * Registra un abono (total o parcial) a un pago específico.
     * Recalcula automáticamente el estado del pago tras el abono.
     */
    @Transactional
    public Abono registrarAbono(AbonoRequest request) {
        Pago pago = pagoRepository.findById(request.getPagoId())
            .orElseThrow(() -> BusinessException.notFound("Pago", request.getPagoId().toString()));

        // No se puede abonar a un pago ya cerrado en corte
        if (pago.getEstado() == EstadoPago.PAGADO) {
            throw BusinessException.conflict(
                "El pago #" + pago.getNumeroPago() + " ya fue liquidado y procesado en corte."
            );
        }

        // Validar que el abono no exceda el saldo pendiente
        BigDecimal totalAbonado = abonoRepository.sumMontoByPagoId(pago.getId());
        BigDecimal saldoPendiente = pago.getMontoProgramado().subtract(totalAbonado);

        if (request.getMontoAbono().compareTo(saldoPendiente) > 0) {
            throw BusinessException.badRequest(
                "El abono de $" + request.getMontoAbono() +
                " excede el saldo pendiente de $" + saldoPendiente
            );
        }

        // Crear abono
        LocalDateTime fechaAbono = request.getFechaAbono() != null
            ? request.getFechaAbono()
            : LocalDateTime.now();

        Abono abono = Abono.builder()
            .pago(pago)
            .montoAbono(request.getMontoAbono())
            .fechaAbono(fechaAbono)
            .build();
        abono = abonoRepository.save(abono);

        // Recalcular estado del pago
        recalcularEstado(pago);

        // Si este pago ya está cubierto, marcar el siguiente como PROXIMO
        if (pago.getEstado() == EstadoPago.PAGADO_SIN_CORTE) {
            activarSiguientePago(pago);
        }

        log.info("Abono de ${} registrado en pago #{} del préstamo {}",
            request.getMontoAbono(), pago.getNumeroPago(), pago.getPrestamo().getNumero());

        return abono;
    }

    /**
     * Recalcula el estado de un pago basándose en la suma de sus abonos.
     *
     * Reglas:
     * - suma abonos >= monto → PAGADO_SIN_CORTE (naranja, pendiente de corte)
     * - suma abonos < monto Y fecha venció → ATRASADO (rojo)
     * - suma abonos < monto Y fecha futura → PROXIMO o PENDIENTE (sin cambio)
     */
    public void recalcularEstado(Pago pago) {
        BigDecimal totalAbonado = abonoRepository.sumMontoByPagoId(pago.getId());
        EstadoPago nuevoEstado;

        if (totalAbonado.compareTo(pago.getMontoProgramado()) >= 0) {
            nuevoEstado = EstadoPago.PAGADO_SIN_CORTE;
        } else if (pago.getFechaProgramada().isBefore(LocalDateTime.now().toLocalDate())) {
            nuevoEstado = EstadoPago.ATRASADO;
        } else {
            nuevoEstado = pago.getEstado(); // sin cambio si es PROXIMO o PENDIENTE
        }

        if (nuevoEstado != pago.getEstado()) {
            pago.setEstado(nuevoEstado);
            pagoRepository.save(pago);
        }
    }

    /**
     * Tras cubrir un pago, busca el siguiente PENDIENTE o ATRASADO del mismo
     * préstamo y lo marca como PROXIMO si todavía no hay un PROXIMO activo.
     */
    private void activarSiguientePago(Pago pagoActual) {
        List<Pago> pagosDelPrestamo = pagoRepository
            .findByPrestamoIdOrderByNumeroPagoAsc(pagoActual.getPrestamo().getId());

        boolean hayProximo = pagosDelPrestamo.stream()
            .anyMatch(p -> p.getEstado() == EstadoPago.PROXIMO);

        if (!hayProximo) {
            pagosDelPrestamo.stream()
                .filter(p -> p.getEstado() == EstadoPago.PENDIENTE || p.getEstado() == EstadoPago.ATRASADO)
                .findFirst()
                .ifPresent(p -> {
                    p.setEstado(EstadoPago.PROXIMO);
                    pagoRepository.save(p);
                });
        }

        // Verificar si el préstamo quedó liquidado (todos los pagos cubiertos)
        boolean todosLiquidados = pagosDelPrestamo.stream()
            .allMatch(p -> p.getEstado() == EstadoPago.PAGADO || p.getEstado() == EstadoPago.PAGADO_SIN_CORTE);

        if (todosLiquidados) {
            Prestamo prestamo = pagoActual.getPrestamo();
            prestamo.setActivo(false);
            prestamoRepository.save(prestamo);
            log.info("Préstamo {} liquidado completamente.", prestamo.getNumero());
        }
    }

    @Transactional(readOnly = true)
    public List<Abono> findByPagoId(UUID pagoId) {
        return abonoRepository.findByPagoIdOrderByFechaAbonoAsc(pagoId);
    }

    @Transactional(readOnly = true)
    public List<Abono> findPendientesDeCorte() {
        return abonoRepository.findByCorteIdIsNullOrderByFechaAbonoAsc();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalSemanalActual() {
        return abonoRepository.sumTotalSemanalActual();
    }
}
