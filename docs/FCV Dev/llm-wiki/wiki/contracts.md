# Contratos REST

## HECHO

El PRD exige REST/JSON entre `citas-web` y `citas-api` y validación cross-repo en funcionalidades clave.

## DECISIÓN — 2026-09-17 · HU-004/005/006/007

El contrato inicial cubre solamente autenticación bajo `/api/v1/auth`:

| Operación | Entrada | Éxito |
|---|---|---|
| `POST /register` | JSON `firstName`, `lastName`, `documentType`, `documentNumber`, `email`, `phone`, `password` | `201`, JSON con `id`, datos públicos y rol `USER`, sin contraseña |
| `POST /login` | JSON `email`, `password` | `200`, JSON `accessToken`, `tokenType=Bearer`, `expiresIn`; cookie `refresh_token` |
| `POST /refresh` | Cookie `refresh_token` | `200`, nuevo access en JSON y nueva cookie refresh; la anterior se revoca |
| `POST /logout` | Cookie `refresh_token` | `204`, revocación de la sesión y cookie borrada |

Email se normaliza con trim y minúsculas. Documento es único por `(documentType, documentNumber)` normalizados. La contraseña de registro es obligatoria y se limita a 72 bytes UTF-8 por el límite de BCrypt; sus espacios no se alteran. Solo se permite autoregistro `USER`. Errores: `400` validación, `409` duplicidad, `401` credencial/refresh inválido, `403` rol insuficiente, en formato Problem Details; login no revela qué credencial falló.

Access JWT y refresh JWT usan secretos distintos, tipo explícito y duraciones configurables (valores iniciales: 15 minutos y 7 días). El access lleva `sub` y roles. El refresh lleva `sub` y `jti`; su identificador se guarda solo como hash en una sesión persistida. Un refresh válido rota ambos tokens atómicamente. Logout revoca solo la sesión indicada; un access emitido conserva validez hasta su expiración.

Para sitios distintos, la cookie es `HttpOnly; Secure; SameSite=None`, con `Path=/api/v1/auth`. CORS permite credenciales únicamente al `FRONTEND_ORIGIN` configurado. Login, refresh y logout requieren `Origin` permitido cuando se envía y `X-Requested-With: XMLHttpRequest`; el perfil HTTP local usa cookie `SameSite=Lax` sin `Secure`. El cliente futuro deberá enviar credenciales y ese encabezado, guardar access únicamente según su diseño aprobado y eliminar su estado local al salir.

### Impacto cross-repo antes del cambio REST

- `citas-api`: nuevo `pom.xml`, código de dominio/aplicación/adaptadores, migración Flyway, configuración, pruebas y este contrato.
- `citas-web`: sin cambios en este incremento; HU-033 integrará las cuatro rutas, cookie y errores. Al ser endpoints nuevos, no hay cliente previo que migrar.
- Compatibilidad: `/api/v1` fija la versión de este contrato; cambios posteriores requieren revisión de ambas partes. Migración: esquema inicial de identidad por Flyway. Pruebas: REST, seguridad, persistencia y `mvn test` en backend; prueba cross-repo cuando exista el cliente.

## PREGUNTA ABIERTA

Las rutas, filtros, paginación y formatos de fecha/hora de las demás HU siguen sin contrato aprobado.

## DECISIÓN — 2026-09-22 · Contrato S3 de agendamiento

Todos los recursos S3 usan `/api/v1`, JWT access en `Authorization: Bearer` y JSON. Las fechas se representan como `YYYY-MM-DD` y las horas como `HH:mm` en la zona `America/Bogota`.

| Recurso | Operación | Rol |
|---|---|---|
| Catálogos | `GET /catalogs/{locations|appointment-statuses|roles|regimes|plans}` | autenticado |
| Especialidades disponibles | `GET /specialties` | autenticado |
| Especialidades ADMIN | `GET|POST|PATCH /admin/specialties[/{id}]` | ADMIN |
| Profesionales | `GET|POST /admin/professionals`; `PUT /admin/professionals/{id}/specialties|locations`; `PATCH /admin/professionals/{id}/active` | ADMIN |
| Bloques propios | `GET|POST /professional/availability-blocks`; `PATCH|DELETE /professional/availability-blocks/{id}` | PROFESSIONAL |
| Disponibilidad | `GET /availability?locationId=&specialtyId=&date=&professionalId?` | USER |
| Reserva | `POST /appointments` | USER |
| Solicitudes especializadas | `GET /admin/appointments/pending-specialized`; `POST /admin/appointments/{id}/decision` | ADMIN |

