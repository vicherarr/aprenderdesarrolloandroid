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
  - `16-layouts-y-modificadores.md` — Fundamentos de Layouts y Modificadores (`Column`, `Row`, `Box`).
  - `17-botones-y-entradas.md` — Botones, Selección y Controles de Entrada (`Button`, `TextField`, `Switch`).
  - `18-tarjetas-listas-y-grids.md` — Tarjetas, Listas y Rejillas Eficientes (`Card`, `LazyColumn`, `LazyVerticalGrid`).
  - `19-scaffold-y-material3-structure.md` — Estructura de Pantalla M3 (`Scaffold`, `TopAppBar`, `NavigationBar`, `Drawer`).
  - `20-feedback-dialogos-y-bottomsheets.md` — Feedback de Usuario, Diálogos y Menús (`Snackbar`, `AlertDialog`, `ModalBottomSheet`).
  - `21-tabs-pagers-badges-y-chips.md` — Pestañas, Pagers, Badges e Indicadores (`TabRow`, `HorizontalPager`, `Badge`, `Chip`).
  - `22-tema-material3-y-dark-mode.md` — Sistema de Diseño Material 3 (`MaterialTheme`, Dark Mode, Dynamic Color).
  - `23-animaciones-ui-compose.md` — Animaciones UI (`animate*AsState`, `AnimatedVisibility`, `Crossfade`).
  - `24-canvas-y-dibujo-personalizado.md` — Dibujo Personalizado y Canvas (`Canvas`, `Path`, `Brush`).
  - `25-gestos-y-diseno-responsive.md` — Gestos Táctiles y Diseño Adaptativo (`pointerInput`, `WindowSizeClass`).
  - `26-integracion-profunda-material3.md` — Integración Profunda de Material 3 y Tokens de Diseño.
  - `27-librerias-y-componentes-de-terceros.md` — Librerías y Componentes UI de Terceros para Producción.
  - `28-hardware-gps-y-ubicacion.md` — GPS y Geolocalización (`FusedLocationProviderClient`).
  - `29-hardware-sensores-movimiento-y-posicion.md` — Sensores de Movimiento y Posición (Acelerómetro, Giroscopio, Brújula, Podómetro).
  - `30-hardware-sensores-ambientales-y-proximidad.md` — Sensores Ambientales y de Proximidad (Luz, Proximidad, Barómetro).
  - `31-hardware-camara-camerax-y-flash.md` — Cámara, CameraX, Escaneo de Códigos y Flash.
  - `32-hardware-microfono-y-audio.md` — Micrófono y Captura de Audio (`MediaRecorder`, `AudioRecord`).
  - `33-hardware-biometria-y-keystore.md` — Autenticación Biométrica y Android KeyStore (`BiometricPrompt`).
  - `34-hardware-bluetooth-y-ble.md` — Bluetooth y Bluetooth Low Energy (BLE).
  - `35-hardware-nfc-lectura-escritura.md` — NFC (Near Field Communication) Lectura y Escritura.
  - `36-hardware-bateria-haptica-y-termico.md` — Batería, Motores Hápticos y Estado Térmico.
  - `37-hardware-usb-salud-y-pantalla.md` — Periféricos USB Host, Pantalla y Sensores de Salud.
  - `38-ia-local-aicore-introduccion.md` — IA Local On-Device y Android AICore.
  - `39-ia-local-google-ai-edge-prompts.md` — Google AI Edge SDK y Generación en Streaming.
  - `40-ia-local-tareas-especializadas-sistema.md` — APIs Especializadas de IA (Resúmenes, Reescritura y Corrección).
  - `41-ia-local-vision-multimodal.md` — Visión Multimodal y Procesamiento de Imágenes en NPU.
  - `42-ia-local-mediapipe-rendimiento.md` — Rendimiento, Cuantización INT4/INT8 y MediaPipe LLM Inference.
  - `43-navegacion-compose-type-safe.md` — Navegación en Jetpack Compose (`Navigation Compose` & Type-Safe Navigation).
  - `44-workmanager-y-foreground-services.md` — Tareas en Segundo Plano y Ejecución Persistente (`WorkManager` & Foreground Services).
  - `45-notificaciones-push-fcm-y-websockets.md` — Notificaciones Push (`FCM`) y Comunicación en Tiempo Real (`WebSockets`).
  - `46-seguridad-cifrado-r8-certificate-pinning.md` — Cifrado, Seguridad Avanzada y Protección de Código (`EncryptedSharedPreferences`, R8/ProGuard & Certificate Pinning).
  - `47-compras-integradas-google-play-billing.md` — Monetización y Compras Integradas (`Google Play Billing Library`).
  - `48-internacionalizacion-i18n-y-rtl.md` — Internacionalización (`i18n`), Soporte RTL e Idioma Per-App.
  - `49-rendimiento-profiling-y-baseline-profiles.md` — Rendimiento, Profiling y Baseline Profiles (`LeakCanary` & `Macrobenchmark`).
  - `50-publicacion-google-play-aab-firmado.md` — Publicación y Distribución en Google Play Store (`.aab` y Play Console).

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

