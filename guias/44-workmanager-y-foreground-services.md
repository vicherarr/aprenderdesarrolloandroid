# Guía 44 — Tareas en Segundo Plano y Ejecución Persistente (`WorkManager` & Foreground Services)

Objetivo: dominar la ejecución asíncrona garantizada en segundo plano mediante `WorkManager` (sincronizaciones con restricciones de red y batería) y servicios en primer plano (*Foreground Services*) con notificaciones persistentes.

---

## 1. ¿Cuándo usar cada componente?

| Solución | Garantía de Ejecución | Caso de uso ideal |
|---|---|---|
| **Corrutinas / Flow** | Mientras la app está abierta en pantalla. | Operaciones UI de corta duración. |
| **`WorkManager`** | **Garantizada incluso si el teléfono se reinicia o la app se cierra.** | Sincronizar bases de datos, subir registros, descargar actualizaciones. |
| **`Foreground Service`** | **Inmediata y continua (con notificación persistente)**. | Reproductores de audio, navegación GPS paso a paso, llamadas VoIP. |

---

## 2. Programar un `CoroutineWorker` con restricciones

```kotlin
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

```kotlin
class SincronizacionDatosWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("WorkManager", "Iniciando sincronización de datos en segundo plano...")
            // Realizar petición de red o sincronización Room
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

// Programar la tarea con restricciones de hardware
fun programarSincronizacion(context: Context) {
    val restricciones = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.UNMETERED) // Solo Wi-Fi
        .setRequiresBatteryNotLow(true) // Batería suficiente
        .build()

    val workRequest = PeriodicWorkRequestBuilder<SincronizacionDatosWorker>(24, TimeUnit.HOURS)
        .setConstraints(restricciones)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "SincronizacionDiaria",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}
```

---

## 3. Servicios en Primer Plano (*Foreground Services*) en Android 14+

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

```kotlin
class ReproductorAudioService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificacion = NotificationCompat.Builder(this, "canal_audio")
            .setContentTitle("Reproduciendo música")
            .setContentText("Canción en curso...")
            .setSmallIcon(R.drawable.ic_music)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notificacion, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, notificacion)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

---

## Fuentes consultadas (18-07-2026)

- Guía oficial de WorkManager: <https://developer.android.com/topic/libraries/architecture/workmanager>
- Tipos de Foreground Services en Android 14: <https://developer.android.com/about/versions/14/changes/fgs-types-required>
