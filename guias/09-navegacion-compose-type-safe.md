# Guía 09 — Navegación en Jetpack Compose Paso a Paso (`Navigation Compose Type-Safe`)

Objetivo: guía directa y secuencial para poner en marcha la navegación tipo-segura en Jetpack Compose (Navigation 2.8+ con `kotlinx.serialization`) desde cero, paso a paso con código listo para copiar y usar.

---

## Paso 1: Añadir dependencias al proyecto

En el archivo `HolaAndroid/app/build.gradle.kts`, añade el plugin de serialización y la dependencia de navegación:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // 1. Añadir plugin de serialization
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // 2. Añadir dependencias de Navigation Compose y kotlinx-serialization
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
```

---

## Paso 2: Definir las rutas / pantallas como clases serializables

Crea un archivo llamado `NavegacionDestinos.kt` donde defines cada pantalla como un `@Serializable` (objeto si no lleva parámetros, data class si lleva parámetros):

```kotlin
import kotlinx.serialization.Serializable

// Pantalla sin parámetros
@Serializable
object RutaInicio

// Pantalla que requiere parámetros (id de usuario y si es premium)
@Serializable
data class RutaPerfil(
    val usuarioId: String,
    val esPremium: Boolean
)
```

---

## Paso 3: Configurar el `NavHost` y declarar las pantallas

Crea la estructura del grafo de navegación asociando cada ruta `@Serializable` con su Composable:

```kotlin
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

@Composable
fun MiAplicacionNavegacion() {
    // 1. Crear el controlador de navegación
    val navController = rememberNavController()

    // 2. Definir el NavHost indicando la pantalla inicial (startDestination)
    NavHost(
        navController = navController,
        startDestination = RutaInicio
    ) {
        // Pantalla 1: Inicio
        composable<RutaInicio> {
            PantallaInicio(
                onIrAPerfil = { idUsuario ->
                    // Navegar hacia RutaPerfil pasando los argumentos requeridos
                    navController.navigate(RutaPerfil(usuarioId = idUsuario, esPremium = true))
                }
            )
        }

        // Pantalla 2: Perfil (recibe argumentos)
        composable<RutaPerfil> { backStackEntry ->
            // Extraer los argumentos del backStackEntry
            val args = backStackEntry.toRoute<RutaPerfil>()

            PantallaPerfil(
                usuarioId = args.usuarioId,
                esPremium = args.esPremium,
                onVolverAtras = {
                    navController.popBackStack() // Volver a la pantalla anterior
                }
            )
        }
    }
}
```

---

## Paso 4: Implementar los Composables de las pantallas

Crea las pantallas que reciben los eventos de navegación mediante lambdas:

```kotlin
// Pantalla de Inicio
@Composable
fun PantallaInicio(onIrAPerfil: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pantalla Principal", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onIrAPerfil("usr_12345") }) {
            Text("Ver Perfil de Usuario")
        }
    }
}

// Pantalla de Perfil
@Composable
fun PantallaPerfil(
    usuarioId: String,
    esPremium: Boolean,
    onVolverAtras: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Perfil del Usuario", style = MaterialTheme.typography.headlineMedium)
        Text("ID: $usuarioId")
        Text("Tipo: ${if (esPremium) "Cuenta Premium 🌟" else "Cuenta Gratuita"}")
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onVolverAtras) {
            Text("Volver Atras")
        }
    }
}
```

---

## Paso 5 (Recomendado): Leer argumentos dentro del ViewModel

Si utilizas ViewModel (con o sin Hilt), no necesitas leer los argumentos en la función Composable. Puedes leerlos directamente en el `ViewModel` mediante `SavedStateHandle`:

```kotlin
@HiltViewModel
class PerfilViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Extrae automáticamente los argumentos con tipado estricto
    private val args = savedStateHandle.toRoute<RutaPerfil>()

    val usuarioId = args.usuarioId
    val esPremium = args.esPremium
}
```

Uso desacoplado en el `composable`:

```kotlin
composable<RutaPerfil> {
    val viewModel: PerfilViewModel = hiltViewModel()
    PantallaPerfil(
        usuarioId = viewModel.usuarioId,
        esPremium = viewModel.esPremium,
        onVolverAtras = { navController.popBackStack() }
    )
}
```

---

## Paso 6 (Avanzado): Devolver un resultado a la pantalla anterior

Para devolver datos desde la pantalla secundaria a la principal (ej. seleccionar una opción):

```kotlin
// 1. En la pantalla origen (Inicio): Escuchar el resultado
composable<RutaInicio> { backStackEntry ->
    val resultado = backStackEntry.savedStateHandle
        .getStateFlow<String?>("dato_devuelto", null)
        .collectAsState()

    Text("Resultado recibido: ${resultado.value}")
}

// 2. En la pantalla destino (Perfil): Establecer el resultado y cerrar
navController.previousBackStackEntry
    ?.savedStateHandle
    ?.set("dato_devuelto", "Opción A seleccionada")

navController.popBackStack()
```

---

## Resumen de la secuencia de trabajo

```
[Paso 1: build.gradle.kts] ──> [Paso 2: Rutas @Serializable] ──> [Paso 3: NavHost] ──> [Paso 4: Composables]
```

---

## Fuentes consultadas (18-07-2026)

- Type-Safe Navigation en Compose (oficial): <https://developer.android.com/guide/navigation/design/type-safety>
- Navigation en Jetpack Compose: <https://developer.android.com/guide/navigation/navigation-compose>
