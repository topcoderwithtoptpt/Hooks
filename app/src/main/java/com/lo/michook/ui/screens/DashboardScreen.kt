package com.lo.michook.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lo.michook.ui.components.MetricTile
import com.lo.michook.ui.components.StatusBadge
import com.lo.michook.ui.components.WaveformVisualizer
import com.lo.michook.ui.theme.AmberWarning
import com.lo.michook.ui.theme.CyanPrimary
import com.lo.michook.ui.theme.CyanSecondary
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
import java.io.File

@Composable
fun DashboardScreen(
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
            // Real-time LSPosed Plugin Status Check Card
            LSPosedRealtimeStatusCard(state = state, viewModel = viewModel)
        }

        item {
            // Hero Status Card
            HeroStatusCard(state = state, viewModel = viewModel)
        }

        item {
            // Live Waveform Visualizer
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
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "REAL-TIME PCM STREAM",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = if (state.isInjectorRunning || state.isTestRecordingActive) "ACTIVE (44.1 kHz)" else "STANDBY",
                    color = if (state.isInjectorRunning || state.isTestRecordingActive) EmeraldSuccess else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            WaveformVisualizer(
                samples = state.waveformSamples,
                isActive = state.isInjectorRunning || state.isTestRecordingActive
            )
        }

        item {
            // Telemetry Grid
            Text(
                text = "INJECTION TELEMETRY",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "Injected Data",
                    value = formatBytes(state.injectedBytes),
                    icon = Icons.Default.Audiotrack,
                    accentColor = CyanPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Audio Loops",
                    value = "#${state.loopCount}",
                    icon = Icons.Default.Refresh,
                    accentColor = VioletAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "Queue Buffer",
                    value = "${state.queueCurrentSize} / ${state.queueCapacity}",
                    icon = Icons.Default.Speed,
                    accentColor = if (state.queueCurrentSize > 0) EmeraldSuccess else AmberWarning,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Frames Served",
                    value = "${state.injectedFrames}",
                    icon = Icons.Default.GraphicEq,
                    accentColor = CyanSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            // Active Audio File Card
            ActiveSourceCard(state = state, viewModel = viewModel)
        }

        item {
            // Live Hook Test Bench
            LiveTesterCard(state = state, viewModel = viewModel)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroStatusCard(
    state: MicHookUiState,
    viewModel: MicHookViewModel
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            DarkSurfaceElevated,
            DarkSurfaceCard
        )
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MicHook System",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "LSposed Universal Mic Interception",
                        color = CyanPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                StatusBadge(
                    label = if (state.isReady) "READY" else if (state.isInjectorRunning) "DECODING" else "IDLE",
                    isActive = state.isReady || state.isInjectorRunning
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (state.isInjectorRunning) CyanPrimary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isInjectorRunning) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (state.isInjectorRunning) CyanPrimary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (state.isInjectorRunning) "Audio Injection Active" else "Audio Injection Paused",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (state.isInjectorRunning) "Replacing mic with MP3 PCM" else "Standby / Pass-through",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = state.isInjectorRunning,
                    onCheckedChange = { if (it) viewModel.startInjector() else viewModel.stopInjector() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = CyanPrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.testTag("master_injector_switch")
                )
            }
        }
    }
}

@Composable
private fun ActiveSourceCard(
    state: MicHookUiState,
    viewModel: MicHookViewModel
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = VioletAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ACTIVE AUDIO SOURCE",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (state.fileExists) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatBytes(state.fileSizeBytes),
                            color = EmeraldSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Synthetic Carrier Mode",
                            color = AmberWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = state.audioPath,
                    color = if (state.fileExists) TextPrimary else TextSecondary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.generateSampleAudioTone() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("generate_test_audio_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Test Tone", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.toggleMute() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isMuted) RoseError.copy(alpha = 0.2f) else DarkSurfaceCard,
                        contentColor = if (state.isMuted) RoseError else TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("mute_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (state.isMuted) Icons.Default.MicOff else Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (state.isMuted) "MUTED" else "MUTE", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun LiveTesterCard(
    state: MicHookUiState,
    viewModel: MicHookViewModel
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VioletAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = VioletAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "LIVE HOOK SIMULATION BENCH",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (state.isTestRecordingActive) {
                    Text(
                        text = "${formatBytes(state.testRecordBytesRead)} read",
                        color = CyanPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                text = "Runs an in-app AudioRecord read loop invoking AudioInjector.fillBuffer() to verify that PCM injection streams properly.",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Button(
                onClick = {
                    if (state.isTestRecordingActive) {
                        viewModel.stopTestRecorder()
                    } else {
                        viewModel.startTestRecorder()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isTestRecordingActive) RoseError else VioletAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("live_test_recorder_btn")
            ) {
                Icon(
                    imageVector = if (state.isTestRecordingActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isTestRecordingActive) "STOP TEST BENCH" else "START AUDIORECORD SIMULATOR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun LSPosedRealtimeStatusCard(
    state: MicHookUiState,
    viewModel: MicHookViewModel
) {
    val isActive = state.isLsposedActive
    val accentColor = if (isActive) EmeraldSuccess else AmberWarning
    val containerBg = if (isActive) DarkSurfaceElevated else DarkSurface

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = if (isActive) 0.4f else 0.35f), RoundedCornerShape(18.dp))
            .testTag("lsposed_status_card")
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "LSPOSED PLUGIN STATUS",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isActive) "ENABLED & ACTIVE" else "PLUGIN DISABLED",
                            color = accentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.refreshLsposedStatus() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("refresh_lsposed_status_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Check",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Check", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = state.lsposedStatusMessage,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Framework: ${state.lsposedFrameworkName}",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Checked: ${state.lsposedLastChecked}",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (!isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AmberWarning.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .border(1.dp, AmberWarning.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "How to enable in LSPosed:",
                                color = AmberWarning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "1. Open LSPosed Manager → Go to Modules\n2. Toggle MicHook switch to ON\n3. Select target apps (e.g. WhatsApp, Discord) in LSPosed Scope\n4. Restart the target app",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.2f MB", mb)
}
