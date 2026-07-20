# Guía 61 — Convivencia de un proyecto Wear OS con la app de smartphone

Objetivo: montar y mantener **un único proyecto** que contenga a la vez la app de
teléfono y la app de reloj: estructura Gradle multi-módulo, **código compartido**
(modelos, lógica, red), reglas de `applicationId`, `versionCode` y **firma**, cómo
Google Play entrega cada APK a su dispositivo (**multi-APK**) y cómo se comunican en
runtime.

Prerrequisitos: Guía 53 (standalone vs. dependiente), Guía 58 (Data Layer, el puente
en runtime) y Guía 50 (publicación con AAB firmado). Esta guía une todo lo anterior
desde el punto de vista del **proyecto**.

---

## 1. El escenario: dos apps, un producto

Tienes (o quieres) una app de móvil y quieres añadirle un reloj. Hay tres modelos:

| Modelo | Cuándo | Publicación |
|---|---|---|
| **Solo reloj** | La experiencia vive en la muñeca (una esfera, una app fitness standalone). | Ficha propia, solo APK de reloj. |
| **Móvil + reloj, misma ficha** | Un producto con presencia en ambos (lo más común). | **Multi-APK** bajo un mismo `applicationId`. |
| **Móvil y reloj independientes** | Rara vez; dos productos distintos. | Dos fichas separadas. |

Esta guía cubre el caso central: **móvil + reloj en el mismo proyecto y misma
ficha**, que es donde aparece la palabra "convivencia".

---

## 2. Estructura Gradle multi-módulo

El patrón profesional separa tres módulos: la app de móvil, la app de reloj y un
módulo de **código compartido**.

```
MiProducto/
├── settings.gradle.kts        → incluye los tres módulos
├── build.gradle.kts           → plugins y versiones comunes
├── gradle/libs.versions.toml  → catálogo de versiones (una sola verdad)
├── shared/                    → library module (Kotlin/Android) COMPARTIDO
│   └── build.gradle.kts       → com.android.library
├── mobile/                    → app de teléfono
│   └── build.gradle.kts       → com.android.application
└── wear/                      → app de reloj
    └── build.gradle.kts       → com.android.application
```

```kotlin
// settings.gradle.kts
include(":shared", ":mobile", ":wear")
```

`mobile` y `wear` son **dos módulos de aplicación** (`com.android.application`)
distintos: producen dos APK. Ambos **dependen** de `shared`:

```kotlin
// mobile/build.gradle.kts  y  wear/build.gradle.kts
dependencies {
    implementation(project(":shared"))
}
```

---

## 3. Qué va en `shared` y qué no

El módulo compartido debe contener lo que es **idéntico** en ambos dispositivos y
**no dependa de UI**:

| Sí va en `shared` | No va en `shared` |
|---|---|
| Modelos de dominio (`data class`, enums). | UI: Compose de móvil vs. Compose de Wear son distintos (Guía 54). |
| DTOs y contratos de la Data Layer (paths, claves). | `Activity`/pantallas específicas de cada factor. |
| Lógica de negocio pura, validaciones, mappers. | Recursos de layout/imágenes específicos. |
| Cliente de red (Retrofit) y modelos de API. | Tiles/Complications/Watch Faces (solo reloj). |
| Constantes compartidas (paths `/entreno/...`). | Navegación (SwipeDismissable en reloj). |

> ⚠️ **La UI no se comparte.** El reloj usa `androidx.wear.compose.*` y el móvil
> `androidx.compose.material3` (Guía 54). Intentar reutilizar composables entre
> ambos casi siempre sale mal: distinta forma, distinto tamaño, distinto modo
> ambiente. Comparte **datos y lógica**, redibuja la UI en cada uno.

Ejemplo típico de contrato compartido para la Data Layer (Guía 58):

```kotlin
// shared/src/main/kotlin/.../DataLayerContract.kt
object DataLayerPaths {
    const val CONFIG_OBJETIVO = "/config/objetivo"
    const val ENTRENO_RESUMEN = "/entreno/resumen"
    const val CAP_TELEFONO = "telefono_app"
    const val CAP_RELOJ = "reloj_app"
}

@Serializable
data class ResumenEntreno(val distanciaM: Int, val kcal: Int, val ppmMedia: Int)
```

Que ambos módulos usen las **mismas constantes** evita el error clásico de la Data
Layer: un path escrito distinto en cada lado y mensajes que nunca llegan.

---

## 4. `applicationId`: la regla que hace posible la convivencia

Para que Google Play trate las dos apps como **el mismo producto** y para que la
**Data Layer** las conecte (Guía 58), ambos módulos deben compartir el mismo
`applicationId`:

```kotlin
// mobile/build.gradle.kts
android {
    namespace = "com.ejemplo.miapp.mobile"   // paquete de código (puede diferir)
    defaultConfig {
        applicationId = "com.ejemplo.miapp"   // ← IDÉNTICO en ambos módulos
        versionName = "1.0.0"
    }
}
```

```kotlin
// wear/build.gradle.kts
android {
    namespace = "com.ejemplo.miapp.wear"
    defaultConfig {
        applicationId = "com.ejemplo.miapp"   // ← EL MISMO que el móvil
        minSdk = 30                            // Wear OS 3+
    }
}
```

Distingue dos conceptos:

- **`namespace`**: el paquete del código (dónde vive `R`, tus clases). Puede y suele
  ser **distinto** por módulo.
- **`applicationId`**: la identidad de la app ante el sistema y Play. Debe ser
  **idéntico** en móvil y reloj para el multi-APK y la Data Layer.

