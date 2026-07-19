# Guía 06 — Estructura de Pantalla Material 3 (`Scaffold`, `TopAppBar`, `NavigationBar`, `Drawer`)

Objetivo: dominar el componente `Scaffold` para articular barras de navegación superiores e inferiores, menús laterales (*Drawers*) y botones flotantes según los estándares visuales de Material 3.

---

## 1. El rol de `Scaffold`

`Scaffold` proporciona la estructura visual fundamental de una pantalla en Material 3, organizando automáticamente las áreas de navegación y ajustando los márgenes para el contenido principal mediante `PaddingValues`:

```kotlin
Scaffold(
    topBar = { /* Barra superior */ },
    bottomBar = { /* Barra inferior / Navegación */ },
    floatingActionButton = { /* Botón flotante */ },
    snackbarHost = { /* Host para notificaciones emergentes */ }
) { innerPadding ->
    // ¡IMPORTANTE! Aplicar innerPadding al contenedor principal del contenido
    Box(modifier = Modifier.padding(innerPadding)) {
        // Contenido de la pantalla
    }
}
```

---

## 2. Variaciones de `TopAppBar`

Material 3 define cuatro tipos de barras superiores:

| Variante | Uso |
|---|---|
| `TopAppBar` | Título pequeño alineado a la izquierda. |
| `CenterAlignedTopAppBar` | Título centrado (estilo iOS / navegación principal). |
| `MediumTopAppBar` | Título mediano que se colapsa al hacer scroll. |
| `LargeTopAppBar` | Título grande que se colapsa en barra estándar al hacer scroll. |

> ⚠️ **Opt-in obligatorio.** Las barras superiores (`TopAppBar`,
> `CenterAlignedTopAppBar`, `MediumTopAppBar`, `LargeTopAppBar`) y sus
> `scrollBehavior` todavía son API experimental de Material 3. La función que las
> use **debe** anotarse con `@OptIn(ExperimentalMaterial3Api::class)` o no
> compilará. No es que sean inestables en la práctica: es la forma en que Google
> marca APIs cuya firma aún puede cambiar.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConColapso() {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Mensajes") },
                navigationIcon = {
                    IconButton(onClick = { /* Abrir menú */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Buscar */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        // Lista scrolleable que colapsará el TopAppBar automáticamente
        LazyColumn(contentPadding = innerPadding) {
            items(50) { index ->
                Text("Mensaje #$index", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
```

---

## 3. Navegación Inferior (`NavigationBar`) y Menú Lateral (`ModalNavigationDrawer`)

```kotlin
@OptIn(ExperimentalMaterial3Api::class) // por CenterAlignedTopAppBar
@Composable
fun AppConNavegacion() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var itemSeleccionado by remember { mutableIntStateOf(0) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Opciones", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                NavigationDrawerItem(
                    label = { Text("Inicio") },
                    selected = itemSeleccionado == 0,
                    onClick = {
                        itemSeleccionado = 0
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Perfil") },
                    selected = itemSeleccionado == 1,
                    onClick = {
                        itemSeleccionado = 1
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Mi Aplicación") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir Drawer")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = itemSeleccionado == 0,
                        onClick = { itemSeleccionado = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Inicio") }
                    )
                    NavigationBarItem(
                        selected = itemSeleccionado == 1,
                        onClick = { itemSeleccionado = 1 },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Perfil") }
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { }) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir")
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Pantalla activa: $itemSeleccionado")
            }
        }
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Scaffold en Compose: <https://developer.android.com/develop/ui/compose/components/scaffold>
- Barras de app y barras de navegación M3: <https://m3.material.io/components/top-app-bar/overview>
