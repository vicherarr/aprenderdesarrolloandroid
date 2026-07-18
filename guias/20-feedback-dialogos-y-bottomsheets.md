# Guía 20 — Feedback de Usuario, Diálogos y Menús Desplegables (`Snackbar`, `AlertDialog`, `ModalBottomSheet`)

Objetivo: dominar las alertas, notificaciones emergentes, láminas inferiores interactivas y menús contextuales para guiar y confirmar acciones del usuario en Jetpack Compose.

---

## 1. Notificaciones efímeras (`Snackbar` y `SnackbarHostState`)

Los `Snackbars` informan sobre el resultado de una operación rápida (ej. "Elemento eliminado") con opción de deshacer.

```kotlin
@Composable
fun DemoSnackbar() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                scope.launch {
                    val resultado = snackbarHostState.showSnackbar(
                        message = "Archivo enviado a la papelera",
                        actionLabel = "Deshacer",
                        duration = SnackbarDuration.Short
                    )
                    if (resultado == SnackbarResult.ActionPerformed) {
                        // Código para restaurar el archivo
                    }
                }
            }) {
                Text("Eliminar Elemento")
            }
        }
    }
}
```

---

## 2. Diálogos de Confirmación (`AlertDialog`)

Los diálogos interrumpe la navegación del usuario para requerir una confirmación o decisión importante:

```kotlin
@Composable
fun DialogoConfirmacionEliminar(
    mostrarDialogo: Boolean,
    onConfirmar: () -> Unit,
    onDescartar: () -> Unit
) {
    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = onDescartar,
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("¿Eliminar la cuenta?") },
            text = { Text("Esta acción es irreversible. Se borrarán todos los datos almacenados.") },
            confirmButton = {
                TextButton(
                    onClick = onConfirmar,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDescartar) {
                    Text("Cancelar")
                }
            }
        )
    }
}
```

---

## 3. Láminas Deslizantes (`ModalBottomSheet`)

`ModalBottomSheet` es el componente predilecto de Material 3 para mostrar acciones complementarias, filtros o menús de compartir desde la parte inferior de la pantalla:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoBottomSheet() {
    var mostrarSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Button(onClick = { mostrarSheet = true }) {
        Text("Opciones de Archivo")
    }

    if (mostrarSheet) {
        ModalBottomSheet(
            onDismissRequest = { mostrarSheet = false },
            sheetState = sheetState
        ) {
            // Contenido de la lámina
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Opciones disponibles", style = MaterialTheme.typography.titleMedium)

                ListItem(
                    headlineContent = { Text("Compartir enlace") },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable { mostrarSheet = false }
                )
                ListItem(
                    headlineContent = { Text("Descargar archivo") },
                    leadingContent = { Icon(Icons.Default.Download, contentDescription = null) },
                    modifier = Modifier.clickable { mostrarSheet = false }
                )
            }
        }
    }
}
```

---

## 4. Menús Desplegables (`DropdownMenu` y `ExposedDropdownMenuBox`)

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorOpciones(
    opciones: List<String>,
    opcionSeleccionada: String,
    onOpcionSelected: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = !expandido }
    ) {
        OutlinedTextField(
            value = opcionSeleccionada,
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoría") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        onOpcionSelected(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Dialogs en Material 3: <https://m3.material.io/components/dialogs/overview>
- Bottom Sheets en Compose: <https://developer.android.com/develop/ui/compose/components/bottom-sheets>
