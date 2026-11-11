package com.example.util

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * অফলাইন ভিডিও ফাইলের সংরক্ষণ ও যাচাই সংক্রান্ত সহায়ক ফাংশন।
 * (ডাউনলোডের মূল লজিক এখন YtDlpEngine এ — yt-dlp ভিত্তিক)
 */
object VideoDownloader {

    /** অ্যাপের ভেতরে অফলাইন ভিডিও রাখার ফোল্ডার */
    fun getVideoStorageDir(context: Context): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val dir = File(baseDir, "study_videos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /** লোকাল ভিডিও ফাইল আছে কি না এবং সেটি সত্যিকারের মিডিয়া ফাইল কি না (HTML/এরর পেজ নয়) যাচাই */
    fun isLocalVideoValid(filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) return false
        val file = File(filePath)
        if (!file.exists() || !file.canRead() || file.length() < 20_000) return false

        return try {
            file.inputStream().use { stream ->
                val header = ByteArray(128)
                val count = stream.read(header)
                if (count < 16) return false
                val text = String(header, 0, count, Charsets.ISO_8859_1).lowercase()
                if (text.contains("<!doctype") || text.contains("<html") || text.contains("<head") ||
                    text.contains("<?xml") || text.contains("{\"error\"") || text.contains("<title>")
                ) {
                    return false
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /** ডাউনলোড করা ভিডিও মুছে ফেলে */
    fun deleteVideo(filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) return false
        return try {
            val file = File(filePath)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
