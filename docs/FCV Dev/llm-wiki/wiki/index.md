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
- Estado global: 20 HU `Completada` y 11 `En desarrollo`, sobre todo por falta de pruebas dedicadas en S3. Cancelación y reprogramación quedaron cubiertas el mismo día.
- Brechas funcionales y entregables S5/S6 pendientes: ver [Trazabilidad](traceability.md).

Siguiente hito: cerrar esas brechas y luego S5 (Swagger, `current-state.md`, webhook n8n real).

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
