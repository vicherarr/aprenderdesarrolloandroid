package com.aprender.holaandroid.di

import android.content.Context
import com.aprender.holaandroid.data.ContadorRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * @EntryPoint: la puerta de acceso al grafo desde código donde Hilt no
 * puede inyectar (objects, ContentProviders, librerías sin Hilt...).
 * Úsalo como último recurso; lo normal es @Inject o @AndroidEntryPoint.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DiagnosticoEntryPoint {
    fun contadorRepository(): ContadorRepository
}

/** Un object no puede usar @Inject: accede al grafo vía EntryPointAccessors. */
object Diagnostico {
    fun saludosEnviados(context: Context): Int {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            DiagnosticoEntryPoint::class.java
        )
        return entryPoint.contadorRepository().contador.value
    }
}
