package org.erthi.note

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.StrikethroughSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.graphics.Typeface
import java.io.ByteArrayOutputStream
import java.util.ArrayDeque
import java.util.Base64
import java.util.Locale
import java.text.BreakIterator

/**
 * Offline, portable rich text. Colours, heading sizes, and inline images are
 * serialized as Android-supported HTML. Images are embedded as compact JPEG
 * data URIs, not temporary document-provider links.
 */
object RichNotes {
    private const val IMAGE_PREFIX = "data:image/jpeg;base64,"
    private const val MAX_IMAGE_BYTES = 1_500_000

    // Html.fromHtml ignores <font face="family">, so we embed font spans as a comment
    // appended to the HTML: <!-- erthi-fonts:[{"s":0,"e":5,"f":"cairo"}] -->
    private val FONT_COMMENT_RE = Regex("""<!--\s*erthi-fonts:(\[.*?])\s*-->""", RegexOption.DOT_MATCHES_ALL)

    fun deserialize(context: Context, html: String, plain: String,
        maxImageWidth: Int = (context.resources.displayMetrics.density * 245).toInt()
    ): SpannableStringBuilder {
        if (html.isBlank()) return SpannableStringBuilder(plain)
        val baseHtml = html.replace(FONT_COMMENT_RE, "")
        val parsed = Html.fromHtml(baseHtml, Html.FROM_HTML_MODE_COMPACT,
            Html.ImageGetter { src -> decodeDrawable(context, src, maxImageWidth) }, null)
        val ssb = SpannableStringBuilder(parsed)
        // Re-apply font spans from supplementary comment
        FONT_COMMENT_RE.find(html)?.groupValues?.getOrNull(1)?.let { json ->
            parseFontJson(json).forEach { (s, e, family) ->
                if (s < e && e <= ssb.length) {
                    ssb.setSpan(TypefaceSpan(family), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
        return ssb
    }

    fun serialize(body: CharSequence): String {
        val ssb = SpannableStringBuilder(body)
        val html = Html.toHtml(ssb, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
        // Collect both standard TypefaceSpan and CustomTypefaceSpan family names for persistence
        val typefaceFonts = ssb.getSpans(0, ssb.length, TypefaceSpan::class.java).mapNotNull { span ->
            val f = span.family ?: return@mapNotNull null
            if (f.isBlank()) return@mapNotNull null
            Triple(ssb.getSpanStart(span), ssb.getSpanEnd(span), f)
        }
        val customFonts = ssb.getSpans(0, ssb.length, CustomTypefaceSpan::class.java).mapNotNull { span ->
            val f = span.familyName
            if (f.isBlank()) return@mapNotNull null
            Triple(ssb.getSpanStart(span), ssb.getSpanEnd(span), f)
        }
        val fonts = typefaceFonts + customFonts
        if (fonts.isEmpty()) return html
        val comment = fonts.joinToString(",", prefix = "<!-- erthi-fonts:[", postfix = "] -->") { (s, e, f) ->
            "{\"s\":$s,\"e\":$e,\"f\":\"${f.replace("\"", "\\\"")}\"}"
        }
        return "$html$comment"
    }

    private fun parseFontJson(json: String): List<Triple<Int, Int, String>> {
        // Minimal hand-rolled JSON array parser — no library dependency.
        val result = mutableListOf<Triple<Int, Int, String>>()
        val objRe = Regex("""\{"s":(\d+),"e":(\d+),"f":"([^"\\]*)"\}""")
        objRe.findAll(json).forEach { m ->
            val s = m.groupValues[1].toIntOrNull() ?: return@forEach
            val e = m.groupValues[2].toIntOrNull() ?: return@forEach
            val f = m.groupValues[3]
            if (f.isNotBlank()) result += Triple(s, e, f)
        }
        return result
    }

    /** A body can contain embedded image replacement chars. */
    fun plainText(body: CharSequence): String =
        body.toString().replace('\uFFFC', '▧')

    fun imageFromUri(context: Context, uri: Uri): Pair<String, Drawable> {
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            // Do not trust provider sizes or allocate unbounded buffers.
            val data = ByteArrayOutputStream()
            val block = ByteArray(8192)
            while (true) {
                val count = stream.read(block)
                if (count < 0) break
                require(data.size() + count <= 10_000_000) { "Image is too large" }
                data.write(block, 0, count)
            }
            data.toByteArray()
        } ?: error("Cannot read image")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid picture" }
        var sample = 1
        while (bounds.outWidth / sample > 720 || bounds.outHeight / sample > 720) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: error("Invalid image")
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, output)
        val encoded = output.toByteArray()
        require(encoded.size < MAX_IMAGE_BYTES) { "Picture exceeds note limit" }
        return (IMAGE_PREFIX + Base64.getEncoder().encodeToString(encoded)) to
            BitmapDrawable(context.resources, bitmap).apply {
                val scale = (context.resources.displayMetrics.density * 245 /
                    bitmap.width).coerceAtMost(1f)
                setBounds(0, 0, (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1))
            }
    }

    fun insertImage(editable: Editable, offset: Int, source: String, drawable: Drawable) {
        val where = offset.coerceIn(0, editable.length)
        editable.insert(where, "\uFFFC")
        editable.setSpan(ImageSpan(drawable, source, ImageSpan.ALIGN_BOTTOM),
            where, where + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun decodeDrawable(context: Context, src: String?, maxWidth: Int): Drawable? {
        if (src == null || !src.startsWith(IMAGE_PREFIX) || src.length > 2_200_000) return null
        return try {
            val bytes = Base64.getDecoder().decode(src.removePrefix(IMAGE_PREFIX))
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            BitmapDrawable(context.resources, bitmap).apply {
                val scale = (maxWidth.toFloat() / bitmap.width).coerceAtMost(1f)
                setBounds(0, 0, (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1))
            }
        } catch (_: Exception) { null }
    }

    /** An empty selection styles the word at the caret; otherwise the exact selection. */
    fun selectionOrWord(text: CharSequence, a: Int, b: Int): Pair<Int, Int> {
        var start = minOf(a, b).coerceIn(0, text.length)
        var end = maxOf(a, b).coerceIn(0, text.length)
        if (end > start) return start to end
        while (start > 0 && isWord(text[start - 1])) start--
        while (end < text.length && isWord(text[end])) end++
        return start to end
    }

    private fun isWord(ch: Char) = ch.isLetterOrDigit() || ch == '_' ||
        ch in '\u064B'..'\u065F'

    fun colour(editable: Editable, start: Int, end: Int, value: Int) {
        if (end > start) editable.setSpan(ForegroundColorSpan(value), start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun heading(editable: Editable, start: Int, end: Int, colour: Int) {
        if (end <= start) return
        editable.setSpan(RelativeSizeSpan(1.55f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        editable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        editable.setSpan(ForegroundColorSpan(colour), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun deletePreviousWord(editable: Editable, position: Int): Int {
        val at = position.coerceIn(0, editable.length)
        var from = at
        while (from > 0 && editable[from - 1].isWhitespace()) from--
        while (from > 0 && !editable[from - 1].isWhitespace()) from--
        if (from == at && from > 0) from--
        editable.delete(from, at)
        return from
    }

    /** Uses grapheme-aware boundaries to avoid splitting an emoji or joined script. */
    fun deletePreviousCharacter(editable: Editable, position: Int): Int {
        val at = position.coerceIn(0, editable.length)
        if (at == 0) return 0
        val iterator = BreakIterator.getCharacterInstance(Locale.getDefault())
        iterator.setText(editable.toString())
        val from = iterator.preceding(at).takeIf { it >= 0 } ?: (at - 1)
        editable.delete(from, at)
        return from
    }

    fun italic(editable: Editable, start: Int, end: Int) {
        if (end > start) editable.setSpan(StyleSpan(Typeface.ITALIC), start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun underline(editable: Editable, start: Int, end: Int) {
        if (end > start) editable.setSpan(UnderlineSpan(), start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun strikethrough(editable: Editable, start: Int, end: Int) {
        if (end > start) editable.setSpan(StrikethroughSpan(), start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun fontSize(editable: Editable, start: Int, end: Int, proportion: Float) {
        if (end > start) editable.setSpan(RelativeSizeSpan(proportion), start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun bold(editable: Editable, start: Int, end: Int) {
        if (end > start) editable.setSpan(StyleSpan(Typeface.BOLD), start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun fontFamily(editable: Editable, start: Int, end: Int, family: String) {
        if (end > start) editable.setSpan(TypefaceSpan(family), start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    /** Apply a downloadable or user-imported font backed by an actual Typeface object. */
    fun customFont(editable: Editable, start: Int, end: Int, family: String, typeface: Typeface) {
        if (end > start) editable.setSpan(CustomTypefaceSpan(typeface, family), start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun absoluteSize(editable: Editable, start: Int, end: Int, sizeSp: Int) {
        if (end > start) editable.setSpan(AbsoluteSizeSpan(sizeSp, true), start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun highlight(editable: Editable, start: Int, end: Int, color: Int) {
        if (end > start) editable.setSpan(BackgroundColorSpan(color), start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun align(editable: Editable, start: Int, end: Int,
              alignment: android.text.Layout.Alignment) {
        if (end > start) editable.setSpan(AlignmentSpan.Standard(alignment), start, end,
            Spanned.SPAN_INCLUSIVE_INCLUSIVE)
    }

    private val SPAN_CLASSES: Array<Class<*>> = arrayOf(
        ForegroundColorSpan::class.java, BackgroundColorSpan::class.java,
        RelativeSizeSpan::class.java, AbsoluteSizeSpan::class.java,
        StyleSpan::class.java, UnderlineSpan::class.java, StrikethroughSpan::class.java,
        TypefaceSpan::class.java, CustomTypefaceSpan::class.java,
        AlignmentSpan.Standard::class.java,
    )

    /** Clears formatting only within [start, end]; spans extending outside are preserved by clipping. */
    fun clearSpans(editable: Editable, start: Int, end: Int) {
        if (end <= start) return
        for (cls in SPAN_CLASSES) {
            @Suppress("UNCHECKED_CAST")
            editable.getSpans(start, end, cls as Class<Any>).forEach { span ->
                val spanStart = editable.getSpanStart(span)
                val spanEnd = editable.getSpanEnd(span)
                val flags = editable.getSpanFlags(span)
                editable.removeSpan(span)
                if (spanStart < start) editable.setSpan(copySpan(span), spanStart, start, flags)
                if (spanEnd > end) editable.setSpan(copySpan(span), end, spanEnd, flags)
            }
        }
    }

    private fun copySpan(span: Any): Any = when (span) {
        is ForegroundColorSpan -> ForegroundColorSpan(span.foregroundColor)
        is BackgroundColorSpan -> BackgroundColorSpan(span.backgroundColor)
        is RelativeSizeSpan -> RelativeSizeSpan(span.sizeChange)
        is AbsoluteSizeSpan -> AbsoluteSizeSpan(span.size, span.dip)
        is StyleSpan -> StyleSpan(span.style)
        is UnderlineSpan -> UnderlineSpan()
        is StrikethroughSpan -> StrikethroughSpan()
        is TypefaceSpan -> TypefaceSpan(span.family ?: "")
        is CustomTypefaceSpan -> CustomTypefaceSpan(span.typeface, span.familyName)
        is AlignmentSpan.Standard -> AlignmentSpan.Standard(span.alignment)
        else -> span
    }
}

/** Local editor history, including spans, title and cursor. No telemetry. */
class RichHistory {
    data class State(val title: String, val body: SpannableStringBuilder)
    private val undo = ArrayDeque<State>()
    private val redo = ArrayDeque<State>()
    private val maxStates = 48

    fun record(title: String, body: CharSequence) {
        val next = State(title, SpannableStringBuilder(body))
        val last = undo.peekLast()
        if (last != null && last.title == next.title && RichNotes.serialize(last.body) ==
            RichNotes.serialize(next.body)) return
        undo.addLast(next)
        if (undo.size > maxStates) undo.removeFirst()
        redo.clear()
    }

    fun undo(title: String, body: CharSequence): State? {
        if (undo.isEmpty()) return null
        redo.addLast(State(title, SpannableStringBuilder(body)))
        return undo.removeLast()
    }

    fun redo(title: String, body: CharSequence): State? {
        if (redo.isEmpty()) return null
        undo.addLast(State(title, SpannableStringBuilder(body)))
        return redo.removeLast()
    }
}
