package com.aprender.holaandroid.testutil

import com.aprender.holaandroid.data.local.FraseDao
import com.aprender.holaandroid.data.local.FraseEntity
import com.aprender.holaandroid.data.repository.ContadorRepository
import com.aprender.holaandroid.data.repository.FraseRepository
import com.aprender.holaandroid.domain.frase.Frase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fakes compartidos. Regla de promoción: un fake nace privado dentro de su
 * test (guía 10) y se muda aquí cuando lo necesita una segunda suite.
 */

class FakeFraseRepository : FraseRepository {

    /** Comportamiento configurable por test: devuelve, lanza... o suspende. */
    var alPedirFrase: suspend () -> Frase = { error("alPedirFrase sin configurar") }

    /** La "tabla" observable: los tests la escriben y el flujo emite. */
    val guardadas = MutableStateFlow<List<Frase>>(emptyList())

    override suspend fun obtenerFraseAleatoria(): Frase = alPedirFrase()

    override fun observarFrasesGuardadas(): Flow<List<Frase>> = guardadas

    override suspend fun alternarFavorita(id: Int) {
        guardadas.value = guardadas.value.map {
            if (it.id == id) it.copy(esFavorita = !it.esFavorita) else it
        }
    }
}

/** Fake con estado: una "tabla" en memoria que reproduce el contrato del DAO. */
class FakeFraseDao : FraseDao {
    private val tabla = MutableStateFlow<List<FraseEntity>>(emptyList())

    override suspend fun insertar(frase: FraseEntity) {
        // Reproduce el upsert de OnConflictStrategy.REPLACE
        tabla.value = tabla.value.filterNot { it.id == frase.id } + frase
    }

    override fun observarTodas(): Flow<List<FraseEntity>> = tabla

    override suspend fun alternarFavorita(id: Int) {
        tabla.value = tabla.value.map {
            if (it.id == id) it.copy(favorita = !it.favorita) else it
        }
    }
}

class FakeContadorRepository : ContadorRepository {
    private val estado = MutableStateFlow(0)
    override val contador: StateFlow<Int> = estado
    override fun incrementar() {
        estado.value++
    }
}
