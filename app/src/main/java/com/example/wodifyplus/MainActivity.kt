package com.example.wodifyplus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.wodifyplus.notifications.NotificationHelper
import com.example.wodifyplus.ui.components.BottomNavBar
import com.example.wodifyplus.ui.navigation.NavGraph
import com.example.wodifyplus.ui.theme.WodifyPlusTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge to edge
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        // Inicializar Python
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // Crear canal de notificaciones
        NotificationHelper.createNotificationChannel(this)

        // Solicitar permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permiso ya concedido
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        setContent {
            WodifyPlusTheme {
                val navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavBar(navController = navController)
                    }
                ) { paddingValues ->
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}
