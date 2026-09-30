---
id: HU-012
tipo: historia-de-usuario
titulo: "Gestionar EPS"
estado: Completada
epica: "[[EP-003-administracion-de-catalogos-y-profesionales]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 3"
dependencias: ["[[HU-006-iniciar-sesion]]", "[[HU-002-modelar-persistencia-3fn]]", "[[HU-004-definir-contrato-rest-inicial]]"]
relacionadas: ["[[HU-013-gestionar-planes-eps]]", "[[HU-011-gestionar-afiliacion]]"]
---
# HU-012 — Gestionar EPS
## Historia de usuario
**COMO** ADMIN  
**QUIERO** crear, consultar, actualizar y activar/desactivar EPS  
**PARA** mantener el catálogo disponible para afiliaciones.
## Contexto y descripción
Es catálogo configurable; no se borra físicamente si está referenciado.
## Alcance
- CRUD lógico de EPS con autorización ADMIN y consulta de catálogo aplicable.
## Fuera de alcance
- Borrado físico de EPS referenciada o integración con EPS real.
## Reglas de negocio
- Referencias transaccionales se preservan mediante activación/desactivación.
## Dependencias y relaciones
- Épica: [[EP-003-administracion-de-catalogos-y-profesionales]]
- Dependencias: [[HU-006-iniciar-sesion]], [[HU-002-modelar-persistencia-3fn]], [[HU-004-definir-contrato-rest-inicial]].
- Relacionadas: [[HU-013-gestionar-planes-eps]], [[HU-011-gestionar-afiliacion]].
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** combina CRUD, referencias, seguridad y contrato.
## Tareas de desarrollo
- [x] **T-01 — Modelar operaciones ADMIN.** Dificultad: Medio. Definir validaciones y contrato.
- [x] **T-02 — Aplicar baja lógica.** Dificultad: Medio. Impedir borrado físico cuando hay referencias.
- [x] **T-03 — Integrar UI y pruebas.** Dificultad: Medio. Probar ADMIN/no ADMIN y catálogo activo.
## Criterios de aceptación
### CA-01 — CRUD autorizado
**Dado** un ADMIN, **cuando** gestiona una EPS válida, **entonces** puede crearla, consultarla o actualizarla según contrato.
### CA-02 — Protección de referencias
**Dado** una EPS referenciada, **cuando** ADMIN intenta retirarla, **entonces** no se borra físicamente y puede desactivarse.
### CA-03 — Restricción de rol
**Dado** un actor distinto de ADMIN, **cuando** intenta gestionar EPS, **entonces** se deniega.
## Definition of Done
- [x] CA-01 a CA-03 probados por rol y persistencia.
- [x] Migración/índices aplicables y cliente ADMIN verificados.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS (backend) | `InsuranceAdminIntegrationTest.adminCrudEpsAndLogicalDeactivationHidesItFromPublicList` | ADMIN crea, consulta (`GET /api/v1/admin/eps`) y actualiza (`PATCH`) EPS; código duplicado responde `409` (`duplicateEpsCodeConflicts`). |
| CA-02 | PASS (backend) | `InsuranceAdminIntegrationTest.adminCrudEpsAndLogicalDeactivationHidesItFromPublicList` | Sin borrado físico; `PATCH .../active:false` la retira del catálogo público `GET /api/v1/eps` conservando el registro y sus referencias en `eps_plans`. |
| CA-03 / DoD | PASS (backend) | `InsuranceAdminIntegrationTest.nonAdminCannotManageEpsOrPlans` | Rol distinto de ADMIN recibe `403` en lectura y escritura administrativa. Falta cliente ADMIN (T-03). |
| Frontend / T-03 | PASS | `InsuranceAdminTab.tsx` (pestaña *EPS y planes* de ADMIN), `s4Screens.test.tsx` HU-012; `npm run lint`, `npm test` 34/34, `npm run build` en `citas-web`; validación manual en Chrome | Crear, renombrar y activar/desactivar EPS con el mismo patrón visual que especialidades; `409` mostrado como código duplicado o EPS inactiva. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-29 — Backend implementado y verificado (Maven 33/33 verde): `GET/POST/PATCH /api/v1/admin/eps` y `GET /api/v1/eps`, clonado del patrón ya aprobado de `specialties`. Sin migración nueva (tabla `eps` ya existía desde V2). Frontend pendiente para una ronda posterior.
- 2026-09-30 — Frontend implementado y verificado en `citas-web` (pasada consolidada de S4 Incrementos 1 y 3): lint, 34/34 pruebas Vitest y build en verde, y flujo validado en Chrome contra el backend en Docker (perfil `local`). HU cerrada como `Completada`.
## Notas y decisiones
- Campos de catálogo aprobados por contrato: `code`, `name`, `active` (baja lógica). Sin atributos adicionales por ahora.
