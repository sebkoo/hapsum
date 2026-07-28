package io.github.sebkoo.hapsum

import android.app.Application

class HapsumApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
