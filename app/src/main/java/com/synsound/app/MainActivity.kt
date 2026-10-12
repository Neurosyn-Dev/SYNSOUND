package com.synsound.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Base64
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.button.MaterialButton
import com.synsound.sdk.analysis.AcousticEvent
import com.synsound.sdk.analysis.AcousticEventType
import com.synsound.sdk.audio.AudioFrame
import com.synsound.sdk.core.SynSoundSDK
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorContainer: LinearLayout
    private lateinit var btnRetry: MaterialButton
    private lateinit var btnNativeBack: ImageButton
    private lateinit var bottomNavContainer: FrameLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var pendingPermissionRequest: PermissionRequest? = null
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private var pendingDownload: DownloadRequestData? = null
    private var nativeAudioStatusView: TextView? = null
    private val nativeAudioFrameIndex = AtomicLong(0)
    private val nativeAudioExecutor = Executors.newSingleThreadExecutor()
    private var nativeAudioTapAtDocumentStart = false
    private val nativeAudioStatusHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val hideNativeAudioStatus = Runnable { nativeAudioStatusView?.visibility = View.GONE }
    private var nativeAudioBridgeInstalled = false

    private val downloadStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val download = pendingDownload
        pendingDownload = null
        if (isGranted && download != null) {
            enqueueDownload(download)
        } else if (!isGranted) {
            Toast.makeText(this, R.string.download_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val request = pendingPermissionRequest
        pendingPermissionRequest = null

        if (isGranted && request != null && isTrustedAudioRequest(request)) {
            request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
        } else {
            request?.deny()
            if (!isGranted && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                showPermissionSettingsDialog()
            } else if (!isGranted) {
                Toast.makeText(this, R.string.mic_permission_rationale, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (fileUploadCallback == null) return@registerForActivityResult

        val resultUris: Array<Uri>? = when {
            result.resultCode != RESULT_OK -> null
            result.data?.clipData != null -> {
                val clipData = result.data!!.clipData!!
                Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            }
            result.data?.data != null -> arrayOf(result.data!!.data!!)
            else -> null
        }

        fileUploadCallback?.onReceiveValue(resultUris)
        fileUploadCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupInsets()
        setupWebView()
        setupBackNavigation()

        if (!com.synsound.sdk.core.SynSoundSDK.isInitialized()) {
            com.synsound.sdk.core.SynSoundSDK.initialize(
                this,
                com.synsound.sdk.core.SynSoundConfig.Builder()
                    .environment(com.synsound.sdk.core.SynSoundEnvironment.Beta)
                    .enableRealTimeDsp(true)
                    .enableEventDetection(true)
                    .build()
            )
        }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            loadInitialUrl(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadInitialUrl(intent)
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        errorContainer = findViewById(R.id.errorContainer)
        btnRetry = findViewById(R.id.btnRetry)
        btnNativeBack = findViewById(R.id.btnNativeBack)
        bottomNavContainer = findViewById(R.id.bottomNavContainer)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        installNativeAudioStatusView()

        swipeRefreshLayout.setColorSchemeResources(R.color.syn_primary)
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.syn_surface)
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
                webView.loadUrl(SYN_SOUND_URL)
            }
        }

        btnNativeBack.setOnClickListener {
            handleBackAction()
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootCoordinator)) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            progressBar.updatePadding(top = systemBars.top)

            bottomNavContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginStart = 16.dpToPx() + systemBars.left
                bottomMargin = 16.dpToPx() + systemBars.bottom
            }

            errorContainer.updatePadding(
                top = systemBars.top + 16.dpToPx(),
                bottom = systemBars.bottom + 16.dpToPx(),
                left = systemBars.left + 16.dpToPx(),
                right = systemBars.right + 16.dpToPx()
            )

            insets
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
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
            setSupportMultipleWindows(false)
            defaultTextEncodingName = "utf-8"
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, false)

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

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

                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                } else {
                    pendingPermissionRequest?.deny()
                    pendingPermissionRequest = request
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest) {
                if (pendingPermissionRequest == request) {
                    pendingPermissionRequest = null
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback

                val intent = runCatching {
                    fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                }.getOrElse {
                    Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                }.apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                }

                try {
                    fileChooserLauncher.launch(intent)
                } catch (_: Exception) {
                    fileUploadCallback?.onReceiveValue(null)
                    fileUploadCallback = null
                    return false
                }
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase() ?: ""

                if (scheme != "http" && scheme != "https") {
                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                        true
                    } catch (_: Exception) {
                        true
                    }
                }

                if (isTrustedHttpsUri(uri)) {
                    return false
                }

                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    true
                } catch (_: Exception) {
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                errorContainer.visibility = View.GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                CookieManager.getInstance().flush()
                if (nativeAudioBridgeInstalled && !nativeAudioTapAtDocumentStart && url != null && runCatching { isTrustedHttpsUri(Uri.parse(url)) }.getOrDefault(false)) {
                    view?.evaluateJavascript(NATIVE_AUDIO_TAP_SCRIPT, null)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    showErrorState()
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.cancel()
                showErrorState()
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
            ): Boolean {
                showErrorState()
                return true
            }
        }

        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            val uri = runCatching { Uri.parse(url) }.getOrNull()
            if (uri == null || !isTrustedHttpsUri(uri)) {
                return@DownloadListener
            }

            val download = DownloadRequestData(
                uri = uri,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimetype
            )

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingDownload = download
                downloadStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                enqueueDownload(download)
            }
        })
        setupNativeAudioBridge()
    }

    private fun setupNativeAudioBridge() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return

        WebViewCompat.addWebMessageListener(
            webView,
            "synSoundNative",
            setOf("https://$TRUSTED_HOST")
        ) { _, message, sourceOrigin, isMainFrame, _ ->
            if (!isMainFrame ||
                !sourceOrigin.scheme.equals("https", ignoreCase = true) ||
                !sourceOrigin.host.equals(TRUSTED_HOST, ignoreCase = true) ||
                (sourceOrigin.port != -1 && sourceOrigin.port != 443)
            ) {
                return@addWebMessageListener
            }

            // WebMessageListener callbacks may run on the UI thread. Keep JSON/base64 decoding
            // and acoustic analysis on a dedicated serial worker to avoid janking the WebView.
            val rawMessage = message.data ?: return@addWebMessageListener
            if (rawMessage.length > MAX_MESSAGE_CHARS) return@addWebMessageListener
            nativeAudioExecutor.execute {
                val payload = runCatching { JSONObject(rawMessage) }.getOrNull()
                    ?: return@execute
                val messageType = payload.optString("type")
                if (messageType == "state" && !payload.optBoolean("active", true)) {
                    runOnUiThread {
                        nativeAudioStatusHandler.removeCallbacks(hideNativeAudioStatus)
                        nativeAudioStatusView?.visibility = View.GONE
                    }
                    return@execute
                }
                if (messageType != "pcm16") return@execute

                val encoded = payload.optString("data")
                if (encoded.isBlank() || encoded.length > MAX_ENCODED_PCM_CHARS) return@execute
                if (payload.optInt("sampleRate", 0) != NATIVE_SAMPLE_RATE ||
                    payload.optInt("channels", 0) != 1
                ) return@execute

                val pcmData = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
                    ?: return@execute
                if (pcmData.isEmpty() || pcmData.size % 2 != 0 || pcmData.size > MAX_PCM_BYTES) {
                    return@execute
                }

                val frame = AudioFrame(
                    pcmData = pcmData,
                    sampleRate = NATIVE_SAMPLE_RATE,
                    channelCount = 1,
                    timestampMs = System.currentTimeMillis(),
                    frameIndex = nativeAudioFrameIndex.incrementAndGet()
                )
                val events = runCatching {
                    SynSoundSDK.getInstance().processExternalAudioFrame(frame)
                }.getOrElse { return@execute }

                runOnUiThread { updateNativeAudioStatus(frame, events) }
            }
        }
        nativeAudioBridgeInstalled = true
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            runCatching {
                WebViewCompat.addDocumentStartJavaScript(
                    webView,
                    NATIVE_AUDIO_TAP_SCRIPT,
                    setOf("https://$TRUSTED_HOST")
                )
                nativeAudioTapAtDocumentStart = true
            }
        }
    }

    private fun installNativeAudioStatusView() {
        val status = TextView(this).apply {
            text = "NATIVE ANALYSIS · LIVE"
            textSize = 11f
            setTextColor(Color.WHITE)
            setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18.dpToPx().toFloat()
                setColor(Color.parseColor("#E612141A"))
                setStroke(1.dpToPx(), ContextCompat.getColor(this@MainActivity, R.color.syn_primary))
            }
            elevation = 8.dpToPx().toFloat()
            maxLines = 1
            visibility = View.GONE
            contentDescription = "Native acoustic analysis status"
        }
        val layoutParams = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 76.dpToPx()
            marginStart = 12.dpToPx()
            marginEnd = 12.dpToPx()
        }
        findViewById<CoordinatorLayout>(R.id.rootCoordinator).addView(status, layoutParams)
        nativeAudioStatusView = status
    }

    private fun updateNativeAudioStatus(frame: AudioFrame, events: List<AcousticEvent>) {
        val candidate = events.lastOrNull { it.type != AcousticEventType.SILENCE_OR_DROPOUT }
        val rms = String.format(Locale.US, "%.0f dBFS", frame.rmsDbfs)
        nativeAudioStatusView?.apply {
            text = if (candidate != null) {
                "NATIVE CANDIDATE · ${candidate.type.displayName}"
            } else {
                "NATIVE ANALYSIS · LIVE · $rms"
            }
            visibility = View.VISIBLE
        }
        nativeAudioStatusHandler.removeCallbacks(hideNativeAudioStatus)
        nativeAudioStatusHandler.postDelayed(hideNativeAudioStatus, NATIVE_STATUS_TIMEOUT_MS)
    }

    private fun enqueueDownload(download: DownloadRequestData) {
        try {
            val fileName = URLUtil.guessFileName(
                download.uri.toString(),
                download.contentDisposition,
                download.mimeType
            )
            val request = DownloadManager.Request(download.uri).apply {
                download.mimeType?.takeIf { it.isNotBlank() }?.let { setMimeType(it) }
                download.userAgent?.takeIf { it.isNotBlank() }?.let {
                    addRequestHeader("User-Agent", it)
                }
                CookieManager.getInstance().getCookie(download.uri.toString())
                    ?.takeIf { it.isNotBlank() }
                    ?.let { addRequestHeader("Cookie", it) }
                setTitle(fileName)
                setDescription(getString(R.string.download_starting))
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )
            }

            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this, R.string.download_starting, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private data class DownloadRequestData(
        val uri: Uri,
        val userAgent: String?,
        val contentDisposition: String?,
        val mimeType: String?
    )

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

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackAction()
            }
        })
    }

    private fun handleBackAction() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            finish()
        }
    }

    private fun showErrorState() {
        progressBar.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
        webView.visibility = View.GONE
        errorContainer.visibility = View.VISIBLE
    }

    private fun loadInitialUrl(intent: Intent?) {
        val incomingUri = intent?.data
        val targetUrl = if (incomingUri != null && isTrustedHttpsUri(incomingUri)) {
            incomingUri.toString()
        } else {
            SYN_SOUND_URL
        }
        webView.loadUrl(targetUrl)
    }

    private fun showPermissionSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(R.string.mic_permission_denied_settings)
            .setPositiveButton(R.string.btn_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        pendingPermissionRequest?.deny()
        pendingPermissionRequest = null
        fileUploadCallback?.onReceiveValue(null)
        fileUploadCallback = null
        pendingDownload = null
        nativeAudioStatusHandler.removeCallbacks(hideNativeAudioStatus)
        nativeAudioExecutor.shutdownNow()
        webView.destroy()
        super.onDestroy()
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()

    companion object {
        private const val TRUSTED_HOST = "synsound-beta.base44.app"
        const val SYN_SOUND_URL = "https://synsound-beta.base44.app"
        private const val NATIVE_SAMPLE_RATE = 16_000
        private const val MAX_PCM_BYTES = 16_000
        private const val MAX_ENCODED_PCM_CHARS = 24_000
        private const val MAX_MESSAGE_CHARS = 25_000
        private const val NATIVE_STATUS_TIMEOUT_MS = 1_500L

        // A silent side branch taps the existing Web Audio microphone source. It never opens
        // Android AudioRecord and does not change the app's audible DSP/output chain.
        private const val NATIVE_AUDIO_TAP_SCRIPT = """
            (function () {
              if (window.__synSoundNativeTapInstalled || !window.synSoundNative ||
                  !window.AudioContext || !window.AudioContext.prototype.createMediaStreamSource) return;
              window.__synSoundNativeTapInstalled = true;
              var taps = window.__synSoundNativeTaps || (window.__synSoundNativeTaps = []);
              function stopTap(tap) {
                if (!tap || tap.stopped) return;
                tap.stopped = true;
                if (tap.processor) tap.processor.onaudioprocess = null;
                try { tap.source.disconnect(tap.processor); } catch (_) {}
                try { tap.processor.disconnect(); } catch (_) {}
                try { tap.mute.disconnect(); } catch (_) {}
                try {
                  window.synSoundNative.postMessage(JSON.stringify({ type: "state", active: false }));
                } catch (_) {}
              }
              window.addEventListener("pagehide", function () {
                taps.slice().forEach(stopTap);
              }, { once: true });
              var originalCreate = window.AudioContext.prototype.createMediaStreamSource;
              window.AudioContext.prototype.createMediaStreamSource = function (stream) {
                var source = originalCreate.call(this, stream);
                try {
                  if (!stream || !stream.getAudioTracks || !stream.getAudioTracks().length ||
                      source.__synSoundNativeTap) return source;
                  var context = this;
                  var processor = context.createScriptProcessor(4096, 1, 1);
                  var mute = context.createGain();
                  mute.gain.value = 0;
                  var tap = {
                    source: source,
                    processor: processor,
                    mute: mute,
                    stream: stream,
                    stopped: false
                  };
                  processor.onaudioprocess = function (event) {
                    if (tap.stopped) return;
                    var tracks = stream.getAudioTracks();
                    if (!tracks.some(function (track) { return track.readyState === "live"; })) {
                      stopTap(tap);
                      return;
                    }
                    var input = event.inputBuffer.getChannelData(0);
                    var inputRate = event.inputBuffer.sampleRate;
                    var outputLength = Math.floor(input.length * 16000 / inputRate);
                    if (outputLength < 256) return;
                    var bytes = new Uint8Array(outputLength * 2);
                    // Area-average each source interval before downsampling. This acts as a
                    // simple low-pass filter and reduces aliasing versus nearest-neighbor picks.
                    var ratio = inputRate / 16000;
                    for (var i = 0; i < outputLength; i++) {
                      var start = i * ratio;
                      var end = Math.min(input.length, (i + 1) * ratio);
                      var first = Math.floor(start);
                      var last = Math.min(input.length - 1, Math.ceil(end) - 1);
                      var sum = 0;
                      var weight = 0;
                      for (var j = first; j <= last; j++) {
                        var overlap = Math.max(0, Math.min(end, j + 1) - Math.max(start, j));
                        sum += (input[j] || 0) * overlap;
                        weight += overlap;
                      }
                      var sample = weight > 0 ? sum / weight : 0;
                      sample = Math.max(-1, Math.min(1, sample));
                      var value = (sample < 0 ? sample * 32768 : sample * 32767) | 0;
                      bytes[i * 2] = value & 255;
                      bytes[i * 2 + 1] = (value >> 8) & 255;
                    }
                    var binary = "";
                    for (var offset = 0; offset < bytes.length; offset += 0x8000) {
                      binary += String.fromCharCode.apply(null, bytes.subarray(offset, Math.min(offset + 0x8000, bytes.length)));
                    }
                    try {
                      window.synSoundNative.postMessage(JSON.stringify({
                        type: "pcm16", sampleRate: 16000, channels: 1, data: btoa(binary)
                      }));
                    } catch (_) {}
                  };
                  source.connect(processor);
                  processor.connect(mute);
                  mute.connect(context.destination);
                  tap.stop = function () { stopTap(tap); };
                  source.__synSoundNativeTap = tap;
                  taps.push(tap);
                  stream.getAudioTracks().forEach(function (track) {
                    track.addEventListener("ended", function () { stopTap(tap); }, { once: true });
                  });
                } catch (_) {}
                return source;
              };
            })();
        """
    }
}
