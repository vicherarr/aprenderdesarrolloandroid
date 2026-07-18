package com.aprender.holaandroid.core.registro

import android.util.Log
import timber.log.Timber

/**
 * El árbol de producción: descarta VERBOSE/DEBUG/INFO (ruido y posible
 * fuga de datos) y conserva solo avisos y errores. En una app real, este
 * es el sitio donde WARN/ERROR se envían a un servicio de crash reporting
 * (Crashlytics, Sentry...) en vez de —o además de— Logcat.
 */
class ArbolRelease : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean =
        priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Log.println(priority, tag ?: TAG_POR_DEFECTO, message)
        // Aquí iría: crashlytics.recordException(t) / log(message)
    }

    private companion object { const val TAG_POR_DEFECTO = "HolaAndroid" }
}
