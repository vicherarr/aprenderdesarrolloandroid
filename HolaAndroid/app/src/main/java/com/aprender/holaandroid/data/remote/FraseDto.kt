package com.aprender.holaandroid.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO (Data Transfer Object): calca la forma EXACTA del JSON de la API.
 * GET https://dummyjson.com/quotes/random →
 *   {"id":254,"quote":"...","author":"Rumi"}
 * No sale de la capa de datos: el repositorio lo mapea al modelo de dominio.
 */
@Serializable
data class FraseDto(
    val id: Int,
    @SerialName("quote") val texto: String,
    @SerialName("author") val autor: String
)
