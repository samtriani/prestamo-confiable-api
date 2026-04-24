package mx.empenya.confiable.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ReporteResponse {

    // ── Financiero ─────────────────────────────────────────────────
    private BigDecimal totalPrestado;
    private BigDecimal totalRecuperado;
    private BigDecimal gananciaNeta;           // recuperado - prestado
    private BigDecimal pendienteSiLiquidan;    // saldo restante de cartera activa
    private BigDecimal proyeccionGanancia;     // ganancia total si todos liquidan

    // ── Cartera ────────────────────────────────────────────────────
    private long prestamosActivos;
    private long prestamosAtrasados;
    private long prestamosAlCorriente;
    private long totalClientes;

    // ── Series por mes ─────────────────────────────────────────────
    private List<MesDato> prestamosPorMes;
    private List<MesDato> abonosPorMes;

    // ── Top deudores (mayor saldo pendiente) ───────────────────────
    private List<TopDeudor> topSaldos;

    @Data
    @Builder
    public static class MesDato {
        private String mes;        // "2024-01"
        private BigDecimal monto;
        private int cantidad;
    }

    @Data
    @Builder
    public static class TopDeudor {
        private String clienteNumero;
        private String clienteNombre;
        private BigDecimal saldoPendiente;
        private BigDecimal totalAbonado;
        private int pagosAtrasados;
    }
}
