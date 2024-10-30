package app.learning.mediachunkupload.ui.main.record

import java.io.File

data class Session(val name: String,
                   val filePath: String,
                   val parts: List<File>) {
    val recordCount: Int
        get() = parts.size
}