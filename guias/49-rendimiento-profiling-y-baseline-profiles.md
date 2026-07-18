# Guía 49 — Rendimiento, Profiling y Baseline Profiles (`LeakCanary` & `Macrobenchmark`)

Objetivo: detectar fugas de memoria en desarrollo, diagnosticar cuellos de botella mediante Android Studio Profiler y acelerar el tiempo de arranque de la app (*cold startup*) hasta un 40% mediante la generación de **Baseline Profiles**.

---

## 1. Detección de Fugas de Memoria en Desarrollo (`LeakCanary`)

Una fuga de memoria (*Memory Leak*) ocurre cuando un objeto inaccesible (como un `Context` de Activity o un Listener) queda retenido por una referencia en segundo plano, impidiendo que el Recolector de Basura (GC) libere la memoria RAM.

### Instalación (`build.gradle.kts`):

```kotlin
dependencies {
    // Solo en debugImplementation para que NO vaya a producción
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}
```

Al compilar en modo debug, LeakCanary detectará fugas automáticamente y mostrará la cadena exacta de referencias que provocó la fuga con una notificación nativa.

---

## 2. Diagnóstico con Android Studio Profiler

Android Studio Profiler permite monitorizar en tiempo real durante la ejecución:

- **CPU Profiler**: Rastrea hilos del sistema y mide tiempos de ejecución de funciones Kotlin.
- **Memory Profiler**: Muestra asignación de objetos en el Heap y permite tomar *Heap Dumps*.
- **Network Profiler**: Visualiza el payload, cabeceras y tamaño de peticiones HTTP en vivo.

---

## 3. Optimización de Tiempo de Arranque con Baseline Profiles

Jetpack Compose se ejecuta en la JVM ART mediante interpretación en caliente (JIT). Los **Baseline Profiles** indican a la plataforma Android exactamente qué clases y métodos Compose deben precompilarse a código máquina AOT (*Ahead-Of-Time*) durante la instalación de la app desde Google Play.

### Instalación de módulo Macrobenchmark (`build.gradle.kts`):

```kotlin
dependencies {
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
}
```

### Generación del perfil con Macrobenchmark:

```kotlin
@RunWith(AndroidJUnit4::class)
class GeneradorBaselineProfile {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generarPerfilArranque() = rule.collect(
        packageName = "com.aprender.holaandroid",
        profileBlock = {
            // Iniciar la app y navegar por el flujo principal de inicio
            pressHome()
            startActivityAndWait()
        }
    )
}
```

---

## Fuentes consultadas (18-07-2026)

- Baseline Profiles en Compose: <https://developer.android.com/topic/performance/baselineprofiles/overview>
- Detección de fugas con LeakCanary: <https://square.github.io/leakcanary/>
