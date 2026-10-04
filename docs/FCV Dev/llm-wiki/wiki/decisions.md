# Registro de decisiones

## DECISIÓN — 2026-09-17

La raíz no se convierte en un tercer repositorio. La única LLM Wiki se versiona dentro de `citas-api/docs/FCV Dev/llm-wiki/`.

## DECISIÓN — 2026-09-17 · Identidad backend

El usuario aprobó el incremento mínimo de HU-001/002/004, el seed parcial de HU-003 y el backend completo de HU-005/006/007. Email se compara sin distinguir mayúsculas y documento por tipo+número. El refresh va en cookie HttpOnly para sitios distintos y rota en cada uso. Logout revoca el refresh de esa sesión. Las obligaciones de interfaz se trasladan a HU-033.

## PREGUNTA ABIERTA

No se han aprobado todavía estados exhaustivos de citas, contratos de las demás HU, zona horaria ni estrategia de reserva concurrente.

## DECISIÓN — 2026-09-22 · Catálogo de subagentes

Los ocho subagentes especializados se mantienen como archivos Markdown versionados en `docs/FCV Dev/subagents/`. El orquestador selecciona el perfil más específico, separa implementación de verificación y conserva la responsabilidad de coordinar cambios cross-repo y actualizar la Wiki.

## HECHO — 2026-09-22 · Frontend

React es el framework detectado en `citas-web`; deja de ser una pregunta abierta. La aprobación visual y la verificación del incremento auth continúan pendientes de evidencia.

## DECISIÓN — 2026-10-04 · Instancia n8n compartida

La cuenta de n8n entregada por el trainer es compartida por varios estudiantes. Todo workflow y credencial del proyecto lleva el prefijo `Andrey`; no se leen, ejecutan, modifican ni eliminan recursos ajenos. Se genera un acceso MCP propio del proyecto. La credencial Gmail OAuth2 se crea con la cuenta Google personal del estudiante (autorizado por el usuario).

## DECISIÓN — 2026-10-04 · Alcance de WF-002

WF-002 también notifica reprogramaciones aprobadas y rechazadas; para ello la API emite `appointment.reschedule.decided` (ver [Contratos](contracts.md)).

## DECISIÓN — 2026-10-04 · Parámetros de WF-001 y WF-003

Ventana de recordatorio de 48 h (WF-001) y resumen diario a las 06:00 hora de Bogotá (WF-003). Los correos de laboratorio van al Gmail personal del estudiante, configurado solo en n8n; el JSON versionado conserva `<<LAB_RECIPIENT_EMAIL>>`.

## HECHO — 2026-10-04 · Riesgo en la instancia compartida

Al importar un workflow cuyo nodo referencia una credencial inexistente, n8n asigna automáticamente una credencial existente del mismo tipo, aunque sea de otro estudiante: el webhook de WF-002 quedó apuntando a una credencial ajena. Tras importar o abrir nodos, hay que verificar que cada nodo use solo credenciales `Andrey - …`.

## HECHO — 2026-10-04 · Deduplicación en n8n

En la instancia del trainer, `$getWorkflowStaticData` no persistió entre ejecuciones de producción: el mismo `eventId` generó dos correos (ejecución #121). Los workflows usan ahora el nodo nativo `Remove Duplicates` (`removeItemsSeenInPreviousExecutions`), que sí descartó el reenvío (#128). No usar static data para estado entre ejecuciones en esta instancia.

## DECISIÓN — 2026-10-04 · WF-002 publicado

El usuario autorizó publicar WF-002 y conectar la API real (`N8N_STATUS_WEBHOOK_URL` en el `.env` raíz).

## PREGUNTA ABIERTA — 2026-10-04 · Exposición de la API a n8n

n8n corre en la nube y no alcanza `localhost:8080`. La opción propuesta es un túnel temporal (Cloudflare quick tunnel) abierto solo durante las pruebas controladas. Requiere autorización explícita del usuario en la configuración de permisos, porque expone la API local a Internet (protegida por JWT).
