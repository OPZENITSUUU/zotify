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
                .onFailure { status = it.message ?: "Could not import this skin." }
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
                    Text("Appearance skins", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Import, preview and switch custom looks",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "Mochy Look packs are accepted. Zotify applies accent colour, pure-black mode and cover corner radius. Custom player layouts, fonts, shaders, pages and scripts are not executed.",
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
                    Text("From file")
                }
                OutlinedButton(
                    onClick = { showLinkDialog = true },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("From link")
                }
            }

            if (busy) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Checking skin manifest…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            status?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Installed skins", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${packs.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            if (packs.isEmpty()) {
                Spacer(Modifier.height(22.dp))
                Text("No custom skins yet.", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Add a Mochy-compatible .zip, manifest.json link or public GitHub skin repository.",
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
                                            "ACTIVE",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                                Text(
                                    "v${pack.version}" + if (pack.author.isNotBlank()) " · ${pack.author}" else "",
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
                            if (pack.supportedSettings.isEmpty()) "No settings supported by this Zotify build"
                            else "Applies: ${pack.supportedSettings.joinToString()}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        if (pack.unsupportedSettings.isNotEmpty()) {
                            Text(
                                "Not applied: ${pack.unsupportedSettings.joinToString()}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (active) {
                                OutlinedButton(onClick = {
                                    runCatching { AppearanceSkinLibrary.disable(context) }
                                        .onSuccess { refresh(); status = "Skin disabled. Your previous appearance was restored." }
                                        .onFailure { status = it.message ?: "Could not disable skin." }
                                }) { Text("Turn off") }
                            } else {
                                Button(
                                    enabled = pack.canApply,
                                    onClick = {
                                        runCatching { AppearanceSkinLibrary.enable(context, pack.id) }
                                            .onSuccess { refresh(); status = "${pack.name} applied." }
                                            .onFailure { status = it.message ?: "Could not apply skin." }
                                    },
                                ) { Text("Apply") }
                            }
                            TextButton(onClick = {
                                clipboard.setText(AnnotatedString(pack.rawManifest))
                                status = "Manifest copied."
                            }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copy")
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = {
                                AppearanceSkinLibrary.remove(context, pack.id)
                                refresh()
                                status = "Skin removed."
                            }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Remove", color = MaterialTheme.colorScheme.error)
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
            title = { Text("Add skin from link") },
            text = {
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("HTTPS manifest, ZIP or GitHub URL") },
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
                ) { Text("Check") }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) { Text("Cancel") }
            },
        )
    }

    pending?.let { pack ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Review skin") },
            text = {
                Column {
                    Text(pack.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Version ${pack.version}" + if (pack.author.isNotBlank()) " · ${pack.author}" else "")
                    if (pack.description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(pack.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (pack.supportedSettings.isEmpty()) "This package has no appearance settings supported by Zotify."
                        else "Supported: ${pack.supportedSettings.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (pack.unsupportedSettings.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Ignored for safety/compatibility: ${pack.unsupportedSettings.joinToString()}",
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
                        status = "Skin installed. Apply it from the list when ready."
                    },
                ) { Text("Install") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Cancel") }
            },
        )
    }
}
