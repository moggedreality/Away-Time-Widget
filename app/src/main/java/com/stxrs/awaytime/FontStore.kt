package com.stxrs.awaytime

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/** Imports and loads the user's own font file. Widgets can't set a typeface on a TextView, so text is drawn into a bitmap. */
object FontStore {
    private const val FILE_NAME = "custom_font.ttf"
    private const val TEMP_NAME = "font_import.tmp"
    private const val MAX_BYTES = 25L * 1024 * 1024

    private var cached: Typeface? = null
    private var cachedStamp = 0L

    /** Copies the picked file into app storage. Returns its display name, or null if it isn't a usable font. */
    fun import(context: Context, uri: Uri): String? {
        val temp = File(context.filesDir, TEMP_NAME)
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { src -> temp.outputStream().use { dst -> src.copyTo(dst) } }
            if (temp.length() == 0L || temp.length() > MAX_BYTES) {
                temp.delete()
                return null
            }
            if (Typeface.Builder(temp).build() == null) {
                temp.delete()
                return null
            }
            val dest = File(context.filesDir, FILE_NAME)
            dest.delete()
            if (!temp.renameTo(dest)) return null
            cached = null
            displayName(context, uri)
        } catch (e: Exception) {
            temp.delete()
            null
        }
    }

    fun load(context: Context): Typeface? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null
        if (cached != null && cachedStamp == file.lastModified()) return cached
        val face = try {
            Typeface.Builder(file).build()
        } catch (e: Exception) {
            null
        }
        cached = face
        cachedStamp = file.lastModified()
        return face
    }

    private fun displayName(context: Context, uri: Uri): String {
        val name = try {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (e: Exception) {
            null
        }
        return name?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "Custom font"
    }
}
