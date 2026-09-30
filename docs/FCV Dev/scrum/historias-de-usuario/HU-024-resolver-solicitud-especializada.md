---
id: HU-024
tipo: historia-de-usuario
titulo: "Resolver solicitud especializada"
estado: Completada
epica: "[[EP-005-busqueda-y-reserva-de-citas]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 4"
dependencias: ["[[HU-023-solicitar-cita-especializada]]"]
relacionadas: ["[[HU-031-consultar-bandeja-administrativa]]", "[[HU-032-consultar-auditoria-de-estados]]"]
---
# HU-024 — Resolver solicitud especializada
## Historia de usuario
**COMO** ADMIN  
**QUIERO** aprobar o rechazar una cita especializada solicitada  
**PARA** decidir la atención y liberar la franja cuando corresponda.
## Contexto y descripción
El rechazo exige motivo; aprobar cambia a `APPROVED`, rechazar a `REJECTED` y libera slots.
## Alcance
- Decisión ADMIN sobre `REQUESTED`, validación de transición, motivo de rechazo, slots e historial.
## Fuera de alcance
- Decidir citas generales o modificar selección clínica.
## Reglas de negocio
- Rechazo exige motivo; transiciones explícitas; rechazo libera la reserva.
## Dependencias y relaciones
- Épica: [[EP-005-busqueda-y-reserva-de-citas]]
- Dependencias: [[HU-023-solicitar-cita-especializada]].
- Relacionadas: [[HU-031-consultar-bandeja-administrativa]], [[HU-032-consultar-auditoria-de-estados]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** coordina autorización, transición, liberación y auditoría.
## Tareas de desarrollo
- [x] **T-01 — Definir decisión ADMIN.** Dificultad: Medio. Acordar precondición `REQUESTED`, motivo y errores.
- [x] **T-02 — Aplicar transición atómica.** Dificultad: Alto. Aprobar o rechazar/liberar sin estados intermedios.
- [x] **T-03 — Integrar bandeja/pruebas.** Dificultad: Alto. Cubrir rol, motivo obligatorio, slots e historial.
## Criterios de aceptación
### CA-01 — Aprobación
**Dado** una cita `REQUESTED`, **cuando** ADMIN aprueba, **entonces** pasa a `APPROVED` y conserva la reserva.
### CA-02 — Rechazo con motivo
**Dado** una cita `REQUESTED`, **cuando** ADMIN rechaza con motivo, **entonces** pasa a `REJECTED`, registra motivo y libera slots.
### CA-03 — Decisión válida
**Dado** actor no ADMIN, estado distinto de `REQUESTED` o rechazo sin motivo, **cuando** intenta decidir, **entonces** se rechaza sin transición.
## Definition of Done
- [x] CA-01 a CA-03 probados con persistencia/REST, cliente ADMIN y auditoría.
- [x] Transición/liberación atómica y contrato cross-repo verificables.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS | `SchedulingServiceIntegrationTest.administrativeApprovalRetainsSlotsAndWritesHistory`; `SpecializedDecisionIntegrationTest.onlyRequestedSpecializedAppointmentsCanBeDecided`; `s4Screens.test.tsx` (HU-031) | `APPROVED` conserva los slots; historial `ADMIN`. |
| CA-02 | PASS | `retainsConsecutiveSlotsAndReleasesThemAfterAdministrativeRejection` (motivo, slots liberados, historial ADMIN); `s3AppointmentRoutes…` (`rejectionReason`); `s4Screens.test.tsx` (motivo obligatorio en la bandeja) |  |
| CA-03 / DoD | PASS | `SpecializedDecisionIntegrationTest.invalidActorsReasonsAndDecisionsDoNotTransition`, `onlyRequestedSpecializedAppointmentsCanBeDecided` | USER/PROFESSIONAL → `403`; rechazo sin motivo o con motivo en blanco y decisión inválida → `400`; cita inexistente → `404`; cita general, ya decidida o cancelada → `409`. Sin transición, sin cambios de slots ni historial adicional. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-30 — Auditoría integral de CA/DoD contra código y pruebas (solicitada por el usuario): estado `Aprobada` → `En desarrollo`. Criterio de auditoría: PASS exige prueba automatizada o propiedad estructural; lo verificado solo por código o manualmente queda PARCIAL/PENDIENTE; la HU se completa solo sin CA pendientes.
- 2026-09-30 — CA-03 cubierto con `SpecializedDecisionIntegrationTest` (guardas de actor, motivo y decisión; solo citas `REQUESTED` especializadas). Red → Green: decidir sobre una cita inexistente, general o ya resuelta/cancelada provocaba `EmptyResultDataAccessException` sin manejar (error 500); `SchedulingJdbcAdapter.decide` responde ahora `404`/`409`. La prueba usa `@AfterEach` para resolver sus citas `REQUESTED` aunque falle, y así no contamina la BD de pruebas persistente. Maven 51/51 en dos ejecuciones consecutivas. Estado `En desarrollo` → `Completada`.
## Notas y decisiones
- La bandeja se especifica en [[HU-031-consultar-bandeja-administrativa]].