`POST /auth/register` acepta `insurancePlanId` opcional; la afiliación es administrativa y no modifica las reglas de agenda. `POST /appointments` recibe `professionalId`, `locationId`, `specialtyId`, `date`, `startTime` y `reason` opcional. La API deriva la naturaleza general o especializada desde la especialidad: devuelve `APPROVED` para general y `REQUESTED` para especializada. Una decisión ADMIN recibe `APPROVE` o `REJECT`; el rechazo exige `reason`. *(Precisión 2026-09-30, verificada por `SpecializedDecisionIntegrationTest`: en `POST /admin/appointments/{id}/decision`, una cita inexistente responde `404`; una cita general, ya decidida o cancelada, `409`; un motivo vacío o en blanco o una decisión inválida, `400`. Antes de esa fecha los casos `404`/`409` terminaban en un error 500 no controlado.)*

`GET /admin/professionals` devuelve solo `id`, nombre, código profesional, matrícula, estado y IDs de asignaciones activas; no devuelve correo, contraseña temporal ni hash. Errores de validación usan `400`; recursos o relaciones inexistentes usan `404`; rol u ownership usan `403`; slots ocupados, selección inválida o transición no permitida usan `409`. El frontend consume estas rutas directamente, sin BFF, y no guarda citas ni slots como fuente de verdad.

## DECISIÓN — 2026-09-22 · Corte web de autenticación

`citas-web` consume las cuatro operaciones de autenticación directamente con `VITE_API_URL` (valor local: `http://localhost:8080`). Envía `credentials: include` y `X-Requested-With: XMLHttpRequest` en login, refresh y logout. El access JWT permanece solo en memoria; el refresh se mantiene en cookie `HttpOnly` y se rota al restaurar la sesión. La interfaz no registra ni muestra tokens o contraseñas.

