# Guía 55 — Componentes de UI de Wear OS y entrada rotatoria (rotary input)

Objetivo: dominar los componentes visuales propios del reloj (botones, `Card`,
selectores tipo *chip/toggle*, texto curvo, indicadores de posición, `EdgeButton`,
`Picker`) y manejar la **entrada rotatoria** de la corona/bisel, que es la forma
principal de desplazarse en Wear OS.

Prerrequisitos: Guía 54 (proyecto Compose for Wear OS, `ScreenScaffold`,
`TransformingLazyColumn`). Se usa Wear Material 3 (`androidx.wear.compose.material3`).

---

## 1. Botones adaptados al reloj

Wear Material 3 ofrece botones pensados para dedos sobre pantalla mínima. El
patrón dominante es el botón **de ancho completo con icono + etiqueta**, no el
botón compacto del móvil.

```kotlin
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.Icon

Button(
    onClick = { /* ... */ },
    modifier = Modifier.fillMaxWidth(),
    icon = { Icon(Icons.Default.Check, contentDescription = null) },
    label = { Text("Confirmar") },
)
```

Variantes: `FilledTonalButton`, `OutlinedButton`, `ChildButton` (menor énfasis) y
`CompactButton` (para cuando de verdad hace falta uno pequeño). Elige según la
jerarquía, igual que en Material 3 de móvil.

---

## 2. `EdgeButton`: el botón que abraza el borde

Novedad de Wear Material 3. Un botón con forma que **sigue la curva inferior** de
la pantalla redonda, típico para la acción principal al final de una lista.

```kotlin
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize

ScreenScaffold(
    scrollState = listState,
    edgeButton = {
        EdgeButton(
            onClick = { /* acción principal */ },
            buttonSize = EdgeButtonSize.Large,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Añadir")
        }
    }
) { contentPadding ->
    TransformingLazyColumn(state = listState, contentPadding = contentPadding) { /* ... */ }
}
```

`ScreenScaffold` coordina el `EdgeButton` con el scroll para que no tape contenido.

---

## 3. `Card`: agrupar información

Como en móvil, pero con estilos de reloj. Útil para bloques de datos o para el
cuerpo de una notificación dentro de la app.

```kotlin
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults

Card(onClick = { /* ... */ }) {
    Text("Frecuencia cardíaca")
    Text("72 ppm")
}

// AppCard / TitleCard aportan huecos predefinidos (título, hora, app-image)
```

---

## 4. Selección: `SwitchButton`, `RadioButton`, `Chip`

Para ajustes y listas seleccionables, Wear Material 3 sustituye los antiguos
`ToggleChip`/`SplitToggleChip` de Wear M2 por botones con control integrado:

```kotlin
import androidx.wear.compose.material3.SwitchButton

var activado by remember { mutableStateOf(true) }

SwitchButton(
    checked = activado,
    onCheckedChange = { activado = it },
    label = { Text("Vibración") },
)
```

`RadioButton` (selección única) y `Checkbox`-como controles siguen el mismo patrón
de `label` + control a la derecha, ocupando el ancho para ser fáciles de pulsar.

---

## 5. Texto curvo (`curvedText`) y `CurvedLayout`

El reloj permite dibujar texto **siguiendo la curva** del borde, ideal para
etiquetas superiores/inferiores o el `TimeText`.

```kotlin
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.material3.curvedText

CurvedLayout(anchor = 90f) {          // 90° = abajo del círculo
    curvedText("Desliza para más")
}
```

`TimeText` internamente es texto curvo. Puedes añadir contenido curvo propio al
`TimeText { }` para mostrar, por ejemplo, un estado junto a la hora.

---

## 6. `Picker`: seleccionar un valor girando

El `Picker` es el selector rotatorio clásico del reloj (elegir hora, número,
opción). Se controla con la corona.

