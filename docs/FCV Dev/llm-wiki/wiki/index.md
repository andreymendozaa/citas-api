# Índice de la LLM Wiki

Última actualización: 2026-09-30.

Implementado y verificado:
- Identidad (HU-005/006/007).
- Agendamiento S3 completo (HU-014 a HU-028).
- S4 completo en backend (Maven 40/40) y frontend (Vitest 34/34 y validación en Chrome), cubriendo:
  - Incremento 1: recuperación y restablecimiento HU-008/009, perfil HU-010, EPS y planes HU-012/013.
  - Incremento 3: agenda profesional HU-029, cierre HU-030, bandeja unificada HU-031, auditoría HU-032, preparación n8n.

S4 queda cerrado. Pantallas de S4 sin aprobación visual Stitch: ver [Riesgos](risks-open-questions.md).

Auditoría del 2026-09-30:
- Estado global: 31 HU `Completada`. Todos los RF funcionales del PRD (RF-01 a RF-19) están implementados y probados.
- Pendientes:
  - HU-004: OpenAPI, previsto en S5.
  - HU-033: integración web, en progreso.
  - HU-034 a 036: automatizaciones n8n.
- Las nuevas pruebas corrigieron 4 errores 500 y la edición y eliminación de bloques pasados.
- Brechas funcionales y entregables S5/S6 pendientes: ver [Trazabilidad](traceability.md).

Actualización del 2026-10-04 (S5, backend):
- HU-004/RF-20 `Completada`: OpenAPI publicado en `/v3/api-docs` y copia en `docs/openapi/openapi-v1.json`. `GET /actuator/health` público.
- Webhook real WF-002 listo en la API, incluidos los eventos de reprogramación: ver [Contratos](contracts.md).
- Estado global: 32 HU `Completada`.

Siguiente hito: workflows n8n WF-001/002/003 (HU-034 a 036) en la instancia compartida, con el prefijo `Andrey` (ver [Decisiones](decisions.md)).

## Lectura recomendada

1. [Resumen](overview.md)
2. [Reglas de dominio](domain-rules.md)
3. [Arquitectura](architecture.md)
4. [Integridad de datos](data-integrity.md)
5. [Contratos](contracts.md)
6. [Decisiones](decisions.md)
7. [Riesgos y preguntas](risks-open-questions.md)
8. [Preferencias](preferences.md)
9. [Trazabilidad](traceability.md)
10. [Subagentes y delegación](subagents.md)

## Gobierno

- [Log](log.md)
- [Convenciones](../schema/page-conventions.md)
- [Gobierno](../schema/governance.md)
- [Manifest RAW](../raw/manifest.md)
