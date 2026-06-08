package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder

class BrowserDownloadManager(
    private val context: Context,
    private val repository: BrowserRepository
) {
    private val okHttpClient = OkHttpClient.Builder().build()

    suspend fun startDownload(
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null
    ) {
        withContext(Dispatchers.IO) {
            // 1. Resolve safe file name
            val fileName = resolveFileName(url, contentDisposition, mimeType)
            val finalMimeType = mimeType ?: getMimeTypeFromUrl(url)

            // 2. Insert PENDING download entry into database
            val entryId = repository.insertDownload(
                DownloadEntry(
                    url = url,
                    fileName = fileName,
                    mimeType = finalMimeType,
                    filePath = "",
                    status = "PENDING"
                )
            ).toInt()

            try {
                // 3. Build HTTP request
                val requestBuilder = Request.Builder().url(url)
                if (userAgent != null) {
                    requestBuilder.header("User-Agent", userAgent)
                }
                
                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful) {
                    throw Exception("Server returned code ${response.code}")
                }

                val body = response.body ?: throw Exception("Empty response body")
                val totalBytes = body.contentLength()

                // 4. Update status to DOWNLOADING
                var downloadEntry = repository.getDownloadById(entryId) ?: return@withContext
                repository.updateDownload(downloadEntry.copy(status = "DOWNLOADING", totalBytes = totalBytes))

                // 5. Open output stream (using MediaStore for general compatibility)
                val targetUriAndStream = createDownloadOutputStream(fileName, finalMimeType)
                if (targetUriAndStream == null) {
                    throw Exception("Could not create output stream")
                }

                val (targetUri, outputStream) = targetUriAndStream

                // 6. Copy streams with progress updates
                val inputStream = body.byteStream()
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes = 0L
                var lastUpdateMillis = System.currentTimeMillis()

                outputStream.use { out ->
                    inputStream.use { input ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            out.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val now = System.currentTimeMillis()
                            // Limit database updates to max once every 200ms to avoid clogging UI
                            if (now - lastUpdateMillis > 200 || downloadedBytes == totalBytes) {
                                lastUpdateMillis = now
                                val progress = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat() * 100f) else 0f
                                downloadEntry = repository.getDownloadById(entryId) ?: break
                                repository.updateDownload(
                                    downloadEntry.copy(
                                        progress = progress,
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = totalBytes
                                    )
                                )
                            }
                        }
                    }
                }

                // 7. Download complete!
                downloadEntry = repository.getDownloadById(entryId) ?: return@withContext
                repository.updateDownload(
                    downloadEntry.copy(
                        status = "COMPLETED",
                        progress = 100f,
                        downloadedBytes = totalBytes,
                        filePath = targetUri.toString()
                    )
                )

            } catch (e: Exception) {
                Log.e("BrowserDownloadManager", "Error downloading file: ${e.message}", e)
                val downloadEntry = repository.getDownloadById(entryId)
                if (downloadEntry != null) {
                    repository.updateDownload(
                        downloadEntry.copy(
                            status = "FAILED",
                            progress = 0f
                        )
                    )
                }
            }
        }
    }

    private fun resolveFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        var filename = ""
        if (contentDisposition != null) {
            val index = contentDisposition.indexOf("filename=")
            if (index >= 0) {
                var rawName = contentDisposition.substring(index + 9).trim().replace("\"", "")
                val spaceIndex = rawName.indexOf(" ")
                if (spaceIndex > 0) {
                    rawName = rawName.substring(0, spaceIndex)
                }
                filename = rawName
            }
        }

        if (filename.isBlank()) {
            val uri = Uri.parse(url)
            filename = uri.lastPathSegment ?: "downloadfile"
            try {
                filename = URLDecoder.decode(filename, "UTF-8")
            } catch (e: Exception) {
                // Ignore decoding error
            }
        }

        // Clean query parameters from file name if any
        if (filename.contains("?")) {
            filename = filename.substring(0, filename.indexOf("?"))
        }

        // Append extension based on mimeType if extension is missing
        if (!filename.contains(".") && mimeType != null) {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (ext != null) {
                filename = "$filename.$ext"
            }
        }

        if (filename.isBlank() || filename == "/") {
            filename = "download_" + System.currentTimeMillis()
        }

        return filename
    }

    private fun getMimeTypeFromUrl(url: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    private fun createDownloadOutputStream(fileName: String, mimeType: String): Pair<Uri, OutputStream>? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Failed to insert MediaStore download")
                val stream = resolver.openOutputStream(uri) ?: throw Exception("Failed to open Uri output stream")
                Pair(uri, stream)
            } else {
                val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, fileName)
                val uri = Uri.fromFile(file)
                val stream = FileOutputStream(file)
                Pair(uri, stream)
            }
        } catch (e: Exception) {
            Log.e("BrowserDownloadManager", "Failed to create public Downloads stream, falling back to cache", e)
            try {
                val file = File(context.cacheDir, fileName)
                val uri = Uri.fromFile(file)
                val stream = FileOutputStream(file)
                Pair(uri, stream)
            } catch (ex: Exception) {
                null
            }
        }
    }
}
