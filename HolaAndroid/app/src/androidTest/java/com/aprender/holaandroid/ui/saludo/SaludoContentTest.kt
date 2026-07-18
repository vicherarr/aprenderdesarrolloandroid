package com.aprender.holaandroid.ui.saludo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aprender.holaandroid.domain.frase.Frase
import com.aprender.holaandroid.ui.theme.HolaAndroidTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests de UI sobre la pantalla SIN estado: se le pasa un SaludoUiState
 * fabricado y se afirma sobre el árbol de SEMÁNTICA (lo mismo que "ve" la
 * accesibilidad). Ni ViewModel, ni Hilt, ni app completa: la división
 * stateless/stateful de la guía 05 pagando su último dividendo.
 */
@RunWith(AndroidJUnit4::class)
class SaludoContentTest {

    @get:Rule
    val compose = createComposeRule()

    private fun estadoCon(
        frase: FraseUiState = FraseUiState.Inicial,
        guardadas: List<Frase> = emptyList(),
        esFormal: Boolean = true
    ) = SaludoUiState(
        saludo = "Hola, Ada.",
        contador = 0,
        esFormal = esFormal,
        tarjeta = "tarjeta",
        frase = frase,
        frasesGuardadas = guardadas
    )

    private fun componer(
        estado: SaludoUiState,
        alPedirFrase: () -> Unit = {},
        alAlternarFavorita: (Frase) -> Unit = {}
    ) {
        compose.setContent {
            HolaAndroidTheme {
                SaludoContent(
                    uiState = estado,
                    alEnviar = {},
                    alCambiarTono = {},
                    alPedirFrase = alPedirFrase,
                    alAlternarFavorita = alAlternarFavorita
                )
            }
        }
    }

    @Test
    fun estadoInicial_muestraLaInvitacionYElBotonHabilitado() {
        componer(estadoCon(frase = FraseUiState.Inicial))

        compose.onNodeWithText("Pulsa \"Frase del día\" para pedir una a la red.")
            .assertIsDisplayed()
        compose.onNodeWithText("Frase del día").assertIsEnabled()
    }

    @Test
    fun estadoCargando_muestraElSpinnerYDeshabilitaElBoton() {
        componer(estadoCon(frase = FraseUiState.Cargando))

        compose.onNodeWithTag("cargando").assertIsDisplayed()
        compose.onNodeWithText("Frase del día").assertIsNotEnabled()
    }

    @Test
    fun estadoExito_muestraFraseYAutor() {
        componer(estadoCon(frase = FraseUiState.Exito("texto", "autora")))

        compose.onNodeWithText("“texto”").assertIsDisplayed()
        compose.onNodeWithText("— autora").assertIsDisplayed()
    }

    @Test
    fun estadoError_muestraElMensaje() {
        componer(estadoCon(frase = FraseUiState.Error("No se pudo cargar la frase. ¿Hay conexión?")))

        compose.onNodeWithText("No se pudo cargar la frase. ¿Hay conexión?").assertIsDisplayed()
    }

    @Test
    fun pulsarElBoton_invocaElCallback() {
        var pulsado = false
        componer(estadoCon(), alPedirFrase = { pulsado = true })

        compose.onNodeWithText("Frase del día").performClick()

        assertTrue(pulsado)
    }

    @Test
    fun lasGuardadas_muestranCabeceraConElTotalYEstrellaEnLaFavorita() {
        componer(
            estadoCon(
                guardadas = listOf(
                    Frase(1, "memoria", "Shakespeare", esFavorita = true),
                    Frase(2, "guardar", "Anónimo")
                )
            )
        )

        compose.onNodeWithText("Guardadas en el dispositivo (2) — toca una para ★")
            .assertIsDisplayed()
        compose.onNodeWithText("★ “memoria” — Shakespeare").assertIsDisplayed()
        compose.onNodeWithText("“guardar” — Anónimo").assertIsDisplayed()
    }

    @Test
    fun tocarUnaGuardada_invocaElCallbackConEsaFraseExacta() {
        var tocada: Frase? = null
        val frase = Frase(2, "guardar", "Anónimo")
        componer(
            estadoCon(guardadas = listOf(Frase(1, "memoria", "Shakespeare"), frase)),
            alAlternarFavorita = { tocada = it }
        )

        compose.onNodeWithText("“guardar” — Anónimo").performClick()

        assertEquals(frase, tocada)
    }

    @Test
    fun elBotonDeTono_anunciaElCambioContrario() {
        componer(estadoCon(esFormal = true))

        compose.onNodeWithText("Cambiar a tono informal").assertIsDisplayed()
    }
}
