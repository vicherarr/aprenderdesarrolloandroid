package com.aprender.holaandroid.domain.frase

/**
 * Modelo de dominio: lo que la app entiende por "frase". No sabe nada de
 * JSON ni de la API (ni id, ni @SerialName): modelo por capa.
 */
data class Frase(
    val texto: String,
    val autor: String
)
