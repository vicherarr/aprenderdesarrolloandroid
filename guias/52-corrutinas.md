# Guía 52 — Corrutinas de Kotlin: Asincronía sin Bloqueos

Objetivo: dominar las corrutinas de Kotlin —el motor de asincronía sobre el que
se construyen los `Flow` (Guía 51), Room, Retrofit y los ViewModels—: funciones
`suspend`, *builders*, `Dispatchers`, concurrencia estructurada, cancelación y
manejo de errores, aplicado al patrón real de Android.

Prerrequisitos: Kotlin básico y la arquitectura por capas de la Guía 18. Esta
lección es el cimiento de la Guía 51 (Flows).

---

## 1. El problema: no bloquear el hilo principal

En Android, el **hilo principal** (*Main*) dibuja la UI. Si lo bloqueas con una
operación lenta (red, disco), la app se congela y el sistema muestra un ANR
(*Application Not Responding*). La solución clásica —*callbacks* anidados— vuelve
el código ilegible ("*callback hell*").

Una **corrutina** es un bloque de código que puede **suspenderse** y reanudarse
sin bloquear el hilo. Escribes código secuencial, de arriba abajo, y por debajo
se ejecuta de forma asíncrona. Son mucho más ligeras que los hilos: puedes lanzar
miles.

```kotlin
// ❌ Bloquea el hilo principal → ANR
fun cargar() {
    val datos = api.descargarSincrono()   // congela la UI varios segundos
    mostrar(datos)
}

// ✅ Suspende sin bloquear: la UI sigue fluida
suspend fun cargar() {
    val datos = api.descargar()   // suspende aquí; el hilo queda libre
    mostrar(datos)                // se reanuda cuando llegan los datos
}
```

---

## 2. Funciones `suspend`

`suspend` marca una función que **puede pausarse**. Solo se puede llamar desde
otra función `suspend` o desde dentro de una corrutina. No bloquea: cede el hilo
mientras espera.

```kotlin
suspend fun obtenerUsuario(id: String): Usuario {
    delay(1000)                 // suspende 1s SIN bloquear (a diferencia de Thread.sleep)
    return api.usuario(id)
}
```

`suspend` por sí solo no cambia de hilo ni lanza nada en paralelo: solo habilita
la suspensión. Quién y dónde se ejecuta lo deciden el *scope* y el *dispatcher*.

---

## 3. Lanzar corrutinas: `launch`, `async` y `runBlocking`

Las corrutinas se lanzan con *builders* desde un `CoroutineScope`:

```kotlin
// launch: "dispara y olvida"; devuelve un Job para controlarla
val job = scope.launch {
    val datos = repositorio.cargar()
    _estado.value = datos
}

// async: cuando necesitas un RESULTADO; devuelve un Deferred<T> que se espera con await()
val deferred = scope.async { repositorio.cargar() }
val resultado = deferred.await()

// runBlocking: bloquea el hilo hasta terminar. SOLO para tests o main() de consola,
// NUNCA en código de producción de Android
fun main() = runBlocking { obtenerUsuario("1") }
```

Regla práctica: **`launch`** para efectos (actualizar estado); **`async`** cuando
lanzas varias tareas y necesitas sus valores.

---

## 4. Concurrencia estructurada: el `CoroutineScope`

Cada corrutina vive en un *scope* que define su ciclo de vida. Las corrutinas
forman una jerarquía padre-hijo: si el padre se cancela, **todos sus hijos se
cancelan** automáticamente. Esto evita fugas: cuando la pantalla desaparece, su
trabajo se detiene solo.

Android te da *scopes* atados al ciclo de vida:

```kotlin
class MiViewModel : ViewModel() {
    fun cargar() {
        viewModelScope.launch {        // se cancela al destruirse el ViewModel
            // ...
        }
    }
}

// En una Activity/Fragment: lifecycleScope (atado al ciclo de vida de la vista)
lifecycleScope.launch { /* ... */ }
```

> ⚠️ **Nunca uses `GlobalScope`.** Vive para siempre, no se cancela con la
> pantalla y filtra memoria y trabajo. Usa siempre un *scope* con ciclo de vida
> (o crea el tuyo con `CoroutineScope(SupervisorJob() + Dispatchers.Main)` y
> cancélalo tú).

