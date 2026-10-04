---
id: HU-019
tipo: historia-de-usuario
titulo: "Modificar bloques futuros"
estado: Completada
epica: "[[EP-004-disponibilidad-del-profesional]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 4"
dependencias: ["[[HU-018-crear-bloques-de-disponibilidad]]"]
relacionadas: ["[[HU-020-consultar-calendario-de-disponibilidad]]"]
---
# HU-019 — Modificar bloques futuros
## Historia de usuario
**COMO** PROFESSIONAL  
**QUIERO** editar o eliminar mis bloques futuros sin citas comprometidas  
**PARA** mantener mi disponibilidad correcta.
## Contexto y descripción
El PRD limita la modificación/eliminación a bloques futuros sin citas comprometidas.
## Alcance
- Editar/eliminar bloque propio que cumpla las condiciones y recalcular slots coherentes.
## Fuera de alcance
- Alterar pasado, bloque ajeno o bloque con cita comprometida.
## Reglas de negocio
- Ownership, futuro, no solapamiento y protección de citas comprometidas.
## Dependencias y relaciones
- Épica: [[EP-004-disponibilidad-del-profesional]]
- Dependencias: [[HU-018-crear-bloques-de-disponibilidad]].
- Relacionadas: [[HU-020-consultar-calendario-de-disponibilidad]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** modifica disponibilidad sin afectar reservas existentes.
## Tareas de desarrollo
- [x] **T-01 — Detectar compromisos y ownership.** Dificultad: Alto. Consultar reservas/retenciones aplicables.
- [x] **T-02 — Aplicar edición/eliminación segura.** Dificultad: Alto. Revalidar futuro, sede y solapamiento.
- [x] **T-03 — Actualizar calendario y pruebas.** Dificultad: Medio. Reflejar éxito/rechazo y probar protección.
## Criterios de aceptación
### CA-01 — Edición permitida
**Dado** un bloque propio futuro sin citas comprometidas, **cuando** PROFESSIONAL lo edita de forma válida, **entonces** la disponibilidad refleja los nuevos slots.
### CA-02 — Eliminación permitida
**Dado** un bloque propio futuro sin citas comprometidas, **cuando** PROFESSIONAL lo elimina, **entonces** sus slots dejan de ofrecerse.
### CA-03 — Protección de compromisos
**Dado** un bloque pasado, ajeno o con citas comprometidas, **cuando** se intenta editar/eliminar, **entonces** se rechaza y las citas no cambian.
## Definition of Done
- [x] CA-01 a CA-03 probados con reglas de agenda y autorización.
- [x] Persistencia/índices aplicables, calendario cliente y contrato REST verificados.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS | `SchedulingServiceIntegrationTest.updatesAndDeletesFutureUncommittedBlocks`; `DashboardScreen.test.tsx` (edición) |  |
| CA-02 | PASS | mismo test (slots eliminados) |  |
| CA-03 / DoD | PASS | `OfferAndAgendaRulesIntegrationTest.foreignCommittedAndPastBlocksCannotBeEditedOrDeleted` | Bloque ajeno → `404`; con cita comprometida → `409` sin cambios en bloque ni slots; bloque pasado → `409` al editar y al eliminar. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-30 — Auditoría integral de CA/DoD contra código y pruebas (solicitada por el usuario): estado `Aprobada` → `En desarrollo`. Criterio de auditoría: PASS exige prueba automatizada o propiedad estructural; lo verificado solo por código o manualmente queda PARCIAL/PENDIENTE; la HU se completa solo sin CA pendientes.
- 2026-09-30 — CA-03 cubierto con `OfferAndAgendaRulesIntegrationTest.foreignCommittedAndPastBlocksCannotBeEditedOrDeleted`. Red → Green: un bloque de un día ya pasado podía moverse al futuro (`200`) y eliminarse. `SchedulingJdbcAdapter.updateBlock/deleteBlock` responden ahora `409` ("El bloque ya no es futuro"). Maven 58/58. Estado `En desarrollo` → `Completada`.
## Notas y decisiones
- “Cita comprometida” se verificará contra estados/retenciones aprobados.
