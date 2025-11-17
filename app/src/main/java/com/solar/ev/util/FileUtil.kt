package com.solar.ev.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object FileUtil {

    private const val TAG = "FileUtil"

    /**
     * Creates a temporary file from a given Uri.
     * The file is created in the application's cache directory.
     *
     * @param context The application context.
     * @param uri The Uri of the content to be copied.
     * @return A File object pointing to the copied content, or null if an error occurs.
     */
    fun getFileFromUri(context: Context, uri: Uri): File? {
        var inputStream: InputStream? = null
        var fileOutputStream: FileOutputStream? = null
        var file: File? = null

        try {
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Unable to open input stream from URI: $uri")
                return null
            }

            // Attempt to get the original file name and extension
            val fileName = getFileName(context, uri) ?: "temp_file_${System.currentTimeMillis()}"
            val tempFile = File(context.cacheDir, fileName)
            tempFile.deleteOnExit() // Ensures the file is deleted when the VM terminates

            fileOutputStream = FileOutputStream(tempFile)
            val buffer = ByteArray(4 * 1024) // 4KB buffer
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                fileOutputStream.write(buffer, 0, read)
            }
            fileOutputStream.flush()
            file = tempFile

        } catch (e: IOException) {
            Log.e(TAG, "Error copying file from URI: $uri", e)
            file?.delete() // Clean up partially created file if error occurs
            return null
        } finally {
            try {
                inputStream?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing input stream", e)
            }
            try {
                fileOutputStream?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing output stream", e)
            }
        }
        return file
    }

    /**
     * Tries to get the file name from a content URI.
     */
    private fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            fileName = cursor.getString(displayNameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting file name from content URI", e)
            }
        }
        if (fileName == null) {
            fileName = uri.path
            val cut = fileName?.lastIndexOf('/')
            if (cut != -1) {
                fileName = fileName?.substring(cut!! + 1)
            }
        }
        // Sanitize the file name if it's too long or contains invalid characters (optional)
        // For simplicity, this basic version doesn't include extensive sanitization.
        return fileName?.replace("[^a-zA-Z0-9._-]".toRegex(), "_") // Basic sanitization
    }
}
