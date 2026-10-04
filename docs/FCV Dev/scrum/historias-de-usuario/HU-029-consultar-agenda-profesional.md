---
id: HU-029
tipo: historia-de-usuario
titulo: "Consultar agenda profesional"
estado: Completada
epica: "[[EP-007-operacion-profesional-y-administrativa]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 6"
dependencias: ["[[HU-022-reservar-cita-general]]", "[[HU-024-resolver-solicitud-especializada]]"]
relacionadas: ["[[HU-030-cerrar-atencion]]"]
---
# HU-029 — Consultar agenda profesional
## Historia de usuario
**COMO** PROFESSIONAL  
**QUIERO** consultar mis citas `APPROVED` por día/semana y sede  
**PARA** organizar mi atención sin ver citas ajenas.
## Contexto y descripción
Es distinta del calendario de bloques de disponibilidad.
## Alcance
- Vista propia de citas aprobadas con filtros día/semana/sede y datos mínimos autorizados.
## Fuera de alcance
- Citas no aprobadas, agenda de otro profesional o historia clínica.
## Reglas de negocio
- Solo `APPROVED` de profesional autenticado; no revelar datos de usuarios fuera de sus citas.
## Dependencias y relaciones
- Épica: [[EP-007-operacion-profesional-y-administrativa]]
- Dependencias: [[HU-022-reservar-cita-general]], [[HU-024-resolver-solicitud-especializada]].
- Relacionadas: [[HU-030-cerrar-atencion]].
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** aplica filtros temporales, rol y restricción de PII.
## Tareas de desarrollo
- [x] **T-01 — Definir representación mínima.** Dificultad: Medio. Acordar campos necesarios para atención.
- [x] **T-02 — Aplicar consulta por ownership.** Dificultad: Medio. Filtrar estado, periodo y sede.
- [x] **T-03 — Entregar agenda/pruebas.** Dificultad: Medio. Probar aislamiento y filtros.
## Criterios de aceptación
### CA-01 — Agenda aprobada propia
**Dado** citas aprobadas asignadas, **cuando** PROFESSIONAL consulta, **entonces** ve solo las propias en estado `APPROVED`.
### CA-02 — Filtros operativos
**Dado** citas en distintas fechas/sedes, **cuando** filtra por día, semana o sede, **entonces** se muestran solo coincidencias.
### CA-03 — Privacidad
**Dado** otro profesional o cita ajena, **cuando** se consulta, **entonces** sus datos no se exponen.
## Definition of Done
- [x] CA-01 a CA-03 probados con rol/ownership, REST y cliente aplicable.
- [x] Campos mínimos y trazabilidad Scrum verificados.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS (backend) | `ProfessionalOperationsIntegrationTest.agendaListsOnlyOwnApprovedAppointmentsWithFilters` | `GET /api/v1/professional/appointments` solo devuelve citas propias `APPROVED`; excluye `REQUESTED` propia y citas de otro profesional. |
| CA-02 | PASS (backend) | `ProfessionalOperationsIntegrationTest.agendaListsOnlyOwnApprovedAppointmentsWithFilters` | Filtros `from`/`to`/`locationId` verificados de forma independiente. |
| CA-03 / DoD | PASS (backend) | mismo test | Aislamiento estructural: no existe parámetro de profesional en el endpoint, solo puede verse la propia agenda. Falta cliente (T-03). |
| Frontend / T-03 | PASS | `ProfessionalAgendaTab.tsx` (pestaña *Agenda* de PROFESSIONAL), `s4Screens.test.tsx` HU-029; `npm run lint`, `npm test` 34/34, `npm run build` en `citas-web`; validación manual en Chrome | Conmutador Día/Semana (lunes-domingo) mapeado a `from`/`to` y filtro de sede; estados de carga, vacío y error. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-29 — Backend implementado y verificado (Maven 40/40 verde): `GET /api/v1/professional/appointments?from=&to=&locationId=`. Frontend pendiente para una ronda posterior.
- 2026-09-30 — Frontend implementado y verificado en `citas-web` (pasada consolidada de S4 Incrementos 1 y 3): lint, 34/34 pruebas Vitest y build en verde, y flujo validado en Chrome contra el backend en Docker (perfil `local`). HU cerrada como `Completada`.
## Notas y decisiones
- Los campos visibles no amplían el PRD ni contienen historia clínica.
