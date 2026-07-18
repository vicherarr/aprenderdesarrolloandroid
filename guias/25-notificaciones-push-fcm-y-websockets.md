# Guía 25 — Notificaciones Push (`FCM`) y Comunicación en Tiempo Real (`WebSockets`)

Objetivo: recibir notificaciones push en tiempo real enviadas desde servidores mediante Firebase Cloud Messaging (FCM) y establecer canales bidireccionales de baja latencia con WebSockets.

---

## 1. Integración de Firebase Cloud Messaging (`FCM`)

FCM es el servicio estándar de transporte de notificaciones en Android, capaz de despertar la app incluso estando cerrada.

### Receptor de Notificaciones (`FirebaseMessagingService`):

```kotlin
class MiFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo Token de FCM registrado: $token")
        // Enviar token al servidor backend
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        remoteMessage.notification?.let { notif ->
            mostrarNotificacionSistema(notif.title ?: "Aviso", notif.body ?: "")
        }
    }

    private fun mostrarNotificacionSistema(titulo: String, mensaje: String) {
        val channelId = "canal_alertas"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alertas Generales", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
```

---

## 2. Comunicación Bidireccional con WebSockets (`OkHttp`)

Para salas de chat, cotizaciones de bolsa o juegos multijugador donde el servidor necesita enviar datos constantemente a la app sin sobrecargar la red:

```kotlin
class GestorWebSocket {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    fun conectar(url: String, onMensajeRecibido: (String) -> Unit) {
        val request = Request.Builder().url(url).build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Conexión WebSocket establecida")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                onMensajeRecibido(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Fallo de conexión", t)
            }
        }

        webSocket = client.newWebSocket(request, listener)
    }

    fun enviarMensaje(texto: String) {
        webSocket?.send(texto)
    }

    fun desconectar() {
        webSocket?.close(1000, "Cierre voluntario")
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Firebase Cloud Messaging en Android: <https://firebase.google.com/docs/cloud-messaging/android/client>
- WebSockets con OkHttp: <https://square.github.io/okhttp/5.x/okhttp/okhttp3/-web-socket/>
