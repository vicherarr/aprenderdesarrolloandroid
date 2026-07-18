package com.aprender.holaandroid.di

import com.aprender.holaandroid.data.Inicializador
import com.aprender.holaandroid.data.PrimerArranqueInicializador
import com.aprender.holaandroid.data.RegistroInicializador
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * @IntoSet agrega cada binding a un Set<Inicializador> común.
 * Añadir un inicializador nuevo = añadir una línea aquí; nadie más cambia.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InicializadoresModule {

    @Binds
    @IntoSet
    abstract fun agregaRegistro(impl: RegistroInicializador): Inicializador

    @Binds
    @IntoSet
    abstract fun agregaPrimerArranque(impl: PrimerArranqueInicializador): Inicializador
}
