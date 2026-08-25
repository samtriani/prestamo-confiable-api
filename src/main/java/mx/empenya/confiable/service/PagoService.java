package mx.empenya.confiable.service;

import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.dto.response.PagoDetalleResponse;
import mx.empenya.confiable.entity.Abono;
import mx.empenya.confiable.entity.Pago;
import mx.empenya.confiable.repository.AbonoRepository;
import mx.empenya.confiable.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enriquece los pagos con el detalle de sus abonos.
 *
 * La corrida de pagos se servía como entidad cruda, sin totalAbonado ni
 * saldoPendiente, así que el front no tenía forma de distinguir un pago
 * cubierto por completo de uno cubierto a medias.
 */
@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository  pagoRepository;
    private final AbonoRepository abonoRepository;

    /** Totales agregados de un pago. */
    private record Totales(BigDecimal totalAbonado, int numAbonos, int sinCorte) {
        static final Totales VACIO = new Totales(BigDecimal.ZERO, 0, 0);
    }

    @Transactional(readOnly = true)
    public List<PagoDetalleResponse> findByPrestamoConDetalle(UUID prestamoId) {
        return enriquecer(pagoRepository.findByPrestamoIdOrderByNumeroPagoAsc(prestamoId), false);
    }

    /** Variante que incluye el desglose abono por abono en la misma respuesta. */
    @Transactional(readOnly = true)
    public List<PagoDetalleResponse> findByPrestamoConAbonos(UUID prestamoId) {
        return enriquecer(pagoRepository.findByPrestamoIdOrderByNumeroPagoAsc(prestamoId), true);
    }

    /**
     * Totales por pago en una sola consulta, para que otros servicios
     * (cobranza) no repitan la agregación.
     */
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> getTotalAbonadoPorPago(List<UUID> pagoIds) {
        return cargarTotales(pagoIds).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().totalAbonado()));
    }

    private List<PagoDetalleResponse> enriquecer(List<Pago> pagos, boolean incluirAbonos) {
        if (pagos.isEmpty()) return List.of();

        List<UUID> ids = pagos.stream().map(Pago::getId).toList();
        Map<UUID, Totales> totales = cargarTotales(ids);

        Map<UUID, List<Abono>> abonosPorPago = incluirAbonos
            ? abonoRepository.findByPagoIdsOrderByFechaAbonoAsc(ids).stream()
                .collect(Collectors.groupingBy(a -> a.getPago().getId()))
            : Map.of();

        return pagos.stream().map(p -> {
            Totales t = totales.getOrDefault(p.getId(), Totales.VACIO);

            // El saldo nunca se reporta negativo aunque el abono excediera.
            BigDecimal saldo = p.getMontoProgramado().subtract(t.totalAbonado()).max(BigDecimal.ZERO);

            return PagoDetalleResponse.builder()
                .id(p.getId())
                .prestamoId(p.getPrestamo().getId())
                .numeroPago(p.getNumeroPago())
                .fechaProgramada(p.getFechaProgramada())
                .montoProgramado(p.getMontoProgramado())
                .estado(p.getEstado().name())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .totalAbonado(t.totalAbonado())
                .saldoPendiente(saldo)
                .numAbonos(t.numAbonos())
                // Parcial = hay dinero abonado pero todavía falta, o se
                // cubrió en más de una exhibición.
                .tieneAbonoParcial(
                    t.numAbonos() > 1
                    || (t.totalAbonado().signum() > 0 && saldo.signum() > 0)
                )
                .tienePendienteCorte(t.sinCorte() > 0)
                .abonos(incluirAbonos ? abonosPorPago.getOrDefault(p.getId(), List.of()) : null)
                .build();
        }).toList();
    }

    private Map<UUID, Totales> cargarTotales(List<UUID> pagoIds) {
        if (pagoIds.isEmpty()) return Map.of();

        Map<UUID, Totales> map = new HashMap<>();
        for (Object[] fila : abonoRepository.sumTotalesByPagoIds(pagoIds)) {
            map.put(
                (UUID) fila[0],
                new Totales(
                    (BigDecimal) fila[1],
                    ((Number) fila[2]).intValue(),
                    fila[3] != null ? ((Number) fila[3]).intValue() : 0
                )
            );
        }
        return map;
    }
}
