# Aprender Desarrollo Android

Repositorio de aprendizaje de desarrollo Android profesional. Contiene proyectos de ejemplo y 50 guías paso a paso divididas en 8 módulos estructurados de cero a senior, probadas en este mismo equipo (CachyOS + Android Studio + Android SDK cmdline-tools).

---

## 📚 Estructura de Guías por Módulos

### 🛠️ Módulo I: Fundamentos y Entorno de Desarrollo
- [01-preparar-entorno.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/01-preparar-entorno.md) — Entorno Android en Linux (SDK, JDK 21, cmdline-tools).
- [02-crear-proyecto-desde-cli.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/02-crear-proyecto-desde-cli.md) — Crear proyecto desde la terminal.

### 🎨 Módulo II: UI y Navegación con Jetpack Compose & Material 3
- [03-layouts-y-modificadores.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/03-layouts-y-modificadores.md) — Fundamentos de Layouts y Modificadores (`Column`, `Row`, `Box`).
- [04-botones-y-entradas.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/04-botones-y-entradas.md) — Botones, Selección y Controles de Entrada (`Button`, `TextField`, `Switch`).
- [05-tarjetas-listas-y-grids.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/05-tarjetas-listas-y-grids.md) — Tarjetas, Listas y Rejillas Eficientes (`Card`, `LazyColumn`, `LazyVerticalGrid`).
- [06-scaffold-y-material3-structure.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/06-scaffold-y-material3-structure.md) — Estructura de Pantalla M3 (`Scaffold`, `TopAppBar`, `NavigationBar`, `Drawer`).
- [07-feedback-dialogos-y-bottomsheets.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/07-feedback-dialogos-y-bottomsheets.md) — Feedback de Usuario, Diálogos y Menús (`Snackbar`, `AlertDialog`, `ModalBottomSheet`).
- [08-tabs-pagers-badges-y-chips.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/08-tabs-pagers-badges-y-chips.md) — Pestañas, Pagers, Badges e Indicadores (`TabRow`, `HorizontalPager`, `Badge`, `Chip`).
- [09-navegacion-compose-type-safe.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/09-navegacion-compose-type-safe.md) — Navegación en Jetpack Compose (`Navigation Compose` & Type-Safe Navigation).
- [10-tema-material3-y-dark-mode.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/10-tema-material3-y-dark-mode.md) — Sistema de Diseño Material 3 (`MaterialTheme`, Dark Mode, Dynamic Color).
- [11-integracion-profunda-material3.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/11-integracion-profunda-material3.md) — Integración Profunda de Material 3 y Tokens de Diseño.
- [12-animaciones-ui-compose.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/12-animaciones-ui-compose.md) — Animaciones UI (`animate*AsState`, `AnimatedVisibility`, `Crossfade`).
- [13-canvas-y-dibujo-personalizado.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/13-canvas-y-dibujo-personalizado.md) — Dibujo Personalizado y Canvas (`Canvas`, `Path`, `Brush`).
- [14-gestos-y-diseno-responsive.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/14-gestos-y-diseno-responsive.md) — Gestos Táctiles y Diseño Adaptativo (`pointerInput`, `WindowSizeClass`).
- [15-librerias-y-componentes-de-terceros.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/15-librerias-y-componentes-de-terceros.md) — Librerías y Componentes UI de Terceros para Producción.

### 🏗️ Módulo III: Arquitectura, Datos e Inyección de Dependencias
- [16-habilitar-hilt.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/16-habilitar-hilt.md) — Habilitar Hilt (inyección de dependencias).
- [17-usar-hilt.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/17-usar-hilt.md) — Usar Hilt en el proyecto.
- [18-arquitectura-recomendada.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/18-arquitectura-recomendada.md) — Arquitectura (UI, ViewModel, Repository, Domain).
- [19-retrofit.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/19-retrofit.md) — Retrofit y peticiones HTTP.
- [20-room.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/20-room.md) — Persistencia local con Room.
- [21-migraciones-room.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/21-migraciones-room.md) — Migraciones en Room.
- [22-logging.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/22-logging.md) — Logging con Timber.
- [23-internacionalizacion-i18n-y-rtl.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/23-internacionalizacion-i18n-y-rtl.md) — Internacionalización (`i18n`), Soporte RTL e Idioma Per-App.

### 🔄 Módulo IV: Servicios de Sistema, Notificaciones y Segundo Plano
- [24-workmanager-y-foreground-services.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/24-workmanager-y-foreground-services.md) — Tareas en Segundo Plano y Ejecución Persistente (`WorkManager` & Foreground Services).
- [25-notificaciones-push-fcm-y-websockets.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/25-notificaciones-push-fcm-y-websockets.md) — Notificaciones Push (`FCM`) y Comunicación en Tiempo Real (`WebSockets`).

