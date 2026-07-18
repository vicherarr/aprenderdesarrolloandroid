# Guía 03 — Habilitar inyección de dependencias con Dagger Hilt

Objetivo: dejar Hilt **habilitado** en `HolaAndroid` (plugins, dependencias y
anotaciones base) y comprobar que el proyecto compila. *Usar* la inyección
(módulos, `@Inject`, ViewModels…) se verá en otra lección.

Esta guía tiene dos partes: **cómo se eligieron las versiones** (la metodología,
que es lo reutilizable) y **los cambios aplicados al proyecto**.

---

## Parte 1 — Metodología: cómo elegir las versiones correctas

Antes de tocar el proyecto hay que responder tres preguntas, cada una con su
fuente oficial. No copies versiones de tutoriales o de Stack Overflow: suelen
estar desactualizadas.

### 1.1 ¿Qué procesador de anotaciones: KSP o kapt?

Hilt genera código en tiempo de compilación y necesita un procesador de
anotaciones. Hay dos: **kapt** (el antiguo, en modo mantenimiento) y **KSP**
(el actual, hasta 2× más rápido).

- Fuente: <https://developer.android.com/build/migrate-to-ksp> — Google
  declara kapt en mantenimiento y recomienda KSP.
- Fuente: <https://dagger.dev/dev-guide/ksp> — Dagger/Hilt soporta KSP de
  forma estable.

**Decisión: KSP.**

### 1.2 ¿Qué versión de Hilt?

Dos fuentes complementarias, y conviene mirar ambas:

1. **La documentación de setup** (para saber *cómo* se configura):
   - <https://dagger.dev/hilt/gradle-setup> (proyecto Dagger, se actualiza con cada release)
   - <https://developer.android.com/training/dependency-injection/hilt-android>
2. **El repositorio Maven** (para saber *cuál es la última versión real*).
   Hilt se publica en **Maven Central** con la coordenada
   `com.google.dagger:hilt-android`:
   - <https://central.sonatype.com/artifact/com.google.dagger/hilt-android>
   - Alternativa: <https://mvnrepository.com/artifact/com.google.dagger/hilt-android>

> **Lección aprendida al hacer esta guía**: el día de la consulta
> (18-07-2026), developer.android.com mostraba `2.57.1` en sus snippets,
> mientras que dagger.dev y Maven Central ya publicaban **`2.60.1`**. La
> documentación enseña la *estructura* del setup, pero la versión fiable es la
> del repositorio Maven, que es donde Gradle descarga los artefactos.

**Candidata inicial: Hilt 2.60.1** (última estable en Maven Central). Pero
"última" no significa "adecuada" — falta un paso.

### 1.3 Comprobar compatibilidad con TU proyecto (el paso que casi nadie hace)

Al probar Hilt 2.60.1 en este proyecto, la compilación falló con este error
real:

```
> Failed to apply plugin 'com.google.dagger.hilt.android'.
   > The Hilt Android Gradle plugin is only compatible with Android Gradle
     plugin (AGP) version 9.0.0 or higher (found Android Gradle Plugin version 8.11.1).
```

La versión más nueva de una librería puede exigir un toolchain más nuevo que el
del proyecto. ¿Cómo se resuelve metódicamente?

1. Identifica qué exige la versión nueva: el propio mensaje de error lo dice
   (AGP ≥ 9.0.0), y lo confirman las **release notes oficiales**:
   <https://github.com/google/dagger/releases>. Allí consta:
   - **2.59** (enero 2026): "AGP 9 is now a requirement along with AGP 9's own
     requirements like Gradle 9.1+".
   - **2.58**: última versión compatible con AGP 8.x.
2. Decide entre dos caminos:
   - **Subir el toolchain** (AGP 9 + Gradle 9.1+): es un cambio grande, con sus
     propias rupturas. Merece su propia lección, no colarse en esta.
   - **Usar la última versión de la librería compatible con tu toolchain**:
     Hilt 2.58.

**Decisión final: Hilt 2.58.** Regla general: la versión adecuada es la más
alta cuyo requisito de toolchain (AGP/Gradle/Kotlin) cumples. Las release
notes del proyecto son la fuente para saberlo.

### 1.4 ¿Qué versión de KSP?

KSP es un plugin del compilador de Kotlin, así que **debe casar con la versión
de Kotlin del proyecto**. Su versionado lo hace explícito:

```
2.1.21-2.0.2
└─┬──┘ └─┬─┘
Kotlin   KSP
```

Pasos:

1. Mira la versión de Kotlin del proyecto en `gradle/libs.versions.toml`
   (aquí: `kotlin = "2.1.21"`).
2. Busca la última versión de KSP cuyo **prefijo** sea exactamente esa:
   - Fuente oficial: <https://github.com/google/ksp/releases>
   - Si la página de releases no muestra tu versión (solo lista las últimas),
     consulta Maven Central. Se puede hacer desde la web o con la API de
     búsqueda desde la terminal:

```bash
curl -s "https://search.maven.org/solrsearch/select?q=g:com.google.devtools.ksp+AND+a:symbol-processing-api&core=gav&rows=100&wt=json" \
  | python3 -c "import json,sys; print([d['v'] for d in json.load(sys.stdin)['response']['docs'] if d['v'].startswith('2.1.21')])"
# Resultado: ['2.1.21-2.0.2', '2.1.21-2.0.1', '2.1.21-RC2-2.0.1', '2.1.21-RC-2.0.0']
```

