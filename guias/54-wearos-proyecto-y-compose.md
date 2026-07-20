# Guía 54 — Compose for Wear OS: proyecto, dependencias y estructura de pantalla

Objetivo: crear la app de reloj con Compose for Wear OS, entender por qué usa
librerías propias (no las de móvil) y montar la estructura base de una pantalla:
`AppScaffold`, `ScreenScaffold`, `TimeText` y la lista curva `ScalingLazyColumn` /
`TransformingLazyColumn`.

Prerrequisitos: Guía 53 (entorno y AVD de reloj) y Jetpack Compose (Módulo II).
Aquí se asume que ya conoces `@Composable`, `Modifier`, estado y listas perezosas.

---

## 1. Dependencias: Wear Compose, no Compose de móvil

Compose for Wear OS reimplementa Material para pantalla redonda y modo ambiente.
Se usan artefactos `androidx.wear.compose:*`, **no** `androidx.compose.material3`.

```kotlin
// build.gradle.kts del módulo :wear
dependencies {
    // Fundamentos de Compose (los mismos que en móvil)
    implementation(platform("androidx.compose:compose-bom:2025.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Wear Compose — Material 3 (recomendado desde 2024/2025)
    implementation("androidx.wear.compose:compose-material3:1.6.2")
    implementation("androidx.wear.compose:compose-foundation:1.6.2")
    implementation("androidx.wear.compose:compose-navigation:1.6.2")

    // navigation-compose aporta el DSL composable()/NavType/navArgument que usa
    // SwipeDismissableNavHost: NO viene transitivo desde wear compose-navigation
    implementation("androidx.navigation:navigation-compose:2.9.8")

    // Previews de reloj: WearDevices y las anotaciones @WearPreview*
    implementation("androidx.wear:wear-tooling-preview:1.0.0")
    implementation("androidx.wear.compose:compose-ui-tooling:1.6.2")

    // Integración con Activity y ciclo de vida
    implementation("androidx.activity:activity-compose:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

> ✅ Versiones verificadas compilando y ensamblando un módulo `:wear` real en este
> equipo (AGP 8.11.1, Kotlin 2.1.21, Compose BOM 2025.06.00, Gradle 8.13,
> `compileSdk` 36, `minSdk` 30). Wear Compose va por su propio tren de versiones
> (serie 1.6.x en jul-2026), independiente del BOM de Compose.

> ⚠️ **No mezcles librerías.** `androidx.wear.compose:compose-material3` (reloj) y
> `androidx.compose.material3` (móvil) tienen clases con el mismo nombre
> (`Button`, `Text`, `Card`). Importar la de móvil en el reloj da componentes que
> no respetan la forma redonda ni el modo ambiente. Comprueba siempre que el
> `import` empieza por `androidx.wear.compose.material3.*`.
>
> Existe también la librería más antigua `androidx.wear.compose:compose-material`
> (basada en Material 2 de Wear). El código nuevo debe usar **material3** de Wear;
> muchos tutoriales viejos aún muestran `Chip`/`ScalingLazyColumn` de la M2.

---

## 2. La Activity de entrada

Igual que en móvil, pero envolviendo la UI en el tema de Wear:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Necesario para el modo ambiente / always-on antes de setContent
        setTheme(android.R.style.Theme_DeviceDefault)
        setContent {
            MiApp()
        }
    }
}
```

---

## 3. El tema de Wear (`MaterialTheme` de reloj)

Wear Material 3 trae su propio `MaterialTheme` con `ColorScheme`, `Typography` y
`Shapes` **pensados para pantalla pequeña** (tamaños de texto legibles a distancia,
alto contraste sobre fondo negro para ahorrar batería en OLED).

```kotlin
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun MiApp() {
    MaterialTheme {   // el de androidx.wear.compose.material3
        Pantalla()
    }
}
```

El **fondo negro** no es estético sin más: en pantallas OLED el negro apaga píxeles
y ahorra batería, clave en el modo always-on.

---

## 4. `AppScaffold` + `ScreenScaffold`: el andamiaje de reloj

En Wear Material 3 la estructura tiene **dos niveles de Scaffold**:

- **`AppScaffold`**: envuelve toda la app una sola vez. Gestiona el `TimeText`
  (la hora arriba) de forma coordinada con las transiciones entre pantallas.
- **`ScreenScaffold`**: envuelve **cada pantalla**. Coordina el indicador de
  scroll y el `TimeText` con el desplazamiento de esa pantalla concreta.

```kotlin
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.Text

@Composable
fun MiApp() {
    MaterialTheme {
        AppScaffold {          // una vez, envuelve toda la app
            Pantalla()
        }
    }
}

@Composable
fun Pantalla() {
    ScreenScaffold {           // una vez por pantalla
        Text("Hola reloj")
    }
}
```

