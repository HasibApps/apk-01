package com.example.util

import android.content.Context
import android.os.Environment
import com.example.model.StudyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object PdfDownloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Directory for storing offline study sheets / PDFs.
     */
    fun getPdfStorageDir(context: Context): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val dir = File(baseDir, "study_sheets")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * File location for a given subject and lecture PDF.
     */
    fun getPdfFileForLecture(context: Context, subjectId: String, lectureId: Int): File {
        val dir = getPdfStorageDir(context)
        return File(dir, "sheet_${subjectId}_${lectureId}.pdf")
    }

    /**
     * Check if offline PDF exists and is valid.
     */
    fun isPdfDownloaded(context: Context, subjectId: String, lectureId: Int): Boolean {
        val file = getPdfFileForLecture(context, subjectId, lectureId)
        return isLocalPdfValid(file)
    }

    /**
     * Validates whether a file exists, is non-empty, and has PDF signature (%PDF).
     */
    fun isLocalPdfValid(file: File): Boolean {
        if (!file.exists() || !file.canRead() || file.length() < 1000) return false
        return try {
            file.inputStream().use { stream ->
                val header = ByteArray(8)
                val count = stream.read(header)
                if (count < 4) return false
                val headerStr = String(header, 0, count)
                headerStr.startsWith("%PDF")
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Resolves Google Drive or generic URLs into direct download links.
     */
    fun resolveDirectDownloadUrl(sheetUrl: String): String {
        val trimmed = sheetUrl.trim()
        if (trimmed.isBlank()) return ""

        if (trimmed.contains("drive.google.com", ignoreCase = true)) {
            val fileIdRegex = Regex("""/file/d/([a-zA-Z0-9_-]+)""")
            val match = fileIdRegex.find(trimmed)
            val fileId = match?.groupValues?.get(1) ?: Regex("""[?&]id=([a-zA-Z0-9_-]+)""").find(trimmed)?.groupValues?.get(1)
            if (fileId != null) {
                return "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t"
            }
        }
        return trimmed
    }

    /**
     * Downloads PDF file with live progress.
     */
    suspend fun downloadPdf(
        context: Context,
        sheetUrl: String,
        subjectId: String,
        lectureId: Int,
        onProgress: (percent: Int, status: String) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val directUrl = resolveDirectDownloadUrl(sheetUrl)
        if (directUrl.isBlank()) {
            withContext(Dispatchers.Main) {
                onProgress(0, "পিডিএফ লিঙ্ক পাওয়া যায়নি")
            }
            return@withContext null
        }

        val targetFile = getPdfFileForLecture(context, subjectId, lectureId)
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")

        try {
            withContext(Dispatchers.Main) {
                onProgress(5, "পিডিএফ ডাউনলোডের জন্য সংযোগ হচ্ছে...")
            }

            val request = Request.Builder()
                .url(directUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    onProgress(0, "ডাউনলোড ব্যর্থ (সার্ভার কোড: ${response.code})")
                }
                return@withContext null
            }

            val body = response.body ?: return@withContext null
            val contentLength = body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            var totalBytesRead = 0L
            var lastPercent = -1

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                if (contentLength > 0) {
                    val percent = ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 100)
                    if (percent != lastPercent) {
                        lastPercent = percent
                        val readKb = totalBytesRead / 1024
                        val totalKb = contentLength / 1024
                        withContext(Dispatchers.Main) {
                            onProgress(percent, "$readKb KB / $totalKb KB ($percent%)")
                        }
                    }
                } else {
                    val readKb = totalBytesRead / 1024
                    withContext(Dispatchers.Main) {
                        onProgress(50, "$readKb KB ডাউনলোড হয়েছে...")
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Verify if downloaded file is genuine PDF
            if (!isLocalPdfValid(tempFile)) {
                tempFile.delete()
                withContext(Dispatchers.Main) {
                    onProgress(0, "ডাউনলোড করা ফাইলটি সঠিক PDF নয় (লিঙ্কটি পাবলিক অ্যাক্সেসযোগ্য কিনা যাচাই করুন)")
                }
                return@withContext null
            }

            // Atomically replace target file
            if (targetFile.exists()) {
                targetFile.delete()
            }
            tempFile.renameTo(targetFile)

            withContext(Dispatchers.Main) {
                onProgress(100, "পিডিএফ সফলভাবে ডাউনলোড হয়েছে! অফলাইনে রেডি ✓")
            }

            return@withContext targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            withContext(Dispatchers.Main) {
                onProgress(0, "ত্রুটি: ${e.localizedMessage ?: "ডাউনলোড ব্যর্থ"}")
            }
            return@withContext null
        }
    }

    /**
     * Delete offline PDF.
     */
    fun deleteDownloadedPdf(context: Context, subjectId: String, lectureId: Int): Boolean {
        val file = getPdfFileForLecture(context, subjectId, lectureId)
        return if (file.exists()) file.delete() else false
    }
}
