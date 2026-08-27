package mx.empenya.confiable.enums;

public enum EstadoPago {
    PENDIENTE,          // pago futuro, sin vencer
    PROXIMO,            // siguiente pago a cobrar (azul)
    ABONO_PARCIAL,      // tiene abonos pero no cubre el monto programado (amarillo)
    PAGADO_SIN_CORTE,   // pagado pero aún no incluido en corte semanal (naranja)
    PAGADO,             // pagado y confirmado en corte semanal (verde)
    ATRASADO            // fecha venció y no tiene ningún abono (rojo)
}
