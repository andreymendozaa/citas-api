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

`POST /auth/register` acepta `insurancePlanId` opcional; la afiliación es administrativa y no modifica las reglas de agenda. `POST /appointments` recibe `professionalId`, `locationId`, `specialtyId`, `date`, `startTime` y `reason` opcional. La API deriva la naturaleza general o especializada desde la especialidad: devuelve `APPROVED` para general y `REQUESTED` para especializada. Una decisión ADMIN recibe `APPROVE` o `REJECT`; el rechazo exige `reason`.

`GET /admin/professionals` devuelve solo `id`, nombre, código profesional, matrícula, estado y IDs de asignaciones activas; no devuelve correo, contraseña temporal ni hash. Errores de validación usan `400`; recursos o relaciones inexistentes usan `404`; rol u ownership usan `403`; slots ocupados, selección inválida o transición no permitida usan `409`. El frontend consume estas rutas directamente, sin BFF, y no guarda citas ni slots como fuente de verdad.

## DECISIÓN — 2026-09-22 · Corte web de autenticación

`citas-web` consume las cuatro operaciones de autenticación directamente con `VITE_API_URL` (valor local: `http://localhost:8080`). Envía `credentials: include` y `X-Requested-With: XMLHttpRequest` en login, refresh y logout. El access JWT permanece solo en memoria; el refresh se mantiene en cookie `HttpOnly` y se rota al restaurar la sesión. La interfaz no registra ni muestra tokens o contraseñas.

El CORS permite exclusivamente `FRONTEND_ORIGIN`, métodos `POST`, `GET`, `OPTIONS`, encabezados `Content-Type`, `Authorization`, `X-Requested-With` y credenciales. La base de referencia ya existente utiliza `BIGINT` para usuarios, `roles.code` y `refresh_tokens`; Flyway hace baseline en versión 0 y `V1` es compatible con ese esquema 3FN.

## DECISIÓN — 2026-09-29 · Consulta USER de Mis citas

`GET /api/v1/appointments/mine?status=&date=` es exclusivo de `USER` y obtiene el usuario desde el JWT; no acepta ni expone un identificador de paciente. `status` y `date` (`YYYY-MM-DD`) son opcionales. Cada elemento devuelve `id`, `professionalName`, `specialtyName`, `locationName`, `startAt`, `endAt`, `durationMinutes`, `status` y `rejectionReason` cuando la cita fue rechazada. La consulta se construye sobre las FKs existentes de `appointments`, catálogos y `appointment_status_history`, por lo que no requiere migración ni altera reservas, slots o estados.

## DECISIÓN — 2026-09-29 · Cancelación y reprogramación

`POST /appointments/{id}/cancel` permite exclusivamente al USER propietario cancelar una cita futura no terminal, libera sus slots y registra `CANCELLED`. `POST /appointments/{id}/reschedule` recibe `locationId`, `date` y `startTime`; conserva profesional/especialidad, retiene la nueva franja y crea una solicitud `PENDING` sin liberar la original. ADMIN consulta `GET /admin/reschedule-requests/pending` y resuelve con `POST /admin/reschedule-requests/{id}/decision`; `APPROVE` intercambia franjas y `REJECT` libera la provisional. V3 añade las tablas normalizadas de solicitudes y sus estados.

## DECISIÓN — 2026-09-29 · Incremento 1 de S4: recuperación, perfil y catálogos EPS (backend)

`POST /api/v1/auth/password-recovery` recibe `email` y siempre responde `202` sin cuerpo, exista o no la cuenta (anti-enumeración, igual que login). Si el email existe, genera un token aleatorio de 32 bytes persistido únicamente como hash SHA-256 en `password_reset_tokens` (migración `V4`, posterior a V3, sin tocar V1-V3), con vigencia `app.password-reset.ttl-minutes` (default 30). `POST /api/v1/auth/password-reset` recibe `token`, `newPassword` y `confirmation`; token inválido, usado o expirado, o confirmación distinta, responden `400` sin cambiar la contraseña. Un reset válido hashea con BCrypt, marca el token `used_at` y revoca **todos** los refresh tokens del usuario (`Ports.Sessions#revokeAllByUserId`); los access JWT ya emitidos no se invalidan activamente y expiran solos a los 15 minutos.

Buzón local de desarrollo: `GET /api/v1/admin/local-mailbox/password-resets?email=` expone el token en claro únicamente cuando el perfil Spring activo es `local` y el actor es ADMIN; en cualquier otro perfil la ruta no existe (`404`). El token vive solo en memoria (`LocalPasswordResetMailbox`), nunca se persiste ni se registra en logs, y una nueva solicitud sobrescribe la anterior del mismo email.

`GET /api/v1/users/me` devuelve los datos propios del USER autenticado (ownership por `sub` del JWT, sin parámetro de usuario). `PATCH /api/v1/users/me` admite exclusivamente `{"phone": "..."}`; cualquier otro campo en el payload (`firstName`, `email`, `documentNumber`, `roles`, etc.) se rechaza con `400` sin modificar la fila — el contrato exige rechazo explícito, no ignorar en silencio.

`GET|POST|PATCH /api/v1/admin/eps[/{id}]` y `GET|POST|PATCH /api/v1/admin/eps-plans[/{id}]` replican exactamente el patrón ya aprobado de `/admin/specialties`: baja lógica vía `active` (sin `DELETE`), `PATCH` parcial (campos `null` no cambian), y sin migración nueva porque `eps`/`eps_plans`/`insurance_regimes` ya existían desde V2. `GET /api/v1/eps` es público (autenticado, cualquier rol) y solo lista activas, igual que `/specialties`. `POST /admin/eps-plans` exige `epsId` existente y activa (`404`/`409`) y `regimeId` existente (`404`); código duplicado de EPS o de plan dentro de la misma EPS responde `409` mediante verificación previa en el adapter, sin depender del mensaje genérico de duplicidad de email/documento. El catálogo público `GET /api/v1/catalogs/plans` (usado por HU-011/registro) no cambió y sigue filtrando solo planes activos.

Errores nuevos: `PasswordResetFailure` → `400` (token/confirmación inválidos). El resto reutiliza `SchedulingFailure` (`404`/`409`) e `IllegalArgumentException` (`400`) ya existentes.

Evidencia: 33/33 pruebas Maven verdes (`PasswordResetIntegrationTest`, `LocalMailboxIntegrationTest`, `ProfileIntegrationTest`, `InsuranceAdminIntegrationTest`). **Frontend (`citas-web`) pendiente**: HU-008, HU-009, HU-010, HU-012 y HU-013 quedan en `En desarrollo`, no `Completada`, hasta esa ronda.
