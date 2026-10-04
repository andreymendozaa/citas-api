---
id: HU-016
tipo: historia-de-usuario
titulo: "Asignar especialidades al profesional"
estado: Completada
epica: "[[EP-003-administracion-de-catalogos-y-profesionales]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 3"
dependencias: ["[[HU-014-gestionar-especialidades-y-duracion]]", "[[HU-015-crear-profesional]]"]
relacionadas: ["[[HU-021-buscar-disponibilidad]]"]
---
# HU-016 — Asignar especialidades al profesional
## Historia de usuario
**COMO** ADMIN  
**QUIERO** asignar una o más especialidades y una primaria a PROFESSIONAL  
**PARA** habilitar reservas solo en su oferta autorizada.
## Contexto y descripción
La relación es N:M; se requiere exactamente la marcación primaria que indique el PRD cuando exista asignación.
## Alcance
- Gestionar asociaciones a especialidades activas y designar/cambiar primaria.
## Fuera de alcance
- Modificar duración por profesional o asignar especialidad inexistente/inactiva.
## Reglas de negocio
- Una especialidad debe estar activa y asociada al profesional para reservarse.
## Dependencias y relaciones
- Épica: [[EP-003-administracion-de-catalogos-y-profesionales]]
- Dependencias: [[HU-014-gestionar-especialidades-y-duracion]], [[HU-015-crear-profesional]].
- Relacionadas: [[HU-021-buscar-disponibilidad]].
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** aplica N:M, vigencia y una regla de primariedad.
## Tareas de desarrollo
- [x] **T-01 — Modelar asociación N:M.** Dificultad: Medio. Garantizar integridad y primaria aprobada.
- [x] **T-02 — Exponer gestión ADMIN.** Dificultad: Medio. Validar especialidad activa y rol.
- [x] **T-03 — Probar y reflejar en UI.** Dificultad: Medio. Cubrir múltiple, primaria y reserva no habilitada.
## Criterios de aceptación
### CA-01 — Múltiples especialidades
**Dado** ADMIN, profesional y especialidades activas, **cuando** realiza asignaciones válidas, **entonces** el profesional puede tener una o más asociaciones.
### CA-02 — Especialidad primaria
**Dado** asociaciones del profesional, **cuando** ADMIN define la primaria, **entonces** la selección queda identificada de forma consistente.
### CA-03 — Reserva restringida
**Dado** una especialidad no asociada o inactiva, **cuando** se busca/reserva con ese profesional, **entonces** no se ofrece como opción válida.
## Definition of Done
- [x] CA-01 a CA-03 validados con pruebas de relación, rol y disponibilidad.
- [x] Persistencia 3FN/migración aplicable y cliente ADMIN verificados.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS | `AuthIntegrationTest.s3AppointmentRoutesKeepGeneralAndSpecializedContracts`; `SchedulingServiceIntegrationTest.listsProfessionalsWithTheirActiveAssignments`; `DashboardScreen.test.tsx` (HU-016 reasigna) | Un profesional admite varias especialidades, también al reasignar. |
| CA-02 | PASS | `OfferAndAgendaRulesIntegrationTest.primarySpecialtyIsSingleAndConsistent` (incluye `primarySpecialtyId` en `GET /admin/professionals`); `DashboardScreen.test.tsx` (HU-016 crea con primaria explícita / reasigna) | Una sola primaria, editable; primaria fuera de la lista → `400`. La UI ADMIN la elige explícitamente al crear y al editar. |
| CA-03 / DoD | PASS | `OfferAndAgendaRulesIntegrationTest.specialtyNotAssociatedWithTheProfessionalIsNeitherOfferedNorBookable`; `rejectsPastOverlappingAndUnavailableSchedulingConfiguration` (inactiva) | Especialidad no asociada o inactiva: no se ofrece ni se reserva. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-30 — Auditoría integral de CA/DoD contra código y pruebas (solicitada por el usuario): estado `Aprobada` → `En desarrollo`. Criterio de auditoría: PASS exige prueba automatizada o propiedad estructural; lo verificado solo por código o manualmente queda PARCIAL/PENDIENTE; la HU se completa solo sin CA pendientes.
- 2026-09-30 — CA-02 y CA-03 cubiertos en backend con `OfferAndAgendaRulesIntegrationTest.primarySpecialtyIsSingleAndConsistent` y `specialtyNotAssociatedWithTheProfessionalIsNeitherOfferedNorBookable` (pasaron directamente). Queda `En desarrollo` solo por el cliente: la pantalla ADMIN no permite elegir la especialidad primaria (toma la primera marcada) ni reasignar especialidades después del alta.
- 2026-09-30 — Cliente completado: `ProfessionalAssignments.tsx` añade un selector explícito de especialidad primaria en el alta y un editor de asignaciones (especialidades, primaria y sedes) para profesionales existentes; la lista muestra nombres y marca la primaria con ★. `GET /admin/professionals` expone `primarySpecialtyId` (cambio aditivo) para no perder la primaria al editar. Estado `En desarrollo` → `Completada`.
## Notas y decisiones
- La unicidad exacta de primaria se justificará en modelo/contrato.
