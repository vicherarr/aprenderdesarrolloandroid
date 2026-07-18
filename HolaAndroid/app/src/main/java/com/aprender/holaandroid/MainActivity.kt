package com.aprender.holaandroid

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aprender.holaandroid.di.Diagnostico
import com.aprender.holaandroid.ui.SaludoViewModel
import com.aprender.holaandroid.ui.theme.HolaAndroidTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Hilt provee la factoría por defecto: viewModels() ya crea ViewModels inyectados. */
    private val viewModel: SaludoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Demostración de @EntryPoint: acceso al grafo desde un object.
        Log.d("HolaAndroid", "Saludos enviados hasta ahora: ${Diagnostico.saludosEnviados(this)}")

        setContent {
            HolaAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PantallaSaludo(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun PantallaSaludo(viewModel: SaludoViewModel, modifier: Modifier = Modifier) {
    val saludo by viewModel.saludo.collectAsState()
    val contador by viewModel.contador.collectAsState()
    val esFormal by viewModel.esFormal.collectAsState()

    ContenidoSaludo(
        saludo = saludo,
        contador = contador,
        esFormal = esFormal,
        tarjeta = viewModel.tarjeta(),
        alEnviar = viewModel::enviarSaludo,
        alCambiarTono = viewModel::alternarTono,
        modifier = modifier
    )
}

@Composable
private fun ContenidoSaludo(
    saludo: String,
    contador: Int,
    esFormal: Boolean,
    tarjeta: String,
    alEnviar: () -> Unit,
    alCambiarTono: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = saludo, style = MaterialTheme.typography.headlineSmall)
        Text(text = "Saludos enviados: $contador")
        Text(text = tarjeta, style = MaterialTheme.typography.bodySmall)
        Button(onClick = alEnviar) {
            Text("Enviar saludo")
        }
        OutlinedButton(onClick = alCambiarTono) {
            Text(if (esFormal) "Cambiar a tono informal" else "Cambiar a tono formal")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContenidoSaludoPreview() {
    HolaAndroidTheme {
        ContenidoSaludo(
            saludo = "Buenas tardes, Android.",
            contador = 3,
            esFormal = true,
            tarjeta = "Tarjeta para Android — saludos enviados: 3",
            alEnviar = {},
            alCambiarTono = {}
        )
    }
}