El CORS permite exclusivamente `FRONTEND_ORIGIN`, métodos `POST`, `GET`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` (corregido 2026-09-30 contra `SecurityConfig.java`; la versión original de esta línea solo listaba `POST`, `GET`, `OPTIONS`), encabezados `Content-Type`, `Authorization`, `X-Requested-With` y credenciales. La base de referencia ya existente utiliza `BIGINT` para usuarios, `roles.code` y `refresh_tokens`; Flyway hace baseline en versión 0 y `V1` es compatible con ese esquema 3FN.

## DECISIÓN — 2026-09-29 · Consulta USER de Mis citas

`GET /api/v1/appointments/mine?status=&date=` es exclusivo de `USER` y obtiene el usuario desde el JWT; no acepta ni expone un identificador de paciente. `status` y `date` (`YYYY-MM-DD`) son opcionales. Cada elemento devuelve `id`, `professionalName`, `specialtyName`, `locationName`, `startAt`, `endAt`, `durationMinutes`, `status` y `rejectionReason` cuando la cita fue rechazada. La consulta se construye sobre las FKs existentes de `appointments`, catálogos y `appointment_status_history`, por lo que no requiere migración ni altera reservas, slots o estados.

## DECISIÓN — 2026-09-30 · Afiliación del USER (HU-011, RF-04) y regímenes (V5)

Por decisión del usuario, HU-011 cubre también consultar y cambiar la afiliación propia, alineando su alcance con sus CA y con el RF-04.

**Endpoints (solo rol `USER`; el usuario se toma del JWT, sin parámetro de usuario):**
- `GET /api/v1/users/me/affiliation`: responde `200` con `planId`, `planCode`, `planName`, `epsId`, `epsName`, `regimeId`, `regimeName` y `membershipNumber`, o `204` si no hay afiliación.
- `PUT /api/v1/users/me/affiliation` con `{"planId": n}`:
  - fija el plan vigente;
  - el mismo plan vigente → `409`;
  - plan inactivo, de EPS inactiva, inexistente o ausente → `400`;
  - ADMIN/PROFESSIONAL → `403`.

**Reglas de no duplicación (CA-02):**
- Nunca hay dos filas del mismo plan para un usuario, ni más de una vigente.
- Volver a un plan anterior reactiva su fila.
- El número de afiliado sigue siendo sintético (`AUTO-<usuario>-<plan>`), como en el registro.

**Cambio de comportamiento:** `GET /api/v1/catalogs/plans` (usado por el registro) y la validación del registro solo aceptan planes activos de una EPS activa. Antes listaban los planes activos aunque su EPS estuviera desactivada.

**Migración V5:** siembra en el catálogo fijo `insurance_regimes` los 5 valores del modelo de referencia del trainer (CONTRIBUTIVO, SUBSIDIADO, ESPECIAL, EXCEPCION, PARTICULAR), aprobados por el usuario. Es idempotente por `code`, no cambia el esquema y no toca V1–V4. La 3FN se mantiene: la afiliación solo guarda FKs.

**Evidencia:**
- `AffiliationIntegrationTest` (5 casos);
- `s4Screens.test.tsx` (2 casos) y `schedulingApi.test.ts`;
- validación en vivo en Chrome.

## DECISIÓN — 2026-09-30 · Cambios aditivos de contrato (HU-003, HU-016, HU-021)

Los dos cambios del backend son aditivos y compatibles: ningún consumidor existente se rompe.
- **HU-003:** `GET /api/v1/catalogs/reschedule-statuses` devuelve `id`, `code`, `name` y `terminal` del catálogo fijo `reschedule_request_statuses` (PENDING, APPROVED, REJECTED, CANCELLED). Los catálogos fijos siguen sin rutas de escritura.
- **HU-016:** cada elemento de `GET /api/v1/admin/professionals` incluye `primarySpecialtyId` (`null` si no hay asignaciones), para que el cliente edite asignaciones sin perder la especialidad primaria.
- **HU-021:** el filtro por tipo general/especializada del RF-10 se resuelve en el cliente sobre el campo `general` que `GET /specialties` ya exponía, sin cambio de contrato.

Evidencia:
- `FixedCatalogsIntegrationTest` y `OfferAndAgendaRulesIntegrationTest` (Maven);
- `BookAppointmentModal.test.tsx` y `DashboardScreen.test.tsx` (Vitest).

## PRECISIÓN — 2026-09-30 · Bloques de disponibilidad pasados

`PATCH` y `DELETE /api/v1/professional/availability-blocks/{id}` responden `409` ("El bloque ya no es futuro") cuando el día del bloque ya pasó. Esto aplica aunque el `PATCH` intente moverlo a una fecha futura. Un bloque ajeno responde `404` y un bloque con citas comprometidas, `409`. Verificado por `OfferAndAgendaRulesIntegrationTest`.

## DECISIÓN — 2026-09-29 · Cancelación y reprogramación

`POST /appointments/{id}/cancel` permite exclusivamente al USER propietario cancelar una cita futura no terminal, libera sus slots y registra `CANCELLED`. *(Precisión 2026-09-30, verificada por `AppointmentLifecycleIntegrationTest`: una cita ajena o inexistente responde `404`; una cita pasada o terminal, `409`. Aplica igual a `POST /appointments/{id}/reschedule`. En `POST /admin/reschedule-requests/{id}/decision`, una solicitud inexistente responde `404` y una ya resuelta, `409`. Antes de esa fecha esos casos terminaban en un error 500 no controlado. Cancelar una cita con reprogramación `PENDING` marca la solicitud como `CANCELLED` y libera ambas franjas.)* `POST /appointments/{id}/reschedule` recibe `locationId`, `date` y `startTime`; conserva profesional/especialidad, retiene la nueva franja y crea una solicitud `PENDING` sin liberar la original. ADMIN consulta `GET /admin/reschedule-requests/pending` y resuelve con `POST /admin/reschedule-requests/{id}/decision`; `APPROVE` intercambia franjas y `REJECT` libera la provisional. V3 añade las tablas normalizadas de solicitudes y sus estados.

## DECISIÓN — 2026-09-29 · Incremento 1 de S4: recuperación, perfil y catálogos EPS (backend)

`POST /api/v1/auth/password-recovery` recibe `email` y siempre responde `202` sin cuerpo, exista o no la cuenta (anti-enumeración, igual que login). Si el email existe, genera un token aleatorio de 32 bytes persistido únicamente como hash SHA-256 en `password_reset_tokens` (migración `V4`, posterior a V3, sin tocar V1-V3), con vigencia `app.password-reset.ttl-minutes` (default 30). `POST /api/v1/auth/password-reset` recibe `token`, `newPassword` y `confirmation`; token inválido, usado o expirado, o confirmación distinta, responden `400` sin cambiar la contraseña. Un reset válido hashea con BCrypt, marca el token `used_at` y revoca **todos** los refresh tokens del usuario (`Ports.Sessions#revokeAllByUserId`); los access JWT ya emitidos no se invalidan activamente y expiran solos a los 15 minutos.

Buzón local de desarrollo: `GET /api/v1/admin/local-mailbox/password-resets?email=` expone el token en claro únicamente cuando el perfil Spring activo es `local` y el actor es ADMIN; en cualquier otro perfil la ruta no existe (`404`). El token vive solo en memoria (`LocalPasswordResetMailbox`), nunca se persiste ni se registra en logs, y una nueva solicitud sobrescribe la anterior del mismo email.

