package com.col.notnetmediaforge.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Guarda los archivos descargados en el almacenamiento público mediante
 * MediaStore (Scoped Storage) y elimina el archivo temporal de la app.
 */
object MediaStoreManager {

    /**
     * Copia [sourceFile] al almacenamiento público y devuelve su [Uri].
     * @param isAudio true → carpeta Música/Descargas y colección de audio; false → Descargas.
     */
    fun save(context: Context, sourceFile: File, displayName: String, mimeType: String, isAudio: Boolean): Uri {
        val resolver = context.contentResolver
        val cleanName = sanitizeDisplayName(displayName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = if (isAudio) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            val relativePath = if (isAudio) {
                "${Environment.DIRECTORY_MUSIC}/NotnetMediaForge"
            } else {
                "${Environment.DIRECTORY_DOWNLOADS}/NotnetMediaForge"
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values)
                ?: throw IllegalStateException("No se pudo insertar en MediaStore")
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    sourceFile.inputStream().use { it.copyTo(out) }
                } ?: throw IllegalStateException("No se pudo abrir el destino")
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            // Android 9 y anteriores: escritura directa + escaneo
            val dir = if (isAudio) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }
            val subDir = File(dir, "NotnetMediaForge").apply { mkdirs() }
            val dest = File(subDir, cleanName)
            sourceFile.copyTo(dest, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(mimeType), null)
            return Uri.fromFile(dest)
        }
    }

    private fun sanitizeDisplayName(name: String): String {
        val cleaned = name.replace(Regex("""[\\/:*?"<>|]"""), "_")
        return if (cleaned.isBlank()) "descarga" else cleaned
    }

    /**
     * Elimina un directorio temporal de descarga de la app.
     */
    fun deleteTempDirectory(dir: File?) {
        if (dir != null) dir.deleteRecursively()
    }
}
