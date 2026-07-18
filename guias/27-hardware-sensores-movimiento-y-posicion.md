# Guía 27 — Hardware: Sensores de Movimiento y Posición (Acelerómetro, Giroscopio, Brújula, Podómetro)

Objetivo: aprender a acceder y procesar lecturas en tiempo real del hardware IMU (*Inertial Measurement Unit*) de los móviles Android: acelerómetro, giroscopio, magnetómetro y contador de pasos.

---

## 1. Arquitectura de Sensores en Android (`SensorManager`)

El acceso a todos los sensores de hardware del dispositivo se gestiona a través del servicio del sistema `SensorManager`:

```kotlin
val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
```

### Principales sensores físicos de movimiento:

| Sensor (`Sensor.TYPE_*`) | Medida física | Unidad | Uso típico |
|---|---|---|---|
| `TYPE_ACCELEROMETER` | Aceleración en ejes X, Y, Z (incluye gravedad). | $m/s^2$ | Detección de agitación (*shake*), inclinación. |
| `TYPE_GYROSCOPE` | Velocidad de rotación angular en X, Y, Z. | $rad/s$ | Juegos 3D, estabilización de cámara, VR/AR. |
| `TYPE_MAGNETIC_FIELD` | Campo magnético en X, Y, Z. | $\mu T$ | Brújula digital, orientación espacial. |
| `TYPE_STEP_COUNTER` | Pasos totales desde el último reinicio. | Entero | Pedometer, apps de salud (bajo consumo). |

---

## 2. Lectura en tiempo real con `SensorEventListener`

```kotlin
class GestorSensoresMovimiento(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun registrar() {
        acelerometro?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI // Frecuencia de muestreo adaptada a UI (~60ms)
            )
        }
    }

    fun desregistrar() {
        // IMPORTANTE: Desregistrar siempre para evitar consumo drástico de batería
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calcular magnitud de aceleración total
            val aceleracionTotal = sqrt(x * x + y * y + z * z)
            if (aceleracionTotal > 15f) { // Umbral de sacudida
                // Dispositivo agitado
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Cambios en la precisión del sensor (ej. calibración de brújula)
    }
}
```

---

## 3. Matriz de Orientación y Brújula Digital

Para construir una brújula precisa combinando Acelerómetro y Magnetómetro:

```kotlin
fun calcularOrientacionBrújula(
    acelerometroValues: FloatArray,
    magnetometroValues: FloatArray
): Float {
    val rotationMatrix = FloatArray(9)
    val orientationAngles = FloatArray(3)

    if (SensorManager.getRotationMatrix(rotationMatrix, null, acelerometroValues, magnetometroValues)) {
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val azimutEnRadianes = orientationAngles[0]
        val azimutEnGrados = (Math.toDegrees(azimutEnRadianes.toDouble()) + 360) % 360
        return azimutEnGrados.toFloat() // Orientación en grados (0° Norte, 90° Este)
    }
    return 0f
}
```

---

## 4. Contador de Pasos por Hardware (`TYPE_STEP_COUNTER`)

Requiere permiso explícito en `AndroidManifest.xml` a partir de Android 10 (API 29):

```xml
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
```

```kotlin
@Composable
fun ContadorPasosDisplay(sensorManager: SensorManager) {
    var pasos by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                    pasos = event.values[0].toInt()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        sensorManager.registerListener(listener, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Text("Pasos acumulados hoy: $pasos", style = MaterialTheme.typography.headlineSmall)
}
```

---

## Fuentes consultadas (18-07-2026)

- Generalidades sobre sensores en Android: <https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview>
- Sensores de movimiento: <https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion>
- Sensores de posición: <https://developer.android.com/develop/sensors-and-location/sensors/sensors_position>
