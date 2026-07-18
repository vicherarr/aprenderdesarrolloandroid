# Guía 10 — Sistema de Diseño y Tema Material 3 (`MaterialTheme`, Dark Mode, Dynamic Color)

Objetivo: dominar el sistema de tematización de Material Design 3 (`MaterialTheme`), definiendo paletas de color para modo claro/oscuro, tipografía coherente, formas personalizadas y soporte para Colores Dinámicos (*Material You*).

---

## 1. La anatomía del tema (`MaterialTheme`)

Un tema en Compose envuelve la jerarquía de componentes y provee valores centralizados a través de tres pilares:

```kotlin
MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = Shapes,
    content = content
)
```

Acceso universal a los valores del tema desde cualquier composable:
- `MaterialTheme.colorScheme.primary`
- `MaterialTheme.typography.titleMedium`
- `MaterialTheme.shapes.medium`

---

## 2. Paletas de Color para Modo Claro y Modo Oscuro (`ColorScheme`)

Material 3 utiliza roles semánticos de color en lugar de nombres fijos como "rojo" o "azul":

```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9)
)
```

---

## 3. Soporte para Color Dinámico (*Material You* en Android 12+)

```kotlin
@Composable
fun MiAplicacionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Extrae los colores del fondo de pantalla del usuario en Android 12+
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

---

## 4. Tipografía y Escalado de Texto

```kotlin
val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)
```

---

## Fuentes consultadas (18-07-2026)

- Tematización en Jetpack Compose: <https://developer.android.com/develop/ui/compose/designsystems/material3>
- Material 3 Color System: <https://m3.material.io/styles/color/overview>
