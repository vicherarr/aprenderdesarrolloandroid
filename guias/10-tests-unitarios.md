# Guía 10 — Tests unitarios: la arquitectura, por fin, se cobra

Objetivo: primera suite de tests del proyecto — 13 tests unitarios sobre el
dominio y la capa de datos, ejecutados en la JVM en ~2 segundos, sin
emulador. Esta guía cubre los cimientos (qué es un test unitario, dobles de
prueba, corrutinas en tests, ver un test fallar); las siguientes irán a por
ViewModel/Flow, los tests instrumentados (Room y migraciones) y la red con
MockWebServer.

---

## 1. El mapa: dónde vive cada tipo de test

| Carpeta | Corre en | Se llama | Velocidad |
|---|---|---|---|
| `app/src/test/` | tu JVM (sin Android) | test unitario / local | milisegundos |
| `app/src/androidTest/` | dispositivo o emulador | test instrumentado | segundos-minutos |

La **pirámide de tests** oficial: muchos unitarios (baratos, rápidos, uno
por comportamiento), menos instrumentados, poquísimos de UI end-to-end. Esta
guía es la base de la pirámide.

La regla para saber dónde va un test: si la clase no toca Android, va en
`test/`. Y aquí es donde **la arquitectura de las guías 05–08 se cobra**:
dominio y repositorios dependen de interfaces y no importan nada de
`android.*`, así que casi toda la lógica de la app se prueba en la JVM.

## 2. Piezas y versiones (verificadas 18-07-2026)

| Librería | Versión | Cómo se decidió |
|---|---|---|
| `junit:junit` | 4.13.2 | ya estaba (plantilla de la guía 02): el runner estándar |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | **1.8.1** | ⚠️ NO la última (1.10.2): la MISMA serie que las corrutinas del proyecto |

¿De dónde sale ese 1.8.1? El proyecto no declara corrutinas: llegan
transitivamente (lifecycle, Room...). Se averigua preguntando a Gradle:

```bash
./gradlew app:dependencies --configuration debugRuntimeClasspath \
    | grep kotlinx-coroutines-core     # → 1.7.3 -> 1.8.1
```

Misma lección de versionado que llevamos desde la guía 03: la versión
correcta no es la última, es **la que casa con tu runtime**.

## 3. Anatomía de un test

```kotlin
@Test
fun `por la manana saluda con buenos dias`() {
    val generador = GeneradorSaludoFormal(calendarioALas(8))   // given
    val saludo = generador.saludar("Ada")                      // when
    assertEquals("Buenos días, Ada.", saludo)                  // then
}
```

- **Given/When/Then**: preparar, ejecutar, comprobar. Un test = UN
  comportamiento; si necesitas "y además...", probablemente son dos tests.
- **Nombres con backticks y en frase**: el informe de un test roto debe
  leerse como la especificación que se ha violado, no como `testSaludo3`.
- Se afirma sobre **comportamiento observable** (lo que devuelve, lo que
  queda guardado), no sobre implementación (qué métodos llamó por dentro):
  los tests de comportamiento sobreviven a los refactors; los de
  implementación se rompen con cada cambio inocente.

### 3.1 El primer dividendo: inyectar dependencias = controlar el mundo

`GeneradorSaludoFormal` recibe el `Calendar` por constructor desde la guía
04. El test fabrica "las 8 de la mañana" y el resultado es determinista:

```kotlin
private fun calendarioALas(hora: Int): Calendar =
    Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hora) }
```

Con `Calendar.getInstance()` *dentro* de la clase, el test pasaría por la
mañana y fallaría por la noche. Todo lo no determinista (hora, aleatorio,
red) debe entrar por el constructor — y los tests de los **bordes** de las
franjas (6, 12, 21) existen porque los límites de un `when` con rangos son
donde viven los bugs.

## 4. Dobles de prueba: fakes escritos a mano

Para probar una clase en aislamiento, sus dependencias se sustituyen por
**dobles**. El vocabulario útil:

| Doble | Qué hace |
|---|---|
| **Stub** | devuelve respuestas fijas ("cuando te pidan la frase, da esta") |
| **Fake** | implementación real simplificada (una "tabla" en memoria) |
| **Mock** | grabadora de llamadas para verificar interacciones |

La guía oficial de testing de Android recomienda **fakes a mano antes que
librerías de mocks** cuando hay interfaces disponibles — y las tenemos
todas, gracias a la arquitectura. Dos ejemplos de la suite:

```kotlin
// Stub anónimo: basta para probar la ELECCIÓN de generador
private val formal = object : GeneradorSaludo {
    override fun saludar(nombre: String) = "formal:$nombre"
}

// Fake con estado: reproduce el contrato del DAO (¡incluido el upsert!)
private class FakeFraseDao : FraseDao {
    private val tabla = MutableStateFlow<List<FraseEntity>>(emptyList())
    override suspend fun insertar(frase: FraseEntity) {
        tabla.value = tabla.value.filterNot { it.id == frase.id } + frase
    }
    override fun observarTodas(): Flow<List<FraseEntity>> = tabla
    // ...
}
```

MockK/Mockito existen y tienen su sitio (interfaces enormes de terceros,
verificación de interacciones); mientras un fake de diez líneas baste, el
fake gana: no hay DSL que aprender y el test se lee solo.

