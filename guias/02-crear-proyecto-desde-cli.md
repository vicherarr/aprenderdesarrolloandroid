# Guía 02 — Crear un proyecto Android básico desde la terminal

Objetivo: crear el proyecto `HolaAndroid` (plantilla *Empty Activity* con Kotlin
y Jetpack Compose, la misma que genera Android Studio) sin abrir el IDE, y
compilarlo hasta obtener un APK. Veremos dos caminos: **Android CLI** (la
herramienta moderna de Google) y el proyecto que ya viene en este repositorio.

## 1. La forma moderna: Android CLI

**Android CLI** es la interfaz de línea de comandos que Google presentó en 2026
para unificar todo el flujo de desarrollo Android desde la terminal. Sustituye a
la colección dispersa de scripts (`sdkmanager`, `avdmanager`, `adb`…) por un
único binario, `android`, con subcomandos coherentes.

Nació con un objetivo declarado: ser *agent-friendly*, es decir, pensada para que
tanto tú como un agente de IA (Claude Code, Gemini o Codex) puedan crear
proyectos, gestionar emuladores e instalar componentes del SDK de forma
predecible y automatizable. Todavía está en evolución, así que consulta
`android <comando> -h` si alguna opción no coincide con lo que aquí se muestra.

### 1.1. Obtenerla y comprobarla

Se descarga desde <https://developer.android.com/tools/agents>, se coloca el
binario `android` en el `PATH` (ver variables de entorno en la Guía 01) y se
verifica:

```bash
android --version     # versión instalada
android info          # SDK que está usando (debería apuntar a ~/Android/Sdk)
```

> Android CLI no reemplaza al SDK ni a Gradle: se apoya en el SDK que ya
> instalaste en la Guía 01 y **no compila** por sí misma. Envuelve las
> herramientas clásicas y delega la construcción en Gradle.

### 1.2. Crear el proyecto

```bash
android create list                                    # plantillas disponibles
android create --name=com.aprender.holaandroid \
               --output=./HolaAndroid \
               empty-activity-agp-9                     # genera el proyecto
```

Esto produce un proyecto equivalente al que crearía Android Studio con *Empty
Activity*: Kotlin, Compose y Gradle Wrapper incluidos. Añade `--dry-run
--verbose` si quieres ver qué archivos crearía sin escribir nada.

### 1.3. Cómo se reparte el trabajo con las herramientas clásicas

| Tarea | Herramienta clásica | Equivalente en Android CLI |
|---|---|---|
| Instalar paquetes del SDK | `sdkmanager` | `android sdk install …` |
| Crear/arrancar emuladores | `avdmanager` + `emulator` | `android emulator create` / `start` |
| Compilar el APK | `./gradlew assembleDebug` | *(sigue siendo Gradle)* |
| Instalar y lanzar la app | `adb install` + `am start` | `android run --apks=…` |

En resumen: **Android CLI para andamiar, gestionar SDK y desplegar; Gradle para
compilar.** El resto de la guía disecciona el proyecto resultante y usa el
`./gradlew` que trae el propio proyecto.

## 2. Estructura del proyecto

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

## 3. Versiones usadas

| Componente | Versión |
|---|---|
| Gradle | 8.13 |
| Android Gradle Plugin (AGP) | 8.11.1 |
| Kotlin | 2.1.21 |
| compileSdk / targetSdk | 36 |
| minSdk | 24 |
| Compose BOM | 2025.06.00 |

## 4. Compilar

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

## 5. Instalar en un dispositivo o emulador

Con las herramientas clásicas:

```bash
adb devices                                   # ver dispositivos conectados
./gradlew installDebug                        # compilar + instalar
adb shell am start -n com.aprender.holaandroid/.MainActivity   # lanzar la app
```

O bien, tras compilar con Gradle, desplegar el APK con Android CLI:

```bash
android run --apks=app/build/outputs/apk/debug/app-debug.apk
```

## 6. Abrir en Android Studio

El proyecto es 100 % compatible con el IDE: en Android Studio usa
**File → Open** y selecciona la carpeta `HolaAndroid`.
