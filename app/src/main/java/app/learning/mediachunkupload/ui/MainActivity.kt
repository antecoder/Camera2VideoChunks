package app.learning.mediachunkupload.ui

import android.content.Intent
import android.graphics.Paint.Align
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.learning.mediachunkupload.R
import app.learning.mediachunkupload.ui.theme.MediaChunkUploadTheme
import app.learning.mediachunkupload.ui.ui.record.Recording
import app.learning.mediachunkupload.ui.ui.record.RecordingActivity

class MainActivity : ComponentActivity() {

    val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediaChunkUploadTheme {
                Scaffold(
                    topBar = {
                        MainAppBar()
                    },
                    floatingActionButton = {
                        StartRecordingButton(onClickListener = {
                            startRecordingActivity()
                        })
                    },
                    modifier = Modifier.fillMaxSize())
                { innerPadding ->
                    val recordings = viewModel.recordings.observeAsState().value
                    MainAppContent(recordings, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun startRecordingActivity() {
        startActivity(Intent(this, RecordingActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshFileList(this)
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppBar(modifier: Modifier = Modifier) {
    TopAppBar(
        title = {
            Text(
                text = "Media List",
                modifier = modifier
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
            ),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun MainAppBarPreview() {
    MediaChunkUploadTheme {
        MainAppBar()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(recordings: List<Recording>?, modifier: Modifier = Modifier) {
    if (recordings.isNullOrEmpty()) {
        Box (
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ){
            Text(
                text = stringResource(R.string.no_recordings_found),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.wrapContentSize()
            )
        }
    } else {
        LazyColumn (
            modifier = modifier.fillMaxSize()
        ) {
            items(items = recordings) { recording ->
                RecordingItemView(recording) {}
                Spacer(
                    modifier = Modifier.background(Color.DarkGray)
                        .height(2.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppContentEmptyPreview(modifier: Modifier = Modifier) {
    MediaChunkUploadTheme {
        MainAppContent(listOf(),  modifier)
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppContentPreview(modifier: Modifier = Modifier) {
    MediaChunkUploadTheme {
        val sampleRecording = Recording("Sample Recording", "path/to/folder", listOf())
        val items = listOf(sampleRecording, sampleRecording, sampleRecording)
        MainAppContent(items,  modifier)
    }
}

@Composable
fun StartRecordingButton(onClickListener: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClickListener,
        containerColor = MaterialTheme.colorScheme.tertiary,
        contentColor = Color.White,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.Videocam,
            contentDescription = "Play icon"
        )
    }
}

@Composable
@Preview
fun StartRecordingButtonPreview() {
    MediaChunkUploadTheme {
        StartRecordingButton({})
    }
}

@Composable
fun RecordingItemView(recording: Recording, onRecordingClickListener: (Recording) -> Unit) {
    Card (
        onClick = {
            onRecordingClickListener(recording)
        },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
            .wrapContentHeight(align = Alignment.CenterVertically)
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = recording.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${recording.recordCount} ${if (recording.recordCount == 1) stringResource(R.string.chunk) else stringResource(R.string.chunks)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = {}
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play icon"
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecordingItemViewPreview(modifier: Modifier = Modifier) {
    val sampleRecording = Recording("Sample Recording", "path/to/folder", listOf())
    MediaChunkUploadTheme {
        RecordingItemView(sampleRecording) {}
    }
}


