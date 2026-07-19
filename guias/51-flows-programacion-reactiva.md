# Guía 51 — Flows de Kotlin: Programación Reactiva con `Flow`, `StateFlow` y `SharedFlow`

Objetivo: entender a fondo los flujos (`Flow`) de Kotlin —qué son, cómo se crean,
transforman y recolectan— y aplicarlos al patrón real de Android:
Room → Repositorio → ViewModel → UI de Compose, con estado observable seguro.

Prerrequisitos: corrutinas básicas (`suspend`, `viewModelScope`), la arquitectura
por capas de la Guía 18 y Room (Guía 20). Esta lección profundiza en el
mecanismo que ya asomaba en la Guía 42.

---

## 1. ¿Qué es un `Flow`?

Una función `suspend` normal devuelve **un** valor cuando termina. Un `Flow`
representa una **secuencia de valores que llegan a lo largo del tiempo**, de forma
asíncrona, sin bloquear el hilo. Es el equivalente reactivo de una `Sequence`,
pero suspendido.

| Herramienta | Cuántos valores | Síncrono/Asíncrono |
|---|---|---|
| `fun foo(): T` | uno | síncrono |
| `suspend fun foo(): T` | uno | asíncrono (suspende) |
| `List<T>` / `Sequence<T>` | muchos | síncrono |
| `Flow<T>` | muchos | asíncrono (suspende entre emisiones) |

Un `Flow` es **frío** (*cold*): el bloque que produce los valores no se ejecuta
hasta que alguien lo **recolecta** (`collect`), y se vuelve a ejecutar entero por
cada colector. No guarda estado ni emite "al aire".

```kotlin
fun cuentaAtras(desde: Int): Flow<Int> = flow {
    for (i in desde downTo 0) {
        delay(1000)     // trabajo asíncrono: no bloquea el hilo
        emit(i)         // entrega un valor al colector
    }
}
```

---

## 2. Cómo crear un `Flow`

```kotlin
// A) flow { } — el constructor general, con emit()
val numeros: Flow<Int> = flow { emit(1); emit(2); emit(3) }

// B) A partir de valores o colecciones existentes
val fijos = flowOf(1, 2, 3)
val desdeLista = listOf("a", "b").asFlow()

// C) callbackFlow { } — envolver una API basada en callbacks/listeners
fun ubicaciones(cliente: LocationClient): Flow<Location> = callbackFlow {
    val callback = object : LocationCallback {
        override fun onLocation(l: Location) {
            trySend(l)          // empuja el valor al flujo (no suspende)
        }
    }
    cliente.registrar(callback)
    awaitClose { cliente.desregistrar(callback) }   // limpieza al cancelar la colección
}
```

`callbackFlow` es la pieza clave para convertir el mundo antiguo de *listeners*
(sensores, GPS, Firebase) en flujos modernos. El bloque `awaitClose` **es
obligatorio**: mantiene el flujo vivo y libera el recurso cuando el colector se
va.

---

## 3. Recolectar: operadores terminales

Un flujo no hace nada hasta que un **operador terminal** lo consume. `collect` es
el más común; al ser `suspend`, se llama dentro de una corrutina.

```kotlin
viewModelScope.launch {
    cuentaAtras(3).collect { valor ->
        println("Faltan $valor")
    }
}
```

Otros operadores terminales: `first()`, `firstOrNull()`, `toList()`, `count()`,
`reduce { }`, `fold(inicial) { }`. Cada uno recolecta el flujo y devuelve un
resultado único.

---

## 4. Operadores intermedios

Transforman el flujo y devuelven **otro flujo** (frío y perezoso): no se ejecutan
hasta que hay un operador terminal.

| Operador | Qué hace |
|---|---|
| `map { }` | Transforma cada valor. |
| `filter { }` | Deja pasar solo los que cumplen la condición. |
| `transform { }` | Emite 0..N valores por cada entrada (`emit` manual). |
| `onEach { }` | Efecto secundario por valor sin transformarlo (logging). |
| `take(n)` / `drop(n)` | Toma / descarta los primeros `n`. |
| `distinctUntilChanged()` | Ignora valores repetidos consecutivos. |
| `debounce(ms)` | Espera a que se calme la emisión (búsquedas). |
| `scan(inicial) { acc, v -> }` | Como `fold` pero emitiendo cada acumulado. |

```kotlin
val textoBusqueda: Flow<String> = /* ... */ flowOf("")

// debounce todavía es API en "preview": la propiedad/función que lo use debe
// llevar @OptIn(FlowPreview::class) o no compilará (con Compose BOM 2025.06.00 /
// kotlinx-coroutines 1.8.x).
@OptIn(FlowPreview::class)
val resultados: Flow<List<Item>> = textoBusqueda
    .debounce(300)                 // no dispares en cada tecla
    .distinctUntilChanged()        // ignora si el texto no cambió
    .filter { it.length >= 2 }
    .map { consulta -> repositorio.buscar(consulta) }
```

