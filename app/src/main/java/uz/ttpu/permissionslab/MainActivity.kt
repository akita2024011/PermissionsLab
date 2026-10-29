package uz.ttpu.permissionslab

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PermissionsLabScreen() } }
    }
}

@Composable
private fun PermissionsLabScreen() {
    val context = LocalContext.current
    val activity = context.findActivity()

    var online by remember { mutableStateOf(isOnline(context)) }
    var cameraGranted by remember {
        mutableStateOf(isGranted(context, Manifest.permission.CAMERA))
    }
    var cameraRequests by rememberSaveable { mutableStateOf(0) }
    var locationFine by remember {
        mutableStateOf(isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION))
    }
    var locationCoarse by remember {
        mutableStateOf(isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var notificationGranted by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 ||
            isGranted(context, Manifest.permission.POST_NOTIFICATIONS))
    }
    var lastLocationResult by remember { mutableStateOf("Not requested yet") }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
        if (!granted) cameraRequests++
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationFine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        locationCoarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        lastLocationResult = "Returned map: $result"
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationGranted = it }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        online = isOnline(context)
        cameraGranted = isGranted(context, Manifest.permission.CAMERA)
        locationFine = isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
        locationCoarse = isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        overlayGranted = Settings.canDrawOverlays(context)
        notificationGranted = Build.VERSION.SDK_INT < 33 ||
            isGranted(context, Manifest.permission.POST_NOTIFICATIONS)
    }

    LaunchedEffect(Unit) { createNotificationChannel(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("PermissionsLab", style = MaterialTheme.typography.headlineMedium)
            Text("Practice 8 • Android permissions demo")
            Spacer(Modifier.height(4.dp))
        }
        item {
            DemoCard("1. Normal permissions — network") {
                Text("Network status: ${if (online) "Online" else "Offline"}")
                Text("INTERNET and ACCESS_NETWORK_STATE are declared in the manifest; no runtime dialog.")
                Button(onClick = { online = isOnline(context) }) { Text("Refresh network status") }
            }
        }
        item {
            DemoCard("2. Runtime permission — camera") {
                Text("Current state: ${if (cameraGranted) "Granted" else "Not granted"}")
                val rationale: Boolean = activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                } ?: false
                Text("shouldShowRationale: $rationale")
                Text("Requests denied in this session: $cameraRequests")
                when {
                    cameraGranted -> Text("Camera permission is granted.")
                    cameraRequests >= 2 && !rationale -> Button(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}"))
                        )
                    }) { Text("Open app settings") }
                    else -> Button(onClick = {
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    }) { Text(if (rationale) "Explain / request camera" else "Request camera permission") }
                }
                Text("Try: Deny → Deny → Open app settings. Reset app permissions to repeat.")
            }
        }
        item {
            DemoCard("3. Special permission — overlay") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Can draw over other apps: ${if (overlayGranted) "Yes" else "No"}",
                        modifier = Modifier.weight(1f))
                    Switch(checked = overlayGranted, onCheckedChange = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    })
                }
                Text("The app re-checks Settings.canDrawOverlays() when it resumes.")
            }
        }
        item {
            DemoCard("4. Runtime permissions — location") {
                Text("Precise: ${if (locationFine) "Granted" else "Not granted"}")
                Text("Approximate: ${if (locationCoarse) "Granted" else "Not granted"}")
                Button(onClick = {
                    locationLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }) { Text("Request location permissions") }
                Text(lastLocationResult)
                Text("After choosing Only this time, background the app and return to see the refreshed state.")
            }
        }
        item {
            DemoCard("5. Runtime permission — notifications") {
                Text("Notifications: ${if (notificationGranted) "Allowed / not required on this Android version" else "Not granted"}")
                if (Build.VERSION.SDK_INT >= 33 && !notificationGranted) {
                    Button(onClick = {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) { Text("Request notification permission") }
                } else {
                    Button(onClick = { sendTestNotification(context) }) {
                        Text("Send test notification")
                    }
                }
                Text("POST_NOTIFICATIONS is requested at runtime only on API 33+.")
            }
        }
        item {
            DemoCard("6. Kotlin denied-twice model") {
                Text("Theory: first denial can show rationale; repeated denial may require app Settings.")
                Text("Use the camera section above to demonstrate the real system permission flow.")
            }
        }
    }
}

@Composable
private fun DemoCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun isGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun isOnline(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "permissionslab_demo",
            "PermissionsLab test notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}

private fun sendTestNotification(context: Context) {
    if (Build.VERSION.SDK_INT >= 33 &&
        !isGranted(context, Manifest.permission.POST_NOTIFICATIONS)
    ) return

    val notification = NotificationCompat.Builder(context, "permissionslab_demo")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("PermissionsLab")
        .setContentText("Test notification sent successfully.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    manager?.notify(1001, notification)
}
