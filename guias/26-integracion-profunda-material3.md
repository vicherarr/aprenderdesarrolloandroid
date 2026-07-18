# Guía 26 — Integración Profunda de Material 3 y Tokens de Diseño

Objetivo: profundizar en el sistema de diseño Material 3 (M3), sus tokens de diseño, roles semánticos de color, elevación tonal (*Tonal Elevation*), adaptabilidad dinámica (*Material You*) y cómo estructurar un sistema de diseño propio basado en M3.

---

## 1. De Material 2 a Material 3: Filosofía y Tokens

Material 3 reemplaza el enfoque rígido de colores por **Tokens de Diseño**. Un token es la representación abstracta de una decisión de diseño (ej. "el fondo de una tarjeta destacada debe usar `surfaceContainerHigh`").

Las diferencias clave respecto a versiones anteriores son:

| Concepto | Material 2 (Antiguo) | Material 3 (Actual) |
|---|---|---|
| **Sombras** | Elevación por sombra z-index pura. | **Elevación Tonal**: la elevación aclara u oscurece el tono de superficie (`surfaceTint`). |
| **Bordes y Esquinas** | Esquinas pequeñas fijas (4dp). | Formas redondeadas expresivas y adaptables (`extraSmall` a `extraLarge` / `full`). |
| **Esquema de Color** | `primary`, `primaryVariant`, `secondary`. | Roles semánticos emparejados (`primary` / `onPrimary`, `primaryContainer` / `onPrimaryContainer`). |
| **Personalización de Usuario** | Tema estático definido en código. | **Dynamic Color**: extracción automática de tonos del fondo de pantalla del usuario. |

---

## 2. Mapa de Roles Semánticos de Color en M3

Para garantizar que el contraste de texto y la accesibilidad (WCAG 2.1) se cumplan automáticamente en modo claro y oscuro, Material 3 exige usar parejas de color:

```kotlin
// Regla de oro: Todo color de superficie viene acompañado de su color "on"
Surface(
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Text("El contraste está garantizado automáticamente")
}
```

### Tabla de roles principales:

| Rol | Para qué se usa | Pareja de texto/icono |
|---|---|---|
| `primary` | Elementos de mayor relevancia (FAB, botón activo). | `onPrimary` |
| `primaryContainer` | Bloques destacados o contenedores de énfasis medio. | `onPrimaryContainer` |
| `secondary` / `secondaryContainer` | Filtros, chips, elementos secundarios de la interfaz. | `onSecondary` / `onSecondaryContainer` |
| `tertiary` / `tertiaryContainer` | Acentos de color contrastantes (ej. notificaciones, badges). | `onTertiary` / `onTertiaryContainer` |
| `surface` / `surfaceVariant` | Fondos de pantalla, tarjetas y menús. | `onSurface` / `onSurfaceVariant` |
| `error` / `errorContainer` | Estados de error, alertas y acciones destructivas. | `onError` / `onErrorContainer` |

---

## 3. Integración de Colores Dinámicos (*Material You*)

Para habilitar la experiencia de personalización en Android 12+ (API level 31+), se configura la extracción dinámica en la fábrica de temas:

```kotlin
@Composable
fun MiAppMaterial3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Habilitado por defecto en dispositivos Android 12+ compatibles
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Configuración global de Ripple y contraste
    CompositionLocalProvider(
        LocalContentColor provides colorScheme.onSurface
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
```

---

## 4. Elevación Tonal (*Tonal Elevation*) vs Sombra

En M3, elevar un contenedor no solo proyecta sombra, sino que modifica el tinte de su color de fondo mezclándolo con `surfaceTint`:

```kotlin
Surface(
    modifier = Modifier.size(120.dp),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp, // Tinta la superficie en lugar de solo aplicar sombra negra
    shadowElevation = 2.dp  // Opcional: añade profundidad sutil en bordes
) {
    Box(contentAlignment = Alignment.Center) {
        Text("Tonal Elevation", style = MaterialTheme.typography.labelMedium)
    }
}
```

---

## 5. Extensión de Iconos Material (`material-icons-extended`)

Para utilizar el catálogo completo de miles de iconos oficiales de Material Design en Compose:

En `HolaAndroid/app/build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
}
```

Uso en código:
```kotlin
Icon(
    imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Soporte automático para idiomas RTL
    contentDescription = "Volver atrás"
)
```

---

## 6. Pasos para migrar una pantalla existente a Material 3 puro

1. Sustituir imports `androidx.compose.material.*` por **`androidx.compose.material3.*`**.
2. Cambiar `ScaffoldState` por la gestión directa de corrutinas con `SnackbarHostState`.
3. Reemplazar botones antiguos `ButtonDefaults.buttonColors()` por las funciones de color semánticas de M3.
4. Envolver toda la jerarquía en el `MaterialTheme` personalizado de la app.

---

## Fuentes consultadas (18-07-2026)

- Especificación oficial de Material 3: <https://m3.material.io/>
- Migración de Material 2 a Material 3 en Compose: <https://developer.android.com/develop/ui/compose/designsystems/material2-material3>
- Guía de tokens de color: <https://m3.material.io/styles/color/the-color-system/color-roles>
