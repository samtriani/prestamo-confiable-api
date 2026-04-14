package mx.empenya.confiable.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Métricas generales del negocio")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Resumen general del negocio")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }
}
