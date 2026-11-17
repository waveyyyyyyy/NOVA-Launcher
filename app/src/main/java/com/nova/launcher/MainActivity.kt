package com.nova.launcher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        setContent { NovaHome() }

        // Ask Android to make NOVA the actual HOME app. Without this, pressing
        // Home can legitimately return the user to the Vivo launcher.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                try {
                    startActivityForResult(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME),
                        REQUEST_HOME_ROLE
                    )
                } catch (_: Exception) {
                    // The user can still select NOVA manually in Android settings.
                }
            }
        }
    }

    companion object {
        private const val REQUEST_HOME_ROLE = 4102
    }
}

private data class LaunchableApp(val label: String, val packageName: String, val icon: Drawable)

@Composable
private fun NovaHome() {
    val context = LocalContext.current
    var allApps by remember { mutableStateOf(emptyList<LaunchableApp>()) }
    var appsLoaded by remember { mutableStateOf(false) }
    var drawerOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var clock by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        try {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            allApps = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
                .mapNotNull { info ->
                    val activity = info.activityInfo ?: return@mapNotNull null
                    if (activity.packageName == context.packageName) return@mapNotNull null
                    LaunchableApp(info.loadLabel(pm).toString(), activity.packageName, info.loadIcon(pm))
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase(Locale.getDefault()) }
        } catch (_: Exception) {
            allApps = emptyList()
        } finally {
            appsLoaded = true
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            clock = Date()
            delay(1000)
        }
    }

    BackHandler(enabled = drawerOpen) {
        drawerOpen = false
        query = ""
    }

    val filtered = allApps.filter { it.label.contains(query, ignoreCase = true) }
    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF15243C), Color(0xFF263E5D), Color(0xFF6D8297), Color(0xFFCBD4DB))
    )

    Box(Modifier.fillMaxSize().background(gradient)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Surface(color = Color.White.copy(alpha = .14f), shape = CircleShape) {
                    Text(
                        "NOVA",
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                        color = Color.White, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp
                    )
                }
            }

            AnimatedVisibility(visible = !drawerOpen, enter = fadeIn(), exit = fadeOut()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(clock),
                        color = Color.White.copy(.88f), fontSize = 15.sp
                    )
                    Text(
                        SimpleDateFormat("h:mm", Locale.getDefault()).format(clock),
                        color = Color.White, fontSize = 72.sp,
                        fontWeight = FontWeight.Light, letterSpacing = (-3).sp
                    )
                    Spacer(Modifier.weight(1f))
                    Text("Your space. Your flow.", color = Color.White.copy(.9f), fontSize = 14.sp)
                    Spacer(Modifier.height(20.dp))

                    if (!appsLoaded) {
                        Text("Loading your apps…", color = Color.White.copy(.8f))
                    } else if (allApps.isEmpty()) {
                        Text("No launchable apps found", color = Color.White)
                    } else {
                        val favorites = allApps.take(4)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            favorites.forEach { app ->
                                AppShortcut(app) { launchApp(context, app.packageName) }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                            .clickable { drawerOpen = true },
                        color = Color.White.copy(alpha = .20f),
                        shape = RoundedCornerShape(30.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(.22f))
                    ) {
                        Row(
                            Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Apps, null, tint = Color.White)
                            Spacer(Modifier.width(10.dp))
                            Text("All apps", color = Color.White, fontSize = 15.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            AnimatedVisibility(
                visible = drawerOpen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.weight(1f)
            ) {
                Column(Modifier.fillMaxSize()) {
                    Surface(color = Color.White.copy(.2f), shape = RoundedCornerShape(24.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Search, null, tint = Color.White)
                            Spacer(Modifier.width(10.dp))
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                                decorationBox = { inner ->
                                    if (query.isEmpty()) Text("Search all apps", color = Color.White.copy(.7f))
                                    inner()
                                }
                            )
                            Text(
                                "Close",
                                color = Color.White,
                                modifier = Modifier.clickable { drawerOpen = false; query = "" }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "ALL APPS",
                        color = Color.White.copy(.72f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    if (!appsLoaded) {
                        Text("Loading apps…", color = Color.White)
                    } else if (filtered.isEmpty()) {
                        Text("No apps match your search.", color = Color.White.copy(.8f))
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filtered, key = { it.packageName }) { app ->
                                AppShortcut(app) { launchApp(context, app.packageName) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppShortcut(app: LaunchableApp, onClick: () -> Unit) {
    val bitmap = remember(app.packageName) {
        app.icon.toBitmap(144, 144).asImageBitmap()
    }
    Column(
        Modifier.width(72.dp).clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            bitmap,
            contentDescription = app.label,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(15.dp))
        )
        Spacer(Modifier.height(7.dp))
        Text(
            app.label,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

private fun launchApp(context: Context, packageName: String) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            Toast.makeText(context, "This app cannot be opened from NOVA.", Toast.LENGTH_SHORT).show()
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Couldn't open this app. Please try again.", Toast.LENGTH_SHORT).show()
    }
}
