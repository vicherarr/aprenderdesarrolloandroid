# Guía 18 — Estructura profesional: la arquitectura oficial recomendada

Objetivo: reorganizar `HolaAndroid` según la **Guía de Arquitectura de Apps de Google** — el patrón recomendado oficialmente — con un ejemplo completo: capas UI / dominio / datos, flujo de datos unidireccional (UDF), patrón repositorio y estado de UI inmutable.

> Fuente principal de toda la guía:
> [developer.android.com/topic/architecture](https://developer.android.com/topic/architecture)

---

## 1. El patrón: tres capas y tres principios

> Fuente: [developer.android.com/topic/architecture](https://developer.android.com/topic/architecture)

La arquitectura de la aplicación se organiza en capas jerárquicas con responsabilidades claramente delimitadas:

```
┌────────────────────────────────────────────┐
│                  UI Layer                  │  Compose + ViewModel
└────────────────────────────────────────────┘
        │                          ▲
        │ eventos                  │ estado (UiState)
        │ (onClick...)             │
        ▼                          │
┌────────────────────────────────────────────┐
│           Domain Layer (opcional)          │  lógica de negocio reutilizable: casos de uso
└────────────────────────────────────────────┘
        │                          ▲
        │ peticiones               │ datos / estado
        │ (llamadas)               │
        ▼                          │
┌────────────────────────────────────────────┐
│                 Data Layer                 │  repositorios + fuentes de datos; dueña de los datos
└────────────────────────────────────────────┘
```

Las dependencias entre componentes apuntan siempre en una sola dirección: **la capa de UI conoce a la capa de dominio, la capa de dominio conoce a la capa de datos, y la capa de datos no conoce a nadie de las capas superiores**.

Los tres principios que sostienen el patrón:

1. **Separación de responsabilidades**: cada clase/paquete/capa tiene un cometido claro y acotado.
2. **Única fuente de verdad (SSOT)**: cada dato tiene UN dueño; solo el dueño puede modificarlo. En nuestro proyecto, el contador pertenece al repositorio: nadie más lo muta.
3. **Flujo de datos unidireccional (UDF)**: los eventos fluyen hacia la capa de datos y el estado fluye hacia la capa de UI.

---

## 2. Mapa Detallado: Qué contiene cada capa y qué NO contiene

| Capa | Qué TIENE dentro | Qué NO debe tener NUNCA | Contrato de entrada / salida |
|---|---|---|---|
| **Capa de UI** | Composables, ViewModels, `UiState` (data classes), `collectAsStateWithLifecycle()`. | Peticiones HTTP, llamadas directas a Room/Preferences, lógica de transformación de datos complejos. | **Entrada**: `UiState` inmutable emitido por ViewModel.<br>**Salida**: Eventos de usuario (`onIncrementarClick()`). |
| **Capa de Dominio** | Casos de uso (`UseCase`), algoritmos puramente Kotlin, modelos de dominio. | Importaciones de Android (`android.*`), librerías de UI (Compose, Material), referencias a bases de datos o Retrofit. | **Entrada**: Parámetros simples (ej. `nombre: String`).<br>**Salida**: Datos procesados / `Flow` de modelo de dominio. |
| **Capa de Datos** | Repositorios (`Repository`), Fuentes de datos (`DataSource`), Room DAOs, Retrofit Interfaces, DTOs de red, Entities. | Referencias a Composables, ViewModels, `UiState`, cualquier import de UI o Jetpack Compose. | **Entrada**: Métodos del contrato `Repository` (`fun guardar()`).<br>**Salida**: `Flow` de datos crudos/mapeados o valores puntuales `Result<T>`. |

---

## 3. La estructura de paquetes resultante

> Fuente: [developer.android.com/topic/architecture/recommendations](https://developer.android.com/topic/architecture/recommendations)

```
com.aprender.holaandroid/
├── HolaAndroidApp.kt            # Application (Hilt)
├── MainActivity.kt              # contenedor mínimo: solo monta la pantalla
├── core/
│   └── inicializacion/          # código transversal, no pertenece a ninguna capa de feature
├── data/                        # CAPA DE DATOS
│   ├── local/
│   │   └── ContadorLocalDataSource.kt
│   └── repository/
│       ├── ContadorRepository.kt          # contrato (interfaz)
│       └── DefaultContadorRepository.kt   # implementación
├── domain/                      # CAPA DE DOMINIO (opcional, aquí didáctica)
│   ├── saludo/                  # modelo/lógica de negocio pura
│   │   ├── GeneradorSaludo.kt
│   │   └── ComponedorTarjeta.kt
│   └── usecase/
│       ├── ObtenerSaludoUseCase.kt
│       ├── EnviarSaludoUseCase.kt
│       └── ObservarContadorUseCase.kt
├── di/                          # módulos de Hilt (el "cableado")
└── ui/                          # CAPA DE UI
    ├── saludo/                  # una carpeta por pantalla/feature
    │   ├── SaludoScreen.kt      # composables (con y sin estado)
    │   ├── SaludoUiState.kt     # estado inmutable de la pantalla
    │   └── SaludoViewModel.kt
    └── theme/
```

En apps grandes esto mismo se lleva a **módulos Gradle** separados (`:core:data`, `:feature:saludo`…) — es el enfoque del ejemplo oficial [Now in Android](https://github.com/android/nowinandroid). Para un proyecto de una pantalla, paquetes bien nombrados son suficientes.

---

## 4. Capa de datos: fuente de datos + repositorio

> Fuente: [developer.android.com/topic/architecture/data-layer](https://developer.android.com/topic/architecture/data-layer)

Dos piezas con papeles distintos:

- **Fuente de datos** (`data/local/ContadorLocalDataSource.kt`): la única clase que sabe *dónde* viven los datos (SharedPreferences/Room/Red).
- **Repositorio** (`data/repository/`): expone los datos al resto de la app y centraliza sus cambios.

El repositorio se divide en contrato e implementación:

```kotlin
interface ContadorRepository {              // lo que la app ve
    val contador: StateFlow<Int>            // SSOT del contador
    fun incrementar()
}

@Singleton
class DefaultContadorRepository @Inject constructor(   // cómo se hace de verdad
    private val localDataSource: ContadorLocalDataSource
) : ContadorRepository { ... }
```

---

## 5. Capa de dominio: casos de uso

> Fuente: [developer.android.com/topic/architecture/domain-layer](https://developer.android.com/topic/architecture/domain-layer)

Reglas oficiales que aplicamos:

- **Nombre**: *verbo en presente + qué + UseCase* → `ObtenerSaludoUseCase`, `EnviarSaludoUseCase`, `ObservarContadorUseCase`.
- **Una responsabilidad por caso de uso.**
- **Invocable como función** con `operator fun invoke()`:

```kotlin
class ObtenerSaludoUseCase @Inject constructor(
    @SaludoFormal private val generadorFormal: GeneradorSaludo,
    @SaludoInformal private val generadorInformal: GeneradorSaludo
) {
    operator fun invoke(nombre: String, esFormal: Boolean): String { ... }
}

// en el ViewModel se llama como si fuera una función:
obtenerSaludo(nombre, formal)
```

---

## 6. Capa de UI: UiState único + ViewModel + pantalla sin estado

> Fuente: [developer.android.com/topic/architecture/ui-layer](https://developer.android.com/topic/architecture/ui-layer)

Tres piezas por pantalla, en `ui/saludo/`:

**1. El estado, inmutable y completo** (`SaludoUiState.kt`): todo lo que la pantalla necesita pintar, en un data class.

**2. El ViewModel** expone **un único** `StateFlow<SaludoUiState>`:

```kotlin
val uiState: StateFlow<SaludoUiState> =
    combine(esFormal, observarContador()) { formal, contador ->
        SaludoUiState(saludo = obtenerSaludo(nombre, formal), contador = contador, ...)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SaludoUiState()
    )
```

**3. La pantalla, dividida en dos composables** (`SaludoScreen.kt`):

- `SaludoScreen` (*stateful*): conecta con el ViewModel usando `collectAsStateWithLifecycle()`.
- `SaludoContent` (*stateless*): recibe `UiState` + callbacks.

---

## 7. Esquema de Comunicación entre Capas (UDF)

```
 ┌──────────────────────────────────────────────────────────────────────────┐
 │                                CAPA DE UI                                │
 │                                                                          │
 │   [Composables] ──(evento onClick)──► [ViewModel]                        │
 │        ▲                                   │                             │
 └────────┼───────────────────────────────────┼─────────────────────────────┘
          │                                   │
   recomposición por                     llamada al Caso de Uso
   emisión de UiState                         │
          │                                   ▼
 ┌────────┼─────────────────────────────────────────────────────────────────┐
 │        │                          CAPA DE DOMINIO                        │
 │        │                                                                 │
 │   [UiState emitido] ◄───(combina)──── [UseCase]                          │
 │        ▲                                   │                             │
 └────────┼───────────────────────────────────┼─────────────────────────────┘
          │                                   │
     emisión de                             llamada al método
     StateFlow                              del Repositorio (contrato)
          │                                   │
          │                                   ▼
 ┌────────┼─────────────────────────────────────────────────────────────────┐
 │        │                          CAPA DE DATOS                          │
 │        │                                                                 │
 │   [StateFlow] ◄───(emite nuevo)─── [Repository] ◄─── (lee/escribe)       │
 │                                            │                             │
 │                                     [DataSource] (Room / Preferences / API)
 └──────────────────────────────────────────────────────────────────────────┘
```

### 7.1 Comunicación UI → Datos: Llamadas de Eventos

Cada capa recibe por constructor (Hilt) una referencia al contrato de la capa inferior y lo invoca:

```kotlin
// UI: el ViewModel tiene el caso de uso (inyectado) y lo llama
fun enviarSaludo() = enviarSaludoUseCase()

// DOMINIO: el caso de uso tiene la interfaz del repositorio y la llama
class EnviarSaludoUseCase @Inject constructor(
    private val contadorRepository: ContadorRepository
) {
    operator fun invoke() = contadorRepository.incrementar()
}

// DATOS: el repositorio ejecuta la acción en la fuente de datos
override fun incrementar() {
    val nuevo = _contador.value + 1
    localDataSource.guardar(nuevo)
}
```

### 7.2 Comunicación Datos → UI: Emisión de Estado Observable

La capa de datos expone un `StateFlow` reactivo. Las capas superiores se suscriben para recibir las emisiones automáticamente sin que la capa de datos tenga conocimiento de la UI:

```kotlin
// DATOS: el repositorio emite hacia su StateFlow
private val _contador = MutableStateFlow(localDataSource.leer())
override val contador: StateFlow<Int> = _contador.asStateFlow()

// UI: el ViewModel observa el flujo mediante combine/stateIn
combine(esFormal, observarContador()) { formal, contador -> ... }
```

### 7.3 Resumen de mecanismos de comunicación

| Necesidad | Mecanismo | Flujo | Ejemplo |
|---|---|---|---|
| Pedir un dato puntual | llamada + valor de retorno (`suspend` si es asíncrona) | UI → Datos (Llamada)<br>Datos → UI (Retorno) | `obtenerSaludo(nombre, formal): String` |
| Ordenar una acción | llamada a método sin retorno | UI → Datos | `enviarSaludo()` |
| Observar datos cambiantes | `Flow`/`StateFlow` reactivo | Datos → UI | `contador: StateFlow<Int>` |

---

## 8. Reglas para mantener la arquitectura limpia

| Prohibido | En su lugar |
|---|---|
| La UI lee SharedPreferences/Room/red directamente | Siempre a través de un repositorio |
| `Context`/Activity dentro de un ViewModel | Inyecta abstracciones; el Context solo en la capa de datos vía `@ApplicationContext` |
| Exponer `MutableStateFlow` al exterior | Expón `StateFlow` inmutable (`asStateFlow()` / `stateIn`) |
| Varias propiedades de estado sueltas en el ViewModel | Un único `UiState` inmutable |
| El dominio importando clases de UI o de Android | Dominio puro: solo Kotlin y contratos de datos |

---

## Fuentes consultadas (18-07-2026)

- Guía de arquitectura (principios y capas): <https://developer.android.com/topic/architecture>
- Capa de dominio y casos de uso: <https://developer.android.com/topic/architecture/domain-layer>
- Capa de UI y UiState: <https://developer.android.com/topic/architecture/ui-layer>
- Capa de datos: <https://developer.android.com/topic/architecture/data-layer>
