package mx.empenya.confiable.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.dto.request.CorteRequest;
import mx.empenya.confiable.dto.response.CorteAbonoItem;
import mx.empenya.confiable.dto.response.CorteDetalleResponse;
import mx.empenya.confiable.entity.Corte;
import mx.empenya.confiable.service.CorteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cortes")
@RequiredArgsConstructor
@Tag(name = "Cortes", description = "Cortes semanales con histórico completo")
public class CorteController {

    private final CorteService corteService;

    @Operation(
        summary = "Realizar corte semanal",
        description = """
            Cierra el periodo semanal:
            - Suma todos los abonos pendientes (naranja)
            - Crea el registro histórico del corte
            - Cambia esos abonos a verde (PAGADO)
            - Reinicia el acumulado semanal a $0
            """
    )
    @PostMapping
    public ResponseEntity<Corte> realizarCorte(@RequestBody(required = false) CorteRequest request) {
        if (request == null) request = new CorteRequest();
        return ResponseEntity.status(HttpStatus.CREATED).body(corteService.realizarCorte(request));
    }

    @Operation(summary = "Histórico de todos los cortes")
    @GetMapping
    public ResponseEntity<List<CorteDetalleResponse>> findHistorico() {
        return ResponseEntity.ok(corteService.findHistorico());
    }

    @Operation(summary = "Detalle de un corte")
    @GetMapping("/{id}")
    public ResponseEntity<CorteDetalleResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(corteService.findById(id));
    }

    @Operation(summary = "Abonos detallados de un corte — para reporte PDF")
    @GetMapping("/{id}/abonos")
    public ResponseEntity<List<CorteAbonoItem>> findAbonos(@PathVariable UUID id) {
        return ResponseEntity.ok(corteService.findAbonosDetalle(id));
    }
}