## 5. Corrutinas en tests: `runTest`

Las funciones `suspend` no se pueden llamar desde un test normal. `runTest`
(de kotlinx-coroutines-test) crea un entorno de corrutinas de test:

```kotlin
@Test
fun `si el repositorio responde, exito con la frase`() = runTest {
    val useCase = ObtenerFraseUseCase(FakeFraseRepository { frase })
    assertEquals(Result.success(frase), useCase())
}
```

Tres cosas que hace y conviene saber:

1. **Tiempo virtual**: los `delay()` se saltan — un retry con backoff de 30
   segundos se testea en milisegundos.
2. **Vigila corrutinas huérfanas**: si el código lanza una corrutina que
   nadie espera y falla, el test falla (no se pierde en silencio).
3. Es el sustituto moderno de `runBlocking` en tests: `runBlocking` espera
   los delays de verdad y no vigila nada.

## 6. Los tests que protegen los matices de guías anteriores

La suite no prueba "que funciona": prueba los **contratos** que las guías
anteriores establecieron.

**El `Result` del caso de uso (guía 06)** — éxito y fallo:

```kotlin
val resultado = useCase()   // el fake lanza IOException("sin red")
assertTrue(resultado.isFailure)
assertTrue(resultado.exceptionOrNull() is IOException)
```

**La cancelación se relanza SIEMPRE (guía 06)** — el test estrella:

```kotlin
@Test(expected = CancellationException::class)
fun `la cancelacion NUNCA se convierte en Result, se relanza`() = runTest {
    val useCase = ObtenerFraseUseCase(
        FakeFraseRepository { throw CancellationException("cancelada") }
    )
    useCase()
}
```

**El upsert del repositorio (guía 07)** — pedir dos veces la misma frase no
la duplica. **El mapeo entidad→dominio con favorita (guía 08)** — alternar
favorita se refleja en el `Flow` de dominio.

### 6.1 Ver el test fallar: la prueba de que protege

Un test que nunca has visto rojo no se sabe si vigila algo. Rompimos el
caso de uso a propósito (quitamos el `catch (e: CancellationException)`,
dejando que el genérico se la trague) y se ejecutó la suite:

```
ObtenerFraseUseCaseTest > la cancelacion NUNCA se convierte en Result, se relanza FAILED
3 tests completed, 1 failed
```

Rojo exactamente donde debía, y los otros dos verdes (el comportamiento con
`IOException` no había cambiado). Restaurado el código, todo verde. Ese
ciclo — romper, ver rojo, arreglar, ver verde — es la forma de calibrar
cualquier test nuevo.

### 6.2 Un dividendo inesperado de la guía 09

`DefaultFraseRepository` llama a `Timber.d(...)`. En la JVM de tests no hay
árboles plantados → **no-op**, y el test pasa. Si el repositorio llamara a
`android.util.Log` directamente, el test moriría con el famoso
`Method d in android.util.Log not mocked`: la fachada de logging también
compró testabilidad.

## 7. Errores típicos en tests unitarios

| Hábito | Problema |
|---|---|
| Probar métodos privados / contar llamadas internas | test de implementación: se rompe con cada refactor sin que haya bug |
| `Calendar.getInstance()` (o `Random`, o red) dentro del código probado | test no determinista: pasa hoy, falla a las 21:01 |
| `runBlocking` en vez de `runTest` | los delays se esperan de verdad y las corrutinas huérfanas no se vigilan |
| Un solo test con veinte asserts | cuando falla no sabes QUÉ comportamiento murió |
| Mockear el mundo entero por costumbre | con interfaces propias, un fake de 10 líneas se lee mejor y no miente |
| No haber visto nunca el test en rojo | quizá no protege nada (§6.1) |

## 8. Verificar

```bash
cd HolaAndroid
./gradlew testDebugUnitTest        # (test a secas lo ejecuta en debug Y release)
```

13 tests, ~2 segundos, sin emulador. El informe navegable queda en
`app/build/reports/tests/testDebugUnitTest/index.html` y el detalle XML en
`app/build/test-results/`. Para ejecutar una sola clase:

```bash
./gradlew testDebugUnitTest --tests "*ObtenerFraseUseCaseTest*"
```

## Fuentes consultadas (18-07-2026)

- Fundamentos de testing en Android (oficial): <https://developer.android.com/training/testing/fundamentals>
- Tests locales (unitarios) en la JVM: <https://developer.android.com/training/testing/local-tests>
- Dobles de prueba y la recomendación de fakes: <https://developer.android.com/training/testing/fundamentals/test-doubles>
- kotlinx-coroutines-test (README oficial, `runTest`): <https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test>
- Qué testear (pirámide y alcance): <https://developer.android.com/training/testing/fundamentals/what-to-test>
- Versión de coroutines-test: resolución real del proyecto vía `gradlew app:dependencies`

Próximas lecciones de la serie de tests: **ViewModel y Flow** (dispatcher
de Main en tests, `SavedStateHandle`, Turbine), **instrumentados** (Room en
memoria y `MigrationTestHelper` con los JSON de `schemas/`, guía 08) y
**red** (MockWebServer, apuntado en la guía 06).
