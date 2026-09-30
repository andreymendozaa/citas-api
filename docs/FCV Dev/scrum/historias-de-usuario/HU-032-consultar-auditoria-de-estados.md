---
id: HU-032
tipo: historia-de-usuario
titulo: "Consultar auditoría de estados"
estado: Completada
epica: "[[EP-007-operacion-profesional-y-administrativa]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 6"
dependencias: ["[[HU-022-reservar-cita-general]]", "[[HU-024-resolver-solicitud-especializada]]", "[[HU-026-cancelar-cita]]", "[[HU-028-resolver-reprogramacion]]", "[[HU-030-cerrar-atencion]]"]
relacionadas: []
---
# HU-032 — Consultar auditoría de estados
## Historia de usuario
**COMO** actor autorizado  
**QUIERO** consultar el historial de estados de una cita dentro de mi alcance  
**PARA** entender decisiones y transiciones sin editar la auditoría.
## Contexto y descripción
Cada cambio guarda cita, estado nuevo, actor cuando existe, fuente SYSTEM/USER/ADMIN, fecha/hora y motivo opcional.
## Alcance
- Registro inmutable en cada transición y lectura protegida según ownership/rol aprobado.
## Fuera de alcance
- CRUD normal de auditoría o acceso sin autorización.
## Reglas de negocio
- Auditoría no modificable; cambios de estado explícitos y verificables.
## Dependencias y relaciones
- Épica: [[EP-007-operacion-profesional-y-administrativa]]
- Dependencias: [[HU-022-reservar-cita-general]], [[HU-024-resolver-solicitud-especializada]], [[HU-026-cancelar-cita]], [[HU-028-resolver-reprogramacion]], [[HU-030-cerrar-atencion]].
- Relacionadas: Ninguna.
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** es transversal a transiciones, fuentes, seguridad e inmutabilidad.
## Tareas de desarrollo
- [x] **T-01 — Centralizar registro de transición.** Dificultad: Alto. Evitar cambios sin historial.
- [x] **T-02 — Definir consulta autorizada.** Dificultad: Alto. Aplicar ownership/rol y campos del PRD.
- [x] **T-03 — Probar inmutabilidad/cobertura.** Dificultad: Alto. Comprobar todas las fuentes/estados aplicables.
## Criterios de aceptación
### CA-01 — Datos de auditoría completos
**Dado** un cambio de estado, **cuando** se registra, **entonces** incluye cita, estado nuevo, actor cuando existe, fuente, fecha/hora y motivo opcional.
### CA-02 — Inmutabilidad
**Dado** un registro histórico, **cuando** se intenta modificarlo por operaciones normales, **entonces** no es posible.
### CA-03 — Lectura restringida
**Dado** una consulta de historial, **cuando** el actor no tiene ownership/rol suficiente, **entonces** no obtiene la auditoría fuera de su alcance.
## Definition of Done
- [x] CA-01 a CA-03 probados con cada transición relevante, persistencia y autorización.
- [x] Migración/índices aplicables y contrato de lectura verificados.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS (backend) | `AppointmentHistoryIntegrationTest.historyRespectsOwnershipAcrossRolesAndIsInvisibleOutsideScope` | Cada transición ya centralizada (`reserve`, `cancel`, `decide`, `decideReschedule`, `closeAppointment`) registra cita, estado, actor cuando existe, fuente y fecha en `appointment_status_history`. |
| CA-02 | PASS (backend) | Revisión de código | No existe ningún endpoint de escritura sobre `appointment_status_history`; solo se inserta internamente en cada transición. |
| CA-03 / DoD | PASS (backend) | mismo test | `GET /api/v1/appointments/{id}/history`: USER dueño, PROFESSIONAL asignado y ADMIN acceden; USER ajeno, PROFESSIONAL no asignado y sin token → denegado (`404`/`401`, sin filtrar existencia). Falta cliente (T-03). |
| Frontend / T-03 | PASS | `AppointmentHistory.tsx` en *Mis citas* (USER), *Agenda* (PROFESSIONAL) y *Bandeja* (ADMIN), `s4Screens.test.tsx` HU-032; `npm run lint`, `npm test` 34/34, `npm run build` en `citas-web`; validación manual en Chrome | Línea de tiempo de solo lectura (estado, fuente, fecha, motivo); `404` mostrado como historial no disponible sin distinguir inexistencia de falta de alcance. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-29 — Backend implementado y verificado (Maven 40/40 verde): `GET /api/v1/appointments/{id}/history`. Frontend pendiente para una ronda posterior.
- 2026-09-30 — Frontend implementado y verificado en `citas-web` (pasada consolidada de S4 Incrementos 1 y 3): lint, 34/34 pruebas Vitest y build en verde, y flujo validado en Chrome contra el backend en Docker (perfil `local`). HU cerrada como `Completada`.
## Notas y decisiones
- Rol de lectura resuelto por contrato: los tres roles (`USER`, `PROFESSIONAL`, `ADMIN`) pueden leer, con ownership variable resuelta en la consulta (`admin` o `patient_user_id` o `professional.user_id`).
