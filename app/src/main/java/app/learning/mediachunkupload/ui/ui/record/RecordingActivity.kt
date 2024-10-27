package app.learning.mediachunkupload.ui.ui.record

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
import android.hardware.camera2.CameraCharacteristics.SENSOR_ORIENTATION
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.learning.mediachunkupload.ui.ui.record.RecordingActivity.Companion.LOG_TAG
import app.learning.mediachunkupload.ui.custom.AutoFitTextureView
import app.learning.mediachunkupload.ui.theme.MediaChunkUploadTheme
import app.learning.mediachunkupload.util.FileUtils
import app.learning.mediachunkupload.util.Constants
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.max


class RecordingActivity : ComponentActivity(), TextureView.SurfaceTextureListener,
    MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener {

    companion object {
        const val LOG_TAG = "RecordingActivity Logger"

        private const val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
        private const val SENSOR_ORIENTATION_INVERSE_DEGREES = 270
        private val DEFAULT_ORIENTATIONS = SparseIntArray()
        private val INVERSE_ORIENTATIONS = SparseIntArray()

        private val PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )

        init {
            DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90)
            DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0)
            DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270)
            DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        init {
            INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270)
            INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180)
            INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90)
            INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0)
        }
    }

    /**
     * Counter index for the number of chunks in record.
     */
    private var chunkIndex = 0

    /**
     * An [AutoFitTextureView] for the camera preview.
     */
    private var textureView: AutoFitTextureView? = null

    /**
     * A reference to the opened [android.hardware.camera2.CameraDevice].
     */
    private var cameraDevice: CameraDevice? = null

    /**
     * A reference to the current [android.hardware.camera2.CameraCaptureSession] for preview.
     */
    private var captureSession: CameraCaptureSession? = null

    /**
     * The [Size] of the camera preview.
     */
    private var previewSize: Size? = null

    /**
     * The [Size] of the recorded video.
     */
    private var videoSize: Size? = null

    /**
     * A reference to the [MediaRecorder] instance for recording audio & video from the specified
     * [cameraDevice]
     */
    private var mediaRecorder: MediaRecorder? = null

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var backgroundThread: HandlerThread? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    private var backgroundHandler: Handler? = null

