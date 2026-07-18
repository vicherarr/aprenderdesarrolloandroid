# Guía 23 — Animaciones de UI en Compose (`animate*AsState`, `AnimatedVisibility`, `Crossfade`)

Objetivo: dominar las APIs declarativas de animación en Jetpack Compose para dar vida a las interfaces con micro-interacciones, transiciones de visibilidad y cambios de estado fluidos.

---

## 1. Animación de valores simples (`animate*AsState`)

Para animar un único valor dinámico cuando cambia el estado (color, tamaño, opacidad, elevación), se utiliza la familia `animate*AsState`:

```kotlin
@Composable
fun BotonMeGustaAnimado(esFavorito: Boolean, onToggle: () -> Unit) {
    // Animación fluida de color de fondo
    val backgroundColor by animateColorAsState(
        targetValue = if (esFavorito) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ColorBoton"
    )

    // Animación de escala del icono
    val escalaIcono by animateFloatAsState(
        targetValue = if (esFavorito) 1.3f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "EscalaIcono"
    )

    IconButton(
        onClick = onToggle,
        modifier = Modifier.background(backgroundColor, shape = CircleShape)
    ) {
        Icon(
            imageVector = if (esFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Favorito",
            modifier = Modifier.scale(escalaIcono),
            tint = if (esFavorito) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

---

## 2. Visibilidad Animada (`AnimatedVisibility`)

`AnimatedVisibility` anima la entrada y salida de elementos del árbol de composición (expandir/colapsar, desplegar alertas):

```kotlin
@Composable
fun TarjetaExpandible(titulo: String, descripcion: String) {
    var expandido by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expandido = !expandido }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Icon(
                    imageVector = if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(
                visible = expandido,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
```

---

## 3. Transición de Contenidos (`Crossfade` y `AnimatedContent`)

`Crossfade` disuelve gradualmente un contenido por otro al cambiar de estado:

```kotlin
@Composable
fun SelectorDePantalla(cargando: Boolean) {
    Crossfade(targetState = cargando, animationSpec = tween(500), label = "CambioContenido") { estadoCargando ->
        if (estadoCargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("¡Datos cargados con éxito!")
            }
        }
    }
}
```

---

## 4. Tipos de Especificación de Animación (`AnimationSpec`)

| `AnimationSpec` | Descripción | Uso típico |
|---|---|---|
| `spring()` | Basado en física (rebote, masa, rigidez). | Micro-interacciones naturales al pulsar o arrastrar. |
| `tween(durationMillis, easing)` | Duración fija con curva de velocidad (`FastOutSlowInEasing`). | Transiciones de visibilidad o cambio de página. |
| `repeatable()` / `infiniteRepeatable()` | Repite una animación N veces o infinitamente. | Efectos de pulso, placeholders de carga (skeleton loaders). |

---

## Fuentes consultadas (18-07-2026)

- Animaciones en Compose (oficial): <https://developer.android.com/develop/ui/compose/animation/introduction>
- Guía de decisiones de animación: <https://developer.android.com/develop/ui/compose/animation/choose-api>