---

## 5. Aplanar flujos: `flatMapLatest`, `flatMapConcat`, `flatMapMerge`

Cuando cada valor genera **otro flujo** (p. ej. una consulta de red por término
de búsqueda), hay que aplanarlos:

- **`flatMapLatest`**: cancela el flujo anterior cuando llega un valor nuevo.
  Perfecto para búsquedas: si el usuario sigue escribiendo, la petición anterior
  se descarta.
- **`flatMapConcat`**: procesa uno tras otro, en orden, sin solaparse.
- **`flatMapMerge`**: los ejecuta todos en paralelo.

```kotlin
// flatMapLatest exige ExperimentalCoroutinesApi; debounce exige FlowPreview
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
val resultados: Flow<List<Item>> = textoBusqueda
    .debounce(300)
    .flatMapLatest { consulta ->
        repositorio.buscarComoFlow(consulta)   // devuelve Flow<List<Item>>
    }
```

---

## 6. Combinar varios flujos: `combine` y `zip`

- **`combine`**: reemite cada vez que **cualquiera** de los flujos emite, usando
  el último valor de los demás. Es lo que arma un estado de UI a partir de varias
  fuentes.
- **`zip`**: empareja emisiones 1 a 1 (espera a que ambos tengan un valor nuevo).

```kotlin
val usuario: Flow<Usuario> = /* ... */ flowOf()
val notificaciones: Flow<Int> = /* ... */ flowOf()

val cabecera: Flow<Cabecera> = combine(usuario, notificaciones) { u, n ->
    Cabecera(nombre = u.nombre, badge = n)
}
```

---

## 7. Contexto de ejecución: `flowOn` y la regla de preservación

Por defecto un flujo se ejecuta en el contexto donde se recolecta. Para mover el
trabajo pesado (red, disco) a otro *dispatcher*, usa **`flowOn`**, que afecta a
todo lo que está **aguas arriba** (antes) en la cadena.

```kotlin
val datos: Flow<List<Item>> = repositorio.observar()
    .map { procesarCostoso(it) }
    .flowOn(Dispatchers.Default)     // map y observar corren en Default
    .catch { emit(emptyList()) }     // esto ya corre en el contexto del colector
```

> ⚠️ **Regla de preservación del contexto.** Nunca cambies el hilo con
> `withContext { emit(...) }` **dentro** de un `flow { }`: lanza
> `IllegalStateException`. Para cambiar de dispatcher se usa `flowOn`, y punto.

Control de ritmo (*backpressure*): `buffer()` deja que el productor siga sin
esperar al colector; `conflate()` se queda solo con el último valor si el colector
va lento; `collectLatest { }` cancela el procesamiento del valor anterior si llega
uno nuevo.

---

## 8. Manejo de errores en el flujo

```kotlin
repositorio.observar()
    .onStart { emit(emptyList()) }          // valor inicial antes de nada
    .retry(retries = 2) { e -> e is IOException }   // reintenta ante fallos de red
    .catch { e -> emit(emptyList()) }        // captura excepciones aguas arriba
    .onCompletion { causa -> /* limpieza */ }
    .collect { /* ... */ }
```

`catch` solo captura lo que ocurre **aguas arriba** de él; una excepción dentro
del propio `collect` va con un `try/catch` normal. Colócalo al final de las
transformaciones y antes del terminal.

---

## 9. Flujos calientes: `StateFlow` y `SharedFlow`

Frente a los flujos fríos, los **calientes** (*hot*) existen con independencia de
los colectores, comparten sus emisiones entre todos y no se reinician por cada
uno.

| | Frío (`flow{}`) | `StateFlow` | `SharedFlow` |
|---|---|---|---|
| ¿Emite sin colectores? | No | Sí (mantiene 1 valor) | Sí (configurable) |
| ¿Valor actual? | No | **Siempre** (`.value`) | No por defecto |
| ¿Duplicados consecutivos? | Sí | No (conflaciona) | Sí |
| Uso típico | fuentes de datos | **estado de UI** | **eventos** (navegar, snackbar) |

```kotlin
// StateFlow: siempre tiene un valor, ideal para el estado de una pantalla
private val _estado = MutableStateFlow<EstadoUi>(EstadoUi.Cargando)
val estado: StateFlow<EstadoUi> = _estado.asStateFlow()   // inmutable hacia fuera
// ...
_estado.value = EstadoUi.Exito(lista)

// SharedFlow: para eventos de una sola vez (no tienen "valor actual")
private val _eventos = MutableSharedFlow<Evento>()
val eventos: SharedFlow<Evento> = _eventos.asSharedFlow()
// ...
_eventos.emit(Evento.MostrarSnackbar("Guardado"))
```

Para convertir un flujo frío en `StateFlow`/`SharedFlow` atado al ciclo de vida
del ViewModel se usa `stateIn` / `shareIn`:

