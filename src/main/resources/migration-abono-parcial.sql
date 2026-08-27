-- ============================================================
-- Migración: estado ABONO_PARCIAL
-- ============================================================
-- Hasta ahora un pago con abonos insuficientes quedaba en ATRASADO,
-- indistinguible de uno sin un solo peso encima. Este script reclasifica
-- los que ya existen en la base.
--
-- pagos.estado es un ENUM nativo de Postgres (tipo estado_pago), así que el
-- valor nuevo se agrega con ALTER TYPE antes de poder usarlo.
--
-- Ejecutar UNA sola vez, DESPUÉS de desplegar la API: la versión anterior
-- hace EstadoPago.valueOf() sobre lo que lee y truena con
-- IllegalArgumentException al toparse con un estado que no conoce.

-- ── Paso 1: agregar el valor al tipo ──────────────────────────────
-- Va SOLO y confirmado aparte: Postgres no permite usar un valor de enum
-- en la misma transacción en que se agregó.
ALTER TYPE estado_pago ADD VALUE IF NOT EXISTS 'ABONO_PARCIAL' BEFORE 'PAGADO_SIN_CORTE';

-- ── Paso 2: reclasificar los pagos existentes ─────────────────────
BEGIN;

-- Revisión previa: qué pagos van a cambiar de estado.
SELECT p.id, pr.numero AS prestamo, p.numero_pago, p.estado,
       p.monto_programado, COALESCE(SUM(a.monto_abono), 0) AS abonado
FROM pagos p
JOIN prestamos pr ON pr.id = p.prestamo_id
LEFT JOIN abonos a ON a.pago_id = p.id
GROUP BY p.id, pr.numero, p.numero_pago, p.estado, p.monto_programado
HAVING COALESCE(SUM(a.monto_abono), 0) > 0
   AND COALESCE(SUM(a.monto_abono), 0) < p.monto_programado
ORDER BY pr.numero, p.numero_pago;

-- Reclasificación.
UPDATE pagos p
SET    estado     = 'ABONO_PARCIAL',
       updated_at = NOW()
FROM (
    SELECT a.pago_id, SUM(a.monto_abono) AS abonado
    FROM abonos a
    GROUP BY a.pago_id
) t
WHERE t.pago_id = p.id
  AND t.abonado > 0
  AND t.abonado < p.monto_programado
  -- Los que ya pasaron por corte se quedan como están: su historia
  -- contable ya está cerrada y reabrirla movería reportes viejos.
  AND p.estado IN ('ATRASADO', 'PROXIMO', 'PENDIENTE');

COMMIT;
