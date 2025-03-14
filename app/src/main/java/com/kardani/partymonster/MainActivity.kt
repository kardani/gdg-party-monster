package com.kardani.partymonster

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kardani.partymonster.service.PartyMonsterService
import com.kardani.partymonster.tiles.PartyMonsterTileService
import com.kardani.partymonster.ui.theme.PartyMonsterTheme
import com.kardani.partymonster.ui.theme.primaryColor

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            Toast.makeText(
                this,
                "All permissions granted",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "Permissions required for full functionality",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:${packageName}")
            startActivity(intent)
        }

        requestRequiredPermissions()

        setContent {
            var isPartying by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                PartyMonsterService.serviceStatus.observe(this@MainActivity) { isRunning ->
                    isPartying = isRunning
                }
            }

            PartyMonsterTheme {
                MainContent(
                    isPartying = isPartying,
                    onRequestAddTile = {
                        requestTileAdd()
                    }
                )
            }
        }
    }

    private fun requestRequiredPermissions() {
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    android.Manifest.permission.CAMERA
                )
            )
        }
    }

    @SuppressLint("WrongConstant")
    private fun requestTileAdd() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager = getSystemService(Context.STATUS_BAR_SERVICE) as StatusBarManager
            statusBarManager.requestAddTileService(
                ComponentName(this, PartyMonsterTileService::class.java),
                "Party Monster",
                Icon.createWithResource(this, R.drawable.ic_party_active),
                {},{}
            )
        }
    }
}

@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    isPartying: Boolean,
    onRequestAddTile: () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->

        Box(modifier = Modifier.fillMaxSize()) {

            PartyBackground(isPartying = isPartying)

            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = if (isPartying) "Now we're talking! 🎉" else "No Party :( 🎈",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 24.sp
                )

            }

            FloatingActionButton(
                onClick = onRequestAddTile,
                shape = FloatingActionButtonDefaults.smallShape,
                containerColor = Color.LightGray,
                modifier = Modifier
                    .padding(32.dp)
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }

        }

    }
}

@Composable
fun PartyBackground(isPartying: Boolean) {
    val partyColors = listOf(
        Color(0xFFFF4081),
        Color(0xFF3D5AFE),
        Color(0xFFFFEB3B),
        Color(0xFF00E676)
    )

    val defaultColor = primaryColor

    val transition = rememberInfiniteTransition()

    val backgroundColor = if (isPartying) {
        val colorIndex by transition.animateFloat(
            initialValue = 0f,
            targetValue = partyColors.size.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )

        val currentIndex = colorIndex.toInt() % partyColors.size
        val nextIndex = (currentIndex + 1) % partyColors.size
        val currentColor = partyColors[currentIndex].copy(alpha = 0.5f)
        val nextColor = partyColors[nextIndex]

        val fraction = colorIndex - colorIndex.toInt()

        lerp(currentColor, nextColor, fraction)
    } else {
        defaultColor
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    )
}

@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    PartyMonsterTheme {
        MainContent(
            isPartying = true,
            onRequestAddTile = {}
        )
    }
}