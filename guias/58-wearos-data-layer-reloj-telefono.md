# Guía 58 — Data Layer API: comunicación reloj ↔ teléfono

Objetivo: sincronizar datos y enviar mensajes entre la app del reloj y la del
teléfono con la **Wearable Data Layer API**: `MessageClient` (mensajes puntuales),
`DataClient` (estado sincronizado y persistente), `CapabilityClient` (descubrir qué
nodo puede hacer qué) y `ChannelClient` (streams/archivos grandes).

Prerrequisitos: Guía 53 (standalone vs. dependiente), corrutinas y Flows. Esta guía
es el **puente en runtime**; la convivencia de módulos en el proyecto se trata en la
Guía 61.

---

## 1. El modelo mental: nodos, capacidades y mensajes

La Data Layer trata reloj y teléfono como **nodos** de una pequeña red conectada
por Bluetooth/Wi-Fi vía Google Play Services. Sobre esa red hay tres estilos de
comunicación:

| Cliente | Metáfora | Uso típico |
|---|---|---|
| `MessageClient` | Un SMS: se envía y llega (o falla). | Órdenes: "empieza el entreno", "abre esta pantalla". |
| `DataClient` | Una carpeta compartida sincronizada. | Estado: ajustes, último valor, perfil. Persiste y se replica. |
| `CapabilityClient` | Guía telefónica: quién sabe hacer qué. | Saber si el teléfono tiene la app compañera instalada. |
| `ChannelClient` | Una tubería/stream. | Enviar archivos o audio grandes. |

Dependencia (en **ambos** módulos, reloj y móvil):

```kotlin
implementation("com.google.android.gms:play-services-wearable:20.0.1")
// .await() sobre los Task<T> de Play Services
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
```

> ✅ Versión verificada compilando un `WearableListenerService` y los envíos con
> `MessageClient`/`DataClient`/`CapabilityClient` (play-services-wearable 20.0.1 en
> jul-2026).

> ⚠️ La Data Layer **solo** conecta apps que comparten el mismo `applicationId` y
> están firmadas con la **misma clave**. Por eso la app de reloj y la de móvil se
> publican juntas (Guía 61). Con firmas distintas, no se ven.

---

## 2. `MessageClient`: enviar una orden

Mensaje puntual de un nodo a otro. No persiste: si el destino no está conectado,
falla. Ideal para acciones inmediatas.

```kotlin
import com.google.android.gms.wearable.Wearable

// --- Emisor (p. ej. el teléfono dispara algo en el reloj) ---
suspend fun enviarOrden(context: Context, nodeId: String) {
    val bytes = "start".toByteArray()
    Wearable.getMessageClient(context)
        .sendMessage(nodeId, "/entreno/start", bytes)
        .await()
}
```

```kotlin
// --- Receptor: un WearableListenerService en el otro nodo ---
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class MiWearableService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            "/entreno/start" -> iniciarEntreno()
        }
    }
}
```

Registro del servicio en el manifest:

```xml
<service android:name=".MiWearableService" android:exported="true">
    <intent-filter>
        <action android:name="com.google.android.gms.wearable.MESSAGE_RECEIVED" />
        <!-- Filtra por prefijo de path para que solo te lleguen los tuyos -->
        <data android:scheme="wear" android:host="*" android:pathPrefix="/entreno" />
    </intent-filter>
</service>
```

---

## 3. `DataClient`: estado sincronizado y persistente

Un `DataItem` es un pequeño objeto (path + `DataMap` de pares clave-valor) que
Google Play Services **replica automáticamente** en todos los nodos y **persiste**.
Si el reloj estaba apagado, recibe el último estado al reconectar. Perfecto para
ajustes o "último dato conocido".

```kotlin
import com.google.android.gms.wearable.PutDataMapRequest

// --- Escribir/actualizar estado ---
suspend fun guardarObjetivo(context: Context, pasosMeta: Int) {
    val request = PutDataMapRequest.create("/config/objetivo").apply {
        dataMap.putInt("pasos", pasosMeta)
        dataMap.putLong("ts", System.currentTimeMillis())  // fuerza cambio para que se propague
    }.asPutDataRequest().setUrgent()
    Wearable.getDataClient(context).putDataItem(request).await()
}
```

