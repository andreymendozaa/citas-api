---
id: HU-027
tipo: historia-de-usuario
titulo: "Solicitar reprogramación"
estado: Completada
epica: "[[EP-006-ciclo-de-vida-de-citas-y-reprogramaciones]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 5"
dependencias: ["[[HU-021-buscar-disponibilidad]]", "[[HU-025-consultar-mis-citas]]"]
relacionadas: ["[[HU-028-resolver-reprogramacion]]"]
---
# HU-027 — Solicitar reprogramación
## Historia de usuario
**COMO** USER  
**QUIERO** solicitar una nueva fecha/hora disponible para mi cita aprobada  
**PARA** cambiarla sin perder la franja original antes de la decisión ADMIN.
## Contexto y descripción
Conserva profesional/especialidad; cambiar profesional es una nueva cita. La solicitud nace `PENDING` y retiene nueva franja.
## Alcance
- Elegibilidad, nueva franja válida, retención y solicitud `PENDING` que conserva cita original.
## Fuera de alcance
- Cambiar profesional/especialidad, aprobar automáticamente o liberar franja original al solicitar.
## Reglas de negocio
- Solo `APPROVED` futura; nueva reserva completa; original se preserva hasta decisión.
## Dependencias y relaciones
- Épica: [[EP-006-ciclo-de-vida-de-citas-y-reprogramaciones]]
- Dependencias: [[HU-021-buscar-disponibilidad]], [[HU-025-consultar-mis-citas]].
- Relacionadas: [[HU-028-resolver-reprogramacion]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** requiere doble reserva coordinada y conserva el estado original.
## Tareas de desarrollo
- [x] **T-01 — Validar cita/elegibilidad.** Dificultad: Alto. Exigir propia, futura y `APPROVED`.
- [x] **T-02 — Retener nueva franja.** Dificultad: Alto. Mantener profesional/especialidad y evitar doble reserva.
- [x] **T-03 — Persistir solicitud/pruebas.** Dificultad: Alto. Conservar original y auditar `PENDING`.
## Criterios de aceptación
### CA-01 — Solicitud válida
**Dado** una cita propia futura `APPROVED`, **cuando** USER selecciona nueva franja disponible del mismo profesional/especialidad, **entonces** se crea reprogramación `PENDING` y se retiene esa franja.
### CA-02 — Cita original preservada
**Dado** la solicitud pendiente, **cuando** se consulta la cita original, **entonces** conserva su franja hasta decisión ADMIN.
### CA-03 — Restricciones
**Dado** cita no elegible, cambio de profesional/especialidad o franja no disponible, **cuando** se solicita, **entonces** se rechaza sin alterar citas/reservas.
## Definition of Done
- [x] CA-01 a CA-03 probados con concurrencia/persistencia, ownership y auditoría.
- [x] Contrato/cliente y migración/índices aplicables verificables.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS | `AppointmentLifecycleIntegrationTest.rescheduleRetainsTheNewRangeAndKeepsTheOriginalUntilDecision`; `DashboardScreen.test.tsx` (HU-027 solicita reprogramación con el mismo profesional) | Reprogramación `PENDING` que retiene la nueva franja (slots 10:00/10:30 asignados a la cita y fuera de la disponibilidad). |
| CA-02 | PASS | mismo test | La cita conserva `APPROVED`, su horario original y sus slots hasta la decisión ADMIN. |
| CA-03 / DoD | PASS | `AppointmentLifecycleIntegrationTest.rescheduleRejectsIneligibleRequestsWithoutSideEffects`; `DashboardScreen.test.tsx` (bloquea segunda reprogramación) | Cita ajena o inexistente → `404`; franja ocupada, sede no asignada, fecha pasada, cita no `APPROVED` o solicitud ya pendiente → `409`; ADMIN → `403`. Sin filas nuevas ni cambios de slots. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-30 — Auditoría integral de CA/DoD contra código y pruebas (solicitada por el usuario): estado `Pendiente de aprobación` → `En desarrollo`. Criterio de auditoría: PASS exige prueba automatizada o propiedad estructural; lo verificado solo por código o manualmente queda PARCIAL/PENDIENTE; la HU se completa solo sin CA pendientes.
- 2026-09-30 — Pruebas dedicadas añadidas (`AppointmentLifecycleIntegrationTest`, 8 casos; 4 casos de cliente en `DashboardScreen.test.tsx`). Red → Green: la cita ajena o inexistente y la solicitud inexistente o ya resuelta provocaban `EmptyResultDataAccessException` sin manejar (error 500); `SchedulingJdbcAdapter.cancel/requestReschedule/decideReschedule` responden ahora `404`/`409`. Maven 48/48 (dos ejecuciones consecutivas) y Vitest verdes. Estado `En desarrollo` → `Completada`.
## Notas y decisiones
- Se conserva el significado exacto de `PENDING` del catálogo fijo.
