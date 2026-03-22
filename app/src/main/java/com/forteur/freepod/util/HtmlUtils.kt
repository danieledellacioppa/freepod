package com.forteur.freepod.util

import android.text.Html

fun stripHtml(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString().trim()
}
