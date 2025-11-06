package io.embrace.shoppingcart.presentation.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var darkMode by remember { mutableStateOf(prefs.getBoolean("darkMode", false)) }
    var notifications by remember { mutableStateOf(prefs.getBoolean("notifications", true)) }
    var analytics by remember { mutableStateOf(prefs.getBoolean("analytics", true)) }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Dark mode")
                Switch(checked = darkMode, onCheckedChange = { darkMode = it })
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Notifications")
                Switch(checked = notifications, onCheckedChange = { notifications = it })
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Analytics")
                Switch(checked = analytics, onCheckedChange = { analytics = it })
            }
            Button(onClick = {
                prefs.edit()
                    .putBoolean("darkMode", darkMode)
                    .putBoolean("notifications", notifications)
                    .putBoolean("analytics", analytics)
                    .apply()
            }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }
}
