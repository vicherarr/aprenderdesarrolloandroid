package com.aprender.holaandroid.ui.saludo

/**
 * Estado de UI inmutable: TODO lo que la pantalla necesita pintar, en un
 * único objeto. La UI no calcula nada; solo representa este estado.
 */
data class SaludoUiState(
    val saludo: String = "",
    val contador: Int = 0,
    val esFormal: Boolean = true,
    val tarjeta: String = "",
    val frase: FraseUiState = FraseUiState.Inicial
)

/**
 * Estado de una carga asíncrona modelado como jerarquía sellada: la UI está
 * OBLIGADA por el compilador (when exhaustivo) a pintar los cuatro casos.
 */
sealed interface FraseUiState {
    data object Inicial : FraseUiState
    data object Cargando : FraseUiState
    data class Exito(val texto: String, val autor: String) : FraseUiState
    data class Error(val mensaje: String) : FraseUiState
}
