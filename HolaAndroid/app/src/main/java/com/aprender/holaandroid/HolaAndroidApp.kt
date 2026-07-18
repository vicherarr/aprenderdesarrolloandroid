package com.aprender.holaandroid

import android.app.Application
import com.aprender.holaandroid.core.inicializacion.Inicializador
import com.aprender.holaandroid.core.registro.ArbolRelease
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

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
        // Timber se planta ANTES que nada (no va en el Set de inicializadores,
        // que no garantiza orden): el logging es infraestructura de la que
        // todos los demás dependen. Un árbol por tipo de build.
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ArbolRelease())
        inicializadores.forEach(Inicializador::inicializar)
    }
}
