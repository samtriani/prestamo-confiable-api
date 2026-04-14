-- ============================================================
-- EMPEÑA CONFIABLE — Schema PostgreSQL (Neon)
-- Ejecutar en orden en Neon SQL Editor
-- ============================================================

-- Extensión para UUIDs
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- CLIENTES
-- ============================================================
CREATE TABLE clientes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero      VARCHAR(8)   NOT NULL UNIQUE,  -- PC-001, PC-002 ...
    nombre      VARCHAR(150) NOT NULL,
    telefono    VARCHAR(20),
    domicilio   TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- PRÉSTAMOS
-- ============================================================
CREATE TABLE prestamos (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id        UUID         NOT NULL REFERENCES clientes(id),
    numero            VARCHAR(8)   NOT NULL UNIQUE,  -- PC-001 (mismo que cliente en primer préstamo, luego PC-001-2)
    monto             DECIMAL(12,2) NOT NULL,
    pago_semanal      DECIMAL(12,2) NOT NULL,         -- monto * 0.10
    fecha_inicio      DATE         NOT NULL,
    fecha_primer_pago DATE         NOT NULL,           -- primer sábado de pago
    activo            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- PAGOS (14 registros por préstamo, generados automáticamente)
-- ============================================================
CREATE TABLE pagos (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    prestamo_id       UUID         NOT NULL REFERENCES prestamos(id),
    numero_pago       INTEGER      NOT NULL CHECK (numero_pago BETWEEN 1 AND 14),
    fecha_programada  DATE         NOT NULL,
    monto_programado  DECIMAL(12,2) NOT NULL,
    estado            VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    -- PENDIENTE       → pago futuro sin vencer
    -- PROXIMO         → siguiente pago a cobrar
    -- PAGADO_SIN_CORTE → pagado pero aún no entra al corte (naranja)
    -- PAGADO          → pagado y ya en corte semanal (verde)
    -- ATRASADO        → fecha venció y no está pagado completo (rojo)
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (prestamo_id, numero_pago)
);

-- ============================================================
-- CORTES SEMANALES
-- ============================================================
CREATE TABLE cortes (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    fecha_corte    DATE          NOT NULL,
    total_semanal  DECIMAL(12,2) NOT NULL DEFAULT 0,
    num_abonos     INTEGER       NOT NULL DEFAULT 0,
    descripcion    TEXT,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ============================================================
-- ABONOS (pagos parciales o completos por pago)
-- ============================================================
CREATE TABLE abonos (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    pago_id      UUID          NOT NULL REFERENCES pagos(id),
    corte_id     UUID          REFERENCES cortes(id),   -- NULL = pendiente de corte (naranja)
    monto_abono  DECIMAL(12,2) NOT NULL,
    fecha_abono  TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ============================================================
-- USUARIOS Y ROLES
-- ============================================================
-- Ejecutar SOLO si no existe aún:
-- CREATE TYPE rol_usuario AS ENUM ('ADMIN', 'OPERADOR');

CREATE TYPE rol_usuario AS ENUM ('ADMIN', 'OPERADOR');

CREATE TABLE usuarios (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,   -- BCrypt hash
    nombre      VARCHAR(150) NOT NULL,
    rol         rol_usuario  NOT NULL DEFAULT 'OPERADOR',
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- ÍNDICES
-- ============================================================
CREATE INDEX idx_prestamos_cliente_id  ON prestamos(cliente_id);
CREATE INDEX idx_pagos_prestamo_id     ON pagos(prestamo_id);
CREATE INDEX idx_pagos_estado          ON pagos(estado);
CREATE INDEX idx_abonos_pago_id        ON abonos(pago_id);
CREATE INDEX idx_abonos_corte_id       ON abonos(corte_id);
CREATE INDEX idx_abonos_sin_corte      ON abonos(pago_id) WHERE corte_id IS NULL;

-- ============================================================
-- VISTA: resumen de pagos con suma de abonos
-- ============================================================
CREATE OR REPLACE VIEW v_pagos_resumen AS
SELECT
    p.id,
    p.prestamo_id,
    p.numero_pago,
    p.fecha_programada,
    p.monto_programado,
    p.estado,
    COALESCE(SUM(a.monto_abono), 0)                                    AS total_abonado,
    COALESCE(SUM(a.monto_abono) FILTER (WHERE a.corte_id IS NOT NULL), 0) AS total_en_corte,
    p.monto_programado - COALESCE(SUM(a.monto_abono), 0)               AS saldo_pendiente,
    COUNT(a.id)                                                         AS num_abonos
FROM pagos p
LEFT JOIN abonos a ON a.pago_id = p.id
GROUP BY p.id, p.prestamo_id, p.numero_pago, p.fecha_programada, p.monto_programado, p.estado;

-- ============================================================
-- VISTA: dashboard general
-- ============================================================
CREATE OR REPLACE VIEW v_dashboard AS
SELECT
    (SELECT COUNT(*) FROM clientes)                                          AS total_clientes,
    (SELECT COUNT(*) FROM prestamos WHERE activo = TRUE)                     AS prestamos_activos,
    (SELECT COALESCE(SUM(monto), 0) FROM prestamos)                         AS total_prestado_historico,
    (SELECT COALESCE(SUM(monto_abono), 0) FROM abonos)                      AS total_recuperado,
    (SELECT COALESCE(SUM(a.monto_abono), 0) FROM abonos a WHERE a.corte_id IS NULL) AS total_semanal_actual,
    (SELECT COUNT(*) FROM pagos WHERE estado = 'ATRASADO')                  AS pagos_atrasados;
