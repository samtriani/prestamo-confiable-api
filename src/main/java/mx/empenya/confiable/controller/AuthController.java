package mx.empenya.confiable.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.dto.request.LoginRequest;
import mx.empenya.confiable.dto.response.LoginResponse;
import mx.empenya.confiable.dto.response.PrestamoActivoResponse;
import mx.empenya.confiable.entity.Usuario;
import mx.empenya.confiable.service.AuthService;
import mx.empenya.confiable.service.ClienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticación JWT")
public class AuthController {

    private final AuthService   authService;
    private final ClienteService clienteService;

    @Operation(summary = "Login — devuelve JWT")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Mi crédito activo — solo rol CLIENTE")
    @GetMapping("/mi-credito")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PrestamoActivoResponse> miCredito(@AuthenticationPrincipal Usuario usuario) {
        if (usuario.getClienteId() == null) {
            return ResponseEntity.notFound().build();
        }
        return clienteService.findHistorialByClienteId(usuario.getClienteId())
            .stream()
            .filter(p -> Boolean.TRUE.equals(p.getActivo()))
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
