package com.aprender.holaandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.aprender.holaandroid.di.Diagnostico
import com.aprender.holaandroid.ui.saludo.SaludoScreen
import com.aprender.holaandroid.ui.saludo.SaludoViewModel
import com.aprender.holaandroid.ui.theme.HolaAndroidTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * La Activity queda mínima: es solo el contenedor. La pantalla vive en
 * ui/saludo/SaludoScreen.kt y la lógica en su ViewModel.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: SaludoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Demostración de @EntryPoint (guía 04): acceso al grafo desde un object.
        Timber.d("Saludos enviados hasta ahora: %d", Diagnostico.saludosEnviados(this))

        setContent {
            HolaAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SaludoScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
