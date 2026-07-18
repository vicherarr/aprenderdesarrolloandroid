# Guía 21 — Componentes Avanzados: Tabs, Pagers, Badges y Chips (`TabRow`, `HorizontalPager`, `Badge`)

Objetivo: incorporar patrones avanzados de navegación y etiquetado visual mediante pestañas vinculadas a páginas deslizables (*Pagers*), insignias numéricas (*Badges*) e indicadores de carga y chips.

---

## 1. Pestañas y Páginas Deslizables (`TabRow` + `HorizontalPager`)

La combinación de `PrimaryTabRow` / `SecondaryTabRow` con `HorizontalPager` permite deslizar suavemente entre vistas (patrón común en apps de mensajería y redes sociales):

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NavegacionPorPestanas() {
    val titulos = listOf("Chats", "Estados", "Llamadas")
    val pagerState = rememberPagerState(pageCount = { titulos.size })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Filas de pestañas
        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
            titulos.forEachIndexed { index, titulo ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = { Text(titulo) }
                )
            }
        }

        // Pager sincronizado
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { pagina ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("Contenido de la pestaña: ${titulos[pagina]}", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
```

---

## 2. Insignias de Notificación (`BadgedBox` y `Badge`)

`Badge` permite añadir un indicador visual (contador numérico o punto de atención) sobre iconos en barras de navegación o listas:

```kotlin
NavigationBarItem(
    selected = true,
    onClick = { },
    icon = {
        BadgedBox(
            badge = {
                Badge {
                    Text("5") // Contador de elementos no leídos
                }
            }
        ) {
            Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")
        }
    },
    label = { Text("Alertas") }
)
```

---

## 3. Chips de Acción, Filtro y Selección

Material 3 define cuatro tipos de Chips para agrupar opciones de forma compacta:

| Tipo | Función |
|---|---|
| `FilterChip` | Filtrar contenidos en listas (con check visual). |
| `InputChip` | Representar elementos introducidos por el usuario (ej. destinatarios de email). |
| `AssistChip` | Sugerir una acción rápida secundaria. |
| `SuggestionChip` | Ofrecer sugerencias dinámicas basadas en el contexto. |

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    var seleccionado by remember { mutableStateOf(false) }

    FilterChip(
        selected = seleccionado,
        onClick = { seleccionado = !seleccionado },
        label = { Text("Solo disponibles") },
        leadingIcon = if (seleccionado) {
            { Icon(Icons.Default.Check, contentDescription = null) }
        } else null
    )

    AssistChip(
        onClick = { /* Compartir */ },
        label = { Text("Compartir") },
        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
    )
}
```

---

## 4. Indicadores de Carga (`CircularProgressIndicator` y `LinearProgressIndicator`)

```kotlin
Column(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    // Indeterminado (para esperas de red de tiempo desconocido)
    CircularProgressIndicator()

    // Determinado (para descargas o progreso cuantificable de 0.0 a 1.0)
    LinearProgressIndicator(
        progress = { 0.7f },
        modifier = Modifier.fillMaxWidth()
    )
}
```

---

## Fuentes consultadas (18-07-2026)

- Pagers en Compose: <https://developer.android.com/develop/ui/compose/layouts/pager>
- Chips en Material 3: <https://m3.material.io/components/chips/overview>