---

## 5. `Dispatchers`: en qué hilo se ejecuta

El *dispatcher* decide el hilo o *pool* de hilos donde corre la corrutina:

| Dispatcher | Para qué |
|---|---|
| `Dispatchers.Main` | UI: actualizar Compose/vistas. En Android es el hilo principal. |
| `Dispatchers.IO` | Entrada/salida: red, disco, base de datos (pool grande de hilos). |
| `Dispatchers.Default` | CPU intensivo: parsear, ordenar, procesar imágenes. |
| `Dispatchers.Unconfined` | Casos avanzados/tests; no fuerza un hilo concreto. |

Para cambiar de hilo **dentro** de una corrutina se usa `withContext`, que
suspende, ejecuta el bloque en el otro dispatcher y devuelve su resultado:

```kotlin
// Patrón "main-safety": la función es segura de llamar desde el hilo principal
suspend fun cargarPerfil(): Perfil = withContext(Dispatchers.IO) {
    val json = api.descargar()      // corre en IO
    parsear(json)                    // sigue en IO
}
// El llamador (en Main) recibe el resultado ya listo, sin haber bloqueado la UI
```

Regla de oro: **la responsabilidad del hilo es de la función que hace el
trabajo**, no de quien la llama. Los repositorios y casos de uso envuelven su
trabajo pesado en `withContext(IO/Default)`; el ViewModel los llama desde `Main`
sin preocuparse.

---

## 6. El `Job`: controlar y cancelar

`launch` devuelve un `Job`; `async`, un `Deferred` (que es un `Job` con
resultado). Con él controlas la corrutina:

```kotlin
val job = scope.launch { trabajoLargo() }
job.cancel()          // pide cancelación
job.join()            // suspende hasta que termine
job.cancelAndJoin()   // ambas
```

La cancelación es **cooperativa**: una corrutina solo se cancela en los puntos de
suspensión. Si haces un cálculo largo sin suspender, debes comprobar tú si sigue
activa:

```kotlin
scope.launch {
    for (dato in millonesDeDatos) {
        ensureActive()        // lanza CancellationException si se canceló
        procesar(dato)
    }
}
```

`isActive` (comprobar sin lanzar), `ensureActive()` (lanza si cancelado) y
`yield()` (cede el hilo y comprueba cancelación) son tus herramientas. Para
liberar recursos aunque haya cancelación, usa `try/finally`; si necesitas
suspender durante la limpieza, envuélvela en `withContext(NonCancellable)`.

---

## 7. Paralelismo con `async` y alcances anidados

Para lanzar tareas **en paralelo** y esperar a todas, combina `coroutineScope`
con `async`:

```kotlin
suspend fun cargarPantalla(): Pantalla = coroutineScope {
    // Ambas empiezan a la vez
    val usuario = async { api.usuario() }
    val noticias = async { api.noticias() }
    // await() espera el resultado; el total tarda lo que la MÁS lenta, no la suma
    Pantalla(usuario.await(), noticias.await())
}

// awaitAll para una lista de tareas
suspend fun cargarTodo(ids: List<String>): List<Usuario> = coroutineScope {
    ids.map { id -> async { api.usuario(id) } }.awaitAll()
}
```

- **`coroutineScope { }`**: crea un alcance hijo; no vuelve hasta que **todas** las
  corrutinas hijas terminan. Si una falla, cancela a las hermanas y propaga el
  error.
- **`supervisorScope { }`**: aísla los fallos; una hija puede fallar sin cancelar
  a las demás.

---

## 8. Manejo de errores

Una excepción en `launch` **se propaga** hacia arriba en el momento; en `async`
se retiene y **estalla al llamar `await()`**.

```kotlin
// try/catch normal alrededor del código que suspende
viewModelScope.launch {
    try {
        val datos = repositorio.cargar()
        _estado.value = Exito(datos)
    } catch (e: IOException) {
        _estado.value = Error("Sin conexión")
    }
}
```

Para un manejo centralizado en un *scope*, un `CoroutineExceptionHandler` captura
lo que no se trató (solo funciona con `launch`, no con `async`):

```kotlin
val handler = CoroutineExceptionHandler { _, e -> Timber.e(e, "Fallo no controlado") }
val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + handler)
```

