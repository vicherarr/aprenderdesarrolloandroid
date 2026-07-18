# Guía 05 — Tarjetas, Listas y Cuadrículas Eficientes (`Card`, `LazyColumn`, `LazyVerticalGrid`)

Objetivo: dominar los contenedores de tarjetas Material 3 y los componentes perezosos (*lazy layouts*) para renderizar grandes cantidades de elementos sin impactar en la memoria ni en la fluidez de fotogramas (60/120 fps).

---

## 1. La familia de Tarjetas (`Card`, `ElevatedCard`, `OutlinedCard`)

Las tarjetas agrupan información y acciones relacionadas sobre una misma superficie:

| Tipo | Variante | Uso recomendado |
|---|---|---|
| `Card` (Filled) | Superficie variante sin borde. | Tarjetas dentro de listas o cuadrículas sobre fondos neutros. |
| `ElevatedCard` | Eleva la superficie con sombra. | Resaltar contenido importante sobre un fondo plano. |
| `OutlinedCard` | Borde marcado sin elevación. | Diseños limpios, minimalistas o contenedores secundarios. |

```kotlin
OutlinedCard(
    onClick = { /* Acción al pulsar */ },
    modifier = Modifier.fillMaxWidth()
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Tarjeta Descriptiva", style = MaterialTheme.typography.titleMedium)
        Text("Detalles adicionales del elemento...", style = MaterialTheme.typography.bodySmall)
    }
}
```

---

## 2. Listas Perezosas (`LazyColumn` y `LazyRow`)

En lugar de crear un composable para cada elemento, los `LazyLayouts` sólo componen e inflan los elementos visibles en pantalla (equivalente a `RecyclerView`).

```kotlin
@Composable
fun ListaNoticias(noticias: List<Noticia>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp), // Padding alrededor de toda la lista
        verticalArrangement = Arrangement.spacedBy(12.dp) // Espaciado entre elementos
    ) {
        // Sección fija en la parte superior
        item {
            Text(
                text = "Últimas Noticias",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Encabezado pegajoso (Sticky Header)
        stickyHeader {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = "HOY",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // Lista dinámica de elementos
        items(
            items = noticias,
            key = { noticia -> noticia.id } // Clave única para optimizar recomposición y animaciones
        ) { noticia ->
            TarjetaNoticia(noticia = noticia)
        }
    }
}
```

---

## 3. Rejillas Adaptativas (`LazyVerticalGrid` y `LazyHorizontalGrid`)

Las rejillas permiten distribuir contenido en columnas bidimensionales con ajuste automático al tamaño de pantalla:

```kotlin
@Composable
fun GaleriaFotos(fotos: List<Foto>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp), // Se adapta automáticamente según el ancho disponible
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(fotos, key = { it.id }) { foto ->
            Card(
                modifier = Modifier.aspectRatio(1f) // Cuadrado perfecto
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = foto.titulo, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
```

---

## 4. Ejemplo práctico: Feed de Productos con Categorías Horizontales y Rejilla

```kotlin
data class Producto(val id: Int, val nombre: String, val precio: String)

@Composable
fun PantallaCatálogo(
    categorias: List<String>,
    productos: List<Producto>
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Carrusel de Categorías (LazyRow dentro de LazyColumn)
        item {
            Column {
                Text(
                    text = "Categorías",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, bottom = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categorias) { categoria ->
                        FilterChip(
                            selected = false,
                            onClick = { },
                            label = { Text(categoria) }
                        )
                    }
                }
            }
        }

        // Título de la sección de productos
        item {
            Text(
                text = "Productos Destacados",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Productos presentados en rejilla horizontal integrada
        items(productos.chunked(2)) { parDeProductos ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (producto in parDeProductos) {
                    ElevatedCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(producto.nombre, style = MaterialTheme.typography.titleSmall)
                            Text(producto.precio, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                // Si el grupo tiene solo 1 elemento, rellenamos el espacio con Spacer
                if (parDeProductos.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Listas perezosas en Compose: <https://developer.android.com/develop/ui/compose/lists>
- Tarjetas en Material 3: <https://m3.material.io/components/cards/overview>
