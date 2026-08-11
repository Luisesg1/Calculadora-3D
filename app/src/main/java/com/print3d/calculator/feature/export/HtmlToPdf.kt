package com.print3d.calculator.feature.export

import android.content.Context
import android.print.PdfWriter
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Renders an HTML string to a real A4 PDF file using an off-screen [WebView] and the
 * platform print pipeline. Produces vector text — not a screenshot.
 */
object HtmlToPdf {

    suspend fun render(context: Context, html: String, outFile: File): File =
        withContext(Dispatchers.Main) {
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = false

            // Wait for the page to finish loading before printing.
            suspendCancellableCoroutine { cont ->
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        val adapter = view.createPrintDocumentAdapter("quote")
                        PdfWriter.write(adapter, outFile, object : PdfWriter.Callback {
                            override fun onDone() { if (cont.isActive) cont.resume(outFile) }
                            override fun onError(t: Throwable) { if (cont.isActive) cont.resumeWithException(t) }
                        })
                    }
                }
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }
}
