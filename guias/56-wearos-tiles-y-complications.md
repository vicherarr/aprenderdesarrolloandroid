# Guía 56 — Tiles y Complications: las superficies *glanceable*

Objetivo: construir un **Tile** (la vista deslizable de un vistazo junto a la
esfera) con la librería Tiles + ProtoLayout, y un **Complication data source** (el
dato que otras watch faces muestran, p. ej. pasos o próximo evento). Son las
superficies donde el usuario ve tu app sin abrirla.

Prerrequisitos: Guía 53 (superficies de Wear OS) y Guía 54. Los Tiles **no usan
Compose**: tienen su propio sistema de layout declarativo (ProtoLayout) pensado
para renderizarse fuera de tu proceso y consumir mínima batería.

---

## 1. ¿Qué es un Tile?

Un **Tile** es una tarjeta a la que el usuario llega **deslizando** desde la
esfera. Muestra información de un vistazo y quizá un par de acciones. No hay scroll
ni navegación: es una foto fija que se refresca cada cierto tiempo.

Diferencias clave con una pantalla de app:

- Se renderiza mediante **ProtoLayout**, no Compose. Tú describes el layout y el
  sistema lo dibuja en su propio proceso (por eso es tan eficiente).
- Se **refresca por intervalos** o bajo demanda (*freshness*), no en tiempo real.
- Interacción limitada: tocar para abrir la app o disparar una acción.

Dependencias:

```kotlin
dependencies {
    implementation("androidx.wear.tiles:tiles:1.6.1")
    implementation("androidx.wear.tiles:tiles-material:1.6.1")     // componentes Material para tiles
    implementation("androidx.wear.protolayout:protolayout:1.4.1")
    implementation("androidx.wear.protolayout:protolayout-material3:1.4.1")
    // onTileRequest devuelve ListenableFuture: este artefacto da el builder future{}
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.8.1")
    debugImplementation("androidx.wear.tiles:tiles-tooling:1.6.1") // preview en el IDE
}
```

> ✅ Versiones verificadas compilando un `TileService` y un
> `ComplicationDataSourceService` reales (serie tiles 1.6.x / protolayout 1.4.x en
> jul-2026).

---

## 2. Un `TileService` mínimo

Un Tile es un `Service` que responde a dos peticiones: el **layout** (qué dibujar)
y los **recursos** (imágenes). La forma cómoda es extender `TileService` (o el
wrapper de corrutinas `SuspendingTileService`).

```kotlin
import androidx.wear.tiles.TileService
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.RequestBuilders

private const val RESOURCES_VERSION = "1"

class PasosTileService : TileService() {

    // future{} (kotlinx-coroutines-guava) convierte una corrutina en el
    // ListenableFuture que onTileRequest debe devolver
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<Tile> = serviceScope.future {
        val pasos = leerPasosDeHoy()   // tu fuente de datos

        val layout = LayoutElementBuilders.Box.Builder()
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("$pasos pasos")
                    .build()
            )
            .build()

        Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(15 * 60 * 1000)   // refresca cada 15 min
            .setTileTimeline(Timeline.fromLayoutElement(layout))
            .build()
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> = serviceScope.future {
        ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
    }
}
```

Registro en el manifest, con el filtro que lo declara como Tile y su vista previa:

```xml
<service
    android:name=".PasosTileService"
    android:label="Pasos"
    android:exported="true"
    android:permission="com.google.android.wearable.permission.BIND_TILE_PROVIDER">
    <intent-filter>
        <action android:name="androidx.wear.tiles.action.BIND_TILE_PROVIDER" />
    </intent-filter>
    <!-- Imagen de vista previa en el selector de tiles -->
    <meta-data
        android:name="androidx.wear.tiles.PREVIEW"
        android:resource="@drawable/tile_preview" />
</service>
```

---

## 3. Layout con ProtoLayout Material 3

En vez de construir cajas a mano, usa los componentes Material para tiles
(`primaryLayout`, botones, textos con estilo) que ya respetan márgenes y forma
redonda:

```kotlin
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.materialScope

fun tileLayout(context: Context, deviceParams: DeviceParameters, pasos: Int) =
    materialScope(context, deviceParams) {
        primaryLayout(
            mainSlot = { text("$pasos pasos".layoutString) },
            bottomSlot = { /* botón o etiqueta */ }
        )
    }
```

---

## 4. Interacción: abrir la app o disparar una acción

