package app.learning.mediachunkupload.ui.main.sessions

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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.learning.mediachunkupload.R
import app.learning.mediachunkupload.ui.main.record.Session
import app.learning.mediachunkupload.ui.theme.MediaChunkUploadTheme
import kotlin.coroutines.EmptyCoroutineContext


/**
 * Composable displaying a list of recorded [Session] items.
 */
@Composable
fun SessionListScreen(viewModel: SessionsViewModel?, onSelectSession: (Session) -> Unit, onRecordButtonClick: () -> Unit, modifier: Modifier = Modifier) {
    val sessions = viewModel?.recordings?.collectAsState(EmptyCoroutineContext)?.value
    MediaChunkUploadTheme {
        Scaffold (
            topBar = {
                SessionListAppBar()
            },
            floatingActionButton = {
                StartRecordingButton(onClickListener = onRecordButtonClick)
            }
        ) {
            SessionListContent(sessions, onSelectSession, modifier.padding(it))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListAppBar(modifier: Modifier = Modifier) {
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

@Composable
fun SessionListContent(sessions: List<Session>?, onSelectSession: (Session) -> Unit, modifier: Modifier = Modifier) {
    if (sessions.isNullOrEmpty()) {
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
            items(items = sessions) { session ->
                SessionItemView(session, onSelectSession)
                Spacer(
                    modifier = Modifier.background(Color.DarkGray)
                        .height(2.dp)
                )
            }
        }
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
fun SessionItemView(session: Session, onSessionClickListener: (Session) -> Unit) {
    Card (
        onClick = {
            onSessionClickListener(session)
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
                    text = session.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${session.recordCount} ${if (session.recordCount == 1) stringResource(
                        R.string.chunk) else stringResource(R.string.chunks)
                    }",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = {
                    onSessionClickListener(session)
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play icon"
                )
            }
        }
    }
}


// PREVIEWS
@Composable
@Preview(showBackground = true)
fun SessionListScreenPreview() {
    MediaChunkUploadTheme {
        SessionListScreen(null, {}, {})
    }
}
