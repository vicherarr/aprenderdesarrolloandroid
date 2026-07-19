# Guía 01 — Preparar el entorno Android (Linux, macOS y Windows)

Objetivo: dejar el equipo listo para compilar proyectos Android desde la
terminal, sin depender de abrir Android Studio.

> **Nota sobre estos tutoriales.** Todo el material se ha preparado y probado
> sobre **Linux (CachyOS)**. Aun así, las herramientas son las mismas en las tres
> plataformas: aquí se muestran los comandos y rutas equivalentes para **Linux,
> macOS y Windows**, y salvo detalles de rutas todo funciona igual.

## 1. Qué necesitamos

| Herramienta | Para qué sirve |
|---|---|
| Android SDK | Plataformas, build-tools, emulador |
| JDK (Java 21) | Ejecutar Gradle y compilar |
| cmdline-tools | `sdkmanager` y `avdmanager` |
| Android CLI | Binario `android`: andamiar proyectos, SDK, emuladores y despliegue en un solo comando |
| Gradle | Sistema de construcción (no se instala: cada proyecto trae su *wrapper* `./gradlew`) |

Las rutas por defecto dependen del sistema operativo:

| | Android SDK | JDK (JBR de Android Studio) |
|---|---|---|
| **Linux** | `~/Android/Sdk` | `~/android-studio/jbr` |
| **macOS** | `~/Library/Android/sdk` | `/Applications/Android Studio.app/Contents/jbr/Contents/Home` |
| **Windows** | `%LOCALAPPDATA%\Android\Sdk` | `C:\Program Files\Android\Android Studio\jbr` |

> Android Studio incluye su propio JDK (**JBR**, JetBrains Runtime), así que no
> necesitas instalar Java aparte. En el equipo donde se prepararon estas guías,
> Android Studio está instalado con **JetBrains Toolbox**, por eso allí el JBR
> vive en `~/.local/share/JetBrains/Toolbox/apps/android-studio/jbr`. Ajusta la
> ruta a donde tengas tu Android Studio.

## 2. Variables de entorno

Define `ANDROID_HOME` (ruta al SDK) y `JAVA_HOME` (ruta al JBR) según la tabla
anterior, y añade sus binarios al `PATH`.

**Linux / macOS** — en `~/.bashrc` o `~/.zshrc`:

```bash
# Ajusta ANDROID_HOME según tu SO:
#   Linux:  $HOME/Android/Sdk
#   macOS:  $HOME/Library/Android/sdk
export ANDROID_HOME="$HOME/Android/Sdk"
export JAVA_HOME="$HOME/android-studio/jbr"   # o la ruta de tu Android Studio
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

**Linux / macOS con fish** — en `~/.config/fish/config.fish`:

```fish
set -gx ANDROID_HOME "$HOME/Android/Sdk"
set -gx JAVA_HOME "$HOME/android-studio/jbr"
fish_add_path $JAVA_HOME/bin $ANDROID_HOME/emulator $ANDROID_HOME/platform-tools $ANDROID_HOME/cmdline-tools/latest/bin
```

**Windows** — en PowerShell (persistente, con `setx`):

```powershell
setx ANDROID_HOME "$env:LOCALAPPDATA\Android\Sdk"
setx JAVA_HOME "C:\Program Files\Android\Android Studio\jbr"
setx PATH "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\emulator;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:PATH"
```

Abre una terminal nueva para que apliquen (en Linux/macOS también puedes hacer
`source ~/.bashrc`).

## 3. Instalar cmdline-tools (si faltan)

Las *command-line tools* traen `sdkmanager`. Se descargan de
<https://developer.android.com/studio#command-line-tools-only> — hay un zip
distinto por sistema operativo (`linux`, `mac`, `win`).

**Linux / macOS:**

```bash
# Cambia "linux" por "mac" en macOS:
curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip" -o /tmp/cmdline-tools.zip
cd /tmp && unzip -q cmdline-tools.zip
mkdir -p "$ANDROID_HOME/cmdline-tools"
mv cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
```

**Windows (PowerShell):**

```powershell
Invoke-WebRequest "https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip" -OutFile "$env:TEMP\cmdline-tools.zip"
Expand-Archive "$env:TEMP\cmdline-tools.zip" -DestinationPath "$env:TEMP\cmdline-tools-extract"
New-Item -ItemType Directory -Force "$env:ANDROID_HOME\cmdline-tools" | Out-Null
Move-Item "$env:TEMP\cmdline-tools-extract\cmdline-tools" "$env:ANDROID_HOME\cmdline-tools\latest"
```

Ojo al detalle: el contenido del zip debe quedar en un subdirectorio llamado
**`latest`**, no directamente en `cmdline-tools/`.

## 4. Comprobar que todo funciona

Estos comandos son idénticos en las tres plataformas:

```bash
java -version          # openjdk 21.x
adb --version          # Android Debug Bridge ...
sdkmanager --version   # 19.0 o superior
sdkmanager --list_installed   # plataformas y build-tools instalados
android --version      # 1.0.x — Android CLI de Google
android info           # confirma a qué SDK apunta
```

Si todos los comandos responden, el entorno está listo.

> **Android CLI** (herramienta reciente de Google, 2026) es opcional pero
> recomendable: unifica `sdkmanager`, `avdmanager` y `adb` bajo el binario
> `android`. Se descarga de <https://developer.android.com/tools/agents> (hay
> versión para Linux, macOS y Windows) y se coloca en el `PATH`. La usamos en la
> Guía 02 para crear el proyecto.
