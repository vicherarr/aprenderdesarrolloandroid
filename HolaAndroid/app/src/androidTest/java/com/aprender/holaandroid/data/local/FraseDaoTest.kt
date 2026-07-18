package com.aprender.holaandroid.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test instrumentado: corre EN el dispositivo, contra el SQLite real.
 * El FakeFraseDao de la JVM (guía 10) prueba a los CLIENTES del DAO; este
 * test prueba al DAO MISMO: que el SQL de @Query hace lo que promete.
 *
 * inMemoryDatabaseBuilder: la base de datos vive en RAM y muere al cerrar —
 * cada test estrena base de datos, sin ficheros que limpiar.
 */
@RunWith(AndroidJUnit4::class)
class FraseDaoTest {

    private lateinit var baseDatos: AppDatabase
    private lateinit var dao: FraseDao

    @Before
    fun preparar() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        baseDatos = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        dao = baseDatos.fraseDao()
    }

    @After
    fun cerrar() {
        baseDatos.close()
    }

    private fun frase(id: Int, guardadaEn: Long, favorita: Boolean = false) =
        FraseEntity(id = id, texto = "texto $id", autor = "autor", guardadaEn = guardadaEn, favorita = favorita)

    @Test
    fun insertarConMismoIdReemplaza_elUpsertDeVerdad() = runTest {
        dao.insertar(frase(id = 1, guardadaEn = 100))
        dao.insertar(frase(id = 1, guardadaEn = 200))

        val todas = dao.observarTodas().first()

        assertEquals(1, todas.size)
        assertEquals(200, todas.single().guardadaEn)
    }

    @Test
    fun elOrdenEsFavoritasPrimeroYLuegoRecencia_elOrderByDeVerdad() = runTest {
        dao.insertar(frase(id = 1, guardadaEn = 100))   // la más antigua
        dao.insertar(frase(id = 2, guardadaEn = 200))
        dao.insertar(frase(id = 3, guardadaEn = 300))   // la más reciente

        dao.alternarFavorita(1)

        val ids = dao.observarTodas().first().map { it.id }
        assertEquals(listOf(1, 3, 2), ids)   // favorita primero, resto por recencia
    }

    @Test
    fun alternarFavoritaDosVecesVuelveAlOrigen_elNotDeSqlite() = runTest {
        dao.insertar(frase(id = 1, guardadaEn = 100))

        dao.alternarFavorita(1)
        dao.alternarFavorita(1)

        assertEquals(false, dao.observarTodas().first().single().favorita)
    }
}
