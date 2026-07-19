# Guía 04 — Botones, Selección y Controles de Entrada (`Button`, `TextField`, `Switch`, `Slider`)

Objetivo: dominar la familia de componentes interactivos de entrada y selección en Material 3, gestionando sus estados, estilos y comportamientos en formularios reales.

---

## 1. La familia de Botones en Material 3

Material Design 3 define una jerarquía clara de énfasis visual para guiar la atención del usuario:

| Botón | Nivel de énfasis | Uso recomendado |
|---|---|---|
| `Button` (Filled) | **Alto** | Acción principal única en la pantalla (ej. "Guardar", "Confirmar"). |
| `ElevatedButton` | **Alto-Medio** | Acciones principales sobre fondos planos o con elevado contraste. |
| `OutlinedButton` | **Medio** | Acciones secundarias importantes (ej. "Cancelar", "Atrás"). |
| `TextButton` | **Bajo** | Acciones terciarias o de bajo impacto (ej. "Saber más", "Omitir"). |
| `IconButton` | **Bajo** | Acciones representadas por iconos en barras de herramientas o listas. |
| `FloatingActionButton` (FAB) | **Destacado** | La acción primaria flotante de la pantalla (ej. "Crear nuevo"). |

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = { /* Acción */ }) {
        Text("Principal")
    }
    OutlinedButton(onClick = { /* Acción */ }) {
        Text("Secundario")
    }
    TextButton(onClick = { /* Acción */ }) {
        Text("Terciario")
    }
}
```

---

## 2. Campos de Texto (`TextField` y `OutlinedTextField`)

Los campos de entrada reciben el valor actual y un lambda `onValueChange` para actualizar el estado (*state hoisting*):

```kotlin
@Composable
fun CampoContrasena(
    valor: String,
    onValorCambiado: (String) -> Unit,
    esError: Boolean,
    mensajeError: String? = null
) {
    var visibilidad by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = valor,
        onValueChange = onValorCambiado,
        label = { Text("Contraseña") },
        placeholder = { Text("Introduce tu clave") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { visibilidad = !visibilidad }) {
                Icon(
                    imageVector = if (visibilidad) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (visibilidad) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        },
        isError = esError,
        supportingText = {
            if (esError && mensajeError != null) {
                Text(text = mensajeError, color = MaterialTheme.colorScheme.error)
            }
        },
        visualTransformation = if (visibilidad) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
```

> ⚠️ **Iconos y dependencias.** `Icons.Default.Visibility` y `VisibilityOff` **no**
> forman parte del set básico de iconos que trae Compose: viven en el paquete
> *extendido*. Para usarlos añade en `app/build.gradle.kts`:
> ```kotlin
> implementation("androidx.compose.material:material-icons-extended")
> ```
> (Iconos como `Lock`, `Home`, `Search`, `Add`, `Email` o `Menu` sí están en el
> set básico y no requieren nada extra). Lo ampliamos en la Guía 11.

**¿Por qué recibir `valor` y `onValorCambiado` en vez de guardar el texto dentro
del propio campo?** Es el patrón *state hoisting*: el estado "sube" al composable
padre (o al ViewModel), y el `TextField` se vuelve una función pura de su
entrada. Así el mismo campo se puede testear, previsualizar y reutilizar sin
sorpresas. Regla práctica: **un composable que muestra estado no debería ser
quien lo posee.**

---

## 3. Componentes de Selección (`Checkbox`, `RadioButton`, `Switch`, `Slider`)

```kotlin
@Composable
fun SeccionAjustes(
    notificacionesActivadas: Boolean,
    onNotificacionesChanged: (Boolean) -> Unit,
    volumen: Float,
    onVolumenChanged: (Float) -> Unit,
    opcionSeleccionada: Int,
    onOpcionSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Interruptor (Switch)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Activar Notificaciones")
            Switch(
                checked = notificacionesActivadas,
                onCheckedChange = onNotificacionesChanged
            )
        }

        // Slider continuo
        Column {
            Text("Nivel de Volumen: ${(volumen * 100).toInt()}%")
            Slider(
                value = volumen,
                onValueChange = onVolumenChanged,
                valueRange = 0f..1f
            )
        }

        // Grupo de RadioButtons
        Column {
            Text("Tema preferido:")
            listOf("Claro", "Oscuro", "Sistema").forEachIndexed { index, titulo ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpcionSelected(index) }
                ) {
                    RadioButton(
                        selected = (opcionSeleccionada == index),
                        onClick = { onOpcionSelected(index) }
                    )
                    Text(text = titulo, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
```

---

## 4. Ejemplo práctico: Formulario Completo de Ajustes de Perfil

```kotlin
@Composable
fun FormularioPerfil() {
    var nombre by rememberSaveable { mutableStateOf("") }
    var recibirBoletin by rememberSaveable { mutableStateOf(true) }
    var rangoBusqueda by rememberSaveable { mutableFloatStateOf(25f) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Editar Perfil", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre de usuario") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = recibirBoletin,
                    onCheckedChange = { recibirBoletin = it }
                )
                Text("Recibir novedades por correo", modifier = Modifier.padding(start = 8.dp))
            }

            Column {
                Text("Radio de búsqueda: ${rangoBusqueda.toInt()} km")
                Slider(
                    value = rangoBusqueda,
                    onValueChange = { rangoBusqueda = it },
                    valueRange = 1f..100f
                )
            }

            Button(
                onClick = { /* Guardar cambios */ },
                enabled = nombre.isNotBlank(),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Guardar Cambios")
            }
        }
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Botones en Material 3: <https://developer.android.com/design/ui/mobile/guides/components/buttons>
- Campos de texto en Compose: <https://developer.android.com/develop/ui/compose/text/user-input>
