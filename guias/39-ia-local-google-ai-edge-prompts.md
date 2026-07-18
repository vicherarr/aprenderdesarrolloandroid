# Guía 39 — IA Local: Google AI Edge SDK y Generación en Streaming

Objetivo: integrar el SDK de Google AI Edge para enviar prompts a modelos locales en Android y renderizar la respuesta token a token en tiempo real (*streaming*) en la UI de Compose.

---

## 1. Configuración de dependencias (`build.gradle.kts`)

```kotlin
dependencies {
    // SDK de ejecución local respaldado por Android AICore
    implementation("com.google.ai.edge.aicore:aicore:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.8.1")
}
```

---

## 2. Generación en Streaming en tiempo real

Para que la respuesta de la IA aparezca de forma fluida a medida que el modelo infiere tokens (en lugar de esperar a que termine todo el párrafo), se utiliza `generateContentStream`:

```kotlin
class GestorIaLocal(context: Context) {
    private val model = GenerativeModel(context = context)

    fun generarRespuestaStream(prompt: String): Flow<String> = flow {
        val stream = model.generateContentStream(prompt)
        stream.collect { chunk ->
            chunk.text?.let { textoToken ->
                emit(textoToken)
            }
        }
    }.flowOn(Dispatchers.IO)
}
```

---

## 3. Integración en UI con Jetpack Compose

```kotlin
@Composable
fun PantallaAsistenteLocal(gestorIa: GestorIaLocal) {
    var prompt by remember { mutableStateOf("") }
    var respuestaCompleta by remember { mutableStateOf("") }
    var estaPensando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Asistente IA Local (Offline)", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Escribe tu consulta...") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !estaPensando
        )

        Button(
            onClick = {
                estaPensando = true
                respuestaCompleta = ""
                scope.launch {
                    gestorIa.generarRespuestaStream(prompt).collect { token ->
                        respuestaCompleta += token
                    }
                    estaPensando = false
                }
            },
            enabled = prompt.isNotBlank() && !estaPensando,
            modifier = Modifier.align(Alignment.End)
        ) {
            if (estaPensando) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Preguntar a IA Local")
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp)
        ) {
            SelectionContainer {
                Text(
                    text = if (respuestaCompleta.isEmpty() && !estaPensando) "Sin respuestas generadas aún." else respuestaCompleta,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Streaming de respuesta en Google AI Edge SDK: <https://developer.android.com/ai/aicore/generative-ai>
