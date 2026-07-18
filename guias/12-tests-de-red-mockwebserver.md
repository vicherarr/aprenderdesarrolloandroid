# Guía 12 — Tests de red con MockWebServer: el stack de Retrofit, de verdad

Objetivo: cerrar el hueco de cobertura que las guías 10 y 11 dejaron a
sabiendas — los fakes sustituían a `FrasesApi`, así que **nada testeaba el
stack real** de Retrofit + conversor + kotlinx.serialization. MockWebServer
levanta un servidor HTTP de verdad dentro del test (JVM, sin emulador) y
Retrofit le habla como hablaría a dummyjson.com.

Tercera entrega de la serie de tests (10: fundamentos; 11: ViewModel/Flow).

---

## 1. El hueco: "todo verde" no significa "todo probado"

La suite de 19 tests estaba verde y sin embargo ninguna de estas preguntas
tenía respuesta automática:

- ¿El `@SerialName` mapea `quote`→`texto` de verdad?
- ¿`ignoreUnknownKeys` sigue puesto en `RedModule`?
- ¿Qué llega al dominio cuando el servidor devuelve un 500? ¿Y un JSON al
  que le falta un campo?

Los errores de la tabla de la guía 06 (`MissingFieldException`,
`HttpException`...) estaban **documentados pero no protegidos**. Un fake de
`FrasesApi` se salta justo la parte que falla en producción.

## 2. La pieza y su versión

| Librería | Versión | Cómo se decidió |
|---|---|---|
| `com.squareup.okhttp3:mockwebserver` | **4.12.0** | lockstep con OkHttp: MISMA versión que el cliente, siempre (como el conversor con Retrofit, guía 06) |

MockWebServer (del equipo de OkHttp) es un servidor HTTP real que corre en
el test: se encolan respuestas, se le apunta el `baseUrl` y se inspeccionan
las peticiones recibidas. La guía 06 §8.7 avisó de que `BASE_URL` debía ser
sustituible — hoy se cobra.

## 3. El montaje: Retrofit de producción, servidor de test

```kotlin
@Before
fun preparar() {
    servidor = MockWebServer()
    servidor.start()
    api = Retrofit.Builder()
        .baseUrl(servidor.url("/"))   // ← en vez de dummyjson.com
        .client(OkHttpClient())
        .addConverterFactory(
            RedModule.proveeJson().asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(FrasesApi::class.java)
}

@After
fun apagar() = servidor.shutdown()
```

El detalle que hace honestos estos tests: **el `Json` es el de producción**.
`RedModule` es un `object` con `@Provides` públicos, así que
`RedModule.proveeJson()` se llama directamente — el test ejercita la
configuración real (`ignoreUnknownKeys`), no una copia que podría divergir.
`start()`/`shutdown()` en `@Before`/`@After`: cada test estrena servidor.

## 4. Los seis tests: respuesta, petición y capas pegadas

**El contrato de deserialización** — el JSON con la forma real de la API:

```kotlin
encola("""{"id":254,"quote":"Do not sit and wait.","author":"Rumi"}""")

val dto = api.obtenerFraseAleatoria()

assertEquals(FraseDto(id = 254, texto = "Do not sit and wait.", autor = "Rumi"), dto)
assertEquals("/quotes/random", servidor.takeRequest().path)
```

La segunda aserción es la novedad conceptual: **también se afirma sobre la
petición**. `takeRequest()` devuelve lo que el servidor recibió — la ruta
que construyó la anotación `@GET`. En APIs con `@Query`/`@Body`, aquí se
verifica que enviamos lo correcto.

**Los tres contratos de error**, por fin ejecutables:

| Test | Respuesta encolada | Qué afirma |
|---|---|---|
| campos desconocidos | JSON con `"categoria"`, `"likes"` extra | parsea igual (protege `ignoreUnknownKeys`) |
| campo que falta | JSON sin `"author"` | `SerializationException` |
| error del servidor | código 500 | `HttpException` con `code() == 500` |

**Y las capas pegadas** — integración servidor→Retrofit→repositorio→caso de
uso, con el `FakeFraseDao` promocionado a `testutil/` (segunda promoción de
la serie):

```kotlin
encola("error", codigo = 500)
val useCase = ObtenerFraseUseCase(DefaultFraseRepository(api, FakeFraseDao()))

assertTrue(useCase().exceptionOrNull() is HttpException)
```

El 500 del servidor atraviesa todo y llega al dominio como el
`Result.failure` que la UI convierte en "¿Hay conexión?" (guías 06 y 11).
El camino feliz, simétrico: del JSON encolado a la frase persistida en el
DAO (guía 07).

## 5. Calibración: romper, rojo, verde

Método de las guías 10 y 11 aplicado a la configuración: se borró
`ignoreUnknownKeys = true` de `RedModule.proveeJson()` y:

```
FrasesApiTest > campos desconocidos no rompen el parseo (protege ignoreUnknownKeys) FAILED
    kotlinx.serialization.json.internal.JsonDecodingException

6 tests completed, 1 failed
```

Rojo en el único test que vigila esa línea, los otros cinco verdes.
Restaurada, verde. Ese `Json {}` de la guía 06 ya no es una convención:
es un contrato con guardián.

## 6. Errores típicos testeando red

| Hábito | Problema |
|---|---|
| Probar el parseo llamando a `Json.decodeFromString` a secas | no pasa por Retrofit ni el conversor: media tubería sin probar |
| Construir en el test un `Json` "igual que el de producción" | divergen en silencio: llama al proveedor real del módulo |
| Olvidar `shutdown()` | sockets abiertos que se acumulan entre tests |
| Encolar una respuesta para dos peticiones | la segunda se queda esperando: una respuesta POR petición |
| Testear contra la API real (dummyjson.com) | lento, frágil y no determinista: la red real no entra en un test unitario |
| No afirmar nunca sobre `takeRequest()` | pruebas lo que recibes pero no lo que ENVÍAS |

## 7. Verificar

```bash
cd HolaAndroid && ./gradlew testDebugUnitTest
```

25 tests (19 previos + 6 de red), siguen siendo segundos en la JVM: un
servidor HTTP local arranca en milisegundos. Informe en
`app/build/reports/tests/testDebugUnitTest/index.html`.

## Fuentes consultadas (18-07-2026)

- MockWebServer (README oficial): <https://github.com/square/okhttp/tree/master/mockwebserver>
- Versión: lockstep con OkHttp en Maven Central (`com.squareup.okhttp3`)
- Manejo de errores HTTP en Retrofit (`HttpException`): <https://square.github.io/retrofit/>
- kotlinx.serialization: excepciones de deserialización: <https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md>

Siguiente de la serie (guía 13): **tests instrumentados** — Room real en
memoria en el dispositivo y `MigrationTestHelper` reproduciendo la
migración 1→2 de la guía 08 contra los JSON de `app/schemas/`.
