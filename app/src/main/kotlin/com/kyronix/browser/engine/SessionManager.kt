package com.kyronix.browser.engine

import com.kyronix.browser.KyronixApp

object SessionManager {

    private val _tabs = mutableListOf<BrowserTab>()
    val tabs: List<BrowserTab> get() = _tabs.toList()
    val tabCount: Int get() = _tabs.size

    var currentIndex: Int = -1
        private set

    val currentTab: BrowserTab?
        get() = if (currentIndex in _tabs.indices) _tabs[currentIndex] else null

    /** Open a new tab, make it current, optionally load a URL. */
    fun newTab(url: String = ""): BrowserTab {
        val tab = BrowserTab()
        tab.session.open(KyronixApp.runtime)
        _tabs.add(tab)
        currentIndex = _tabs.lastIndex
        if (url.isNotBlank()) tab.session.loadUri(url)
        return tab
    }

    /** Switch current tab by ID. */
    fun switchTo(id: String): Boolean {
        val index = _tabs.indexOfFirst { it.id == id }
        return if (index >= 0) { currentIndex = index; true } else false
    }

    /** Close a tab by ID. Adjusts currentIndex safely. */
    fun closeTab(id: String) {
        val index = _tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        _tabs[index].session.close()
        _tabs.removeAt(index)
        currentIndex = when {
            _tabs.isEmpty()            -> -1
            currentIndex >= _tabs.size -> _tabs.lastIndex
            else                       -> currentIndex
        }
    }

    fun closeAll() {
        _tabs.forEach { it.session.close() }
        _tabs.clear()
        currentIndex = -1
    }
}
