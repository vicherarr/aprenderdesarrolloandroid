package com.aprender.holaandroid.data.repository

import com.aprender.holaandroid.data.local.ContadorLocalDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementación por defecto (convención oficial de nombres: prefijo
 * "Default", o uno descriptivo como "OfflineFirst..."). Coordina la fuente
 * de datos local y expone el estado como flujo observable.
 */
@Singleton
class DefaultContadorRepository @Inject constructor(
    private val localDataSource: ContadorLocalDataSource
) : ContadorRepository {

    private val _contador = MutableStateFlow(localDataSource.leer())
    override val contador: StateFlow<Int> = _contador.asStateFlow()

    override fun incrementar() {
        val nuevo = _contador.value + 1
        localDataSource.guardar(nuevo)
        _contador.value = nuevo
    }
}
