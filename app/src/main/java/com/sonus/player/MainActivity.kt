package com.sonus.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.sonus.player.ui.navigation.SonusNavigation
import com.sonus.player.ui.permission.PermissionScreen
import com.sonus.player.ui.theme.SonusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // TODO: REMOVE THIS — Temporary crash to verify Crashlytics is working
        throw RuntimeException("Crashlytics test crash — remove after verification")
        setContent {
            SonusTheme {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                var hasPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, permission) ==
                            PackageManager.PERMISSION_GRANTED
                    )
                }

                val snackbarHostState = remember { SnackbarHostState() }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    if (hasPermission) {
                        SonusNavigation()
                    } else {
                        PermissionScreen(
                            onPermissionGranted = { hasPermission = true }
                        )
                    }
                }
            }
        }
    }
}
