package com.aprender.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.rememberPickerState
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

// ---- Guía 54 §2: Activity de entrada ----
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MiApp() }
    }
}

// ---- Guía 54 §4 y §6: AppScaffold + navegación deslizable ----
@Composable
fun MiApp() {
    val navController = rememberSwipeDismissableNavController()
    MaterialTheme {
        AppScaffold {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "lista"
            ) {
                composable("lista") {
                    ListaTareas(
                        tareas = listOf("Comprar", "Correr", "Leer"),
                        onAbrir = { id -> navController.navigate("detalle/$id") }
                    )
                }
                composable(
                    route = "detalle/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { backStack ->
                    Detalle(id = backStack.arguments?.getString("id"))
                }
            }
        }
    }
}

// ---- Guías 54 §5 y 55 §2/§7: lista curva + EdgeButton + rotary ----
@Composable
fun ListaTareas(tareas: List<String>, onAbrir: (Int) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val focusRequester = remember { FocusRequester() }

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = { onAbrir(-1) },
                buttonSize = EdgeButtonSize.Large,
            ) {
                Text("Añadir")
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier
                .rotaryScrollable(
                    RotaryScrollableDefaults.behavior(scrollableState = listState),
                    focusRequester = focusRequester,
                )
                .focusRequester(focusRequester)
                .focusable(),
        ) {
            items(tareas.size) { i ->
                Card(onClick = { onAbrir(i) }, modifier = Modifier.fillMaxWidth()) {
                    Text(tareas[i])
                }
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

// ---- Guías 55 §1 (Button) y §4 (SwitchButton) y §6 (Picker) ----
@Composable
fun Detalle(id: String?) {
    ScreenScaffold {
        var activado by remember { mutableStateOf(true) }
        val pickerState = rememberPickerState(initialNumberOfOptions = 60)

        TimeText()
        Text("Detalle $id")

        Button(
            onClick = { /* confirmar */ },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Confirmar") },
        )

        SwitchButton(
            checked = activado,
            onCheckedChange = { activado = it },
            label = { Text("Vibración") },
        )

        Picker(state = pickerState, contentDescription = { "Minutos" }) { index ->
            Text(text = "%02d".format(index))
        }
    }
}

@WearPreviewDevices
@Composable
fun PreviewLista() {
    MaterialTheme {
        ListaTareas(listOf("Comprar", "Correr", "Leer"), onAbrir = {})
    }
}