**Decisión: KSP 2.1.21-2.0.2** (la más alta estable para Kotlin 2.1.21).

> Nota: las versiones más nuevas de KSP (2.3.x) han dejado de llevar el prefijo
> de Kotlin, pero requieren Kotlin más moderno. Mientras el proyecto use Kotlin
> 2.1.21, la regla del prefijo es la que aplica. Si en el futuro se actualiza
> Kotlin, habrá que actualizar KSP a la par.

### Resumen de decisiones

| Pieza | Versión | Fuente de la decisión |
|---|---|---|
| Procesador | KSP (no kapt) | developer.android.com/build/migrate-to-ksp |
| Hilt | 2.58 (no 2.60.1: exige AGP 9) | Maven Central + release notes de Dagger |
| KSP | 2.1.21-2.0.2 | Maven Central, filtrado por Kotlin 2.1.21 |

---

## Parte 2 — Cambios en el proyecto

Hilt necesita cinco cambios: catálogo de versiones, plugin raíz, plugin del
módulo + dependencias, una clase `Application` anotada, y (para poder inyectar
en el futuro) anotar la Activity.

### 2.1 Catálogo de versiones (`gradle/libs.versions.toml`)

```toml
[versions]
hilt = "2.58"          # última compatible con AGP 8.x (2.59+ exige AGP 9)
ksp = "2.1.21-2.0.2"   # el prefijo DEBE coincidir con la versión de kotlin

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }

[plugins]
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### 2.2 `build.gradle.kts` raíz — declarar los plugins sin aplicarlos

```kotlin
plugins {
    // ...
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}
```

`apply false` significa: "descarga y fija la versión aquí, pero aplícalo solo
en los módulos que lo pidan".

### 2.3 `app/build.gradle.kts` — aplicar plugins y añadir dependencias

```kotlin
plugins {
    // ...
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

dependencies {
    // ...
    implementation(libs.hilt.android)   // la librería (runtime)
    ksp(libs.hilt.compiler)             // el generador de código (solo compila)
}
```

Fíjate en la configuración `ksp(...)`: el compilador de Hilt no viaja dentro
del APK, solo genera código durante la compilación.

### 2.4 Clase `Application` con `@HiltAndroidApp`

Hilt necesita una clase `Application` propia donde ancla el contenedor de
dependencias de nivel aplicación. Nuevo archivo
`app/src/main/java/com/aprender/holaandroid/HolaAndroidApp.kt`:

```kotlin
@HiltAndroidApp
class HolaAndroidApp : Application()
```

Y hay que **registrarla en el `AndroidManifest.xml`**, o Android seguirá usando
la `Application` genérica y Hilt nunca se inicializará:

```xml
<application
    android:name=".HolaAndroidApp"
    ... >
```

### 2.5 `@AndroidEntryPoint` en la Activity

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() { ... }
```

Marca la Activity como punto de entrada del grafo de dependencias. Todavía no
inyectamos nada, pero con esto la puerta queda abierta para la siguiente
lección.

---

## Parte 3 — Verificar

```bash
cd HolaAndroid
./gradlew assembleDebug
```

Si compila, Hilt está bien integrado: sus anotaciones fallan en *compilación*
(no en ejecución) cuando algo está mal montado, que es precisamente su gran
ventaja frente a otros contenedores DI.

Puedes ver el código que Hilt ha generado (no se toca, solo para curiosear):

```bash
ls app/build/generated/ksp/debug/java/com/aprender/holaandroid/
# Hilt_MainActivity.java  HolaAndroidApp_GeneratedInjector.java
# MainActivity_GeneratedInjector.java  component_sources  component_trees
```

## Errores típicos en este paso

| Síntoma | Causa |
|---|---|
| `Expected @HiltAndroidApp to have a value...` al compilar | Falta el plugin `hilt-android` en el módulo app |
| Crash al arrancar: `Hilt Activity must be attached to @HiltAndroidApp Application` | Se anotó la Activity pero la `Application` no está en el Manifest |
| `ksp-x.y.z is too old for kotlin-a.b.c` (o viceversa) | El prefijo de la versión de KSP no coincide con la de Kotlin |
| `...only compatible with Android Gradle plugin (AGP) version 9.0.0 or higher` | Hilt ≥ 2.59 con AGP 8.x: baja Hilt a 2.58 o sube el toolchain |

## Fuentes consultadas (18-07-2026)

- Setup oficial de Hilt: <https://dagger.dev/hilt/gradle-setup>
- Guía de Hilt en Android: <https://developer.android.com/training/dependency-injection/hilt-android>
- Migración kapt → KSP: <https://developer.android.com/build/migrate-to-ksp>
- Versiones de Hilt: <https://central.sonatype.com/artifact/com.google.dagger/hilt-android>
- Release notes de Dagger/Hilt (requisitos de AGP): <https://github.com/google/dagger/releases>
- Versiones de KSP: <https://github.com/google/ksp/releases> y API de Maven Central
