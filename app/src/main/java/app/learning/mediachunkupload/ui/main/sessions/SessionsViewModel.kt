package app.learning.mediachunkupload.ui.main.sessions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.learning.mediachunkupload.ui.main.record.Session
import app.learning.mediachunkupload.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for handling [Session] related data to be displayed on the Session List.
 */
class SessionsViewModel: ViewModel() {

    private var _recordings: MutableStateFlow<List<Session>?> = MutableStateFlow(null)
    val recordings: StateFlow<List<Session>?> = _recordings

    fun refreshSessionList(context: Context) {
        viewModelScope.launch {
            loadRecordedSessions(context)
        }
    }

    private suspend fun loadRecordedSessions(context: Context) {
        val recordings = mutableListOf<Session>()
        val filesDir = FileUtils.getStorageDir(context)
        filesDir.listFiles()?.let { fileList ->
            for (file in fileList) {
                recordings.add(Session(file.name, file.path, file.listFiles()?.toList() ?: listOf()))
            }
        }
        _recordings.emit(recordings)
    }

}