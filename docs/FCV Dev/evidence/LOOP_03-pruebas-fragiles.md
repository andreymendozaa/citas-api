# LOOP_03 — Reto independiente: pruebas frágiles frente a la BD de pruebas persistente

**Fecha:** 2026-10-04 · **Repo:** `citas-api` (`develop`) · **Plantilla:** `prompts/goal-loop/LOOP_03_RETO_INDEPENDIENTE.md`
**Problema elegido por el estudiante:** riesgo real registrado en la wiki (`risks-open-questions.md`, 2026-09-30): "la base de pruebas Maven es persistente y algunas suites asumen ser las únicas con citas `REQUESTED` (`$[0]`, un solo pendiente)".

## 1. Disparador

Corridas de la suite que fallan, o que modifican datos ajenos, cuando la BD de pruebas persistente (`citas_fcv_training_andrey_test`) contiene solicitudes pendientes que dejó otra suite o una corrida interrumpida. Ya ocurrió el 2026-09-29 y el 2026-09-30, y hubo que recrear el esquema a mano.

## 2. Meta verificable

La suite Maven completa pasa **2 veces consecutivas** con datos ajenos sembrados (`scripts/loop03-seed-foreign-data.sql`):
- 2 solicitudes especializadas `REQUESTED`, una de ellas con la fecha más temprana;
- 1 reprogramación `PENDING`.

Además, **sin modificar `src/main`** (comportamiento aprobado intacto) y **sin alterar los datos ajenos**.

## 3. Estado observado / persistente

- BD de pruebas MySQL persistente (estado entre iteraciones), con datos ajenos identificados por el prefijo `LOOP03`.
- Logs por iteración en el contenedor: `/tmp/loop03-it0.log`, `/tmp/loop03-latent.log`, `/tmp/loop03-it1-run{1,2}.log`; reportes en `target/surefire-reports`.
- Este archivo como bitácora persistente.

## 4. Alcance del Builder

- **Puede editar:** `src/test/java/**`.
- **No puede editar:** `src/main/**`, migraciones, configuración, ni la lógica de los datos ajenos.
- Cada corrección debe mantener la intención original de la prueba (mismos CA verificados).

## 5. Evidencia del Verifier

El Verifier no implementa. En cada iteración:
1. aplica el sembrado;
2. corre la suite completa;
3. revisa `git status`/`git diff` (solo `src/test` y el script);
4. consulta en SQL el estado de los datos ajenos.

| Iteración | Rol | Acción | Resultado |
|---|---|---|---|
| 0 | Verifier | Sembrado (1 REQUESTED +30 d, 1 PENDING) y suite completa **sin cambios** | **Red:** 72 pruebas, 1 fallo: `SchedulingServiceIntegrationTest.retainsConsecutiveSlots…:37` (`singleElement()` sobre `pending()` global) |
| 0b | Verifier | Revisión de código: `AuthIntegrationTest` toma `$[0]` de `/admin/appointments/pending-specialized` (lista global) | Fragilidad **latente**: pasó solo porque la cita ajena se ordenaba después |
| 1 | Builder | `SchedulingServiceIntegrationTest`: `filteredOn(id == appointment.id())`. `AuthIntegrationTest`: busca su propia solicitud por `specialtyName` único antes de decidir | 2 archivos de `src/test` modificados; `src/main` intacto |
| 1 | Verifier | Endurece el sembrado: 2.ª REQUESTED ajena con la fecha más temprana (+1 d 06:00). Corre la versión **original** de `AuthIntegrationTest` (vía `git stash`) | **Latente confirmado:** la prueba original **rechazó la solicitud ajena** y luego falló (`:232 No value at JSON path "$[0].rejectionReason"`). El sembrado se ajustó para restaurar el estado ajeno en cada corrida |
| 1 | Verifier | Re-sembrado y suite completa, corrida 1 | **Green:** 72/72, 0 fallos |
| 1 | Verifier | Re-sembrado y suite completa, corrida 2 | **Green:** 72/72, 0 fallos |
| 1 | Verifier | Estado de los datos ajenos tras las 2 corridas | Las 2 `REQUESTED` siguen `REQUESTED`; la reprogramación sigue `PENDING`. **Ninguna prueba consumió datos ajenos** |

Registro estructurado:

```json
[
  {"loop": "LOOP_03", "iteration": 0, "builder": "none", "backendTests": "71/72", "verifier": "FAIL", "result": "RED"},
  {"loop": "LOOP_03", "iteration": 1, "builder": "completed (src/test only)", "latentCheck": "original AuthIntegrationTest FAIL + side effect", "backendTests": "72/72 x2", "foreignDataIntact": true, "verifier": "PASS", "result": "COMPLETED"}
]
```

## 6. Presupuesto de iteraciones

Máximo **5** iteraciones Builder→Verifier. Usadas: **1** (más la línea base 0).

## 7. Condición de parada

Dos corridas consecutivas de la suite completa en verde, con el sembrado endurecido, `git diff` limitado a `src/test` + el script, y datos ajenos sin modificar. **Alcanzada en la iteración 1.**

## 8. Condición de escalamiento humano

Escalar al estudiante si ocurre cualquiera de estas condiciones:
- la corrección exige tocar `src/main` (sería un cambio de comportamiento, no de prueba);
- una prueba solo puede pasar debilitando su aserción (perdiendo el CA que verifica);
- se agota el presupuesto de 5 iteraciones;
- el sembrado necesita datos reales o la BD de desarrollo.

**No fue necesario escalar.**

## 9. Log de ejecución

Ver la tabla del §5 y el registro JSON. Comandos usados:
- sembrado: `mysql … citas_fcv_training_andrey_test < scripts/loop03-seed-foreign-data.sql`;
- suite: `docker compose exec citas-api-dev mvn -q -B test`;
- latencia: `git stash push -- …/AuthIntegrationTest.java && mvn test -Dtest=AuthIntegrationTest; git stash pop`.

## 10. Por qué no bastaba un único prompt

- La fragilidad depende del **estado acumulado** de una BD persistente y del **orden** de los datos. Un único prompt que "arreglara las pruebas" no podía observar ese estado. La primera corrida solo mostró uno de los dos problemas.
- El segundo problema, el `$[0]` de `AuthIntegrationTest`, era **latente**: pasaba en verde. Solo apareció cuando el Verifier endureció el escenario según lo observado. Ahí reveló además un **efecto colateral** grave: la prueba rechazaba solicitudes ajenas.
- La meta exige estabilidad (2 corridas consecutivas) y la preservación de datos ajenos. Eso requiere verificación repetida e independiente de quien implementa, con una condición de parada objetiva.
