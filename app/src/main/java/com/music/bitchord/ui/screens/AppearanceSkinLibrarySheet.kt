package com.music.bitchord.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.AppearanceLookOverrides
import com.music.bitchord.data.settings.AppearanceSkinLibrary
import com.music.bitchord.data.settings.AppearanceSkinState
import com.music.bitchord.data.settings.UiSkin

private data class BuiltInAppearancePreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val uiSkin: UiSkin,
    val overrides: AppearanceLookOverrides,
    val background: Color,
    val surface: Color,
    val accent: Color,
    val foreground: Color,
    val muted: Color,
)

private val BuiltInAppearancePresets = listOf(
    BuiltInAppearancePreset(
        id = "zotify",
        name = "Zotify Original",
        subtitle = "The default Zotify Music look",
        description = "Keep the original interface and your selected light or dark theme.",
        uiSkin = UiSkin.ZOTIFY,
        overrides = AppearanceLookOverrides(),
        background = Color(0xFF101114),
        surface = Color(0xFF202228),
        accent = Color(0xFFF4F4F5),
        foreground = Color.White,
        muted = Color(0xFF9A9CA4),
    ),
    BuiltInAppearancePreset(
        id = "mochy",
        name = "Mochy Music",
        subtitle = "Mochy-inspired dark interface",
        description = "A bundled monochrome look with dark surfaces, softer typography and rounded artwork. It is already inside the app.",
        uiSkin = UiSkin.MOCHY,
        overrides = AppearanceLookOverrides(),
        background = Color.Black,
        surface = Color(0xFF080808),
        accent = Color(0xFFE0E0E0),
        foreground = Color.White,
        muted = Color(0xFF9A9A9A),
    ),
    BuiltInAppearancePreset(
        id = "amoled-cyan",
        name = "AMOLED Cyan",
        subtitle = "Pure black with a cool accent",
        description = "A high-contrast black interface with cyan highlights and rounded artwork.",
        uiSkin = UiSkin.ZOTIFY,
        overrides = AppearanceLookOverrides(
            accentArgb = Color(0xFF5DD6FF).toArgb(),
            pureBlack = true,
            thumbnailCornerRadius = 22,
        ),
        background = Color.Black,
        surface = Color(0xFF090E12),
        accent = Color(0xFF5DD6FF),
        foreground = Color.White,
        muted = Color(0xFF8AA1AC),
    ),
    BuiltInAppearancePreset(
        id = "midnight-violet",
        name = "Midnight Violet",
        subtitle = "Dark surfaces with violet highlights",
        description = "A calm violet accent across the existing Zotify interface.",
        uiSkin = UiSkin.ZOTIFY,
        overrides = AppearanceLookOverrides(
            accentArgb = Color(0xFFB39DFF).toArgb(),
            pureBlack = true,
            thumbnailCornerRadius = 24,
        ),
        background = Color(0xFF050408),
        surface = Color(0xFF17121F),
        accent = Color(0xFFB39DFF),
        foreground = Color.White,
        muted = Color(0xFFA69BB8),
    ),
    BuiltInAppearancePreset(
        id = "emerald-night",
        name = "Emerald Night",
        subtitle = "Dark green accent",
        description = "A deep night palette with a clear emerald highlight.",
        uiSkin = UiSkin.ZOTIFY,
        overrides = AppearanceLookOverrides(
            accentArgb = Color(0xFF63D6A5).toArgb(),
            pureBlack = true,
            thumbnailCornerRadius = 20,
        ),
        background = Color(0xFF030907),
        surface = Color(0xFF102019),
        accent = Color(0xFF63D6A5),
        foreground = Color.White,
        muted = Color(0xFF93B6A6),
    ),
    BuiltInAppearancePreset(
        id = "rose-night",
        name = "Rose Night",
        subtitle = "Dark with a warm rose accent",
        description = "A softer rose highlight on the familiar Zotify interface.",
        uiSkin = UiSkin.ZOTIFY,
        overrides = AppearanceLookOverrides(
            accentArgb = Color(0xFFFF829D).toArgb(),
            pureBlack = true,
            thumbnailCornerRadius = 24,
        ),
        background = Color(0xFF0C0508),
        surface = Color(0xFF211018),
        accent = Color(0xFFFF829D),
        foreground = Color.White,
        muted = Color(0xFFC5A0AA),
    ),
)

