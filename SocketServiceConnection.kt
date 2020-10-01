package com.md.matur.service

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.md.matur.NailsApplication
import com.md.matur.utils.logd
import kotlinx.coroutines.delay


class SocketServiceConnection(val app: Application) : ServiceConnection {

    private val serviceIntent by lazy {
        Intent(app, SocketService::class.java)
    }
    var bounded = false

    /**
     * Before calling implicit get() method, checking that
     * (bounded == true) is required.
     */
    private lateinit var socketBinder: SocketService.SocketServiceBinder

    override fun onServiceConnected(className: ComponentName, serviceIBinder: IBinder) {
        socketBinder = serviceIBinder as SocketService.SocketServiceBinder
        bounded = true
    }

    override fun onServiceDisconnected(arg0: ComponentName) {
        bounded = false
    }

    suspend fun serviceBinder(): SocketService.SocketServiceBinder {
        initServiceIfNeeded()
        return socketBinder
    }

    fun disconnectService() {
        bounded = false
        socketBinder.stopService()
        app.unbindService(this)
    }

    private suspend fun initServiceIfNeeded() {
        if (!bounded) {
            connectService()
            while (!bounded)
                delay(10)
        }
    }

    private fun connectService() {
        app.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)
    }

    init {
        NailsApplication.appComponent.inject(this)
    }
}