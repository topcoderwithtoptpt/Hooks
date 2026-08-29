package com.lo.michook.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lo.michook.ui.theme.AmberWarning
import com.lo.michook.ui.theme.CyanPrimary
import com.lo.michook.ui.theme.DarkBackground
import com.lo.michook.ui.theme.DarkSurface
import com.lo.michook.ui.theme.DarkSurfaceCard
import com.lo.michook.ui.theme.DarkSurfaceElevated
import com.lo.michook.ui.theme.EmeraldSuccess
import com.lo.michook.ui.theme.RoseError
import com.lo.michook.ui.theme.TextMuted
import com.lo.michook.ui.theme.TextPrimary
import com.lo.michook.ui.theme.TextSecondary
import com.lo.michook.ui.theme.VioletAccent
import com.lo.michook.ui.viewmodel.MicHookUiState
import com.lo.michook.ui.viewmodel.MicHookViewModel

@Composable
fun HooksOverviewScreen(
    state: MicHookUiState,
    viewModel: MicHookViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HOOK ARCHITECTURE & PIPELINE",
                        color = CyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Comprehensive 3-tier interception covering all Java & Native microphone paths.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // LSPosed Scope Information Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (state.isLsposedActive) EmeraldSuccess.copy(alpha = 0.3f) else AmberWarning.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (state.isLsposedActive) EmeraldSuccess else AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "LSPOSED SCOPE CONTROL",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    if (state.isLsposedActive) EmeraldSuccess.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (state.isLsposedActive) "LSPOSED ACTIVE" else "DISABLED",
                                color = if (state.isLsposedActive) EmeraldSuccess else AmberWarning,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "In the modern LSPosed Framework, target application scope is configured entirely within the LSPosed Manager. There is no in-app scope list required.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "To manage target apps:",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "• Open LSPosed Manager → Tap Modules → Select MicHook\n• Check/uncheck target apps in the Scope list (e.g. WhatsApp, Discord, Games)\n• Force stop or restart target apps to apply the audio hook",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Layer 1: AudioRecord
        item {
            HookLayerCard(
                title = "Layer 1: Java AudioRecord",
                badge = "8 Overloads Intercepted",
                accentColor = CyanPrimary,
                icon = Icons.Default.Code,
                description = "Hooks all read() methods before execution, sets param.result to skip real mic capture, and fills buffer directly with PCM audio.",
                details = listOf(
                    "read(byte[], int, int) → fillBuffer",
                    "read(byte[], int, int, int) → API 23+ readMode",
                    "read(short[], int, int) → fillShortBuffer",
                    "read(short[], int, int, int) → readMode variant",
                    "read(ByteBuffer, int) → fillByteBuffer (NDK wrapper)",
                    "read(ByteBuffer, int, int) → Direct buffer readMode",
                    "read(float[], int, int, int) → PCM16 to float conversion",
                    "startRecording() → state validation & log tracking"
                )
            )
        }

        // Layer 2: MediaRecorder
        item {
            HookLayerCard(
                title = "Layer 2: Java MediaRecorder",
                badge = "Source Redirection",
                accentColor = VioletAccent,
                icon = Icons.Default.Radio,
                description = "MediaRecorder records directly to disk/fd without exposing PCM buffers. We hook setAudioSource and redirect to REMOTE_SUBMIX loopback.",
                details = listOf(
                    "setAudioSource(int) → Redirected to REMOTE_SUBMIX (8)",
                    "prepare() → Intercepted & logcat registered",
                    "start() → Active recording stream logged"
                )
            )
        }

        // Layer 3: Native ShadowHook
        item {
            HookLayerCard(
                title = "Layer 3: Native PLT & C++ Hooks",
                badge = "ShadowHook Engine",
                accentColor = EmeraldSuccess,
                icon = Icons.Default.Memory,
                description = "PLT and inline hooks into native shared libraries used by VoIP engines, game engines, and low-latency audio frameworks.",
                details = listOf(
                    "libaaudio.so :: AAudioStream_read → inject_pcm()",
                    "libaaudio.so :: AAudioStream_write → logging stream",
                    "libaudioclient.so :: AudioRecord::read (mangled C++)",
                    "libOpenSLES.so :: Buffer queue enqueue handling"
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HookLayerCard(
    title: String,
    badge: String,
    accentColor: Color,
    icon: ImageVector,
    description: String,
    details: List<String>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        modifier = Modifier.fillMaxWidth().border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badge,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                details.forEach { detail ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Text(
                            text = detail,
                            color = TextPrimary.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
