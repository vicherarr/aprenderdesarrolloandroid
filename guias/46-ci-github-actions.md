# Guía 46 — Integración Continua (CI) con GitHub Actions: la suite automatizada

Objetivo: cerrar el ciclo de calidad automatizando la ejecución de la suite
completa de tests (25 unitarios + 12 instrumentados) y la generación del APK
de debug en cada `push` o `pull request`, garantizando que el código en `main`
siempre compila y pasa todas sus verificaciones.

Con esta guía culmina la serie de aprendizaje de Android:

```
    CI (GitHub Actions)     ← esta guía (ejecución automatizada sin humanos)
        UI (Compose)        ← guía 14 (8 tests, emulador)
      datos instrumentados  ← guía 13 (4 tests, emulador)
    red (MockWebServer)     ← guía 12 (6 tests, JVM)
  ViewModel y Flow          ← guía 11 (6 tests, JVM)
dominio y repositorio       ← guías 10 (13 tests, JVM)
```

---

## 1. Por qué CI: una suite que no se ejecuta no protege nada

En las guías 10 a 14 construimos 37 tests. Sin embargo, depender de que cada
desarrollador ejecute `./gradlew test` o `./gradlew connectedDebugAndroidTest`
en su equipo antes de subir cambios es frágil.

La **Integración Continua (CI)** resuelve esto:
1. **Ejecución automática**: Cada commit enviado a GitHub activa un ejecutor remoto (*runner*).
2. **Entorno limpio y reproducible**: Se compila en una máquina virtual aislada desde cero.
3. **Feedback rápido**: Notifica inmediatamente si un cambio rompe un test o impida compilar.
4. **Artefactos descargables**: Produce el archivo `.apk` listo para probar sin requerir compilación local.

---

## 2. El flujo de trabajo (`.github/workflows/ci.yml`)

GitHub Actions busca los archivos de configuración `.yml` en el directorio `.github/workflows/`.

El archivo `.github/workflows/ci.yml` define dos trabajos (*jobs*):

```yaml
name: Android CI

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
    branches: [ "main", "master" ]

jobs:
  unit-tests-and-build:
    name: Unit Tests & Build APK
    runs-on: ubuntu-latest

    defaults:
      run:
        working-directory: HolaAndroid

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Make gradlew executable
        run: chmod +x gradlew

      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest

      - name: Build Debug APK
        run: ./gradlew assembleDebug

      - name: Upload Test Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: unit-test-report
          path: HolaAndroid/app/build/reports/tests/testDebugUnitTest/

      - name: Upload Debug APK
        uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: HolaAndroid/app/build/outputs/apk/debug/app-debug.apk

  instrumented-tests:
    name: Instrumented Tests (Emulator)
    runs-on: ubuntu-latest

    defaults:
      run:
        working-directory: HolaAndroid

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Enable KVM for hardware acceleration
        run: |
          echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666", OPTIONS+="static_node=kvm"' | sudo tee /etc/udev/rules.d/99-kvm-rules.rules
          sudo udevadm control --reload-rules && sudo udevadm trigger --name-match=kvm

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Make gradlew executable
        run: chmod +x gradlew

      - name: Run Instrumented Tests on Android Emulator
        uses: ReactiveCircus/android-emulator-runner@v2
        with:
          api-level: 34
          target: aosp_atd
          arch: x86_64
          force-avd-creation: false
          emulator-options: -no-window -gpu swiftshader_indirect -no-audio -no-boot-anim -camera-back none
          disable-animations: true
          script: cd HolaAndroid && ./gradlew connectedDebugAndroidTest

      - name: Upload Instrumented Test Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: instrumented-test-report
          path: HolaAndroid/app/build/reports/androidTests/connected/
```

---

## 3. Desglose técnico de la configuración

| Concepto | Acción / Ajuste | Razón |
|---|---|---|
| **Subdirectorio de trabajo** | `working-directory: HolaAndroid` | El proyecto Gradle vive en `HolaAndroid/`, no en la raíz del repositorio Git. |
| **Java SDK** | `actions/setup-java@v4` con JDK 21 (Temurin) | Coincide con el JDK de Android Studio utilizado a lo largo de las guías. |
| **Caché de Gradle** | `gradle/actions/setup-gradle@v4` | Acción oficial de Gradle que gestiona la caché de dependencias y de build automáticamente, reduciendo tiempo de ejecución. |
| **Permisos de Gradle** | `chmod +x gradlew` | Asegura que el script Gradle Wrapper tenga permisos de ejecución en Linux. |
| **Tests de JVM** | `./gradlew testDebugUnitTest` | Corre los 25 tests unitarios (dominio, repositorios, ViewModel, MockWebServer). |
| **Generación del APK** | `./gradlew assembleDebug` | Genera el APK de prueba en `app/build/outputs/apk/debug/app-debug.apk`. |
| **Publicación de artefactos** | `actions/upload-artifact@v4` | Permite descargar los informes HTML de tests y el APK directamente desde la interfaz web de GitHub. |

---

## 4. Tests instrumentados en CI (Emulador con KVM)

Correr tests instrumentados en un servidor CI requiere un emulador de Android funcional:

1. **Aceleración por Hardware (KVM)**: En instancias Linux de GitHub Actions (`ubuntu-latest`), se habilita el acceso a `/dev/kvm` para permitir virtualización nativa por hardware.
2. **Imagen de sistema optimizada (`aosp_atd`)**: *Automated Test Development* (ATD) es una imagen ligera de AOSP optimizada para headless testing, libre de servicios pesados de fondo.
3. **Bandera `if: always()`**: El informe de tests instrumentados se sube como artefacto incluso si algún test falla, facilitando la depuración visual del informe HTML.

---

## 5. Resumen de comandos y reportes generados

| Tarea | Comando Gradle | Ruta del reporte / artefacto resultante |
|---|---|---|
| Tests Unitarios (JVM) | `./gradlew testDebugUnitTest` | `app/build/reports/tests/testDebugUnitTest/index.html` |
| Tests Instrumentados (Emulador) | `./gradlew connectedDebugAndroidTest` | `app/build/reports/androidTests/connected/debug/index.html` |
| APK Debug | `./gradlew assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` |

---

## 6. Probar y validar el workflow

1. **Comprobar sintaxis localmente**: Se puede utilizar la herramienta `act` (<https://github.com/nektos/act>) para simular GitHub Actions en la máquina local.
2. **Subir los cambios a GitHub**:
   ```bash
   git add .github/workflows/ci.yml guias/15-ci-github-actions.md
   git commit -m "feat: agregar Guía 15 y workflow de GitHub Actions"
   git push origin main
   ```
3. **Verificar en GitHub**: Entra a la pestaña **Actions** en tu repositorio de GitHub para observar la ejecución de los dos *jobs* en tiempo real.

---

## Fuentes consultadas (18-07-2026)

- Documentación oficial de GitHub Actions: <https://docs.github.com/en/actions>
- Acción de configuración de Gradle (`setup-gradle`): <https://github.com/gradle/actions>
- Acción para emulador Android (`android-emulator-runner`): <https://github.com/ReactiveCircus/android-emulator-runner>
- Guías oficiales de CI/CD para Android: <https://developer.android.com/studio/build/building-cmdline>

Con esta guía se completa la serie completa de aprendizaje de desarrollo en Android (Guías 01 a 15).
