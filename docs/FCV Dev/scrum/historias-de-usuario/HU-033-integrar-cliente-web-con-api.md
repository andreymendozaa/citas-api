---
id: HU-033
tipo: historia-de-usuario
titulo: "Integrar cliente web con API"
estado: Completada
epica: "[[EP-008-cliente-web-y-automatizaciones-posteriores]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 7"
dependencias: ["[[HU-004-definir-contrato-rest-inicial]]", "[[HU-032-consultar-auditoria-de-estados]]"]
relacionadas: []
---
# HU-033 — Integrar cliente web con API
## Historia de usuario
**COMO** usuario de cualquiera de los roles  
**QUIERO** acceder por web a las pantallas obligatorias conectadas directamente a la API  
**PARA** completar los flujos aprobados del producto.
## Contexto y descripción
La estética se deriva del prototipo React/Vite importado desde AI Studio. Esta HU consolida integración, no duplica reglas backend.
## Alcance
- Rutas/pantallas obligatorias de registro, sesión, perfil, disponibilidad/citas, dashboards profesional y ADMIN, y CRUD ADMIN mediante contrato aprobado.
## Fuera de alcance
- Elegir framework sin aprobación, Express/BFF o inventar comportamiento no disponible en API.
## Reglas de negocio
- REST directo a `citas-api`; URL por environment; CORS explícito; UI respeta roles/ownership del backend.
## Dependencias y relaciones
- Épica: [[EP-008-cliente-web-y-automatizaciones-posteriores]]
- Dependencias: [[HU-004-definir-contrato-rest-inicial]], [[HU-032-consultar-auditoria-de-estados]].
- Relacionadas: Ninguna.
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** integra múltiples flujos/roles y depende de decisión visual/framework.
## Tareas de desarrollo
- [x] **T-01 — Acordar framework/diseño.** Dificultad: Alto. Prototipo React/Vite importado; se conserva su composición visual para el corte de autenticación.
- [x] **T-02 — Implementar navegación/estado por rol.** Dificultad: Alto. Pestañas por rol en el dashboard (USER: *Mis citas* / *Mi perfil*; PROFESSIONAL: *Agenda* / *Disponibilidad*; ADMIN: *Bandeja* / *Oferta* / *EPS y planes*), más recuperación y restablecimiento desde el login.
- [x] **T-03 — Conectar cliente REST.** Dificultad: Alto. Registro, login, refresh y logout consumen el contrato versionado directo, por environment y sin BFF.
- [x] **T-04 — Verificar cross-repo.** Dificultad: Alto. Typecheck, pruebas, build y flujo visual USER comprobados contra API/MySQL.
## Criterios de aceptación
### CA-01 — Pantallas obligatorias
**Dado** el diseño y las HU aprobadas, **cuando** cada rol navega, **entonces** puede llegar a las pantallas obligatorias pertinentes del PRD.
### CA-02 — Consumo directo
**Dado** una acción web de una HU aprobada, **cuando** se ejecuta, **entonces** el cliente llama directamente a `citas-api` por REST/JSON usando URL configurable.
### CA-03 — Manejo de seguridad
**Dado** una respuesta de autorización/validación, **cuando** el cliente la recibe, **entonces** muestra un resultado acorde al contrato sin exponer tokens o secretos.
## Definition of Done
- [x] CA-01 a CA-03 validados para los flujos aprobados en ambos repositorios.
- [x] Framework y diseño visual aprobados; build/typecheck y pruebas aplicables con evidencia. *Excepción aceptada por el usuario (2026-10-04): no existe aprobación Stitch/AI Studio de las pantallas de S3/S4; extienden el estilo del prototipo importado. Queda como deuda visual documentada.*
- [x] No existe Express/BFF; contrato/cors/environment y Scrum están actualizados.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS | `s4Screens.test.tsx`, `DashboardScreen.test.tsx`, `authScreens.test.tsx`; validación manual en Chrome (2026-09-30) | Las pantallas obligatorias del PRD son alcanzables por rol: autenticación y recuperación; USER (reserva, Mis citas con cancelar/reprogramar, perfil y afiliación); PROFESSIONAL (disponibilidad, agenda y cierre); ADMIN (bandeja unificada, oferta y asignaciones, EPS/planes, historial). |
| CA-02 | PASS | `src/api/schedulingApi.ts`, `src/auth/authApi.ts`, `schedulingApi.test.ts`, `authApi.test.ts`; `VITE_API_URL` | REST/JSON directo a `citas-api` (`/api/v1`), sin Express/BFF; contrato publicado en OpenAPI (HU-004). |
| CA-03 | PASS | `authApi.test.ts`, `authScreens.test.tsx`, `DashboardScreen.test.tsx` | Access token solo en memoria, refresh en cookie HttpOnly; 400/401/403/404/409 mapeados a mensajes sin exponer tokens. |
| DoD | PASS con excepción | 2026-10-04: `npm run lint` OK, `npx vitest run` 44/44, `npm run build` OK; `mvn test` 72/72 | Excepción: sin aprobación visual Stitch/AI Studio (deuda aceptada por el usuario). |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-22 — Corte React de autenticación implementado y verificado cross-repo; la HU queda en progreso hasta cubrir las pantallas de sus dependencias posteriores.
- 2026-10-04 — Auditoría de cierre: todas las pantallas por rol existen desde S4 (HU-008 a HU-032) y la web pasa lint, Vitest 44/44 y build. Por decisión del usuario, la falta de aprobación Stitch/AI Studio se acepta como deuda visual documentada. Estado: `Completada`.
## Notas y decisiones
- React + TypeScript + Vite se adopta para este corte a partir del prototipo entregado `portal-de-citas.zip`.
- 2026-09-17: aquí quedan las tareas visuales diferidas de HU-005/006/007: formulario de registro, feedback de login, renovación desde navegador y limpieza de estado autenticado al salir. Integrar `credentials`, `X-Requested-With` y el contrato de cookie cuando se aborde la UI.
