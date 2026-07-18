# Guía 43 — Navegación en Jetpack Compose (`Navigation Compose` & Type-Safe Navigation)

Objetivo: dominar el flujo de navegación entre pantallas en aplicaciones Compose utilizando la API oficial de **Type-Safe Navigation** (Navigation 2.8+ con `kotlinx.serialization`), pasando argumentos complejos con tipado estricto, animaciones de transición y gestión de *Deep Links*.

---

## 1. La API de Type-Safe Navigation

A diferencia del sistema de navegación antiguo basado en URLs escritas en String (ej. `"perfil/{usuarioId}"`), la navegación moderna utiliza objetos y clases serializables en Kotlin (`@Serializable`):

```kotlin
// Definición fuertemente tipada de rutas
@Serializable
object RutaInicio

@Serializable
data class RutaPerfil(val usuarioId: String, val esPremium: Boolean)

@Serializable
data class RutaDetalleProducto(val productoId: Int)
```

---

## 2. Dependencias (`build.gradle.kts`)

```kotlin
plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
```

---

## 3. Configuración del `NavHost` y `NavController`

```kotlin
@Composable
fun AppNavegacion() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = RutaInicio,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        // Pantalla Principal
        composable<RutaInicio> {
            PantallaInicio(
                onVerPerfil = { id ->
                    navController.navigate(RutaPerfil(usuarioId = id, esPremium = true))
                }
            )
        }

        // Pantalla de Perfil recibiendo argumento tipado automágicamente
        composable<RutaPerfil> { backStackEntry ->
            val args = backStackEntry.toRoute<RutaPerfil>()
            PantallaPerfil(
                usuarioId = args.usuarioId,
                esPremium = args.esPremium,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}
```

---

## 4. Deep Links (Abrir la App desde una URL web)

```kotlin
composable<RutaDetalleProducto>(
    deepLinks = listOf(
        navDeepLink<RutaDetalleProducto>(basePath = "https://mi-tienda.com/producto")
    )
) { backStackEntry ->
    val args = backStackEntry.toRoute<RutaDetalleProducto>()
    PantallaDetalleProducto(productoId = args.productoId)
}
```

---

## Fuentes consultadas (18-07-2026)

- Type-Safe Navigation en Compose: <https://developer.android.com/guide/navigation/design/type-safety>
- Transiciones en Navigation Compose: <https://developer.android.com/guide/navigation/navigation-compose/animation>
