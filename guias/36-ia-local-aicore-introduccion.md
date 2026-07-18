# Guía 36 — IA Local: Introducción a Ejecución On-Device y Android AICore

Objetivo: entender los cimientos de la Inteligencia Artificial generativa ejecutada localmente en el dispositivo (*on-device AI*) mediante **Android AICore** y la NPU/TPU del procesador móvil, sin necesidad de conexión a internet ni costes de API por token.

---

## 1. ¿Por qué ejecutar IA en Local (*On-Device*)?

Tradicionalmente, las llamadas a modelos de lenguaje (LLMs) se realizaban mediante peticiones HTTP a servidores en la nube. La ejecución en el chip acelerador del propio teléfono (*NPU/TPU*) aporta ventajas determinantes:

| Aspecto | IA en la Nube (Cloud APIs) | IA Local (*On-Device*) |
|---|---|---|
| **Privacidad** | Los datos del usuario viajan por internet a servidores externos. | **100% Privado**: Los datos nunca salen del chip del teléfono. |
| **Latencia** | Depende de la red (300ms - 2000ms). | **Inmediata**: Respuesta directa de la NPU local. |
| **Sin Conexión** | Requiere internet (Fail si no hay cobertura/modo avión). | **Funciona 100% Offline**. |
| **Coste** | Facturación por cada token generado/procesado. | **Coste Cero en APIs**. |

---

## 2. El subsistema de sistema operativo: Android AICore

**Android AICore** es el servicio a nivel de sistema operativo de Android que gestiona la aceleración por hardware de modelos de lenguaje ligeros (como la familia **Gemini Nano**).

AICore se encarga automáticamente de:
- Descargar y mantener actualizados los pesos del modelo local.
- Asignar la carga de trabajo al acelerador por hardware disponible (NPU, TPU o GPU).
- Proteger la memoria RAM del teléfono mediante paginación inteligente.

---

## 3. Comprobar si el dispositivo soporta IA Local

No todos los dispositivos cuentan con una NPU con suficiente memoria unificada para ejecutar modelos locales. La disponibilidad se verifica mediante `GenerativeModel`:

```kotlin
import com.google.ai.edge.aicore.GenerativeModel

suspend fun verificarSoporteIaLocal(context: Context): Boolean {
    return try {
        val model = GenerativeModel(context = context)
        val disponible = model.isAvailable()
        Log.d("LocalAI", "¿IA Local disponible en NPU?: $disponible")
        disponible
    } catch (e: Exception) {
        Log.e("LocalAI", "Hardware no compatible con AICore", e)
        false
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Android AICore y Gemini Nano on-device: <https://developer.android.com/ai/aicore>
- Documentación de Google AI Edge: <https://developer.android.com/ai/design-ai-features>
