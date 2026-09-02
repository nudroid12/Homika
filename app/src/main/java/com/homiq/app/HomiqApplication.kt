package com.homiq.app

import android.app.Application
import com.homiq.app.data.HomiqAppContainer

class HomiqApplication : Application() {
    val container: HomiqAppContainer by lazy {
        HomiqAppContainer(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        container.startBackgroundServices()
    }
}