```kotlin
// --- Escuchar cambios en el otro nodo ---
class MiWearableService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == "/config/objetivo") {
                val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                aplicarObjetivo(map.getInt("pasos"))
            }
        }
    }
}
```

> 💡 Si escribes el **mismo** `DataMap` dos veces, no se propaga (los `DataItem`
> son idempotentes por contenido). Por eso se añade un timestamp cuando quieres
> forzar la notificación. Para imágenes usa `Asset` dentro del `DataMap`.

---

## 4. `CapabilityClient`: ¿hay app compañera? ¿a qué nodo hablo?

Antes de enviar nada conviene saber **qué nodo** puede recibirlo. Cada app declara
sus **capacidades** en un fichero de recursos `res/values/wear.xml`:

```xml
<resources>
    <string-array name="android_wear_capabilities">
        <item>telefono_app</item>   <!-- la app de móvil anuncia esta capacidad -->
    </string-array>
</resources>
```

Y el reloj busca quién la tiene:

```kotlin
import com.google.android.gms.wearable.CapabilityClient

suspend fun nodoConAppTelefono(context: Context): String? {
    val info = Wearable.getCapabilityClient(context)
        .getCapability("telefono_app", CapabilityClient.FILTER_REACHABLE)
        .await()
    return info.nodes.firstOrNull { it.isNearby }?.id
}
```

Si no hay ningún nodo con esa capacidad, el teléfono **no tiene la app instalada**:
puedes ofrecer abrir su ficha de Play en el móvil (`RemoteActivityHelper`).

---

## 5. `ChannelClient`: datos grandes (archivos, audio)

`DataClient` es para objetos pequeños (límite ~100 KB). Para transferir un archivo
de audio grabado en el reloj o una foto, se abre un **canal** (stream):

```kotlin
val channelClient = Wearable.getChannelClient(context)
val channel = channelClient.openChannel(nodeId, "/audio/nota").await()
val output = channelClient.getOutputStream(channel).await()
output.use { it.write(bytesDeAudio) }
```

El otro nodo recibe `onChannelOpened` y lee el `InputStream`.

---

## 6. Patrón completo: reloj toma un dato, el móvil lo persiste

Un flujo típico de una app de fitness:

1. El reloj mide un entreno con Health Services (Guía 57).
2. Al terminar, el reloj escribe el resumen en un `DataItem` `/entreno/resumen`.
3. El `WearableListenerService` del **móvil** recibe `onDataChanged`, guarda el
   entreno en su Room y lo sube a la nube.
4. El móvil confirma con un `MessageClient` `/entreno/sincronizado`, y el reloj
   marca el registro como enviado.

Así cada dispositivo hace lo que mejor sabe: el reloj mide, el móvil almacena y
sincroniza con el servidor.

---

## 7. Probarlo en emuladores

Necesitas **reloj + teléfono emparejados** (Guía 53 §6). Trucos:

```bash
# Ver los nodos conectados desde el reloj
adb -s <reloj> shell dumpsys activity service \
  com.google.android.gms/.wearable.service.WearableService | grep -i node
```

Sin emparejar, la Data Layer no descubre nodos y todo falla en silencio: verifica
primero la conexión.

---

## 8. Buenas prácticas y errores comunes

| Hábito | Problema / recomendación |
|---|---|
| Mismo `DataMap` reescrito | No se propaga: añade timestamp si necesitas forzarlo. |
| Enviar archivos por `DataClient` | Límite ~100 KB: usa `ChannelClient`. |
| Asumir que el teléfono está | Comprueba con `CapabilityClient` antes de enviar. |
| Firmas distintas en las dos apps | No se comunican: misma clave y `applicationId` (Guía 61). |
| Lógica pesada en el `WearableListenerService` | Delega a WorkManager; el callback debe ser breve. |
| No usar `.setUrgent()` para cambios que corren prisa | El sistema podría retrasar la sincronización. |

---

## Fuentes consultadas (20-07-2026)

- Wearable Data Layer API: <https://developer.android.com/training/wearables/data/data-layer>
- Enviar y recibir mensajes: <https://developer.android.com/training/wearables/data/messages>
- Sincronizar datos con DataClient: <https://developer.android.com/training/wearables/data/data-items>
- Detectar capacidades de nodos: <https://developer.android.com/training/wearables/data/network-bandwidth>
- Canales para transferir datos grandes: <https://developer.android.com/training/wearables/data/channels>