Un toque en un elemento del tile puede lanzar una `Activity` (`LaunchAction`) o
avisar a tu servicio (`LoadAction`). Se asocia con `Clickable`:

```kotlin
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ActionBuilders

val abrirApp = Clickable.Builder()
    .setOnClick(
        ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setClassName("com.ejemplo.wear.MainActivity")
                    .setPackageName("com.ejemplo.wear")
                    .build()
            ).build()
    ).build()
```

---

## 5. Actualizar un Tile

El tile se refresca solo según `setFreshnessIntervalMillis`. Para forzar una
actualización inmediata cuando cambian tus datos (llegó un mensaje, terminó un
entreno), notifica al sistema:

```kotlin
import androidx.wear.tiles.TileService

// Pide al sistema que vuelva a llamar a onTileRequest de tu servicio
TileService.getUpdater(context)
    .requestUpdate(PasosTileService::class.java)
```

> 💡 No abuses de las actualizaciones: cada una gasta batería. Refresca por
> intervalo razonable y solo fuerza *update* ante un cambio real que el usuario
> notaría.

---

## 6. ¿Qué es una Complication?

Una **complication** es un dato pequeño que **las watch faces** muestran dentro de
la esfera (pasos, temperatura, próximo evento, batería). Tú no controlas dónde se
pinta: publicas un **data source** y la watch face lo coloca en uno de sus huecos.

Tu trabajo es implementar un `ComplicationDataSourceService` que entregue el dato
en el **tipo** que corresponda (texto corto, rango/gauge, icono, etc.).

Dependencia:

```kotlin
implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.3.0")
```

---

## 7. Un `ComplicationDataSourceService`

```kotlin
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.data.*

class PasosComplicationService : SuspendingComplicationDataSourceService() {

    // Cómo se ve en el editor de la watch face (dato de ejemplo)
    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        when (type) {
            ComplicationType.SHORT_TEXT -> shortText("8.2K")
            ComplicationType.RANGED_VALUE -> ranged(8200f, 10000f)
            else -> null
        }

    // El dato real que se muestra
    override suspend fun onComplicationRequest(
        request: ComplicationRequest
    ): ComplicationData {
        val pasos = leerPasosDeHoy()
        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> shortText("${pasos / 1000}K")
            ComplicationType.RANGED_VALUE -> ranged(pasos.toFloat(), 10000f)
            else -> NoDataComplicationData()
        }
    }

    private fun shortText(texto: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(texto).build(),
            contentDescription = PlainComplicationText.Builder("Pasos de hoy").build()
        ).build()

    private fun ranged(valor: Float, max: Float) =
        RangedValueComplicationData.Builder(
            value = valor, min = 0f, max = max,
            contentDescription = PlainComplicationText.Builder("Progreso de pasos").build()
        ).build()
}
```

Manifest, declarando los **tipos soportados** y el periodo de actualización:

```xml
<service
    android:name=".PasosComplicationService"
    android:exported="true"
    android:label="Pasos"
    android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER">
    <intent-filter>
        <action android:name="android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST" />
    </intent-filter>
    <meta-data
        android:name="android.support.wearable.complications.SUPPORTED_TYPES"
        android:value="SHORT_TEXT,RANGED_VALUE" />
    <meta-data
        android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
        android:value="900" />   <!-- 15 min -->
</service>
```

Para forzar refresco fuera del periodo se usa
`ComplicationDataSourceUpdateRequester`.

---

## 8. Tiles vs. Complications vs. App: cuándo usar cada una

| Necesito… | Superficie |
|---|---|
| Mostrar 1–4 datos y un par de acciones de un vistazo | **Tile** |
| Que mi dato aparezca dentro de la esfera del usuario | **Complication** |
| Interacción rica, scroll, varias pantallas | **App** (Guías 54–55) |

Lo ideal es ofrecer las tres: la app para lo profundo, un tile para el resumen y
una complication para engancharse a la esfera.

---

## Fuentes consultadas (20-07-2026)

- Tiles (guía oficial): <https://developer.android.com/training/wearables/tiles>
- ProtoLayout y Material para Tiles: <https://developer.android.com/training/wearables/tiles/layouts>
- Complications overview: <https://developer.android.com/training/wearables/watch-faces/complications>
- Exponer datos como complication data source: <https://developer.android.com/training/wearables/watch-faces/complications/data-sources>
- Actualizar Tiles y Complications: <https://developer.android.com/training/wearables/tiles/tile-lifecycle>
