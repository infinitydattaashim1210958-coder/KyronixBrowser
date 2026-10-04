package com.kyronix.browser

import android.app.Application
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class KyronixApp : Application() {

    companion object {
        lateinit var runtime: GeckoRuntime
            private set
    }

    override fun onCreate() {
        super.onCreate()
        val settings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .build()
        runtime = GeckoRuntime.create(this, settings)
    }
}
