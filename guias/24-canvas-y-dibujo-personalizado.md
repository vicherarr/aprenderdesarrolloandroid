# Guía 24 — Dibujo Personalizado y Canvas (`Canvas`, `Path`, `Brush`, `drawWithContent`)

Objetivo: aprender a dibujar formas geométricas vectoriales avanzadas, trazados gráficos personalizados (`Path`), degradados (`Brush`) y gráficos de datos mediante el componente `Canvas` de Jetpack Compose.

---

## 1. El componente `Canvas`

`Canvas` te otorga control píxel a píxel sobre el área de dibujo accediendo al contexto `DrawScope`:

```kotlin
@Composable
fun DemoCirculoGradiente() {
    Canvas(modifier = Modifier.size(200.dp)) {
        // El tamaño del canvas está disponible en `size` (width, height)
        val centro = Offset(size.width / 2, size.height / 2)
        val radio = size.minDimension / 2

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFD700), Color(0xFFFF4500)),
                center = centro,
                radius = radio
            ),
            center = centro,
            radius = radio
        )
    }
}
```

---

## 2. Dibujar trazados vectoriales complejos (`Path`)

Con `Path` se pueden dibujar polígonos, curvas Bézier y figuras complejas no estándar:

```kotlin
@Composable
fun GraficoDeLineas(puntos: List<Float>) {
    val colorLinea = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(16.dp)
    ) {
        if (puntos.isEmpty()) return@Canvas

        val pasoX = size.width / (puntos.size - 1)
        val maxY = puntos.maxOrNull() ?: 1f
        val minY = puntos.minOrNull() ?: 0f
        val rangoY = (maxY - minY).coerceAtLeast(1f)

        val path = Path().apply {
            puntos.forEachIndexed { i, valor ->
                val x = i * pasoX
                val y = size.height - ((valor - minY) / rangoY * size.height)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        // Dibujar línea del gráfico
        drawPath(
            path = path,
            color = colorLinea,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
```

---

## 3. Modificadores de Dibujo (`Modifier.drawBehind` y `drawWithContent`)

Cuando no necesitas un composable `Canvas` separado, puedes añadir capacidades de dibujo directamente a cualquier elemento mediante modificadores:

```kotlin
// Indicador personalizado dibujado detrás del contenido
Text(
    text = "Texto con fondo destacado",
    modifier = Modifier
        .padding(8.dp)
        .drawBehind {
            drawRoundRect(
                color = Color(0xFFFFF59D),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
        }
)
```

---

## 4. Ejemplo práctico: Medidor Circular de Progreso Personalizado

```kotlin
@Composable
fun MedidorCircularProgreso(
    progreso: Float, // 0.0 a 1.0
    modifier: Modifier = Modifier
) {
    val colorFondo = MaterialTheme.colorScheme.surfaceVariant
    val colorProgreso = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(120.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grosor = 12.dp.toPx()

            // Pista de fondo
            drawArc(
                color = colorFondo,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = grosor, cap = StrokeCap.Round)
            )

            // Arco de progreso activo
            drawArc(
                color = colorProgreso,
                startAngle = 135f,
                sweepAngle = 270f * progreso,
                useCenter = false,
                style = Stroke(width = grosor, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "${(progreso * 100).toInt()}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Graphics y Canvas en Compose: <https://developer.android.com/develop/ui/compose/graphics/draw/overview>
- Modificadores de dibujo: <https://developer.android.com/develop/ui/compose/graphics/draw/modifiers>
