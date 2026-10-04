# Trazabilidad

## HECHO

Las sesiones S2-S6 requieren commits y evidencias específicas. El backend y frontend deben mantener historial trazable; las pruebas y la evidencia cross-repo son parte de la evaluación.

## HECHO — 2026-09-17

Las HU-001 a HU-036 existen en `docs/FCV Dev/scrum/`. HU-001/002/003 tienen avance parcial; HU-004 define por ahora solo el contrato de identidad. La implementación backend de HU-005/006/007 cuenta con `AuthIntegrationTest`, `IdentityTest` y `AuthRequestGuardTest`; la integración web se controla mediante HU-033.

## HECHO — 2026-09-22

El catálogo de ocho subagentes fue versionado en `docs/FCV Dev/subagents/` y enlazado desde el orquestador. `citas-web` contiene trabajo local React/Vite de autenticación y pruebas; no debe declararse completado hasta ejecutar build, typecheck, tests y verificación cross-repo.

## HECHO — 2026-09-22 · Integración de autenticación

El prototipo `citas-web/portal-de-citas.zip` se importó como React/Vite y se integró con HU-005/006/007. La comprobación usa MySQL persistente, CORS explícito, registro/login/refresh/logout reales y pruebas de frontend. HU-033 permanece en progreso porque las pantallas de perfil, agenda y roles posteriores siguen fuera del corte de autenticación.

## HECHO — 2026-09-29 · Cierre técnico S2/S3 verificado

El servicio de aplicación de agenda depende de `Ports.Scheduling`; las operaciones JDBC viven en `SchedulingJdbcAdapter`. El reloj de negocio se configuró con `America/Bogota`. La suite backend verificó 19 pruebas, incluidas reserva concurrente, aprobación/rechazo con historial, retención/liberación de slots, edición/eliminación de bloques futuros, profesional/sede/especialidad inválidos, catálogo tipado de profesionales, autorización de rutas S3 y compatibilidad MockMvc para reserva general/especializada y decisión ADMIN. El frontend verificó 16 pruebas, `lint` y `build`, incluyendo los contratos de reserva y bloques, adaptación de slots API, mensajes `403`/`409`, confirmación `APPROVED` y pantallas ADMIN/PROFESSIONAL. El caso rojo de concurrencia pasó a verde tras proteger la asignación condicional de slots; el hook frontend y backend registran FAIL/PASS con secretos sintéticos. El Verifier reutilizable se ejecutó inmediatamente después de cada Prompt y registró su decisión PASS/FAIL. Este cierre técnico no declara completadas HU-003, HU-004, HU-011 ni HU-014 a HU-024: todavía requieren la cobertura y evidencia integral exigidas por sus DoD.

## HECHO — 2026-09-30 · Revisión integral de requerimientos (PRD, restricciones, guía S2–S6, Scrum)

Revisión solicitada por el usuario sobre `citas`, `citas-api` y `citas-web`, contrastando la documentación con el código y las pruebas reales. Evidencia base: Maven 40/40 y Vitest 34/34 en `develop`.

**Requerimientos funcionales del PRD.**
- Completos (16): RF-01, 02, 03, 08, 09, 11, 12, 13, 14, 15, 16, 17, 18 y 19.
- Parciales:
  - RF-04: la afiliación solo se elige al registrarse, sin consulta ni cambio posterior (HU-011).
  - RF-05: los estados de reprogramación están sembrados pero no se exponen.
  - RF-06: la UI de especialidades no edita nombre ni duración.
  - RF-07: la UI no reasigna especialidades o sedes tras el alta ni elige la especialidad primaria.
  - RF-10: falta el filtro por tipo general/especializada.
  - RF-20: falta publicar OpenAPI.
- RN-01 a RN-12 y la seguridad mínima están cubiertas.
- Desviación documentada: RF-19 lista las fuentes `SYSTEM/USER/ADMIN`, y el cierre de atención usa además `PROFESSIONAL`.
- Recomendación de `RESTRICCIONES_TECNICAS.md` no aplicada: Actuator health no está en el `pom.xml`.

**Trazabilidad Scrum.** Se auditaron CA/DoD de HU-003, HU-004 y HU-014 a HU-028 con este criterio:
- PASS solo con prueba automatizada o propiedad estructural;
- lo verificado solo por código o manualmente queda PARCIAL/PENDIENTE.

