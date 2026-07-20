# Guía 57 — Health Services: sensores de salud, ejercicio y monitorización pasiva

Objetivo: leer datos de salud del reloj (frecuencia cardíaca, pasos, calorías,
distancia) mediante **Health Services**, la API que Wear OS ofrece por encima de
los sensores crudos para que el sistema gestione batería y precisión. Cubre las
mediciones puntuales, el seguimiento de un **ejercicio** y la **monitorización
pasiva** en segundo plano.

Prerrequisitos: Guía 53 (entorno), corrutinas y Flows (Guías 51–52). Conviene haber
visto los sensores crudos del móvil (Guía 27) para apreciar por qué en el reloj
**no** se usan directamente.

---

## 1. Por qué Health Services y no `SensorManager`

En el móvil se lee el acelerómetro con `SensorManager`. En el reloj eso sería
ruinoso para la batería y poco fiable. **Health Services** es una capa que:

- Da acceso a **métricas ya procesadas** (ppm, pasos, ritmo, VO2max…) en vez de
  señales crudas.
- **Agrupa y programa** las lecturas de varias apps para encender el sensor lo
  mínimo (*batching*), clave para la autonomía.
- Ofrece **monitorización pasiva** que sigue midiendo con la app cerrada, con
  coste mínimo.
- Abstrae las diferencias de hardware entre relojes.

Dependencia:

```kotlin
// Health Services aún no tiene 1.0.0 estable; rc02 es la más reciente (jul-2026)
implementation("androidx.health:health-services-client:1.1.0-rc02")
// .await() sobre los ListenableFuture de Health Services
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.8.1")
```

Permisos en el manifest (según lo que midas):

```xml
<uses-permission android:name="android.permission.BODY_SENSORS" />
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<!-- Para medir en segundo plano de forma continua (Android 13+) -->
<uses-permission android:name="android.permission.BODY_SENSORS_BACKGROUND" />
```

`BODY_SENSORS` es un permiso **peligroso**: hay que solicitarlo en runtime
(Guía 26 explica el patrón de permisos).

---

## 2. Comprobar capacidades del reloj

No todos los relojes miden lo mismo. **Siempre** consulta las capacidades antes de
suscribirte a una métrica:

```kotlin
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataType

val healthClient = HealthServices.getClient(context)
val measureClient = healthClient.measureClient

val capabilities = measureClient.getCapabilitiesAsync().await()
val soportaHR = DataType.HEART_RATE_BPM in capabilities.supportedDataTypesMeasure
```

---

## 3. Medición puntual: `MeasureClient` (heart rate en vivo)

Para una pantalla que muestra las pulsaciones **mientras el usuario mira**
(medición activa, corta), se registra un callback y se expone como `Flow`:

```kotlin
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun heartRateFlow(measureClient: MeasureClient): Flow<Double> = callbackFlow {
    // MeasureCallback obliga a implementar los cuatro métodos (verificado
    // compilando): onRegistered y onRegistrationFailed además de los dos de datos
    val callback = object : MeasureCallback {
        override fun onRegistered() {}
        override fun onRegistrationFailed(throwable: Throwable) { close(throwable) }
        override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {}
        override fun onDataReceived(data: DataPointContainer) {
            data.getData(DataType.HEART_RATE_BPM).lastOrNull()?.let {
                trySend(it.value)
            }
        }
    }
    measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
    awaitClose { measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback) }
}
```

Recuerda el `awaitClose` (Guía 51): sin él, el sensor sigue encendido y drena la
batería. La medición activa es cara; úsala solo con la pantalla visible.

---

## 4. Seguir un ejercicio: `ExerciseClient`

Para una app de entreno (correr, bici, natación) se usa el `ExerciseClient`, que
gestiona toda la sesión: métricas agregadas, auto-pausa, GPS y estado del ejercicio.

```kotlin
import androidx.health.services.client.data.*

val exerciseClient = healthClient.exerciseClient

val config = ExerciseConfig.builder(ExerciseType.RUNNING)
    .setDataTypes(setOf(
        DataType.HEART_RATE_BPM,
        DataType.DISTANCE_TOTAL,
        DataType.CALORIES_TOTAL,
        DataType.HEART_RATE_BPM_STATS,
    ))
    .setIsAutoPauseAndResumeEnabled(true)
    .setIsGpsEnabled(true)
    .build()

exerciseClient.setUpdateCallback(object : ExerciseUpdateCallback {
    override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
        val distancia = update.latestMetrics.getData(DataType.DISTANCE_TOTAL)
        // actualizar la UI / Ongoing Activity
    }
    override fun onLapSummaryReceived(summary: ExerciseLapSummary) {}
    override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {}
    override fun onRegistered() {}
    override fun onRegistrationFailed(throwable: Throwable) {}
})

exerciseClient.startExerciseAsync(config).await()
// ... pauseExerciseAsync(), resumeExerciseAsync(), endExerciseAsync()
```

