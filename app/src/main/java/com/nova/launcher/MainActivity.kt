package com.nova.launcher

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Rect
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
    }
}

private data class LaunchableApp(val label: String, val packageName: String, val icon: Drawable)

@Composable
private fun NovaHome() {
    val context = LocalContext.current
    val launchSourceView = LocalView.current
    var allApps by remember { mutableStateOf(emptyList<LaunchableApp>()) }
    var drawerOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var clock by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        allApps = pm.queryIntentActivities(intent, 0)
            .mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                if (activity.packageName == context.packageName) return@mapNotNull null
                LaunchableApp(info.loadLabel(pm).toString(), activity.packageName, info.loadIcon(pm))
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
    LaunchedEffect(Unit) {
        while (true) { clock = Date(); delay(1000) }
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
                    Row(Modifier.padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Settings, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("NOVA", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
                    }
                }
            }
            Spacer(Modifier.height(48.dp))
            Text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(clock), color = Color.White.copy(.88f), fontSize = 15.sp)
            Text(SimpleDateFormat("h:mm", Locale.getDefault()).format(clock), color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Light, letterSpacing = (-3).sp)
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(visible = !drawerOpen, enter = fadeIn(), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your space. Your flow.", color = Color.White.copy(.9f), fontSize = 14.sp)
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        allApps.take(4).forEach { app -> AppShortcut(app) { bounds -> launchApp(context, app.packageName, launchSourceView, bounds) } }
                    }
                    Spacer(Modifier.height(22.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(58.dp).clickable { drawerOpen = true },
                        color = Color.White.copy(alpha = .20f), shape = RoundedCornerShape(30.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(.22f))
                    ) {
                        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Rounded.Search, null, tint = Color.White)
                            Spacer(Modifier.width(10.dp))
                            Text("Search apps", color = Color.White.copy(.95f), fontSize = 15.sp)
                        }
                    }
                }
            }
            AnimatedVisibility(visible = drawerOpen, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.weight(1f)) {
                Column(Modifier.fillMaxSize()) {
                    Surface(color = Color.White.copy(.2f), shape = RoundedCornerShape(24.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Search, null, tint = Color.White)
                            Spacer(Modifier.width(10.dp))
                            BasicTextField(
                                value = query, onValueChange = { query = it },
                                singleLine = true, modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                                decorationBox = { inner ->
                                    if (query.isEmpty()) Text("Find an app", color = Color.White.copy(.7f))
                                    inner()
                                }
                            )
                            Text("Close", color = Color.White, modifier = Modifier.clickable { drawerOpen = false; query = "" })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("ALL APPS", color = Color.White.copy(.72f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))
                    LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            AppShortcut(app) { launchApp(context, app.packageName) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppShortcut(app: LaunchableApp, onClick: (Rect) -> Unit) {
    var iconBounds by remember { mutableStateOf(Rect.Zero) }
    val bitmap = remember(app.packageName) { app.icon.toBitmap(144, 144).asImageBitmap() }
    Column(
        Modifier.width(72.dp).onGloballyPositioned { iconBounds = it.boundsInWindow() }.clip(RoundedCornerShape(18.dp)).clickable { onClick(iconBounds) }.padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.Image(bitmap, contentDescription = app.label, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)))
        Spacer(Modifier.height(7.dp))
        Text(app.label, color = Color.White, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}

private fun launchApp(
    context: android.content.Context,
    packageName: String,
    sourceView: android.view.View,
    iconBounds: Rect
) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // Expand from the tapped shortcut; Android/OEM controls the final timing.
    val x = iconBounds.left.toInt().coerceAtLeast(0)
    val y = iconBounds.top.toInt().coerceAtLeast(0)
    val width = iconBounds.width.toInt().coerceAtLeast(1)
    val height = iconBounds.height.toInt().coerceAtLeast(1)
    val options = ActivityOptions.makeScaleUpAnimation(sourceView, x, y, width, height).toBundle()
    try {
        context.startActivity(intent, options)
    } catch (_: Exception) {
        context.startActivity(intent)
    }
}
