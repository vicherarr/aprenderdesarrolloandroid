# Guía 59 — Energía, modo ambiente, notificaciones y Ongoing Activity

Objetivo: gestionar lo que hace especial (y difícil) a Wear OS: la **batería**. Ver
el **modo ambiente / always-on**, cómo mantener trabajo vivo con una **Ongoing
Activity**, cómo funcionan las **notificaciones** (propias y puenteadas desde el
móvil) y las reglas de trabajo en segundo plano en el reloj.

Prerrequisitos: Guías 54 (Compose for Wear OS), 24 (WorkManager/servicios) y 25
(notificaciones). Aquí se ven las diferencias específicas del reloj.

---

## 1. El presupuesto de energía del reloj

Un reloj tiene una batería diminuta y una pantalla que debe poder estar **siempre
encendida**. El sistema es, por diseño, muy agresivo:

- Cierra apps que dejan de estar en primer plano.
- Limita y agrupa el trabajo en segundo plano.
- Reduce la CPU y atenúa la pantalla al bajar la muñeca (**modo ambiente**).

Consecuencia práctica: **no puedes** asumir que tu app sigue viva ni que la pantalla
está a pleno brillo. Toda función continua (un cronómetro, un entreno, una ruta)
necesita una **Ongoing Activity** para sobrevivir.

---

## 2. Modo ambiente (ambient) y always-on

Cuando el usuario baja la muñeca, el reloj entra en **modo ambiente**: pantalla
atenuada, pocos colores, refresco lento (para ahorrar). Tu app puede:

- **Dejar que el sistema muestre la esfera** (comportamiento por defecto): al bajar
  la muñeca, se sale de tu app. Sencillo y barato.
- **Ser always-on**: tu propia UI permanece visible en ambiente (útil en un entreno
  para ver las pulsaciones sin levantar la muñeca).

Con Compose, el soporte always-on se gestiona con la librería
`androidx.wear:wear-ongoing` + el `AmbientLifecycleObserver`:

```kotlin
implementation("androidx.wear:wear:1.4.0")   // AmbientLifecycleObserver
```

```kotlin
import androidx.wear.ambient.AmbientLifecycleObserver

class MainActivity : ComponentActivity() {
    private val ambientObserver = AmbientLifecycleObserver(this, object :
        AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(details: AmbientLifecycleObserver.AmbientDetails) {
            // Simplifica la UI: fondo negro, sin animaciones, texto tenue
        }
        override fun onExitAmbient() {
            // Vuelve a la UI interactiva completa
        }
        override fun onUpdateAmbient() {
            // Se llama ~1/min en ambiente para refrescar la hora/datos
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(ambientObserver)
        setContent { MiApp() }
    }
}
```

Reglas de oro del modo ambiente:

| Regla | Motivo |
|---|---|
| Fondo negro, mínimo color, sin degradados grandes | OLED apaga píxeles negros = ahorro. |
| Nada de animaciones ni refrescos frecuentes | La pantalla actualiza ~1/min. |
| Mueve el contenido unos píxeles cada minuto (*burn-in*) | Evita quemar la pantalla OLED. |
| Muestra solo lo esencial (hora + 1 dato) | El resto se ve al levantar la muñeca. |

---

## 3. Ongoing Activity: mantener viva una tarea en curso

Una **Ongoing Activity** es una notificación continua "enriquecida" que Wear OS
muestra en la esfera y en el lanzador (un icono/estado tocable) mientras tu app hace
algo que el usuario debe poder retomar: un entreno, una ruta, un temporizador, una
reproducción.

Es **la** manera de decirle al sistema "no me mates, estoy trabajando".

```kotlin
implementation("androidx.wear:wear-ongoing:1.1.0")
implementation("androidx.core:core-ktx:1.13.0")
```

> ✅ `OngoingActivity` verificada compilando (wear-ongoing 1.1.0, wear 1.4.0 en
> jul-2026).

