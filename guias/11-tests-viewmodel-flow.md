# Guía 11 — Tests de ViewModel y Flow: Turbine y el dispatcher de Main

Objetivo: testear `SaludoViewModel` entero en la JVM — el `combine` de
cuatro flujos, los estados `Cargando → Exito/Error` y la reactividad
datos→UI — sin emulador, sin Hilt y sin Android. Segunda entrega de la
serie de tests (fundamentos: guía 10).

---

## 1. Los dos problemas nuevos que trae un ViewModel

Un ViewModel no es una clase más; para testearlo hay que resolver dos
cosas que el dominio no tenía:

1. **`viewModelScope` vive en `Dispatchers.Main`** — el hilo de UI de
   Android, que **no existe** en la JVM de tests. Sin arreglarlo:
   `Module with the Main dispatcher had failed to initialize`.
2. **`uiState` es un `StateFlow` perezoso y conflacionado** —
   `stateIn(WhileSubscribed)` no arranca el `combine` hasta que alguien
   colecciona, y un `StateFlow` puede saltarse estados intermedios si
   nadie los lee a tiempo (conflación). Afirmar sobre `uiState.value` a
   secas o no ver nunca el `Cargando` son las dos trampas clásicas.

## 2. Piezas y versiones (verificadas 18-07-2026)

| Librería | Versión | Cómo se decidió |
|---|---|---|
| `app.cash.turbine:turbine` | **1.1.0** | ⚠️ NO la última (1.2.0): su POM en Maven Central declara coroutines 1.8.0 — nuestra serie. La 1.2.0 arrastra la 1.9.0 |

Turbine (de Cash App) es el estándar para testear flujos: convierte un
`Flow` en una cola de emisiones que se consumen una a una con `awaitItem()`
— y **falla el test** si llega una emisión que no esperabas o no llega la
que esperabas.

## 3. `MainDispatcherRule`: el Main de mentira (patrón oficial)

`testutil/MainDispatcherRule.kt` — una `TestWatcher` de JUnit que instala
un dispatcher de test como Main antes de cada test y lo restaura después:

```kotlin
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(testDispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

En el test, una línea:

```kotlin
@get:Rule
val mainDispatcherRule = MainDispatcherRule()
```

Es el patrón literal de la documentación oficial de testing de Android.
`UnconfinedTestDispatcher` ejecuta las corrutinas inmediatamente — cómodo
para ViewModels; su hermano `StandardTestDispatcher` encola y te obliga a
avanzar el tiempo a mano (más control, más ceremonia).

## 4. Construir el ViewModel sin Hilt (y qué dice eso de la arquitectura)

Hilt no pinta nada en un test unitario: el constructor es público y las
dependencias son interfaces. Casos de uso **reales** alimentados por los
fakes de la guía 10, promocionados a `testutil/`:

```kotlin
SaludoViewModel(
    savedStateHandle = SavedStateHandle(mapOf("nombre" to "Ada")),
    observarContador = ObservarContadorUseCase(contadorRepo),      // fake debajo
    obtenerFrase = ObtenerFraseUseCase(fraseRepo),                 // fake debajo
    tarjetaFactory = object : ComponedorTarjetaFactory {
        override fun crear(nombre: String) = ComponedorTarjeta(contadorRepo, nombre)
    },
    // ...
)
```

Decisiones con intención:

- **Casos de uso reales, fakes solo en la frontera de datos**: se testea la
  orquestación completa dominio+ViewModel. Mockear los casos de uso probaría
  un ViewModel de mentira.
- **`SavedStateHandle` de verdad** (funciona en la JVM): el test fija
  `nombre=Ada` como lo haría la navegación.
- **Regla de promoción de fakes**: `FakeFraseRepository` nació privado en el
  test del caso de uso (guía 10); cuando esta suite lo necesitó, se mudó a
  `testutil/Fakes.kt`. Ni antes ni después.

## 5. Turbine sobre el `uiState`

```kotlin
viewModel.uiState.test {
    val estado = expectMostRecentItem()      // lo último, ignorando intermedios
    assertEquals("formal:Ada", estado.saludo)

    viewModel.alternarTono()

    assertEquals("informal:Ada", awaitItem().saludo)   // la SIGUIENTE emisión
}
```

- **`test { }` colecciona**: eso arranca el `stateIn(WhileSubscribed)` y el
  `combine` — igual que cuando la pantalla aparece. Sin coleccionista el
  ViewModel está dormido, también en el test.
- **`expectMostRecentItem()`** para estados conflacionados ("dame donde
  estás"); **`awaitItem()`** para exigir la siguiente emisión ("esto DEBE
  emitir"). Elegir mal entre ambos es la fuente número uno de tests de Flow
  inestables.
- Al salir del bloque, Turbine cancela la colección y **falla si quedaron
  emisiones sin consumir** — un `combine` que emite de más también es un bug.

## 6. El fake "con puerta": hacer visible el `Cargando`

Con un dispatcher Unconfined, la carga entera (petición al fake + éxito)
ocurre de un tirón: `Cargando` sería sobrescrito por `Exito` antes de que
nadie lo lea. La solución profesional es un fake cuya suspensión controla
el test:

```kotlin
val puerta = CompletableDeferred<Frase>()
fraseRepo.alPedirFrase = { puerta.await() }      // se queda suspendido AQUÍ