```kotlin
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.rememberPickerState

val state = rememberPickerState(initialNumberOfOptions = 60)

// Ojo: en Wear Material 3, contentDescription es una lambda () -> String
// (no un String como en la Material 2 de Wear). Verificado compilando.
Picker(state = state, contentDescription = { "Minutos" }) { index ->
    Text(text = "%02d".format(index))
}
// Valor elegido: state.selectedOptionIndex
```

---

## 7. Entrada rotatoria (rotary input): la corona y el bisel

La **corona giratoria** (o el bisel táctil en algunos relojes) genera eventos de
scroll. En Wear OS es la forma principal —y más cómoda— de desplazar listas y
`Picker`, sin tapar la pantalla con el dedo.

`TransformingLazyColumn` y `Picker` **ya responden a la corona** si tienen el foco.
Para conectarlo correctamente se usa `rotaryScrollable` enlazado al estado de la
lista y a un `FocusRequester`:

```kotlin
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults

@Composable
fun ListaConCorona(items: List<String>) {
    val listState = rememberTransformingLazyColumnState()
    val focusRequester = remember { FocusRequester() }

    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = padding,
            modifier = Modifier
                .rotaryScrollable(
                    RotaryScrollableDefaults.behavior(scrollableState = listState),
                    focusRequester = focusRequester,
                )
        ) {
            items(items.size) { i -> Text(items[i]) }
        }
    }

    // La lista debe tener el foco para recibir los eventos de la corona
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
```

> ⚠️ Si la lista no tiene el foco, la corona no hará nada. El `FocusRequester` +
> `requestFocus()` es imprescindible. Prueba **siempre con la corona del
> emulador** (la rueda del ratón sobre la ventana del AVD la simula), no solo con
> arrastre táctil.

Para un giro que ajusta un valor (volumen, zoom) en lugar de desplazar, existe
`rotaryScrollable` con comportamiento *snap* o el modo de eventos crudos
`onRotaryScrollEvent` para casos a medida.

---

## 8. Indicadores de posición y desplazamiento

En pantalla redonda no cabe una barra de scroll recta. Wear usa un **arco lateral**
(`ScrollIndicator`/`PositionIndicator`) que `ScreenScaffold` ya dibuja
automáticamente cuando le pasas el `scrollState`. Rara vez hay que colocarlo a mano;
si lo haces, se ancla al borde derecho siguiendo la curva.

---

## 9. Diálogos y confirmaciones

Wear Material 3 trae diálogos a pantalla completa optimizados: `AlertDialog`
(confirmar/cancelar con botones grandes) y `ConfirmationDialog` (un tic animado
breve tras una acción, "Guardado ✓"). Evita diálogos densos: en el reloj, una
pregunta y dos botones grandes.

---

## 10. Buenas prácticas de UI en el reloj

| Recomendación | Motivo |
|---|---|
| Diseña *circular-first* y verifica en redondo | Las esquinas se recortan; el texto ancho se corta. |
| Botones y objetivos táctiles grandes (≥ 48 dp) | El dedo tapa media pantalla; el error de pulsación es alto. |
| Contenido *glanceable*: 1 dato principal por pantalla | El usuario mira 2 segundos. |
| Soporta la corona en todo lo desplazable | Es más cómodo y no tapa la pantalla. |
| Fondo oscuro, alto contraste | Ahorra batería OLED y se lee bajo el sol. |
| Nada de tablas densas ni formularios largos | Divide en pasos o delega al teléfono. |

---

## Fuentes consultadas (20-07-2026)

- Componentes de Compose for Wear OS: <https://developer.android.com/training/wearables/compose/components>
- Entrada rotatoria (rotary input): <https://developer.android.com/training/wearables/compose/rotary-input>
- Botones y EdgeButton (Wear Material 3): <https://developer.android.com/jetpack/androidx/releases/wear-compose>
- Pickers y selección: <https://developer.android.com/training/wearables/compose/lists>
- Diseño de UI para Wear OS: <https://developer.android.com/design/ui/wear>
