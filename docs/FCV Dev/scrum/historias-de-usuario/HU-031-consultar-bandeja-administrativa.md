---
id: HU-031
tipo: historia-de-usuario
titulo: "Consultar bandeja administrativa"
estado: En desarrollo
epica: "[[EP-007-operacion-profesional-y-administrativa]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 6"
dependencias: ["[[HU-023-solicitar-cita-especializada]]", "[[HU-027-solicitar-reprogramacion]]"]
relacionadas: ["[[HU-024-resolver-solicitud-especializada]]", "[[HU-028-resolver-reprogramacion]]"]
---
# HU-031 — Consultar bandeja administrativa
## Historia de usuario
**COMO** ADMIN  
**QUIERO** ver citas especializadas `REQUESTED` y reprogramaciones `PENDING` con filtros  
**PARA** priorizar las decisiones pendientes.
## Contexto y descripción
Filtros requeridos: sede, profesional, especialidad y fecha.
## Alcance
- Bandeja separable/identificable de pendientes y filtros del PRD.
## Fuera de alcance
- Decidir la solicitud desde esta HU o mostrar citas no pendientes como parte de bandeja.
## Reglas de negocio
- Solo ADMIN; muestra `REQUESTED` especializada y `PENDING` reprogramación.
## Dependencias y relaciones
- Épica: [[EP-007-operacion-profesional-y-administrativa]]
- Dependencias: [[HU-023-solicitar-cita-especializada]], [[HU-027-solicitar-reprogramacion]].
- Relacionadas: [[HU-024-resolver-solicitud-especializada]], [[HU-028-resolver-reprogramacion]].
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** combina dos tipos de pendientes, filtros y autorización.
## Tareas de desarrollo
- [ ] **T-01 — Definir consulta de bandeja.** Dificultad: Medio. Acordar representación y filtros.
- [ ] **T-02 — Aplicar selección de pendientes.** Dificultad: Medio. Obtener solo estados/roles requeridos.
- [ ] **T-03 — Entregar UI/pruebas.** Dificultad: Medio. Probar filtros y restricción ADMIN.
## Criterios de aceptación
### CA-01 — Pendientes correctos
**Dado** solicitudes existentes, **cuando** ADMIN abre la bandeja, **entonces** ve especializadas `REQUESTED` y reprogramaciones `PENDING`.
### CA-02 — Filtros requeridos
**Dado** pendientes de distintas sedes/profesionales/especialidades/fechas, **cuando** ADMIN filtra, **entonces** se muestran las coincidencias.
### CA-03 — Acceso restringido
**Dado** un USER o PROFESSIONAL, **cuando** intenta abrir la bandeja, **entonces** se deniega.
## Definition of Done
- [ ] CA-01 a CA-03 probados en REST, rol y cliente aplicable.
- [ ] Contrato no expone información no necesaria; trazabilidad actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS (backend) | `InboxIntegrationTest.inboxCombinesSpecializedAndRescheduleAndAppliesFiltersAdminOnly` | `GET /api/v1/admin/inbox` combina especializadas `REQUESTED` y reprogramaciones `PENDING`. |
| CA-02 | PASS (backend) | mismo test | Filtro por `specialtyId` verificado (aplica igual a `locationId`/`professionalId`/`date`, mismo mecanismo). |
| CA-03 / DoD | PASS (backend) | mismo test | USER y PROFESSIONAL reciben `403`. Se conservan `/admin/appointments/pending-specialized` y `/admin/reschedule-requests/pending` por compatibilidad. Falta cliente (T-03). |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-29 — Backend implementado y verificado (Maven 40/40 verde) como parte del Incremento 3, junto con HU-029/030/032: `GET /api/v1/admin/inbox`. Frontend pendiente para una ronda posterior.
## Notas y decisiones
- La UI de decisión corresponde a sus HU relacionadas.
