---
id: HU-013
tipo: historia-de-usuario
titulo: "Gestionar planes de EPS"
estado: En desarrollo
epica: "[[EP-003-administracion-de-catalogos-y-profesionales]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 3"
dependencias: ["[[HU-012-gestionar-eps]]"]
relacionadas: ["[[HU-011-gestionar-afiliacion]]"]
---
# HU-013 — Gestionar planes de EPS
## Historia de usuario
**COMO** ADMIN  
**QUIERO** gestionar planes asociados a una EPS  
**PARA** que USER seleccione combinaciones de afiliación válidas.
## Contexto y descripción
El plan depende de EPS y tampoco se borra físicamente si está referenciado.
## Alcance
- CRUD lógico de planes, asociación a EPS y filtros de planes activos aplicables.
## Fuera de alcance
- Plan sin EPS, borrado físico referenciado o reglas comerciales reales.
## Reglas de negocio
- Un plan se relaciona con EPS; las referencias mantienen integridad.
## Dependencias y relaciones
- Épica: [[EP-003-administracion-de-catalogos-y-profesionales]]
- Dependencias: [[HU-012-gestionar-eps]].
- Relacionadas: [[HU-011-gestionar-afiliacion]].
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** exige integridad referencial y gestión administrativa segura.
## Tareas de desarrollo
- [ ] **T-01 — Definir contrato y relación plan-EPS.** Dificultad: Medio. Validar EPS existente/activa según regla aprobada.
- [ ] **T-02 — Implementar baja lógica.** Dificultad: Medio. Preservar planes referenciados.
- [ ] **T-03 — Entregar UI/pruebas.** Dificultad: Medio. Cubrir relación inválida, rol y consulta.
## Criterios de aceptación
### CA-01 — Plan asociado
**Dado** una EPS válida, **cuando** ADMIN crea o actualiza un plan, **entonces** el plan queda asociado a esa EPS.
### CA-02 — Integridad de catálogo
**Dado** un plan referenciado, **cuando** ADMIN intenta eliminarlo, **entonces** se conserva y se permite desactivación cuando aplique.
### CA-03 — Selección consistente
**Dado** un consumidor de afiliación, **cuando** consulta planes de una EPS, **entonces** solo puede seleccionar planes válidos de ella conforme al contrato.
## Definition of Done
- [ ] CA-01 a CA-03 probados en persistencia/REST y cliente aplicable.
- [ ] Relación 3FN, migración aplicable y autorización ADMIN verificadas.
- [ ] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS (backend) | `InsuranceAdminIntegrationTest.epsPlanValidatesExistingActiveEpsAndRegime` | `POST /api/v1/admin/eps-plans` exige `epsId`/`regimeId` existentes; el plan creado queda asociado a la EPS indicada. |
| CA-02 | PASS (backend) | `InsuranceAdminIntegrationTest.epsPlanValidatesExistingActiveEpsAndRegime` | Sin borrado físico; `PATCH .../active:false` conserva el registro y lo retira del catálogo público `GET /api/v1/catalogs/plans` (regresión de [[HU-011-gestionar-afiliacion]] verificada explícitamente). |
| CA-03 / DoD | PASS (backend) | `InsuranceAdminIntegrationTest.epsPlanValidatesExistingActiveEpsAndRegime` | EPS inexistente o régimen inválido devuelven `404`; EPS inactiva devuelve `409`; código de plan duplicado dentro de la misma EPS devuelve `409`. Falta cliente (T-03). |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-29 — Backend implementado y verificado (Maven 33/33 verde): `GET/POST/PATCH /api/v1/admin/eps-plans`, sin migración nueva (tabla `eps_plans` ya existía desde V2). Frontend pendiente para una ronda posterior.
## Notas y decisiones
- Confirmado: sin atributo comercial nuevo para el plan; solo `code`, `name`, `active` son editables por ADMIN además de la asociación `epsId`/`regimeId` fijada al crear.
