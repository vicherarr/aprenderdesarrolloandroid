# Guía 60 — Watch Faces (Watch Face Format) y publicación en Google Play

Objetivo: crear una **esfera de reloj** (watch face) con el moderno **Watch Face
Format (WFF)** —declarativo, sin código— e integrar complications; y cubrir la
**publicación** de apps y esferas de Wear OS en Google Play (multi-APK, requisitos
de la ficha y testing final).

Prerrequisitos: Guías 53–56. Para publicar, conviene haber visto la Guía 50
(publicación en Play con `.aab` firmado), que aquí se especializa para el reloj.

---

## 1. Dos formas de hacer una watch face

| Enfoque | Descripción | Estado |
|---|---|---|
| **Watch Face Format (WFF)** | La esfera se describe en **XML declarativo**; Wear OS la renderiza. Sin código Kotlin, sin proceso propio. | **Recomendado.** Obligatorio para esferas nuevas en Play desde 2024/2025. |
| **Watch Face Service (Jetpack)** | Servicio con `Renderer` en Kotlin (Canvas/OpenGL). Máximo control, más batería y mantenimiento. | Solo para casos muy dinámicos que WFF no cubre. |

Google **prioriza WFF**: es más eficiente (lo pinta el sistema), sobrevive mejor al
modo ambiente y es lo que se exige para publicar esferas nuevas. Esta guía se centra
en WFF.

---

## 2. Anatomía de un proyecto Watch Face Format

Una watch face WFF es un APK **sin código**: solo recursos. Estructura mínima:

```
watchface/
├── src/main/
│   ├── AndroidManifest.xml
│   └── res/
│       ├── raw/watchface.xml        → la definición de la esfera (WFF)
│       ├── xml/watch_face_info.xml  → metadatos (categorías, vista previa)
│       └── drawable/…               → imágenes, fondos, manecillas
└── build.gradle.kts
```

El `AndroidManifest.xml` declara que el APK es una watch face WFF:

```xml
<application>
    <property
        android:name="com.google.wear.watchface.format.version"
        android:value="2" />
    <meta-data
        android:name="com.google.android.wearable.watchface.preview"
        android:resource="@drawable/preview" />
    <service ... >   <!-- servicio estándar de watch face, sin lógica propia -->
        <intent-filter>
            <action android:name="android.service.wallpaper.WallpaperService" />
            <category android:name="com.google.android.wearable.watchface.category.WATCH_FACE" />
        </intent-filter>
        <meta-data
            android:name="com.google.android.wearable.watchface.wearableConfigurationAction"
            android:value="androidx.wear.watchface.editor.action.WATCH_FACE_EDITOR" />
    </service>
</application>
```

---

## 3. La definición WFF (`watchface.xml`)

WFF es XML declarativo: describes escenas, relojes analógicos/digitales, y los
enlazas a **fuentes de datos** (`[HOUR_0_23]`, `[MINUTE]`, `[STEP_COUNT]`, …) que el
sistema rellena. Ejemplo de una esfera digital simple:

```xml
<WatchFace width="450" height="450">
    <Metadata key="CLOCK_TYPE" value="DIGITAL" />
    <Scene>
        <!-- Fondo negro: barato en OLED -->
        <PartDraw x="0" y="0" width="450" height="450">
            <Rectangle x="0" y="0" width="450" height="450">
                <Fill color="#FF000000" />
            </Rectangle>
        </PartDraw>

        <!-- Hora HH:MM centrada -->
        <PartText x="0" y="180" width="450" height="90">
            <Text align="CENTER">
                <Font family="SYNC_TO_DEVICE" size="80" color="#FFFFFFFF">
                    <Template>%02d:%02d
                        <Parameter expression="[HOUR_0_23]" />
                        <Parameter expression="[MINUTE]" />
                    </Template>
                </Font>
            </Text>
        </PartText>
    </Scene>
</WatchFace>
```

WFF soporta expresiones (aritmética, condicionales), animaciones ligeras, variantes
para el **modo ambiente** (una escena atenuada aparte) y **configuración de usuario**
(el usuario elige color/estilo desde el editor del sistema, sin que programes nada).

> 💡 Existen editores visuales (Watch Face Studio de Samsung exporta a WFF) que
> generan este XML sin escribirlo a mano. Para producción es lo más práctico.

