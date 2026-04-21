package mx.empenya.confiable.service;

import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.enums.EstadoPago;
import mx.empenya.confiable.repository.AbonoRepository;
import mx.empenya.confiable.repository.ClienteRepository;
import mx.empenya.confiable.repository.PagoRepository;
import mx.empenya.confiable.repository.PrestamoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ClienteRepository clienteRepository;
    private final PrestamoRepository prestamoRepository;
    private final AbonoRepository abonoRepository;
    private final PagoRepository pagoRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        Map<String, Object> data = new HashMap<>();

        data.put("totalClientes",         clienteRepository.count());
        data.put("prestamosActivos",      prestamoRepository.countByActivoTrue());
        data.put("totalPrestadoHistorico",prestamoRepository.sumMontoHistorico());
        data.put("totalRecuperado",       abonoRepository.sumTotalRecuperado());
        data.put("totalSemanalActual",    abonoRepository.sumTotalSemanalActual());
        data.put("pagosAtrasados",        pagoRepository.countByEstado(EstadoPago.ATRASADO));
        data.put("abonosPendientesCorte", abonoRepository.countByCorteIdIsNull());
        data.put("pagosPendientes",       pagoRepository.countByEstado(EstadoPago.PROXIMO)
                                        + pagoRepository.countByEstado(EstadoPago.PENDIENTE));

        BigDecimal prestado   = (BigDecimal) data.get("totalPrestadoHistorico");
        BigDecimal recuperado = (BigDecimal) data.get("totalRecuperado");
        BigDecimal porRecuperar = prestado.subtract(recuperado).max(BigDecimal.ZERO);
        data.put("porRecuperar", porRecuperar);

        return data;
    }
}
