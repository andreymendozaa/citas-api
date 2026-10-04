# Evidencia S6 — WF-002 Notificación por cambio de estado (HU-035)

**Fecha:** 2026-10-04 · **Instancia:** n8n del trainer (2.40.7, cuenta compartida; recursos con prefijo `Andrey`).
**Workflow:** `Andrey - WF-002 Notificacion por cambio de estado`, **publicado** (versión `v2 - dedup con Remove Duplicates`) con autorización del usuario.
**JSON versionado:** `automations/n8n/WF-002-status-notifications.json` (sin ids de credenciales, `webhookId`, URLs ni correos).
**Emisor:** `N8nWebhookAppointmentEvents` (`citas-api` `bd22790`), activado con `N8N_STATUS_WEBHOOK_URL` / `N8N_STATUS_WEBHOOK_BEARER_TOKEN` en el `.env` raíz (no versionado).

## Configuración

| Elemento | Valor |
|---|---|
| Disparador | Webhook `POST /webhook/andrey-citas-status`, Header Auth `Andrey - WF-002 webhook auth` |
| Gmail | Credencial OAuth2 `Andrey - Gmail` (cliente propio en Google Cloud, proyecto `andrey-fcv-citas-n8n`, modo de prueba) |
| Destinatario | Buzón de laboratorio del estudiante (solo en n8n; marcador `<<LAB_RECIPIENT_EMAIL>>` en el JSON) |
| Datos guardados | Ejecuciones exitosas y fallidas (el payload no contiene PII ni tokens) |

## 1. Prueba controlada (URL de prueba, payloads sintéticos)

| Caso | HTTP | Recorrido |
|---|---|---|
| `status.changed` APPROVED/ADMIN | 202 | rama "Especializada aprobada" → Gmail OK |
| `status.changed` REJECTED/ADMIN | 202 | rama "Especializada rechazada" → Gmail OK |
| `status.changed` CANCELLED/USER | 202 | rama "Cancelacion" → Gmail OK |
| `reschedule.decided` APPROVED | 202 | rama "Reprogramacion aprobada" → Gmail OK |
| `reschedule.decided` REJECTED | 202 | rama "Reprogramacion rechazada" → Gmail OK |
| `status` = `COMPLETED` (inválido) | 400 | "Responder 400" → "Registrar invalido", sin correo |
| Sin header `Authorization` | 403 | rechazado por n8n antes de ejecutar (n8n responde 403, no 401) |

## 2. Prueba real de extremo a extremo (producción)

Cuentas sintéticas `lab.*`; especialidad "Dermatología Laboratorio", sede HIC. Acciones ejecutadas contra la API local (script de laboratorio, fuera del repositorio):

| # | Acción en la API | Ejecución n8n | `notificationType` | Cita | Resultado |
|---|---|---|---|---|---|
| 1 | ADMIN aprueba especializada | #129 | SPECIALIZED_APPROVED | 8 | sent |
| 2 | ADMIN rechaza especializada | #130 | SPECIALIZED_REJECTED | 9 | sent |
| 3 | ADMIN aprueba reprogramación (solicitud 4) | #131 | RESCHEDULE_APPROVED | 8 | sent |
| 4 | ADMIN rechaza reprogramación (solicitud 5) | #132 | RESCHEDULE_REJECTED | 8 | sent |
| 5 | USER cancela | #133 | CANCELLATION | 8 | sent |

- Log de la API: 5 líneas `n8n event <uuid> delivered (HTTP 202)`; solo `eventId` y código HTTP, sin token ni datos personales.
- Historial de la cita 8 tras el flujo: `REQUESTED/USER → APPROVED/ADMIN → APPROVED/ADMIN "Reprogramación aprobada" → CANCELLED/USER`. No hay registros atribuibles al workflow.

## 3. Deduplicación por `eventId`

| Ejecución | Envío | Resultado |
|---|---|---|
| #127 | primer envío de `eventId 11111111-…-555555555555` | sent (685 ms) |
| #128 | reenvío del mismo `eventId` | **duplicate** (51 ms, sin correo) |

**Hallazgo corregido (Red → Green):** la v1 deduplicaba con `$getWorkflowStaticData`. En esta instancia el static data no persistió entre ejecuciones de producción y el reenvío (#121) generó un segundo correo. La v2 usa el nodo nativo `Remove Duplicates` (`removeItemsSeenInPreviousExecutions`, clave `eventId`, historial 10 000). WF-001 se corrigió del mismo modo (clave `id|startAt`).

## 4. Criterios de aceptación

| CA | Resultado | Evidencia |
|---|---|---|
| CA-01 Disparo por cambio | PASS | §2: cinco transiciones reales → cinco ejecuciones con datos mínimos (`schemaVersion`, `eventId`, `eventType`, `appointmentId`, `status`, `source`, `occurredAt`). |
| CA-02 Núcleo preservado | PASS | Estructural: WF-002 no tiene nodos HTTP hacia `citas-api`. Entrega post-commit y asíncrona: un webhook caído no afecta la transacción (`N8nWebhookAppointmentEventsTest.unreachableWebhookNeverThrowsToTheCaller`). Historial de la cita 8 sin cambios atribuibles al flujo. |
| CA-03 Seguridad del workflow | PASS | JSON en `automations/n8n/` validado por `check-workflows.mjs` (sin ids de credencial, `webhookId`, URLs, correos ni tokens). |

## Riesgos residuales

- Cuenta n8n compartida: otros usuarios con acceso al proyecto "Personal" pueden ver el workflow y sus ejecuciones. Al importar, n8n asignó automáticamente una credencial ajena (corregido; ver la wiki `decisions.md`).
- El token del webhook se mostró una vez en el chat de trabajo; por decisión del usuario no se rota.
- La app OAuth de Google está en modo de prueba: solo el usuario de prueba puede autorizarla y el refresh token puede caducar a los 7 días; habría que reconectarla.
- Entrega del emisor *best-effort* (1 reintento); un evento perdido no se reintenta después.
