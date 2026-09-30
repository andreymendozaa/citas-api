---
id: HU-017
tipo: historia-de-usuario
titulo: "Asignar sedes y estado del profesional"
estado: Completada
epica: "[[EP-003-administracion-de-catalogos-y-profesionales]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 3"
dependencias: ["[[HU-015-crear-profesional]]", "[[HU-003-publicar-catalogos-fijos]]"]
relacionadas: ["[[HU-018-crear-bloques-de-disponibilidad]]"]
---
# HU-017 — Asignar sedes y estado del profesional
## Historia de usuario
**COMO** ADMIN  
**QUIERO** asignar una o ambas sedes y activar/desactivar a PROFESSIONAL  
**PARA** controlar dónde y cuándo puede publicar disponibilidad.
## Contexto y descripción
Las dos sedes son catálogo fijo. La habilitación es requisito de agenda.
## Alcance
- Asociar sedes fijas y cambiar estado de profesional desde rol ADMIN.
## Fuera de alcance
- Crear sedes, asignar más de las fijas o publicar bloques como profesional inhabilitado.
## Reglas de negocio
- El profesional trabaja en una o ambas sedes; solo publica bloques en sedes asignadas.
## Dependencias y relaciones
- Épica: [[EP-003-administracion-de-catalogos-y-profesionales]]
- Dependencias: [[HU-015-crear-profesional]], [[HU-003-publicar-catalogos-fijos]].
- Relacionadas: [[HU-018-crear-bloques-de-disponibilidad]].
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** combina N:M de sedes, estado y reglas de agenda.
## Tareas de desarrollo
- [x] **T-01 — Modelar asignación/estado.** Dificultad: Medio. Usar catálogo fijo y relación normalizada.
- [x] **T-02 — Aplicar gestión ADMIN.** Dificultad: Medio. Validar que solo se asigne sede fija.
- [x] **T-03 — Integrar/verificar agenda.** Dificultad: Medio. Probar inhabilitado o sede no asignada.
## Criterios de aceptación
### CA-01 — Sedes permitidas
**Dado** ADMIN y un profesional, **cuando** asigna sedes, **entonces** puede asociar HIC, ICV o ambas, sin valores externos.
### CA-02 — Estado administrable
**Dado** un profesional, **cuando** ADMIN lo activa o desactiva, **entonces** el estado queda aplicado para las reglas de disponibilidad.
### CA-03 — Publicación restringida
**Dado** un profesional inactivo o una sede no asignada, **cuando** intenta crear disponibilidad, **entonces** la aplicación lo impide.
## Definition of Done
- [x] CA-01 a CA-03 probados por rol, relación y regla de agenda.
- [x] Persistencia/migración aplicable y cliente ADMIN verificados.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS | `setProfessionalLocations` en las suites de integración; `LocationsRequest @Size(max=2)` y FK a `locations`; validación manual en Chrome | Solo sedes del catálogo (HIC/ICV), una o ambas. |
| CA-02 | PASS | `SchedulingServiceIntegrationTest.rejectsReservationsForAnUnavailableProfessionalOrLocation`, `rejectsPastOverlappingAndUnavailableSchedulingConfiguration` | `setProfessionalActive(false)` bloquea reserva y publicación. |
| CA-03 / DoD | PASS | `rejectsPastOverlappingAndUnavailableSchedulingConfiguration` (sede no habilitada, profesional inactivo) | Cliente ADMIN: asignación al crear y activar/desactivar (`DashboardScreen`, pestaña Oferta). Observación: la UI no reasigna sedes después del alta. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-30 — Auditoría integral de CA/DoD contra código y pruebas (solicitada por el usuario): estado `Aprobada` → `Completada`. Criterio de auditoría: PASS exige prueba automatizada o propiedad estructural; lo verificado solo por código o manualmente queda PARCIAL/PENDIENTE; la HU se completa solo sin CA pendientes.
## Notas y decisiones
- No se define el efecto retroactivo sobre citas existentes al desactivar; debe mantener integridad PRD.
