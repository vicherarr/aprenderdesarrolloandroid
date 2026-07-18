package com.aprender.holaandroid.data.remote

import retrofit2.http.GET

/**
 * La fuente de datos remota. Solo se declara la interfaz: Retrofit genera
 * la implementación en runtime. Una función suspend por endpoint; Retrofit
 * la hace main-safe (ejecuta la petición en su propio pool de hilos).
 */
interface FrasesApi {

    @GET("quotes/random")
    suspend fun obtenerFraseAleatoria(): FraseDto
}
