# Guía 06 — Añadir Retrofit correctamente y usarlo

Objetivo: consumir una API REST real ("frase del día" desde
`https://dummyjson.com/quotes/random`) integrándola en la arquitectura de la
lección 05: Retrofit como **fuente de datos remota**, DTO mapeado a modelo de
dominio, `Result` en el caso de uso, y estados de carga/éxito/error en la UI.

---

## 1. Elegir las piezas y sus versiones (metodología de la guía 03)

### 1.1 Qué piezas forman un stack de red moderno

| Pieza | Papel | Elección |
|---|---|---|
| Cliente HTTP | conexiones, caché, interceptores | OkHttp (lo trae Retrofit) |
| Cliente REST | convierte una interfaz Kotlin en llamadas HTTP | Retrofit |
| Serializador JSON | JSON ↔ objetos Kotlin | kotlinx.serialization |
| Conversor | conecta Retrofit con el serializador | `converter-kotlinx-serialization` (oficial de Retrofit) |

¿Por qué kotlinx.serialization y no Gson/Moshi? Es la librería oficial de
JetBrains para Kotlin, valida la nulabilidad en tiempo de compilación (Gson
puede meter `null` en un campo no-nulo vía reflexión) y es la usada por el
ejemplo oficial Now in Android.

### 1.2 Versiones verificadas (18-07-2026)

> Fuentes: README oficial de Retrofit
> (<https://github.com/square/retrofit>) y Maven Central (API de búsqueda).

| Librería | Versión | Cómo se decidió |
|---|---|---|
| `com.squareup.retrofit2:retrofit` | **3.0.0** | última estable (mayo 2025); requiere Java 8+ / API 21+ |
| `com.squareup.retrofit2:converter-kotlinx-serialization` | **3.0.0** | misma versión que Retrofit, siempre |
| `com.squareup.okhttp3:logging-interceptor` | **4.12.0** | Maven marca "latest" la `5.0.0-alpha.16`; **una alpha no va a producción** → última estable de la serie 4 |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | **1.8.1** | la 1.9.0 está compilada con Kotlin 2.2; nuestro proyecto usa 2.1.21 → se elige la última construida con nuestra serie |
| plugin `org.jetbrains.kotlin.plugin.serialization` | = versión de Kotlin | es un plugin del compilador: SIEMPRE la misma versión que Kotlin |

Dos lecciones de versionado nuevas respecto a la guía 03:

1. **"Latest" en Maven no significa "estable"**: hay que mirar el sufijo
   (`-alpha`, `-beta`, `-RC`) y quedarse con la última sin sufijo.
2. **Las librerías kotlinx van ligadas a la serie de Kotlin** con la que se
   compilaron; el plugin de serialización va ligado exactamente.

### 1.3 La API elegida

`https://dummyjson.com/quotes/random` — pública, sin autenticación, respuesta
mínima (verificada con `curl` antes de escribir código):

```json
{"id":254,"quote":"Don't sit and wait...","author":"Rumi"}
```

## 2. Configuración del proyecto

### 2.1 Permiso de Internet (el paso que siempre se olvida)

`AndroidManifest.xml`, antes de `<application>`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Sin él, toda petición falla con `SecurityException`. Es un permiso "normal":
se concede en la instalación, sin diálogo. Nota: Android bloquea HTTP en claro
por defecto (*cleartext*); usamos HTTPS, que es lo correcto.

### 2.2 Plugin y dependencias

`libs.versions.toml` + plugin `kotlin-serialization` en raíz (`apply false`) y
en `app`. Dependencias nuevas en `app/build.gradle.kts`:

```kotlin
implementation(libs.retrofit)
implementation(libs.retrofit.converter.kotlinx.serialization)
implementation(libs.okhttp.logging.interceptor)
implementation(libs.kotlinx.serialization.json)
```

## 3. El stack de red en la capa de datos

### 3.1 El DTO calca el JSON (`data/remote/FraseDto.kt`)

```kotlin
@Serializable
data class FraseDto(
    val id: Int,
    @SerialName("quote") val texto: String,
    @SerialName("author") val autor: String
)
```

`@SerialName` desacopla el nombre del campo JSON del nombre Kotlin: la API
habla inglés, nuestro código no tiene por qué.

### 3.2 La interfaz de la API (`data/remote/FrasesApi.kt`)

> Fuente: <https://github.com/square/retrofit> (declaración de interfaces y
> soporte suspend)

```kotlin
interface FrasesApi {
    @GET("quotes/random")
    suspend fun obtenerFraseAleatoria(): FraseDto
}
```

Solo se declara la interfaz: **Retrofit genera la implementación**. Cada
función es un endpoint; con `suspend`, Retrofit ejecuta la petición fuera del
hilo principal (main-safe) y reanuda la corrutina con el resultado. Otras
anotaciones del mismo estilo: `@POST`, `@Path("id")`, `@Query("q")`, `@Body`.

### 3.3 El módulo de red (`di/RedModule.kt`)

Todo el stack se construye una vez (`@Singleton`) — OkHttp debe compartir pool
de conexiones — y con `@Provides` porque son clases de terceros (guía 04):

```kotlin
@Provides @Singleton
fun proveeRetrofit(cliente: OkHttpClient, json: Json): Retrofit =
    Retrofit.Builder()
        .baseUrl("https://dummyjson.com/")     // SIEMPRE acaba en /
        .client(cliente)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
```

Detalles que importan:

- `baseUrl` **debe terminar en `/`** (Retrofit lanza excepción si no) y los
  endpoints **no deben empezar** por `/`.
- `Json { ignoreUnknownKeys = true }`: si la API añade campos mañana, la app
  no rompe. Sin esto, un campo desconocido = excepción.
