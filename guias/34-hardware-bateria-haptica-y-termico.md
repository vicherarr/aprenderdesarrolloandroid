# Guía 34 — Hardware: Batería, Motores Hápticos y Estado Térmico (`VibratorManager`, `BatteryManager`)

Objetivo: aprender a monitorear la salud y carga de la batería por hardware, gestionar el estado térmico del dispositivo para prevenir el sobrecalentamiento y activar vibraciones hápticas avanzadas.

---

## 1. Monitor de Batería y Fuente de Alimentación (`BatteryManager`)

La API `BatteryManager` permite leer el porcentaje de carga, temperatura física de la celda de la batería, voltaje y si se está cargando por USB, cargador de pared o carga inalámbrica Qi:

```kotlin
@Composable
fun EstadoBateriaMonitor() {
    val context = LocalContext.current
    var nivelBateria by remember { mutableIntStateOf(0) }
    var estaCargando by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    nivelBateria = (level * 100 / scale.toFloat()).toInt()

                    val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    estaCargando = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                }
            }
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    Text("Batería: $nivelBateria% ${if (estaCargando) "⚡ (Cargando)" else ""}")
}
```

---

## 2. Motores de Vibración Háptica (`VibratorManager` / `VibrationEffect`)

Los actuadores hápticos modernos (motores lineales de eje X) permiten generar patrones de retroalimentación táctil precisos (clicks, confirmaciones, alertas de error):

```xml
<uses-permission android:name="android.permission.VIBRATE" />
```

```kotlin
fun ejecutarVibracionHaptica(context: Context, tipo: TipoHaptico) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator

        val efecto = when (tipo) {
            TipoHaptico.CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            TipoHaptico.CONFIRMACION -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            TipoHaptico.ERROR -> VibrationEffect.createWaveform(longArrayOf(0, 50, 100, 50), intArrayOf(0, 255, 0, 255), -1)
        }
        vibrator.vibrate(efecto)
    }
}

enum class TipoHaptico { CLICK, CONFIRMACION, ERROR }
```

---

## 3. Estado Térmico del Hardware (`PowerManager.OnThermalStatusChangedListener`)

A partir de Android 10 (API 29), se puede escuchar el estado térmico del procesador/batería para reducir la carga de trabajo (disminuir fotogramas por segundo, pausar tareas en segundo plano) si el dispositivo sufre estrangulamiento térmico (*thermal throttling*):

```kotlin
@RequiresApi(Build.VERSION_CODES.Q)
fun registrarListenerTermico(context: Context) {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    powerManager.addThermalStatusListener { status ->
        when (status) {
            PowerManager.THERMAL_STATUS_NONE -> Log.d("Thermal", "Temperatura normal")
            PowerManager.THERMAL_STATUS_LIGHT -> Log.d("Thermal", "Elevación térmica ligera")
            PowerManager.THERMAL_STATUS_MODERATE -> Log.d("Thermal", "Moderado: sugerido reducir FPS")
            PowerManager.THERMAL_STATUS_SEVERE -> Log.w("Thermal", "Severo: pausar procesamiento pesado")
            PowerManager.THERMAL_STATUS_CRITICAL -> Log.e("Thermal", "Crítico: apagar funciones inmediatas")
        }
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Estado de Batería y Carga: <https://developer.android.com/develop/background-work/services/battery-optimization>
- Guía de Háptica en Android: <https://developer.android.com/design/ui/mobile/guides/patterns/haptics-design-guidelines>
