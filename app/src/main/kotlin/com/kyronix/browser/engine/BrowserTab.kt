package com.kyronix.browser.engine

import org.mozilla.geckoview.GeckoSession
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val session: GeckoSession = GeckoSession(),
    var title: String = "New Tab",
    var url: String = "",
    var canGoBack: Boolean = false,
    var canGoForward: Boolean = false
)
