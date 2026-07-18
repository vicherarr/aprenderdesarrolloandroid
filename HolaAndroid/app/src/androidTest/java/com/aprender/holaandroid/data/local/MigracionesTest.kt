package com.aprender.holaandroid.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La migración 1→2 de la guía 08, convertida en test automático. El helper
 * usa los JSON de app/schemas/ (que el plugin de Room exporta y versionamos
 * en git) para crear una base de datos EXACTAMENTE como era en la versión 1
 * — aunque el código de FraseEntity ya no tenga esa forma.
 *
 * runMigrationsAndValidate hace las dos cosas que probamos a mano en el
 * emulador: ejecuta la migración Y valida que el esquema resultante es
 * idéntico al que espera la versión 2 (defaultValue incluido, guía 08 §2).
 */
@RunWith(AndroidJUnit4::class)
class MigracionesTest {

    private val nombreBd = "migracion-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrar1a2ConservaLasFilasYFavoritaNaceEn0() {
        // Base de datos en VERSIÓN 1 (desde schemas/1.json) con un usuario dentro
        helper.createDatabase(nombreBd, 1).apply {
            execSQL(
                "INSERT INTO frases (id, texto, autor, guardada_en) " +
                    "VALUES (42, 'texto', 'autora', 100)"
            )
            close()
        }

        // Ejecuta MIGRACION_1_2 y valida el esquema contra schemas/2.json
        val bd = helper.runMigrationsAndValidate(nombreBd, 2, true, MIGRACION_1_2)

        bd.query("SELECT id, autor, favorita FROM frases").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(42, cursor.getInt(0))          // la fila sobrevivió
            assertEquals("autora", cursor.getString(1))
            assertEquals(0, cursor.getInt(2))           // favorita nace en 0
            assertEquals(1, cursor.count)
        }
    }
}