`GET /api/v1/users/me` devuelve los datos propios del USER autenticado (ownership por `sub` del JWT, sin parámetro de usuario). `PATCH /api/v1/users/me` admite exclusivamente `{"phone": "..."}`; cualquier otro campo en el payload (`firstName`, `email`, `documentNumber`, `roles`, etc.) se rechaza con `400` sin modificar la fila — el contrato exige rechazo explícito, no ignorar en silencio.

`GET|POST|PATCH /api/v1/admin/eps[/{id}]` y `GET|POST|PATCH /api/v1/admin/eps-plans[/{id}]` replican exactamente el patrón ya aprobado de `/admin/specialties`: baja lógica vía `active` (sin `DELETE`), `PATCH` parcial (campos `null` no cambian), y sin migración nueva porque `eps`/`eps_plans`/`insurance_regimes` ya existían desde V2. `GET /api/v1/eps` es público (autenticado, cualquier rol) y solo lista activas, igual que `/specialties`. `POST /admin/eps-plans` exige `epsId` existente y activa (`404`/`409`) y `regimeId` existente (`404`); código duplicado de EPS o de plan dentro de la misma EPS responde `409` mediante verificación previa en el adapter, sin depender del mensaje genérico de duplicidad de email/documento. El catálogo público `GET /api/v1/catalogs/plans` (usado por HU-011/registro) no cambió y sigue filtrando solo planes activos.

Errores nuevos: `PasswordResetFailure` → `400` (token/confirmación inválidos). El resto reutiliza `SchedulingFailure` (`404`/`409`) e `IllegalArgumentException` (`400`) ya existentes.

Evidencia: 33/33 pruebas Maven verdes (`PasswordResetIntegrationTest`, `LocalMailboxIntegrationTest`, `ProfileIntegrationTest`, `InsuranceAdminIntegrationTest`). **Frontend (`citas-web`) pendiente**: HU-008, HU-009, HU-010, HU-012 y HU-013 quedan en `En desarrollo`, no `Completada`, hasta esa ronda.

## DECISIÓN — 2026-09-29 · Incremento 3 de S4: operación profesional, auditoría, bandeja unificada y n8n-ready (backend)

`GET /api/v1/professional/appointments?from=&to=&locationId=` (rol `PROFESSIONAL`) devuelve exclusivamente las citas propias en estado `APPROVED`; el aislamiento entre profesionales es estructural (no existe parámetro de profesional en el endpoint). `POST /api/v1/professional/appointments/{id}/closure` recibe `{"result":"COMPLETED"|"NO_SHOW","reason":"..."}`; exige cita propia, en estado `APPROVED`, con `scheduledEndAt` ya transcurrido respecto al reloj `America/Bogota`. Cita ajena o inexistente → `404`; cita no `APPROVED` (incluida una ya cerrada, sirviendo de guarda de idempotencia) o aún no finalizada → `409`; `result` inválido → `400`. Cada cierre inserta en `appointment_status_history` con `change_source='PROFESSIONAL'`.

`GET /api/v1/appointments/{id}/history` es accesible por `USER`, `PROFESSIONAL` y `ADMIN` con ownership resuelta dentro de la propia consulta SQL (`admin` o `patient_user_id=caller` o `professional.user_id=caller`); una lista vacía siempre responde `404`, sin distinguir "la cita no existe" de "no está en tu alcance". No existe ningún endpoint de escritura sobre `appointment_status_history`: es estrictamente de solo lectura, poblada únicamente por las transiciones ya centralizadas (`reserve`, `cancel`, `decide`, `decideReschedule`, `closeAppointment`).

`GET /api/v1/admin/inbox?locationId=&professionalId=&specialtyId=&date=` (rol `ADMIN`, HU-031) combina en una sola lista las solicitudes especializadas `REQUESTED` y las reprogramaciones `PENDING`, cada elemento con un campo `type` (`SPECIALIZED`/`RESCHEDULE`). Los endpoints previos `GET /admin/appointments/pending-specialized` y `GET /admin/reschedule-requests/pending` se conservan sin cambios por compatibilidad; el frontend migrará a la bandeja unificada en su propia ronda.

