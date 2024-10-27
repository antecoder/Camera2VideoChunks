package app.learning.mediachunkupload.ui.ui.record

import java.io.File

data class Recording(val name: String,
                     val folderPath: String,
                     val parts: List<File>) {
    val recordCount: Int
        get() = parts.size
}