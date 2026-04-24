package mx.empenya.confiable.service;

import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.dto.response.ReporteResponse;
import mx.empenya.confiable.dto.response.ReporteResponse.MesDato;
import mx.empenya.confiable.dto.response.ReporteResponse.TopDeudor;
import mx.empenya.confiable.entity.Abono;
import mx.empenya.confiable.entity.Pago;
import mx.empenya.confiable.entity.Prestamo;
import mx.empenya.confiable.enums.EstadoPago;
import mx.empenya.confiable.repository.AbonoRepository;
import mx.empenya.confiable.repository.ClienteRepository;
import mx.empenya.confiable.repository.PrestamoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReportesService {

    private static final BigDecimal FACTOR_RECUPERACION = new BigDecimal("1.4");

    private final PrestamoRepository prestamoRepository;
    private final AbonoRepository    abonoRepository;
    private final ClienteRepository  clienteRepository;

    @Transactional(readOnly = true)
    public ReporteResponse getReporte() {

        // ── Financiero ─────────────────────────────────────────────
        BigDecimal totalPrestado   = prestamoRepository.sumMontoHistorico();
        BigDecimal totalRecuperado = abonoRepository.sumTotalRecuperado();
        BigDecimal gananciaNeta    = totalRecuperado.subtract(totalPrestado);

        BigDecimal montoActivosTotal  = prestamoRepository.sumMontoActivosTotal();
        BigDecimal abonadoEnActivos   = abonoRepository.sumAbonadoEnActivos();
        BigDecimal pendienteSiLiquidan = montoActivosTotal
                .multiply(FACTOR_RECUPERACION)
                .subtract(abonadoEnActivos)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // Ganancia total proyectada si todos los activos liquidan
        BigDecimal proyeccionGanancia = totalRecuperado
                .add(pendienteSiLiquidan)
                .subtract(totalPrestado)
                .setScale(2, RoundingMode.HALF_UP);

        // ── Cartera activa ─────────────────────────────────────────
        List<Prestamo> activos = prestamoRepository.findPrestamosActivosConDetalle();
        long prestamosActivos  = activos.size();

        long prestamosAtrasados = activos.stream()
                .filter(p -> p.getPagos().stream()
                        .anyMatch(pg -> pg.getEstado() == EstadoPago.ATRASADO))
                .count();

        long prestamosAlCorriente = prestamosActivos - prestamosAtrasados;
        long totalClientes        = clienteRepository.count();

        // ── Top 10 deudores por saldo pendiente ────────────────────
        List<TopDeudor> topSaldos = activos.stream()
                .map(p -> {
                    List<Pago> pagos = p.getPagos() != null ? p.getPagos() : List.of();

                    BigDecimal totalAbonado = pagos.stream()
                            .flatMap(pag -> pag.getAbonos() != null
                                    ? pag.getAbonos().stream() : Stream.empty())
                            .map(Abono::getMontoAbono)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalARecuperar = p.getMonto()
                            .multiply(FACTOR_RECUPERACION)
                            .setScale(2, RoundingMode.HALF_UP);

                    BigDecimal saldo = totalARecuperar.subtract(totalAbonado)
                            .max(BigDecimal.ZERO);

                    int atrasados = (int) pagos.stream()
                            .filter(pag -> pag.getEstado() == EstadoPago.ATRASADO)
                            .count();

                    return TopDeudor.builder()
                            .clienteNumero(p.getCliente().getNumero())
                            .clienteNombre(p.getCliente().getNombre())
                            .saldoPendiente(saldo)
                            .totalAbonado(totalAbonado)
                            .pagosAtrasados(atrasados)
                            .build();
                })
                .sorted(Comparator.comparing(TopDeudor::getSaldoPendiente).reversed())
                .limit(10)
                .toList();

        // ── Series por mes ─────────────────────────────────────────
        List<MesDato> prestamosPorMes = prestamoRepository.findPrestamosPorMes()
                .stream()
                .map(row -> MesDato.builder()
                        .mes(row[0].toString())
                        .cantidad(((Number) row[1]).intValue())
                        .monto(new BigDecimal(row[2].toString()))
                        .build())
                .toList();

        List<MesDato> abonosPorMes = abonoRepository.findAbonosPorMes()
                .stream()
                .map(row -> MesDato.builder()
                        .mes(row[0].toString())
                        .monto(new BigDecimal(row[1].toString()))
                        .cantidad(0)
                        .build())
                .toList();

        return ReporteResponse.builder()
                .totalPrestado(totalPrestado)
                .totalRecuperado(totalRecuperado)
                .gananciaNeta(gananciaNeta)
                .pendienteSiLiquidan(pendienteSiLiquidan)
                .proyeccionGanancia(proyeccionGanancia)
                .prestamosActivos(prestamosActivos)
                .prestamosAtrasados(prestamosAtrasados)
                .prestamosAlCorriente(prestamosAlCorriente)
                .totalClientes(totalClientes)
                .prestamosPorMes(prestamosPorMes)
                .abonosPorMes(abonosPorMes)
                .topSaldos(topSaldos)
                .build();
    }
}
