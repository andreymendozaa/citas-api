---
id: HU-034
tipo: historia-de-usuario
titulo: "Automatizar recordatorios"
estado: Completada
epica: "[[EP-008-cliente-web-y-automatizaciones-posteriores]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 7"
dependencias: ["[[HU-025-consultar-mis-citas]]", "[[HU-032-consultar-auditoria-de-estados]]"]
relacionadas: ["[[HU-036-generar-resumen-operativo-diario]]"]
---
# HU-034 — Automatizar recordatorios
## Historia de usuario
**COMO** operación del laboratorio  
**QUIERO** ejecutar recordatorios de citas próximas mediante n8n y Gmail  
**PARA** comunicar oportunamente las citas sin cambiar el núcleo funcional.
## Contexto y descripción
Automatización prevista para S5/S6; usa instancia central del trainer y credenciales configuradas por estudiante, nunca versionadas.
## Alcance
- Workflow n8n exportado en `automations/n8n/`, selección de próximas citas y envío Gmail configurado externamente.
## Fuera de alcance
- SMTP propio, SMS/WhatsApp, credenciales en JSON o cambio de reglas de cita.
## Reglas de negocio
- n8n no altera núcleo; datos/credenciales se tratan de forma segura y sintética para pruebas.
## Dependencias y relaciones
- Épica: [[EP-008-cliente-web-y-automatizaciones-posteriores]]
- Dependencias: [[HU-025-consultar-mis-citas]], [[HU-032-consultar-auditoria-de-estados]].
- Relacionadas: [[HU-036-generar-resumen-operativo-diario]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** integra automatización externa, datos de agenda y gestión segura de credenciales.
## Tareas de desarrollo
- [x] **T-01 — Definir fuente/criterio de próximas citas.** Dificultad: Alto. Acordar contrato de lectura sin cambiar núcleo.
- [x] **T-02 — Configurar workflow en n8n trainer.** Dificultad: Alto. Usar credenciales externas y manejo de fallos.
- [x] **T-03 — Exportar/verificar JSON.** Dificultad: Medio. Versionar sin credenciales y probar con datos sintéticos.
## Criterios de aceptación
### CA-01 — Recordatorio ejecutable
**Dado** citas próximas elegibles y configuración externa válida, **cuando** corre el workflow, **entonces** prepara/envía el recordatorio por Gmail según el diseño aprobado.
### CA-02 — Núcleo intacto
**Dado** la ejecución, **cuando** se revisan citas/estados, **entonces** el workflow no altera el núcleo funcional.
### CA-03 — Exportación segura
**Dado** el workflow terminado, **cuando** se revisa su JSON versionado, **entonces** está en `automations/n8n/` y no contiene credenciales.
## Definition of Done
- [x] CA-01 a CA-03 tienen evidencia de ejecución controlada, JSON y revisión de seguridad.
- [x] Credenciales permanecen solo en n8n/environment y se usan datos sintéticos.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS | `evidence/S5-WF-001-WF-003.md` (#171 envío, #172 sin duplicado, #169 rama de error) | Ventana vigente de 24 h por decisión del usuario (las pruebas se hicieron con 48 h); el correo no incluye datos personales del paciente. |
| CA-02 | PASS | Estructura de `WF-001-appointment-reminders.json` | Solo login y `GET`; ningún endpoint de escritura. |
| CA-03 / DoD | PASS | `automations/n8n/WF-001-appointment-reminders.json` + `check-workflows.mjs` | Sin credenciales, URLs ni correos (marcadores `<<...>>`). |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-04 — Aprobada por el usuario (ventana de 48 h). Validada con las ejecuciones controladas #169, #171 y #172 a través de un túnel temporal. Estado: `Completada`.
- 2026-10-04 — El usuario cambia la ventana de recordatorio a 24 h (`REMINDER_WINDOW_HOURS=24`).
## Notas y decisiones
- No se presupone cuándo se considera “próxima”; requiere configuración aprobada.
