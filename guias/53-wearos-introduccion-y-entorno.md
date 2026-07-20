# Guía 53 — Wear OS: introducción, arquitectura y preparación del entorno

Objetivo: entender qué es Wear OS, en qué se diferencia del desarrollo para móvil,
cuáles son sus restricciones de hardware y modelo de app, y dejar el entorno listo
para compilar y ejecutar en un emulador de reloj (AVD Wear OS).

Prerrequisitos: entorno Android funcionando (Guía 01), saber crear proyectos desde
la CLI (Guía 02) y conocer Jetpack Compose (Módulo II). Wear OS moderno se programa
con Compose, así que todo lo aprendido allí sirve de base.

---

## 1. ¿Qué es Wear OS?

Wear OS (antes Android Wear) es la variante de Android para relojes inteligentes.
Desde Wear OS 3 (2021) está construido sobre la misma base que Android pero con un
conjunto de librerías, componentes de UI y APIs propios adaptados a una pantalla
diminuta, redonda en muchos casos, con batería mínima y uso de segundos, no de
minutos.

Ideas clave que cambian respecto al móvil:

- **Pantalla muy pequeña y a menudo redonda.** El contenido se recorta en las
  esquinas; el diseño debe ser **circular-first**.
- **Interacciones brevísimas.** El usuario mira el reloj 1–5 segundos. La app debe
  mostrar lo esencial de inmediato (*glanceable*).
- **Batería crítica.** El sistema es agresivo cerrando apps y limitando el trabajo
  en segundo plano. Hay un **modo ambiente** (pantalla atenuada) permanente.
- **Entrada distinta.** Además del táctil hay una **corona/bisel rotatorio**
  (*rotary input*) y botones físicos.
- **Autonomía respecto al teléfono.** Desde Wear OS 3 las apps son **standalone**:
  deben funcionar sin un teléfono emparejado.

---

## 2. Superficies de una app de reloj

En Wear OS "una app" no es solo una pantalla que abres desde el lanzador. Hay
varias **superficies** (*surfaces*), cada una con su propia API y su guía dedicada
en este módulo:

| Superficie | Qué es | Guía |
|---|---|---|
| **App** | Pantallas a las que entra el usuario, hechas con Compose for Wear OS. | 54, 55 |
| **Tiles** | Vistas *glanceable* deslizables junto a la esfera; sin scroll, un vistazo. | 56 |
| **Complications** | Datos que otras watch faces muestran (pasos, próximo evento). | 56 |
| **Watch Face** | La propia esfera del reloj. | 60 |
| **Ongoing Activity** | Actividad en curso (entreno, ruta) visible en el sistema. | 59 |
| **Notificaciones** | Alertas, puenteadas desde el móvil o propias. | 59 |

Una buena app de reloj rara vez es solo pantallas: combina app + tile +
complication para estar donde el usuario mira.

---

## 3. App standalone vs. dependiente del teléfono

| Tipo | Descripción |
|---|---|
| **Standalone** | Funciona sin teléfono. Tiene su propia conectividad (Wi-Fi/LTE), su cuenta, sus permisos. **Es el modelo recomendado y obligatorio** para publicar por separado. |
| **Dependiente** | Necesita una app compañera en el móvil para funcionar (login, datos). |

Se declara en el `AndroidManifest.xml` del módulo de reloj:

```xml
<application ...>
    <!-- true = la app funciona por sí sola -->
    <meta-data
        android:name="com.google.android.wearable.standalone"
        android:value="true" />
</application>
```

Aunque sea standalone, puede **sincronizarse** con una app de móvil mediante la
Data Layer API (Guía 58) cuando ambas están instaladas.

---

## 4. Requisitos del proyecto

Wear OS moderno exige, como mínimo:

- **`minSdk` 30** (Wear OS 3 = Android 11). Muchas apps nuevas apuntan a 33/34.
- Dependencia de la *feature* de reloj y de las librerías `androidx.wear.*`.
- Compose for Wear OS (Módulo VI de Compose para reloj), **no** Material 3 de móvil.

Declaración de que es una app de reloj en el manifest:

