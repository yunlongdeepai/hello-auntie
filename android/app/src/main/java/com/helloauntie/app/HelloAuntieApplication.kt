package com.helloauntie.app

import android.app.Application

class HelloAuntieApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