Resultado:
- HU-017, HU-022 y HU-025 pasan a `Completada`.
- Las otras 14 quedan `En desarrollo`, con el faltante explícito en su tabla de evidencia.
- Estado global: 17 `Completada`, 14 `En desarrollo`, 1 `Aprobada` (HU-011), 1 `En progreso` (HU-033), 3 `Pendiente de aprobación` (HU-034 a 036).
- Hallazgo principal: la cancelación y la reprogramación (HU-026 a 028) solo tienen pruebas indirectas; el código implementa sus guardas, pero no hay pruebas dedicadas de slots, ownership ni estados inválidos.

**Entregables S2–S6 pendientes.**
- Evidencia de aprobación visual Stitch/AI Studio de todas las pantallas.
- Evidencia del LOOP propio del estudiante (`LOOP_03`) y sus logs.
- Merge `develop → main` en los tres repositorios: `main` está 12/16/8 commits atrás en `citas`/`citas-api`/`citas-web`.
- WF-001 y WF-002 exportados en JSON: hoy solo existen especificaciones `.md` en `automations/n8n/`.
- Evidencia de MCP, documento de riesgos residuales, Swagger, `current-state.md` y webhook real de n8n.

**HECHO — actualización del mismo día · pruebas de cancelación y reprogramación.** Se añadió `AppointmentLifecycleIntegrationTest` (8 casos REST + BD sobre slots, ownership, roles, estados inválidos y auditoría) y 4 pruebas de cliente en `DashboardScreen.test.tsx`.

Siguiendo Red → Green, 3 casos fallaron por `EmptyResultDataAccessException` sin manejar (error 500) en cita ajena/inexistente y en solicitud inexistente/ya resuelta. Se corrigió en `SchedulingJdbcAdapter` para responder `404`/`409`.

También apareció contaminación de la base de pruebas persistente: dos suites existentes asumen ser las únicas con citas `REQUESTED`. La nueva prueba resuelve su propia solicitud y se limpiaron las filas huérfanas del esquema `_test`.

Resultado:
- Maven 48/48 en dos ejecuciones consecutivas.
- HU-026, HU-027 y HU-028 pasan a `Completada`.
- Estado global: 20 `Completada` y 11 `En desarrollo`.

**HECHO — segunda actualización · decisión de solicitudes especializadas.** Se añadió `SpecializedDecisionIntegrationTest` (3 casos). HU-023 CA-03 (historial inicial `REQUESTED`/`USER`) pasó directamente. HU-024 CA-03 reprodujo el mismo error 500 en `decide` para citas inexistentes, generales o ya resueltas. Se corrigió siguiendo Red → Green (`404`/`409`) y se verificó que no quedan consultas de fila única sin manejar.

La prueba usa `@AfterEach` para resolver sus citas `REQUESTED` aunque falle: la ejecución Red no dejó residuos. Maven 51/51 en dos ejecuciones consecutivas.

Estado global: 22 `Completada` y 9 `En desarrollo` (HU-003, 004, 014, 015, 016, 018, 019, 020, 021).

**HECHO — tercera actualización · reglas de oferta y agenda.** Se añadió `OfferAndAgendaRulesIntegrationTest` (7 casos), que cubre:
- HU-014: duración 30/60 en alta y `PATCH`.
- HU-015: alta solo por ADMIN; duplicados sin filas parciales, con reversión transaccional del usuario.
- HU-016: especialidad primaria única y especialidad no asociada.
- HU-018: dos franjas sin el hueco intermedio, también para 60 min.
- HU-019: bloques ajenos, comprometidos y pasados.
- HU-020: filtros de fecha y sede.

Siguiendo Red → Green, solo falló HU-019: un bloque de un día ya pasado podía moverse al futuro y eliminarse. `updateBlock/deleteBlock` responden ahora `409`.

Resultado:
- Maven 58/58.
- Estado global: 27 `Completada` y 4 `En desarrollo`: HU-003 y HU-021 por funcionalidad, HU-016 por UI, HU-004 por OpenAPI.
- Ninguna HU queda pendiente solo por falta de pruebas.

