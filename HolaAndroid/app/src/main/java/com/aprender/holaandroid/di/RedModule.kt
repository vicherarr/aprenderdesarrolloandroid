package com.aprender.holaandroid.di

import com.aprender.holaandroid.data.remote.FrasesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import timber.log.Timber

/**
 * Todo el stack de red se construye aquí, una sola vez (@Singleton):
 * crear OkHttp/Retrofit es caro y deben compartir pool de conexiones.
 */
@Module
@InstallIn(SingletonComponent::class)
object RedModule {

    private const val BASE_URL = "https://dummyjson.com/"

    @Provides
    @Singleton
    fun proveeJson(): Json = Json {
        // La API puede añadir campos nuevos sin romper la app
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun proveeOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                // El logger de OkHttp desemboca en Timber (tag fijo "OkHttp"):
                // un único grifo controla TODOS los logs de la app, y en
                // release el tráfico HTTP enmudece solo (nivel DEBUG)
                HttpLoggingInterceptor { mensaje -> Timber.tag("OkHttp").d(mensaje) }
                    .apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
            .build()

    @Provides
    @Singleton
    fun proveeRetrofit(cliente: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun proveeFrasesApi(retrofit: Retrofit): FrasesApi =
        retrofit.create(FrasesApi::class.java)
}
