---
id: HU-020
tipo: historia-de-usuario
titulo: "Consultar calendario de disponibilidad"
estado: Completada
epica: "[[EP-004-disponibilidad-del-profesional]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 4"
dependencias: ["[[HU-018-crear-bloques-de-disponibilidad]]"]
relacionadas: ["[[HU-029-consultar-agenda-profesional]]"]
---
# HU-020 — Consultar calendario de disponibilidad
## Historia de usuario
**COMO** PROFESSIONAL  
**QUIERO** consultar mi calendario de bloques  
**PARA** conocer la disponibilidad que publiqué.
## Contexto y descripción
La agenda de disponibilidad no sustituye la agenda visible de citas aprobadas.
## Alcance
- Consulta del calendario/bloques propios con sede y fecha.
## Fuera de alcance
- Datos de citas de otros profesionales o modificación desde esta consulta.
## Reglas de negocio
- Solo ownership; mostrar bloques/sedes sin exponer información de USER.
## Dependencias y relaciones
- Épica: [[EP-004-disponibilidad-del-profesional]]
- Dependencias: [[HU-018-crear-bloques-de-disponibilidad]].
- Relacionadas: [[HU-029-consultar-agenda-profesional]].
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** requiere filtros y aislamiento de datos en una vista de calendario.
## Tareas de desarrollo
- [x] **T-01 — Definir consulta/filtros.** Dificultad: Medio. Acordar fecha/sede y representación sin imponer UI.
- [x] **T-02 — Aplicar ownership.** Dificultad: Medio. Restringir al calendario del autenticado.
- [x] **T-03 — Entregar calendario y pruebas.** Dificultad: Medio. Probar filtro y ausencia de agenda ajena.
## Criterios de aceptación
### CA-01 — Visualización propia
**Dado** bloques propios publicados, **cuando** PROFESSIONAL consulta su calendario, **entonces** ve fecha, franja y sede de sus bloques.
### CA-02 — Filtro aplicable
**Dado** bloques en diversas fechas/sedes, **cuando** aplica los filtros del contrato, **entonces** recibe solo los bloques coincidentes.
### CA-03 — Aislamiento
**Dado** otro profesional, **cuando** intenta consultar calendario ajeno, **entonces** no obtiene esos bloques.
## Definition of Done
- [x] CA-01 a CA-03 probados en autorización/REST y cliente aplicable.
- [x] No se exponen datos de USER ni información ajena; trazabilidad actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS | `updatesAndDeletesFutureUncommittedBlocks` (`blocks(owner, …)`); `DashboardScreen.test.tsx` | Fecha, franja y sede visibles en *Disponibilidad*. |
| CA-02 | PASS | `OfferAndAgendaRulesIntegrationTest.calendarFiltersByDateAndLocation` | `GET /professional/availability-blocks` sin filtros, con `date`, con `locationId` y con ambos devuelve solo los bloques coincidentes y en orden. |
| CA-03 / DoD | PASS (estructural) | El profesional se resuelve desde el JWT; no existe parámetro de profesional; bloque ajeno → `404` (`foreignCommittedAndPastBlocksCannotBeEditedOrDeleted`) | Mismo aislamiento que HU-029. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-30 — Auditoría integral de CA/DoD contra código y pruebas (solicitada por el usuario): estado `Aprobada` → `En desarrollo`. Criterio de auditoría: PASS exige prueba automatizada o propiedad estructural; lo verificado solo por código o manualmente queda PARCIAL/PENDIENTE; la HU se completa solo sin CA pendientes.
- 2026-09-30 — CA-02 cubierto con `OfferAndAgendaRulesIntegrationTest.calendarFiltersByDateAndLocation` (pasó directamente). Maven 58/58. Estado `En desarrollo` → `Completada`. Queda como mejora de UI, fuera de los CA, que la pestaña *Disponibilidad* exponga estos filtros.
## Notas y decisiones
- El formato visual queda bajo el diseño aprobado.
