package app.learning.mediachunkupload.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File

object FileUtils {

    /**
     * Get the storage directory for the app.
     */
    fun getStorageDir(context: Context): File {
        return context.getExternalFilesDir("Videos")!!
    }

    /**
     * Create a file for saving a video.
     */
    fun getOutputMediaFile(context: Context, sessionName: String): File? {
        val dir = File(getStorageDir(context), sessionName)
        dir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    Log.d("MyCameraApp", "failed to create directory")
                    return null
                }
            }
        }
        return dir
    }

}