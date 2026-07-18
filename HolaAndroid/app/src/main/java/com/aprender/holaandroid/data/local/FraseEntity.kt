package com.aprender.holaandroid.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Room = una fila de la tabla "frases". Es el modelo de la
 * PERSISTENCIA, igual que el DTO es el modelo de la RED: ninguno de los dos
 * sale de la capa de datos (modelo por capa).
 *
 * La clave primaria es el id que asigna la API: si la misma frase llega dos
 * veces, no se duplica (ver OnConflictStrategy.REPLACE en el DAO).
 */
@Entity(tableName = "frases")
data class FraseEntity(
    @PrimaryKey val id: Int,
    val texto: String,
    val autor: String,
    // Momento de guardado (epoch millis): permite ordenar por recencia
    @ColumnInfo(name = "guardada_en") val guardadaEn: Long,
    // defaultValue es el DEFAULT de SQL: debe coincidir con el que declara
    // la migración 1→2, o Room rechazará el esquema al validar (guía 08)
    @ColumnInfo(defaultValue = "0") val favorita: Boolean = false
)
