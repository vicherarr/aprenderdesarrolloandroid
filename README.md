# Aprender Desarrollo Android

Repositorio de aprendizaje de desarrollo Android. Contiene proyectos de ejemplo y
guías sencillas, probadas paso a paso en este mismo equipo (CachyOS + Android
Studio + Android SDK cmdline-tools).

## Estructura

- `HolaAndroid/` — Primer proyecto: plantilla básica *Empty Activity* (Kotlin + Jetpack Compose).
- `guias/` — Guías paso a paso de lo que se va aprendiendo.

## Requisitos del equipo

- Android SDK en `~/Android/Sdk` (variable `ANDROID_HOME`).
- JDK 21 (el JBR incluido con Android Studio, variable `JAVA_HOME`).
- `adb`, `emulator` y `sdkmanager` en el `PATH`.

Ver `guias/01-preparar-entorno.md` para el detalle de la configuración.

## Compilar el primer proyecto

```bash
cd HolaAndroid
./gradlew assembleDebug
```

El APK resultante queda en `HolaAndroid/app/build/outputs/apk/debug/`.
