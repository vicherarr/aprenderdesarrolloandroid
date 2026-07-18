# Guía 22 — Logs profesionales: Logcat, Timber y un árbol por build

Objetivo: pasar de `Log.d(TAG, ...)` sueltos a un sistema de logging con un
único punto de control: en debug se ve todo con tags automáticos; en release
los logs de diagnóstico **desaparecen** y solo sobreviven avisos y errores
(los que en una app real irían a crash reporting). Todo verificado en el
emulador con los dos builds.

---

## 1. La base oficial: `android.util.Log` y Logcat

Android trae un sistema de log central (Logcat) y una API de plataforma con
cinco niveles. Usarlos con criterio ya es la mitad del profesionalismo:

| Nivel | Método | Cuándo |
|---|---|---|
| VERBOSE | `Log.v` | detalle extremo, casi nunca |
| DEBUG | `Log.d` | diagnóstico de desarrollo ("guardé la frase 165") |
| INFO | `Log.i` | hitos normales de la app |
| WARN | `Log.w` | fallo **esperado y manejado** (se cayó la red) |
| ERROR | `Log.e` | fallo que no debería haber pasado |

Y se consume con `adb logcat` (o la ventana Logcat de Android Studio):

```bash
adb logcat -s ObtenerFraseUseCase OkHttp   # solo estos tags
adb logcat "*:W"                           # solo WARN o peor
adb logcat -d > volcado.txt                # volcar y salir
```

### 1.1 Por qué `Log` a pelo no escala

1. **El TAG es artesanal**: cada clase repite su `companion object { TAG }`
   (así estaban nuestros `Inicializadores` desde la guía 04).
2. **No hay interruptor global**: `Log.d` imprime igual en debug que en el
   APK de producción. La guía oficial de seguridad es explícita: no loguear
   datos de usuario, y los logs en release son la fuga más tonta (cualquier
   app con adb puede leerlos en dispositivos antiguos, y en todos quedan en
   los volcados de errores).
3. **Sin punto de control**: no hay dónde enchufar "los errores, además, a
   Crashlytics" sin tocar cien llamadas.

## 2. La elección: Timber (versión verificada 18-07-2026)

| Librería | Versión | Cómo se decidió |
|---|---|---|
| `com.jakewharton.timber:timber` | **5.0.1** | última estable en Maven Central; es la MISMA que usan los architecture-samples oficiales de Google (verificado en su `libs.versions.toml`) |

No hay librería oficial de logging de apps en androidx; el estándar de
facto profesional es Timber: una fachada mínima sobre `Log` donde **las
llamadas no deciden nada** (`Timber.d(...)` y ya) y **los árboles plantados
deciden todo** (si se imprime, dónde y cómo). Google la usa en sus propios
ejemplos de arquitectura, que es la mejor pista de "oficialmente
recomendable" que existe para una lib de terceros.

## 3. Un árbol por tipo de build

### 3.1 Plantado en la Application — antes que nada

```kotlin
override fun onCreate() {
    super.onCreate()
    Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ArbolRelease())
    inicializadores.forEach(Inicializador::inicializar)
}
```

Dos decisiones con intención:

- **No va en el `Set` de inicializadores** (guía 04): un multibinding no
  garantiza orden, y el logging es infraestructura de la que los demás
  inicializadores ya dependen (ellos mismos loguean). Se planta primero, a
  mano.
- **`BuildConfig.DEBUG` decide el árbol**. Ojo: en AGP 8 la clase
  `BuildConfig` no se genera por defecto; hay que activarla:

  ```kotlin
  buildFeatures { buildConfig = true }
  ```

### 3.2 `DebugTree`: el árbol de desarrollo

Imprime todo y **deduce el tag del nombre de la clase** que llama — adiós
companion objects con TAG. Nuestros logs de debug salen así (capturado):

```
D RegistroInicializador: RegistroInicializador ejecutado
D PrimerArranqueInicializador: Primer arranque registrado en: 1784388444144
D MainActivity: Saludos enviados hasta ahora: 0
D OkHttp  : --> GET https://dummyjson.com/quotes/random
D OkHttp  : <-- 200 https://dummyjson.com/quotes/random (1896ms, unknown-length body)
D DefaultFraseRepository: Frase 165 guardada en la base de datos
```

### 3.3 `ArbolRelease`: el árbol de producción (`core/registro/`)

```kotlin
class ArbolRelease : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean =
        priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Log.println(priority, tag ?: TAG_POR_DEFECTO, message)
        // Aquí iría: crashlytics.recordException(t) / log(message)
    }
}
```

- `isLoggable` corta VERBOSE/DEBUG/INFO **antes** de que Timber formatee el
  mensaje: las llamadas `Timber.d("Frase %d...", id)` en release no
  imprimen *y tampoco construyen el String* (por eso se pasan argumentos
  `%d` en vez de concatenar con `$`).
- El método `log` es EL punto único donde una app real enchufa Crashlytics
  o Sentry: los `Timber.w/e` de toda la app desembocan aquí sin que nadie
  más lo sepa.

## 4. Las llamadas: qué se loguea y con qué nivel

