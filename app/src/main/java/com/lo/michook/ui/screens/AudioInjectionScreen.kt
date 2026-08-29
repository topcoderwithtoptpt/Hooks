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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lo.michook.audio.MicHookSettings
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
fun AudioInjectionScreen(
    state: MicHookUiState,
    viewModel: MicHookViewModel,
    modifier: Modifier = Modifier
) {
    var pathInput by remember(state.audioPath) { mutableStateOf(state.audioPath) }
    var isSavedConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "AUDIO SOURCE CONFIGURATION",
                color = CyanPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Select or configure the audio file injected into hooked microphone channels.",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        item {
            // Path configuration Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Audio File Path",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (state.fileExists) {
                            Text(
                                text = "File Found ✓",
                                color = EmeraldSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "File Not Found",
                                color = AmberWarning,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = pathInput,
                        onValueChange = {
                            pathInput = it
                            isSavedConfirm = false
                        },
                        label = { Text("Absolute storage path (.mp3 / .wav)") },
                        leadingIcon = {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = CyanPrimary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("audio_path_input")
                    )

                    // Presets
                    Text(
                        text = "Quick Presets:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                pathInput = MicHookSettings.DEFAULT_MP3
                                viewModel.updateAudioPath(pathInput)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Default MP3", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.generateSampleAudioTone()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Generate WAV", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.updateAudioPath(pathInput)
                            viewModel.startInjector()
                            isSavedConfirm = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("save_audio_path_btn")
                    ) {
                        Icon(
                            imageVector = if (isSavedConfirm) Icons.Default.Check else Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSavedConfirm) "CONFIG SAVED & LOADED" else "APPLY & START DECODER",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            // Audio Stream Parameters Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "STREAM PARAMETERS",
                        color = VioletAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Volume Boost
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.volume > 1.0f) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Volume Multiplier", color = TextPrimary, fontSize = 14.sp)
                            }
                            Text(
                                text = "${(state.volume * 100).toInt()}%",
                                color = CyanPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = state.volume,
                            onValueChange = { viewModel.setVolume(it) },
                            valueRange = 0f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanPrimary,
                                activeTrackColor = CyanPrimary,
                                inactiveTrackColor = DarkBackground
                            ),
                            modifier = Modifier.testTag("volume_slider")
                        )
                    }

                    // Loop mode switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Loop,
                                contentDescription = null,
                                tint = VioletAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text("Infinite Loop Mode", color = TextPrimary, fontSize = 14.sp)
                                Text("Seek to 0:00 automatically on EOF", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Switch(
                            checked = state.loopAudio,
                            onCheckedChange = { viewModel.toggleLoop() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = VioletAccent
                            ),
                            modifier = Modifier.testTag("loop_audio_switch")
                        )
                    }

                    // Mute mode switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = RoseError,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text("Mute / Silence Injection", color = TextPrimary, fontSize = 14.sp)
                                Text("Inject zeroed PCM (blocks mic input)", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Switch(
                            checked = state.isMuted,
                            onCheckedChange = { viewModel.toggleMute() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = RoseError
                            ),
                            modifier = Modifier.testTag("mute_audio_switch")
                        )
                    }
                }
            }
        }

        item {
            // Architecture Info Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "DECODER SPECIFICATION",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "• Decoder: Android MediaCodec (Hardware/Software) via MediaExtractor\n" +
                               "• Output: Raw Signed 16-bit PCM (Linear Little Endian)\n" +
                               "• Buffer Queue: LinkedBlockingQueue<ByteArray> (200-chunk lock-free)\n" +
                               "• Config File: /sdcard/MicHook/config.txt (live runtime reload)",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
