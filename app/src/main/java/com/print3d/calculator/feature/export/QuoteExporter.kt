package com.print3d.calculator.feature.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.print3d.calculator.data.settings.AppSettings
import com.print3d.calculator.domain.model.Quotation
import java.io.File
import java.io.FileOutputStream

/** Produces shareable PDF and PNG files from a [Quotation]. */
object QuoteExporter {

    private fun exportsDir(context: Context): File =
        File(context.cacheDir, "shared").apply { mkdirs() }

    /**
     * Renders the quotation to a real A4 PDF from print-ready HTML (crisp vector text),
     * not a screenshot. [materialName] feeds the summary block (not stored on the quote).
     */
    suspend fun exportPdf(
        context: Context,
        quote: Quotation,
        settings: AppSettings,
        materialName: String? = null,
        pro: Boolean = true
    ): File {
        val html = QuoteHtml.build(context, quote, settings, materialName, pro)
        val file = File(exportsDir(context), "${quote.number}.pdf")
        return HtmlToPdf.render(context, html, file)
    }

    fun exportPng(context: Context, quote: Quotation, settings: AppSettings): File {
        val scale = 2f // crisp for social sharing
        val bmp = Bitmap.createBitmap(
            (QuoteRenderer.PAGE_W * scale).toInt(),
            (QuoteRenderer.PAGE_H * scale).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        canvas.scale(scale, scale)
        QuoteRenderer.draw(canvas, context, quote, settings)

        val file = File(exportsDir(context), "${quote.number}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    fun share(context: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
