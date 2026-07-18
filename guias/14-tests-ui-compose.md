# Guía 14 — Tests de UI de Compose: la cúspide de la pirámide

Objetivo: probar lo único que quedaba sin guardián — **lo que el usuario
ve**. Ocho tests componen `SaludoContent` con estados fabricados y afirman
sobre el árbol de semántica: el spinner de `Cargando`, el botón que se
deshabilita, la ★ de las favoritas y los callbacks con la frase exacta.

Con esta guía, la pirámide de la serie (10-14) queda completa:

```
        UI (Compose)          ← esta guía (8 tests, emulador)
      datos instrumentados    ← guía 13 (4 tests, emulador)
    red (MockWebServer)       ← guía 12 (6 tests, JVM)
  ViewModel y Flow            ← guía 11 (6 tests, JVM)
dominio y repositorio         ← guías 10 (13 tests, JVM)
```

---

## 1. Cero dependencias nuevas (estaban esperando desde la guía 02)

La plantilla trajo `androidx-ui-test-junit4` (androidTest) y
`ui-test-manifest` (debug) que nunca habíamos usado. Los tests de Compose
son **instrumentados**: componen UI real en el dispositivo. Existe la
alternativa de correrlos en la JVM con Robolectric, pero el camino oficial
de referencia es el dispositivo.

## 2. Se testea la pantalla SIN estado (el último dividendo de la guía 05)

`SaludoScreen` se partió en dos en la guía 05: la *stateful* (conecta con
el ViewModel) y `SaludoContent` (*stateless*: recibe `SaludoUiState` y
callbacks). Aquella división prometía testabilidad; hoy se cobra:

```kotlin
@get:Rule
val compose = createComposeRule()

compose.setContent {
    HolaAndroidTheme {
        SaludoContent(
            uiState = estadoCon(frase = FraseUiState.Cargando),  // estado FABRICADO
            alPedirFrase = { pulsado = true },                   // callbacks espía
            // ...
        )
    }
}
```

Ni ViewModel, ni Hilt, ni Activity de la app: `createComposeRule` levanta
una Activity vacía (para eso estaba `ui-test-manifest`) y el test fabrica
el estado que quiere ver pintado — incluidos los difíciles de provocar a
mano, como `Cargando` congelado.

Cambio de visibilidad que lo permite: `SaludoContent` pasa de `private` a
**`internal`** — visible para los tests del mismo módulo, invisible para
cualquier otro. Es el ajuste estándar.

## 3. El árbol de semántica: lo que "ve" el test es lo que ve la accesibilidad

Los tests de Compose no miran píxeles: consultan el **árbol de semántica**,
la descripción de la UI que Compose expone para accesibilidad (TalkBack) y
tests. La consecuencia es profunda: si tu test no encuentra un nodo, un
lector de pantalla probablemente tampoco. Testear así audita accesibilidad
gratis.

La gramática, en tres familias:

| Familia | Ejemplos | Papel |
|---|---|---|
| Finders | `onNodeWithText`, `onNodeWithTag` | localizar nodos |
| Assertions | `assertIsDisplayed`, `assertIsNotEnabled` | afirmar sobre ellos |
| Actions | `performClick` | actuar sobre ellos |

Y para nodos **sin texto** (el spinner), el ancla explícita:

```kotlin
CircularProgressIndicator(modifier = modifier.size(28.dp).testTag("cargando"))
// en el test:
compose.onNodeWithTag("cargando").assertIsDisplayed()
```

`testTag` es el único cambio "para tests" tolerado en producción: no pinta
nada, no cambia semántica de usuario, y evita trucos frágiles.

## 4. Los ocho tests: cada estado del sealed, pintado y verificado

El `when` exhaustivo de `FraseSeccion` (guía 06) obligaba a *decidir* cómo
pintar cada caso; estos tests fijan lo *decidido*:

- **Inicial** → la invitación visible y el botón habilitado.
- **Cargando** → el spinner visible **y el botón deshabilitado** — la
  protección contra el doble-tap, ahora con guardián.
- **Exito** → `"“texto”"` y `"— autora"` visibles.
- **Error** → el mensaje para humanos visible.

Y las interacciones, con **callbacks espía** (una variable que el callback
escribe y el test lee):

```kotlin
var tocada: Frase? = null
componer(estadoCon(guardadas = listOf(otra, frase)), alAlternarFavorita = { tocada = it })

compose.onNodeWithText("“guardar” — Anónimo").performClick()

assertEquals(frase, tocada)   // la frase EXACTA, no "algo fue tocado"
```

Más la cabecera con el total, la ★ solo en la favorita y el texto del
botón de tono. Nótese lo que NO se testea: colores, tamaños, posiciones —
eso es diseño, cambia sin romper comportamiento, y clavarlo a tests
produce suites que gritan con cada retoque estético.

## 5. Calibración: romper, rojo, verde

Se quitó el `enabled = uiState.frase != FraseUiState.Cargando` del botón:

```
SaludoContentTest > estadoCargando_muestraElSpinnerYDeshabilitaElBoton FAILED
    java.lang.AssertionError: Failed to assert the following: (is not enabled)
    Semantics of the node: ...
```

Detalle valioso del error: **incluye el volcado de semántica del nodo** —
se ve exactamente qué propiedades tenía frente a las esperadas. Restaurada
la condición, 12/12 verdes.

## 6. Errores típicos en tests de UI de Compose

| Hábito | Problema |
|---|---|
| Testear contra la pantalla stateful (con ViewModel dentro) | necesitas Hilt, red, base de datos... para probar un texto: componer la stateless con estado fabricado |
| Afirmar colores/paddings/posiciones | tests de diseño: se rompen con cada retoque estético sin proteger comportamiento |
| `Thread.sleep` esperando recomposiciones | la regla sincroniza sola; si de verdad hay asincronía, `waitUntil` con condición |
| `testTag` en todo "por si acaso" | si tiene texto o rol, búscalo por semántica: los tags son para nodos mudos |
| Texto duplicado en pantalla y `onNodeWithText` a secas | falla con "multiple nodes": afina el matcher o usa `onAllNodesWithText` |
| Ignorar que el árbol de semántica es el de accesibilidad | un nodo infindable para el test suele serlo para TalkBack: es un aviso, no un estorbo |

## 7. Verificar

```bash
cd HolaAndroid
./gradlew connectedDebugAndroidTest    # emulador: guía 07 §7.1
```

12 tests instrumentados (4 de datos + 8 de UI) en ~20 s con el emulador
arrancado. **La suite completa del proyecto: 25 en la JVM + 12 en el
dispositivo = 37 tests.** Informe en
`app/build/reports/androidTests/connected/debug/index.html`.

## Fuentes consultadas (18-07-2026)

- Testear layouts de Compose (oficial): <https://developer.android.com/develop/ui/compose/testing>
- Semántica en Compose (oficial): <https://developer.android.com/develop/ui/compose/accessibility/semantics>
- Cheatsheet de testing de Compose: <https://developer.android.com/develop/ui/compose/testing/testing-cheatsheet>
- APIs `createComposeRule` / finders / assertions: <https://developer.android.com/reference/kotlin/androidx/compose/ui/test/junit4/package-summary>

Con esto termina la serie de ESCRIBIR tests (10-14: pirámide completa).
El broche de la serie es la guía 15: **CI con GitHub Actions** — una suite
que nadie ejecuta no protege nada; toca ejecutarla en cada push, sin
humanos. (Temas avanzados que quedan para el futuro: screenshot testing,
tests con Hilt inyectado, benchmarks.)
