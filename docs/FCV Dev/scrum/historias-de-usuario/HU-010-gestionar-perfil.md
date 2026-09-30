---
id: HU-010
tipo: historia-de-usuario
titulo: "Gestionar perfil"
estado: En desarrollo
epica: "[[EP-002-identidad-y-perfil-del-usuario]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 2"
dependencias: ["[[HU-006-iniciar-sesion]]", "[[HU-004-definir-contrato-rest-inicial]]"]
relacionadas: ["[[HU-011-gestionar-afiliacion]]"]
---
# HU-010 — Gestionar perfil
## Historia de usuario
**COMO** USER autenticado  
**QUIERO** consultar y actualizar los datos permitidos de mi perfil  
**PARA** mantener mi información de contacto vigente.
## Contexto y descripción
El PRD no enumera cuáles campos de identidad pueden cambiar; esa limitación debe resolverse en contrato sin inventarla.
## Alcance
- Consulta por ownership, edición de campos permitidos aprobados y validación server-side.
## Fuera de alcance
- Cambio de rol, acceso a perfil ajeno, y campos no autorizados por contrato.
## Reglas de negocio
- Ownership obligatorio; unicidad se preserva si los campos editables incluyen email/documento.
## Dependencias y relaciones
- Épica: [[EP-002-identidad-y-perfil-del-usuario]]
- Dependencias: [[HU-006-iniciar-sesion]], [[HU-004-definir-contrato-rest-inicial]].
- Relacionadas: [[HU-011-gestionar-afiliacion]].
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** combina ownership, validación y ambigüedad de campos permitidos.
## Tareas de desarrollo
- [ ] **T-01 — Acordar campos editables.** Dificultad: Medio. Documentar el límite sin ampliar el PRD.
- [ ] **T-02 — Implementar consulta/actualización propia.** Dificultad: Medio. Aplicar ownership, validación y unicidad.
- [ ] **T-03 — Integrar pantalla y pruebas.** Dificultad: Medio. Mostrar datos y errores de validación autorizados.
## Criterios de aceptación
### CA-01 — Consulta propia
**Dado** un USER autenticado, **cuando** consulta su perfil, **entonces** recibe únicamente sus datos permitidos.
### CA-02 — Actualización válida
**Dado** cambios permitidos y válidos, **cuando** los guarda, **entonces** quedan disponibles al volver a consultar el perfil.
### CA-03 — Protección de datos
**Dado** una solicitud para otro usuario o datos inválidos/duplicados, **cuando** se procesa, **entonces** se deniega o valida sin modificar información no permitida.
## Definition of Done
- [ ] CA-01 a CA-03 validados con pruebas de ownership, validación y cliente aplicable.
- [ ] El contrato enumera los campos permitidos antes de completar la HU.
- [ ] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS (backend) | `ProfileIntegrationTest.meRequiresAuthenticationAndReturnsOwnDataWithoutSecrets` | `GET /api/v1/users/me` exige autenticación y devuelve solo datos propios sin `passwordHash`. |
| CA-02 | PASS (backend) | `ProfileIntegrationTest.patchMeUpdatesOnlyPhoneAndPersists` | `PATCH /api/v1/users/me` persiste `phone` y se refleja en consultas posteriores. |
| CA-03 / DoD | PASS (backend) | `ProfileIntegrationTest.patchMeRejectsPayloadsThatTouchImmutableFields`, `patchMeOnlyAffectsTheAuthenticatedUser` | Cualquier campo distinto de `phone` en el payload es rechazado (`400`) sin modificar la fila; ownership aislado por usuario. Falta cliente (T-03). |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-29 — Backend implementado y verificado (Maven 33/33 verde): `GET/PATCH /api/v1/users/me`. Frontend pendiente para una ronda posterior.
## Notas y decisiones
- Campos editables resueltos por contrato (`S4.md`): únicamente `phone`. Nombres, email, documento y roles permanecen inmutables; el endpoint rechaza explícitamente cualquier otro campo en el payload en vez de ignorarlo silenciosamente.
