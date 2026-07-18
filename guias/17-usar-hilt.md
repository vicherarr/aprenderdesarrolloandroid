# Guía 17 — Usar Dagger Hilt a fondo

Objetivo: entender **todas las formas de proveer e inyectar dependencias** que
ofrece Hilt, con ejemplos reales implementados y compilados en `HolaAndroid`.
Cada sección indica la fuente oficial de la que sale la información y el
archivo del proyecto donde está el ejemplo funcionando.

Lo que se construye: una pequeña funcionalidad de "saludos" — la pantalla
saluda en tono formal o informal, cuenta cuántos saludos se han enviado (y lo
persiste entre arranques) y compone una "tarjeta" con datos de runtime. Es
deliberadamente simple para que todo el protagonismo sea del grafo de
dependencias.

---

## 1. La idea central: el grafo de dependencias

> Fuente: [developer.android.com/training/dependency-injection/hilt-android](https://developer.android.com/training/dependency-injection/hilt-android)

Hilt construye en **tiempo de compilación** un grafo con todas las clases que
sabe crear. Cuando una clase declara lo que necesita en su constructor, Hilt
resuelve la cadena completa. Si falta un eslabón, **falla la compilación**, no
la app en producción — esa es su gran ventaja frente a un *service locator*
casero.

En este proyecto el grafo queda así:

```
SharedPreferences ──► ContadorRepository ──► SaludoViewModel ◄── GeneradorSaludo (formal/informal)
       │                      │                    ▲
       └── PrimerArranqueInicializador             │
                                        SavedStateHandle (binding por defecto)
```

## 2. La jerarquía de componentes (el mapa que hay que memorizar)

> Fuente: [dagger.dev/hilt/components](https://dagger.dev/hilt/components.html)
> (consultada el 18-07-2026; la tabla es transcripción de la documentación oficial)

Cada componente vive un ciclo de vida Android concreto. Un binding instalado en
un componente es visible en ese componente **y en sus hijos**, nunca al revés.

| Componente | Scope asociado | Se crea en | Se destruye en | Bindings por defecto |
|---|---|---|---|---|
| `SingletonComponent` | `@Singleton` | `Application#onCreate()` | proceso terminado | `Application`, `@ApplicationContext Context` |
| `ActivityRetainedComponent` | `@ActivityRetainedScoped` | `Activity#onCreate()` | `Activity#onDestroy()` (sobrevive a rotaciones) | `ActivityRetainedLifecycle` |
| `ViewModelComponent` | `@ViewModelScoped` | creación del ViewModel | destrucción del ViewModel | `SavedStateHandle`, `ViewModelLifecycle` |
| `ActivityComponent` | `@ActivityScoped` | `Activity#onCreate()` | `Activity#onDestroy()` | `Activity`, `@ActivityContext Context` |
| `FragmentComponent` | `@FragmentScoped` | `Fragment#onAttach()` | `Fragment#onDestroy()` | `Fragment` |
| `ViewComponent` | `@ViewScoped` | constructor del `View` | destrucción del `View` | `View` |
| `ViewWithFragmentComponent` | `@ViewScoped` | constructor del `View` | destrucción del `View` | `View` |
| `ServiceComponent` | `@ServiceScoped` | `Service#onCreate()` | `Service#onDestroy()` | `Service` |

Ejemplo en el proyecto: `di/SaludoModule.kt` se instala en
`ViewModelComponent` (solo los ViewModels ven esos bindings), mientras que
`di/AppModule.kt` se instala en `SingletonComponent` (visible en toda la app).

## 3. Las tres formas de proveer una dependencia

> Fuente: [developer.android.com/training/dependency-injection/hilt-android#define-bindings](https://developer.android.com/training/dependency-injection/hilt-android)

| Forma | Cuándo usarla | Ejemplo en el proyecto |
|---|---|---|
| `@Inject constructor` | Clases tuyas. La opción por defecto, sin módulos | `data/ContadorRepository.kt`, `data/GeneradorSaludo.kt` |
| `@Binds` (módulo abstracto) | "Cuando pidan la interfaz, da esta implementación". No genera código de construcción | `di/SaludoModule.kt` |
| `@Provides` (módulo objeto) | Tipos que no puedes anotar: framework, terceros, builders | `di/AppModule.kt` (SharedPreferences, Calendar) |

Regla práctica: usa `@Inject constructor` siempre que puedas; `@Binds` para
interfaces; `@Provides` solo cuando no hay otra opción (es la más costosa en
código generado).

### 3.1 `@Inject constructor` — inyección por constructor

```kotlin
// data/ContadorRepository.kt
@Singleton
class ContadorRepository @Inject constructor(
    private val prefs: SharedPreferences
) { ... }
```

Con solo esto, cualquier clase del grafo puede pedir un `ContadorRepository`.

### 3.2 `@Binds` — interfaz → implementación

```kotlin
// di/SaludoModule.kt
@Module
@InstallIn(ViewModelComponent::class)
abstract class SaludoModule {
    @Binds @SaludoFormal
    abstract fun bindeaSaludoFormal(impl: GeneradorSaludoFormal): GeneradorSaludo
    ...
}
```

### 3.3 `@Provides` — tipos que no controlas

```kotlin
// di/AppModule.kt
@Provides
@Singleton
fun proveeSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
    context.getSharedPreferences("hola_android_prefs", Context.MODE_PRIVATE)
```

Nota el parámetro `@ApplicationContext context`: es un **binding por defecto**
(tabla de la sección 2) — Hilt lo ofrece sin que nadie lo declare.

## 4. Cualificadores: dos bindings del mismo tipo

> Fuente: [developer.android.com/training/dependency-injection/hilt-android#multiple-bindings](https://developer.android.com/training/dependency-injection/hilt-android)

### 4.1 El problema que resuelven

Hilt resuelve las dependencias **por tipo**. Cuando el `SaludoViewModel` pide
un `GeneradorSaludo`, Hilt busca en el grafo quién provee ese tipo. Pero en
este proyecto hay **dos** implementaciones (formal e informal), y si el módulo
las bindease sin más:

```kotlin
@Binds abstract fun a(impl: GeneradorSaludoFormal): GeneradorSaludo
@Binds abstract fun b(impl: GeneradorSaludoInformal): GeneradorSaludo
```

la compilación fallaría con `[Dagger/DuplicateBindings]`: hay dos respuestas a
la misma pregunta y Hilt se niega a adivinar. Es como pedir "un café" donde hay
café solo y café con leche: falta el apellido.

### 4.2 La solución: ampliar la clave de búsqueda

Un cualificador es una etiqueta que pasa a formar parte de la **clave** con la
que Hilt indexa el grafo. Sin cualificador la clave es solo el tipo; con él, la
clave es *tipo + etiqueta*:

```
Clave del grafo                        →  Qué entrega
──────────────────────────────────────────────────────────
@SaludoFormal   + GeneradorSaludo      →  GeneradorSaludoFormal
@SaludoInformal + GeneradorSaludo      →  GeneradorSaludoInformal
```

Dos entradas distintas, como dos claves distintas en un mapa: la ambigüedad
desaparece.

### 4.3 Las tres piezas en el código

**1. Crear la etiqueta** (`di/Cualificadores.kt`). Es una anotación tuya,
marcada a su vez con `@Qualifier` — eso le dice a Dagger que forma parte de la
clave:

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SaludoFormal
```

(`@Retention(BINARY)` solo significa "consérvala en el `.class` para que el
procesador de anotaciones la vea"; es el valor estándar para cualificadores.)

**2. Etiquetar el binding** (`di/SaludoModule.kt`) — el lado del que *ofrece*:

```kotlin
@Binds
@SaludoFormal    // esta oferta se registra bajo la clave (SaludoFormal, GeneradorSaludo)
abstract fun bindeaSaludoFormal(impl: GeneradorSaludoFormal): GeneradorSaludo
```

**3. Etiquetar la petición** (`ui/SaludoViewModel.kt`) — el lado del que *pide*:

```kotlin
@SaludoFormal private val generadorFormal: GeneradorSaludo,
@SaludoInformal private val generadorInformal: GeneradorSaludo,
```

**Regla de oro: la anotación debe estar en los dos lados y coincidir
exactamente.** Si el módulo binde con `@SaludoFormal` pero el constructor pide
`GeneradorSaludo` a secas, son claves distintas: Hilt dará
`[Dagger/MissingBinding]` ("nadie provee `GeneradorSaludo` sin etiqueta"),
aunque existan dos versiones etiquetadas.

### 4.4 ¿Por qué no `@Named`?

Existe un atajo de serie (`javax.inject.Named`) que hace lo mismo con un
string:

```kotlin
@Binds @Named("formal") abstract fun ...
// y al pedir:
@Named("formal") private val generador: GeneradorSaludo
```

Funciona, pero la clave es el string: un typo (`@Named("froma1")`) compila y
solo revienta después como *missing binding*, sin pista de la causa. Con un
cualificador propio, el nombre mal escrito **no compila** porque la anotación
no existe. Por eso la práctica recomendada son cualificadores propios.

### 4.5 Dónde más los verás

Es un patrón constante en proyectos reales — siempre la misma situación:
**mismo tipo, configuraciones distintas**:

- Dos `CoroutineDispatcher`: `@IoDispatcher` / `@MainDispatcher`
- Dos `Retrofit` apuntando a APIs distintas: `@ApiPublica` / `@ApiInterna`
- Dos `OkHttpClient`: con y sin autenticación

Y de hecho ya usamos uno sin declararlo: `@ApplicationContext Context` (en
`di/AppModule.kt`) es un cualificador que trae Hilt para distinguir el
`Context` de aplicación del de Activity (`@ActivityContext`) — mismo tipo
`Context`, dos orígenes distintos.

## 5. Scopes: quién comparte instancia y cuánto vive

> Fuente: [dagger.dev/hilt/components](https://dagger.dev/hilt/components.html) y
> [developer.android.com/training/dependency-injection/hilt-android#component-scopes](https://developer.android.com/training/dependency-injection/hilt-android)

- **Sin scope (por defecto)**: instancia **nueva en cada inyección**. En el
  proyecto: `proveeCalendario()` devuelve un `Calendar` nuevo cada vez — así el
  saludo formal usa la hora del momento.
- **Con scope**: una única instancia por vida del componente. En el proyecto:
  `ContadorRepository` es `@Singleton`; ViewModel, Application y el EntryPoint
  comparten el mismo contador.

Restricción importante: el scope debe **coincidir con el componente del
módulo** (`@Singleton` solo en `SingletonComponent`, `@ViewModelScoped` solo en
`ViewModelComponent`…). Y no hagas todo singleton "por si acaso": un scope
mantiene el objeto vivo en memoria todo el ciclo del componente.

## 6. Field injection en clases del sistema

> Fuente: [developer.android.com/training/dependency-injection/hilt-android#android-classes](https://developer.android.com/training/dependency-injection/hilt-android)

`Application`, `Activity`, etc. los instancia Android, no Hilt: no podemos usar
su constructor. En clases anotadas (`@HiltAndroidApp`, `@AndroidEntryPoint`)
Hilt rellena los campos `@Inject` antes de `onCreate()`:

```kotlin
// HolaAndroidApp.kt
@HiltAndroidApp
class HolaAndroidApp : Application() {
    @Inject lateinit var inicializadores: Set<@JvmSuppressWildcards Inicializador>
    override fun onCreate() { super.onCreate(); inicializadores.forEach(...) }
}
```

`@AndroidEntryPoint` soporta: Activity, Fragment, View, Service y
BroadcastReceiver (fuente: misma página, sección *Supported Android classes*).

## 7. Multibindings: `@IntoSet` / `@IntoMap`

> Fuente: [dagger.dev/dev-guide/multibindings](https://dagger.dev/dev-guide/multibindings.html)

Varios módulos pueden aportar elementos a una colección común. En el proyecto,
dos `Inicializador` se agregan a un `Set` (`di/InicializadoresModule.kt` con
`@Binds @IntoSet`) y la `Application` los ejecuta todos sin conocerlos. Añadir
un tercero = una línea nueva en el módulo; nada más cambia.

Detalle Kotlin: al inyectar la colección hace falta
`Set<@JvmSuppressWildcards Inicializador>` — sin esa anotación los genéricos de
Kotlin compilan a `Set<? extends Inicializador>` y Hilt no encuentra el binding
(fuente: [dagger.dev/dev-guide/faq](https://dagger.dev/dev-guide/faq.html)).

`@IntoMap` funciona igual pero con clave (`@StringKey`, `@ClassKey`…); es la
base de cosas como las factorías de *workers* de WorkManager.

## 8. `@HiltViewModel`: integración con Jetpack

> Fuente: [developer.android.com/training/dependency-injection/hilt-jetpack](https://developer.android.com/training/dependency-injection/hilt-jetpack)

```kotlin
// ui/SaludoViewModel.kt
@HiltViewModel
class SaludoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,   // binding por defecto del ViewModelComponent
    @SaludoFormal private val generadorFormal: GeneradorSaludo,
    ...
) : ViewModel()
```

Y en una Activity `@AndroidEntryPoint`, la delegación estándar ya usa la
factoría de Hilt — no hay que hacer nada más:

```kotlin
private val viewModel: SaludoViewModel by viewModels()
```

Para Compose con Navigation se usa `hiltViewModel()`. Ojo a un cambio reciente
del artefacto que lo contiene (verificado en Google Maven el 18-07-2026, ambos
en versión estable 1.4.0):

- Antes: `androidx.hilt:hilt-navigation-compose`
- Ahora (documentación actual): `androidx.hilt:hilt-lifecycle-viewmodel-compose`

En esta app, con una sola Activity y sin Navigation, `by viewModels()` es
suficiente y no requiere dependencias extra.

`@ViewModelScoped` existe para dependencias compartidas *dentro de un mismo
ViewModel*; para compartir entre ViewModels se usa `@ActivityRetainedScoped` o
`@Singleton` (fuente: misma página).

## 9. Inyección asistida: mezclar grafo + datos de runtime

> Fuente: [dagger.dev/dev-guide/assisted-injection](https://dagger.dev/dev-guide/assisted-injection.html)

Para clases donde parte de los parámetros vienen del grafo y parte los pone el
llamante (un id, un nombre, una config...):

```kotlin
// data/ComponedorTarjeta.kt
class ComponedorTarjeta @AssistedInject constructor(
    private val contadorRepository: ContadorRepository,  // la pone Hilt
    @Assisted private val nombre: String                 // la pone el llamante
) { ... }

@AssistedFactory
interface ComponedorTarjetaFactory {
    fun crear(nombre: String): ComponedorTarjeta
}
```

Se inyecta la **factoría** (nunca la clase directamente) y se llama
`tarjetaFactory.crear(nombre)` — ver `ui/SaludoViewModel.kt`. Limitaciones
oficiales: las clases `@AssistedInject` no admiten scope, y si hay dos
parámetros asistidos del mismo tipo se desambiguan con `@Assisted("etiqueta")`.

## 10. `@EntryPoint`: acceder al grafo desde donde Hilt no llega

> Fuente: [developer.android.com/training/dependency-injection/hilt-android#not-supported](https://developer.android.com/training/dependency-injection/hilt-android) y
> [dagger.dev/hilt/entry-points](https://dagger.dev/hilt/entry-points.html)

Un `object` de Kotlin, un `ContentProvider` o una librería sin Hilt no pueden
usar `@Inject`. Para esos casos se define una interfaz `@EntryPoint`:

```kotlin
// di/DiagnosticoEntryPoint.kt
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DiagnosticoEntryPoint {
    fun contadorRepository(): ContadorRepository
}

// acceso:
EntryPointAccessors.fromApplication(context, DiagnosticoEntryPoint::class.java)
```

Es una puerta de escape: si te ves usándolo a menudo, revisa el diseño. En el
proyecto se usa una vez, en `MainActivity.onCreate()`, para loguear el contador.

## 11. `Lazy<T>` y `Provider<T>` (mención, sin ejemplo en el proyecto)

> Fuente: [dagger.dev/api/latest/dagger/Lazy.html](https://dagger.dev/api/latest/dagger/Lazy.html)

Cualquier binding `T` puede inyectarse también como:

- `dagger.Lazy<T>` — no se crea hasta el primer `.get()`; útil si la creación
  es cara y quizá no se use.
- `javax.inject.Provider<T>` — cada `.get()` evalúa el binding otra vez (con un
  binding sin scope, eso significa instancia nueva cada vez).

```kotlin
class Ejemplo @Inject constructor(
    private val caro: dagger.Lazy<ParserEnorme>,      // se crea al usarlo
    private val fabrica: Provider<ElementoLista>      // uno nuevo por .get()
)
```

## 12. Mapa de archivos: qué concepto vive en cada sitio

> Nota: en la lección 05 el proyecto se reorganizó en capas (data/domain/ui);
> las rutas de esta tabla son las actuales.

| Archivo | Concepto que demuestra |
|---|---|
| `data/repository/DefaultContadorRepository.kt` | `@Inject constructor` + `@Singleton` + estado compartido |
| `domain/saludo/GeneradorSaludo.kt` | interfaz con 2 implementaciones inyectables |
| `domain/saludo/ComponedorTarjeta.kt` | `@AssistedInject` / `@Assisted` / `@AssistedFactory` |
| `core/inicializacion/Inicializadores.kt` | contribuciones a un multibinding |
| `di/Cualificadores.kt` | `@Qualifier` propios |
| `di/AppModule.kt` | `@Provides`, `@ApplicationContext`, con/sin scope |
| `di/SaludoModule.kt` | `@Binds` + cualificadores + módulo en `ViewModelComponent` |
| `di/RepositorioModule.kt` | `@Binds` contrato de repositorio → implementación |
| `di/InicializadoresModule.kt` | `@IntoSet` |
| `di/DiagnosticoEntryPoint.kt` | `@EntryPoint` + `EntryPointAccessors` |
| `ui/saludo/SaludoViewModel.kt` | `@HiltViewModel`, `SavedStateHandle`, factoría asistida |
| `HolaAndroidApp.kt` | field injection + `@JvmSuppressWildcards` |
| `MainActivity.kt` | `by viewModels()` con Hilt, uso del EntryPoint |

## 13. Verificar

```bash
cd HolaAndroid
./gradlew assembleDebug     # si compila, el grafo entero valida
./gradlew installDebug      # con emulador o móvil conectado
adb logcat -s HolaAndroid   # verás los inicializadores y el contador
```

Comportamiento esperado en pantalla: el saludo cambia de tono con el botón, el
contador aumenta con "Enviar saludo" y **persiste tras cerrar y reabrir la
app** (SharedPreferences vía el singleton).

## 14. Errores típicos al usar Hilt

| Error de compilación / síntoma | Causa |
|---|---|
| `[Dagger/MissingBinding] X cannot be provided` | Nadie provee `X`: falta `@Inject`, `@Binds` o `@Provides`, o está en un componente que no ve el solicitante |
| `[Dagger/DuplicateBindings]` | Dos bindings del mismo tipo sin cualificador |
| `[Dagger/DependencyCycle]` | A necesita B y B necesita A: rompe el ciclo con `Lazy`/`Provider` o rediseña |
| `@Singleton` en módulo de `ViewModelComponent` | El scope no corresponde al componente del módulo |
| `MissingBinding` de `Set<Inicializador>` solo desde Kotlin | Falta `@JvmSuppressWildcards` en el punto de inyección |
| Inyectar `ComponedorTarjeta` directamente | Las clases asistidas no se inyectan: inyecta su `@AssistedFactory` |
| `lateinit property ... has not been initialized` | Usar un campo `@Inject` antes de `super.onCreate()` |

## Fuentes consultadas (18-07-2026)

- Guía principal de Hilt: <https://developer.android.com/training/dependency-injection/hilt-android>
- Jerarquía de componentes: <https://dagger.dev/hilt/components.html>
- Hilt + ViewModel/Jetpack: <https://developer.android.com/training/dependency-injection/hilt-jetpack>
- Inyección asistida: <https://dagger.dev/dev-guide/assisted-injection.html>
- Multibindings: <https://dagger.dev/dev-guide/multibindings.html>
- Entry points: <https://dagger.dev/hilt/entry-points.html>
- Versiones de `androidx.hilt` verificadas en Google Maven: `https://dl.google.com/android/maven2/androidx/hilt/<artefacto>/maven-metadata.xml`

Próxima lección natural: **testing con Hilt** (`hilt-android-testing`,
`@HiltAndroidTest`, reemplazar módulos con `@TestInstallIn`).