### 📱 Módulo V: Hardware del Dispositivo Completo
- [26-hardware-gps-y-ubicacion.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/26-hardware-gps-y-ubicacion.md) — GPS y Geolocalización (`FusedLocationProviderClient`).
- [27-hardware-sensores-movimiento-y-posicion.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/27-hardware-sensores-movimiento-y-posicion.md) — Sensores de Movimiento y Posición (Acelerómetro, Giroscopio, Brújula, Podómetro).
- [28-hardware-sensores-ambientales-y-proximidad.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/28-hardware-sensores-ambientales-y-proximidad.md) — Sensores Ambientales y de Proximidad (Luz, Proximidad, Barómetro).
- [29-hardware-camara-camerax-y-flash.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/29-hardware-camara-camerax-y-flash.md) — Cámara, CameraX, Escaneo de Códigos y Flash.
- [30-hardware-microfono-y-audio.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/30-hardware-microfono-y-audio.md) — Micrófono y Captura de Audio (`MediaRecorder`, `AudioRecord`).
- [31-hardware-biometria-y-keystore.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/31-hardware-biometria-y-keystore.md) — Autenticación Biométrica y Android KeyStore (`BiometricPrompt`).
- [32-hardware-bluetooth-y-ble.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/32-hardware-bluetooth-y-ble.md) — Bluetooth y Bluetooth Low Energy (BLE).
- [33-hardware-nfc-lectura-escritura.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/33-hardware-nfc-lectura-escritura.md) — NFC (Near Field Communication) Lectura y Escritura.
- [34-hardware-bateria-haptica-y-termico.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/34-hardware-bateria-haptica-y-termico.md) — Batería, Motores Hápticos y Estado Térmico.
- [35-hardware-usb-salud-y-pantalla.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/35-hardware-usb-salud-y-pantalla.md) — Periféricos USB Host, Pantalla y Sensores de Salud.

### 🤖 Módulo VI: IA Local On-Device
- [36-ia-local-aicore-introduccion.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/36-ia-local-aicore-introduccion.md) — IA Local On-Device y Android AICore.
- [37-ia-local-google-ai-edge-prompts.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/37-ia-local-google-ai-edge-prompts.md) — Google AI Edge SDK y Generación en Streaming.
- [38-ia-local-tareas-especializadas-sistema.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/38-ia-local-tareas-especializadas-sistema.md) — APIs Especializadas de IA (Resúmenes, Reescritura y Corrección).
- [39-ia-local-vision-multimodal.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/39-ia-local-vision-multimodal.md) — Visión Multimodal y Procesamiento de Imágenes en NPU.
- [40-ia-local-mediapipe-rendimiento.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/40-ia-local-mediapipe-rendimiento.md) — Rendimiento, Cuantización INT4/INT8 y MediaPipe LLM Inference.

### 🧪 Módulo VII: Testing Automatizado y CI/CD
- [41-tests-unitarios.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/41-tests-unitarios.md) — Tests unitarios en JVM.
- [42-tests-viewmodel-flow.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/42-tests-viewmodel-flow.md) — Testing de ViewModels y Flows.
- [43-tests-de-red-mockwebserver.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/43-tests-de-red-mockwebserver.md) — Testing de red con MockWebServer.
- [44-tests-instrumentados-room.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/44-tests-instrumentados-room.md) — Tests instrumentados con Room.
- [45-tests-ui-compose.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/45-tests-ui-compose.md) — Tests de UI en Jetpack Compose.
- [46-ci-github-actions.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/46-ci-github-actions.md) — Integración Continua con GitHub Actions.

### 🔒 Módulo VIII: Seguridad, Monetización, Rendimiento y Publicación
- [47-seguridad-cifrado-r8-certificate-pinning.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/47-seguridad-cifrado-r8-certificate-pinning.md) — Cifrado, Seguridad Avanzada y Protección de Código (`EncryptedSharedPreferences`, R8/ProGuard & Certificate Pinning).
- [48-compras-integradas-google-play-billing.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/48-compras-integradas-google-play-billing.md) — Monetización y Compras Integradas (`Google Play Billing Library`).
- [49-rendimiento-profiling-y-baseline-profiles.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/49-rendimiento-profiling-y-baseline-profiles.md) — Rendimiento, Profiling y Baseline Profiles (`LeakCanary` & `Macrobenchmark`).
- [50-publicacion-google-play-aab-firmado.md](file:///home/victor/develop/AprenderDesarrolloAndroid/guias/50-publicacion-google-play-aab-firmado.md) — Publicación y Distribución en Google Play Store (`.aab` y Play Console).

---

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
