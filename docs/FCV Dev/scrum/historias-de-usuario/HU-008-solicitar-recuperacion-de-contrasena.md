---
id: HU-008
tipo: historia-de-usuario
titulo: "Solicitar recuperación de contraseña"
estado: Completada
epica: "[[EP-002-identidad-y-perfil-del-usuario]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 2"
dependencias: ["[[HU-005-registrar-usuario]]", "[[HU-004-definir-contrato-rest-inicial]]"]
relacionadas: ["[[HU-009-restablecer-contrasena]]"]
---

# HU-008 — Solicitar recuperación de contraseña
## Historia de usuario
**COMO** usuario que no recuerda su contraseña  
**QUIERO** solicitar un token temporal de recuperación por email  
**PARA** restablecer mi acceso de forma segura.
## Contexto y descripción
SMTP real es opcional; en desarrollo el token solo puede exponerse por vía segura controlada.
## Alcance
- Solicitud por email, emisión temporal/de único uso y canal controlado de desarrollo aprobado.
## Fuera de alcance
- SMTP obligatorio, SMS/WhatsApp o revelar tokens en logs públicos.
## Reglas de negocio
- Token temporal y de único uso; secretos/tokens no deben registrarse.
## Dependencias y relaciones
- Épica: [[EP-002-identidad-y-perfil-del-usuario]]
- Dependencias: [[HU-005-registrar-usuario]], [[HU-004-definir-contrato-rest-inicial]].
- Relacionadas: [[HU-009-restablecer-contrasena]].
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** exige token seguro y una decisión de canal de desarrollo.
## Tareas de desarrollo
- [x] **T-01 — Definir token temporal.** Dificultad: Alto. Establecer expiración, consumo y almacenamiento seguro.
- [x] **T-02 — Diseñar solicitud y respuesta segura.** Dificultad: Medio. No filtrar datos o token sin mecanismo aprobado.
- [x] **T-03 — Integrar pantalla/feedback.** Dificultad: Bajo. Informar resultado sin revelar información sensible.
- [x] **T-04 — Probar ciclo de emisión.** Dificultad: Medio. Cubrir email existente/no existente sin enumeración indebida.
## Criterios de aceptación
### CA-01 — Token temporal
**Dado** una solicitud válida, **cuando** se procesa, **entonces** se genera un token temporal asociado al usuario y apto para un solo uso.
### CA-02 — Respuesta segura
**Dado** cualquier email recibido, **cuando** se solicita recuperación, **entonces** la respuesta no expone credenciales, tokens ni información innecesaria de existencia de cuenta.
### CA-03 — Desarrollo controlado
**Dado** que no hay SMTP real, **cuando** se ejecuta en desarrollo, **entonces** el token solo se entrega mediante mecanismo seguro previamente aprobado.
## Definition of Done
- [x] CA-01 a CA-03 tienen pruebas/evidencia y el canal de desarrollo está documentado.
- [x] Tokens no aparecen en logs ni repositorio; esquema/migración aplicable está verificado.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS (backend) | `PasswordResetIntegrationTest.resetPasswordHashesUpdatesAndRevokesAllSessions` | Token aleatorio de 32 bytes, persistido solo como hash SHA-256, un solo uso. |
| CA-02 | PASS (backend) | `PasswordResetIntegrationTest.recoveryAlwaysAcceptsAndNeverLeaksAccountExistence` | `POST /api/v1/auth/password-recovery` responde `202` igual para email existente/inexistente. |
| CA-03 / DoD | PASS (backend) | `LocalMailboxIntegrationTest.adminReadsTokenUserIsForbiddenAndTokenResetsThePassword` | Buzón local activo solo con perfil `local`, exclusivo ADMIN; `PasswordResetIntegrationTest.localMailboxRouteDoesNotExistOutsideLocalProfile` confirma `404` fuera de ese perfil. Falta pantalla/feedback de frontend (T-03). |
| Frontend / T-03 | PASS | `ForgotPasswordScreen` (`PasswordRecoveryScreens.tsx`), `s4Screens.test.tsx` HU-008, `authApi.test.ts`; `npm run lint`, `npm test` 34/34, `npm run build` en `citas-web`; validación manual en Chrome | Enlace desde login; mensaje neutral idéntico exista o no la cuenta; el cliente acepta `202` sin cuerpo y nunca muestra ni registra el token. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-29 — Backend implementado y verificado (Maven 33/33 verde): `POST /api/v1/auth/password-recovery`, migración `V4__password_reset_tokens.sql`, buzón local `@Profile("local")`. Frontend pendiente para una ronda posterior.
- 2026-09-30 — Frontend implementado y verificado en `citas-web` (pasada consolidada de S4 Incrementos 1 y 3): lint, 34/34 pruebas Vitest y build en verde, y flujo validado en Chrome contra el backend en Docker (perfil `local`). HU cerrada como `Completada`.
## Notas y decisiones
- Mecanismo seguro de entrega en desarrollo resuelto: buzón local en memoria (`LocalPasswordResetMailbox`), nunca persistido ni logueado en texto claro, expuesto solo por `GET /api/v1/admin/local-mailbox/password-resets` bajo perfil `local` y rol ADMIN.
