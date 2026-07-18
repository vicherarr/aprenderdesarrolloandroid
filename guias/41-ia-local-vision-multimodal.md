# Guía 41 — IA Local: Visión Multimodal y Procesamiento de Imágenes On-Device

Objetivo: enviar imágenes directamente desde la cámara o la galería al modelo multimodal local ejecutado en la NPU para extraer texto, describir el contenido visual o analizar documentos sin subir fotografías a la nube.

---

## 1. Entrada Multimodal en Modelos Locales

Los modelos multimodales locales procesan datos visuales (matriz de píxeles en formato `Bitmap`) junto con instrucciones de texto (`Content` con `Image` y `Text`).

Toda la computación gráfica y matricial se ejecuta en la **NPU/GPU local** del procesador móvil, evitando transferir archivos de imagen pesados por la red móvil.

---

## 2. Pasar un `Bitmap` de cámara a la IA Local

```kotlin
import com.google.ai.edge.aicore.Content
import com.google.ai.edge.aicore.GenerativeModel

class AnalizadorImagenLocal(context: Context) {
    private val model = GenerativeModel(context = context)

    suspend fun describirImagen(bitmap: Bitmap, pregunta: String): String {
        return try {
            // Escalar la imagen si es muy grande para optimizar el uso de VRAM en la NPU
            val bitmapReducido = Bitmap.createScaledBitmap(bitmap, 512, 512, true)

            val inputContent = Content.Builder()
                .addImage(bitmapReducido)
                .addText(pregunta)
                .build()

            val response = model.generateContent(inputContent)
            response.text ?: "No se obtuvo descripción."
        } catch (e: Exception) {
            Log.e("VisionLocal", "Error al procesar imagen en local", e)
            "Fallo en análisis visual."
        }
    }
}
```

---

## 3. Integración con CameraX para OCR e Inspección Visual

```kotlin
@Composable
fun EscanerDocumentosLocal(analizador: AnalizadorImagenLocal) {
    var resultadoTexto by remember { mutableStateOf("Apunta con la cámara a un documento") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Vista de cámara utilizando la Guía 31
        VistaPreviaCamara(
            modifier = Modifier.weight(1f),
            onFotoCapturada = { imageCapture ->
                // Al tomar la foto, procesarla inmediatamente con la IA local
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                            val bitmap = imageProxy.toBitmap()
                            imageProxy.close()

                            scope.launch {
                                resultadoTexto = analizador.describirImagen(
                                    bitmap = bitmap,
                                    pregunta = "Extrae todo el texto visible en esta imagen y organízalo por campos."
                                )
                            }
                        }
                    }
                )
            }
        )

        Surface(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Text(
                text = resultadoTexto,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Entradas multimodales en Google AI Edge: <https://developer.android.com/ai/aicore/multimodal>