```kotlin
val uiState: StateFlow<EstadoUi> = repositorio.observar()
    .map { EstadoUi.Exito(it) }
    .stateIn(
        scope = viewModelScope,
        // WhileSubscribed(5s): arranca al primer colector y para 5s después del
        // último — sobrevive a rotaciones sin trabajar de más en segundo plano
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EstadoUi.Cargando
    )
```

---

## 10. Ejemplo integrador: Room → Repositorio → ViewModel → Compose

El patrón reactivo completo que usarás a diario. Room emite un `Flow` que se
reejecuta solo cuando cambian los datos; el cambio fluye hasta la UI sin que nadie
"refresque" nada.

```kotlin
// 1. DAO: Room devuelve un Flow y reemite ante cada cambio en la tabla
@Dao
interface TareaDao {
    @Query("SELECT * FROM tareas ORDER BY creada DESC")
    fun observarTareas(): Flow<List<Tarea>>
}

// 2. Repositorio: expone el flujo y fija el dispatcher de datos
class TareaRepository(private val dao: TareaDao) {
    fun observarTareas(): Flow<List<Tarea>> =
        dao.observarTareas().flowOn(Dispatchers.IO)
}

// 3. ViewModel: transforma a estado de UI y lo publica como StateFlow
class TareasViewModel(repo: TareaRepository) : ViewModel() {
    val uiState: StateFlow<TareasUiState> = repo.observarTareas()
        .map { tareas -> TareasUiState(tareas, cargando = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TareasUiState(emptyList(), cargando = true)
        )
}

data class TareasUiState(val tareas: List<Tarea>, val cargando: Boolean)
```

```kotlin
// 4. Compose: recolecta respetando el ciclo de vida
@Composable
fun PantallaTareas(viewModel: TareasViewModel) {
    // collectAsStateWithLifecycle detiene la colección en STOPPED (pantalla oculta)
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    if (estado.cargando) {
        CircularProgressIndicator()
    } else {
        LazyColumn {
            items(estado.tareas, key = { it.id }) { tarea ->
                Text(tarea.titulo)
            }
        }
    }
}
```

> `collectAsStateWithLifecycle()` (del artefacto `lifecycle-runtime-compose`, ya
> incluido en el proyecto) es la forma correcta de recolectar en Android: pausa la
> colección cuando la pantalla no está visible. Evita `collectAsState()` a secas
> para flujos que representan trabajo, porque seguiría activo en segundo plano.

---

## 11. Eventos de una sola vez: por qué `SharedFlow` y no `StateFlow`

Para "navega ahora" o "muestra este snackbar", un `StateFlow` es mala elección:
tiene valor actual, así que tras una rotación la UI volvería a leer el último
evento y lo repetiría. Un `SharedFlow` (sin *replay*) entrega el evento una vez a
quien esté escuchando y no lo retiene.

```kotlin
class FormularioViewModel : ViewModel() {
    private val _eventos = MutableSharedFlow<Evento>()
    val eventos: SharedFlow<Evento> = _eventos.asSharedFlow()

    fun guardar() = viewModelScope.launch {
        // ...
        _eventos.emit(Evento.NavegarAtras)
    }
}

@Composable
fun PantallaFormulario(viewModel: FormularioViewModel, onNavegarAtras: () -> Unit) {
    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                Evento.NavegarAtras -> onNavegarAtras()
            }
        }
    }
    // ...
}
```

---

## 12. Buenas prácticas y errores comunes

| Hábito | Problema / recomendación |
|---|---|
| Exponer `MutableStateFlow` público | Rompe la encapsulación: expón `StateFlow` con `asStateFlow()`. |
| `collectAsState()` para trabajo de fondo | Sigue activo con la pantalla oculta: usa `collectAsStateWithLifecycle()`. |
| `withContext { emit() }` dentro de `flow{}` | Excepción: cambia el hilo con `flowOn`, nunca a mano. |
| `StateFlow` para eventos (snackbar, navegar) | Se repiten al recomponer/rotar: usa `SharedFlow`. |
| Olvidar `awaitClose` en `callbackFlow` | Fuga de recursos: siempre libera el listener ahí. |
| `stateIn` con `Eagerly` sin motivo | Trabaja aunque nadie mire: `WhileSubscribed(5_000)` por defecto. |
| Recolectar el mismo flujo frío N veces | Cada `collect` reejecuta el trabajo: comparte con `shareIn`/`stateIn` si hace falta. |

---

## Fuentes consultadas (19-07-2026)

- Kotlin Flow (guía oficial de Android): <https://developer.android.com/kotlin/flow>
- StateFlow y SharedFlow: <https://developer.android.com/kotlin/flow/stateflow-and-sharedflow>
- Recolectar flujos con conciencia del ciclo de vida: <https://developer.android.com/topic/libraries/architecture/coroutines#lifecycle-aware>
- Referencia de operadores de `kotlinx.coroutines.flow`: <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/>