Preparación para n8n: `GET /api/v1/admin/appointments/upcoming?from=&to=&locationId=` (rol `ADMIN`, solo lectura, verificado que no muta ninguna fila) devuelve citas `APPROVED` futuras con `patientName`, `patientPhone` (sin `patientEmail`), profesional, especialidad, sede y horario. Internamente se publica un evento `Ports.AppointmentStatusChanged` (cita, estado anterior/nuevo, fuente, actor, fecha; sin PII) únicamente tras una decisión ADMIN (`APPROVE`/`REJECT`) o una cancelación de usuario — nunca para la auto-aprobación de medicina general ni para decisiones de reprogramación. La publicación ocurre desde `SchedulingJdbcAdapter` vía `ApplicationEventPublisher`, y un listener `@TransactionalEventListener(phase=AFTER_COMMIT)` la reenvía al puerto de salida `Ports.AppointmentEvents` solo después de que la transacción confirma. `NoOpAppointmentEventsAdapter` es la única implementación por ahora (S5 conectará ahí el webhook HTTP real hacia n8n Cloud sin modificar estos puntos de publicación).

Sin migración Flyway nueva: los estados `COMPLETED`/`NO_SHOW` ya estaban sembrados desde `V2__scheduling_core.sql`, y `appointment_status_history.change_source` no tiene `CHECK`/FK que restrinja sus valores.

Evidencia: 40/40 pruebas Maven verdes (`ProfessionalOperationsIntegrationTest`, `AppointmentHistoryIntegrationTest`, `UpcomingAppointmentsIntegrationTest`, `AppointmentEventsIntegrationTest`, `InboxIntegrationTest`). **Frontend (`citas-web`) pendiente**: HU-029, HU-030, HU-031 y HU-032 quedan en `En desarrollo`, no `Completada`, hasta esa ronda.

## DECISIÓN — 2026-09-30 · Frontend consolidado de S4 (Incrementos 1 y 3)

`citas-web` consume sin cambios de contrato todos los endpoints de los Incrementos 1 y 3; `citas-api` no se modificó. Navegación: se conserva la máquina de estados sin router y se añaden pestañas por rol dentro del dashboard (USER: *Mis citas* | *Mi perfil*; PROFESSIONAL: *Agenda* | *Disponibilidad*; ADMIN: *Bandeja* | *Oferta* | *EPS y planes*).

- Recuperación (HU-008/009): pantallas propias desde el login. El restablecimiento acepta el código por campo o por `?token=`, que se retira de la URL con `history.replaceState` al iniciar. El cliente de auth trata `202` sin cuerpo como éxito. Ninguna UI muestra tokens; en local el token se obtiene del buzón ADMIN por fuera de la interfaz.
- Perfil (HU-010): `PATCH /users/me` envía solo `{phone}`; la identidad se muestra en solo lectura.
- EPS y planes (HU-012/013): mismo patrón visual que especialidades; el régimen se lee de `GET /catalogs/regimes`.
- Agenda y cierre (HU-029/030): Día/Semana (lunes-domingo) se traduce a `from`/`to`. Los botones de cierre aparecen solo cuando `endAt` ya pasó en hora Bogotá, como mejora de experiencia; la regla sigue siendo del backend.
- Bandeja (HU-031): el cliente migra a `GET /admin/inbox` y deja de consumir `GET /admin/appointments/pending-specialized`, que sigue expuesto por compatibilidad. Las decisiones se enrutan por `type`: `SPECIALIZED` → `/admin/appointments/{id}/decision`, `RESCHEDULE` → `/admin/reschedule-requests/{id}/decision`.
- Auditoría (HU-032): componente de historial de solo lectura, reutilizado por los tres roles; un `404` se muestra como "no disponible", sin distinguir inexistencia de falta de alcance.

Diseño: **no existe referencia Stitch/AI Studio aprobada para estas pantallas**. Por decisión del usuario se extendió el estilo Tailwind ya presente (tarjetas `rounded-3xl`, acciones `blue-600`, tarjeta de autenticación del login); queda como deuda de aprobación visual.

Evidencia: `npm run lint`, `npm test` (34/34, incluye `s4Screens.test.tsx`) y `npm run build` en verde. Además se validó manualmente en Chrome contra el backend en Docker con perfil `local`:
- recuperación y restablecimiento con login posterior;
- edición de teléfono;
- creación y baja lógica de EPS y plan;
- rechazo con motivo de una reprogramación y aprobación de una especializada desde la bandeja;
- agenda semanal y cierre `COMPLETED`, con historial `PROFESSIONAL` verificado vía API;
- historial visible como USER, PROFESSIONAL y ADMIN.

HU-008, 009, 010, 012, 013, 029, 030, 031 y 032 pasan a `Completada`, y S4 queda cerrado.
