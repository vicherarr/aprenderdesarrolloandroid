# Anexo 5 — Buenas Prácticas con Jetpack Compose

Objetivo: reunir los hábitos que separan un Compose "que pinta" de un Compose
correcto y eficiente: flujo de datos unidireccional, gestión de estado, efectos
secundarios y control de las recomposiciones. Complementa las Guías 03–14 (los
componentes) con el **cómo usarlos bien**.

Cada punto incluye el porqué y una comparación de la forma a evitar (❌) frente a
la recomendada (✅).

---

## 1. Flujo de datos unidireccional: estado abajo, eventos arriba

El patrón central de Compose (*Unidirectional Data Flow*): el estado **baja** como
parámetros y los eventos **suben** como lambdas. El composable no posee su estado:
lo recibe y notifica lo que ocurre. Así es reutilizable, previsualizable y
testeable.

```kotlin
// ❌ Stateful: el composable esconde y posee su estado; nadie más puede controlarlo
@Composable
fun Contador() {
    var cuenta by remember { mutableStateOf(0) }
    Button(onClick = { cuenta++ }) { Text("Total: $cuenta") }
}

// ✅ Stateless: recibe el estado y emite el evento. El dueño del estado decide.
@Composable
fun Contador(cuenta: Int, onIncrementar: () -> Unit) {
    Button(onClick = onIncrementar) { Text("Total: $cuenta") }
}
```

> 💡 Puedes ofrecer ambas: una versión *stateful* que envuelve a la *stateless*
> (*state hoisting* opcional). La *stateless* contiene la UI; la *stateful* le
> conecta el estado.

---

## 2. Eleva el estado al nivel adecuado

Cuando dos composables comparten un estado, **elévalo** (*hoist*) hasta su
ancestro común más bajo. Si el estado representa la pantalla y sobrevive a giros o
navegación, su sitio es el **ViewModel**, expuesto como `StateFlow` (Guía 51).

```kotlin
@Composable
fun PantallaPerfil(viewModel: PerfilViewModel) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    // La pantalla recibe estado + lambdas; NO recibe el ViewModel entero hacia abajo
    ContenidoPerfil(estado = estado, onEditar = viewModel::editar)
}
```

> ⚠️ **No pases el ViewModel a los composables hijos.** Pásales el estado que
> necesitan y lambdas para los eventos. Un hijo que recibe el ViewModel deja de
> ser reutilizable y previsualizable.

---

## 3. `remember` y `rememberSaveable`

Un composable puede ejecutarse (recomponerse) muchas veces. Todo lo que crees en
su cuerpo se **recrea en cada recomposición** salvo que lo envuelvas en `remember`.

```kotlin
// ❌ Se crea un ColorPicker nuevo en cada recomposición
val picker = ColorPicker()

// ✅ Se recuerda entre recomposiciones (se recalcula solo si cambia la clave)
val picker = remember { ColorPicker() }
val formateado = remember(cantidad) { formatearMoneda(cantidad) }  // recalcula si cambia 'cantidad'
```

`rememberSaveable` va un paso más allá: **sobrevive a cambios de configuración**
(giro de pantalla, cambio de idioma) guardando el valor en el `Bundle`. Úsalo para
estado de UI que el usuario no querría perder al girar (texto de un campo, pestaña
activa).

---

## 4. Acepta siempre un parámetro `Modifier`

Por convención, todo composable reutilizable recibe un `Modifier` como **primer
parámetro opcional** con valor por defecto `Modifier`, y lo aplica a su elemento
raíz. Así quien lo usa puede ajustar tamaño, padding o comportamiento desde fuera.

```kotlin
@Composable
fun Etiqueta(
    texto: String,
    modifier: Modifier = Modifier,   // primer opcional, por defecto Modifier
) {
    Text(texto, modifier = modifier) // se aplica al raíz
}
```

Recuerda que **el orden de los modificadores importa** (Guía 03): no es lo mismo
`padding` antes que `background` que al revés.

---

## 5. La composición debe ser libre de efectos secundarios

Tu función `@Composable` puede ejecutarse en **cualquier orden**, en **paralelo**,
**saltarse** recomposiciones y ejecutarse **muchas veces por segundo**. Por eso su
cuerpo debe ser como una función pura: no escribas en variables externas, no hagas
peticiones de red ni logging directamente en él.

```kotlin
// ❌ Efecto secundario en el cuerpo: se dispara en cada recomposición, impredecible
@Composable
fun Pantalla(id: String) {
    analytics.registrar("vista_$id")   // ← MAL: se ejecuta N veces
    // ...
}

// ✅ Encapsula el efecto en una API de efecto, atada a una clave
@Composable
fun Pantalla(id: String) {
    LaunchedEffect(id) {               // se ejecuta al entrar y cuando cambia 'id'
        analytics.registrar("vista_$id")
    }
}
```

---

## 6. Efectos secundarios con la API correcta

| API | Cuándo usarla |
|---|---|
| `LaunchedEffect(clave)` | Lanzar trabajo `suspend` al entrar en composición / al cambiar la clave. |
| `rememberCoroutineScope()` | Lanzar corrutinas **desde un callback** (ej. `onClick`), no en composición. |
| `DisposableEffect(clave)` | Registrar algo que hay que **liberar** al salir (listeners, observers). |
| `rememberUpdatedState(valor)` | Referenciar el último valor dentro de un efecto de larga vida sin reiniciarlo. |
| `snapshotFlow { }` | Convertir estado de Compose en un `Flow`. |
| `produceState(inicial)` | Producir un `State` desde una fuente asíncrona. |
| `SideEffect { }` | Publicar el estado de Compose a código no-Compose en cada recomposición exitosa. |