```xml
<uses-feature android:name="android.hardware.type.watch" />

<!-- Wear OS necesita esta librería del sistema en runtime -->
<uses-library
    android:name="com.google.android.wearable"
    android:required="false" />
```

> ⚠️ La UI de reloj usa `androidx.wear.compose:compose-material` /
> `compose-material3` (Wear), **distintos** de `androidx.compose.material3` del
> móvil. Mezclarlos produce componentes que no encajan en pantalla redonda ni
> respetan el modo ambiente. Lo veremos en la Guía 54.

---

## 5. Crear un emulador de reloj (AVD Wear OS)

Sin un reloj físico, se prueba en un AVD de Wear OS. Con las `cmdline-tools` y el
binario `android` documentado en las Guías 01–02:

```bash
# 1. Instalar la system image de Wear OS (redonda, API 34, arquitectura del equipo)
android sdk install "system-images;android-34;android-wear;x86_64"

# 2. Crear el AVD con perfil de reloj redondo
android avd create \
  --name "wear-redondo" \
  --package "system-images;android-34;android-wear;x86_64" \
  --device "wearos_large_round"

# 3. Arrancar el emulador
android avd start "wear-redondo"
```

Perfiles de dispositivo útiles: `wearos_large_round`, `wearos_small_round`,
`wearos_square`. Prueba **siempre en redondo**, que es el caso que rompe layouts.

> 💡 Si usas Android Studio, el *Device Manager* incluye plantillas "Wear OS"
> equivalentes. La imagen debe ser `android-wear` (con Play) o
> `android-wear-*` según disponibilidad; la variante con Google APIs es necesaria
> para probar Health Services y la Data Layer con servicios de Google Play.

---

## 6. Emparejar el emulador con un teléfono (opcional)

Para probar la Data Layer (Guía 58), notificaciones puenteadas o el flujo de
instalación, se empareja el AVD de reloj con un AVD de teléfono (o un móvil real)
mediante la app **Wear OS** de Google Play y el reenvío de puertos de `adb`:

```bash
# Reenvía el puerto de comunicación del reloj al host para el emparejamiento
adb -s <serial-del-reloj> forward tcp:5601 tcp:5601
```

Luego, en la app Wear OS del teléfono, se elige "Emparejar con emulador". Para
desarrollo puro de UI **no es necesario**: el reloj standalone se prueba solo.

---

## 7. Anatomía del proyecto multi-módulo

Lo habitual es un proyecto Gradle con módulos separados. Aunque una app de reloj
puede vivir sola, el patrón profesional es:

```
MiApp/
├── app/          → app de teléfono (opcional, compañera)
├── wear/         → app de Wear OS (module type: com.android.application)
└── shared/       → código común (modelos, lógica) compartido por ambas
```

Cada módulo (`app` y `wear`) declara su propio `applicationId` idéntico o
relacionado; Google Play entrega el APK de reloj al reloj y el de móvil al móvil
mediante *multi-APK* bajo la misma ficha (Guía 60).

---

## 8. Checklist antes de escribir código

- [ ] `minSdk >= 30` en el módulo de reloj.
- [ ] `uses-feature android.hardware.type.watch` en el manifest.
- [ ] `meta-data standalone` declarado (normalmente `true`).
- [ ] AVD de Wear OS **redondo** creado y arrancando.
- [ ] Dependencias `androidx.wear.compose:*` en lugar de las de móvil (Guía 54).
- [ ] Mentalidad *glanceable*: ¿se entiende la pantalla en 2 segundos?

---

## Fuentes consultadas (20-07-2026)

- Wear OS para desarrolladores (portal oficial): <https://developer.android.com/training/wearables>
- Principios de diseño de Wear OS: <https://developer.android.com/design/ui/wear>
- Crear y ejecutar una app Wear OS: <https://developer.android.com/training/wearables/get-started/creating>
- Apps standalone: <https://developer.android.com/training/wearables/apps/standalone-apps>
- Configurar un emulador de Wear OS: <https://developer.android.com/training/wearables/get-started/connect-phone>
