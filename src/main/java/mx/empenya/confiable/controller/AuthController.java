package mx.empenya.confiable.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.dto.request.LoginRequest;
import mx.empenya.confiable.dto.response.LoginResponse;
import mx.empenya.confiable.dto.response.PagoDetalleResponse;
import mx.empenya.confiable.dto.response.PrestamoActivoResponse;
import mx.empenya.confiable.entity.Usuario;
import mx.empenya.confiable.service.AuthService;
import mx.empenya.confiable.service.ClienteService;
import mx.empenya.confiable.service.PagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticación JWT")
public class AuthController {

    private final AuthService    authService;
    private final ClienteService clienteService;
    private final PagoService    pagoService;

    @Operation(summary = "Login — devuelve JWT")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Mi crédito activo — solo rol CLIENTE")
    @GetMapping("/mi-credito")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PrestamoActivoResponse> miCredito(@AuthenticationPrincipal Usuario usuario) {
        return prestamoActivoDe(usuario)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Corrida de pagos de mi crédito — solo rol CLIENTE",
        description = "Incluye el desglose de abonos de cada pago para que el "
                    + "cliente vea de cuánto fue cada abono parcial."
    )
    @GetMapping("/mi-credito/pagos")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<PagoDetalleResponse>> miCreditoPagos(@AuthenticationPrincipal Usuario usuario) {
        return prestamoActivoDe(usuario)
            .map(p -> ResponseEntity.ok(pagoService.findByPrestamoConAbonos(p.getId())))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Préstamo activo del cliente asociado al usuario autenticado. */
    private java.util.Optional<PrestamoActivoResponse> prestamoActivoDe(Usuario usuario) {
        if (usuario.getClienteId() == null) {
            return java.util.Optional.empty();
        }
        return clienteService.findHistorialByClienteId(usuario.getClienteId())
            .stream()
            .filter(p -> Boolean.TRUE.equals(p.getActivo()))
            .findFirst();
    }
}
