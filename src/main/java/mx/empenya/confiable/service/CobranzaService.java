package mx.empenya.confiable.service;

import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.dto.response.CobranzaItemResponse;
import mx.empenya.confiable.entity.Pago;
import mx.empenya.confiable.enums.EstadoPago;
import mx.empenya.confiable.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CobranzaService {

    private final PagoRepository pagoRepository;

    @Transactional(readOnly = true)
    public List<CobranzaItemResponse> getCobranzaSemana() {
        List<Pago> pagos = pagoRepository.findByEstadosConDetalle(
            List.of(EstadoPago.PROXIMO, EstadoPago.ATRASADO)
        );

        LocalDate hoy = LocalDate.now();

        return pagos.stream()
            .map(p -> {
                boolean atrasado = p.getEstado() == EstadoPago.ATRASADO;
                int dias = atrasado
                    ? (int) ChronoUnit.DAYS.between(p.getFechaProgramada(), hoy)
                    : 0;

                return CobranzaItemResponse.builder()
                    .pagoId(p.getId())
                    .numeroPago(p.getNumeroPago())
                    .fechaProgramada(p.getFechaProgramada())
                    .montoProgramado(p.getMontoProgramado())
                    .estado(p.getEstado().name())
                    .diasVencido(dias)
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
