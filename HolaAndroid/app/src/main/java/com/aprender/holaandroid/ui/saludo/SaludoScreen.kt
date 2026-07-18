package com.aprender.holaandroid.ui.saludo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aprender.holaandroid.domain.frase.Frase
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
        alPedirFrase = viewModel::cargarFrase,
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
    alPedirFrase: () -> Unit,
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
        OutlinedButton(
            onClick = alPedirFrase,
            enabled = uiState.frase != FraseUiState.Cargando
        ) {
            Text("Frase del día")
        }
        FraseSeccion(uiState.frase)
        FrasesGuardadasSeccion(uiState.frasesGuardadas, modifier = Modifier.weight(1f))
    }
}

/**
 * Lo persistido en Room: visible aunque no haya conexión. LazyColumn solo
 * compone las filas visibles (la lista crece con cada frase pedida).
 */
@Composable
private fun FrasesGuardadasSeccion(frases: List<Frase>, modifier: Modifier = Modifier) {
    if (frases.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Guardadas en el dispositivo (${frases.size})",
            style = MaterialTheme.typography.titleSmall
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(frases) { frase ->
                Text(
                    text = "“${frase.texto}” — ${frase.autor}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** El when es exhaustivo: si mañana FraseUiState gana un caso, esto no compila. */
@Composable
private fun FraseSeccion(frase: FraseUiState, modifier: Modifier = Modifier) {
    when (frase) {
        FraseUiState.Inicial ->
            Text(
                text = "Pulsa \"Frase del día\" para pedir una a la red.",
                style = MaterialTheme.typography.bodySmall
            )

        FraseUiState.Cargando ->
            CircularProgressIndicator(modifier = modifier.size(28.dp))

        is FraseUiState.Exito -> {
            Text(text = "“${frase.texto}”", style = MaterialTheme.typography.bodyMedium)
            Text(text = "— ${frase.autor}", style = MaterialTheme.typography.labelMedium)
        }

        is FraseUiState.Error ->
            Text(
                text = frase.mensaje,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
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
                tarjeta = "Tarjeta para Android — saludos enviados: 3",
                frase = FraseUiState.Exito(
                    texto = "El que tiene un porqué puede soportar casi cualquier cómo.",
                    autor = "Nietzsche"
                ),
                frasesGuardadas = listOf(
                    Frase("La memoria es el centinela del cerebro.", "Shakespeare"),
                    Frase("Lo que se guarda, se encuentra.", "Anónimo")
                )
            ),
            alEnviar = {},
            alCambiarTono = {},
            alPedirFrase = {}
        )
    }
}
