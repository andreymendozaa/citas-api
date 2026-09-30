---
id: HU-009
tipo: historia-de-usuario
titulo: "Restablecer contraseña"
estado: Completada
epica: "[[EP-002-identidad-y-perfil-del-usuario]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 2"
dependencias: ["[[HU-008-solicitar-recuperacion-de-contrasena]]"]
relacionadas: ["[[HU-007-renovar-y-cerrar-sesion]]"]
---
# HU-009 — Restablecer contraseña
## Historia de usuario
**COMO** usuario con token de recuperación válido  
**QUIERO** definir una nueva contraseña  
**PARA** recuperar mi acceso sin reutilizar el token.
## Contexto y descripción
Cambiar contraseña consume/invalida el token de recuperación.
## Alcance
- Validar token, cambiar hash, consumir token y actualizar sesión según política aprobada.
## Fuera de alcance
- Cambio de contraseña dentro de perfil o envío SMTP obligatorio.
## Reglas de negocio
- Token temporal de único uso; contraseña nunca se persiste en texto plano.
## Dependencias y relaciones
- Épica: [[EP-002-identidad-y-perfil-del-usuario]]
- Dependencias: [[HU-008-solicitar-recuperacion-de-contrasena]].
- Relacionadas: [[HU-007-renovar-y-cerrar-sesion]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** altera credenciales y debe impedir reutilización y sesiones indebidas.
## Tareas de desarrollo
- [x] **T-01 — Validar/consumir token.** Dificultad: Alto. Aplicar vigencia y uso único de forma atómica.
- [x] **T-02 — Actualizar contraseña segura.** Dificultad: Alto. Aplicar hash adaptativo y política de sesión aprobada.
- [x] **T-03 — Integrar formulario y pruebas.** Dificultad: Medio. Validar nueva contraseña, token inválido y reutilización.
## Criterios de aceptación
### CA-01 — Restablecimiento válido
**Dado** un token vigente no usado y una contraseña válida, **cuando** se confirma el cambio, **entonces** la nueva contraseña permite autenticación posterior.
### CA-02 — Consumo del token
**Dado** un token usado, vencido o inválido, **cuando** se intenta restablecer, **entonces** se rechaza y no cambia la contraseña.
### CA-03 — Seguridad posterior
**Dado** una contraseña cambiada, **cuando** se revisa la persistencia y sesiones afectadas, **entonces** no hay texto plano y se aplica la invalidez de sesión definida en el contrato.
## Definition of Done
- [x] CA-01 a CA-03 tienen pruebas de dominio/seguridad y REST aplicable.
- [x] Token consumido y hash adaptativo verificados; migración si corresponde.
- [x] Cliente y trazabilidad Scrum actualizados.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS (backend) | `PasswordResetIntegrationTest.resetPasswordHashesUpdatesAndRevokesAllSessions` | Login posterior con la nueva contraseña exitoso; la anterior deja de funcionar. |
| CA-02 | PASS (backend) | `PasswordResetIntegrationTest.resetPasswordRejectsExpiredToken`, `resetPasswordRejectsUnknownTokenAndMismatchedConfirmation` | Token usado, expirado, inexistente o con confirmación distinta rechazan el cambio (`400`) sin tocar `password_hash`. |
| CA-03 / DoD | PASS (backend) | `PasswordResetIntegrationTest.resetPasswordHashesUpdatesAndRevokesAllSessions` | Hash BCrypt (`$2...`), token marcado `used_at`, y todos los refresh tokens del usuario revocados (`Ports.Sessions#revokeAllByUserId`); acceso JWT ya emitido expira por su propia vigencia de 15 min. Falta cliente (T-03). |
| Frontend / T-03 | PASS | `ResetPasswordScreen` (`PasswordRecoveryScreens.tsx`), `s4Screens.test.tsx` HU-009; `npm run lint`, `npm test` 34/34, `npm run build` en `citas-web`; validación manual en Chrome | Código precargado desde `?token=` y retirado de la barra de direcciones al iniciar; confirmación distinta bloqueada en cliente; `400` traducido a mensaje genérico; login posterior con la nueva contraseña verificado en vivo. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-29 — Backend implementado y verificado (Maven 33/33 verde): `POST /api/v1/auth/password-reset`, revocación total de sesiones y consumo de un solo uso. Frontend pendiente para una ronda posterior.
- 2026-09-30 — Frontend implementado y verificado en `citas-web` (pasada consolidada de S4 Incrementos 1 y 3): lint, 34/34 pruebas Vitest y build en verde, y flujo validado en Chrome contra el backend en Docker (perfil `local`). HU cerrada como `Completada`.
## Notas y decisiones
- Política de sesión aplicada: reset revoca todos los `refresh_tokens` del usuario (nuevo `Ports.Sessions#revokeAllByUserId`); los access JWT ya emitidos no se invalidan activamente y expiran solos a los 15 minutos, consistente con [[HU-007-renovar-y-cerrar-sesion]].
