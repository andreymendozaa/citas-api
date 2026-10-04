---
tipo: indice-scrum
estado: Vigente
actualizado: 2026-09-30
---

# Mapa Scrum / Spec-Driven Development — Sistema de citas

## Propósito y límites

Este mapa convierte el PRD v1 y las restricciones autorizadas en trabajo secuencial y verificable. El usuario aprobó el 2026-09-17 el corte backend de HU-001/002/004/005/006/007 y el seed parcial de roles de HU-003; las demás HU requieren revisión explícita. El contrato de autenticación está en la LLM Wiki; el framework web sigue sin aprobación. *(Texto original del 2026-09-17; el estado vigente está en "Estado auditado · 2026-09-30".)*

## Arquitectura y supuestos constatados

- Backend requerido: Java 21, Spring Boot 3.5.x, Maven, arquitectura hexagonal, JPA, Flyway, MySQL 8.4 y REST/JSON.
- Cliente implementado: TypeScript con React/Vite, consume REST directo; no hay Express ni BFF.
- Los catálogos fijos se cargan por seed; los datos del laboratorio son sintéticos.
- El repositorio contiene la aplicación backend y el contrato inicial de identidad. HU-005/006/007 están `Completada` para el corte backend con evidencia de `mvn test` (8 pruebas, 0 fallos); HU-001/002/003 y las épicas EP-001/002 permanecen parciales.

## Registro S4 — cierre técnico sin cierre de HU · 2026-09-29

El corte S3 conserva sus rutas y la BD Flyway V1/V2 sin modificación. Las pruebas verificadas son 19 Maven y 16 Vitest, más `npm run lint` y `npm run build`. Se comprobó concurrencia, reglas de slots, aprobación/rechazo e historial, autorizaciones, profesional/sede/especialidad inválidos, contratos de formularios para USER, ADMIN y PROFESSIONAL, y las rutas S3 de reserva general/especializada y decisión ADMIN mediante MockMvc. El caso rojo de concurrencia quedó verde tras la protección transaccional de los slots. Los endpoints de oferta y agenda se consumen desde React sin datos simulados; `GET /admin/professionals` no devuelve datos de acceso. El Verifier reutilizable se aplica justo después de cada Prompt antes de avanzar al siguiente. Este registro no marca HU-003, HU-004, HU-011 ni HU-014 a HU-024 como completadas: sus CA/DoD permanecen sujetos a la auditoría integral.

## Estado auditado · 2026-09-30

El usuario solicitó una auditoría integral de CA/DoD contra el código y las pruebas reales, para poner al día la trazabilidad de S3/S4. Criterio aplicado:
- un CA es **PASS** solo con prueba automatizada o propiedad estructural (por ejemplo, ausencia de ruta de escritura);
- lo verificado únicamente por revisión de código o de forma manual queda **PARCIAL/PENDIENTE**;
- una HU pasa a `Completada` solo si ningún CA queda pendiente.

La evidencia de cada CA está en la tabla de cada HU.

| Estado | Cantidad | HU |
|---|---|---|
| `Completada` | 34 | HU-001 a HU-032, HU-034 y HU-035 (HU-004, HU-034 y HU-035 cerradas el 2026-10-04) |
| `En progreso` | 1 | HU-033 |
| `En desarrollo` | 1 | HU-036 (WF-003 validado; falta una ejecución con citas del día, CA-01 parcial) |

Actualización del mismo día: HU-026, HU-027 y HU-028 pasan a `Completada` con `AppointmentLifecycleIntegrationTest` (8 casos) y 4 pruebas de cliente. Las pruebas destaparon un bug: se respondía un error 500 ante una cita ajena o inexistente y ante una solicitud ya resuelta; se corrigió siguiendo Red → Green.

Segunda actualización del mismo día: HU-023 y HU-024 pasan a `Completada` con `SpecializedDecisionIntegrationTest`. La decisión ADMIN sobre una cita inexistente, general o ya resuelta también respondía un error 500; se corrigió siguiendo Red → Green. El backend ya no tiene consultas de fila única sin manejar.