//    private var backgroundExecutor: ExecutorService? = null

    /**
     * Current state of the [MediaRecorder].
     */
    private var recordingState: RecordingState = RecordingState.IDLE

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val cameraOpenCloseLock = Semaphore(1)

    private var sensorOrientation: Int? = null
    private var nextVideoAbsolutePath: String? = null
    private var outputFile: File? = null
    private var previewBuilder: CaptureRequest.Builder? = null

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its status.
     */
    private val cameraStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            this@RecordingActivity.cameraDevice = cameraDevice
            Log.d(LOG_TAG, "Camera opened")
            // camera device gotten, start preview presentiation to user.
            startPreview()
            cameraOpenCloseLock.release()
            textureView?.let {
                configureTransformFillViewPort(it.width, it.height)
            }
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            // release camera.
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@RecordingActivity.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, errorCode: Int) {
            // release camera.
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@RecordingActivity.cameraDevice = null
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContent {
            var recordingState by rememberSaveable { mutableStateOf(RecordingState.IDLE) }
            var millisInRecord: Long by rememberSaveable { mutableStateOf(0) }

            MediaChunkUploadTheme {
                Box(modifier = Modifier
                    .fillMaxSize()
                ) {
                    CameraFeedView(textureViewConsumer = {
                        Log.d(LOG_TAG, "Gotten texture view.")
                        this@RecordingActivity.textureView = it
                        onResume()
                    })
                    CameraHudView(
                        millisInRecord = millisInRecord,
                        onClickCloseButton = {
                            stopRecording()
                            finish()
                        }
                    )
                    CameraControlView(recordingState, onClickRecordButton = {
                        when(recordingState) {
                            RecordingState.IDLE -> {
                                recordingState = RecordingState.RECORDING
                                startRecording()
                            }

                            RecordingState.RECORDING ->{
                                recordingState = RecordingState.IDLE
                                stopRecording()
                            }
                        }
                    }, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "On Resume")
        startBackgroundExecutor()
        textureView?.let {
            if (it.isAvailable) {
                Log.d(LOG_TAG, "Texture view available")
                openCamera(it.width, it.height)
            } else {
                Log.d(LOG_TAG, "Texture view not available")
                it.surfaceTextureListener = this
            }
        }
    }

    override fun onPause() {
        closeCamera()
        releaseBackgroundExecutor()
        super.onPause()
        Log.d(LOG_TAG, "On Pause called")
    }


    private fun startBackgroundExecutor() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
//        backgroundExecutor = Executors.newSingleThreadExecutor { backgroundThread }
    }

    private fun releaseBackgroundExecutor() {
        backgroundThread?.let {
            it.quitSafely()
            try {
                it.join()
                backgroundThread = null
                backgroundHandler = null
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

//        backgroundExecutor?.let {
//            it.shutdown()
//            backgroundExecutor = null
//        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(width: Int, height: Int) {
        Log.d(LOG_TAG, "Opening camera")
        if (isFinishing) return

        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Timeout acquiring lock when opening camera.")
            }
            val cameraId = cameraManager.cameraIdList[0]

            // Choose the sizes for camera preview and video recording
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(SCALER_STREAM_CONFIGURATION_MAP)
                ?: throw RuntimeException("Cannot get available preview/video sizes")

            sensorOrientation = characteristics.get(SENSOR_ORIENTATION)
            videoSize = chooseVideoSize(
                map.getOutputSizes(
                    MediaRecorder::class.java
                )
            )
            previewSize = chooseOptimalSize(
                map.getOutputSizes(
                    SurfaceTexture::class.java
                ),
                width, height, videoSize
            )

            val orientation = resources.configuration.orientation
            previewSize?.let {
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView?.setAspectRatio(it.width, it.height)
                } else {
                    textureView?.setAspectRatio(it.height, it.width)
                }
            }
            configureTransformFillViewPort(width, height)

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                // fallback for versions < API Version 31, ignore deprecation.
                MediaRecorder()
            }
            cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
//            cameraManager.openCamera(cameraId, backgroundExecutor!!, cameraStateCallback)
        } catch (exception: CameraAccessException) {
            Log.e(LOG_TAG, "Camera access exception: " + exception.message)
            exception.printStackTrace()
            finish()
        } catch (exception: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Log.e(LOG_TAG, "Camera2API not supported exception: " + exception.message)
            exception.printStackTrace()
            finish()
        } catch (exception: InterruptedException)  {
            exception.printStackTrace()
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }
    }
    
    private fun closeCamera() {
        try {
            cameraOpenCloseLock.tryAcquire()
            closePreview()
            cameraDevice?.close()
            cameraDevice = null
        } catch (exception: InterruptedException) {
            Log.d(LOG_TAG, "Interrupted while trying to lock camera closing.")
            exception.printStackTrace()
            throw RuntimeException("Interrupted while trying to lock camera closing.")
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun startPreview() {
        Log.d(LOG_TAG, "Starting preview")
        if (cameraDevice == null || textureView == null || !textureView!!.isAvailable || previewSize == null) {
            Log.d(LOG_TAG, "Camera not ready")
            return
        }
        try {
            closePreview()
            val surfaceTexture = checkNotNull(textureView!!.surfaceTexture)
            surfaceTexture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
            previewBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            val previewSurface = Surface(surfaceTexture)
            previewBuilder!!.addTarget(previewSurface)

            val sessionCallback: CameraCaptureSession.StateCallback = object: CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    Log.d(LOG_TAG, "Configured camera")
                    captureSession = session
                    updatePreview()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.d(LOG_TAG, "Failed to configure camera")
                    Log.d(LOG_TAG, "Error failing to configure")
                }
            }

//            val config = SessionConfiguration(SESSION_REGULAR,
//                listOf(OutputConfiguration(previewSurface)),
//                backgroundExecutor!!, sessionCallback)
//            cameraDevice!!.createCaptureSession(config)
            cameraDevice!!.createCaptureSession(listOf(previewSurface), sessionCallback, backgroundHandler)

        } catch (exception: CameraAccessException) {
            exception.printStackTrace()
            Log.d(LOG_TAG, "Camera access exception")
        }
    }

    private fun updatePreview() {
        Log.d(LOG_TAG, "Updating preview")
        cameraDevice?.let {
            try {
                previewBuilder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                captureSession?.setRepeatingRequest(previewBuilder!!.build(), null, backgroundHandler)
            } catch (exception: CameraAccessException) {
                exception.printStackTrace()
                Log.d(LOG_TAG, "Camera access exception")
            }
        }
    }

    private fun closePreview() {
        captureSession?.let {
            it.close()
            captureSession = null
        }
    }

    fun configureTransform(width: Int, height: Int) {
        if (textureView == null || previewSize == null) {
            return
        }
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val bufferRect =
            RectF(0f, 0f, previewSize!!.height.toFloat(), previewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = max(
                (height.toFloat() / previewSize!!.height).toDouble(),
                (width.toFloat() / previewSize!!.width).toDouble()
            ).toFloat()
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        }
        textureView!!.setTransform(matrix)
    }

    private fun configureTransformFillViewPort(viewWidth: Int, viewHeight: Int) {
        if (textureView == null || previewSize == null) {
            return
        }

        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect =
            RectF(0f, 0f, previewSize!!.height.toFloat(), previewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale: Float = Math.max(
                viewHeight.toFloat() / previewSize!!.height,
                viewWidth.toFloat() / previewSize!!.width
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        } /*I added this else*/ else {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale: Float = Math.max(
                viewHeight.toFloat() / previewSize!!.height,
                viewWidth.toFloat() / previewSize!!.width
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate(0f, centerX, centerY)
        }
        textureView!!.setTransform(matrix)
    }

    private fun getNextChunkPath(path: String): String {
        chunkIndex++
        val chunkPath = "${path}/PART_${chunkIndex}.mp4"
        nextVideoAbsolutePath = chunkPath
        Log.d(LOG_TAG, "Chunk $chunkIndex video file path: $chunkPath")
        return chunkPath
    }

    private fun setupMediaRecorder(mediaRecorder: MediaRecorder){
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        outputFile = FileUtils.getOutputMediaFile(this, timeStamp)

        mediaRecorder.setVideoEncodingBitRate(800000)
        mediaRecorder.setOutputFile(getNextChunkPath(outputFile!!.path))
        mediaRecorder.setVideoFrameRate(Constants.VIDEO_FRAME_RATE)
        mediaRecorder.setVideoSize(640, 480)

        // set size according to orientation
//        val orientation = resources.configuration.orientation
//        videoSize?.let {
//            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                mediaRecorder.setVideoSize(it.width, it.height)
//            } else {
//                mediaRecorder.setVideoSize(it.height, it.width)
//            }
//        }

        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setOnInfoListener(this)
        mediaRecorder.setOnErrorListener(this)
        mediaRecorder.setMaxFileSize((1024 * 1024 * Constants.VIDEO_SIZE_MAX_MB))

        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        }
        when (sensorOrientation) {
            SENSOR_ORIENTATION_DEFAULT_DEGREES -> mediaRecorder.setOrientationHint(
                DEFAULT_ORIENTATIONS[display.rotation]
            )

            SENSOR_ORIENTATION_INVERSE_DEGREES -> mediaRecorder.setOrientationHint(
                INVERSE_ORIENTATIONS[display.rotation]
            )
        }
        mediaRecorder.prepare()
    }

    private fun startRecording() {
        if (cameraDevice == null || !textureView!!.isAvailable || previewSize == null) {
            Log.d(LOG_TAG, "Camera not ready")
            return
        }
        try {
            closePreview()
            setupMediaRecorder(mediaRecorder!!)
            val texture = checkNotNull(textureView!!.surfaceTexture)
            texture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
            previewBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            val surfaces: MutableList<Surface> = ArrayList()

            // Set up Surface for the camera preview
            val previewSurface = Surface(texture)
            surfaces.add(previewSurface)
            previewBuilder!!.addTarget(previewSurface)

            // Set up Surface for the MediaRecorder
            val recorderSurface = mediaRecorder!!.surface
            surfaces.add(recorderSurface)
            previewBuilder!!.addTarget(recorderSurface)
            previewBuilder!!.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(10, 10))

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            val sessionCallback: CameraCaptureSession.StateCallback = object: CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    updatePreview()
                    runOnUiThread {
                        recordingState = RecordingState.RECORDING
                        mediaRecorder!!.start()
                    }
//                    recordingState = RecordingState.RECORDING
                    // TODO Show recording state
//                    requireActivity().runOnUiThread { // UI
//                        mButtonVideo!!.setText(R.string.stop)
//                        mIsRecordingVideo = true
//
//                        // Start recording
//                        mMediaRecorderStarted = true
//                        mMediaRecorder!!.start()
//                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.d(LOG_TAG, "Failed to configure camera")
                    finish()
                }
            }
