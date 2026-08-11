package com.print3d.calculator.feature.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.print3d.calculator.R
import com.print3d.calculator.core.util.CurrencyFormatter
import com.print3d.calculator.core.util.DateFormatter
import com.print3d.calculator.data.settings.AppSettings
import com.print3d.calculator.domain.model.AppCurrency
import com.print3d.calculator.domain.model.Quotation
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * Builds the print-ready HTML for a quotation. Rendered to PDF by [HtmlToPdf] so the
 * document has crisp vector text and a consistent A4 layout — not a UI screenshot.
 *
 * Minimalist commercial style (Stripe/Linear/Notion): white, soft grey, app blue,
 * generous whitespace, thin dividers, no heavy shadows.
 */
object QuoteHtml {

    fun build(context: Context, quote: Quotation, settings: AppSettings, materialName: String?, pro: Boolean = true): String {
        val currency = AppCurrency.fromCode(quote.currencyCode)
        fun money(v: Double) = esc(CurrencyFormatter.format(v, currency))
        fun s(id: Int) = esc(context.getString(id))

        // Free tier carries a subtle attribution line; Pro removes it.
        val watermarkHtml = if (pro) "" else
            """<div style="text-align:center;font-size:10px;color:#9a9a9a;margin-top:6px;">""" +
            esc(context.getString(R.string.pdf_watermark) + " " + context.getString(R.string.app_name)) +
            "</div>"

        val r = quote.result
        val input = quote.input

        val bizName = settings.businessName.ifBlank { context.getString(R.string.app_name) }
        val logoUri = loadImageDataUri(context, settings.logoUri)
        val qr = QrGenerator.generate(settings.businessWeb.ifBlank { quote.number }, 200)
        val qrData = qr?.let { bitmapDataUri(it) }
        val date = DateFormatter.format(quote.createdAt, settings.dateFormat, currency.localeTag)

        // Client rows — only those with data (model has name/phone/email; RUT/city/region not stored).
        val clientRows = buildList {
            if (input.clientName.isNotBlank()) add(context.getString(R.string.quote_client) to input.clientName)
            if (input.clientEmail.isNotBlank()) add(context.getString(R.string.business_email) to input.clientEmail)
            if (input.clientPhone.isNotBlank()) add(context.getString(R.string.business_phone) to input.clientPhone)
        }
        val clientRowsHtml = if (clientRows.isEmpty()) {
            """<div class="kv"><span class="k">${s(R.string.quote_client)}</span><span class="v">—</span></div>"""
        } else clientRows.joinToString("\n") { (k, v) ->
            """<div class="kv"><span class="k">${esc(k)}</span><span class="v">${esc(v)}</span></div>"""
        }

        // Cost table — deliberately simple: Material, Print time, Labor, Subtotal (+ Total band).
        val printTimeCost = r.machineCost + r.electricityCost
        val costRows = listOf(
            context.getString(R.string.result_material) to r.materialCost,
            context.getString(R.string.pdf_print_time) to printTimeCost,
            context.getString(R.string.result_labor) to r.laborCost,
            context.getString(R.string.result_subtotal) to r.subtotal
        ).joinToString("\n") { (k, v) ->
            """<tr><td>${esc(k)}</td><td class="num">${money(v)}</td></tr>"""
        }

        // Summary — informational only, NO subtotal/total (those live in the cost table).
        val summaryItems = buildList {
            add(context.getString(R.string.result_material) to (materialName?.takeIf { it.isNotBlank() } ?: "—"))
            add(context.getString(R.string.pdf_time) to formatHours(input.printTimeH))
            add(context.getString(R.string.field_quantity) to "${r.quantity} ${context.getString(R.string.pdf_units)}")
        }.joinToString("\n") { (k, v) ->
            """<div class="sum-item"><div class="sum-k">${esc(k)}</div><div class="sum-v">${esc(v)}</div></div>"""
        }

        val logoHtml = if (logoUri != null) """<img class="logo" src="$logoUri"/>""" else ""

        // Footer contacts laid out horizontally: Instagram · WhatsApp · Correo.
        val footerCells = buildList {
            if (settings.businessInstagram.isNotBlank())
                add("Instagram" to settings.businessInstagram)
            if (settings.businessPhone.isNotBlank())
                add("WhatsApp" to settings.businessPhone)
            if (settings.businessEmail.isNotBlank())
                add(context.getString(R.string.business_email) to settings.businessEmail)
        }.joinToString("\n") { (k, v) ->
            """<div class="fcell"><div class="fk">${esc(k)}</div><div class="fv">${esc(v)}</div></div>"""
        }

        val qrHtml = if (qrData != null) """<div class="fcell qrcell"><img class="qr" src="$qrData"/></div>""" else ""

        return """<!DOCTYPE html>
<html><head><meta charset="utf-8"/>
<style>
  @page { size: A4; margin: 0; }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  :root {
    --ink:#111114; --muted:#8A8790; --label:#6B6873; --accent:#5B5BD6;
    --hair:#ECEBEF; --hair2:#F2F1F5;
  }
  html, body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  body {
    font-family: -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    color: var(--ink); font-size: 11px; line-height: 1.5;
    width: 210mm; height: 297mm; padding: 22mm 20mm 20mm;
    display: flex; flex-direction: column; justify-content: center;
    -webkit-font-smoothing: antialiased;
  }
  .label { font-size: 8.5px; font-weight: 600; letter-spacing: 0.8px;
    text-transform: uppercase; color: var(--label); }

  /* Header */
  .head { display: flex; justify-content: space-between; align-items: center; }
  .brand { display: flex; align-items: center; gap: 14px; }
  .logo { height: 44px; width: auto; object-fit: contain; }
  .biz { font-size: 17px; font-weight: 700; letter-spacing: -0.3px; }
  .doc-title { font-size: 23px; font-weight: 700; color: var(--accent);
    letter-spacing: 2.5px; text-transform: uppercase; }

  /* Meta row: Número · Fecha · Cliente · Producto */
  .meta { display: flex; margin-top: 40px; padding-top: 24px; border-top: 1px solid var(--hair); }
  .meta .cell { flex: 1; padding-right: 16px; }
  .meta .cell:last-child { padding-right: 0; }
  .meta .val { font-size: 13.5px; font-weight: 700; margin-top: 7px; letter-spacing: -0.2px; }

  /* Body */
  .body { display: flex; gap: 44px; margin-top: 60px; }
  .col-left { flex: 0 0 56%; }
  .col-right { flex: 1; }
  .section { margin-bottom: 52px; }
  .section:last-child { margin-bottom: 0; }
  .section > .label { margin-bottom: 16px; }

  /* Client data + Summary — light, no heavy card */
  .kv { display: flex; justify-content: space-between; align-items: baseline;
    padding: 8px 0; border-bottom: 1px solid var(--hair2); }
  .kv:last-child { border-bottom: 0; }
  .kv .k { color: var(--muted); font-weight: 500; }
  .kv .v { font-weight: 700; text-align: right; letter-spacing: -0.2px; }

  /* Cost table — compact, professional */
  table { width: 100%; border-collapse: collapse; }
  td { padding: 6.5px 0; border-bottom: 1px solid var(--hair2);
    font-size: 11.5px; }
  tbody td:first-child { color: var(--muted); font-weight: 500; }
  td.num { text-align: right; font-variant-numeric: tabular-nums;
    font-weight: 700; color: var(--ink); }
  thead td { font-size: 8.5px; font-weight: 600; letter-spacing: 0.8px;
    text-transform: uppercase; color: var(--label); border-bottom: 1px solid var(--hair);
    padding-bottom: 8px; }
  tbody tr:last-child td { font-weight: 700; color: var(--ink); }
  tbody tr:last-child td:first-child { color: var(--ink); }

  /* Total bar — focal point */
  .total { display: flex; justify-content: space-between; align-items: center;
    background: var(--accent); color: #fff; border-radius: 14px;
    padding: 20px 24px; margin-top: 22px; }
  .total .t-label { font-size: 13px; font-weight: 600; letter-spacing: 1px;
    text-transform: uppercase; opacity: .92; }
  .total .t-val { font-size: 24px; font-weight: 700;
    font-variant-numeric: tabular-nums; letter-spacing: -0.5px; }

  /* Summary rows — light */
  .sum-item { padding: 12px 0; border-bottom: 1px solid var(--hair2); }
  .sum-item:last-child { border-bottom: 0; }
  .sum-item:first-child { padding-top: 0; }
  .sum-k { font-size: 8.5px; font-weight: 600; letter-spacing: 0.8px;
    text-transform: uppercase; color: var(--label); margin-bottom: 5px; }
  .sum-v { font-size: 15px; font-weight: 700; letter-spacing: -0.2px; }
  .thumb { width: 100%; height: auto; border-radius: 12px;
    border: 1px solid var(--hair); margin-bottom: 20px; }

  /* Footer — integrated, horizontal */
  .foot { margin-top: 60px; padding-top: 22px; border-top: 1px solid var(--hair); }
  .foot-row { display: flex; justify-content: space-between; align-items: center; }
  .fcell { display: flex; flex-direction: column; gap: 4px; }
  .fk { font-size: 8px; font-weight: 600; letter-spacing: 0.8px;
    text-transform: uppercase; color: var(--label); }
  .fv { font-size: 11px; font-weight: 600; color: var(--ink); }
  .qrcell { align-items: flex-end; }
  .qr { width: 74px; height: 74px; }
  .thanks { text-align: center; color: var(--muted); font-size: 11px;
    font-weight: 500; margin-top: 20px; }
</style></head>
<body>
  <div class="head">
    <div class="brand">$logoHtml<div class="biz">${esc(bizName)}</div></div>
    <div class="doc-title">${s(R.string.quotation)}</div>
  </div>

  <div class="meta">
    <div class="cell"><div class="label">${s(R.string.quote_number)}</div><div class="val">${esc(quote.number)}</div></div>
    <div class="cell"><div class="label">${s(R.string.quote_date)}</div><div class="val">${esc(date)}</div></div>
    <div class="cell"><div class="label">${s(R.string.quote_client)}</div><div class="val">${esc(input.clientName.ifBlank { "—" })}</div></div>
    <div class="cell"><div class="label">${s(R.string.pdf_product)}</div><div class="val">${esc(input.projectName.ifBlank { "—" })}</div></div>
  </div>

  <div class="body">
    <div class="col-left">
      <div class="section">
        <div class="label">${s(R.string.pdf_client_data)}</div>
        $clientRowsHtml
      </div>
      <div class="section">
        <div class="label">${s(R.string.pdf_cost_detail)}</div>
        <table>
          <thead><tr><td>${s(R.string.pdf_concept)}</td><td class="num">${s(R.string.pdf_value)}</td></tr></thead>
          <tbody>
            $costRows
          </tbody>
        </table>
        <div class="total">
          <span class="t-label">${s(R.string.result_total)}</span>
          <span class="t-val">${money(r.total)}</span>
        </div>
      </div>
    </div>

    <div class="col-right">
      <div class="section">
        <div class="label">${s(R.string.pdf_summary)}</div>
        $summaryItems
      </div>
    </div>
  </div>

  <div class="foot">
    <div class="foot-row">
      $footerCells
      $qrHtml
    </div>
    <div class="thanks">${s(R.string.quote_footer)}</div>
    $watermarkHtml
  </div>
</body></html>"""
    }

    private fun formatHours(h: Double): String {
        if (h <= 0.0) return "—"
        val totalMin = (h * 60).roundToInt()
        val hh = totalMin / 60
        val mm = totalMin % 60
        return when {
            hh > 0 && mm > 0 -> "${hh}h ${mm}m"
            hh > 0 -> "${hh}h"
            else -> "${mm}m"
        }
    }

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun loadImageDataUri(context: Context, uri: String): String? {
        if (uri.isBlank()) return null
        return runCatching {
            val bmp = context.contentResolver.openInputStream(Uri.parse(uri)).use {
                BitmapFactory.decodeStream(it)
            } ?: return null
            bitmapDataUri(bmp)
        }.getOrNull()
    }

    private fun bitmapDataUri(bmp: Bitmap): String {
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        return "data:image/png;base64,$b64"
    }
}