> ⚠️ Un ejercicio en curso **debe** ir acompañado de una **Ongoing Activity**
> (Guía 59) para que el sistema no mate el proceso y el usuario vea el entreno en
> curso desde la esfera. Sin ella, Wear OS puede cerrar tu app.

---

## 5. Monitorización pasiva: `PassiveMonitoringClient`

Para acumular pasos/calorías/ppm **todo el día con la app cerrada** y coste mínimo,
se usa monitorización pasiva. Los datos llegan a un `Service` propio, no a la UI.

```kotlin
import androidx.health.services.client.data.PassiveListenerConfig

val passiveClient = healthClient.passiveMonitoringClient

val config = PassiveListenerConfig.builder()
    .setDataTypes(setOf(DataType.STEPS_DAILY, DataType.CALORIES_DAILY))
    .build()

passiveClient.setPassiveListenerServiceAsync(
    MiPassiveListenerService::class.java, config
).await()
```

El servicio recibe los lotes de datos periódicamente:

```kotlin
import androidx.health.services.client.PassiveListenerService

class MiPassiveListenerService : PassiveListenerService() {
    override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
        val pasos = dataPoints.getData(DataType.STEPS_DAILY).lastOrNull()?.value
        // persistir (Room) o refrescar el Tile / Complication de pasos
    }
}
```

También puede disparar **eventos de salud** (p. ej. ppm por encima de un umbral)
sin mantener nada encendido en tu proceso.

---

## 6. Objetivos y eventos de salud

Health Services puede avisarte al alcanzar una meta sin que tú vigiles el sensor:

```kotlin
val objetivo = PassiveGoal(
    dataTypeCondition = DataTypeCondition(
        DataType.STEPS_DAILY,
        10_000L,
        ComparisonType.GREATER_THAN_OR_EQUAL
    )
)
// añadido al PassiveListenerConfig -> el sistema te notifica al llegar a 10 000 pasos
```

Ideal para "10 000 pasos alcanzados": el trabajo pesado lo hace el sistema.

---

## 7. Probar sin reloj real: sensores sintéticos

El emulador no tiene corazón. Health Services trae un **cliente sintético** para
inyectar datos de prueba desde `adb`:

```bash
# Activar el proveedor sintético en el emulador
adb shell am broadcast \
  -a "whs.USE_SYNTHETIC_PROVIDERS" \
  com.google.android.wearable.healthservices

# Simular una carrera (genera HR, pasos, distancia...)
adb shell am broadcast \
  -a "whs.synthetic.user.START_WALKING" \
  com.google.android.wearable.healthservices
```

Hay *broadcasts* para caminar, correr, pedalear y parar. Es la única forma
práctica de desarrollar apps de fitness sin hardware.

---

## 8. Buenas prácticas

| Recomendación | Motivo |
|---|---|
| Consulta capacidades antes de suscribirte | El hardware varía entre relojes. |
| Medición activa (`MeasureClient`) solo con pantalla visible | Es la más cara en batería. |
| Todo el día → `PassiveMonitoringClient`, no `MeasureClient` | Diseñado para coste mínimo. |
| Ejercicio → siempre con Ongoing Activity | Evita que el sistema mate la sesión. |
| Persiste con Room y sincroniza al móvil por Data Layer | El reloj no es el almacén final. |
| Libera callbacks (`awaitClose`, `unregister`) | Fuga = sensor encendido = batería a cero. |

---

## Fuentes consultadas (20-07-2026)

- Health Services on Wear OS: <https://developer.android.com/health-and-fitness/guides/health-services>
- Medir datos con MeasureClient: <https://developer.android.com/health-and-fitness/guides/health-services/measure-data>
- Seguir ejercicios con ExerciseClient: <https://developer.android.com/health-and-fitness/guides/health-services/active-data>
- Monitorización pasiva: <https://developer.android.com/health-and-fitness/guides/health-services/passive-data>
- Datos sintéticos para pruebas: <https://developer.android.com/health-and-fitness/guides/health-services/test-with-synthetic-data>
