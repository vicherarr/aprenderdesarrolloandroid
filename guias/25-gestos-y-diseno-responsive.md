# Guía 25 — Gestos Táctiles Avanzados y Diseño Responsive (`pointerInput`, `WindowSizeClass`)

Objetivo: aprender a manejar gestos táctiles complejos (zoom multitáctil, arrastre, pulsación doble/larga) y adaptar la interfaz de usuario dinámicamente a distintos tamaños de pantalla (móviles, plegables y tablets) utilizando las clases de tamaño de ventana (*Window Size Classes*).

---

## 1. Detección de gestos personalizados (`pointerInput`)

Compose ofrece la función `pointerInput` para reaccionar a eventos táctiles de bajo nivel:

```kotlin
@Composable
fun ImagenConZoomYPan(
    bitmap: ImageBitmap
) {
    var escala by remember { mutableFloatStateOf(1f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                // Detectar gestos multitáctiles de escala (zoom) y paneo (traslación)
                detectTransformGestures { _, pan, zoom, _ ->
                    escala = (escala * zoom).coerceIn(1f, 5f)
                    desplazamiento += pan
                }
            }
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "Imagen zoomable",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = escala,
                    scaleY = escala,
                    translationX = desplazamiento.x,
                    translationY = desplazamiento.y
                )
        )
    }
}
```

### Gestos comunes (`detectTapGestures`):

```kotlin
Box(
    modifier = Modifier
        .size(150.dp)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { /* Al tocar */ },
                onDoubleTap = { /* Al hacer doble tap */ },
                onLongPress = { /* Al mantener pulsado */ },
                onTap = { /* Al soltar tras toque simple */ }
            )
        }
)
```

---

## 2. Diseño Responsive con *WindowSizeClass*

Material 3 divide el espacio de pantalla en tres categorías estándar (*WindowSizeClass*):

| Ancho de Pantalla | Categoría (`WindowWidthSizeClass`) | Dispositivos representativos |
|---|---|---|
| `< 600 dp` | **Compact** | Teléfonos móviles verticales. |
| `600 dp – 839 dp` | **Medium** | Teléfonos horizontales, plegables abiertos, tablets pequeñas. |
| `>= 840 dp` | **Expanded** | Tablets en horizontal, pantallas de escritorio. |

```kotlin
@Composable
fun LayoutAdaptativo(
    windowWidthSizeClass: WindowWidthSizeClass,
    contenidoLista: @Composable () -> Unit,
    contenidoDetalle: @Composable () -> Unit
) {
    when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            // En teléfonos móviles: vista de 1 sola columna (Lista O Detalle)
            contenidoLista()
        }
        WindowWidthSizeClass.Medium, WindowWidthSizeClass.Expanded -> {
            // En tablets y plegables: vista dividida de 2 columnas (Master-Detail)
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) { contenidoLista() }
                Box(modifier = Modifier.weight(2f)) { contenidoDetalle() }
            }
        }
    }
}
```

---

## 3. Ejemplo práctico: Visor Adaptativo con Pestaña / Panel lateral

```kotlin
@Composable
fun AppAdaptativaScreen(widthSizeClass: WindowWidthSizeClass) {
    val esPantallaGrande = widthSizeClass != WindowWidthSizeClass.Compact

    Row(modifier = Modifier.fillMaxSize()) {
        // Si es pantalla grande, mostramos un NavigationRail lateral en lugar del BottomBar
        if (esPantallaGrande) {
            NavigationRail {
                NavigationRailItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Inicio") }
                )
                NavigationRailItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Ajustes") }
                )
            }
        }

        Scaffold(
            bottomBar = {
                if (!esPantallaGrande) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = true,
                            onClick = { },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("Inicio") }
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text("Ajustes") }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Contenido principal adaptado")
            }
        }
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Gestos en Compose: <https://developer.android.com/develop/ui/compose/touch-input/pointer-input/introduction>
- Compatibilidad con distintos tamaños de pantalla (WindowSizeClass): <https://developer.android.com/develop/ui/compose/layouts/adaptive/window-size-classes>
