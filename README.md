# Aprender Desarrollo Android

Repositorio de aprendizaje de desarrollo Android. Contiene proyectos de ejemplo y
guías sencillas, probadas paso a paso en este mismo equipo (CachyOS + Android
Studio + Android SDK cmdline-tools).

## Estructura

- `HolaAndroid/` — Primer proyecto: plantilla básica *Empty Activity* (Kotlin + Jetpack Compose).
- `guias/` — Guías paso a paso de lo que se va aprendiendo:
  - `01-preparar-entorno.md` — Entorno Android en Linux.
  - `02-crear-proyecto-desde-cli.md` — Proyecto desde terminal.
  - `03-habilitar-hilt.md` — Configuración de Hilt.
  - `04-usar-hilt.md` — Inyección de dependencias con Hilt.
  - `05-arquitectura-recomendada.md` — Arquitectura (UI, ViewModel, Repository, Domain).
  - `06-retrofit.md` — Retrofit y peticiones HTTP.
  - `07-room.md` — Persistencia local con Room.
  - `08-migraciones-room.md` — Migraciones en Room.
  - `09-logging.md` — Logging con Timber.
  - `10-tests-unitarios.md` — Tests unitarios en JVM.
  - `11-tests-viewmodel-flow.md` — Testing de ViewModels y Flows.
  - `12-tests-de-red-mockwebserver.md` — Testing de red con MockWebServer.
  - `13-tests-instrumentados-room.md` — Tests instrumentados con Room.
  - `14-tests-ui-compose.md` — Tests de UI en Jetpack Compose.
  - `15-ci-github-actions.md` — Integración Continua con GitHub Actions.

## Requisitos del equipo

- Android SDK en `~/Android/Sdk` (variable `ANDROID_HOME`).
- JDK 21 (el JBR incluido con Android Studio, variable `JAVA_HOME`).
- `adb`, `emulator` y `sdkmanager` en el `PATH`.

Ver [guias/01-preparar-entorno.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/01-preparar-entorno.md) para el detalle de la configuración.

## Compilar el primer proyecto

```bash
cd HolaAndroid
./gradlew assembleDebug
```

El APK resultante queda en `HolaAndroid/app/build/outputs/apk/debug/`.