```kotlin
// data — diagnóstico (D). Ids sí, contenidos no: regla anti-fugas
Timber.d("Frase %d guardada en la base de datos", dto.id)

// domain — fallo esperado y manejado (W, no E), la excepción como 1er argumento
} catch (e: Exception) {
    Timber.w(e, "Fallo al obtener la frase de la red")
    Result.failure(e)
}
```

- La excepción va **como argumento**, no interpolada: Timber le imprime el
  stacktrace completo (y el árbol de release la recibiría entera para el
  crash reporting).
- `W` y no `E` porque perder la red es un escenario contemplado que la UI
  maneja (guía 06): `E` se reserva para lo que no debería pasar. Si en
  release solo hay ruido de ERROR, nadie distingue un incendio real.

### 4.1 OkHttp desemboca en Timber

El interceptor de logging de la guía 06 admite un logger propio:

```kotlin
HttpLoggingInterceptor { mensaje -> Timber.tag("OkHttp").d(mensaje) }
    .apply { level = HttpLoggingInterceptor.Level.BASIC }
```

Un único grifo controla ahora TODOS los logs de la app: el tráfico HTTP
sale con tag `OkHttp` (ya no `okhttp.OkHttpClient` como en la guía 06) y en
release **enmudece solo**, porque entra como DEBUG. El matiz de "nivel BODY
nunca en release" de la guía 06 queda resuelto estructuralmente.

## 5. Verificado en el emulador: debug vs release

Misma secuencia en ambos builds (abrir app → pedir frase con red → modo
avión → pedir frase):

**Debug** (`installDebug`): los seis logs del §3.2 y, al fallar la red, el
aviso con stacktrace completo bajo su tag de clase:

```
W ObtenerFraseUseCase: Fallo al obtener la frase de la red
W ObtenerFraseUseCase: java.net.UnknownHostException: Unable to resolve host...
```

**Release** (`installRelease`): **ni un solo log DEBUG** — arranque, tráfico
HTTP y guardado en Room, en silencio — y el fallo de red presente:

```
W HolaAndroid: Fallo al obtener la frase de la red
W HolaAndroid: java.net.UnknownHostException: Unable to resolve host...
```

Dos detalles finos que el experimento hizo visibles:

1. El tag es `HolaAndroid` (el por defecto de nuestro árbol): **deducir el
   tag de la clase es un extra del `DebugTree`**, no de Timber en general.
2. El stacktrace aparece aunque nuestro `log()` ignora `t`: Timber pliega
   el stacktrace dentro de `message` antes de llamar al árbol.

Para poder instalar el build release en local se firmó con la clave de
debug (`signingConfig = signingConfigs.getByName("debug")` con comentario
en el build.gradle.kts); una app publicada usa su firma real, y ese release
de verdad llevaría además `minifyEnabled = true` (R8), que con la regla
`-assumenosideeffects` puede incluso eliminar del APK las llamadas a `Log`
que Timber descarta — nuestro árbol las silencia, pero los Strings siguen
dentro del binario.

## 6. Errores típicos con logs

| Síntoma / hábito | Problema |
|---|---|
| `Log.d("MiApp", "token=$token")` | fuga de datos: nunca secretos ni PII, en ningún nivel |
| `Timber.d("total: " + calcularCaro())` | la concatenación se evalúa AUNQUE el log se descarte: usa `%s` y argumentos |
| Loguear en E lo que se maneja en la UI | el canal de errores se llena de ruido y los incendios reales no se ven |
| `catch (e) { Timber.w(e); throw e }` repetido en cada capa | el mismo error sale 4 veces: se loguea donde se MANEJA, una vez |
| `Log.wtf(...)` "porque suena gracioso" | puede terminar el proceso según el dispositivo: no es un `Log.e` más fuerte |
| Plantar `DebugTree` incondicionalmente | release chivato: el árbol se elige por `BuildConfig.DEBUG` |

## 7. Verificar

```bash
cd HolaAndroid
./gradlew installDebug   && adb logcat -s DefaultFraseRepository OkHttp ObtenerFraseUseCase
./gradlew installRelease && adb logcat -s DefaultFraseRepository OkHttp ObtenerFraseUseCase HolaAndroid
```

En debug: pedir una frase pinta la petición OkHttp y el "Frase N guardada";
el modo avión añade el WARN con stacktrace. En release: silencio total
salvo ese WARN. (Receta completa de emulador por CLI: guía 07, §7.1.)

## Fuentes consultadas (18-07-2026)

- Logcat y niveles de log (oficial): <https://developer.android.com/studio/debug/logcat>
- `android.util.Log` (referencia): <https://developer.android.com/reference/android/util/Log>
- Seguridad: no loguear datos de usuario (oficial): <https://developer.android.com/privacy-and-security/security-tips>
- Timber (README y API): <https://github.com/JakeWharton/timber>
- architecture-samples de Google usando Timber 5.0.1: <https://github.com/android/architecture-samples/blob/main/gradle/libs.versions.toml>
- Logger propio del HttpLoggingInterceptor: <https://square.github.io/okhttp/features/interceptors/>
- BuildConfig en AGP 8 (off por defecto): <https://developer.android.com/build/releases/gradle-plugin-api-updates>

Próxima lección natural: **tests** — unitarios del dominio (el `Result` del
caso de uso), `MigrationTestHelper` para las migraciones (guía 08) y
MockWebServer para la capa de red (guía 06).
