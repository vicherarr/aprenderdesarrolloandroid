package com.aprender.holaandroid.domain.frase

/**
 * Modelo de dominio: lo que la app entiende por "frase". No sabe nada de
 * JSON ni de @SerialName: modelo por capa.
 *
 * Desde la lección 08 el dominio SÍ tiene id: al poder marcar favoritas, la
 * app necesita señalar QUÉ frase cambia, y eso es identidad. El modelo de
 * dominio evoluciona cuando los casos de uso lo exigen — no antes.
 */
data class Frase(
    val id: Int,
    val texto: String,
    val autor: String,
    val esFavorita: Boolean = false
)
