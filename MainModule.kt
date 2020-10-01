package com.md.matur.di.module

import android.app.Application
import android.content.Context
import com.md.matur.service.PlayerServiceConnection
import com.md.matur.service.SocketServiceConnection
import com.md.matur.utils.Config
import com.md.matur.utils.cacheDirNamed
import com.md.matur.utils.extensions.config
import com.md.matur.utils.messengerDirNamed
import com.md.matur.utils.portfolioDirNamed
import dagger.Module
import dagger.Provides
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
class MainModule(private val app: Application) {

    @Provides
    @Singleton
    fun provideApp(): Context = app

    @Provides
    @Singleton
    fun provideAppConfig(): Config = app.config

    @Provides
    @Named(cacheDirNamed)
    @Singleton
    fun provideCacheDir(context: Context) = context.cacheDir

    @Provides
    @Named(portfolioDirNamed)
    @Singleton
    fun providePortfolioDir(context: Context): File {
        val dir = File(context.filesDir, "portfolio")
        if (!dir.exists())
            dir.mkdir()
        return dir
    }

    @Provides
    @Named(messengerDirNamed)
    @Singleton
    fun provideMessengerDir(context: Context): File {
        val dir = File(context.filesDir, "messenger")
        if (!dir.exists())
            dir.mkdir()
        return dir
    }


    @Provides
    @Singleton
    fun providesPlayerServiceConnection() = PlayerServiceConnection(app)

    @Provides
    @Singleton
    fun providesSocketServiceConnection() = SocketServiceConnection(app)
}
