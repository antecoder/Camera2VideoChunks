package app.learning.mediachunkupload.ui

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.learning.mediachunkupload.ui.ui.record.Recording
import app.learning.mediachunkupload.util.FileUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainActivityViewModel: ViewModel() {

    companion object {
        const val LOG_TAG = "MainActivityViewModel"
    }

    private var _recordings: MutableLiveData<List<Recording>> = MutableLiveData()
    val recordings: LiveData<List<Recording>> = _recordings

    fun refreshFileList(context: Context) {
        Log.d(LOG_TAG, "Refreshing file list")
        val recordings = mutableListOf<Recording>()
        val filesDir = FileUtils.getStorageDir(context)
        Log.d(LOG_TAG, "Files dir: ${filesDir.absolutePath}")
        filesDir.listFiles()?.let { fileList ->
            for (file in fileList) {
                Log.d(LOG_TAG, "Session: ${file.name}")
                recordings.add(Recording(file.name, file.path, file.listFiles()?.toList() ?: listOf()))
            }
        }
        _recordings.postValue(recordings)
    }

}