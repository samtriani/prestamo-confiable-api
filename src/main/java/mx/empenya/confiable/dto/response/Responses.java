package mx.empenya.confiable.dto.response;

import lombok.Builder;
import lombok.Data;
import mx.empenya.confiable.enums.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// ── Cliente ──────────────────────────────────────────────────
@Data
@Builder
class ClienteResponse {
    private UUID id;
    private String numero;
    private String nombre;
    private String telefono;
    private String domicilio;
    private LocalDateTime createdAt;
    private List<PrestamoResumenResponse> prestamos;
}

// ── Préstamo (resumen en lista de cliente) ────────────────────
@Data
@Builder
class PrestamoResumenResponse {
    private UUID id;
    private String numero;
    private BigDecimal monto;
    private BigDecimal pagoSemanal;
    private LocalDate fechaInicio;
    private LocalDate fechaPrimerPago;
    private Boolean activo;
    private int pagosRealizados;
    private int totalPagos;
    private BigDecimal totalAbonado;
    private BigDecimal saldoPendiente;
}

// ── Préstamo (detalle completo) ────────────────────────────────
@Data
@Builder
class PrestamoDetalleResponse {
    private UUID id;
    private String numero;
    private String clienteNumero;
    private String clienteNombre;
    private String clienteTelefono;
    private BigDecimal monto;
    private BigDecimal pagoSemanal;
    private BigDecimal totalARecuperar; // monto * 14 * 0.10 = monto * 1.4
    private LocalDate fechaInicio;
    private LocalDate fechaPrimerPago;
    private Boolean activo;
    private List<PagoResponse> pagos;
}

// ── Pago ──────────────────────────────────────────────────────
@Data
@Builder
class PagoResponse {
    private UUID id;
    private Integer numeroPago;
    private LocalDate fechaProgramada;
    private BigDecimal montoProgramado;
    private EstadoPago estado;
    private String colorEstado; // VERDE, ROJO, NARANJA, AZUL, GRIS
    private BigDecimal totalAbonado;
    private BigDecimal saldoPendiente;
    private List<AbonoResponse> abonos;
}

// ── Abono ─────────────────────────────────────────────────────
@Data
@Builder
class AbonoResponse {
    private UUID id;
    private BigDecimal montoAbono;
    private LocalDateTime fechaAbono;
    private boolean enCorte;
    private UUID corteId;
}

// ── Corte ─────────────────────────────────────────────────────
@Data
@Builder
class CorteResponse {
    private UUID id;
    private LocalDate fechaCorte;
    private BigDecimal totalSemanal;
    private Integer numAbonos;
    private String descripcion;
    private LocalDateTime createdAt;
}

// ── Dashboard ─────────────────────────────────────────────────
@Data
@Builder
class DashboardResponse {
    private long totalClientes;
    private long prestamosActivos;
    private BigDecimal totalPrestadoHistorico;
    private BigDecimal totalRecuperado;
    private BigDecimal totalSemanalActual;   // acumulado antes del próximo corte
    private long pagosAtrasados;
}
