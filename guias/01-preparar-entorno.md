# Guía 01 — Preparar el entorno Android en Linux (CachyOS)

Objetivo: dejar el equipo listo para compilar proyectos Android desde la
terminal, sin depender de abrir Android Studio.

## 1. Qué necesitamos

| Herramienta | Para qué sirve | Dónde está en este equipo |
|---|---|---|
| Android SDK | Plataformas, build-tools, emulador | `~/Android/Sdk` |
| JDK (Java 21) | Ejecutar Gradle y compilar | JBR incluido con Android Studio |
| cmdline-tools | `sdkmanager` y `avdmanager` | `~/Android/Sdk/cmdline-tools/latest` |
| Android CLI | Binario `android`: andamiar proyectos, SDK, emuladores y despliegue en un solo comando | `~/.local/bin/android` |
| Gradle | Sistema de construcción | No se instala: cada proyecto trae su *wrapper* (`./gradlew`) |

> Android Studio está instalado con **JetBrains Toolbox**, por eso su JDK está en
> `~/.local/share/JetBrains/Toolbox/apps/android-studio/jbr` y no en `/opt`.

## 2. Variables de entorno

En `~/.zshrc` y `~/.bashrc`:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export JAVA_HOME="$HOME/.local/share/JetBrains/Toolbox/apps/android-studio/jbr"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

En `~/.config/fish/config.fish`:

```fish
set -gx ANDROID_HOME "/home/victor/Android/Sdk"
set -gx JAVA_HOME "/home/victor/.local/share/JetBrains/Toolbox/apps/android-studio/jbr"
fish_add_path $JAVA_HOME/bin $ANDROID_HOME/emulator $ANDROID_HOME/platform-tools $ANDROID_HOME/cmdline-tools/latest/bin
```

Abre una terminal nueva (o `source ~/.zshrc`) para que apliquen.

## 3. Instalar cmdline-tools (si faltan)

Las *command-line tools* traen `sdkmanager`. Se descargan de
<https://developer.android.com/studio#command-line-tools-only> y se instalan así:

```bash
curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip" -o /tmp/cmdline-tools.zip
cd /tmp && unzip -q cmdline-tools.zip
mkdir -p ~/Android/Sdk/cmdline-tools
mv cmdline-tools ~/Android/Sdk/cmdline-tools/latest
```

Ojo al detalle: el contenido del zip debe quedar en un subdirectorio llamado
**`latest`**, no directamente en `cmdline-tools/`.

## 4. Comprobar que todo funciona

```bash
java -version          # openjdk 21.x
adb --version          # Android Debug Bridge ...
sdkmanager --version   # 19.0 o superior
sdkmanager --list_installed   # plataformas y build-tools instalados
android --version      # 1.0.x — Android CLI de Google
android info           # confirma que apunta a ~/Android/Sdk
```

Si todos los comandos responden, el entorno está listo.

> **Android CLI** (herramienta reciente de Google, 2026) es opcional pero
> recomendable: unifica `sdkmanager`, `avdmanager` y `adb` bajo el binario
> `android`. Se descarga de <https://developer.android.com/tools/agents> y se
> coloca en el `PATH`. La usamos en la Guía 02 para crear el proyecto.
