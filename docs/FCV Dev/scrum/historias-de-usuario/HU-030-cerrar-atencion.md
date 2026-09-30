---
id: HU-030
tipo: historia-de-usuario
titulo: "Cerrar atención"
estado: Completada
epica: "[[EP-007-operacion-profesional-y-administrativa]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 6"
dependencias: ["[[HU-029-consultar-agenda-profesional]]"]
relacionadas: ["[[HU-032-consultar-auditoria-de-estados]]"]
---
# HU-030 — Cerrar atención
## Historia de usuario
**COMO** PROFESSIONAL  
**QUIERO** marcar una cita pasada/aplicable como `COMPLETED` o `NO_SHOW`  
**PARA** cerrar su atención con trazabilidad.
## Contexto y descripción
El PRD no permite que PROFESSIONAL haga otras decisiones de cita.
## Alcance
- Validar cita propia y aplicable, transición explícita e historial.
## Fuera de alcance
- Cerrar cita ajena/no aplicable, cancelar o aprobar solicitudes.
## Reglas de negocio
- Solo `COMPLETED`/`NO_SHOW`; debe registrarse historial.
## Dependencias y relaciones
- Épica: [[EP-007-operacion-profesional-y-administrativa]]
- Dependencias: [[HU-029-consultar-agenda-profesional]].
- Relacionadas: [[HU-032-consultar-auditoria-de-estados]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** requiere transición de estado, temporalidad y ownership rigurosos.
## Tareas de desarrollo
- [x] **T-01 — Definir aplicabilidad.** Dificultad: Alto. Traducir “pasada/aplicable” a condición verificable aprobada.
- [x] **T-02 — Aplicar transición/auditoría.** Dificultad: Alto. Restringir actor/estado y guardar fuente/fecha.
- [x] **T-03 — Integrar acción/pruebas.** Dificultad: Medio. Cubrir ambos resultados y casos denegados.
## Criterios de aceptación
### CA-01 — Cierre permitido
**Dado** una cita propia pasada/aplicable, **cuando** PROFESSIONAL selecciona `COMPLETED` o `NO_SHOW`, **entonces** se registra el estado elegido.
### CA-02 — Restricción de propiedad/aplicabilidad
**Dado** una cita ajena o no aplicable, **cuando** PROFESSIONAL intenta cerrarla, **entonces** la aplicación lo impide.
### CA-03 — Auditoría
**Dado** un cierre exitoso, **cuando** se consulta historial, **entonces** consta cita, estado nuevo, actor/fuente y fecha/hora aplicables.
## Definition of Done
- [x] CA-01 a CA-03 probados en dominio/REST, autorización y auditoría.
- [x] La definición operativa de “aplicable” queda acordada/documentada antes de completar.
- [x] Cliente y trazabilidad Scrum actualizados.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS (backend) | `ProfessionalOperationsIntegrationTest.closesAppointmentsWithGuardsAndHistory`, `closesAppointmentAsNoShow` | `POST /api/v1/professional/appointments/{id}/closure` registra `COMPLETED` o `NO_SHOW` sobre cita propia aplicable. |
| CA-02 | PASS (backend) | mismo archivo | Cita ajena → `404`; cita no `APPROVED` o ya cerrada → `409`; cita aún no finalizada → `409`; `result` inválido → `400`. |
| CA-03 / DoD | PASS (backend) | mismo archivo | Historial registra `change_source='PROFESSIONAL'` y `changed_by_user_id`. Falta cliente (T-03). |
| Frontend / T-03 | PASS | `ProfessionalAgendaTab.tsx`, `s4Screens.test.tsx` HU-030, `schedulingApi.test.ts`; `npm run lint`, `npm test` 34/34, `npm run build` en `citas-web`; validación manual en Chrome | Botones *Atendida*/*No asistió* solo cuando `endAt` ya pasó (hora Bogotá), con observación opcional; el backend sigue siendo la autoridad (`409` traducido). Cierre en vivo registró `COMPLETED` con fuente `PROFESSIONAL`. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-29 — Backend implementado y verificado (Maven 40/40 verde): `POST /api/v1/professional/appointments/{id}/closure`. Frontend pendiente para una ronda posterior.
- 2026-09-30 — Frontend implementado y verificado en `citas-web` (pasada consolidada de S4 Incrementos 1 y 3): lint, 34/34 pruebas Vitest y build en verde, y flujo validado en Chrome contra el backend en Docker (perfil `local`). HU cerrada como `Completada`.
## Notas y decisiones
- Condición “aplicable” resuelta por contrato (`S4.md`): cita propia, `APPROVED`, y `scheduledEndAt` ya transcurrido respecto al reloj `America/Bogota`.
