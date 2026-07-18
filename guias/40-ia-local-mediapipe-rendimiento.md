# Guía 40 — IA Local: Rendimiento, Cuantización y MediaPipe LLM Inference

Objetivo: aprender a ejecutar modelos LLM personalizados de código abierto (Llama, Gemma, Phi) en local utilizando la API **MediaPipe LLM Inference**, optimizando el rendimiento (tokens/segundo) mediante cuantización INT4/INT8 y aceleración por GPU/NPU.

---

## 1. Ejecutar Modelos Personalizados con MediaPipe

Cuando necesitas ejecutar modelos open-source propios (ej. `gemma-2b-it.bin` o `llama-3-8b-instruct.task`) en lugar de los modelos gestionados por AICore, se utiliza **MediaPipe LLM Inference API**.

```kotlin
dependencies {
    implementation("com.google.mediapipe:tasks-genai:0.10.14")
}
```

---

## 2. Inicialización del Motor de Inferencia (`LlmInference`)

```kotlin
import com.google.mediapipe.tasks.genai.llminference.LlmInference

class GestorMediaPipeLlm(context: Context, rutaModeloLocal: String) {
    private var llmInference: LlmInference? = null

    init {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(rutaModeloLocal) // Archivo .bin o .task en assets/disco
            .setMaxTokens(512)
            .setResultListener { parcialText, done ->
                // Callback de streaming de respuesta
            }
            .setErrorListener { error ->
                Log.e("MediaPipe", "Error en inferencia LLM", error)
            }
            .build()

        llmInference = LlmInference.createFromOptions(context, options)
    }

    fun generarRespuesta(prompt: String): String {
        return llmInference?.generateResponse(prompt) ?: "Error: Motor no listo"
    }
}
```

---

## 3. Técnicas de Cuantización (INT4 vs INT8 vs FP16)

Para cargar un modelo de lenguaje de miles de millones de parámetros en la memoria RAM de un móvil (típicamente 8GB a 12GB compartidos con la GPU), el modelo debe estar **cuantizado**:

| Formato | Tamaño en Disco / VRAM | Pérdida de Calidad | Velocidad de Inferencia |
|---|---|---|---|
| **FP16** (16-bit Float) | ~4.0 GB por 2B parámetros | Ninguna | Lenta en teléfonos. |
| **INT8** (8-bit Integer) | ~2.0 GB por 2B parámetros | Despreciable (< 1%) | **Muy Rápida** (Soportada por la NPU). |
| **INT4** (4-bit Integer) | **~1.1 GB por 2B parámetros** | Leve | **Máxima velocidad** en dispositivos móviles. |

---

## 4. Medir y Optimizar Métricas de Rendimiento (tokens/segundo)

El rendimiento de la IA local se mide en dos métricas fundamentales:

1. **TTFT (*Time To First Token*)**: Tiempo que tarda la NPU en procesar el prompt de entrada y devolver el primer token (ms).
2. **Decode Speed (tok/s)**: Velocidad de generación de tokens continuos (ej. 15-30 tok/s es una velocidad muy fluida para lectura humana).

```kotlin
fun medirRendimientoInferencia(bloque: () -> String) {
    val inicio = System.currentTimeMillis()
    val resultado = bloque()
    val fin = System.currentTimeMillis()

    val tiempoTotalMs = fin - inicio
    val estimadoTokens = resultado.split(" ").size * 1.3 // Estimación aproximada de tokens
    val tokensPorSegundo = (estimadoTokens / (tiempoTotalMs / 1000.0))

    Log.d("RendimientoIA", "Tiempo: ${tiempoTotalMs}ms | Velocidad: %.2f tok/s".format(tokensPorSegundo))
}
```

---

## Resumen de la Serie de IA Local (Guías 38 a 42):

| Guía | Concepto clave | Tecnología principal |
|---|---|---|
| **38** | Introducción a IA On-Device | **Android AICore**, NPU/TPU, `GenerativeModel.isAvailable()` |
| **39** | Prompts y Streaming | **Google AI Edge SDK**, `generateContentStream`, Flow en Compose |
| **40** | APIs Especializadas | `Summarizer`, `Proofreader`, `SmartReply` |
| **41** | Visión Multimodal | Procesamiento de `Bitmap` con `CameraX` sin subir fotos a la nube |
| **42** | Custom LLMs y Rendimiento | **MediaPipe LLM Inference**, Cuantización INT4/INT8, medición tok/s |

---

## Fuentes consultadas (18-07-2026)

- MediaPipe LLM Inference API para Android: <https://developers.google.com/mediapipe/solutions/genai/llm_inference/android>
- Modelos Gemma para dispositivos móviles: <https://ai.google.dev/gemma/docs/formatting>
