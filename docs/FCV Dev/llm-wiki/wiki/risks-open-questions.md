# Riesgos y preguntas abiertas

- (2026-10-04) **Riesgos residuales S5/S6 (n8n, MCP, contenido no confiable):** R1–R8 en `evidence/S5-MCP-contenido-no-confiable-y-riesgos.md`. Los principales: cuenta n8n compartida (el token MCP actúa sobre toda la cuenta), prompt injection vía contenido de terceros, scopes amplios de Gmail OAuth y túnel público temporal. OpenAPI (HU-004) quedó resuelto el 2026-10-04.

- ~~`citas-web` contiene cambios locales aún no versionados~~ — resuelto: todo está versionado y empujado a `develop` (2026-09-30).
- ~~No está definido el conjunto de estados y transiciones~~ — resuelto: catálogo sembrado en V2/V3 y transiciones centralizadas en `SchedulingJdbcAdapter`.
- ~~Falta estrategia de exclusión concurrente de slots~~ — resuelto: asignación condicional transaccional, probada en `concurrentReservationsProduceExactlyOneAppointmentAndOneConflict`.
- ~~Falta política de zona horaria~~ — resuelto: reloj de negocio `America/Bogota`.
- ~~(2026-09-30) Cancelación y reprogramación (HU-026 a 028) sin pruebas dedicadas~~ — resuelto el mismo día con `AppointmentLifecycleIntegrationTest`.
- ~~(2026-09-30) `SchedulingJdbcAdapter.decide` mantiene el patrón `queryForMap` que causaba errores 500~~ — confirmado y corregido el mismo día (`SpecializedDecisionIntegrationTest`). Ya no quedan `queryForMap` en el backend; solo `queryForObject` sobre `count(*)`, que siempre devuelve una fila.
- ~~(2026-09-30) La base de pruebas Maven es persistente y algunas suites asumen ser las únicas con citas `REQUESTED` (`pending()` con un solo elemento, `$[0]`).~~ Resuelto el 2026-10-04 con LOOP_03 (`evidence/LOOP_03-pruebas-fragiles.md`): las aserciones filtran por su propia cita. La suite pasa 2 veces con datos ajenos sembrados (`scripts/loop03-seed-foreign-data.sql`) y ya no los modifica.
- (2026-09-30) Brechas abiertas:
  - edición de nombre y duración de especialidades en la UI (mejora fuera de los CA de HU-014);
  - OpenAPI (HU-004);
  - Actuator.

  Resueltas el mismo día: catálogo de estados de reprogramación (HU-003), filtro por tipo de cita (HU-021), reasignación de profesionales con primaria (HU-016) y afiliación posterior al registro (HU-011).
- ~~(2026-09-30) El catálogo de regímenes solo contiene `PARTICULAR`~~ — resuelto con la V5 (5 regímenes del modelo de referencia, aprobados por el usuario).
- (2026-09-30) `main` no se ha actualizado desde S2 en ninguno de los tres repositorios; el merge `develop → main` es entregable de S4/S6.
- (2026-09-30) Repositorio raíz `citas` publicado pese a la restricción de dos repos públicos y a "No inicializar Git en la raíz" (`AGENTS.md`). Pendiente de decisión del usuario.
- Es ambiguo si reservas `REQUESTED` o reprogramaciones `PENDING` bloquean la edición de bloques.
- Falta lista completa de catálogos fijos y semillas.
- Falta política de afiliación activa/histórica.
- Los refresh tokens de HU-007 ya tienen rotación, expiración, revocación y almacenamiento definidos; queda pendiente la política futura para gestión multidispositivo.
- El contrato REST de HU-005/006/007 está aprobado; faltan los contratos de las demás HU.
- React 19 + TypeScript + Vite es el framework frontend detectado; falta evidencia de aprobación visual y cierre cross-repo de HU-033.
- n8n necesita contrato de eventos, idempotencia, reintentos y autenticación de webhook.
- (2026-09-30) Las pantallas de S4 (recuperación, perfil, EPS/planes, agenda profesional, bandeja, historial) no tienen referencia Stitch/AI Studio aprobada; se construyeron extendiendo el estilo existente y requieren aprobación visual posterior.
- (2026-09-30) El hash BCrypt de `database/seed-lab-scheduling.sql` no corresponde a la contraseña documentada `Demo1234*` (`database/reference/README_DB.md`), así que las cuentas `lab.*@citas.test` no inician sesión con ella. Para la validación del 2026-09-30 se reasignó localmente un hash conocido solo en la BD de desarrollo. Pendiente: corregir el seed o la documentación.
- La Skill `scrum-spec-orchestrator` conserva una allowlist interna para `docs/wiki/scrum/`; debe actualizarse explícitamente antes de usarla con `docs/FCV Dev/scrum/`.