```kotlin
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status

fun mostrarOngoing(context: Context, notifId: Int) {
    // 1) Una notificación normal (foreground service) como base
    val builder = NotificationCompat.Builder(context, CANAL_ENTRENO)
        .setSmallIcon(R.drawable.ic_run)
        .setContentTitle("Carrera en curso")
        .setOngoing(true)
        .setContentIntent(pendingIntentAAbrirLaApp)

    // 2) La capa Ongoing Activity sobre esa notificación
    val ongoing = OngoingActivity.Builder(context, notifId, builder)
        .setStaticIcon(R.drawable.ic_run)
        .setTouchIntent(pendingIntentAAbrirLaApp)
        .setStatus(Status.Builder().addTemplate("#distancia# km").build())
        .build()
    ongoing.apply(context)

    NotificationManagerCompat.from(context).notify(notifId, builder.build())
}
```

Se emite desde un **foreground service** (Guía 24). El patrón: inicias el servicio
al empezar el entreno, publicas la Ongoing Activity, actualizas su `Status` con las
métricas de Health Services (Guía 57) y la retiras al terminar.

---

## 4. Notificaciones en Wear OS

Hay dos orígenes:

1. **Puenteadas (bridged) desde el teléfono.** Por defecto, las notificaciones de
   una app de móvil se **reflejan automáticamente** en el reloj emparejado, sin que
   escribas nada. Puedes desactivar el puente por notificación si tu app de reloj
   ya las gestiona (para no duplicar).
2. **Locales del reloj.** Una app standalone crea sus propias notificaciones con la
   misma `NotificationCompat` del móvil; Wear OS las adapta a la pantalla redonda.

Extras específicos del reloj se añaden con `WearableExtender`:

```kotlin
import androidx.core.app.NotificationCompat.WearableExtender

val notif = NotificationCompat.Builder(context, CANAL)
    .setSmallIcon(R.drawable.ic_msg)
    .setContentTitle("Nuevo mensaje")
    .setContentText("¿Respondes?")
    .extend(
        WearableExtender()
            .addAction(accionResponderVoz)   // respuesta por voz/emoji en el reloj
    )
    .build()
```

Evitar duplicados cuando existen app de móvil y de reloj: se controla con
`setLocalOnly(true)` (solo en este dispositivo) o desactivando el *bridging* con
`setBridgeTag`/la config de puente. Regla: **una notificación, un dispositivo**.

---

## 5. Trabajo en segundo plano en el reloj

- **WorkManager** funciona en Wear OS igual que en móvil (Guía 24) y es la vía
  recomendada para trabajo diferible (sincronizar al reconectar, refrescar un
  Tile). Respeta las restricciones de red y batería del reloj.
- **Foreground services**: permitidos, pero deben ir con notificación/Ongoing
  Activity y durar lo justo. Un entreno es válido; "sondear un servidor cada 5 s",
  no.
- **Alarmas exactas**: muy limitadas por batería; evita despertar la CPU con
  frecuencia.

> ⚠️ Nada de bucles activos ni *polling* frecuente. El reloj penaliza durísimo el
> trabajo continuo fuera de un foreground service justificado. Diseña por eventos
> (Data Layer, Health Services pasivo, WorkManager).

---

## 6. Medir el consumo

- **Battery Historian** y `adb shell dumpsys batterystats` para ver qué despierta el
  reloj.
- El **Power profiler** de Android Studio con el reloj conectado.
- Prueba real: deja la app una hora con la muñeca bajada y mira el gasto. Si baja
  mucho, algo sigue encendido (sensor sin liberar, animación en ambiente, wakelock).

---

## 7. Buenas prácticas

| Recomendación | Motivo |
|---|---|
| Toda tarea continua → foreground service + Ongoing Activity | Sobrevive al cierre agresivo del sistema. |
| UI de ambiente negra, estática, con desplazamiento anti burn-in | Ahorra batería y protege el OLED. |
| Una notificación por dispositivo (`setLocalOnly`/bridging) | Evita duplicados reloj/móvil. |
| WorkManager para lo diferible; nunca *polling* | El sistema lo agrupa y respeta la batería. |
| Libera sensores y wakelocks siempre | La causa nº1 de batería drenada. |

---

## Fuentes consultadas (20-07-2026)

- Mantener la app visible (always-on / ambient): <https://developer.android.com/training/wearables/apps/always-on>
- Ongoing Activity: <https://developer.android.com/training/wearables/apps/ongoing-activity>
- Notificaciones en Wear OS: <https://developer.android.com/training/wearables/notifications>
- Puenteo (bridging) de notificaciones: <https://developer.android.com/training/wearables/notifications/bridger>
- Ahorro de energía y rendimiento: <https://developer.android.com/training/wearables/apps/performance>
