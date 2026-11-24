package org.erthi.note

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.TypefaceSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.Base64

@RunWith(AndroidJUnit4::class)
class RichNoteInstrumentedTest {
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext


    @Test fun quickSettingsTileIsRegisteredWithSystemOnlyPermission() {
        val name = android.content.ComponentName(ctx, ErthiNoteTileService::class.java)
        val info = ctx.packageManager.getServiceInfo(name, 0)
        assertEquals("android.permission.BIND_QUICK_SETTINGS_TILE", info.permission)
        assertTrue(info.exported)
        assertEquals("Erthi Note", ctx.getString(R.string.quick_tile_label))
    }

    @Test fun formattingRoundTripsAndIndependentColoursStaySeparate() {
        val note = SpannableStringBuilder("أحمر أخضر")
        RichNotes.colour(note, 0, 4, Color.RED)
        RichNotes.colour(note, 5, note.length, Color.GREEN)
        RichNotes.heading(note, 5, note.length, Color.BLUE)
        val html = RichNotes.serialize(note)
        val restored = RichNotes.deserialize(ctx, html, "")
        assertTrue(restored.toString().contains("أحمر"))
        val spans = restored.getSpans(0, restored.length, ForegroundColorSpan::class.java)
        assertTrue("Must retain multiple coloured spans", spans.size >= 2)
    }

    @Test fun wordAndGraphemeDeletionAndUndoCanRestore() {
        val ed = SpannableStringBuilder("كلمة ثانية 😀")
        val history = RichHistory()
        history.record("العنوان", ed)
        val pos = RichNotes.deletePreviousCharacter(ed, ed.length)
        assertEquals("كلمة ثانية ", ed.toString())
        assertEquals(ed.length, pos)
        val prior = history.undo("العنوان", ed)
        assertEquals("كلمة ثانية 😀", prior!!.body.toString())
        val text = SpannableStringBuilder("hello world")
        RichNotes.deletePreviousWord(text, text.length)
        assertEquals("hello ", text.toString())
    }

    @Test fun pngAndPdfExportsProduceReadableSignatures() {
        val styled = SpannableStringBuilder("Rich colour test\nثاني سطر")
        RichNotes.colour(styled, 0, 4, Color.RED)
        val note = Note(1L, styled.toString(), 123L, 0, false, null,
            "Erthi Note export", RichNotes.serialize(styled))
        val png = ByteArrayOutputStream()
        NoteExporter.png(ctx, note, png)
        val p = png.toByteArray()
        assertTrue(p.size > 500)
        assertEquals(0x89, p[0].toInt() and 0xFF)
        assertEquals(0x50, p[1].toInt() and 0xFF)
        val pdf = ByteArrayOutputStream()
        NoteExporter.pdf(ctx, note, pdf)
        assertTrue(String(pdf.toByteArray().copyOfRange(0, 5)).startsWith("%PDF-"))
    }

    // ── Font persistence regression tests ─────────────────────────────────────

    @Test fun fontSerializeProducesErthinFontsComment() {
        val ssb = SpannableStringBuilder("hello world")
        ssb.setSpan(TypefaceSpan("Cairo"), 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val html = RichNotes.serialize(ssb)
        assertTrue("erthi-fonts comment missing", html.contains("<!-- erthi-fonts:"))
        assertTrue("Cairo family missing", html.contains("\"f\":\"Cairo\""))
        assertTrue("start position wrong", html.contains("\"s\":0"))
        assertTrue("end position wrong", html.contains("\"e\":5"))
    }

    @Test fun fontSerializeWithNoSpansProducesNoComment() {
        val ssb = SpannableStringBuilder("plain text")
        val html = RichNotes.serialize(ssb)
        assertFalse("unexpected erthi-fonts comment", html.contains("erthi-fonts"))
    }

    @Test fun fontSerializeEscapesFontNameQuotes() {
        val ssb = SpannableStringBuilder("test")
        ssb.setSpan(TypefaceSpan("sans\"serif"), 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val html = RichNotes.serialize(ssb)
        assertFalse("unescaped quote breaks JSON", html.contains("\"sans\"serif\""))
        assertTrue("escaped quote missing", html.contains("sans\\\"serif"))
    }

    @Test fun fontSerializeMultipleSpansProducesAllEntries() {
        val ssb = SpannableStringBuilder("hello world")
        ssb.setSpan(TypefaceSpan("Cairo"), 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        ssb.setSpan(TypefaceSpan("Amiri"), 6, 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val html = RichNotes.serialize(ssb)
        val commentMatch = Regex("""<!-- erthi-fonts:(\[.*?])\s*-->""", RegexOption.DOT_MATCHES_ALL)
            .find(html)
        val jsonPart = commentMatch?.groupValues?.getOrNull(1) ?: ""
        val entries = Regex("""\{"s":\d+,"e":\d+,"f":"[^"]*"\}""").findAll(jsonPart).toList()
        assertEquals("expected 2 font entries", 2, entries.size)
    }

    @Test fun bulletListTogglesPrefix() {
        val ssb = SpannableStringBuilder("line one\nline two")
        RichNotes.bulletList(ssb, 0, ssb.length)
        assertTrue(ssb.toString().startsWith("• line one"))
        RichNotes.bulletList(ssb, 0, ssb.length)
        assertTrue(ssb.toString().startsWith("line one"))
    }

    @Test fun numberedListTogglesPrefix() {
        val ssb = SpannableStringBuilder("alpha\nbeta")
        RichNotes.numberedList(ssb, 0, ssb.length)
        assertTrue(ssb.toString().startsWith("1. alpha"))
        assertTrue(ssb.toString().contains("2. beta"))
        RichNotes.numberedList(ssb, 0, ssb.length)
        assertTrue(ssb.toString().startsWith("alpha"))
    }

    // ── Existing tests ─────────────────────────────────────────────────────────

    @Test fun embeddedJpegImageIsRestoredWithEditableSpan() {
        val bitmap = Bitmap.createBitmap(20, 15, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(19, 140, 115))
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, output)
        val source = "data:image/jpeg;base64," +
            Base64.getEncoder().encodeToString(output.toByteArray())
        val editable = SpannableStringBuilder("Above\nBelow")
        val image = BitmapDrawable(ctx.resources, bitmap)
        image.setBounds(0, 0, 100, 75)
        RichNotes.insertImage(editable, 6, source, image)
        val encoded = RichNotes.serialize(editable)
        val restored = RichNotes.deserialize(ctx, encoded, editable.toString())
        assertTrue(restored.getSpans(0, restored.length, ImageSpan::class.java).isNotEmpty())
    }
}
