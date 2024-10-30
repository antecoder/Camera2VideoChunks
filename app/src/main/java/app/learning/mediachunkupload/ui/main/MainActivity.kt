package app.learning.mediachunkupload.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.learning.mediachunkupload.ui.main.sessions.SessionListScreen
import app.learning.mediachunkupload.ui.main.sessions.SessionsViewModel
import app.learning.mediachunkupload.ui.theme.MediaChunkUploadTheme
import app.learning.mediachunkupload.ui.main.record.RecordingActivity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.sample.android.screens.playback.PlaybackScreen
import app.learning.mediachunkupload.ui.main.playback.PlaybackViewModel

/**
 * Main entry-point of the Application.
 */
class MainActivity : ComponentActivity() {

    companion object {

        private val PERMISSIONS_REQUIRED = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO)

        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }


    private val sessionsViewModel: SessionsViewModel by viewModels()
    private val playbackViewModel: PlaybackViewModel by viewModels()

    // Used later to request camera & recording permissions.
    private val requestRecordingPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val isGranted = result.all { it.value }
            if (isGranted) {
                startRecordingActivity()
            } else {
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                // Ignore for now.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }

    private fun startRecordingActivity() {
        if (!hasPermissions(this)) {
            requestRecordingPermissionsLauncher.launch(PERMISSIONS_REQUIRED)
            return
        }
        startActivity(Intent(this, RecordingActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        // refresh list of sessions from the storage directory
        sessionsViewModel.refreshSessionList(applicationContext)
    }

    @Composable
    private fun App() {
        MediaChunkUploadTheme {
            val navController = rememberNavController()
            NavHost (
                navController = navController, startDestination = Screen.Sessions.route
            ) {
                composable(Screen.Sessions.route) { SessionListScreen(
                    viewModel = sessionsViewModel,
                    onRecordButtonClick = {
                        startRecordingActivity()
                    },
                    onSelectSession = { session ->
                        navController.navigateTo(Screen.Playback.createRoute(session.filePath))
                    }
                )
                }
                composable(Screen.Playback.route, arguments = listOf(navArgument(Screen.ARG_FILE_PATH) {
                    nullable = false
                    type = NavType.StringType
                })) {
                    val sessionPath = Screen.Playback.getFilePath(it.arguments)
                    PlaybackScreen(sessionPath, navController, playbackViewModel)
                }
            }
        }
    }

}

fun NavHostController.navigateTo(route: String) = navigate(route) {
    popUpTo(route)
    launchSingleTop = false
}

sealed class Screen(val route: String) {
    data object Sessions : Screen("sessions")
    data object Playback : Screen("playback?${ARG_FILE_PATH}={$ARG_FILE_PATH}") {
        fun createRoute(filePath: String): String {
            return "playback?${ARG_FILE_PATH}=${filePath}"
        }

        fun getFilePath(bundle: Bundle?): String {
            return bundle?.getString(ARG_FILE_PATH)!!
        }
    }
    companion object {
        const val ARG_FILE_PATH: String = "arg_file_path"
    }
}