**HECHO — cuarta actualización · funcionales pequeñas.** Se cerraron tres HU:
- HU-003: endpoint de estados de reprogramación y `FixedCatalogsIntegrationTest` (3 casos).
- HU-016: `primarySpecialtyId` en el listado ADMIN, selector explícito de primaria y editor de asignaciones; la lista ahora muestra nombres en lugar de ids.
- HU-021: filtro por tipo de cita en la reserva.

Todas las pruebas nuevas pasaron en la primera ejecución.

Estado global: 30 `Completada`, 1 `En desarrollo` (HU-004, OpenAPI/S5), 1 `Aprobada` (HU-011, afiliación posterior al registro), 1 `En progreso` (HU-033) y 3 `Pendiente de aprobación` (HU-034 a 036, n8n).

En las brechas del PRD quedan resueltos RF-05, RF-07 (UI) y RF-10. Siguen abiertos:
- RF-04: afiliación, HU-011.
- RF-06 (UI): edición de nombre y duración de especialidades.
- RF-20: OpenAPI.

**HECHO — quinta actualización · HU-011 y cierre de requerimientos funcionales.** HU-011 pasa a `Completada`:
- consulta y cambio de la afiliación propia, sin duplicar planes;
- 5 regímenes sembrados con la V5;
- `AffiliationIntegrationTest` (5 casos) y 3 pruebas de cliente;
- validación en vivo en Chrome.

Con esto, todos los RF funcionales del PRD (RF-01 a RF-19) quedan implementados y probados. RF-20 sigue parcial hasta publicar OpenAPI (HU-004, S5).

Estado global: 31 `Completada`, 1 `En desarrollo` (HU-004), 1 `En progreso` (HU-033) y 3 `Pendiente de aprobación` (HU-034 a 036, n8n).

**HECHO — 2026-10-04 · S5 backend.** HU-004 pasa a `Completada` y RF-20 queda cubierto: OpenAPI publicado (`/v3/api-docs`, Swagger UI, copia en `docs/openapi/openapi-v1.json`) y verificado por `OpenApiContractIntegrationTest`. El backend de HU-035 está listo: webhook real con eventos de estado y de reprogramación (`N8nWebhookAppointmentEventsTest`, `AppointmentEventsIntegrationTest`). HU-035 sigue `Pendiente de aprobación` hasta construir y validar el workflow.

Estado global: 32 `Completada`, 1 `En progreso` (HU-033) y 3 `Pendiente de aprobación` (HU-034 a 036, n8n).

**HECHO — 2026-10-04 · S6 WF-002.** HU-035 pasa a `Completada`. WF-002 está publicado en la instancia compartida (v2) y validado con 5 eventos reales de la API, prueba controlada (202/400/403) y deduplicación por `eventId`. Evidencia: `evidence/S6-WF-002-notificaciones.md`. WF-001 y WF-003 están importados con sus credenciales, sin publicar, a la espera del túnel hacia la API local.

Estado global: 33 `Completada`, 1 `En progreso` (HU-033) y 2 `Pendiente de aprobación` (HU-034, HU-036).

**HECHO — 2026-10-04 · WF-001 y WF-003.** Validados con ejecuciones manuales controladas a través de un túnel temporal; los workflows siguen sin publicar. HU-034 pasa a `Completada`: envío (#171), sin duplicado (#172) y rama de error (#169). HU-036 queda `En desarrollo`: envío correcto (#173), pero CA-01 es parcial porque faltan datos del día y la API no tiene agregado por estado (brecha B4). Evidencia: `evidence/S5-WF-001-WF-003.md`.

Estado global: 34 `Completada`, 1 `En progreso` (HU-033) y 1 `En desarrollo` (HU-036).

**HECHO — entorno.** En Docker sobre Windows, el vigilante de archivos de Vite no detecta cambios hechos desde el host. Tras editar `citas-web` hay que reiniciar `npm run dev`, o lanzarlo con sondeo, para verlos en `localhost:5173`.

**PREGUNTA ABIERTA.** `RESTRICCIONES_TECNICAS.md` limita a dos repos públicos, y el `AGENTS.md` raíz indica no inicializar Git en la raíz. Sin embargo, `citas` es un repositorio publicado en GitHub. Falta decidir si se documenta como excepción de orquestación o se deja de versionar.
