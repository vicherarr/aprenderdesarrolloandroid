package com.aprender.holaandroid.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * La base de datos: registra las entidades (tablas) y expone los DAOs.
 * `version` sube con cada cambio de esquema; el plugin de Gradle exporta el
 * esquema de cada versión a app/schemas/ para poder escribir migraciones.
 */
@Database(entities = [FraseEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fraseDao(): FraseDao
}
