package org.erthi.note

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast

class TrashActivity : Activity() {

    private lateinit var database: NoteStore
    private lateinit var listContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(Palette.BACK)
        window.insetsController?.hide(WindowInsets.Type.systemBars())
        database = NoteStore(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Palette.BACK)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(Palette.PANEL)
        }
        header.addView(icon(R.drawable.ic_collapse, getString(R.string.about_back),
            Palette.PANEL_LIGHT, 38) { finish() })
        header.addView(label(getString(R.string.trash_title), 18f, Palette.WHITE, true).apply {
            setPadding(dp(14), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(action(getString(R.string.trash_empty), false) {
            confirmEmptyTrash()
        }.apply {
            textSize = 13f
        })
        root.addView(header, LinearLayout.LayoutParams(-1, -2))

        val scroll = ScrollView(this)
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }
        scroll.addView(listContainer)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        displayDeleted()
    }

    private fun displayDeleted() {
        listContainer.removeAllViews()
        val deleted = database.allDeleted()
        if (deleted.isEmpty()) {
            listContainer.addView(label(getString(R.string.trash_empty_message), 15f,
                Palette.MUTED).apply {
                gravity = Gravity.CENTER
                val v = dp(60)
                setPadding(dp(20), v, dp(20), v)
            })
            return
        }
        deleted.forEach { note ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                background = roundShape(Palette.PANEL, 14, Palette.BORDER)
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.bottomMargin = dp(8)
                layoutParams = lp
            }
            val title = note.title.ifBlank { getString(R.string.untitled) }
            card.addView(label(title, 15f, Palette.WHITE, true))
            if (note.body.isNotBlank()) {
                card.addView(label(NoteText.preview(note.body).take(80), 13f, Palette.MUTED).apply {
                    setPadding(0, dp(4), 0, dp(8))
                })
            }
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }
            actions.addView(action(getString(R.string.trash_restore), true) {
                database.restore(note.id)
                displayDeleted()
                Toast.makeText(this, R.string.note_restored, Toast.LENGTH_SHORT).show()
            }.apply {
                val lp = LinearLayout.LayoutParams(-2, -2)
                lp.marginEnd = dp(8)
                layoutParams = lp
            })
            actions.addView(action(getString(R.string.trash_delete_forever), false) {
                confirmDeleteForever(note.id, title)
            })
            card.addView(actions)
            listContainer.addView(card)
        }
    }

    private fun confirmDeleteForever(id: Long, title: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.trash_delete_forever))
            .setMessage(getString(R.string.trash_delete_confirm, title))
            .setPositiveButton(getString(R.string.trash_delete_forever)) { _, _ ->
                database.permanentlyDelete(id)
                displayDeleted()
            }
            .setNegativeButton(getString(R.string.keep_editing), null)
            .show()
    }

    private fun confirmEmptyTrash() {
        val count = database.allDeleted().size
        if (count == 0) return
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.trash_empty))
            .setMessage(getString(R.string.trash_empty_confirm, count))
            .setPositiveButton(getString(R.string.trash_empty)) { _, _ ->
                database.emptyTrash()
                displayDeleted()
            }
            .setNegativeButton(getString(R.string.keep_editing), null)
            .show()
    }
}
