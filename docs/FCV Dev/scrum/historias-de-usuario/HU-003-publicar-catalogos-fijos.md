---
id: HU-003
tipo: historia-de-usuario
titulo: "Publicar catálogos fijos"
estado: Completada
epica: "[[EP-001-fundacion-y-contrato-del-producto]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 1"
dependencias: ["[[HU-001-inicializar-fundacion-tecnica]]", "[[HU-002-modelar-persistencia-3fn]]"]
relacionadas: ["[[HU-012-gestionar-eps]]"]
---

# HU-003 — Publicar catálogos fijos
## Historia de usuario
**COMO** consumidor autorizado de la aplicación  
**QUIERO** consultar los catálogos fijos del laboratorio  
**PARA** usar valores consistentes al registrar, configurar y agendar.
## Contexto y descripción
Roles, estados de cita, estados de reprogramación, regímenes y sedes son de solo lectura y se precargan.
## Alcance
- Seed y consulta REST documentada de catálogos fijos, incluidas las dos sedes descritas.
## Fuera de alcance
- CRUD ADMIN de EPS, planes o especialidades.
## Reglas de negocio
- Son solo lectura y se cargan por seed; datos sintéticos salvo referencias públicas permitidas de sedes.
## Dependencias y relaciones
- Épica: [[EP-001-fundacion-y-contrato-del-producto]]
- Dependencias: [[HU-001-inicializar-fundacion-tecnica]], [[HU-002-modelar-persistencia-3fn]].
- Relacionadas: [[HU-012-gestionar-eps]].
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** cruza seed, contrato, seguridad y consistencia de datos.
## Tareas de desarrollo
- [x] **T-01 — Definir valores autorizados.** Dificultad: Medio. Reflejar los tipos exigidos y las dos sedes sin datos personales.
- [x] **T-02 — Cargar catálogos por seed.** Dificultad: Medio. Hacerlo repetible y coherente con Flyway.
- [x] **T-03 — Exponer lectura por REST.** Dificultad: Medio. Aplicar autorización aprobada y validaciones.
- [x] **T-04 — Probar inmutabilidad funcional.** Dificultad: Bajo. Verificar que no exista operación de modificación por la API.
## Criterios de aceptación
### CA-01 — Catálogos disponibles
**Dado** una instalación inicial, **cuando** se consulta cada catálogo fijo, **entonces** roles, estados, regímenes y sedes exigidos están disponibles.
### CA-02 — Sedes correctas
**Dado** el catálogo de sedes, **cuando** se consulta, **entonces** contiene HIC e ICV con las referencias públicas del PRD.
### CA-03 — Solo lectura
**Dado** un consumidor autorizado, **cuando** intenta modificar un catálogo fijo mediante el contrato, **entonces** la operación no está permitida.
## Definition of Done
- [x] CA-01 a CA-03 tienen evidencia de seed, contrato y pruebas relevantes.
- [x] La migración/seed usa datos autorizados y no contiene secretos.
- [x] La trazabilidad Scrum está actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | PASS | `FixedCatalogsIntegrationTest.everyFixedCatalogIsSeededAndReadable` | Roles (USER/PROFESSIONAL/ADMIN); estados de cita (6, con `terminal`); estados de reprogramación (PENDING/APPROVED/REJECTED/CANCELLED); regímenes no vacíos. Catálogo desconocido → `400`. |
| CA-02 | PASS | `FixedCatalogsIntegrationTest.locationsAreTheTwoPrdSites` | Exactamente HIC (Hospital Internacional de Colombia) e ICV (Instituto Cardiovascular). |
| CA-03 / DoD | PASS | `FixedCatalogsIntegrationTest.fixedCatalogsAreReadOnlyThroughTheContract` | `POST`/`DELETE` sobre `/catalogs/locations`, incluso como ADMIN → `4xx`, sin cambios en la tabla. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-30 — Auditoría integral de CA/DoD contra código y pruebas (solicitada por el usuario): estado se mantiene `En desarrollo`. Criterio de auditoría: PASS exige prueba automatizada o propiedad estructural; lo verificado solo por código o manualmente queda PARCIAL/PENDIENTE; la HU se completa solo sin CA pendientes.
- 2026-09-30 — Se expone `GET /api/v1/catalogs/reschedule-statuses` (cambio aditivo; antes estaba sembrado en V3 sin endpoint). `FixedCatalogsIntegrationTest` cubre los tres CA. Estado `En desarrollo` → `Completada`. Observación: el catálogo de regímenes solo contiene `PARTICULAR`; el PRD no fija sus valores.
## Notas y decisiones
- Los valores de estados deberán alinearse con el catálogo fijo aprobado.
- 2026-09-17: se aprobó únicamente el seed de roles `USER`, `PROFESSIONAL`, `ADMIN` como dependencia de identidad. La publicación REST de roles y los demás catálogos quedan pendientes; HU-003 conserva su estado.