- `HttpLoggingInterceptor` en nivel `BASIC` deja ver cada petición en Logcat.
  En una app real: nivel `BODY` solo en builds debug, nunca en release (los
  cuerpos pueden contener tokens y datos personales).

### 3.4 El repositorio traduce DTO → dominio (`data/repository/`)

```kotlin
override suspend fun obtenerFraseAleatoria(): Frase =
    api.obtenerFraseAleatoria().aDominio()

private fun FraseDto.aDominio() = Frase(texto = texto, autor = autor)
```

Es la recomendación "modelo por capa" de la guía de arquitectura
(<https://developer.android.com/topic/architecture/recommendations>): el `id`
y los `@SerialName` **no salen de la capa de datos**. Si la API cambia su
JSON, solo se tocan DTO y mapper.

## 4. El dominio convierte el fallo en un valor: `Result`

Red = fallos garantizados (sin conexión, timeout, 500...). El caso de uso los
convierte en un `Result` explícito para que el ViewModel no necesite try/catch:

```kotlin
suspend operator fun invoke(): Result<Frase> =
    try {
        Result.success(fraseRepository.obtenerFraseAleatoria())
    } catch (e: CancellationException) {
        throw e                      // ⚠️ SIEMPRE se relanza
    } catch (e: Exception) {
        Result.failure(e)
    }
```

**El detalle del `CancellationException`** es de los que separan código
correcto de código que "casi siempre funciona": las corrutinas se cancelan
lanzando esa excepción por dentro. Si la capturas (cosa que `runCatching` hace
sin avisar), la cancelación estructurada se rompe y corrutinas muertas siguen
ejecutando código. Regla: atrapa excepciones concretas, y si atrapas genérico,
relanza la de cancelación.

## 5. La UI modela la carga como estados sellados

`ui/saludo/SaludoUiState.kt`:

```kotlin
sealed interface FraseUiState {
    data object Inicial : FraseUiState
    data object Cargando : FraseUiState
    data class Exito(val texto: String, val autor: String) : FraseUiState
    data class Error(val mensaje: String) : FraseUiState
}
```

Y el `when` de `FraseSeccion` (en `SaludoScreen.kt`) es **exhaustivo**: si
mañana añades un caso al sealed interface, la pantalla no compila hasta que
decidas cómo pintarlo. Los booleanos sueltos (`isLoading`, `hasError`...)
permiten estados imposibles (¿cargando y con error a la vez?); la jerarquía
sellada no.

El ViewModel lanza la petición con cancelación estructurada:

```kotlin
fun cargarFrase() {
    viewModelScope.launch {          // si la pantalla muere, la petición se cancela
        fraseState.value = FraseUiState.Cargando
        obtenerFrase().fold(
            onSuccess = { fraseState.value = FraseUiState.Exito(it.texto, it.autor) },
            onFailure = { fraseState.value = FraseUiState.Error("No se pudo cargar...") }
        )
    }
}
```

## 6. El viaje completo de la petición

Con la orientación de la guía 05 (datos arriba, usuario abajo):

```
INTERNET   dummyjson.com  ◄── GET /quotes/random ── OkHttp ── Retrofit
                                                                ▲    │ FraseDto
DATA       DefaultFraseRepository.obtenerFraseAleatoria() ──────┘    │
             (mapea FraseDto → Frase)                                ▼
DOMAIN     ObtenerFraseUseCase() ── try/catch ──► Result<Frase>
              ▲                                        │
UI         viewModel.cargarFrase()                     ▼
              ▲                    fraseState: Cargando → Exito/Error
           [Botón "Frase del día"]        └─► combine ─► uiState ─► recompone
```

## 7. Errores típicos con Retrofit

| Síntoma | Causa |
|---|---|
| `SecurityException: Permission denied (missing INTERNET permission?)` | falta el permiso en el Manifest |
| `IllegalArgumentException: baseUrl must end in /` | la baseUrl no acaba en `/` |
| `kotlinx.serialization.MissingFieldException` | el JSON trae menos campos que el DTO (haz el campo nullable o con valor por defecto) |
| `JsonDecodingException: Encountered an unknown key` | falta `ignoreUnknownKeys = true` en `Json {}` |
| `NetworkOnMainThreadException` | llamada síncrona (`.execute()`) en el hilo principal — con `suspend` no pasa |
| `CleartextNotPermittedException` | baseUrl con `http://`: usa HTTPS |
| La UI se queda "cargando" para siempre tras rotar | el estado de carga vivía en el composable y no en el ViewModel |

## 8. Verificar

```bash
cd HolaAndroid && ./gradlew installDebug
adb logcat -s okhttp.OkHttpClient   # se ve cada petición del interceptor
```

En la app: pulsa **"Frase del día"** → aparece el spinner → la frase con su
autor. En modo avión → el mensaje de error. Cada pulsación trae una frase
distinta (endpoint aleatorio).

## Fuentes consultadas (18-07-2026)

- Retrofit (README oficial, v3.0.0): <https://github.com/square/retrofit>
- Conversor kotlinx-serialization de Retrofit: <https://github.com/square/retrofit/tree/trunk/retrofit-converters/kotlinx-serialization>
- kotlinx.serialization (guía oficial): <https://github.com/Kotlin/kotlinx.serialization>
- OkHttp (interceptores/logging): <https://square.github.io/okhttp/features/interceptors/>
- Permiso de Internet: <https://developer.android.com/develop/connectivity/network-ops/connecting>
- Modelo por capa y capa de datos: <https://developer.android.com/topic/architecture/data-layer>
- Versiones: Maven Central (`search.maven.org`, consultas de la guía 03)

Próxima lección natural: **Room** (base de datos local) para cachear las
frases y verlas sin conexión — el repositorio pasaría a coordinar dos fuentes
de datos, que es su verdadera razón de ser.
