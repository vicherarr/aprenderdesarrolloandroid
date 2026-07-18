# Guía 43 — Navegación en Jetpack Compose (`Navigation Compose` & Type-Safe Navigation)

Objetivo: dominar el flujo de navegación entre pantallas en aplicaciones Compose utilizando la API oficial de **Type-Safe Navigation** (Navigation 2.8+ con `kotlinx.serialization`), inyección de argumentos directamente en ViewModels con `SavedStateHandle`, paso de tipos complejos con `NavType` personalizado, modularización por subgrafos y devolución de resultados entre pantallas.

---

## 1. La API de Type-Safe Navigation

A diferencia del sistema de navegación antiguo basado en rutas String frágiles (ej. `"perfil/{usuarioId}"`), la arquitectura moderna de Google utiliza objetos y clases serializables en Kotlin (`@Serializable`):

```kotlin
import kotlinx.serialization.Serializable

// Definición fuertemente tipada de destinos
@Serializable
object RutaInicio

@Serializable
data class RutaPerfil(val usuarioId: String, val esPremium: Boolean)

@Serializable
data class RutaEditarProducto(val producto: ProductoParcelable)
```

---

## 2. Inyección Directa en ViewModel con `SavedStateHandle` (Práctica Recomendada #1)

La recomendación oficial de arquitectura de Android es **NO pasar los argumentos de navegación manualmente en la función Composable**, sino inyectarlos directamente dentro del `ViewModel` desde `SavedStateHandle` usando `toRoute<T>()`:

```kotlin
@HiltViewModel
class PerfilViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerUsuarioUseCase: ObtenerUsuarioUseCase
) : ViewModel() {

    // Extracción automática y type-safe de los argumentos desde la ruta
    private val args = savedStateHandle.toRoute<RutaPerfil>()

    val usuarioId: String = args.usuarioId
    val esPremium: Boolean = args.esPremium

    init {
        Log.d("PerfilVM", "Cargando datos para usuarioId=${args.usuarioId}, esPremium=${args.esPremium}")
    }
}
```

De este modo, el Composable queda 100% limpio y desacoplado:

```kotlin
composable<RutaPerfil> {
    // hiltViewModel() obtiene automáticamente los argumentos de la ruta mediante SavedStateHandle
    val viewModel: PerfilViewModel = hiltViewModel()
    PantallaPerfil(viewModel = viewModel, onVolver = { navController.popBackStack() })
}
```

---

## 3. Tipos Personalizados (`NavType`) para Objetos Complejos

Para pasar clases de datos compuestas o `Parcelable`, se crea un `NavType` personalizado serializado en JSON:

```kotlin
@Serializable
@Parcelize
data class ProductoParcelable(val id: Int, val nombre: String, val precio: Double) : Parcelable

// Custom NavType para Kotlin Serialization + Navigation
val ProductoNavType = object : NavType<ProductoParcelable>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): ProductoParcelable? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, ProductoParcelable::class.java)
        } else {
            @Suppress("DEPRECATION") bundle.getParcelable(key)
        }
    }

    override fun parseValue(value: String): ProductoParcelable {
        return Json.decodeFromString(Uri.decode(value))
    }

    override fun put(bundle: Bundle, key: String, value: ProductoParcelable) {
        bundle.putParcelable(key, value)
    }

    override fun serializeAsValue(value: ProductoParcelable): String {
        return Uri.encode(Json.encodeToString(value))
    }
}
```

Registro en el `composable`:

```kotlin
composable<RutaEditarProducto>(
    typeMap = mapOf(typeOf<ProductoParcelable>() to ProductoNavType)
) {
    val viewModel: EditarProductoViewModel = hiltViewModel()
    PantallaEditarProducto(viewModel = viewModel)
}
```

---

## 4. Devolución de Resultados entre Pantallas (*Pop with Result*)

Para devolver un resultado desde una pantalla de selección o edición hacia la pantalla anterior:

```kotlin
// Pantalla A (Origen): Escuchar el resultado devuelto en el SavedStateHandle
composable<RutaInicio> { backStackEntry ->
    val resultadoSeleccion = backStackEntry.savedStateHandle
        .getStateFlow<String?>("categoria_seleccionada", null)
        .collectAsState()

    PantallaInicio(
        categoria = resultadoSeleccion.value,
        onAbrirSeleccion = { navController.navigate(RutaSeleccionarCategoria) }
    )
}

// Pantalla B (Destino): Establecer resultado y volver atrás
composable<RutaSeleccionarCategoria> {
    PantallaSeleccionarCategoria(
        onCategoriaSelected = { categoria ->
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("categoria_seleccionada", categoria)
            navController.popBackStack()
        }
    )
}
```

---

## 5. Subgrafos y Modularización (`NavGraphBuilder`)

Para evitar un archivo `NavHost` gigantesco, la práctica moderna es encapsular cada flujo en funciones de extensión de `NavGraphBuilder`:

```kotlin
fun NavGraphBuilder.seccionPerfilGraph(navController: NavHostController) {
    navigation<RutaPerfilGraphGroup>(startDestination = RutaPerfilMain) {
        composable<RutaPerfilMain> {
            PantallaPerfil(onEditar = { navController.navigate(RutaEditarPerfil) })
        }
        composable<RutaEditarPerfil> {
            PantallaEditarPerfil(onGuardar = { navController.popBackStack() })
        }
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Type-Safe Navigation en Compose (oficial): <https://developer.android.com/guide/navigation/design/type-safety>
- Argumentos en ViewModel con SavedStateHandle: <https://developer.android.com/guide/navigation/design/type-safety#savedstatehandle>
- Devolver resultados en Navigation Compose: <https://developer.android.com/guide/navigation/navigation-programmatic#return_a_result>
