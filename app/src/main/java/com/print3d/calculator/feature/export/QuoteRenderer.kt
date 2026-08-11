package com.print3d.calculator.feature.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import com.print3d.calculator.R
import com.print3d.calculator.core.util.CurrencyFormatter
import com.print3d.calculator.core.util.DateFormatter
import com.print3d.calculator.data.settings.AppSettings
import com.print3d.calculator.domain.calc.CostKeys
import com.print3d.calculator.domain.model.AppCurrency
import com.print3d.calculator.domain.model.Quotation

/**
 * Draws a professional, ERP-style quotation onto a [Canvas]. Shared by both the
 * PDF exporter (vector page) and the PNG exporter (bitmap) so the look is identical.
 *
 * Coordinate system is a 720 x 1018 "page" (A4 ratio); callers scale as needed.
 */
object QuoteRenderer {

    const val PAGE_W = 720f
    const val PAGE_H = 1018f

    private val INK = Color.parseColor("#1C1B1F")
    private val MUTED = Color.parseColor("#6B6873")
    private val ACCENT = Color.parseColor("#5B5BD6")
    private val HAIR = Color.parseColor("#E4E2E8")
    private val SOFT = Color.parseColor("#F4F3F7")

    fun draw(canvas: Canvas, context: Context, quote: Quotation, settings: AppSettings) {
        canvas.drawColor(Color.WHITE)
        val currency = AppCurrency.fromCode(quote.currencyCode)
        fun money(v: Double) = CurrencyFormatter.format(v, currency)
        fun str(id: Int) = context.getString(id)

        val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val reg = Typeface.SANS_SERIF
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val margin = 48f
        var y = 64f

        // Logo
        loadLogo(context, settings.logoUri)?.let { logo ->
            val h = 64f
            val w = h * logo.width / logo.height
            canvas.drawBitmap(Bitmap.createScaledBitmap(logo, w.toInt(), h.toInt(), true), margin, y - 8, p)
        }

        // Business name + quotation label
        p.typeface = bold; p.textSize = 26f; p.color = INK
        val bizName = settings.businessName.ifBlank { str(R.string.app_name) }
        canvas.drawText(bizName, if (settings.logoUri.isBlank()) margin else margin + 84f, y + 24f, p)

        p.typeface = bold; p.textSize = 30f; p.color = ACCENT
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText(str(R.string.quotation).uppercase(), PAGE_W - margin, y + 24f, p)
        p.textAlign = Paint.Align.LEFT

        y += 64f
        // Business contact line
        p.typeface = reg; p.textSize = 12f; p.color = MUTED
        val contact = listOf(
            settings.businessPhone, settings.businessEmail, settings.businessWeb
        ).filter { it.isNotBlank() }.joinToString("  ·  ")
        if (contact.isNotBlank()) { canvas.drawText(contact, margin, y, p); y += 18f }
        if (settings.businessAddress.isNotBlank()) { canvas.drawText(settings.businessAddress, margin, y, p); y += 18f }

        y += 12f
        canvas.drawLine(margin, y, PAGE_W - margin, y, hair())
        y += 28f

        // Meta: number / date / client
        p.typeface = reg; p.textSize = 12f; p.color = MUTED
        canvas.drawText(str(R.string.quote_number), margin, y, p)
        canvas.drawText(str(R.string.quote_date), margin + 200f, y, p)
        canvas.drawText(str(R.string.quote_client), margin + 400f, y, p)
        y += 20f
        p.typeface = bold; p.textSize = 15f; p.color = INK
        canvas.drawText(quote.number, margin, y, p)
        canvas.drawText(DateFormatter.format(quote.createdAt, settings.dateFormat), margin + 200f, y, p)
        canvas.drawText(quote.input.clientName.ifBlank { "—" }, margin + 400f, y, p)
        // Client contact (phone / email) beneath the name, when known.
        val clientContact = listOf(quote.input.clientPhone, quote.input.clientEmail)
            .filter { it.isNotBlank() }.joinToString("  ·  ")
        if (clientContact.isNotBlank()) {
            p.typeface = reg; p.textSize = 11f; p.color = MUTED
            canvas.drawText(clientContact, margin + 400f, y + 16f, p)
        }
        y += 16f
        if (quote.input.projectName.isNotBlank()) {
            y += 12f
            p.typeface = reg; p.textSize = 13f; p.color = MUTED
            canvas.drawText(quote.input.projectName, margin, y, p)
            y += 8f
        }

        y += 24f
        // Cost table header
        val rowH = 34f
        p.color = SOFT
        canvas.drawRoundRect(RectF(margin, y, PAGE_W - margin, y + rowH), 8f, 8f, p)
        p.typeface = bold; p.textSize = 12f; p.color = MUTED
        canvas.drawText(str(R.string.pdf_concept).uppercase(), margin + 16f, y + 22f, p)
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText(str(R.string.pdf_value).uppercase(), PAGE_W - margin - 16f, y + 22f, p)
        p.textAlign = Paint.Align.LEFT
        y += rowH + 6f

        val r = quote.result
        val lines = buildList {
            add(str(R.string.result_material) to r.materialCost)
            add(str(R.string.result_electricity) to r.electricityCost)
            add(str(R.string.result_machine) to r.machineCost)
            add(str(R.string.result_labor) to r.laborCost)
            add(str(R.string.result_extras) to r.extrasCost)
            add(str(R.string.result_profit) to r.profit)
            if (r.discountAmount > 0) add(str(R.string.result_discount) to -r.discountAmount)
            if (r.surchargeAmount > 0) add(str(R.string.result_surcharge_line) to r.surchargeAmount)
            if (r.taxAmount > 0) add(str(R.string.result_tax) to r.taxAmount)
        }.filter { it.second != 0.0 }

        lines.forEach { (label, amount) ->
            p.typeface = reg; p.textSize = 14f; p.color = INK
            canvas.drawText(label, margin + 16f, y + 20f, p)
            p.textAlign = Paint.Align.RIGHT
            canvas.drawText(money(amount), PAGE_W - margin - 16f, y + 20f, p)
            p.textAlign = Paint.Align.LEFT
            y += rowH
            canvas.drawLine(margin + 8f, y, PAGE_W - margin - 8f, y, hair())
        }

        // Total band
        y += 16f
        p.color = ACCENT
        canvas.drawRoundRect(RectF(margin, y, PAGE_W - margin, y + 52f), 12f, 12f, p)
        p.typeface = bold; p.textSize = 18f; p.color = Color.WHITE
        canvas.drawText(str(R.string.result_total), margin + 20f, y + 33f, p)
        p.textAlign = Paint.Align.RIGHT
        p.textSize = 22f
        canvas.drawText(money(r.total), PAGE_W - margin - 20f, y + 34f, p)
        p.textAlign = Paint.Align.LEFT
        y += 52f

        if (r.quantity > 1) {
            y += 22f
            p.typeface = reg; p.textSize = 13f; p.color = MUTED
            canvas.drawText("${str(R.string.result_per_unit)}: ${money(r.perUnit)}  ·  x${r.quantity}", margin, y, p)
        }

        // Notes
        if (quote.input.notes.isNotBlank()) {
            y += 34f
            p.typeface = bold; p.textSize = 12f; p.color = MUTED
            canvas.drawText(str(R.string.field_notes).uppercase(), margin, y, p)
            y += 20f
            p.typeface = reg; p.textSize = 13f; p.color = INK
            wrap(quote.input.notes, p, PAGE_W - 2 * margin).forEach {
                canvas.drawText(it, margin, y, p); y += 18f
            }
        }

        // QR (bottom-right) — encodes web or quote number, ready for future deep links.
        val qrContent = settings.businessWeb.ifBlank { quote.number }
        QrGenerator.generate(qrContent, 120)?.let { qr ->
            canvas.drawBitmap(qr, PAGE_W - margin - 96f, PAGE_H - 190f, p)
        }

        // Footer
        canvas.drawLine(margin, PAGE_H - 64f, PAGE_W - margin, PAGE_H - 64f, hair())
        p.typeface = reg; p.textSize = 12f; p.color = MUTED
        canvas.drawText(str(R.string.quote_footer), margin, PAGE_H - 40f, p)
        val ig = settings.businessInstagram
        if (ig.isNotBlank()) {
            p.textAlign = Paint.Align.RIGHT
            canvas.drawText(ig, PAGE_W - margin, PAGE_H - 40f, p)
            p.textAlign = Paint.Align.LEFT
        }
    }

    private fun hair(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = HAIR; strokeWidth = 1f
    }

    private fun loadLogo(context: Context, uri: String): Bitmap? {
        if (uri.isBlank()) return null
        return runCatching {
            context.contentResolver.openInputStream(Uri.parse(uri)).use {
                android.graphics.BitmapFactory.decodeStream(it)
            }
        }.getOrNull()
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (w in words) {
            val candidate = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(candidate) > maxWidth) {
                if (current.isNotEmpty()) lines.add(current)
                current = w
            } else current = candidate
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines.take(6)
    }
}
