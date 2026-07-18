# Guía 28 — Hardware: Sensores Ambientales y de Proximidad (Luz, Proximidad, Barómetro)

Objetivo: aprender a controlar los sensores ambientales de hardware del dispositivo para responder a la proximidad física del usuario, el nivel de iluminancia ambiente y la presión barométrica.

---

## 1. Catálogo de Sensores Ambientales

| Sensor (`Sensor.TYPE_*`) | Medida | Unidad | Caso de uso principal |
|---|---|---|---|
| `TYPE_PROXIMITY` | Distancia a objeto cercano al auricular. | $cm$ (o binario Cerca/Lejos). | Apagar la pantalla en llamadas al acercarse a la oreja. |
| `TYPE_LIGHT` | Iluminancia del entorno. | $lux$ ($lx$) | Ajuste dinámico de brillo / modo nocturno de pantalla. |
| `TYPE_PRESSURE` | Presión atmosférica (Barómetro). | $hPa$ / $mbar$ | Cálculo de altitud sobre el nivel del mar e interiores. |
| `TYPE_AMBIENT_TEMPERATURE` | Temperatura del aire ambiente. | $^\circ C$ | Medición ambiental (poco común en smartphones). |

---

## 2. Sensor de Proximidad (`TYPE_PROXIMITY`)

El sensor de proximidad suele ubicarse junto al altavoz superior. Devuelve la distancia exacta en centímetros o, en la mayoría de los dispositivos modernos, valores binarios (0 para `CERCA` y la distancia máxima para `LEJOS`):

```kotlin
class ControlProximidadLlamada(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximidad = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    fun iniciarEscucha(onCerca: () -> Unit, onLejos: () -> Unit) {
        proximidad?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            val distancia = event.values[0]
            val maxRango = event.sensor.maximumRange

            if (distancia < maxRango) {
                // Objeto muy cerca (teléfono junto a la cara)
            } else {
                // Objeto lejos
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
```

---

## 3. Sensor de Luz Ambiental (`TYPE_LIGHT`)

Mide el nivel de iluminancia ambiente en lux ($lx$). Valores de referencia habituales:

- **0 - 10 lx**: Habitación a oscuras.
- **100 - 500 lx**: Iluminación de oficina estándar.
- **10.000 - 100.000 lx**: Luz solar directa.

```kotlin
@Composable
fun SensorLuzIndicador(sensorManager: SensorManager) {
    var lux by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
                    lux = event.values[0]
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Column {
        Text("Nivel de Luz: ${lux.toInt()} lx", style = MaterialTheme.typography.titleLarge)
        Text(
            text = when {
                lux < 50f -> "Ambiente oscuro — Aplicar tema oscuro"
                lux > 5000f -> "Luz solar intensa — Aumentar contraste al máximo"
                else -> "Luz ambiente adecuada"
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
```

---

## 4. Barómetro y Estimación de Altitud (`TYPE_PRESSURE`)

La presión barométrica cambia con la altitud atmosférica. Se puede estimar la altitud en metros mediante la fórmula barométrica reducida integrada en `SensorManager.getAltitude`:

```kotlin
fun calcularAltitudActual(presionHpa: Float): Float {
    // Presión al nivel del mar estándar = 1013.25 hPa
    return SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, presionHpa)
}
```

---

## Fuentes consultadas (18-07-2026)

- Sensores de entorno en Android: <https://developer.android.com/develop/sensors-and-location/sensors/sensors_environment>
- APIs de `SensorManager`: <https://developer.android.com/reference/android/hardware/SensorManager>
