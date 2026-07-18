# Guía 02 — Crear un proyecto Android básico desde la terminal

Objetivo: crear el proyecto `HolaAndroid` (plantilla *Empty Activity* con Kotlin
y Jetpack Compose, la misma que genera Android Studio) sin abrir el IDE, y
compilarlo hasta obtener un APK.

## 1. Estructura del proyecto

```
HolaAndroid/
├── settings.gradle.kts          # Nombre del proyecto, módulos y repositorios
├── build.gradle.kts             # Plugins comunes (a nivel raíz, sin aplicar)
├── gradle.properties            # Opciones de Gradle/AndroidX
├── gradle/
│   ├── libs.versions.toml       # Catálogo de versiones (dependencias centralizadas)
│   └── wrapper/                 # Gradle Wrapper (jar + properties)
├── gradlew                      # Script que descarga y ejecuta Gradle
├── local.properties             # Ruta al SDK (NO se sube a git)
└── app/                         # El módulo de la aplicación
    ├── build.gradle.kts         # Config del módulo: SDK, dependencias, Compose
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/aprender/holaandroid/
        │   ├── MainActivity.kt          # Actividad principal (Compose)
        │   └── ui/theme/                # Tema Material 3
        └── res/                         # Recursos: strings, tema, icono
```

Conceptos clave:

- **Gradle Wrapper (`./gradlew`)**: cada proyecto fija su versión de Gradle y la
  descarga solo la primera vez. Por eso no hace falta instalar Gradle en el
  sistema.
- **Catálogo de versiones (`libs.versions.toml`)**: todas las versiones de
  plugins y librerías en un único archivo; los `build.gradle.kts` las
  referencian con `libs.xxx`.
- **`local.properties`**: contiene `sdk.dir=/home/victor/Android/Sdk`. Es
  específico de cada máquina y va en `.gitignore`.

## 2. Versiones usadas

| Componente | Versión |
|---|---|
| Gradle | 8.13 |
| Android Gradle Plugin (AGP) | 8.11.1 |
| Kotlin | 2.1.21 |
| compileSdk / targetSdk | 36 |
| minSdk | 24 |
| Compose BOM | 2025.06.00 |

## 3. Compilar

```bash
cd HolaAndroid
./gradlew assembleDebug
```

La primera ejecución tarda varios minutos: descarga la distribución de Gradle y
todas las dependencias. Las siguientes son mucho más rápidas gracias a la caché
(`~/.gradle`).

El APK queda en:

```
app/build/outputs/apk/debug/app-debug.apk
```

## 4. Instalar en un dispositivo o emulador

```bash
adb devices                                   # ver dispositivos conectados
./gradlew installDebug                        # compilar + instalar
adb shell am start -n com.aprender.holaandroid/.MainActivity   # lanzar la app
```

## 5. Abrir en Android Studio

El proyecto es 100 % compatible con el IDE: en Android Studio usa
**File → Open** y selecciona la carpeta `HolaAndroid`.