Tercera actualización del mismo día: `OfferAndAgendaRulesIntegrationTest` (7 casos) cubre los CA pendientes de HU-014, 015, 016, 018, 019 y 020.
- Seis casos pasaron directamente.
- El de HU-019 descubrió que un bloque de un día ya pasado podía moverse al futuro y eliminarse; se corrigió siguiendo Red → Green con `409`.
- Resultado: HU-014, 015, 018, 019 y 020 pasan a `Completada`. HU-016 sigue `En desarrollo` solo por la UI de la especialidad primaria.
- Ninguna HU queda pendiente únicamente por falta de pruebas.

Cuarta actualización del mismo día, sobre las funcionales pequeñas:
- HU-003: endpoint `GET /catalogs/reschedule-statuses` y `FixedCatalogsIntegrationTest`.
- HU-016: `primarySpecialtyId` en `GET /admin/professionals`, y selector de primaria y editor de asignaciones en la UI ADMIN.
- HU-021: filtro "Tipo de cita" en la reserva.

Las tres pasan a `Completada`. Solo queda HU-004, que depende de OpenAPI (S5).

Quinta actualización del mismo día: HU-011 pasa de `Aprobada` a `Completada`. Por decisión del usuario se amplió su alcance a consultar y cambiar la afiliación propia, y se sembraron los 5 regímenes del modelo de referencia (V5). Con esto, **todos los RF funcionales del PRD (RF-01 a RF-19) quedan implementados y probados**. Pendientes: HU-004/RF-20 (OpenAPI), HU-033 (en progreso) y HU-034 a 036 (n8n, S5/S6).

Causas registradas en la auditoría inicial (todas resueltas salvo HU-004):
- **Falta de pruebas dedicadas.** Las guardas existen en el código, pero no hay una prueba que las verifique:
  - HU-014: rechazo de duración ≠ 30/60.
  - HU-015: unicidad sin registro parcial.
  - HU-016: especialidad primaria y especialidad no asociada.
  - HU-018: dos franjas el mismo día.
  - HU-019: bloque ajeno, pasado o comprometido.
  - HU-020: filtros de calendario.
  - HU-023: historial inicial `REQUESTED`/`USER`.
  - HU-024: rechazo sin motivo y estado inválido.
  - HU-026 a HU-028: cancelación y reprogramación, cuyas únicas pruebas son indirectas (`AppointmentEventsIntegrationTest`, `InboxIntegrationTest`).
- **Huecos funcionales:**
  - HU-003: catálogo de estados de reprogramación no expuesto.
  - HU-021: falta el filtro por tipo general/especializada (RF-10).
  - HU-016: sin selector de especialidad primaria en la UI.
- **Documentación de contrato:** HU-004 queda pendiente de la especificación OpenAPI (S5).

HU-025 a HU-028 pasan de `Pendiente de aprobación` a su estado auditado porque ya estaban implementadas y el usuario solicitó su auditoría el 2026-09-30.

## Épicas

- [[EP-001-fundacion-y-contrato-del-producto]]
- [[EP-002-identidad-y-perfil-del-usuario]]
- [[EP-003-administracion-de-catalogos-y-profesionales]]
- [[EP-004-disponibilidad-del-profesional]]
- [[EP-005-busqueda-y-reserva-de-citas]]
- [[EP-006-ciclo-de-vida-de-citas-y-reprogramaciones]]
- [[EP-007-operacion-profesional-y-administrativa]]
- [[EP-008-cliente-web-y-automatizaciones-posteriores]]

## Incrementos sugeridos

Los sprints son incrementos funcionales secuenciales, no estimaciones de duración ni capacidad.

