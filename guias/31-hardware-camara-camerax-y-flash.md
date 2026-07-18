# Guía 31 — Hardware: Cámara, CameraX, Escaneo de Códigos y Flash

Objetivo: dominar la integración del hardware de cámara en Android usando CameraX para la vista previa en directo en Jetpack Compose, captura de fotos, control de la linterna/flash y análisis de fotogramas en tiempo real.

---

## 1. La suite de CameraX

CameraX es la librería Jetpack recomendada para interactuar con los sensores de cámara del dispositivo de forma compatible en el 98%+ del ecosistema Android:

- **`Preview`**: Muestra la imagen capturada por el sensor en vivo.
- **`ImageCapture`**: Captura fotos de alta resolución y las guarda en disco/Media Provider.
- **`ImageAnalysis`**: Procesa fotogramas sin comprimir en tiempo real (útil para IA / ML Kit / QR).

---

## 2. Permisos y Dependencias (`build.gradle.kts` y `AndroidManifest.xml`)

```xml
<uses-feature android:name="android.hardware.camera.any" />
<uses-permission android:name="android.permission.CAMERA" />
```

```kotlin
dependencies {
    val cameraxVersion = "1.3.2"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
}
```

---

## 3. Vista previa de Cámara en Compose (`PreviewView`)

```kotlin
@Composable
fun VistaPreviaCamara(
    modifier: Modifier = Modifier,
    onFotoCapturada: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val selector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        Log.e("CameraX", "Error al vincular cámara", e)
                    }
                }, executor)

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Botón de captura flotante
        IconButton(
            onClick = { onFotoCapturada(imageCapture) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .size(72.dp)
                .background(Color.White, CircleShape)
        ) {
            Icon(Icons.Default.Camera, contentDescription = "Tomar foto", tint = Color.Black)
        }
    }
}
```

---

## 4. Control de la Linterna / Flash (`CameraControl`)

```kotlin
fun alternarLinterna(context: Context, activar: Boolean) {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraId = cameraManager.cameraIdList[0] // Cámara trasera por defecto
    cameraManager.setTorchMode(cameraId, activar)
}
```

---

## Fuentes consultadas (18-07-2026)

- Arquitectura y componentes de CameraX: <https://developer.android.com/develop/ui/views/camerax>
- Vista previa de cámara en Compose: <https://developer.android.com/develop/ui/views/camerax/preview>
