package mx.empenya.confiable.service;

import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.dto.response.CobranzaItemResponse;
import mx.empenya.confiable.entity.Pago;
import mx.empenya.confiable.enums.EstadoPago;
import mx.empenya.confiable.repository.AbonoRepository;
import mx.empenya.confiable.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CobranzaService {

    private final PagoRepository  pagoRepository;
    private final AbonoRepository abonoRepository;

    @Transactional(readOnly = true)
    public List<CobranzaItemResponse> getCobranzaSemana() {
        List<Pago> pagos = pagoRepository.findByEstadosConDetalle(
            List.of(EstadoPago.PROXIMO, EstadoPago.ATRASADO)
        );

        if (pagos.isEmpty()) return List.of();

        // Un pago listado aquí puede traer abonos parciales previos. Sin
        // esta información el operador intentaría cobrar el importe
        // completo y la API rechazaría el abono por exceder el saldo.
        Map<UUID, BigDecimal> abonadoPorPago = new HashMap<>();
        Map<UUID, Integer>    numAbonosPorPago = new HashMap<>();
        for (Object[] fila : abonoRepository.sumTotalesByPagoIds(pagos.stream().map(Pago::getId).toList())) {
            UUID pagoId = (UUID) fila[0];
            abonadoPorPago.put(pagoId, (BigDecimal) fila[1]);
            numAbonosPorPago.put(pagoId, ((Number) fila[2]).intValue());
        }

        LocalDate hoy = LocalDate.now();

        return pagos.stream()
            .map(p -> {
                boolean atrasado = p.getEstado() == EstadoPago.ATRASADO;
                int dias = atrasado
                    ? (int) ChronoUnit.DAYS.between(p.getFechaProgramada(), hoy)
                    : 0;

                BigDecimal abonado = abonadoPorPago.getOrDefault(p.getId(), BigDecimal.ZERO);
                BigDecimal saldo   = p.getMontoProgramado().subtract(abonado).max(BigDecimal.ZERO);

                return CobranzaItemResponse.builder()
                    .pagoId(p.getId())
                    .numeroPago(p.getNumeroPago())
                    .fechaProgramada(p.getFechaProgramada())
                    .montoProgramado(p.getMontoProgramado())
                    .estado(p.getEstado().name())
                    .diasVencido(dias)
                    .totalAbonado(abonado)
                    .saldoPendiente(saldo)
                    .numAbonos(numAbonosPorPago.getOrDefault(p.getId(), 0))
                    .prestamoId(p.getPrestamo().getId())
                    .prestamoNumero(p.getPrestamo().getNumero())
                    .clienteId(p.getPrestamo().getCliente().getId())
                    .clienteNumero(p.getPrestamo().getCliente().getNumero())
                    .clienteNombre(p.getPrestamo().getCliente().getNombre())
                    .clienteTelefono(p.getPrestamo().getCliente().getTelefono())
                    .build();
            })
            // Atrasados primero, luego por fecha más antigua
            .sorted(Comparator
                .comparing((CobranzaItemResponse r) -> "ATRASADO".equals(r.getEstado()) ? 0 : 1)
                .thenComparing(CobranzaItemResponse::getFechaProgramada))
            .toList();
    }
}
