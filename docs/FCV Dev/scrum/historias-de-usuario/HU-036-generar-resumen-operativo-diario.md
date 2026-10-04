---
id: HU-036
tipo: historia-de-usuario
titulo: "Generar resumen operativo diario"
estado: Completada
epica: "[[EP-008-cliente-web-y-automatizaciones-posteriores]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 7"
dependencias: ["[[HU-032-consultar-auditoria-de-estados]]", "[[HU-034-automatizar-recordatorios]]"]
relacionadas: ["[[HU-035-notificar-cambios-de-estado]]"]
---
# HU-036 — Generar resumen operativo diario
## Historia de usuario
**COMO** operación del laboratorio  
**QUIERO** recibir un resumen diario por sede y estado  
**PARA** conocer la situación operativa de agenda.
## Contexto y descripción
Es el caso adicional de automatización S5/S6 descrito por el PRD.
## Alcance
- Consulta agregada autorizada, workflow n8n diario y salida definida por operación sin datos innecesarios.
## Fuera de alcance
- Reportería clínica, cambio de citas, credenciales en repositorio o métricas no definidas.
## Reglas de negocio
- Agrupar por sede/estado; workflow no modifica núcleo; datos sintéticos para pruebas.
## Dependencias y relaciones
- Épica: [[EP-008-cliente-web-y-automatizaciones-posteriores]]
- Dependencias: [[HU-032-consultar-auditoria-de-estados]], [[HU-034-automatizar-recordatorios]].
- Relacionadas: [[HU-035-notificar-cambios-de-estado]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** define agregación operativa, automatización y minimización de datos.
## Tareas de desarrollo
- [x] **T-01 — Acordar consulta agregada.** Dificultad: Alto. Precisar periodo, estados y campos mínimos por sede.
- [x] **T-02 — Configurar workflow diario.** Dificultad: Alto. Ejecutar en instancia trainer y entregar salida aprobada.
- [x] **T-03 — Exportar/verificar seguridad.** Dificultad: Medio. Versionar JSON sin secretos y probar datos sintéticos.
## Criterios de aceptación
### CA-01 — Agregación por sede/estado
**Dado** citas sintéticas con diversos estados y sedes, **cuando** corre el flujo diario, **entonces** produce el resumen agrupado por sede y estado.
### CA-02 — Sin impacto funcional
**Dado** la ejecución del resumen, **cuando** se revisa la agenda, **entonces** no modifica citas, slots ni auditoría.
### CA-03 — Artefacto seguro
**Dado** el workflow terminado, **cuando** se revisa el repositorio, **entonces** su JSON se versiona en `automations/n8n/` sin credenciales.
## Definition of Done
- [x] CA-01 a CA-03 tienen evidencia de agregación, ejecución controlada y revisión de seguridad.
- [x] La definición de periodo/salida está aprobada y minimiza datos personales.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS (limitación B4) | `evidence/S5-WF-001-WF-003.md` (#329, día simulado con `scripts/hu036-simulacion-dia.sql`) | `APPROVED` del día por sede (HIC 2, ICV 1) y por especialidad, más pendientes por tipo (1 especializada, 1 reprogramación). COMPLETED/NO_SHOW/CANCELLED: "no disponible por contrato" (brecha B4, mejora futura). |
| CA-02 | PASS | Estructura de `WF-003-daily-operational-summary.json` | Solo login y `GET`. |
| CA-03 | PASS | `automations/n8n/WF-003-daily-operational-summary.json` + `check-workflows.mjs` | Sin credenciales, URLs ni correos. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-04 — Aprobada por el usuario (06:00 Bogotá, buzón de laboratorio). Ejecución controlada #173. Estado: `En desarrollo` (CA-01 parcial).
- 2026-10-04 — Con aprobación del usuario se simula un día operativo con datos sintéticos (`scripts/hu036-simulacion-dia.sql`). Ejecución #329: resumen correcto por sede, especialidad y pendientes. Estado: `Completada` (la brecha B4 queda como mejora futura).
## Notas y decisiones
- Pregunta abierta: destinatario/canal del resumen, fuera del PRD.
