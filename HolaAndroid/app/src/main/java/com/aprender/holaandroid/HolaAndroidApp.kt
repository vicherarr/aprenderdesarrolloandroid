package com.aprender.holaandroid

import android.app.Application
import com.aprender.holaandroid.core.inicializacion.Inicializador
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HolaAndroidApp : Application() {

    /**
     * Field injection: en clases que instancia el sistema (Application,
     * Activity...) no controlamos el constructor, así que Hilt inyecta
     * los campos @Inject justo antes de onCreate().
     * @JvmSuppressWildcards es necesario en multibindings desde Kotlin.
     */
    @Inject
    lateinit var inicializadores: Set<@JvmSuppressWildcards Inicializador>

    override fun onCreate() {
        super.onCreate()
        inicializadores.forEach(Inicializador::inicializar)
    }
}
