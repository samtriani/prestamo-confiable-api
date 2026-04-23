package mx.empenya.confiable.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.dto.request.ClienteRequest;
import mx.empenya.confiable.dto.request.ClienteUpdateRequest;
import mx.empenya.confiable.dto.response.PrestamoActivoResponse;
import mx.empenya.confiable.entity.Cliente;
import mx.empenya.confiable.entity.Prestamo;
import mx.empenya.confiable.service.ClienteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Alta y consulta de clientes con su préstamo inicial")
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(summary = "Listar todos los clientes")
    @GetMapping
    public ResponseEntity<List<Cliente>> findAll() {
        return ResponseEntity.ok(clienteService.findAll());
    }

    @Operation(summary = "Obtener cliente por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Cliente> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(clienteService.findById(id));
    }

    @Operation(summary = "Obtener cliente por número (PC-001)")
    @GetMapping("/numero/{numero}")
    public ResponseEntity<Cliente> findByNumero(@PathVariable String numero) {
        return ResponseEntity.ok(clienteService.findByNumero(numero));
    }

    @Operation(
        summary = "Alta de cliente",
        description = "Registra el cliente y genera automáticamente el préstamo con su corrida de 14 pagos semanales al 10%"
    )
    @PostMapping
    public ResponseEntity<Cliente> alta(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.altaCliente(request));
    }

    @Operation(summary = "Actualizar teléfono y/o domicilio de un cliente")
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ClienteUpdateRequest request) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, request));
    }

    @Operation(summary = "Préstamos activos de todos los clientes")
    @GetMapping("/prestamos/activos")
    public ResponseEntity<List<PrestamoActivoResponse>> prestamosActivos() {
        return ResponseEntity.ok(clienteService.findPrestamosActivos());
    }

    @Operation(
        summary = "Historial de préstamos de un cliente",
        description = "Devuelve todos los préstamos del cliente (activos e históricos), del más reciente al más antiguo."
    )
    @GetMapping("/{id}/historial")
    public ResponseEntity<List<PrestamoActivoResponse>> historial(@PathVariable UUID id) {
        return ResponseEntity.ok(clienteService.findHistorialByClienteId(id));
    }

    @Operation(
        summary = "Nuevo préstamo para cliente existente",
        description = "Solo permitido si el cliente NO tiene préstamos activos. " +
                      "Lanza 409 Conflict si intenta crear uno mientras tiene otro vigente."
    )
    @PostMapping("/{id}/prestamos")
    public ResponseEntity<Prestamo> nuevoPrestamo(
            @PathVariable UUID id,
            @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(clienteService.nuevoPrestamo(id, request));
    }
}
