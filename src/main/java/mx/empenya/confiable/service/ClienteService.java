package mx.empenya.confiable.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.empenya.confiable.dto.request.ClienteRequest;
import mx.empenya.confiable.dto.response.PrestamoActivoResponse;
import mx.empenya.confiable.entity.Abono;
import mx.empenya.confiable.entity.Cliente;
import mx.empenya.confiable.entity.Pago;
import mx.empenya.confiable.entity.Prestamo;
import mx.empenya.confiable.enums.EstadoPago;
import mx.empenya.confiable.exception.BusinessException;
import mx.empenya.confiable.repository.ClienteRepository;
import mx.empenya.confiable.repository.PagoRepository;
import mx.empenya.confiable.repository.PrestamoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteService {

    private static final int        TOTAL_PAGOS  = 14;
    private static final BigDecimal TASA_SEMANAL = new BigDecimal("0.10");

    private final ClienteRepository  clienteRepository;
    private final PrestamoRepository prestamoRepository;
    private final PagoRepository     pagoRepository;

    @Transactional(readOnly = true)
    public List<Cliente> findAll() {
        return clienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Cliente findById(UUID id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Cliente", id.toString()));
    }

    @Transactional(readOnly = true)
    public Cliente findByNumero(String numero) {
        return clienteRepository.findByNumero(numero)
            .orElseThrow(() -> BusinessException.notFound("Cliente", numero));
    }

    @Transactional(readOnly = true)
    public List<PrestamoActivoResponse> findHistorialByClienteId(UUID clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw BusinessException.notFound("Cliente", clienteId.toString());
        }
        return prestamoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId)
            .stream()
            .map(this::toActivoResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PrestamoActivoResponse> findPrestamosActivos() {
        return prestamoRepository.findPrestamosActivosConDetalle()
            .stream()
            .map(this::toActivoResponse)
            .toList();
    }

    private PrestamoActivoResponse toActivoResponse(Prestamo p) {
        Cliente c    = p.getCliente();
        List<Pago> pagos = p.getPagos() != null ? p.getPagos() : List.of();

        int pagosSinCorte = (int) pagos.stream()
            .filter(pag -> pag.getEstado() == EstadoPago.PAGADO_SIN_CORTE)
            .count();

        int pagosCubiertos = pagosSinCorte + (int) pagos.stream()
            .filter(pag -> pag.getEstado() == EstadoPago.PAGADO)
            .count();

        int pagosAtrasados = (int) pagos.stream()
            .filter(pag -> pag.getEstado() == EstadoPago.ATRASADO)
            .count();

        BigDecimal totalAbonado = pagos.stream()
            .flatMap(pag -> pag.getAbonos() != null ? pag.getAbonos().stream() : Stream.empty())
            .map(Abono::getMontoAbono)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal semanalSinCorte = pagos.stream()
            .filter(pag -> pag.getEstado() == EstadoPago.PAGADO_SIN_CORTE)
            .flatMap(pag -> pag.getAbonos() != null ? pag.getAbonos().stream() : Stream.empty())
            .filter(a -> a.getCorte() == null)
            .map(Abono::getMontoAbono)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalARecuperar = p.getMonto().multiply(new BigDecimal("1.4"))
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal saldoPendiente = totalARecuperar.subtract(totalAbonado)
            .max(BigDecimal.ZERO);

        return PrestamoActivoResponse.builder()
            .id(p.getId())
            .clienteId(c.getId())
            .clienteNumero(c.getNumero())
            .clienteNombre(c.getNombre())
            .clienteTelefono(c.getTelefono())
            .numero(p.getNumero())
            .monto(p.getMonto())
            .pagoSemanal(p.getPagoSemanal())
            .fechaInicio(p.getFechaInicio())
            .fechaPrimerPago(p.getFechaPrimerPago())
            .activo(p.getActivo())
            .createdAt(p.getCreatedAt())
            .updatedAt(p.getUpdatedAt())
            .totalPagos(TOTAL_PAGOS)
            .pagosCubiertos(pagosCubiertos)
            .pagosSinCorte(pagosSinCorte)
            .pagosAtrasados(pagosAtrasados)
            .totalAbonado(totalAbonado)
            .saldoPendiente(saldoPendiente)
            .semanalSinCorte(semanalSinCorte)
            .totalARecuperar(totalARecuperar)
            .build();
    }

    // ── Alta cliente nuevo (primer préstamo) ──────────────────────

    @Transactional
    public Cliente altaCliente(ClienteRequest request) {
        Cliente cliente = clienteRepository.save(
            Cliente.builder()
                .numero(generarNumeroCliente())
                .nombre(request.getNombre().toUpperCase().trim())
                .telefono(request.getTelefono())
                .domicilio(request.getDomicilio())
                .build()
        );
        log.info("Cliente creado: {}", cliente.getNumero());
        crearPrestamo(cliente, request);
        return cliente;
    }

    // ── Nuevo préstamo para cliente existente ─────────────────────

    @Transactional
    public Prestamo nuevoPrestamo(UUID clienteId, ClienteRequest request) {
        Cliente cliente = clienteRepository.findById(clienteId)
            .orElseThrow(() -> BusinessException.notFound("Cliente", clienteId.toString()));

        if (prestamoRepository.existsByClienteIdAndActivoTrue(clienteId)) {
            throw BusinessException.conflict(
                "El cliente " + cliente.getNumero() + " – " + cliente.getNombre() +
                " tiene un préstamo activo. Debe liquidarlo antes de solicitar uno nuevo."
            );
        }

        Prestamo prestamo = crearPrestamo(cliente, request);
        log.info("Nuevo préstamo {} para cliente existente {}", prestamo.getNumero(), cliente.getNumero());
        return prestamo;
    }

    // ── Privados ──────────────────────────────────────────────────

    private Prestamo crearPrestamo(Cliente cliente, ClienteRequest request) {
        BigDecimal pagoSemanal = request.getMonto()
            .multiply(TASA_SEMANAL)
            .setScale(2, RoundingMode.HALF_UP);

        Prestamo prestamo = prestamoRepository.save(
            Prestamo.builder()
                .cliente(cliente)
                .numero(generarNumeroPrestamo())
                .monto(request.getMonto())
                .pagoSemanal(pagoSemanal)
                .fechaInicio(request.getFechaInicio())
                .fechaPrimerPago(request.getFechaPrimerPago())
                .activo(true)
                .build()
        );

        generarCorrida(prestamo, request.getFechaPrimerPago(), pagoSemanal);
        return prestamo;
    }

    private void generarCorrida(Prestamo prestamo, LocalDate fechaPrimerPago, BigDecimal pagoSemanal) {
        LocalDate hoy = LocalDate.now();
        List<Pago> pagos = new ArrayList<>();
        boolean proximoAsignado = false;

        for (int i = 0; i < TOTAL_PAGOS; i++) {
            LocalDate fechaPago = fechaPrimerPago.plusWeeks(i);

            EstadoPago estado;
            if (fechaPago.isBefore(hoy)) {
                estado = EstadoPago.ATRASADO;
            } else if (!proximoAsignado) {
                estado = EstadoPago.PROXIMO;
                proximoAsignado = true;
            } else {
                estado = EstadoPago.PENDIENTE;
            }

            pagos.add(Pago.builder()
                .prestamo(prestamo)
                .numeroPago(i + 1)
                .fechaProgramada(fechaPago)
                .montoProgramado(pagoSemanal)
                .estado(estado)
                .build());
        }
        pagoRepository.saveAll(pagos);
    }

    private String generarNumeroCliente() {
        return String.format("PC-%03d", clienteRepository.findMaxNumeroSecuencia() + 1);
    }

    private String generarNumeroPrestamo() {
        return String.format("PR-%03d", prestamoRepository.findMaxNumeroSecuencia() + 1);
    }
}
