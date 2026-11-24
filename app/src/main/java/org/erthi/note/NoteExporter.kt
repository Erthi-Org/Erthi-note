package org.erthi.note

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import java.io.OutputStream

/** Visible offline rendering of rich note content as PDF or a single PNG. */
object NoteExporter {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val PAD = 42

    private fun layout(context: Context, note: Note, width: Int, textSize: Float): StaticLayout {
        val styled = RichNotes.deserialize(context, note.richHtml, note.body, width)
        val content = SpannableStringBuilder(styled)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(37, 49, 51)
            this.textSize = textSize
        }
        return StaticLayout.Builder.obtain(content, 0, content.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(true)
            .setLineSpacing(2f, 1.1f)
            .build()
    }

    private fun drawHeader(canvas: Canvas, note: Note, width: Int, pad: Int) {
        canvas.drawColor(Color.rgb(255, 253, 245))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (note.titleInk == 0) Color.rgb(15, 105, 103) else note.titleInk
            textSize = note.titleSizeSp.coerceIn(14, 36).toFloat() *
                (if (width > 700) 1.4f else 1f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val title = note.title
        val shown = if (title.length > 100) title.take(99) + "…" else title
        canvas.drawText(shown, pad.toFloat(), 79f, paint)
        paint.color = Color.rgb(239, 191, 99)
        canvas.drawRoundRect(pad.toFloat(), 96f, (width-pad).toFloat(),
            100f, 2f, 2f, paint)
    }

    fun txt(note: Note, destination: OutputStream) {
        destination.write((note.title + "\n\n" + note.body).toByteArray(Charsets.UTF_8))
        destination.flush()
    }

    fun html(context: Context, note: Note, destination: OutputStream) {
        val escaped = android.text.TextUtils.htmlEncode(note.title)
        val body = if (note.richHtml.isNotBlank()) {
            RichNotes.serialize(RichNotes.deserialize(context, note.richHtml, note.body))
        } else android.text.TextUtils.htmlEncode(note.body).replace("\n", "<br/>")
        val ink = if (note.titleInk == 0) "#0F6967"
            else String.format("#%06X", note.titleInk and 0xFFFFFF)
        val output = "<!doctype html><html lang=\"ar\"><head><meta charset=\"utf-8\"/>" +
            "<title>" + escaped + "</title></head><body><h1 style=\"color:" + ink +
            ";font-size:" + note.titleSizeSp.coerceIn(14, 36) + "px\">" + escaped +
            "</h1><article>" + body + "</article></body></html>"
        destination.write(output.toByteArray(Charsets.UTF_8))
        destination.flush()
    }

    fun pdf(context: Context, note: Note, destination: OutputStream) {
        val document = PdfDocument()
        try {
            val text = layout(context, note, PAGE_W - 2 * PAD, 13f)
            val available = PAGE_H - 150
            val pages = maxOf(1, (text.height + available - 1) / available)
            require(pages <= 75) { "Note too long for PDF export" }
            for (pageIndex in 0 until pages) {
                val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageIndex + 1).create()
                val page = document.startPage(info)
                val canvas = page.canvas
                drawHeader(canvas, note, PAGE_W, PAD)
                canvas.save()
                canvas.clipRect(PAD, 125, PAGE_W - PAD, PAGE_H - 30)
                canvas.translate(PAD.toFloat(), 125f - pageIndex * available)
                text.draw(canvas)
                canvas.restore()
                document.finishPage(page)
            }
            document.writeTo(destination)
        } finally {
            document.close()
        }
    }

    fun png(context: Context, note: Note, destination: OutputStream) {
        val width = 1080
        val layout = layout(context, note, width - 100, 30f)
        val height = (layout.height + 230).coerceAtLeast(750)
        require(height <= 8000) { "Long note: use PDF export instead" }
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bmp)
            canvas.save()
            canvas.scale(1f, 1f)
            drawHeader(canvas, note, width, 50)
            canvas.translate(50f, 150f)
            layout.draw(canvas)
            canvas.restore()
            require(bmp.compress(Bitmap.CompressFormat.PNG, 100, destination)) {
                "PNG conversion failed"
            }
            destination.flush()
        } finally {
            bmp.recycle()
        }
    }

    /**
     * Minimal DOCX (Office Open XML) export. Produces a valid .docx readable by
     * Word, LibreOffice, and Google Docs. Rich-text spans are flattened to plain
     * paragraphs for portability; the note title is styled as Heading 1.
     */
    fun docx(note: Note, destination: OutputStream) {
        val titleText = escapeXml(note.title.ifBlank { "Untitled" })
        val bodyLines = note.body.split('\n')
        val paragraphs = buildString {
            bodyLines.forEach { line ->
                append("<w:p><w:r><w:t xml:space=\"preserve\">")
                append(escapeXml(line))
                append("</w:t></w:r></w:p>")
            }
        }
        val doc = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:wpc="http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas" xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006" xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math" xmlns:v="urn:schemas-microsoft-com:vml" xmlns:wp14="http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:w10="urn:schemas-microsoft-com:office:word" xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml" xmlns:wpg="http://schemas.microsoft.com/office/word/2010/wordprocessingGroup" xmlns:wpi="http://schemas.microsoft.com/office/word/2010/wordprocessingInk" xmlns:wne="http://schemas.microsoft.com/office/word/2006/wordml" xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape" mc:Ignorable="w14 wp14">
<w:body>
<w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>$titleText</w:t></w:r></w:p>
$paragraphs
<w:sectPr><w:pgSz w:w="12240" w:h="15840"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr>
</w:body></w:document>"""
        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
        val styles = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:rPr><w:b/><w:sz w:val="52"/></w:rPr></w:style>
<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:rPr><w:sz w:val="24"/></w:rPr></w:style>
</w:styles>"""
        val packageRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""
        java.util.zip.ZipOutputStream(destination).use { zip ->
            fun entry(name: String, content: String) {
                zip.putNextEntry(java.util.zip.ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entry("[Content_Types].xml", contentTypes)
            entry("_rels/.rels", packageRels)
            entry("word/document.xml", doc)
            entry("word/_rels/document.xml.rels", rels)
            entry("word/styles.xml", styles)
        }
    }

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
