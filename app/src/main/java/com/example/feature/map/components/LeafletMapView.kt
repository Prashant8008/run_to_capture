package com.example.feature.map.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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
    val webView = remember {
        WebView(context).apply {
            setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFFE2E8F0.toInt())
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                loadWithOverviewMode = true
                useWideViewPort = true
                cacheMode = WebSettings.LOAD_DEFAULT
                databaseEnabled = true
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
