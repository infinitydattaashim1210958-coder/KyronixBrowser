package com.kyronix.browser

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kyronix.browser.databinding.ActivityBrowserBinding
import com.kyronix.browser.engine.SessionManager
import org.mozilla.geckoview.GeckoSession

class BrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserBinding
    private var isPageLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (SessionManager.tabCount == 0) {
            SessionManager.newTab("https://www.google.com")
        }

        attachCurrentTab()
        setupListeners()

        // Handle "Open with Kyronix Browser" from external apps
        intent?.dataString?.let { url -> SessionManager.currentTab?.session?.loadUri(url) }
    }

    // ── Tab Attachment ─────────────────────────────────────────

    private fun attachCurrentTab() {
        val tab = SessionManager.currentTab ?: return

        tab.session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                tab.title = title ?: "Untitled"
                runOnUiThread { binding.tvTitle.text = tab.title }
            }
            override fun onCrash(session: GeckoSession) {
                runOnUiThread {
                    Toast.makeText(this@BrowserActivity, "Page crashed — reloading…", Toast.LENGTH_SHORT).show()
                    session.reload()
                }
            }
        }

        tab.session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: List<GeckoSession.PermissionDelegate.ContentPermission>
            ) {
                tab.url = url ?: ""
                runOnUiThread {
                    if (!binding.etUrl.isFocused) binding.etUrl.setText(url ?: "")
                }
            }
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                tab.canGoBack = canGoBack
                runOnUiThread { binding.btnBack.isEnabled = canGoBack }
            }
            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                tab.canGoForward = canGoForward
                runOnUiThread { binding.btnForward.isEnabled = canGoForward }
            }
        }

        tab.session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                runOnUiThread {
                    isPageLoading = true
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.progress = 5
                    binding.btnRefresh.text = "✕"
                }
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                runOnUiThread {
                    isPageLoading = false
                    binding.progressBar.visibility = View.GONE
                    binding.btnRefresh.text = "⟳"
                }
            }
            override fun onProgressChange(session: GeckoSession, progress: Int) {
                runOnUiThread { binding.progressBar.progress = progress }
            }
            override fun onSecurityChange(
                session: GeckoSession,
                securityInfo: GeckoSession.ProgressDelegate.SecurityInformation
            ) {
                runOnUiThread {
                    binding.ivLock.text = if (securityInfo.isSecure) "🔒" else "⚠️"
                }
            }
        }

        // Display this tab's GeckoSession in the GeckoView
        binding.geckoView.setSession(tab.session)

        // Restore UI state
        binding.tvTitle.text = tab.title
        binding.etUrl.setText(tab.url)
        binding.btnBack.isEnabled = tab.canGoBack
        binding.btnForward.isEnabled = tab.canGoForward
        updateTabBadge()
    }

    // ── Listeners ──────────────────────────────────────────────

    private fun setupListeners() {
        binding.etUrl.setOnEditorActionListener { v, actionId, event ->
            val submit = actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || (event?.keyCode == KeyEvent.KEYCODE_ENTER
                    && event.action == KeyEvent.ACTION_DOWN)
            if (submit) { navigate(v.text.toString().trim()); true } else false
        }

        binding.btnBack.setOnClickListener    { SessionManager.currentTab?.session?.goBack() }
        binding.btnForward.setOnClickListener { SessionManager.currentTab?.session?.goForward() }
        binding.btnRefresh.setOnClickListener {
            if (isPageLoading) SessionManager.currentTab?.session?.stop()
            else               SessionManager.currentTab?.session?.reload()
        }

        binding.btnHome.setOnClickListener {
            navigate("https://www.google.com")
        }
        binding.btnBookmark.setOnClickListener {
            Toast.makeText(this, "Bookmarks — coming in v1.1", Toast.LENGTH_SHORT).show()
        }
        binding.btnNewTab.setOnClickListener {
            SessionManager.newTab("https://www.google.com")
            attachCurrentTab()
        }
        binding.btnTabs.setOnClickListener   { showTabsMenu() }
        binding.btnSettings.setOnClickListener {
            Toast.makeText(this, "Settings — coming in v1.1", Toast.LENGTH_SHORT).show()
        }
        binding.btnMenu.setOnClickListener   { showBrowserMenu() }
    }

    // ── Navigation ─────────────────────────────────────────────

    private fun navigate(input: String) {
        if (input.isEmpty()) return
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.startsWith("file://")                                  -> input
            input.contains(".") && !input.contains(" ")                  -> "https://$input"
            else -> "https://www.google.com/search?q=${android.net.Uri.encode(input)}"
        }
        binding.etUrl.setText(url)
        binding.etUrl.clearFocus()
        SessionManager.currentTab?.session?.loadUri(url)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etUrl.windowToken, 0)
    }

    // ── Menus ──────────────────────────────────────────────────

    private fun showBrowserMenu() {
        val popup = PopupMenu(this, binding.btnMenu)
        with(popup.menu) {
            add(0, 1, 0, "New Tab")
            add(0, 2, 0, "Bookmark This Page")
            add(0, 3, 0, "Share")
            add(0, 4, 0, "Find in Page")
            add(0, 5, 0, "Request Desktop Site")
            add(0, 6, 0, "Settings")
        }
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                1 -> { SessionManager.newTab("https://www.google.com"); attachCurrentTab() }
            }
            true
        }
        popup.show()
    }

    private fun showTabsMenu() {
        val popup = PopupMenu(this, binding.btnTabs)
        SessionManager.tabs.forEachIndexed { i, tab ->
            val label = if (tab.id == SessionManager.currentTab?.id) "✓ ${tab.title}" else tab.title
            popup.menu.add(0, i, i, label)
        }
        popup.menu.add(0, 9999, 9999, "＋ New Tab")
        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == 9999) {
                SessionManager.newTab("https://www.google.com")
            } else {
                SessionManager.switchTo(SessionManager.tabs[item.itemId].id)
            }
            attachCurrentTab()
            true
        }
        popup.show()
    }

    private fun updateTabBadge() {
        binding.tvTabCount.text = SessionManager.tabCount.toString()
    }

    // ── Back Key ───────────────────────────────────────────────

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val tab = SessionManager.currentTab
            if (tab != null && tab.canGoBack) {
                tab.session.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
