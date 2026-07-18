# Guía 03 — Fundamentos de Layouts y Modificadores (`Column`, `Row`, `Box`, `Modifier`)

Objetivo: dominar los bloques fundamentales de maquetación en Jetpack Compose
y aprender el funcionamiento interno de la cadena de `Modifier` para crear
diseños limpios, adaptables y con estilo.

---

## 1. El modelo de diseño en Compose

A diferencia del sistema XML tradicional (donde las vistas se miden e inflan en árbol jerárquico), Compose utiliza una sola pasada de medición y pintado (*Single Pass Layout*).

Las tres piezas fundamentales de posicionamiento son:

| Layout | Comportamiento | Equivalente en XML / CSS |
|---|---|---|
| `Column` | Posiciona elementos en sentido vertical uno debajo de otro. | `LinearLayout (vertical)` / `flex-direction: column` |
| `Row` | Posiciona elementos en sentido horizontal uno al lado de otro. | `LinearLayout (horizontal)` / `flex-direction: row` |
| `Box` | Superpone elementos unos encima de otros en capas (z-index). | `FrameLayout` / `position: absolute` |
| `Surface` | Contenedor Material que aporta elevación, fondo, forma y color semántico. | `CardView` / `View` con background |

---

## 2. Alineación y Distribución (`Alignment` y `Arrangement`)

En `Column` y `Row`, el control del espacio se divide entre el eje principal (*main axis*) y el eje cruzado (*cross axis*):

```kotlin
// Distribución vertical en Column (Main Axis: Arrangement)
// Alineación horizontal en Column (Cross Axis: Alignment)
Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.SpaceBetween, // Top, Bottom, Center, SpaceBetween, SpaceAround, SpaceEvenly
    horizontalAlignment = Alignment.CenterHorizontally // Start, CenterHorizontally, End
) {
    Text("Elemento Superior")
    Text("Elemento Inferior")
}

// Distribución horizontal en Row (Main Axis: Arrangement)
// Alineación vertical en Row (Cross Axis: Alignment)
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(16.dp), // Espaciado fijo entre elementos
    verticalAlignment = Alignment.CenterVertically
) {
    Icon(Icons.Default.Star, contentDescription = null)
    Text("Estrella")
}
```

---

## 3. El Modificador (`Modifier`): La cadena de responsabilidad

El orden en el que aplicas los modificadores **IMPORTA**. Cada modificador envuelve al resultado del modificador anterior:

```kotlin
// EJEMPLO 1: Padding antes de Background
Box(
    modifier = Modifier
        .padding(16.dp) // 1. Aplica un margen exterior
        .background(Color.Blue) // 2. El fondo azul no incluye el padding exterior
        .size(100.dp)
)

// EJEMPLO 2: Background antes de Padding
Box(
    modifier = Modifier
        .background(Color.Blue) // 1. Aplica el fondo azul a todo el espacio disponible
        .padding(16.dp) // 2. El contenido interno quedará desplazado 16.dp hacia dentro
        .size(100.dp)
)
```

### Tabla de modificadores indispensables:

| Modificador | Función |
|---|---|
| `fillMaxSize()`, `fillMaxWidth()`, `fillMaxHeight()` | Ocupar todo el espacio disponible del padre. |
| `size(width, height)`, `width()`, `height()` | Fijar dimensiones absolutas. |
| `padding(all)` / `padding(horizontal, vertical)` | Añadir margen/separación. |
| `clip(shape)` | Recortar el contenido según una forma (`RoundedCornerShape`, `CircleShape`). |
| `background(color, shape)` | Pintar un fondo (color sólido o gradiente). |
| `clickable { ... }` | Hacer que cualquier elemento responda a pulsaciones con efecto Ripple. |
| `weight(float)` | Repartir el espacio sobrante proporcionalmente dentro de `Row` o `Column`. |

---

## 4. Ejemplo práctico: Tarjeta de Perfil de Usuario

```kotlin
@Composable
fun TarjetaPerfil(
    nombre: String,
    rol: String,
    urlFoto: String,
    onContactarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar circular con borde
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = nombre.take(1),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Información (ocupa el peso sobrante)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = rol,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Acción
            IconButton(onClick = onContactarClick) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Contactar a $nombre",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Layouts en Jetpack Compose (oficial): <https://developer.android.com/develop/ui/compose/layouts/basics>
- Modificadores en Compose: <https://developer.android.com/develop/ui/compose/layouts/modifiers>