//            val config = SessionConfiguration(SESSION_REGULAR,
//                listOf(OutputConfiguration(previewSurface)),
//                backgroundExecutor!!, sessionCallback)
            cameraDevice!!.createCaptureSession(surfaces, sessionCallback, backgroundHandler)
        } catch (exception: CameraAccessException) {
            recordingState = RecordingState.IDLE
            exception.printStackTrace()
        } catch (exception: IOException) {
            recordingState = RecordingState.IDLE
            exception.printStackTrace()
        }
    }

    private fun stopRecording() {
        // Stop recording
        if (recordingState == RecordingState.RECORDING) {
            // TODO update UI state
            recordingState = RecordingState.IDLE
            mediaRecorder?.stop()
            mediaRecorder?.reset()
        }

        nextVideoAbsolutePath = null
        chunkIndex = 0
        Log.d(LOG_TAG, "Recording Stopped")
        startPreview()
    }

    private fun takeSnapshot() {

    }

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        openCamera(width, height)
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        configureTransformFillViewPort(width, height)
    }

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {

    }

    override fun onInfo(mediaRecorder: MediaRecorder?, what: Int, extra: Int) {
        Log.d(LOG_TAG, "mediaRecorder onInfo, what:$what  extra:$extra  recorderIndex: 1")
        when (what) {
            MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED -> return
            MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING -> {
                Log.d(LOG_TAG, "MEDIA_RECORDER_INFO_MAX_FILE_SIZE_APPROACHING")
                val nextFile = File(getNextChunkPath(outputFile!!.path))
                Log.d(LOG_TAG, "Next file: $nextFile")
                try {
                    mediaRecorder!!.setNextOutputFile(nextFile)
                    Log.d(LOG_TAG, "Set next file: $nextFile")
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.d(LOG_TAG, "Error setting next recorder")
                }
                return
            }

            MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED -> {
                Log.d(LOG_TAG, "MEDIA_RECORDER_INFO_MAX_FILE_SIZE_REACHED")
                return
            }
            MediaRecorder.MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED -> {
                Log.d(LOG_TAG, "MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED")
                return
            }
        }
    }

    override fun onError(mediaRecorder: MediaRecorder?, what: Int, extra: Int) {
        Log.d(LOG_TAG, "mediaRecorder onError, what:$what  extra:$extra")
    }

}

