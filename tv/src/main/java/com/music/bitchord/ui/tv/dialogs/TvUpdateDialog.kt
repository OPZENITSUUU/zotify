package com.music.bitchord.ui.tv.dialogs

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.bitchord.data.AppUpdateChecker
import com.music.bitchord.data.AppUpdateChecker.DownloadState
import com.music.bitchord.ui.tv.auth.TvQrCodeView
import com.music.bitchord.ui.tv.components.TvDialogButton
import com.music.bitchord.ui.tv.theme.TvGlass
import com.music.bitchord.ui.tv.theme.TvType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Outlives the alert on purpose: "Hide" closes it, and the download has to keep
 * going behind it. [AppUpdateChecker] holds the one download there can be.
 */
private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

/**
 * A new Zotify Music TV release is out: download its APK and hand it to the system
 * installer without leaving the app, as the phone's update sheet does.
 *
 * Closing the alert mid-download only hides it — the download keeps going, and
 * Settings' "Software Update" row reopens the alert onto wherever it got to.
 * A release with no TV APK on it (or a download that can't be installed) has
 * nowhere on a TV to send the user, as most have no browser, so the releases
 * page is offered as a code to scan with a phone instead.
 */
@Composable
fun TvUpdateDialog(
    update: AppUpdateChecker.UpdateInfo,
    onDismiss: () -> Unit,
    onNotNow: () -> Unit = onDismiss,
) {
    val state by AppUpdateChecker.download.collectAsState()
    val notes = remember(update.notes) { update.notes?.let(::plainNotes) }
    val context = LocalContext.current.applicationContext
    val permanentlyDismissOptionalUpdate = {
        if (!update.mandatory) {
            AppUpdateChecker.dismiss(context, update.version)
            onNotNow()
        }
    }

    TvUpdateAlert(
        title = if (update.mandatory) "Required Zotify Music update" else "New Update Available",
        message = buildString {
            append("Current: ").append(com.music.bitchord.BuildConfig.VERSION_NAME)
                .append(" · Available: ").append(update.version)
            when {
                update.apkUrl == null -> append("\nScan the code with your phone to get this update from GitHub.")
                state is DownloadState.Ready -> append("\nDownloaded. Select Install Now to update.")
                state is DownloadState.Failed -> append("\n").append((state as DownloadState.Failed).message)
                update.mandatory -> append("\nThis release sets a minimum supported app version.")
            }
        },
        width = 512.dp,
        // Back/outside is only a temporary hide. Mandatory alerts ignore Back.
        onDismissRequest = { if (!update.mandatory) onDismiss() },
    ) {
        if (update.apkUrl == null) {
            TvQrCodeView(content = update.releaseUrl, size = 147.dp)
            Spacer(modifier = Modifier.height(21.dp))
            TvDialogButton(
                text = "OK",
                onClick = { if (!update.mandatory) onDismiss() },
                initialFocus = true,
            )
        } else {
            TvUpdateControls(state, notes, update.mandatory, onDismiss, permanentlyDismissOptionalUpdate)
        }
    }
}

/** Same centered TV alert treatment as [com.music.bitchord.ui.tv.components.TvDialog],
 * but update alerts allow outside-tap dismissal as well as Back. */
@Composable
private fun TvUpdateAlert(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    width: androidx.compose.ui.unit.Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(220),
        label = "tvUpdateDialogIn",
    )
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress }
                .background(Color(0xF2111113)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(width)
                    .graphicsLayer {
                        val scale = 0.96f + 0.04f * progress
                        scaleX = scale
                        scaleY = scale
                    }
                    .padding(vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = TvType.Title,
                    color = TvGlass.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                if (message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = TvType.Callout,
                        color = TvGlass.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.height(21.dp))
                content()
            }
        }
    }
}

@Composable
private fun TvUpdateControls(
    state: DownloadState,
    notes: String?,
    mandatory: Boolean,
    onHide: () -> Unit,
    onNotNow: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    if (!notes.isNullOrBlank() && state is DownloadState.Idle) {
        Text(
            text = notes,
            style = TvType.Callout,
            color = TvGlass.TextSecondary,
            textAlign = TextAlign.Start,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(bottom = 21.dp),
        )
    }

    // Keyed on the state's kind, so each new set of buttons comes in with
    // focus on its first one rather than leaving focus on a button that
    // has just gone away.
    key(state::class) {
        when (val s = state) {
            DownloadState.Idle -> {
                TvDialogButton(
                    text = "Download and Install",
                    onClick = { downloadScope.launch { AppUpdateChecker.downloadApk(context) } },
                    initialFocus = true,
                )
                if (!mandatory) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TvDialogButton(text = "Not Now", onClick = onNotNow)
                }
            }

            is DownloadState.Downloading -> {
                TvDownloadProgress(fraction = s.fraction ?: 0f)
                Spacer(modifier = Modifier.height(21.dp))
                if (!mandatory) {
                    TvDialogButton(text = "Hide", onClick = onHide, initialFocus = true)
                }
                Spacer(modifier = Modifier.height(6.dp))
                TvDialogButton(
                    text = "Cancel Download",
                    destructive = true,
                    onClick = AppUpdateChecker::cancelDownload,
                    initialFocus = mandatory,
                )
            }

            is DownloadState.Ready -> {
                TvDialogButton(
                    text = "Install Now",
                    onClick = {
                        AppUpdateChecker.installerIntent(context, s.file)
                            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ?.let(context::startActivity)
                    },
                    initialFocus = true,
                )
                if (!mandatory) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TvDialogButton(text = "Not Now", onClick = onNotNow)
                }
            }

            is DownloadState.Failed -> {
                TvDialogButton(
                    text = "Try Again",
                    onClick = { downloadScope.launch { AppUpdateChecker.downloadApk(context) } },
                    initialFocus = true,
                )
                if (!mandatory) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TvDialogButton(
                        text = "Not Now",
                        onClick = {
                            AppUpdateChecker.resetDownload()
                            onNotNow()
                        },
                    )
                }
            }
        }
    }
}

/** A thin rounded bar and the percentage under it. The fill is drawn, not laid out. */
@Composable
private fun TvDownloadProgress(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .drawBehind {
                drawRect(TvGlass.FillStrong)
                drawRoundRect(
                    color = TvGlass.TextPrimary,
                    size = Size(size.width * fraction.coerceIn(0f, 1f), size.height),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            },
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "Downloading… ${(fraction * 100).toInt()}%",
        style = TvType.Caption,
        color = TvGlass.TextSecondary,
    )
}

/**
 * The release body is GitHub Markdown; an alert shows it as plain lines —
 * headings and emphasis markers dropped, list items as bullets, links as their
 * text.
 */
private fun plainNotes(markdown: String): String =
    markdown.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("<!--") && !it.startsWith("![") }
        .map { line ->
            line.replace(Regex("""\[([^\]]+)]\([^)]*\)"""), "$1")
                .replace(Regex("""^#+\s*"""), "")
                .replace(Regex("""^[-*+]\s+"""), "• ")
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
        }
        .joinToString("\n")
