package mx.empenya.confiable.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.dto.request.CreateUsuarioRequest;
import mx.empenya.confiable.dto.request.UpdateUsuarioRequest;
import mx.empenya.confiable.dto.response.UsuarioResponse;
import mx.empenya.confiable.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de usuarios — solo ADMIN")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Listar todos los usuarios")
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> findAll() {
        return ResponseEntity.ok(usuarioService.findAll());
    }

    @Operation(summary = "Crear usuario")
    @PostMapping
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody CreateUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.create(request));
    }

    @Operation(summary = "Editar usuario (nombre, rol, contraseña)")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.update(id, request));
    }

    @Operation(summary = "Activar / desactivar usuario")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<UsuarioResponse> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.toggleActivo(id));
    }
}
