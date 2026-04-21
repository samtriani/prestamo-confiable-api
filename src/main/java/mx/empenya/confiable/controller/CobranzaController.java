package mx.empenya.confiable.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.dto.response.CobranzaItemResponse;
import mx.empenya.confiable.service.CobranzaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cobranza")
@RequiredArgsConstructor
@Tag(name = "Cobranza", description = "Vista semanal de pagos pendientes y vencidos")
public class CobranzaController {

    private final CobranzaService cobranzaService;

    @Operation(summary = "Pagos PROXIMO y ATRASADO — lista de cobranza semanal")
    @GetMapping("/semana")
    public ResponseEntity<List<CobranzaItemResponse>> getSemana() {
        return ResponseEntity.ok(cobranzaService.getCobranzaSemana());
    }
}
