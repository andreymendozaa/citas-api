---
id: HU-018
tipo: historia-de-usuario
titulo: "Crear bloques de disponibilidad"
estado: Completada
epica: "[[EP-004-disponibilidad-del-profesional]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 4"
dependencias: ["[[HU-016-asignar-especialidades-al-profesional]]", "[[HU-017-asignar-sedes-y-estado-del-profesional]]"]
relacionadas: ["[[HU-019-modificar-bloques-futuros]]", "[[HU-021-buscar-disponibilidad]]"]
---
# HU-018 — Crear bloques de disponibilidad
## Historia de usuario
**COMO** PROFESSIONAL  
**QUIERO** crear bloques futuros por día y sede  
**PARA** publicar horarios reservables de mi agenda.
## Contexto y descripción
Puede crear múltiples bloques (por ejemplo mañana/tarde); cada uno se discretiza en slots de 30 min.
## Alcance
- Crear bloque futuro con inicio/fin/sede y generar disponibilidad discreta según diseño aprobado.
## Fuera de alcance
- Bloques pasados, solapados o en sedes no asignadas.
## Reglas de negocio
- Sin pasado/solapamiento; profesional activo y habilitado en sede; slots de 30 min.
## Dependencias y relaciones
- Épica: [[EP-004-disponibilidad-del-profesional]]
- Dependencias: [[HU-016-asignar-especialidades-al-profesional]], [[HU-017-asignar-sedes-y-estado-del-profesional]].
- Relacionadas: [[HU-019-modificar-bloques-futuros]], [[HU-021-buscar-disponibilidad]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** exige validación temporal, autorización, sede y base para concurrencia de reservas.
## Tareas de desarrollo
- [x] **T-01 — Modelar bloque y slots.** Dificultad: Alto. Preservar 3FN e índices de agenda.
- [x] **T-02 — Validar publicación.** Dificultad: Alto. Aplicar future-only, no solapamiento y sede/estado.
- [x] **T-03 — Entregar calendario/formulario y pruebas.** Dificultad: Alto. Cubrir casos válidos e inválidos.
## Criterios de aceptación
### CA-01 — Bloque futuro válido
**Dado** PROFESSIONAL activo asignado a una sede, **cuando** crea un bloque futuro válido, **entonces** queda disponible en slots de 30 minutos.
### CA-02 — Restricciones de bloque
**Dado** un bloque pasado, solapado o de sede no asignada, **cuando** intenta crearlo, **entonces** se rechaza sin publicar disponibilidad.
### CA-03 — Múltiples franjas
**Dado** un día sin conflicto, **cuando** crea dos franjas separadas, **entonces** ambas quedan disponibles sin incluir el intervalo intermedio.
## Definition of Done
- [x] CA-01 a CA-03 tienen pruebas de dominio/aplicación/REST y cliente aplicable.
- [x] Migración/índices de agenda aplicables y ownership de PROFESSIONAL verificados.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS | `retainsConsecutiveSlotsAndReleasesThemAfterAdministrativeRejection` (bloque → slots de 30 min); `schedulingApi.test.ts` (contrato de bloques) |  |
| CA-02 | PASS | `rejectsPastOverlappingAndUnavailableSchedulingConfiguration` (pasado, solapado, sede no habilitada) |  |
| CA-03 / DoD | PASS | `OfferAndAgendaRulesIntegrationTest.twoBlocksTheSameDayDoNotExposeTheGapBetweenThem` | Bloques 08–10 y 14–16: solo se ofrecen inicios dentro de cada franja. Para 60 min no aparece 09:30, porque 09:30 y 14:00 no son consecutivos. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-30 — Auditoría integral de CA/DoD contra código y pruebas (solicitada por el usuario): estado `Aprobada` → `En desarrollo`. Criterio de auditoría: PASS exige prueba automatizada o propiedad estructural; lo verificado solo por código o manualmente queda PARCIAL/PENDIENTE; la HU se completa solo sin CA pendientes.
- 2026-09-30 — CA-03 cubierto con `OfferAndAgendaRulesIntegrationTest.twoBlocksTheSameDayDoNotExposeTheGapBetweenThem` (pasó directamente). Maven 58/58. Estado `En desarrollo` → `Completada`.
## Notas y decisiones
- La representación interna de slots se decide en [[HU-002-modelar-persistencia-3fn]].
