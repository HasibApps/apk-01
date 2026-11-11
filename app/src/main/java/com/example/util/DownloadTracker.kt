package com.example.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ডাউনলোড ডায়ালগের বাইরে (অ্যাপ-লেভেলে) চলা ডাউনলোডের অবস্থা।
 * ডায়ালগ Hide/বন্ধ করলেও ডাউনলোড চলতে থাকে এবং % ও স্পিড এখান থেকে দেখানো যায়।
 */
object DownloadTracker {
    data class State(
        val active: Boolean = false,
        val lectureId: Int = -1,
        val progress: DlProgress = DlProgress(),
        val status: String = ""
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun start(
        context: Context,
        lectureId: Int,
        url: String,
        format: DlFormat,
        onCompleted: (String) -> Unit
    ) {
        if (_state.value.active) return
        val app = context.applicationContext
        _state.value = State(active = true, lectureId = lectureId, status = "Downloading…")
        scope.launch {
            try {
                val dir = VideoDownloader.getVideoStorageDir(app)
                val file = YtDlpEngine.download(
                    context = app,
                    url = url,
                    format = format,
                    targetDir = dir,
                    baseName = "lecture_${lectureId}_${System.currentTimeMillis()}"
                ) { p -> _state.value = _state.value.copy(progress = p) }

                if (VideoDownloader.isLocalVideoValid(file.absolutePath)) {
                    _state.value = State(active = false, lectureId = lectureId, status = "Done ✅")
                    onCompleted(file.absolutePath)
                } else {
                    file.delete()
                    _state.value = State(
                        active = false, lectureId = lectureId,
                        status = "Failed: ডাউনলোড হওয়া ফাইলটি কার্যকর ভিডিও নয়"
                    )
                }
            } catch (e: DownloadCancelledException) {
                _state.value = State(active = false, lectureId = lectureId, status = "Cancelled")
            } catch (e: Throwable) {
                _state.value = State(
                    active = false, lectureId = lectureId,
                    status = "Failed: ${e.message?.take(200)}"
                )
            }
        }
    }

    fun cancel() {
        if (_state.value.active) {
            _state.value = _state.value.copy(status = "Cancelling…")
            YtDlpEngine.cancel()
        }
    }

    fun clearStatus() {
        if (!_state.value.active) _state.value = State()
    }
}
