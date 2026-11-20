package com.music.bitchord.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.zotifymusic.R
import com.music.bitchord.data.settings.AppearanceSkinLibrary
import com.music.bitchord.data.settings.AppearanceSkinPack
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSkinLibrarySheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var packs by remember { mutableStateOf(AppearanceSkinLibrary.list(context)) }
    var activeId by remember { mutableStateOf(AppearanceSkinLibrary.activeId(context)) }
    var pending by remember { mutableStateOf<AppearanceSkinPack?>(null) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        packs = AppearanceSkinLibrary.list(context)
        activeId = AppearanceSkinLibrary.activeId(context)
    }

    fun importPack(block: suspend () -> AppearanceSkinPack) {
        if (busy) return
        busy = true
        status = null
        scope.launch {
            val result = runCatching { block() }
            busy = false
            result.onSuccess { pending = it }
                .onFailure { status = context.getString(R.string.appearance_skin_import_failed) }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importPack { AppearanceSkinLibrary.importFile(context, uri) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 22.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                ) {
                    Icon(
                        Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(11.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(context.getString(R.string.appearance_skins_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        context.getString(R.string.appearance_skins_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                context.getString(R.string.appearance_skins_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        filePicker.launch(
                            arrayOf(
                                "application/zip",
                                "application/x-zip-compressed",
                                "application/json",
                                "text/plain",
                                "application/octet-stream",
                            ),
                        )
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(context.getString(R.string.appearance_skins_from_file))
                }
                OutlinedButton(
                    onClick = { showLinkDialog = true },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(context.getString(R.string.appearance_skins_from_link))
                }
            }

            if (busy) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(context.getString(R.string.appearance_skins_checking), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            status?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(context.getString(R.string.appearance_skins_installed), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(context.getString(R.string.appearance_skins_count, packs.size), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            if (packs.isEmpty()) {
                Spacer(Modifier.height(22.dp))
                Text(context.getString(R.string.appearance_skins_empty_title), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    context.getString(R.string.appearance_skins_empty_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(16.dp))
            } else {
                packs.forEach { pack ->
                    val active = activeId == pack.id
                    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Icon(
                                    if (active) Icons.Rounded.CheckCircle else Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        pack.name,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (active) {
                                        Spacer(Modifier.width(7.dp))
                                        Text(
                                            context.getString(R.string.appearance_skins_active),
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                                Text(
                                    if (pack.author.isNotBlank()) context.getString(R.string.appearance_skins_version_author, pack.version, pack.author) else context.getString(R.string.appearance_skins_version, pack.version),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        if (pack.description.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(pack.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (pack.supportedSettings.isEmpty()) context.getString(R.string.appearance_skins_no_settings_applied)
                            else context.getString(R.string.appearance_skins_applies, pack.supportedSettings.joinToString()),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        if (pack.unsupportedSettings.isNotEmpty()) {
                            Text(
                                context.getString(R.string.appearance_skins_not_applied, pack.unsupportedSettings.joinToString()),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (active) {
                                OutlinedButton(onClick = {
                                    runCatching { AppearanceSkinLibrary.disable(context) }
                                        .onSuccess { refresh(); status = context.getString(R.string.appearance_skins_disabled) }
                                        .onFailure { status = context.getString(R.string.appearance_skins_disable_failed) }
                                }) { Text(context.getString(R.string.appearance_skins_turn_off)) }
                            } else {
                                Button(
                                    enabled = pack.canApply,
                                    onClick = {
                                        runCatching { AppearanceSkinLibrary.enable(context, pack.id) }
                                            .onSuccess { refresh(); status = context.getString(R.string.appearance_skins_applied, pack.name) }
                                            .onFailure { status = context.getString(R.string.appearance_skins_apply_failed) }
                                    },
                                ) { Text(context.getString(R.string.appearance_skins_apply)) }
                            }
                            TextButton(onClick = {
                                clipboard.setText(AnnotatedString(pack.rawManifest))
                                status = context.getString(R.string.appearance_skins_manifest_copied)
                            }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(context.getString(R.string.appearance_skins_copy))
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = {
                                AppearanceSkinLibrary.remove(context, pack.id)
                                refresh()
                                status = context.getString(R.string.appearance_skins_removed)
                            }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(context.getString(R.string.appearance_skins_remove), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                }
            }
        }
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text(context.getString(R.string.appearance_skins_add_from_link)) },
            text = {
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text(context.getString(R.string.appearance_skins_link_label)) },
                    placeholder = { Text("https://…") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = link.isNotBlank() && !busy,
                    onClick = {
                        val value = link.trim()
                        showLinkDialog = false
                        link = ""
                        importPack { AppearanceSkinLibrary.importUrl(context, value) }
                    },
                ) { Text(context.getString(R.string.appearance_skins_check)) }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) { Text(context.getString(R.string.appearance_skins_cancel)) }
            },
        )
    }

    pending?.let { pack ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(context.getString(R.string.appearance_skins_review)) },
            text = {
                Column {
                    Text(pack.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(if (pack.author.isNotBlank()) context.getString(R.string.appearance_skins_version_author, pack.version, pack.author) else context.getString(R.string.appearance_skins_version, pack.version))
                    if (pack.description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(pack.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (pack.supportedSettings.isEmpty()) context.getString(R.string.appearance_skins_no_settings_supported)
                        else context.getString(R.string.appearance_skins_supported, pack.supportedSettings.joinToString()),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (pack.unsupportedSettings.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            context.getString(R.string.appearance_skins_ignored, pack.unsupportedSettings.joinToString()),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pack.canApply,
                    onClick = {
                        AppearanceSkinLibrary.install(context, pack)
                        refresh()
                        pending = null
                        status = context.getString(R.string.appearance_skins_installed_success)
                    },
                ) { Text(context.getString(R.string.appearance_skins_install)) }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text(context.getString(R.string.appearance_skins_cancel)) }
            },
        )
    }
}
