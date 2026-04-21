package mx.empenya.confiable.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.dto.request.AbonoRequest;
import mx.empenya.confiable.entity.Abono;
import mx.empenya.confiable.service.AbonoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/abonos")
@RequiredArgsConstructor
@Tag(name = "Abonos", description = "Registro de pagos totales o parciales")
public class AbonoController {

    private final AbonoService abonoService;

    @Operation(
        summary = "Registrar abono",
        description = "Registra un pago total o parcial. El estado del pago se recalcula automáticamente."
    )
    @PostMapping
    public ResponseEntity<Abono> registrar(@Valid @RequestBody AbonoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(abonoService.registrarAbono(request));
    }

    @Operation(summary = "Abonos de un pago específico")
    @GetMapping("/pago/{pagoId}")
    public ResponseEntity<List<Abono>> findByPago(@PathVariable UUID pagoId) {
        return ResponseEntity.ok(abonoService.findByPagoId(pagoId));
    }

    @Operation(summary = "Abonos pendientes de corte (naranja)")
    @GetMapping("/pendientes")
    public ResponseEntity<List<Abono>> findPendientes() {
        return ResponseEntity.ok(abonoService.findPendientesDeCorte());
    }

    @Operation(summary = "Total acumulado semana actual (antes del corte)")
    @GetMapping("/total-semanal")
    public ResponseEntity<Map<String, BigDecimal>> totalSemanal() {
        return ResponseEntity.ok(Map.of("totalSemanal", abonoService.getTotalSemanalActual()));
    }
}