/**
 * Compares two `Size`s based on their areas.
 */
internal class CompareSizesByArea : Comparator<Size> {
    override fun compare(lhs: Size, rhs: Size): Int {
        // We cast here to ensure the multiplications won't overflow
        return java.lang.Long.signum(
            lhs.width.toLong() * lhs.height -
                    rhs.width.toLong() * rhs.height
        )
    }
}


/**
 * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
 * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
 *
 * @param choices The list of available sizes
 * @return The video size
 */
private fun chooseVideoSize(choices: Array<Size>): Size {
    for (size in choices) {
        Log.d(LOG_TAG, "Choosing video size for option: $size")
        if (size.width == size.height * 4 / 3 && size.width <= 1080) {
            return size
        }
    }
    Log.e(LOG_TAG, "Couldn't find any suitable video size")
    return choices[choices.size - 1]
}

/**
 * Given `choices` of `Size`s supported by a camera, chooses the smallest one whose
 * width and height are at least as large as the respective requested values, and whose aspect
 * ratio matches with the specified value.
 *
 * @param choices     The list of sizes that the camera supports for the intended output class
 * @param width       The minimum desired width
 * @param height      The minimum desired height
 * @param aspectRatio The aspect ratio
 * @return The optimal `Size`, or an arbitrary one if none were big enough
 */
private fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int, aspectRatio: Size?): Size {
    // Collect the supported resolutions that are at least as big as the preview Surface
    val bigEnough: MutableList<Size> = ArrayList()
    val w = aspectRatio!!.width
    val h = aspectRatio.height
    choices.forEach { option ->
        Log.d(LOG_TAG, "Choosing optimal size for option: $option")
        if (option.height == option.width * h / w && option.width >= width && option.height >= height)
            bigEnough.add(option)
    }

    // Pick the smallest of those, assuming we found any
    if (bigEnough.size > 0) {
        return Collections.min(bigEnough, CompareSizesByArea())
    } else {
        Log.e(LOG_TAG, "Couldn't find any suitable preview size")
        return choices[0]
    }
}

@Composable
fun CameraFeedView(textureViewConsumer: (AutoFitTextureView) -> Unit, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .background(Color.Black)
            .fillMaxSize(),
        factory = { context ->
            AutoFitTextureView(context).apply {
                // any modifications
                textureViewConsumer(this)
            }
        },
        update = {
            // any re-configurations
        }
    )
}

@Composable
fun CameraControlView(recordingState: RecordingState, onClickRecordButton : () -> Unit, modifier: Modifier = Modifier) {
    Box (
        modifier = modifier.padding(24.dp)
    ){
        OutlinedButton(
            onClick = onClickRecordButton,
            shape = CircleShape,
            border = BorderStroke(6.dp, White),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = White, containerColor =
            if (recordingState == RecordingState.RECORDING) Color.Red else Color.Black.copy(alpha = 0.2f)),
            modifier = Modifier.size(80.dp, 80.dp)
        ) {
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraHudView(
    millisInRecord: Long,
    onClickCloseButton: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            navigationIconContentColor = White,
            actionIconContentColor = White
        ),

        navigationIcon = {
            FilledIconButton(
                onClick = onClickCloseButton,
                colors = IconButtonDefaults.filledIconButtonColors(
                    contentColor = White,
                    containerColor = Color.Black.copy(
                        alpha = 0.3f
                    )
                ),
                modifier = Modifier.size(40.dp, 40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                )
            }
        },
        title = {
            Box(
                modifier = Modifier.wrapContentSize()
                    .background(color = Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
            ) {
                Text(
                    "00:00",
                    color = White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                        .padding(8.dp, 4.dp, 8.dp, 4.dp)
                )
            }
        },
        modifier = modifier.padding(start = 8.dp, end = 8.dp)
    )
}

@Preview
@Composable
fun CameraFeedViewPreview(){
    Surface(color = Color.DarkGray) {
        MediaChunkUploadTheme {
            Box() {
                CameraFeedView(textureViewConsumer = {})
                CameraHudView(
                    millisInRecord = 0,
                    onClickCloseButton = {},
                    modifier = Modifier.align(Alignment.TopCenter))
                CameraControlView(recordingState = RecordingState.IDLE, onClickRecordButton = {}, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}