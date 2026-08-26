package com.example.feature.map.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.feature.map.bridge.LeafletBridge
import com.example.feature.map.bridge.MapEventListener

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletMapView(
    onBridgeReady: (LeafletBridge) -> Unit,
    eventListener: MapEventListener,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var reloadTrigger by remember { mutableIntStateOf(0) }

    val webView = remember(reloadTrigger) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF0F172A.toInt())
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            // Use LAYER_TYPE_NONE to avoid Mesa rendernode GPU crashes on headless/emulator environments
            setLayerType(View.LAYER_TYPE_NONE, null)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                loadWithOverviewMode = true
                useWideViewPort = true
                cacheMode = WebSettings.LOAD_DEFAULT
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
        }
    }

    val bridge = remember(webView) {
        LeafletBridge(webView).also { b ->
            b.setEventListener(eventListener)
            webView.addJavascriptInterface(b.JavascriptInterfaceBridge(), "AndroidMapBridge")
        }
    }

    DisposableEffect(bridge, eventListener) {
        bridge.setEventListener(eventListener)
        onDispose {
            bridge.setEventListener(null)
        }
    }

    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                else -> {}
            }
        }
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(webView) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                eventListener.onTileLoading()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onBridgeReady(bridge)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                val description = error?.description?.toString() ?: "WebView loading error"
                if (request?.isForMainFrame == true) {
                    eventListener.onTileError(description)
                }
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
            ): Boolean {
                // Prevent app termination and cleanly trigger auto-recovery
                try {
                    view?.let { wv ->
                        (wv.parent as? ViewGroup)?.removeView(wv)
                        wv.destroy()
                    }
                } catch (_: Exception) {}
                reloadTrigger++
                return true
            }
        }

        webView.loadUrl("file:///android_asset/leaflet_map.html")

        onDispose {
            try {
                webView.removeJavascriptInterface("AndroidMapBridge")
                webView.stopLoading()
                webView.destroy()
            } catch (_: Exception) {
            }
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}
