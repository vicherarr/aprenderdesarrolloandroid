# Guía 30 — Hardware: Micrófono y Captura de Audio (`MediaRecorder`, `AudioRecord`)

Objetivo: dominar la captura de audio por hardware a través del micrófono del dispositivo, monitorear la amplitud en tiempo real para visualizadores de voz y grabar archivos de sonido (`.m4a` / `.aac`).

---

## 1. El subsistema de Audio en Android

Android ofrece dos APIs para interactuar con el micrófono:
- **`MediaRecorder`**: API de alto nivel. Graba audio comprimido directamente a un archivo de disco (ideal para notas de voz).
- **`AudioRecord`**: API de bajo nivel. Graba datos PCM en bruto en búferes de memoria (ideal para procesamiento de voz en tiempo real, ecualizadores o análisis de frecuencia FFT).

---

## 2. Permisos en `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

---

## 3. Grabación de notas de voz con `MediaRecorder`

```kotlin
class GestorGrabadoraAudio(private val context: Context) {
    private var recorder: MediaRecorder? = null

    fun iniciarGrabacion(archivoSalida: File) {
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(archivoSalida.absolutePath)

            try {
                prepare()
                start()
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Fallo al preparar o iniciar la grabación", e)
            }
        }
    }

    fun obtenerAmplitudMaxima(): Int {
        return recorder?.maxAmplitude ?: 0 // Devuelve la amplitud del micrófono de 0 a 32767
    }

    fun detenerGrabacion() {
        try {
            recorder?.stop()
            recorder?.release()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error al detener grabadora", e)
        } finally {
            recorder = null
        }
    }
}
```

---

## 4. Visualizador de Onda de Voz en Compose

```kotlin
@Composable
fun VisualizadorOndaVoz(amplitudNormalizada: Float) {
    // amplitudNormalizada debe ir de 0.0f a 1.0f
    val alturaBarras by animateFloatAsState(
        targetValue = amplitudNormalizada * 100dp.value,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "AmplitudOnda"
    )

    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(alturaBarras.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Grabación de Audio con MediaRecorder: <https://developer.android.com/develop/ui/views/media/mediarecorder>
- API AudioRecord en Android: <https://developer.android.com/reference/android/media/AudioRecord>
