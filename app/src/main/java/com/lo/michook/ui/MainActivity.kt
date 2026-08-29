package com.lo.michook.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lo.michook.R
import com.lo.michook.ui.screens.AudioInjectionScreen
import com.lo.michook.ui.screens.DashboardScreen
import com.lo.michook.ui.screens.HooksOverviewScreen
import com.lo.michook.ui.screens.LogViewerScreen
import com.lo.michook.ui.theme.CyanPrimary
import com.lo.michook.ui.theme.DarkBackground
import com.lo.michook.ui.theme.DarkSurface
import com.lo.michook.ui.theme.DarkSurfaceElevated
import com.lo.michook.ui.theme.EmeraldSuccess
import com.lo.michook.ui.theme.MicHookTheme
import com.lo.michook.ui.theme.TextPrimary
import com.lo.michook.ui.theme.TextSecondary
import com.lo.michook.ui.theme.VioletAccent
import com.lo.michook.ui.viewmodel.MicHookViewModel

enum class NavigationScreen(
    val title: String,
    val icon: ImageVector,
    val tag: String
) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    AUDIO("Audio Source", Icons.Default.Audiotrack, "nav_audio"),
    HOOKS("Hook Layers", Icons.Default.Layers, "nav_hooks"),
    LOGS("Event Logs", Icons.Default.Description, "nav_logs")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MicHookViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MicHookTheme {
                MicHookApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MicHookApp(viewModel: MicHookViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf(NavigationScreen.DASHBOARD) }

    Scaffold(
        topBar = {
            TopAppBarHeader(
                isReady = uiState.isReady,
                isInjectorRunning = uiState.isInjectorRunning,
                isLsposedActive = uiState.isLsposedActive
            )
        },
        bottomBar = {
            BottomNavBar(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        },
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                NavigationScreen.DASHBOARD -> DashboardScreen(state = uiState, viewModel = viewModel)
                NavigationScreen.AUDIO -> AudioInjectionScreen(state = uiState, viewModel = viewModel)
                NavigationScreen.HOOKS -> HooksOverviewScreen(state = uiState, viewModel = viewModel)
                NavigationScreen.LOGS -> LogViewerScreen(state = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun TopAppBarHeader(
    isReady: Boolean,
    isInjectorRunning: Boolean,
    isLsposedActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "MicHook",
                    tint = CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = "MicHook",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "LSposed Core v1.0",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        val badgeActive = isLsposedActive || isReady
        val badgeColor = if (isReady) EmeraldSuccess else if (isLsposedActive) CyanPrimary else com.lo.michook.ui.theme.AmberWarning

        Box(
            modifier = Modifier
                .background(
                    badgeColor.copy(alpha = 0.15f),
                    RoundedCornerShape(16.dp)
                )
                .border(
                    1.dp,
                    badgeColor.copy(alpha = 0.4f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Text(
                    text = if (isReady) "PCM INJECTING" else if (isLsposedActive) "LSPOSED ACTIVE" else "MODULE DISABLED",
                    color = badgeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    currentScreen: NavigationScreen,
    onScreenSelected: (NavigationScreen) -> Unit
) {
    NavigationBar(
        containerColor = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationScreen.values().forEach { screen ->
            val isSelected = currentScreen == screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(screen) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = CyanPrimary,
                    indicatorColor = CyanPrimary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                ),
                modifier = Modifier.testTag(screen.tag)
            )
        }
    }
}
