# Guía 40 — IA Local: APIs Especializadas del Sistema (Resúmenes, Reescritura y Corrección)

Objetivo: utilizar las APIs especializadas listas para usar de Android AICore para tareas comunes de lenguaje (resumen automático de texto, corrección gramatical y sugerencias inteligentes de respuesta) sin necesidad de ingeniería de prompts manual.

---

## 1. APIs Especializadas vs Prompts Libres

Además de aceptar prompts de texto abierto, Android AICore ofrece **APIs de tareas específicas** que utilizan adaptadores LORA optimizados por hardware para máxima velocidad y precisión:

| API Especializada | Función | Latencia media en NPU |
|---|---|---|
| **Summarization API** | Resume textos largos o correos en puntos clave de viñetas. | ~150 - 300 ms |
| **Proofreading API** | Corrige gramática, ortografía y tono de escritura. | ~100 - 200 ms |
| **Smart Reply API** | Genera 3 sugerencias cortas de respuesta según el último mensaje recibido. | ~50 ms |

---

## 2. API de Resúmenes Locales (`SummarizationApi`)

```kotlin
import com.google.ai.edge.aicore.Summarizer

class GestorResumenes(context: Context) {
    private val summarizer = Summarizer.create(context)

    suspend fun resumirTextoLargo(textoOriginal: String): String {
        return try {
            val opciones = SummarizerOptions.Builder()
                .setFormat(SummarizerOptions.Format.BULLET_POINTS) // Opciones: PARAGRAPH o BULLET_POINTS
                .setLength(SummarizerOptions.Length.SHORT)
                .build()

            val resultado = summarizer.summarize(textoOriginal, opciones)
            resultado.summaryText
        } catch (e: Exception) {
            Log.e("Summarizer", "Fallo al generar resumen local", e)
            "No se pudo generar el resumen en local."
        }
    }
}
```

---

## 3. API de Corrección de Texto (`Proofreader`)

```kotlin
import com.google.ai.edge.aicore.Proofreader

suspend fun corregirTexto(context: Context, textoSucio: String): String {
    val proofreader = Proofreader.create(context)
    val resultado = proofreader.proofread(textoSucio)
    return resultado.correctedText
}
```

---

## 4. Estrategia de Prompting para Modelos Locales Reducidos

Los modelos locales (como Gemini Nano) tienen un tamaño de parámetros menor que los modelos masivos de la nube (Gemini Ultra/Pro). Para obtener resultados óptimos:

1. **Formatos Estrictos (JSON / Listas)**: Da instrucciones directas y concisas.
2. **Ejemplos Few-Shot**: Proporciona 1 o 2 ejemplos de entrada/salida deseada en la instrucción.
3. **Instrucciones al principio**: Pon la instrucción antes que el contexto largo.

```kotlin
val promptOptimo = """
System: Transforma el siguiente correo en una tarea breve.
Ejemplo Entrada: "Hola Victor, ¿puedes enviar la memoria del proyecto antes de las 5pm?"
Ejemplo Salida: "Enviar memoria de proyecto (límite 17:00h)"

Entrada: "$correoOriginal"
Salida:
""".trimIndent()
```

---

## Fuentes consultadas (18-07-2026)

- APIs de tareas de Android AICore: <https://developer.android.com/ai/aicore/task-apis>
