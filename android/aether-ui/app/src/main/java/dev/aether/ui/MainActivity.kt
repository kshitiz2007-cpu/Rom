package dev.aether.ui

import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ThemePreset(val name: String, val primary: Color, val secondary: Color, val bg: Color, val surface: Color, val accent: Color) {
    companion object {
        val Aurora = ThemePreset("Aurora", Color(0xFF9B87FF), Color(0xFF74CFFF), Color(0xFF10121C), Color(0xFF1B1E2D), Color(0xFF78E0CE))
        val Mint = ThemePreset("Mint", Color(0xFF71D6BC), Color(0xFF92D6FF), Color(0xFF101918), Color(0xFF1D2928), Color(0xFFC4F4E3))
        val Rose = ThemePreset("Rose", Color(0xFFFF8EBD), Color(0xFFC9A5FF), Color(0xFF1A121B), Color(0xFF2B1E2B), Color(0xFFFFC9E0))
        val Solar = ThemePreset("Solar", Color(0xFFE6B96C), Color(0xFFFF985F), Color(0xFF191713), Color(0xFF28231B), Color(0xFFFFDCA2))
        val presets = listOf(Aurora, Mint, Rose, Solar)
    }
}
private fun themeOf(t: ThemePreset) = darkColorScheme(primary=t.primary, secondary=t.secondary, background=t.bg, surface=t.surface, tertiary=t.accent)

private fun Drawable.toBitmap(): Bitmap {
    val w = intrinsicWidth.takeIf { it > 0 } ?: 96
    val h = intrinsicHeight.takeIf { it > 0 } ?: 96
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, w, h)
    draw(canvas)
    return bitmap
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var theme by remember { mutableStateOf(ThemePreset.Aurora) }
            MaterialTheme(colorScheme=themeOf(theme)) { AetherApp(theme, { theme=it }) }
        }
    }
}

@Composable
private fun AetherApp(theme: ThemePreset, onTheme: (ThemePreset)->Unit) {
    var drawer by remember { mutableStateOf(false) }
    var studio by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val pm = context.packageManager
    val apps = remember {
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
    }
    when {
        studio -> ThemeStudio(theme, onTheme, { studio=false })
        drawer -> Drawer(apps, { drawer=false }, { app ->
            runCatching { context.startActivity(pm.getLaunchIntentForPackage(app.activityInfo.packageName)) }
        })
        else -> Home(theme, { drawer=true }, { studio=true }, { pkg ->
            runCatching { context.startActivity(pm.getLaunchIntentForPackage(pkg)) }
        })
    }
}

@Composable
private fun Home(t: ThemePreset, openDrawer: ()->Unit, openStudio: ()->Unit, launch: (String)->Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val favorites = listOf("com.google.android.dialer", "com.google.android.apps.messaging", "com.android.camera2", "com.google.android.apps.photos")
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(t.bg, t.surface, t.primary.copy(alpha=.12f))))) {
        Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement=Arrangement.SpaceBetween) {
            Column(verticalArrangement=Arrangement.spacedBy(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                    Text("AETHER", color=t.primary, fontWeight=FontWeight.Bold, letterSpacing=4.sp)
                    IconButton(onClick=openStudio) { Icon(Icons.Default.Palette, "Theme Studio", tint=t.primary) }
                }
                Text(SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date()), color=Color.White.copy(alpha=.7f))
                Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), fontSize=72.sp, fontWeight=FontWeight.Light, color=Color.White)
                Surface(shape=RoundedCornerShape(28.dp), color=t.surface, modifier=Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(18.dp), verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Your space", color=t.primary, fontWeight=FontWeight.SemiBold)
                            Text("Calm, focused, personal.", color=Color.White.copy(alpha=.65f))
                        }
                        Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(t.primary,t.secondary))), contentAlignment=Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, null, tint=t.bg)
                        }
                    }
                }
                Text("FAVORITES", color=Color.White.copy(alpha=.55f), letterSpacing=2.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    favorites.forEach { pkg ->
                        if (pm.getLaunchIntentForPackage(pkg) != null) {
                            Surface(onClick={launch(pkg)}, shape=RoundedCornerShape(20.dp), color=t.surface, modifier=Modifier.weight(1f).height(62.dp)) {
                                Box(contentAlignment=Alignment.Center) {
                                    val drawable=runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
                                    if (drawable!=null) Image(drawable.toBitmap().asImageBitmap(), contentDescription=pkg, modifier=Modifier.size(32.dp))
                                    else Icon(Icons.Default.Apps, null)
                                }
                            }
                        } else Spacer(Modifier.weight(1f))
                    }
                }
            }
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Surface(onClick=openDrawer, shape=RoundedCornerShape(24.dp), color=t.surface, modifier=Modifier.fillMaxWidth().height(56.dp)) {
                    Row(Modifier.padding(horizontal=16.dp), verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint=t.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Search apps", color=Color.White.copy(alpha=.65f))
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.KeyboardArrowUp, null, tint=t.primary)
                    }
                }
                Text("Aether UI · v0.1", color=Color.White.copy(alpha=.45f), style=MaterialTheme.typography.labelSmall, modifier=Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun Drawer(apps: List<ResolveInfo>, back: ()->Unit, launch: (ResolveInfo)->Unit) {
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val pm = context.packageManager
    val filtered = apps.filter { it.loadLabel(pm).toString().contains(query, true) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(18.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=back) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("All apps", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold)
        }
        OutlinedTextField(query, {query=it}, modifier=Modifier.fillMaxWidth(), singleLine=true, placeholder={Text("Find an app")}, leadingIcon={Icon(Icons.Default.Search, null)})
        LazyColumn {
            items(filtered, key={it.activityInfo.packageName}) { app ->
                Row(Modifier.fillMaxWidth().clickable { launch(app) }.padding(vertical=10.dp), verticalAlignment=Alignment.CenterVertically) {
                    Image(app.loadIcon(pm).toBitmap().asImageBitmap(), contentDescription=app.loadLabel(pm).toString(), modifier=Modifier.size(42.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(app.loadLabel(pm).toString())
                }
            }
        }
    }
}

@Composable
private fun ThemeStudio(current: ThemePreset, onTheme: (ThemePreset)->Unit, back: ()->Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=back) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Theme Studio", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold)
        }
        Text("Choose a color world", color=MaterialTheme.colorScheme.onBackground.copy(alpha=.65f), modifier=Modifier.padding(bottom=12.dp))
        ThemePreset.presets.forEach { preset ->
            Surface(onClick={onTheme(preset)}, shape=RoundedCornerShape(22.dp), color=preset.surface, modifier=Modifier.fillMaxWidth().padding(vertical=7.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(preset.primary,preset.secondary,preset.accent))))
                    Text(preset.name, Modifier.weight(1f).padding(start=14.dp), fontWeight=FontWeight.SemiBold)
                    if (preset==current) Icon(Icons.Default.Check, null, tint=preset.primary)
                }
            }
        }
        Text("Presets · live preview", color=MaterialTheme.colorScheme.onBackground.copy(alpha=.5f), modifier=Modifier.padding(top=16.dp))
    }
}
