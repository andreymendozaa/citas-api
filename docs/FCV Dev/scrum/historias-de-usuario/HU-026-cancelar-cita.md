---
id: HU-026
tipo: historia-de-usuario
titulo: "Cancelar cita"
estado: Completada
epica: "[[EP-006-ciclo-de-vida-de-citas-y-reprogramaciones]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 5"
dependencias: ["[[HU-025-consultar-mis-citas]]"]
relacionadas: ["[[HU-032-consultar-auditoria-de-estados]]"]
---
# HU-026 — Cancelar cita
## Historia de usuario
**COMO** USER  
**QUIERO** cancelar una cita futura no terminal  
**PARA** liberar su franja cuando ya no la necesito.
## Contexto y descripción
Una cancelada no se reactiva directamente y debe registrarse historial.
## Alcance
- Validar ownership/estado/futuro, transición `CANCELLED`, liberación y auditoría.
## Fuera de alcance
- Reactivación directa o cancelación de cita terminal/no futura.
## Reglas de negocio
- `CANCELLED` libera slots; transiciones explícitas e historial obligatorio.
## Dependencias y relaciones
- Épica: [[EP-006-ciclo-de-vida-de-citas-y-reprogramaciones]]
- Dependencias: [[HU-025-consultar-mis-citas]].
- Relacionadas: [[HU-032-consultar-auditoria-de-estados]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** cambia estado y disponibilidad sin permitir transiciones inválidas.
## Tareas de desarrollo
- [x] **T-01 — Definir elegibilidad.** Dificultad: Alto. Validar futuro, no terminal y ownership.
- [x] **T-02 — Aplicar cancelación atómica.** Dificultad: Alto. Actualizar estado, liberar slots y auditar.
- [x] **T-03 — Integrar acción/pruebas.** Dificultad: Medio. Cubrir éxito, inválida y visibilidad posterior.
## Criterios de aceptación
### CA-01 — Cancelación permitida
**Dado** una cita propia futura no terminal, **cuando** USER cancela, **entonces** cambia a `CANCELLED` y libera sus slots.
### CA-02 — Cancelación restringida
**Dado** cita ajena, pasada o terminal, **cuando** USER intenta cancelar, **entonces** se rechaza sin modificarla.
### CA-03 — No reactivación directa
**Dado** una cita `CANCELLED`, **cuando** se intenta restaurarla por la misma capacidad, **entonces** no se permite y el historial conserva la cancelación.
## Definition of Done
- [x] CA-01 a CA-03 probados con estado, slots, ownership y auditoría.
- [x] Contrato/cliente y persistencia/índices aplicables verificables.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS | `AppointmentLifecycleIntegrationTest.cancelReleasesSlotsRecordsHistoryAndCannotBeRepeated`, `cancellingWithPendingRescheduleCancelsTheRequestAndReleasesBothRanges`; `DashboardScreen.test.tsx` (HU-026 cancela tras confirmar / no cancela sin confirmar) | `CANCELLED`, slots liberados y reservables de nuevo; historial con fuente `USER` y actor; la reprogramación pendiente pasa a `CANCELLED` y libera también su franja. |
| CA-02 | PASS | `AppointmentLifecycleIntegrationTest.cancelRejectsForeignMissingPastTerminalAndNonUserRequestsWithoutChanges` | Cita ajena o inexistente → `404`; pasada o terminal (`REJECTED`) → `409`; ADMIN/PROFESSIONAL → `403`. En todos los casos no cambian ni el estado ni los slots. |
| CA-03 / DoD | PASS | `cancelReleasesSlotsRecordsHistoryAndCannotBeRepeated`; no existe ruta de reactivación (estructural) | Una segunda cancelación responde `409` y el historial conserva un único registro `CANCELLED`. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-30 — Auditoría integral de CA/DoD contra código y pruebas (solicitada por el usuario): estado `Pendiente de aprobación` → `En desarrollo`. Criterio de auditoría: PASS exige prueba automatizada o propiedad estructural; lo verificado solo por código o manualmente queda PARCIAL/PENDIENTE; la HU se completa solo sin CA pendientes.
- 2026-09-30 — Pruebas dedicadas añadidas (`AppointmentLifecycleIntegrationTest`, 8 casos; 4 casos de cliente en `DashboardScreen.test.tsx`). Red → Green: la cita ajena o inexistente y la solicitud inexistente o ya resuelta provocaban `EmptyResultDataAccessException` sin manejar (error 500); `SchedulingJdbcAdapter.cancel/requestReschedule/decideReschedule` responden ahora `404`/`409`. Maven 48/48 (dos ejecuciones consecutivas) y Vitest verdes. Estado `En desarrollo` → `Completada`.
## Notas y decisiones
- El catálogo determina cuáles estados son terminales.
