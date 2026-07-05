package com.tapman104.mpvplayer.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object UriResolver {

    /**
     * Returns a human-readable display name for [uri].
     *
     * Resolution order:
     *  1. Query [OpenableColumns.DISPLAY_NAME] from ContentResolver (works for
     *     content:// URIs from file managers, Downloads, MediaStore, etc.)
     *  2. Fall back to the last path segment, URL-decoded.
     *  3. If neither yields a non-blank result, return "Unknown".
     */
    fun getDisplayName(context: Context, uri: Uri): String {
        // Attempt 1 — ContentResolver column (most reliable for content:// URIs)
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(0)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            } catch (_: Exception) { /* fall through */ }
        }

        // Attempt 2 — last path segment, URL-decoded
        val segment = uri.lastPathSegment
        if (!segment.isNullOrBlank()) {
            val decoded = Uri.decode(segment)
            // Strip everything before the last '/' that may survive decoding
            val leaf = decoded.substringAfterLast('/')
            if (leaf.isNotBlank()) return leaf
        }

        return "Unknown"
    }
}
