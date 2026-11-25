package com.example.util

import android.content.Context
import com.example.data.StudyRepository
import com.example.model.withLocalVideoPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

/**
 * অ্যাপ-লেভেলের ডাউনলোড ম্যানেজার (একসাথে সর্বোচ্চ [MAX_DOWNLOADS] টি ভিডিও)।
 *
 * - ডাউনলোড চলে [VideoDownloadService] (Foreground Service) এর ছায়ায়, তাই অন্য পেজে গেলে
 *   বা অ্যাপ থেকে বের হয়ে গেলেও ব্যাকগ্রাউন্ডে চলতে থাকে এবং নোটিফিকেশনে % / MB দেখায়।
 * - শেষ হলে ফাইলের পাথ লেকচারে সেভ হয় (ViewModel জীবিত থাকলে তার মাধ্যমে, নইলে সরাসরি Repository তে)।
 */
object DownloadTracker {
    const val MAX_DOWNLOADS = 10

    data class Item(
        val key: String,
        val run: Long,
        val subjectId: String,
        val subjectName: String,
        val chapterName: String,
        val lectureId: Int,
        val title: String,
        val oldPath: String = "",
        val active: Boolean = true,
        val progress: DlProgress = DlProgress(),
        val status: String = "",
        val success: Boolean = false,
        val cancelled: Boolean = false
    )

    data class State(val items: Map<String, Item> = emptyMap()) {
        val activeItems: List<Item> get() = items.values.filter { it.active }
        val activeCount: Int get() = items.values.count { it.active }
    }

    enum class StartResult { STARTED, ALREADY_RUNNING, LIMIT_REACHED }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val runCounter = AtomicLong(0)

    /**
     * ViewModel জীবিত থাকলে সে এটি সেট করে (UI লিস্ট আপডেট + সেভ)। true = হ্যান্ডেল করেছে।
     */
    @Volatile
    var completionHandler: ((Item, String) -> Boolean)? = null

    fun key(subjectId: String, chapterName: String, lectureId: Int) = "$subjectId|$chapterName|$lectureId"
    private fun processId(key: String) = "dl_$key"

    fun itemFor(state: State, subjectId: String, chapterName: String, lectureId: Int): Item? =
        state.items[key(subjectId, chapterName, lectureId)]

    fun start(
        context: Context,
        subjectId: String,
        subjectName: String,
        chapterName: String,
        lectureId: Int,
        title: String,
        url: String,
        format: DlFormat,
        oldPath: String
    ): StartResult {
        val app = context.applicationContext
        val key = key(subjectId, chapterName, lectureId)
        val cur = _state.value
        if (cur.items[key]?.active == true) return StartResult.ALREADY_RUNNING
        if (cur.activeCount >= MAX_DOWNLOADS) return StartResult.LIMIT_REACHED

        val run = runCounter.incrementAndGet()
        val item = Item(
            key = key, run = run, subjectId = subjectId, subjectName = subjectName,
            chapterName = chapterName, lectureId = lectureId, title = title,
            oldPath = oldPath, status = "Downloading…"
        )
        _state.update { it.copy(items = it.items + (key to item)) }

        // ফোরগ্রাউন্ড সার্ভিস চালু → অ্যাপ বন্ধ/ব্যাকগ্রাউন্ডে গেলেও প্রসেস বেঁচে থাকে
        VideoDownloadService.ensureRunning(app)

        jobs[key] = scope.launch {
            try {
                val dir = VideoDownloader.getVideoStorageDir(app)
                val file = YtDlpEngine.download(
                    context = app,
                    url = url,
                    format = format,
                    targetDir = dir,
                    baseName = "lecture_${lectureId}_${abs(key.hashCode())}_${System.currentTimeMillis()}",
                    processId = processId(key)
                ) { p -> updateItem(key, run) { it.copy(progress = p) } }

                if (VideoDownloader.isLocalVideoValid(file.absolutePath)) {
                    val path = file.absolutePath
                    val finalItem = _state.value.items[key] ?: item
                    val handled = try {
                        completionHandler?.invoke(finalItem, path) ?: false
                    } catch (_: Throwable) {
                        false
                    }
                    if (!handled) saveDirectly(app, finalItem, path)
                    if (item.oldPath.isNotBlank() && item.oldPath != path) {
                        VideoDownloader.deleteVideo(item.oldPath)
                    }
                    updateItem(key, run) {
                        it.copy(
                            active = false, success = true, status = "Done ✅",
                            progress = it.progress.copy(percent = 1f, processing = false)
                        )
                    }
                } else {
                    file.delete()
                    updateItem(key, run) {
                        it.copy(active = false, status = "Failed: ডাউনলোড হওয়া ফাইলটি কার্যকর ভিডিও নয়")
                    }
                }
            } catch (e: DownloadCancelledException) {
                updateItem(key, run) { it.copy(active = false, cancelled = true, status = "Cancelled") }
            } catch (e: kotlinx.coroutines.CancellationException) {
                updateItem(key, run) { it.copy(active = false, cancelled = true, status = "Cancelled") }
                throw e
            } catch (e: Throwable) {
                updateItem(key, run) { it.copy(active = false, status = "Failed: ${e.message?.take(200)}") }
            } finally {
                jobs.remove(key)
            }
        }
        return StartResult.STARTED
    }

    /** পুরোনো (আগের রান) কলব্যাক যেন নতুন রানের আইটেম নষ্ট না করে — run মেলালে তবেই আপডেট */
    private fun updateItem(key: String, run: Long, block: (Item) -> Item) {
        _state.update { st ->
            val it = st.items[key] ?: return@update st
            if (it.run != run) return@update st
            st.copy(items = st.items + (key to block(it)))
        }
    }

    /** UI বন্ধ থাকলে (ViewModel নেই) সরাসরি সংরক্ষণে লেকচারের পাথ বসায় */
    private fun saveDirectly(app: Context, item: Item, path: String) {
        try {
            val repo = StudyRepository(app)
            val updated = repo.loadSubjects().withLocalVideoPath(item.subjectId, item.chapterName, item.lectureId, path)
            if (updated != null) repo.saveSubjects(updated)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun cancel(key: String) {
        val it = _state.value.items[key] ?: return
        if (!it.active) return
        _state.update { st ->
            val cur = st.items[key] ?: return@update st
            st.copy(items = st.items + (key to cur.copy(status = "Cancelling…")))
        }
        YtDlpEngine.cancel(processId(key))
    }

    fun cancelAll() {
        _state.value.activeItems.forEach { cancel(it.key) }
    }

    /** শেষ হওয়া (সফল/ব্যর্থ/বাতিল) আইটেমের স্ট্যাটাস মুছে ফেলে */
    fun clearStatus(key: String) {
        _state.update { st ->
            val cur = st.items[key] ?: return@update st
            if (cur.active) st else st.copy(items = st.items - key)
        }
    }
}