`AppScaffold` ya dibuja el `TimeText` por defecto. Si necesitas personalizarlo o
lo pintas manualmente, se hace con el componente `TimeText { ... }`.

---

## 5. `ScalingLazyColumn` / `TransformingLazyColumn`: la lista curva

El componente estrella del reloj. Una lista vertical perezosa (como `LazyColumn`)
que además **escala y desvanece** los elementos según se acercan al borde curvo de
la pantalla, dando sensación de profundidad y centrando el foco.

- En Wear Material **2**: `ScalingLazyColumn`.
- En Wear Material **3**: `TransformingLazyColumn` (con `rememberTransformingLazyColumnState()`), que además reserva el espacio correcto para el `TimeText` y el `EdgeButton`.

```kotlin
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ScreenScaffold

@Composable
fun ListaTareas(tareas: List<String>) {
    val listState = rememberTransformingLazyColumnState()

    // ScreenScaffold recibe el estado para coordinar TimeText e indicador de scroll
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,   // deja hueco a TimeText y bordes
        ) {
            items(tareas.size) { i ->
                Text(tareas[i])
            }
        }
    }
}
```

> 💡 El `contentPadding` que entrega `ScreenScaffold` es importante: reserva el
> espacio de la hora arriba y del `EdgeButton` abajo, evitando que el primer y
> último elemento queden tapados o cortados por la curva.

---

## 6. Navegación entre pantallas

Se usa `androidx.wear.compose:compose-navigation`, con `SwipeDismissableNavHost`:
en el reloj **deslizar desde el borde izquierdo hacia la derecha vuelve atrás**
(equivale al botón "atrás" del móvil).

```kotlin
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.navigation.compose.composable

@Composable
fun MiApp() {
    val navController = rememberSwipeDismissableNavController()
    MaterialTheme {
        AppScaffold {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "lista"
            ) {
                composable("lista") {
                    ListaTareas(onAbrir = { id -> navController.navigate("detalle/$id") })
                }
                composable("detalle/{id}") { backStack ->
                    Detalle(id = backStack.arguments?.getString("id"))
                }
            }
        }
    }
}
```

`SwipeDismissableNavHost` gestiona el gesto de deslizar-para-cerrar propio del
reloj; **no uses el `NavHost` de móvil**, que no lo implementa.

---

## 7. Previews con forma de reloj

Para ver el layout en redondo y cuadrado desde el IDE, se usa
`@WearPreviewDevices` (o `@Preview` con `device`):

```kotlin
// La anotación multipreview vive en compose-ui-tooling de Wear (no en
// wear-tooling-preview, que solo aporta las constantes WearDevices)
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@WearPreviewDevices          // genera preview redonda y cuadrada a la vez
@Composable
fun PreviewLista() {
    MaterialTheme { ListaTareas(listOf("Comprar", "Correr", "Leer")) }
}
```

Requiere `androidx.wear.compose:compose-ui-tooling`. Alternativa ligera sin esa
dependencia: `@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)`, con
`WearDevices` de `androidx.wear.tooling.preview.devices`.

---

## 8. Ejecutar en el emulador

```bash
cd MiApp
./gradlew :wear:installDebug          # instala en el reloj conectado

# Lanzar la Activity principal (ajusta el applicationId y la clase)
adb shell am start -n com.ejemplo.wear/.MainActivity
```

Verifica en pantalla redonda que: (1) el texto no se corta en las esquinas, (2) la
hora aparece arriba, (3) la lista escala hacia los bordes, (4) deslizar desde el
borde izquierdo vuelve atrás.

---

## 9. Errores comunes

| Error | Causa / solución |
|---|---|
| Componentes cuadrados feos en pantalla redonda | Importaste Material de móvil: usa `androidx.wear.compose.material3.*`. |
| El primer ítem queda bajo la hora | Falta pasar `contentPadding` de `ScreenScaffold` a la lista. |
| "Atrás" no funciona | Usaste `NavHost` de móvil: cambia a `SwipeDismissableNavHost`. |
| Fondo blanco que gasta batería | No apliques temas claros: el reloj usa fondo negro por defecto. |
| Texto ilegible a distancia | Respeta la `Typography` de Wear; no reduzcas tamaños. |

---

## Fuentes consultadas (20-07-2026)

- Compose for Wear OS (guía oficial): <https://developer.android.com/training/wearables/compose>
- Estructura de una app con Scaffold: <https://developer.android.com/training/wearables/compose/scaffold>
- Listas con ScalingLazyColumn / TransformingLazyColumn: <https://developer.android.com/training/wearables/compose/lists>
- Navegación en Wear Compose: <https://developer.android.com/training/wearables/compose/navigation>
- Wear Compose Material 3 (release notes): <https://developer.android.com/jetpack/androidx/releases/wear-compose>
