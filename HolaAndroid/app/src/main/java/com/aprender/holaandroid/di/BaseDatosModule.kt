package com.aprender.holaandroid.di

import android.content.Context
import androidx.room.Room
import com.aprender.holaandroid.data.local.AppDatabase
import com.aprender.holaandroid.data.local.FraseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * La base de datos se construye UNA vez (@Singleton): abrir SQLite es caro y
 * dos instancias sobre el mismo fichero acaban en corrupción o bloqueos.
 * Los DAOs se piden a la base de datos, nunca se construyen a mano.
 */
@Module
@InstallIn(SingletonComponent::class)
object BaseDatosModule {

    @Provides
    @Singleton
    fun proveeBaseDatos(@ApplicationContext contexto: Context): AppDatabase =
        Room.databaseBuilder(contexto, AppDatabase::class.java, "holaandroid.db")
            .build()

    @Provides
    fun proveeFraseDao(baseDatos: AppDatabase): FraseDao = baseDatos.fraseDao()
}
