package app.learning.mediachunkupload.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.learning.mediachunkupload.Camera1VideoFragment
import app.learning.mediachunkupload.R

class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        savedInstanceState ?: supportFragmentManager.beginTransaction()
            .replace(R.id.container, Camera1VideoFragment.newInstance())
            .commit()
    }

}
