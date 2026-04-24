package mx.empenya.confiable.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.dto.response.ReporteResponse;
import mx.empenya.confiable.service.ReportesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Reportes financieros y de cartera")
public class ReportesController {

    private final ReportesService reportesService;

    @Operation(summary = "Reporte financiero completo — resumen, series mensuales y top deudores")
    @GetMapping
    public ResponseEntity<ReporteResponse> getReporte() {
        return ResponseEntity.ok(reportesService.getReporte());
    }
}
