package com.aprender.holaandroid.di

import com.aprender.holaandroid.data.repository.ContadorRepository
import com.aprender.holaandroid.data.repository.DefaultContadorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Cablea cada contrato de repositorio con su implementación por defecto.
 * La app entera depende de interfaces; solo este módulo conoce las clases
 * concretas.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositorioModule {

    @Binds
    abstract fun bindeaContadorRepository(impl: DefaultContadorRepository): ContadorRepository
}
