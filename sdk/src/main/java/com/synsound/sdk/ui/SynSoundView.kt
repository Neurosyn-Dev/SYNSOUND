package com.synsound.sdk.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.util.AttributeSet
import android.view.View
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.synsound.sdk.R

/**
 * High-performance embeddable SynSound web platform view container with integrated
 * permission forwarding, error states, and session persistence.
 */
class SynSoundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface NavigationCallback {
        fun onPageStarted(url: String?) {}
        fun onPageFinished(url: String?) {}
        fun onErrorReceived(description: String?) {}
        fun onExternalUrlRequested(url: String) {}
    }

    interface PermissionCallback {
        fun onRequestAudioPermission(request: PermissionRequest)
    }

    interface FileChooserCallback {
        fun onShowFileChooser(
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: WebChromeClient.FileChooserParams?
        ): Boolean
    }

    val webView: WebView
    private val progressBar: ProgressBar
    private val errorContainer: LinearLayout
    private val btnRetry: MaterialButton
    private val swipeRefreshLayout: SwipeRefreshLayout

    var navigationCallback: NavigationCallback? = null
    var permissionCallback: PermissionCallback? = null
    var fileChooserCallback: FileChooserCallback? = null

    var defaultUrl: String = DEFAULT_SYNSOUND_URL

    init {
        swipeRefreshLayout = SwipeRefreshLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setColorSchemeResources(R.color.syn_sdk_primary)
            setProgressBackgroundColorSchemeResource(R.color.syn_sdk_surface)
        }

        webView = WebView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        swipeRefreshLayout.addView(webView)
        addView(swipeRefreshLayout)

        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 8.dpToPx()).apply {
                topMargin = 0
            }
            max = 100
            visibility = View.GONE
        }
        addView(progressBar)

        errorContainer = LinearLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(context.getColor(R.color.syn_sdk_background))
            visibility = View.GONE
            setPadding(32.dpToPx(), 32.dpToPx(), 32.dpToPx(), 32.dpToPx())
        }

        val errorTitle = android.widget.TextView(context).apply {
            text = context.getString(R.string.syn_sdk_connection_error)
            setTextColor(context.getColor(R.color.syn_sdk_text_primary))
            textSize = 18f
            gravity = android.view.Gravity.CENTER
        }
        btnRetry = MaterialButton(context).apply {
            text = context.getString(R.string.syn_sdk_retry)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16.dpToPx()
            }
        }
        errorContainer.addView(errorTitle)
        errorContainer.addView(btnRetry)
        addView(errorContainer)

        setupListeners()
        configureWebView()
    }

    private fun setupListeners() {
        swipeRefreshLayout.setOnRefreshListener {
            errorContainer.visibility = View.GONE
            webView.visibility = View.VISIBLE
            webView.reload()
        }

        btnRetry.setOnClickListener {
            errorContainer.visibility = View.GONE
            webView.visibility = View.VISIBLE
            if (webView.url != null && webView.url != "about:blank") {
                webView.reload()
            } else {
                load(defaultUrl)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = false
            setAllowFileAccessFromFileURLs(false)
            setAllowUniversalAccessFromFileURLs(false)
            setSupportMultipleWindows(false)
            defaultTextEncodingName = "utf-8"
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, false)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress in 1..99) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                } else {
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                if (!isTrustedAudioRequest(request)) {
                    request.deny()
                    return
                }

                val callback = permissionCallback
                if (callback == null) {
                    request.deny()
                    return
                }

                callback.onRequestAudioPermission(request)
            }

            override fun onShowFileChooser(
                view: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return fileChooserCallback?.onShowFileChooser(filePathCallback, fileChooserParams)
                    ?: run {
                        filePathCallback?.onReceiveValue(null)
                        true
                    }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val host = uri.host?.lowercase() ?: ""
                val scheme = uri.scheme?.lowercase() ?: ""

                if (scheme != "http" && scheme != "https") {
                    navigationCallback?.onExternalUrlRequested(uri.toString())
                    return true
                }

                if (isTrustedHttpsUri(uri)) {
                    return false
                }

                navigationCallback?.onExternalUrlRequested(uri.toString())
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                errorContainer.visibility = View.GONE
                navigationCallback?.onPageStarted(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                CookieManager.getInstance().flush()
                navigationCallback?.onPageFinished(url)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    showErrorState()
                    navigationCallback?.onErrorReceived(error?.description?.toString())
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.cancel()
                showErrorState()
                navigationCallback?.onErrorReceived("SSL Error")
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                showErrorState()
                return true
            }
        }
    }

    fun load(url: String = defaultUrl) {
        webView.loadUrl(url)
    }

    fun canGoBack(): Boolean = webView.canGoBack()

    fun goBack() {
        if (webView.canGoBack()) webView.goBack()
    }

    fun reload() {
        webView.reload()
    }

    fun showErrorState() {
        progressBar.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
        webView.visibility = View.GONE
        errorContainer.visibility = View.VISIBLE
    }

    private fun isTrustedAudioRequest(request: PermissionRequest): Boolean {
        val uri = request.origin
        return uri != null &&
            uri.scheme.equals("https", ignoreCase = true) &&
            (uri.port == -1 || uri.port == 443) &&
            uri.host?.equals(TRUSTED_HOST, ignoreCase = true) == true &&
            request.resources.toSet() == setOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
    }

    private fun isTrustedHttpsUri(uri: Uri): Boolean {
        return uri.scheme.equals("https", ignoreCase = true) &&
            (uri.port == -1 || uri.port == 443) &&
            uri.host?.equals(TRUSTED_HOST, ignoreCase = true) == true
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()

    companion object {
        private const val TRUSTED_HOST = "synsound-beta.base44.app"
        const val DEFAULT_SYNSOUND_URL = "https://synsound-beta.base44.app"
    }
}
