# Guía 05 — Estructura profesional: la arquitectura oficial recomendada

Objetivo: reorganizar `HolaAndroid` según la **Guía de Arquitectura de Apps de
Google** — el patrón recomendado oficialmente — con un ejemplo básico pero
completo: capas UI / dominio / datos, flujo de datos unidireccional (UDF),
patrón repositorio y estado de UI inmutable.

> Fuente principal de toda la guía:
> [developer.android.com/topic/architecture](https://developer.android.com/topic/architecture)
> y sus páginas hijas (ui-layer, domain-layer, data-layer, recommendations),
> consultadas el 18-07-2026. Cada sección cita la suya.

---

## 1. El patrón: tres capas y tres principios

> Fuente: [developer.android.com/topic/architecture](https://developer.android.com/topic/architecture)

```
┌─────────────────────┐
│      UI Layer        │  pinta estado y captura eventos (Compose + ViewModel)
└─────────┬───────────┘
          │ depende de
┌─────────▼───────────┐
│   Domain Layer       │  (OPCIONAL) lógica de negocio reutilizable: casos de uso
└─────────┬───────────┘
          │ depende de
┌─────────▼───────────┐
│     Data Layer       │  repositorios + fuentes de datos; dueña de los datos
└─────────────────────┘
```

Las flechas de dependencia solo apuntan **hacia abajo**: la UI conoce el
dominio, el dominio conoce los datos, y nunca al revés.

Los tres principios que sostienen el patrón (transcritos de la fuente):

1. **Separación de responsabilidades**: cada clase/paquete/capa tiene un
   cometido claro y acotado.
2. **Única fuente de verdad (SSOT)**: cada dato tiene UN dueño; solo el dueño
   puede modificarlo. En nuestro proyecto, el contador pertenece al
   repositorio: nadie más lo muta.
3. **Flujo de datos unidireccional (UDF)**: el estado fluye hacia abajo
   (datos → UI) y los eventos hacia arriba (UI → datos).

## 2. La estructura de paquetes resultante

> Fuente: [developer.android.com/topic/architecture/recommendations](https://developer.android.com/topic/architecture/recommendations)
> ("keep small apps' data layer in a `data` package; UI layer in a `ui` package")

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

En apps grandes esto mismo se lleva a **módulos Gradle** separados
(`:core:data`, `:feature:saludo`…) — es el enfoque del ejemplo oficial
[Now in Android](https://github.com/android/nowinandroid). Para un proyecto de
una pantalla, paquetes bien nombrados son suficientes y el salto a módulos es
mecánico.

## 3. Capa de datos: fuente de datos + repositorio

> Fuente: [developer.android.com/topic/architecture/data-layer](https://developer.android.com/topic/architecture/data-layer) y
> [recommendations](https://developer.android.com/topic/architecture/recommendations)

Dos piezas con papeles distintos:

- **Fuente de datos** (`data/local/ContadorLocalDataSource.kt`): la única
  clase que sabe *dónde* viven los datos (SharedPreferences). Migrar a
  DataStore o Room mañana = tocar solo este archivo.
- **Repositorio** (`data/repository/`): expone los datos al resto de la app y
  centraliza sus cambios. La recomendación oficial es crearlo **aunque solo
  haya una fuente de datos** — es la barrera que impide que la UI hable
  directamente con SharedPreferences/Room/red (recomendación explícita de la
  fuente).

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

El prefijo `Default` es la convención oficial de nombres para la
implementación estándar (tabla *Naming conventions* de recommendations; en
apps reales verás también `OfflineFirstNewsRepository` y `Fake...` para tests).
El módulo `di/RepositorioModule.kt` cablea contrato → implementación con
`@Binds` (guía 04).

## 4. Capa de dominio: casos de uso

> Fuente: [developer.android.com/topic/architecture/domain-layer](https://developer.android.com/topic/architecture/domain-layer)

Es **opcional** — la doc lo dice literalmente: úsala para encapsular lógica
compleja o reutilizada por varios ViewModels. Aquí la incluimos por valor
didáctico. Reglas oficiales que aplicamos:

- **Nombre**: *verbo en presente + qué + UseCase* → `ObtenerSaludoUseCase`,
  `EnviarSaludoUseCase`, `ObservarContadorUseCase`.
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

- **Dependen de repositorios** (y de otros casos de uso), nunca de la UI.
- Deben ser *main-safe*: si hicieran trabajo pesado, moverían el cálculo de
  hilo con `withContext(dispatcher)` (aquí no hace falta).

La lógica de negocio pura (`GeneradorSaludo`, `ComponedorTarjeta`) también vive
en `domain/`: no importa nada de Android ni de la UI.

## 5. Capa de UI: UiState único + ViewModel + pantalla sin estado

> Fuente: [developer.android.com/topic/architecture/ui-layer](https://developer.android.com/topic/architecture/ui-layer) y
> [recommendations](https://developer.android.com/topic/architecture/recommendations)

Tres piezas por pantalla, en `ui/saludo/`:

**1. El estado, inmutable y completo** (`SaludoUiState.kt`): todo lo que la
pantalla necesita pintar, en un data class. La UI no calcula nada.

**2. El ViewModel** expone **un único** `StateFlow<SaludoUiState>` — es la
recomendación oficial textual ("expose UI state via single uiState property as
StateFlow"), con el patrón exacto de la doc:

```kotlin
val uiState: StateFlow<SaludoUiState> =
    combine(esFormal, observarContador()) { formal, contador ->
        SaludoUiState(saludo = obtenerSaludo(nombre, formal), contador = contador, ...)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),   // patrón oficial
        initialValue = SaludoUiState()
    )
```

`WhileSubscribed(5_000)` detiene el flujo cuando nadie mira (app en segundo
plano) con 5 s de gracia para sobrevivir a una rotación sin reiniciar el flujo.

Reglas del ViewModel (recommendations): sin `Context` ni referencias a
Activity/recursos, con ámbito de pantalla, y evitando `AndroidViewModel`.

**3. La pantalla, dividida en dos composables** (`SaludoScreen.kt`):

- `SaludoScreen` (*stateful*): conecta con el ViewModel usando
  `collectAsStateWithLifecycle()` — la forma recomendada de consumir flujos en
  Compose (para de coleccionar cuando la UI no está visible). Requiere la
  dependencia `androidx.lifecycle:lifecycle-runtime-compose`.
- `SaludoContent` (*stateless*): recibe `UiState` + callbacks. Al no depender
  de nada, la `@Preview` y los tests son triviales.

## 6. El UDF en acción: qué pasa al pulsar "Enviar saludo"

```
[Botón]  onClick ──► viewModel.enviarSaludo()          (evento, hacia arriba)
                        └─► EnviarSaludoUseCase()
                              └─► ContadorRepository.incrementar()
                                    ├─► LocalDataSource.guardar(n)   (persiste)
                                    └─► _contador.value = n          (SSOT emite)
[Pantalla] ◄── recompone ◄── uiState nuevo ◄── combine reacciona     (estado, hacia abajo)
```

Nadie "pinta el resultado del click": el click **modifica el dato** en su
única fuente de verdad, y el nuevo estado fluye solo hasta la pantalla. Por
eso el contador sobrevive a rotaciones, a matar la app, y siempre es
consistente lo mires desde donde lo mires.

## 7. Qué se movió respecto a la lección 04

| Antes | Ahora | Por qué |
|---|---|---|
| `data/ContadorRepository.kt` (clase) | `data/repository/` (interfaz + `Default...` + datasource) | patrón repositorio completo |
| `data/GeneradorSaludo.kt` | `domain/saludo/` | es lógica de negocio, no acceso a datos |
| `data/ComponedorTarjeta.kt` | `domain/saludo/` | ídem |
| `data/Inicializadores.kt` | `core/inicializacion/` | transversal, no es de ninguna capa |
| `ui/SaludoViewModel.kt` | `ui/saludo/` + `SaludoUiState` + `SaludoScreen` | paquete por pantalla, UiState único |
| lógica en el ViewModel | `domain/usecase/*UseCase.kt` | demostrar la capa de dominio |
| composables en `MainActivity` | `ui/saludo/SaludoScreen.kt` | la Activity queda de contenedor |

## 8. Reglas rápidas para no romper la arquitectura

| Prohibido | En su lugar |
|---|---|
| La UI lee SharedPreferences/Room/red directamente | siempre a través de un repositorio |
| `Context`/Activity dentro de un ViewModel | inyecta abstracciones; el Context solo en la capa de datos vía `@ApplicationContext` |
| Exponer `MutableStateFlow` al exterior | expón `StateFlow` inmutable (`asStateFlow()` / `stateIn`) |
| Varias propiedades de estado sueltas en el ViewModel | un único `UiState` inmutable |
| El dominio importando clases de UI o de Android | dominio puro: solo Kotlin y contratos de datos |
| Repositorio devolviendo tipos de la fuente (Cursor, DTO de red...) | mapea a modelos propios |

## 9. Verificar

```bash
cd HolaAndroid
./gradlew assembleDebug
```

El comportamiento de la app es **idéntico** al de la lección 04 — esa es la
gracia: una refactorización de arquitectura no cambia lo que la app hace,
cambia lo que la app aguanta (crecer, testearse, cambiar de base de datos...).

## Fuentes consultadas (18-07-2026)

- Guía de arquitectura (principios y capas): <https://developer.android.com/topic/architecture>
- Recomendaciones oficiales (qué es "strongly recommended"): <https://developer.android.com/topic/architecture/recommendations>
- Capa de dominio y casos de uso: <https://developer.android.com/topic/architecture/domain-layer>
- Capa de UI y UiState: <https://developer.android.com/topic/architecture/ui-layer>
- Capa de datos: <https://developer.android.com/topic/architecture/data-layer>
- Ejemplo oficial a escala real: <https://github.com/android/nowinandroid>

Próximas lecciones naturales: **tests unitarios del ViewModel y el repositorio**
(con un `FakeContadorRepository`), o **DataStore** como sustituto moderno de
SharedPreferences tocando solo la fuente de datos.