---

## 4. Integrar complications en tu watch face

Una watch face define **huecos de complication** (`ComplicationSlot`) donde el
usuario coloca los datos que quiera (los de la Guía 56, u otros). En WFF se declaran
como `ComplicationSlot` con su posición, forma y tipos aceptados:

```xml
<ComplicationSlot slotId="0" x="300" y="200" width="80" height="80"
    supportedTypes="SHORT_TEXT RANGED_VALUE">
    <BoundingOval />
</ComplicationSlot>
```

El sistema aporta el editor para que el usuario elija qué complication va en cada
hueco. Así tu esfera muestra pasos, batería o el próximo evento sin que leas esos
datos tú mismo.

---

## 5. Requisitos de rendimiento de una watch face

Google exige que una esfera sea **eficiente**: hay un presupuesto de consumo que se
mide en la revisión. Reglas prácticas:

- Fondo predominantemente **negro**; evita animaciones continuas.
- El **modo ambiente** debe ser drásticamente más simple (menos píxeles encendidos).
- Aplica **desplazamiento anti burn-in** en ambiente.
- WFF ya está optimizado para esto; el `Renderer` en Kotlin es donde es fácil
  pasarse de consumo.

---

## 6. Empaquetar reloj + móvil para Google Play

Una app de Wear OS se publica en la **misma ficha** que su app de móvil (si existe),
mediante **multi-APK**: subes ambos APK/AAB bajo el mismo `applicationId` y Play
entrega a cada dispositivo el suyo.

Requisitos para que Play acepte y entregue el APK de reloj:

- Mismo `applicationId` en móvil y reloj, **misma clave de firma**.
- `versionCode` del reloj **distinto** (típicamente mayor) al del móvil para que el
  sistema los diferencie.
- El APK de reloj declara `uses-feature android.hardware.type.watch` (Guía 53); el
  de móvil, no.
- La app de reloj marcada como **standalone** si funciona sola (Guía 53 §3).

La convivencia de los dos módulos en el proyecto (código compartido, `versionCode`,
firma) se detalla en la **Guía 61**.

> 💡 Puedes publicar una app **solo de reloj** (sin app de móvil): es perfectamente
> válido y cada vez más común. Basta con el módulo de reloj standalone.

---

## 7. Subir a Play Console

El proceso base es el de la Guía 50 (AAB firmado, Play Console), con estas
particularidades de Wear OS:

1. Genera el AAB del módulo de reloj (`:wear:bundleRelease`) firmado con tu clave.
2. En Play Console, en la misma app, sube el AAB de reloj a la pista deseada.
3. Rellena la ficha con **capturas de Wear OS** (redondas) —son obligatorias para
   aparecer en la sección Wear OS de Play.
4. Marca la casilla de que la app es **compatible con Wear OS**; pasará una revisión
   específica (calidad de UI en reloj, batería, standalone).
5. Para watch faces, la categoría y las capturas deben mostrar la esfera; se revisa
   el cumplimiento de las guías de esferas y el presupuesto de energía.

---

## 8. Checklist de calidad antes de publicar

- [ ] Probado en **redondo pequeño, redondo grande y cuadrado**.
- [ ] Funciona **sin teléfono** (standalone) si así se anuncia.
- [ ] Responde a la **corona** en todo lo desplazable (Guía 55).
- [ ] Modo ambiente correcto: negro, estático, anti burn-in (Guía 59).
- [ ] Tareas continuas con **Ongoing Activity** (Guías 57 y 59).
- [ ] `versionCode` de reloj distinto y misma firma que el móvil.
- [ ] Capturas de Wear OS en la ficha.
- [ ] Sin *polling* ni wakelocks; batería medida y razonable.

---

## Fuentes consultadas (20-07-2026)

- Watch Face Format (guía oficial): <https://developer.android.com/training/wearables/wff>
- Referencia del formato WFF: <https://developer.android.com/training/wearables/wff/watch-face>
- Crear watch faces (visión general): <https://developer.android.com/training/wearables/watch-faces>
- Empaquetar apps de Wear OS (multi-APK): <https://developer.android.com/training/wearables/apps/packaging>
- Distribuir en Google Play: <https://developer.android.com/training/wearables/apps/publishing>
