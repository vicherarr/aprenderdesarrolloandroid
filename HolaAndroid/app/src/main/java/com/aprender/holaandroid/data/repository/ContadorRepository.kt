package com.aprender.holaandroid.data.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato del repositorio. El resto de la app depende de esta interfaz,
 * nunca de la implementación: facilita testear (Fake) y cambiar el origen
 * de los datos sin tocar UI ni dominio.
 */
interface ContadorRepository {
    /** Única fuente de verdad (SSOT) del contador de saludos. */
    val contador: StateFlow<Int>

    fun incrementar()
}
