# Guía 37 — Hardware: Periféricos USB Host, Pantalla y Sensores de Salud

Objetivo: dominar la interacción con periféricos conectados por puerto USB-C (USB Host), pantallas externas y tasa de refresco adaptativa (90Hz/120Hz/144Hz) e integración con sensores biométricos de salud (Health Connect).

---

## 1. Conexión de Periféricos por USB Host (`UsbManager`)

Android puede actuar como puerto anfitrión (*USB Host Mode*) para comunicarse con lectores de tarjetas de crédito externos, lectores de código de barras USB, placas Arduino / microcontroladores o interfaces de audio profesionales mediante el conector USB-C:

```xml
<uses-feature android:name="android.hardware.usb.host" />
```

```kotlin
fun listarDispositivosUsbConectados(context: Context) {
    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList

    deviceList.values.forEach { device ->
        Log.d("USB", "Dispositivo USB detectado: ${device.deviceName} (VendorID: ${device.vendorId}, ProductID: ${device.productId})")
    }
}
```

---

## 2. Tasa de Refresco de Pantalla Adaptativa (`DisplayManager`)

Los smartphones modernos cuentan con pantallas OLED de alta tasa de refresco (90Hz, 120Hz, 144Hz con LTPO dinámico de 1Hz a 120Hz). Es posible consultar las capacidades del hardware de pantalla mediante `DisplayManager`:

```kotlin
fun obtenerModosPantalla(context: Context) {
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

    val refreshRateActual = defaultDisplay.refreshRate
    Log.d("Display", "Tasa de refresco actual: $refreshRateActual Hz")

    val supportedModes = defaultDisplay.supportedModes
    supportedModes.forEach { mode ->
        Log.d("Display", "Modo soportado: ${mode.physicalWidth}x${mode.physicalHeight} @ ${mode.refreshRate}Hz")
    }
}
```

---

## 3. Integración con Sensores de Salud y Wearables (Health Connect)

En lugar de leer directamente los sensores ópticos de frecuencia cardíaca o pulsioximetría (SpO2) de los smartwatches, Android centraliza la lectura mediante la API unificada **Health Connect**:

```kotlin
dependencies {
    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")
}
```

```kotlin
suspend fun leerRitmoCardiaco(context: Context): List<HeartRateRecord> {
    val healthConnectClient = HealthConnectClient.getOrCreate(context)

    val request = ReadRecordsRequest(
        recordType = HeartRateRecord::class,
        timeRangeFilter = TimeRangeFilter.after(Instant.now().minus(7, ChronoUnit.DAYS))
    )

    val response = healthConnectClient.readRecords(request)
    return response.records
}
```

---

## Resumen de la Serie Completa de Hardware (Guías 28 a 37):

| Guía | Subsistema de Hardware cubierto | Componente principal / API |
|---|---|---|
| **28** | Ubicación y GPS | `FusedLocationProviderClient`, GPS/GLONASS/Galileo |
| **29** | Movimiento y Posición | Acelerómetro, Giroscopio, Brújula, Podómetro (`SensorManager`) |
| **30** | Entorno y Proximidad | Luz ambiental (Lux), Proximidad, Barómetro (Altitud) |
| **31** | Cámara y Flash | `CameraX` (`PreviewView`, `ImageCapture`), Control de Linterna |
| **32** | Micrófono y Audio | `MediaRecorder`, `AudioRecord`, Amplitud de onda de voz |
| **33** | Biometría y Criptografía | `BiometricPrompt` (Huella/Rostro) + TEE / StrongBox |
| **34** | Radio Inalámbrica | Bluetooth Clásico + Bluetooth Low Energy (BLE GATT Scanner) |
| **35** | Radio de Corto Alcance | NFC (`NfcAdapter`, NDEF Tags, Card Emulation HCE) |
| **36** | Alimentación y Térmico | `BatteryManager`, Motores Hápticos (`VibrationEffect`), `ThermalManager` |
| **37** | Conectividad y Display | `UsbManager` (Host), Tasa de Refresco Display, Health Connect |

---

## Fuentes consultadas (18-07-2026)

- Modo USB Host en Android: <https://developer.android.com/develop/connectivity/usb/host>
- Documentación de Pantalla y DisplayManager: <https://developer.android.com/reference/android/hardware/display/DisplayManager>
- API Health Connect de Android: <https://developer.android.com/health-and-fitness/guides/health-connect>
