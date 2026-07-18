package com.aprender.holaandroid.ui.saludo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aprender.holaandroid.ui.theme.HolaAndroidTheme

/**
 * Pantalla "con estado": conecta con el ViewModel y delega el pintado.
 * collectAsStateWithLifecycle() es la forma recomendada de consumir
 * flujos en Compose (deja de coleccionar con la app en segundo plano).
 */
@Composable
fun SaludoScreen(viewModel: SaludoViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SaludoContent(
        uiState = uiState,
        alEnviar = viewModel::enviarSaludo,
        alCambiarTono = viewModel::alternarTono,
        modifier = modifier
    )
}

/**
 * Pantalla "sin estado": solo recibe datos y callbacks. Es trivial de
 * previsualizar y de testear porque no depende de nada.
 */
@Composable
private fun SaludoContent(
    uiState: SaludoUiState,
    alEnviar: () -> Unit,
    alCambiarTono: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = uiState.saludo, style = MaterialTheme.typography.headlineSmall)
        Text(text = "Saludos enviados: ${uiState.contador}")
        Text(text = uiState.tarjeta, style = MaterialTheme.typography.bodySmall)
        Button(onClick = alEnviar) {
            Text("Enviar saludo")
        }
        OutlinedButton(onClick = alCambiarTono) {
            Text(if (uiState.esFormal) "Cambiar a tono informal" else "Cambiar a tono formal")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SaludoContentPreview() {
    HolaAndroidTheme {
        SaludoContent(
            uiState = SaludoUiState(
                saludo = "Buenas tardes, Android.",
                contador = 3,
                esFormal = true,
                tarjeta = "Tarjeta para Android — saludos enviados: 3"
            ),
            alEnviar = {},
            alCambiarTono = {}
        )
    }
}