---

## 5. `versionCode`: distintos y coordinados

Los dos APK comparten `applicationId`, así que Play necesita distinguirlos por
`versionCode`. La regla habitual: **el APK de reloj lleva un `versionCode` mayor**
(por convención se le suma un desplazamiento), de modo que un dispositivo nunca
degrade de uno a otro.

```kotlin
// build.gradle.kts raíz (o convención compartida)
val baseVersionCode = 42

// mobile
defaultConfig { versionCode = baseVersionCode }        // 42
// wear
defaultConfig { versionCode = baseVersionCode + 1000 } // 1042 — siempre por encima
```

Así, al publicar la versión 43 subes `mobile=43` y `wear=1043`, y nunca se cruzan.

---

## 6. Firma: la MISMA clave, obligatoriamente

Los dos APK **deben** firmarse con la **misma clave de release**. Es requisito de:

- **Multi-APK en Play** (misma app, mismo certificado).
- **Data Layer API** (Guía 58): solo conecta apps con igual `applicationId` **y**
  igual firma. Con firmas distintas, reloj y móvil no se ven.

Se configura el mismo `signingConfig` para ambos módulos (idealmente definido una
vez y reutilizado, o vía Play App Signing, que gestiona la clave por ti):

```kotlin
// convención de firma aplicada a mobile y wear por igual
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_PATH"))
        storePassword = System.getenv("KEYSTORE_PASS")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASS")
    }
}
```

> ⚠️ El fallo silencioso más común: probar en debug (donde ambos usan la misma
> `debug.keystore` y todo funciona) y romper en release por firmar cada módulo con
> claves distintas. La Data Layer deja de conectar. **Misma clave en release.**

---

## 7. Cómo llega cada APK a su dispositivo (multi-APK)

Con todo lo anterior, la entrega es automática:

1. Subes a la **misma** app de Play Console dos artefactos: el AAB de `mobile` y el
   de `wear`.
2. Play los diferencia por `versionCode` y por el `uses-feature
   android.hardware.type.watch` que **solo** declara el de reloj (Guía 53).
3. Un teléfono descarga el APK de móvil; un reloj emparejado descarga el de reloj.
4. Si el usuario instala la app en el móvil y tiene un reloj, Play ofrece/instala la
   versión de reloj automáticamente.

No hay que hacer nada especial en runtime: la separación la resuelve la `uses-feature`
y el `versionCode`.

---

## 8. Detectar al "otro" en runtime

Aun conviviendo en el mismo producto, cada app se ejecuta en su dispositivo. Para
saber si la contraparte está instalada y comunicarse se usa la Data Layer (Guía 58):

```kotlin
// Desde el reloj: ¿tiene el usuario la app de móvil?
val nodo = Wearable.getCapabilityClient(context)
    .getCapability(DataLayerPaths.CAP_TELEFONO, CapabilityClient.FILTER_REACHABLE)
    .await()
    .nodes.firstOrNull { it.isNearby }

if (nodo == null) {
    // No hay app de móvil: ofrece abrir su ficha en el teléfono
    RemoteActivityHelper(context).startRemoteActivity(
        Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse("market://details?id=com.ejemplo.miapp"))
    )
}
```

Cada app anuncia su capacidad en `res/values/wear.xml` (`telefono_app` en el móvil,
`reloj_app` en el reloj), y usan las constantes de `shared` para no descoordinarse.

---

## 9. Reparto de responsabilidades entre módulos

Diseña **qué hace cada dispositivo**, no dupliques todo:

| Responsabilidad | Dónde | Por qué |
|---|---|---|
| Login, cuenta, configuración pesada | Móvil | Pantalla grande, teclado. |
| Almacén principal, sincronización con la nube | Móvil | Batería y conectividad estable. |
| Captura de datos de sensores/entreno | Reloj | Está en la muñeca (Guía 57). |
| Acciones rápidas *glanceable* | Reloj (Tiles/Complications) | Un vistazo (Guía 56). |
| Modelos, contratos, lógica de negocio | `shared` | Una sola verdad para ambos. |

El reloj **mide y muestra**; el móvil **almacena y sincroniza**; `shared` **define**
el lenguaje común. Los datos viajan por la Data Layer.

---

## 10. Checklist de convivencia

- [ ] Módulos `mobile`, `wear` (ambos `com.android.application`) y `shared`
      (`com.android.library`).
- [ ] `applicationId` **idéntico** en `mobile` y `wear`.
- [ ] `namespace` distinto por módulo (sin choques de `R`).
- [ ] `versionCode` del reloj **mayor** y coordinado con el del móvil.
- [ ] **Misma clave de firma** de release en ambos (o Play App Signing).
- [ ] Solo `wear` declara `uses-feature android.hardware.type.watch`.
- [ ] Contratos de la Data Layer (paths, capacidades, DTOs) en `shared`.
- [ ] UI **no** compartida; datos y lógica **sí**.
- [ ] Capacidades anunciadas en `wear.xml` a ambos lados.

---

## Fuentes consultadas (20-07-2026)

- Empaquetar y distribuir apps de Wear OS (multi-APK): <https://developer.android.com/training/wearables/apps/packaging>
- Estructura de proyectos con módulos: <https://developer.android.com/topic/modularization>
- Apps standalone y compañeras: <https://developer.android.com/training/wearables/apps/standalone-apps>
- Wearable Data Layer (firma y applicationId compartidos): <https://developer.android.com/training/wearables/data/data-layer>
- Publicar en Google Play: <https://developer.android.com/training/wearables/apps/publishing>
