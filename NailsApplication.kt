package com.md.matur

import android.app.Application
import android.content.Intent
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.md.matur.data.repository.NailsApiService
import com.md.matur.data.repository.NailsApiService.AppVersion
import com.md.matur.di.component.AppComponent
import com.md.matur.di.component.DaggerAppComponent
import com.md.matur.di.module.MainModule
import com.md.matur.feature.base.splash.UpdateActivity
import com.md.matur.service.SocketServiceConnection
import com.md.matur.utils.*
import com.md.matur.utils.extensions.showToast
import com.md.matur.utils.extensions.showYesOrNoDialog
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.search.SearchFactory
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.system.measureTimeMillis

class NailsApplication : Application() {

    @Inject
    lateinit var nailsApiService: NailsApiService

    @Inject
    lateinit var config: Config

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.builder().mainModule(MainModule(this)).build()

        asyncOnIOThread {
            delay(4000)
            appComponent.inject(this@NailsApplication)
            if (config.logged)
                nailsApiService.loggedCheck(tokenString = config.token)
                    .subscribeBy(onSuccess = {
                        logd("LoggedCheck success")
                    }, onError = {
                        logd("LoggedCheck FAILED")
                    })
        }
    }

    companion object {
        lateinit var appComponent: AppComponent

        val authComponent by lazy {
            appComponent.authComponent().build()
        }

        val masterProfileComponent by lazy {
            appComponent.masterProfileComponent().build()
        }

        val loggedUserComponent by lazy {
            appComponent.loggedUserComponent().build()
        }
    }
}

