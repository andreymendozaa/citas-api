---
id: HU-011
tipo: historia-de-usuario
titulo: "Gestionar afiliación"
estado: Completada
epica: "[[EP-002-identidad-y-perfil-del-usuario]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 2"
dependencias: ["[[HU-003-publicar-catalogos-fijos]]"]
relacionadas: []
---
# HU-011 — Gestionar afiliación
## Historia de usuario
**COMO** visitante que se registra
**QUIERO** seleccionar opcionalmente un plan activo
**PARA** guardar una afiliación normalizada como dato administrativo de mi cuenta.
## Contexto y descripción
EPS y planes son configurables; régimen es catálogo fijo.
## Alcance
- Crear una afiliación inicial opcional desde el registro usando un plan activo.
- Consultar y cambiar la afiliación propia desde el perfil, sin duplicar planes (ampliado el 2026-09-30 por decisión del usuario para alinear el alcance con CA-02/CA-03 y con el PRD RF-04).
## Fuera de alcance
- CRUD ADMIN de EPS/planes (HU-012/HU-013), eliminación de la afiliación, número de afiliado editable y uso de la afiliación en reglas de agenda.
## Reglas de negocio
- La afiliación usa FKs; no afecta disponibilidad, precio, aprobación ni reserva.
## Dependencias y relaciones
- Épica: [[EP-002-identidad-y-perfil-del-usuario]]
- Dependencias: [[HU-003-publicar-catalogos-fijos]].
- Relacionadas: Ninguna.
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** enlaza catálogos configurables/fijos, integridad y ownership.
## Tareas de desarrollo
- [x] **T-01 — Publicar planes activos.** Dificultad: Medio. Usar la relación normalizada EPS/plan/régimen.
- [x] **T-02 — Extender registro opcional.** Dificultad: Alto. Validar plan activo y crear la afiliación sin duplicar textos de catálogo.
- [x] **T-03 — Probar selección u omisión.** Dificultad: Medio. La omisión no impide el registro ni afecta agenda.
## Criterios de aceptación
### CA-01 — Asociación válida
**Dado** catálogos activos y una combinación válida, **cuando** USER guarda afiliación, **entonces** queda asociada a su perfil.
### CA-02 — Sin duplicidad
**Dado** una afiliación existente, **cuando** USER repite EPS, régimen o plan dentro de su afiliación, **entonces** la aplicación evita la duplicación definida.
### CA-03 — Aislamiento por usuario
**Dado** un USER autenticado, **cuando** consulta o modifica afiliación, **entonces** solo opera sobre su propia afiliación.
## Definition of Done
- [x] CA-01 a CA-03 probados en dominio/REST y cliente aplicable.
- [x] Persistencia 3FN y migración aplicable verificadas; no hay textos de catálogo duplicados.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS | `AffiliationIntegrationTest.userAssociatesAndConsultsTheirAffiliation`, `onlySelectablePlansAreAccepted`; `AuthIntegrationTest.registrationCreatesOptionalCurrentInsuranceAffiliation`; `s4Screens.test.tsx` (HU-011 asocia un plan); validación manual en Chrome | Sin afiliación → `204`; plan válido → `200` con EPS, plan, régimen y número. Plan inactivo, de EPS inactiva, inexistente o ausente → `400` sin cambios. |
| CA-02 | PASS | `AffiliationIntegrationTest.plansAreNeverDuplicatedWithinTheAffiliation`; `s4Screens.test.tsx` (no permite guardar el mismo plan) | Mismo plan vigente → `409`; volver a un plan anterior reactiva su fila. Nunca hay dos filas del mismo plan ni más de una vigente. La UI deshabilita guardar el plan vigente. |
| CA-03 / DoD | PASS | `AffiliationIntegrationTest.affiliationIsIsolatedPerUserAndRestrictedToUsers`; `schedulingApi.test.ts` (contrato GET/PUT) | Cada USER solo ve y cambia la suya; ADMIN/PROFESSIONAL → `403`; sin token → `401`. 3FN: la afiliación solo guarda FKs (`plan_id` → EPS y régimen), sin textos de catálogo duplicados; V5 solo agrega filas al catálogo fijo. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-30 — Decisiones del usuario: (1) ampliar el alcance a consultar y cambiar la afiliación propia; (2) sembrar los 5 regímenes del modelo de referencia con la migración V5 (sin tocar V1–V4). Implementado: `GET/PUT /api/v1/users/me/affiliation` (solo USER; ownership por JWT), `Ports.Affiliations.current/changeCurrent` (transaccional; volver a un plan anterior reactiva su fila), planes seleccionables solo si el plan y su EPS están activos (también en `GET /catalogs/plans`), y la sección *Mi afiliación* en *Mi perfil*. Maven y Vitest en verde; validado en vivo en Chrome (asociar Particular → cambiar a Contributivo; en BD: 2 filas, 1 vigente). Estado `Aprobada` → `Completada`.
## Notas y decisiones
- La regla de vigencia de una EPS/plan se abordará con sus HU administrativas.
