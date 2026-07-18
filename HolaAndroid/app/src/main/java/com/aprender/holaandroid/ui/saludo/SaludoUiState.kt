package com.aprender.holaandroid.ui.saludo

/**
 * Estado de UI inmutable: TODO lo que la pantalla necesita pintar, en un
 * único objeto. La UI no calcula nada; solo representa este estado.
 */
data class SaludoUiState(
    val saludo: String = "",
    val contador: Int = 0,
    val esFormal: Boolean = true,
    val tarjeta: String = ""
)
