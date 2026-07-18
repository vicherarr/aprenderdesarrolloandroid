# Guía 27 — Librerías y Componentes UI de Terceros Gratuito para Producción

Objetivo: analizar las librerías de terceros *open source* y gratuitas más potentes y consolidadas en el ecosistema Android moderno para resolver problemas complejos de UI (carga de imágenes remotas, animaciones vectoriales, permisos de usuario y componentes avanzados) con código listo para producción.

---

## 1. El criterio de selección de librerías de terceros

No todas las librerías de GitHub deben terminar en tu archivo `build.gradle.kts`. Para que una librería sea apta para proyectos reales debe cumplir:
1. **Gratuita y Open Source** (Licencias Apache 2.0 o MIT).
2. **Desarrollada en Kotlin nativo para Compose** (sin wrappers pesados ni interop con Views antiguas).
3. **Alto mantenimiento y respaldo de la comunidad** (Google, Airbnb, Square, desarrolladores reconocidos).
4. **Impacto mínimo en el tamaño del APK** y sin fugas de memoria.

---

## 2. Carga y Caché de Imágenes: Coil (`coil-kt`)

`Coil` (Coroutine Image Loader) es el estándar *de facto* respaldado por Google para cargar imágenes de red, locales y SVGs en Jetpack Compose.

### Instalación (`build.gradle.kts`):
```kotlin
implementation("io.coil-kt:coil-compose:2.6.0")
implementation("io.coil-kt:coil-svg:2.6.0") // Soporte opcional para archivos SVG
```

### Uso en Compose (`AsyncImage`):
```kotlin
@Composable
fun AvatarUsuarioRemoto(urlImagen: String, nombre: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(urlImagen)
            .crossfade(true) // Animación suave al cargar
            .placeholder(R.drawable.ic_avatar_placeholder)
            .error(R.drawable.ic_avatar_error)
            .build(),
        contentDescription = "Foto de perfil de $nombre",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
    )
}
```

---

## 3. Animaciones Vectoriales Complejas: Lottie (`lottie-compose`)

`Lottie` de Airbnb permite renderizar animaciones diseñadas en Adobe After Effects o lottiefiles.com (formato JSON o `.lottie`) en tiempo real sin perder resolución.

### Instalación:
```kotlin
implementation("com.airbnb.android:lottie-compose:6.4.0")
```

### Uso en Compose:
```kotlin
@Composable
fun AnimacionExitoOnboarding() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.animacion_exito))
    val progreso by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever // Bucle infinito o número de repeticiones
    )

    LottieAnimation(
        composition = composition,
        progress = { progreso },
        modifier = Modifier.size(200.dp)
    )
}
```

---

## 4. Gestión Elegante de Permisos: Accompanist Permissions

`Accompanist` es el laboratorio oficial de Google para probar componentes que terminarán integrándose en Jetpack Compose. `accompanist-permissions` permite solicitar permisos de cámara, ubicación o notificaciones de forma declarativa.

### Instalación:
```kotlin
implementation("com.google.accompanist:accompanist-permissions:0.34.0")
```

### Uso en Compose:
```kotlin
@OptIn(ExperimentalAccompanistPermissionsApi::class)
@Composable
fun SolicitarPermisoCamara(onCamaraConcedida: @Composable () -> Unit) {
    val camaraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    when {
        camaraPermissionState.status.isGranted -> {
            onCamaraConcedida()
        }
        camaraPermissionState.status.shouldShowRationale -> {
            Column {
                Text("La cámara es requerida para escanear el código QR.")
                Button(onClick = { camaraPermissionState.launchPermissionRequest() }) {
                    Text("Reintentar")
                }
            }
        }
        else -> {
            Button(onClick = { camaraPermissionState.launchPermissionRequest() }) {
                Text("Permitir acceso a la cámara")
            }
        }
    }
}
```

---

## 5. Efectos de Carga Shimmer: `valentinilk/shimmer`

En lugar de mostrar un spinner de carga genérico, las apps modernas (Facebook, YouTube) utilizan esqueletos parpadeantes (*Shimmer Placeholders*) mientras descargan datos de red.

### Instalación:
```kotlin
implementation("com.valentinilk.shimmer:compose-shimmer:1.3.0")
```

### Uso en Compose:
```kotlin
@Composable
fun TarjetaEsqueletoCargando() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shimmer() // Aplica el barrido de luz animado a todos los hijos
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(width = 120.dp, height = 16.dp).background(Color.LightGray))
            Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(Color.LightGray))
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp).background(Color.LightGray))
        }
    }
}
```

---

## 6. Selección de Fechas y Horas avanzadas: Sheets Modal (`sheets-compose-dialogs`)

Aunque Material 3 incluye DatePickers básicos, `sheets-compose-dialogs` de Max Keppeler ofrece selectores altamente pulidos de fechas, horas, colores y opciones con integración M3 nativa.

### Instalación:
```kotlin
implementation("com.maxkeppeler.sheets-compose-dialogs:core:1.3.0")
implementation("com.maxkeppeler.sheets-compose-dialogs:calendar:1.3.0")
```

### Resumen de la Suite Recomendada para Producción:

| Necesidad de la App | Solución de Terceros Recomendada | Licencia |
|---|---|---|
| Carga e imagen en caché | **Coil** (`coil-kt`) | Apache 2.0 |
| Animaciones vectoriales interactivas | **Lottie** (`lottie-compose`) | Apache 2.0 |
| Permisos de runtime de Android | **Accompanist Permissions** | Apache 2.0 |
| Placeholders de carga elegante | **Compose Shimmer** | MIT |
| Visor de fotos multitáctil | **Telephoto Zoomable** (`me.saket.telephoto`) | Apache 2.0 |

---

## Fuentes consultadas (18-07-2026)

- Documentación oficial de Coil: <https://coil-kt.github.io/coil/compose/>
- Lottie para Android Compose: <https://github.com/airbnb/lottie-android>
- Repositorio Accompanist de Google: <https://github.com/google/accompanist>
- Telephoto Zoomable por Saket Narayan: <https://github.com/saket/telephoto>