```kotlin
// Lanzar una corrutina desde un evento (no desde composición)
val scope = rememberCoroutineScope()
Button(onClick = { scope.launch { snackbar.showSnackbar("Hecho") } }) { Text("Guardar") }

// Registrar y liberar un recurso
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event -> /* ... */ }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```

---

## 7. `derivedStateOf` para estado derivado

Cuando un valor se **calcula** a partir de otro estado pero cambia **menos a
menudo** que sus entradas, `derivedStateOf` evita recomposiciones inútiles: solo
notifica cuando el resultado cambia de verdad.

```kotlin
val listState = rememberLazyListState()

// ❌ Recompone en CADA píxel de scroll (firstVisibleItemIndex cambia mucho)
val mostrarBotonArriba = listState.firstVisibleItemIndex > 0

// ✅ Solo recompone cuando el booleano cambia (de false a true y viceversa)
val mostrarBotonArriba by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}
```

---

## 8. Difiere la lectura del estado lo máximo posible

Leer un estado que cambia con frecuencia (scroll, animación) en el cuerpo del
composable recompone todo. Si en cambio lo lees dentro de una **lambda** (en los
modificadores que la aceptan), el trabajo se aplaza a las fases de *layout* o
*dibujo*, sin recomponer.

```kotlin
// ❌ Lee 'offset' en composición: recompone en cada fotograma del scroll
Box(Modifier.offset(x = offset.value.dp))

// ✅ Lee 'offset' en la lambda: se salta la recomposición (solo re-layout)
Box(Modifier.offset { IntOffset(x = offset.value, y = 0) })
```

---

## 9. Usa tipos estables e inmutables para los parámetros

Compose **salta** la recomposición de un composable si sus parámetros no han
cambiado, pero solo si sus tipos son **estables**. Los `data class` con `val` de
tipos primitivos lo son; una `List` (interfaz mutable en potencia) el compilador
la considera inestable. Modela el estado con `data class` inmutables y, si sabes
más que el compilador, anótalo con `@Immutable` o `@Stable`.

```kotlin
@Immutable
data class PerfilUiState(
    val nombre: String,
    val etiquetas: List<String>,   // el @Immutable promete que no mutará
)
```

> 💡 Para colecciones garantizadas como estables, existe
> `kotlinx.collections.immutable` (`ImmutableList`). Como mínimo, no expongas
> `MutableList`/`MutableState` en tus modelos de UI.

---

## 10. Da identidad a los elementos de las listas con `key`

En `LazyColumn`/`LazyRow`, pasa una `key` única y estable por elemento. Sin ella,
Compose usa la posición: al insertar o reordenar recompone de más y las
animaciones de elemento fallan (Guía 05).

```kotlin
LazyColumn {
    items(tareas, key = { it.id }) { tarea ->   // identidad estable por id
        FilaTarea(tarea)
    }
}
```

---

## 11. No hagas trabajo caro en composición

Ordenar, filtrar o parsear en el cuerpo del composable se repite en cada
recomposición. Envuélvelo en `remember` con la clave adecuada, o hazlo antes (en
el ViewModel).

```kotlin
// ✅ El orden se recalcula solo si cambia la lista de entrada
val ordenadas = remember(tareas) { tareas.sortedBy { it.fecha } }
```

---

## 12. Recolecta flujos con conciencia del ciclo de vida

Para observar un `StateFlow`/`Flow` del ViewModel, usa
`collectAsStateWithLifecycle()` (Guía 51), que pausa la colección cuando la
pantalla no está visible, en lugar de `collectAsState()` a secas.

```kotlin
val estado by viewModel.uiState.collectAsStateWithLifecycle()
```

---

## 13. Componentes reutilizables con *slot APIs*

En vez de un composable con quince parámetros de configuración, exprésalo con
**huecos de contenido** (lambdas `@Composable`), como hacen `Scaffold` o `Card`.
Quien lo usa decide qué va dentro; tú controlas la disposición.

```kotlin
@Composable
fun TarjetaAccion(
    titulo: String,
    modifier: Modifier = Modifier,
    accion: @Composable () -> Unit,   // hueco: el llamador pone su botón
) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            accion()
        }
    }
}
```

---

## 14. Previsualiza composables *stateless*

Las `@Preview` funcionan sin emulador y aceleran el diseño, pero solo si el
composable es *stateless* (recibe su estado). Alimenta varios casos con
`@PreviewParameter` (con datos, vacío, error).

```kotlin
@Preview(showBackground = true)
@Composable
private fun ContadorPreview() {
    MaterialTheme {
        Contador(cuenta = 3, onIncrementar = {})
    }
}
```

Por eso el punto 1 (composables *stateless*) también es lo que hace tu UI fácil de
previsualizar: todo encaja.

---

## Fuentes consultadas (19-07-2026)

- Pensar en Compose y flujo de datos: <https://developer.android.com/develop/ui/compose/mental-model>
- Estado y *state hoisting*: <https://developer.android.com/develop/ui/compose/state>
- Efectos secundarios: <https://developer.android.com/develop/ui/compose/side-effects>
- Rendimiento y estabilidad: <https://developer.android.com/develop/ui/compose/performance/stability>
- API de Compose y convención del `Modifier`: <https://developer.android.com/develop/ui/compose/api-guidelines>