1. **Incremento 1 — Fundaciones trazables:** [[HU-001-inicializar-fundacion-tecnica]], [[HU-002-modelar-persistencia-3fn]], [[HU-003-publicar-catalogos-fijos]], [[HU-004-definir-contrato-rest-inicial]]. Resultado: base verificable para construir y consumir el producto.
2. **Incremento 2 — Acceso y datos del usuario:** [[HU-005-registrar-usuario]], [[HU-006-iniciar-sesion]], [[HU-007-renovar-y-cerrar-sesion]], [[HU-008-solicitar-recuperacion-de-contrasena]], [[HU-009-restablecer-contrasena]], [[HU-010-gestionar-perfil]], [[HU-011-gestionar-afiliacion]]. Resultado: USER autenticado y con perfil/afiliación coherentes.
3. **Incremento 3 — Oferta clínica administrable:** [[HU-012-gestionar-eps]], [[HU-013-gestionar-planes-eps]], [[HU-014-gestionar-especialidades-y-duracion]], [[HU-015-crear-profesional]], [[HU-016-asignar-especialidades-al-profesional]], [[HU-017-asignar-sedes-y-estado-del-profesional]]. Resultado: oferta sintética y habilitable para agenda.
4. **Incremento 4 — Disponibilidad y reserva:** [[HU-018-crear-bloques-de-disponibilidad]], [[HU-019-modificar-bloques-futuros]], [[HU-020-consultar-calendario-de-disponibilidad]], [[HU-021-buscar-disponibilidad]], [[HU-022-reservar-cita-general]], [[HU-023-solicitar-cita-especializada]], [[HU-024-resolver-solicitud-especializada]]. Resultado: citas generales aprobadas y especializadas bajo decisión ADMIN.
5. **Incremento 5 — Continuidad de la cita:** [[HU-025-consultar-mis-citas]], [[HU-026-cancelar-cita]], [[HU-027-solicitar-reprogramacion]], [[HU-028-resolver-reprogramacion]]. Resultado: USER administra sus citas sin vulnerar reservas.
6. **Incremento 6 — Operación controlada:** [[HU-029-consultar-agenda-profesional]], [[HU-030-cerrar-atencion]], [[HU-031-consultar-bandeja-administrativa]], [[HU-032-consultar-auditoria-de-estados]]. Resultado: profesionales y ADMIN operan con visibilidad y auditoría.
7. **Incremento 7 — Cliente y automatizaciones posteriores:** [[HU-033-integrar-cliente-web-con-api]], [[HU-034-automatizar-recordatorios]], [[HU-035-notificar-cambios-de-estado]], [[HU-036-generar-resumen-operativo-diario]]. Resultado: flujos web integrados y automatizaciones S5/S6 sin cambiar el núcleo.

## Decisiones e incógnitas que requieren revisión

- ~~Seleccionar React o Angular~~ — resuelto: React 19 + TypeScript + Vite. Sigue pendiente la evidencia de aprobación visual Stitch/AI Studio.
- ~~Diseñar y aprobar el contrato REST~~ — resuelto por incremento en la LLM Wiki (`contracts.md`). Falta publicar OpenAPI ([[HU-004-definir-contrato-rest-inicial]]).
- ~~Definir los valores de catálogos fijos~~ — sembrados en V1/V2/V3. Falta exponer los estados de reprogramación ([[HU-003-publicar-catalogos-fijos]]).
- ~~Canal seguro del token de recuperación~~ — resuelto: buzón local ADMIN bajo perfil `local` ([[HU-008-solicitar-recuperacion-de-contrasena]]).
- Las automatizaciones de [[EP-008-cliente-web-y-automatizaciones-posteriores]] son posteriores al núcleo y dependen de la instancia/credenciales del trainer; sus JSON vivirán en `automations/n8n/` sin credenciales.

## Regla de selección para S2, S3 y S4

Solo se puede seleccionar una HU cuyo estado sea `Aprobada`, cuyas dependencias estén `Completada` o se incluyan explícitamente en el mismo incremento secuencial, y cuyo contrato/decisión pendiente no altere su alcance. Este índice no concede aprobación.
