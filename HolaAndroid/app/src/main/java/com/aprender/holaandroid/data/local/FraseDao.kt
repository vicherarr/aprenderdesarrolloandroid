package com.aprender.holaandroid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object): declara las operaciones sobre la tabla y Room
 * genera la implementación (como Retrofit con la interfaz de la API).
 *
 * - Las funciones de un disparo son suspend: Room las hace main-safe.
 * - La consulta devuelve Flow: NO es suspend, y emite la lista actualizada
 *   cada vez que la tabla cambia (consulta observable).
 */
@Dao
interface FraseDao {

    /** REPLACE: si ya existe una fila con ese id, la sustituye (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(frase: FraseEntity)

    @Query("SELECT * FROM frases ORDER BY favorita DESC, guardada_en DESC")
    fun observarTodas(): Flow<List<FraseEntity>>

    /** NOT sobre un INTEGER 0/1: así se conmuta un booleano en SQLite. */
    @Query("UPDATE frases SET favorita = NOT favorita WHERE id = :id")
    suspend fun alternarFavorita(id: Int)
}
