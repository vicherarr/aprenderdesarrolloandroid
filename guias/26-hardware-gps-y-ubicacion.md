# Guía 26 — Hardware: GPS y Geolocalización (`FusedLocationProviderClient`)

Objetivo: dominar el hardware de posicionamiento global (GPS, GLONASS, Galileo) e integración de ubicación precisa y de bajo consumo en Android utilizando Google Play Services Fused Location Provider.

---

## 1. El subsistema de posición en Android

Android obtiene la ubicación a través de múltiples fuentes de hardware:
- **GPS / GNSS**: Satélites (alta precisión ~3-5m, alto consumo de batería, solo exteriores).
- **Redes Móviles (Triangulación de Antenas)**: Coarse location (~100m-1km, consumo medio).
- **Wi-Fi / Bluetooth Beacons**: Posicionamiento en interiores y zonas urbanas (~10-50m).

El cliente `FusedLocationProviderClient` combina automáticamente estas fuentes en segundo plano para maximizar la precisión al menor consumo energético.

---

## 2. Permisos en Manifest (`AndroidManifest.xml`)

```xml
<!-- Ubicación aproximada (Red / Wi-Fi) -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Ubicación de alta precisión (GPS por Hardware) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- Ubicación en segundo plano (Android 10+ / API 29+) -->
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

---

## 3. Obtener la última posición conocida (`getLastLocation`)

```kotlin
class UbicaciónManager(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun obtenerUltimaUbicacion(
        onExito: (Location) -> Unit,
        onError: (Exception) -> Unit
    ) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    onExito(location)
                } else {
                    onError(Exception("No hay ubicación disponible en caché"))
                }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}
```

---

## 4. Actualizaciones de Ubicación en Tiempo Real (Tracking GPS)

Para rastrear movimientos (ej. apps de deportes o mapas), se suscribe un `LocationCallback` con `LocationRequest`:

```kotlin
@SuppressLint("MissingPermission")
fun iniciarSeguimientoUbicacion(
    context: Context,
    onUbicacionRecibida: (Location) -> Unit
): LocationCallback {
    val client = LocationServices.getFusedLocationProviderClient(context)

    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L) // Cada 5 segundos
        .setMinUpdateIntervalMillis(2000L) // Mínimo 2 segundos entre actualizaciones
        .setWaitForAccurateLocation(false)
        .build()

    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (location in result.locations) {
                onUbicacionRecibida(location)
            }
        }
    }

    client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    return callback // Guardar referencia para detener el rastreo con removeLocationUpdates
}
```

---

## 5. Integración con Jetpack Compose y Estado

```kotlin
@Composable
fun CoordenadasScreen(ubicacion: Location?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))

        if (ubicacion != null) {
            Text("Latitud: ${ubicacion.latitude}", style = MaterialTheme.typography.titleMedium)
            Text("Longitud: ${ubicacion.longitude}", style = MaterialTheme.typography.titleMedium)
            Text("Altitud: ${ubicacion.altitude} m", style = MaterialTheme.typography.bodyMedium)
            Text("Precisión: ±${ubicacion.accuracy} m", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Obteniendo señal GPS...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Documentación oficial de Fused Location Provider: <https://developer.android.com/develop/sensors-and-location/location/retrieve-current>
- Optimización de batería en servicios de ubicación: <https://developer.android.com/develop/sensors-and-location/location/battery>
