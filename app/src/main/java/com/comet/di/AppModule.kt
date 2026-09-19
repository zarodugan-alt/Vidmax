package com.comet.di

import android.content.Context
import com.comet.data.db.CometDatabase
import com.comet.engine.VideoEngine
import com.comet.engine.YtDlpEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CometDatabase =
        CometDatabase.get(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds
    @Singleton
    abstract fun bindVideoEngine(impl: YtDlpEngine): VideoEngine
}