@Composable
fun AppearanceSkinLibrarySheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uiSkin by AppSettings.uiSkin.collectAsStateWithLifecycle()
    val accentArgb by AppearanceSkinState.accentArgb.collectAsStateWithLifecycle()
    val pureBlack by AppearanceSkinState.pureBlack.collectAsStateWithLifecycle()
    val cornerRadius by AppearanceSkinState.thumbnailCornerRadius.collectAsStateWithLifecycle()
    var status by remember { mutableStateOf<String?>(null) }
    var applyingId by remember { mutableStateOf<String?>(null) }

    fun isSelected(preset: BuiltInAppearancePreset): Boolean =
        uiSkin == preset.uiSkin &&
            accentArgb == preset.overrides.accentArgb &&
            pureBlack == preset.overrides.pureBlack &&
            cornerRadius == preset.overrides.thumbnailCornerRadius

    fun applyPreset(preset: BuiltInAppearancePreset) {
        if (applyingId != null) return
        applyingId = preset.id
        status = null
        runCatching {
            // Retire a previously imported pack first so its overrides cannot
            // silently sit on top of a built-in theme.
            if (AppearanceSkinLibrary.activeId(context) != null) {
                AppearanceSkinLibrary.disable(context)
            }
            AppearanceSkinState.apply(preset.overrides)
            AppSettings.setUiSkin(preset.uiSkin)
        }.onSuccess {
            status = "${preset.name} applied."
        }.onFailure {
            status = it.message ?: "Could not apply this appearance."
        }
        applyingId = null
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
                .padding(top = 12.dp, bottom = 28.dp),
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
                    Text(
                        "Appearance",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Built-in themes and interface styles",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Every look below is bundled inside Zotify. No ZIP, JSON, URL or external skin file is needed.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(14.dp),
                )
            }

            Spacer(Modifier.height(22.dp))
            Text("Built-in looks", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Preview a style and apply it with one tap.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))

            BuiltInAppearancePresets.forEach { preset ->
                val selected = isSelected(preset)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    ),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        SkinPreview(preset)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(preset.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text(
                                    preset.subtitle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (selected) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            preset.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(12.dp))
                        if (selected) {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Currently applied")
                            }
                        } else {
                            Button(
                                onClick = { applyPreset(preset) },
                                enabled = applyingId == null,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    if (applyingId == preset.id) "Applying…" else "Apply",
                                )
                            }
                        }
                    }
                }
            }

            status?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            )
            Text(
                "Mochy Music style is included locally in the app. Selecting it changes the native Zotify interface immediately and works offline.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SkinPreview(preset: BuiltInAppearancePreset) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(116.dp),
        color = preset.background,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, preset.accent.copy(alpha = 0.24f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (preset.id == "mochy") "MOCHY MUSIC" else "ZOTIFY MUSIC",
                    color = preset.foreground,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(preset.accent),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                repeat(3) { index ->
                    Surface(
                        modifier = Modifier.size(42.dp),
                        color = if (index == 1) preset.accent.copy(alpha = 0.26f) else preset.surface,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(if (index == 1) 14.dp else 11.dp)
                                    .clip(CircleShape)
                                    .background(preset.accent.copy(alpha = if (index == 1) 0.95f else 0.55f)),
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Now playing",
                        color = preset.foreground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Your music, your style",
                        color = preset.muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(preset.surface),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(preset.accent),
                )
            }
        }
    }
}
