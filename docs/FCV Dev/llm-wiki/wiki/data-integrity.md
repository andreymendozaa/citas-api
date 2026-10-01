# Datos e integridad

## HECHO

El diseño debe alcanzar 3FN, resolver relaciones N:M con tablas puente, evitar duplicación de catálogos y justificar claves, índices, snapshots, auditoría y prevención de doble reserva.

## PREGUNTA ABIERTA

La estrategia concreta de concurrencia, retención de slots, zona horaria y representación de fechas aún no está aprobada.

## DECISIÓN — 2026-09-22 · Modelo de identidad integrado

Flyway V1 usa el modelo 3FN de referencia: `users`, `roles`, `user_roles` y `refresh_tokens`. La relación usuario–rol es N:M mediante puente y cada refresh referencia exactamente un usuario. Email canónico tiene UK global; documento usa UK `(document_type, document_number)`. `token_hash` tiene UK y nunca se guarda el JWT. El índice de usuario y la búsqueda por hash con bloqueo de fila permiten rotación concurrente.

Los atributos personales dependen solo de `users.id`, nombres de rol solo de `roles.id` y vigencia/revocación solo de `refresh_tokens.id`; no hay listas ni dependencias parciales o transitivas entre atributos no clave en este corte. Flyway registra baseline 0 para adoptar una base de referencia ya cargada y ejecuta V1 idempotente.

## HECHO — 2026-09-30 · V5 y afiliación (HU-011)

- **V5 (`V5__insurance_regimes_seed.sql`).** Solo siembra filas en el catálogo fijo `insurance_regimes`, sin cambios de esquema:
  - valores: CONTRIBUTIVO, SUBSIDIADO, ESPECIAL, EXCEPCION, PARTICULAR, tomados del modelo de referencia y aprobados por el usuario;
  - es idempotente por la clave única `code` y no modifica V1–V4.
- **3FN de la afiliación.** `user_insurance_affiliations` guarda solo `user_id` y `plan_id` (FK); la EPS y el régimen se derivan del plan, sin textos de catálogo duplicados.
- **No duplicación (garantizada en `InsuranceJpaAdapter.changeCurrent`, transaccional y con bloqueo de las filas del usuario):**
  - nunca hay dos filas del mismo plan por usuario;
  - a lo sumo una tiene `is_current=true`.
  - La clave única existente `(user_id, plan_id, membership_number)` respalda la regla, porque el número sintético depende del plan.