viewModel.uiState.test {
    expectMostRecentItem()

    viewModel.cargarFrase()
    assertEquals(FraseUiState.Cargando, awaitItem().frase)   // congelado en la puerta

    puerta.complete(Frase(id = 1, texto = "texto", autor = "autora"))
    assertEquals(FraseUiState.Exito("texto", "autora"), awaitItem().frase)
}
```

El test controla el tiempo del mundo: primero afirma el estado intermedio
con la corrutina suspendida, luego abre la puerta y afirma el final. Sin
puerta este test sería una lotería de conflación.

### 6.1 Calibración: romper, rojo, verde

Como en la guía 10, el test se calibró rompiendo el código: se quitó la
línea `fraseState.value = Cargando` del ViewModel y:

```
SaludoViewModelTest > cargar una frase pasa por Cargando y termina en Exito FAILED
    app.cash.turbine.TurbineAssertionError
        Caused by: TurbineTimeoutCancellationException
```

Turbine agotó el tiempo esperando una emisión que ya no existe. Restaurada
la línea, verde. El test protege exactamente el spinner que el usuario ve.

## 7. Los otros contratos que la suite fija

- **El error es de UI, no de excepción**: el estado `Error` lleva el mensaje
  para humanos ("¿Hay conexión?"), no el `IOException("sin red")` del fake.
  Que la excepción no se filtre a la pantalla también es contrato.
- **Reactividad sin acción del usuario**: escribir en el `StateFlow` del
  fake de datos (`fraseRepo.guardadas.value = ...`) hace emitir al
  `uiState` — el camino Room→repositorio→combine→UI de la guía 07, probado
  en milisegundos.
- **El contador atraviesa el combine**: `enviarSaludo()` incrementa el fake
  y estado Y tarjeta reflejan el 1 en la misma emisión.

## 8. Errores típicos testeando ViewModels y Flows

| Hábito | Problema |
|---|---|
| Olvidar `MainDispatcherRule` | `Main dispatcher had failed to initialize` al primer `viewModelScope.launch` |
| Afirmar `uiState.value` sin coleccionar | con `WhileSubscribed`, el flujo ni ha arrancado: ves el `initialValue` siempre |
| `awaitItem()` donde el estado conflaciona | test inestable: a veces llega el intermedio, a veces no (usa `expectMostRecentItem`) |
| Probar `Cargando` sin puerta en el fake | lotería: la carga instantánea lo pisa antes de leerlo |
| Mockear los casos de uso del ViewModel | pruebas un ViewModel de mentira; con fakes en la frontera pruebas la orquestación real |
| `Thread.sleep` / `delay` "para que dé tiempo" | lento y frágil: Turbine espera emisiones, no relojes |

## 9. Verificar

```bash
cd HolaAndroid && ./gradlew testDebugUnitTest
```

19 tests (13 de la guía 10 + 6 del ViewModel), ~3 segundos en la JVM.
Informe en `app/build/reports/tests/testDebugUnitTest/index.html`.

## Fuentes consultadas (18-07-2026)

- Testear corrutinas y el patrón MainDispatcherRule (oficial): <https://developer.android.com/kotlin/coroutines/test>
- Testear StateFlow/stateIn (oficial): <https://developer.android.com/kotlin/flow/test>
- Turbine (README y API): <https://github.com/cashapp/turbine>
- Versión de Turbine: POM de `turbine-jvm` en Maven Central (dependencia de coroutines por versión)
- UnconfinedTestDispatcher vs StandardTestDispatcher: <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/>

Siguientes de la serie: **tests instrumentados** (Room en memoria en el
dispositivo y `MigrationTestHelper` con los JSON de `schemas/`, guía 08) y
**red con MockWebServer** (guía 06).
