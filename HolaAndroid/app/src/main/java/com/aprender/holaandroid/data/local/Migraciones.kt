package com.aprender.holaandroid.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Cada Migration lleva los datos EXISTENTES de una versión de esquema a la
 * siguiente. Room las encadena: con 1→2 y 2→3 registradas, un usuario que
 * venga de la versión 1 pasa por ambas en orden, dentro de una transacción.
 *
 * El DEFAULT 0 debe coincidir con el defaultValue de la entidad: tras migrar,
 * Room compara el esquema real con el esperado y aborta si no son idénticos.
 */
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE frases ADD COLUMN favorita INTEGER NOT NULL DEFAULT 0")
    }
}
