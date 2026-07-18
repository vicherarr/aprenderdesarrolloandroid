# Guía 34 — Hardware: Bluetooth y Bluetooth Low Energy (BLE)

Objetivo: aprender a escanear, conectarse y comunicarse con dispositivos inalámbricos mediante Bluetooth Clásico y Bluetooth Low Energy (BLE) a través del chip de radiofrecuencia del móvil.

---

## 1. El chip Bluetooth y BLE en Android

- **Bluetooth Classic**: Diseñado para transmisión continua de datos a alta velocidad (auriculares de audio, altavoces, periféricos de entrada).
- **Bluetooth Low Energy (BLE)**: Diseñado para dispositivos con batería de botón (pulseras cuantificadoras, sensores médicos, *Beacons*) con transferencias periódicas de paquetes pequeños mediante el protocolo GATT (*Generic Attribute Profile*).

---

## 2. Permisos según versión de Android (`AndroidManifest.xml`)

A partir de Android 12 (API 31), se requieren permisos específicos de Bluetooth sin requerir permisos de localización física:

```xml
<!-- Permisos para Android 12+ (API 31+) -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

<!-- Compatibilidad con Android 11 o inferior -->
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />
```

---

## 3. Comprobar si el Radio Bluetooth está activado

```kotlin
fun verificarBluetoothActivado(context: Context): Boolean {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter = bluetoothManager.adapter
    return adapter != null && adapter.isEnabled
}
```

---

## 4. Escaneo de Dispositivos BLE (`BluetoothLeScanner`)

```kotlin
@SuppressLint("MissingPermission")
fun iniciarEscaneoBLE(
    context: Context,
    onDispositivoEncontrado: (ScanResult) -> Unit
) {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val scanner: BluetoothLeScanner? = bluetoothManager.adapter?.bluetoothLeScanner

    val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { onDispositivoEncontrado(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLE", "Fallo en escaneo BLE: código $errorCode")
        }
    }

    val settings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    scanner?.startScan(null, settings, scanCallback)
}
```

---

## Fuentes consultadas (18-07-2026)

- Generalidades sobre Bluetooth en Android: <https://developer.android.com/develop/connectivity/bluetooth>
- Bluetooth Low Energy (BLE): <https://developer.android.com/develop/connectivity/bluetooth/ble/ble-overview>
