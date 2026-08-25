package mx.empenya.confiable.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.dto.response.PagoDetalleResponse;
import mx.empenya.confiable.entity.Pago;
import mx.empenya.confiable.enums.EstadoPago;
import mx.empenya.confiable.exception.BusinessException;
import mx.empenya.confiable.repository.PagoRepository;
import mx.empenya.confiable.service.AbonoService;
import mx.empenya.confiable.service.PagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Consulta de corridas de pago por préstamo")
public class PagoController {

    private final PagoRepository pagoRepository;
    private final AbonoService  abonoService;
    private final PagoService   pagoService;

    @Operation(
        summary = "Obtener los 14 pagos de un préstamo",
        description = "Cada pago viene con totalAbonado, saldoPendiente y numAbonos "
                    + "para poder identificar los pagos cubiertos parcialmente."
    )
    @GetMapping("/prestamo/{prestamoId}")
    public ResponseEntity<List<PagoDetalleResponse>> findByPrestamo(@PathVariable UUID prestamoId) {
        return ResponseEntity.ok(pagoService.findByPrestamoConDetalle(prestamoId));
    }

    @Operation(summary = "Obtener un pago por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Pago> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(
            pagoRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Pago", id.toString()))
        );
    }

    @Operation(summary = "Pagos por estado (ATRASADO, PROXIMO, PAGADO, etc.)")
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Pago>> findByEstado(@PathVariable EstadoPago estado) {
        return ResponseEntity.ok(pagoRepository.findByEstadoOrderByFechaProgramadaAsc(estado));
    }
}