`SupervisorJob` hace que el fallo de una corrutina hija no tumbe a sus hermanas
(en un `Job` normal, un hijo que falla cancela a todo el árbol).

> ⚠️ **No captures `CancellationException`.** Es la excepción con la que
> funciona la cancelación estructurada. Si haces `catch (e: Exception)`, relánzala
> o usa `catch (e: IOException)` específico. Mejor aún, comprueba con
> `if (e is CancellationException) throw e`.

---

## 9. Tiempos límite: `withTimeout`

```kotlin
// Lanza TimeoutCancellationException si no termina a tiempo
val datos = withTimeout(5_000) { api.descargar() }

// Devuelve null en vez de lanzar
val datosOpcionales = withTimeoutOrNull(5_000) { api.descargar() }
```

---

## 10. Puente con APIs de *callback*: `suspendCancellableCoroutine`

Para convertir una API antigua basada en *callbacks* en una función `suspend`
(equivalente a lo que `callbackFlow` hace para flujos):

```kotlin
suspend fun ubicacionActual(cliente: LocationClient): Location =
    suspendCancellableCoroutine { continuation ->
        val callback = object : LocationCallback {
            override fun onLocation(l: Location) = continuation.resume(l)
            override fun onError(e: Throwable) = continuation.resumeWithException(e)
        }
        cliente.pedirUbicacion(callback)
        // Si la corrutina se cancela, liberamos el recurso
        continuation.invokeOnCancellation { cliente.cancelar(callback) }
    }
```

---

## 11. Patrón completo en Android

```kotlin
// Capa de datos: main-safe, inyecta el dispatcher para poder testear
class UsuarioRepository(
    private val api: Api,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun cargar(id: String): Usuario = withContext(io) {
        api.usuario(id)
    }
}

// ViewModel: lanza en viewModelScope (Main), delega el trabajo al repositorio
class PerfilViewModel(private val repo: UsuarioRepository) : ViewModel() {
    private val _estado = MutableStateFlow<PerfilUiState>(PerfilUiState.Cargando)
    val estado: StateFlow<PerfilUiState> = _estado.asStateFlow()

    fun cargar(id: String) {
        viewModelScope.launch {
            _estado.value = try {
                PerfilUiState.Exito(repo.cargar(id))
            } catch (e: IOException) {
                PerfilUiState.Error("No se pudo cargar el perfil")
            }
        }
    }
}
```

En vistas clásicas, para recolectar respetando el ciclo de vida se usa
`repeatOnLifecycle` (en Compose es `collectAsStateWithLifecycle`, Guía 51):

```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        // Se reinicia en STARTED y se cancela en STOPPED (pantalla oculta)
        viewModel.estado.collect { render(it) }
    }
}
```

---

## 12. Buenas prácticas y errores comunes

| Hábito | Problema / recomendación |
|---|---|
| `GlobalScope.launch` | No se cancela, filtra: usa `viewModelScope`/`lifecycleScope`. |
| `Thread.sleep()` en una corrutina | Bloquea el hilo: usa `delay()`, que suspende. |
| Fijar el dispatcher en el llamador | El trabajo pesado fija su propio hilo con `withContext(IO/Default)`. |
| `Dispatchers.IO` hardcodeado en la clase | Inyéctalo (`io: CoroutineDispatcher = Dispatchers.IO`) para testear. |
| `catch (e: Exception)` que traga `CancellationException` | Rompe la cancelación: reláncela o captura tipos concretos. |
| `CoroutineExceptionHandler` con `async` | No aplica a `async`: allí el error salta en `await()`. |
| Cálculo largo sin puntos de suspensión | No se puede cancelar: añade `ensureActive()`/`yield()`. |
| `runBlocking` en producción | Bloquea el hilo principal: solo para tests o `main()`. |

---

## Fuentes consultadas (19-07-2026)

- Corrutinas en Android (guía oficial): <https://developer.android.com/kotlin/coroutines>
- Buenas prácticas de corrutinas: <https://developer.android.com/kotlin/coroutines/coroutines-best-practices>
- Concurrencia estructurada y cancelación: <https://kotlinlang.org/docs/cancellation-and-timeouts.html>
- Excepciones en corrutinas: <https://kotlinlang.org/docs/exception-handling.html>
