package com.lquerin.sleeptimer

import android.Manifest
import android.app.StatusBarManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.lquerin.sleeptimer.ui.Midnight
import com.lquerin.sleeptimer.ui.SleepTimerTheme
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val QUICK_PRESETS = listOf(15, 30, 45, 60, 90)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark, transparent system bars (light icons) on top of the night sky.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            SleepTimerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NightSky()
                        SleepTimerScreen()
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

@Composable
fun SleepTimerScreen() {
    val context = LocalContext.current
    var endTime by remember { mutableLongStateOf(TimerPrefs.endTime(context)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selected by rememberSaveable { mutableIntStateOf(TimerPrefs.lastMinutes(context)) }
    var hasAccess by remember { mutableStateOf(MediaPauser.hasNotificationAccess(context)) }

    // Follow changes made from the notification or the Quick Settings tile.
    DisposableEffect(Unit) {
        val prefs = TimerPrefs.prefs(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            endTime = TimerPrefs.endTime(context)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // Coming back from Settings: re-check notification access and timer state.
    LifecycleResumeEffect(Unit) {
        endTime = TimerPrefs.endTime(context)
        hasAccess = MediaPauser.hasNotificationAccess(context)
        onPauseOrDispose { }
    }

    // Countdown tick.
    LaunchedEffect(endTime) {
        while (endTime > 0L) {
            now = System.currentTimeMillis()
            if (now >= endTime) {
                endTime = TimerPrefs.endTime(context)
                break
            }
            delay(250)
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val end = TimerPrefs.endTime(context)
        if (granted && end > 0L) TimerNotification.show(context, end)
    }

    val startTimer: (Int) -> Unit = { minutes ->
        SleepTimer.start(context, minutes)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Header()

        if (endTime > 0L) {
            RunningSection(endTime = endTime, now = now)
        } else {
            SetupSection(
                selected = selected,
                onSelectedChange = { selected = it },
                onStart = { startTimer(selected) },
            )
        }

        PauseMethodCard(hasAccess = hasAccess)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            QuickSettingsCard()
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Header() {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = if (hour >= 20 || hour < 5) R.string.greeting_night else R.string.greeting_day
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.header_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(greeting),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RunningSection(endTime: Long, now: Long) {
    val context = LocalContext.current
    val remaining = (endTime - now).coerceAtLeast(0L)
    val total = TimerPrefs.totalMillis(context).takeIf { it > 0L } ?: remaining
    val progress = if (total > 0L) remaining.toFloat() / total else 0f

    TimerDial(progress = progress) {
        Text(text = "💤", fontSize = 30.sp)
        Text(
            text = formatCountdown(remaining),
            style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Light,
        )
        Text(
            text = stringResource(R.string.until_clock, formatClock(context, endTime)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalButton(
            onClick = { SleepTimer.extend(context) },
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(stringResource(R.string.extend_emoji), style = MaterialTheme.typography.titleSmall)
        }
        OutlinedButton(
            onClick = { SleepTimer.cancel(context) },
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Text(stringResource(R.string.cancel), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupSection(selected: Int, onSelectedChange: (Int) -> Unit, onStart: () -> Unit) {
    val context = LocalContext.current
    val stopsAt = System.currentTimeMillis() + selected * 60_000L

    TimerDial(progress = selected.toFloat() / SleepTimer.MAX_MINUTES) {
        Text(text = "🌙", fontSize = 30.sp)
        Text(
            text = formatDuration(selected),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.stops_at, formatClock(context, stopsAt)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    GlassCard {
        // Quick presets
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            QUICK_PRESETS.forEach { minutes ->
                FilterChip(
                    selected = selected == minutes,
                    onClick = { onSelectedChange(minutes) },
                    label = { Text(formatDuration(minutes)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }

        // Custom value
        Slider(
            value = selected.toFloat(),
            onValueChange = { onSelectedChange(it.roundToInt().coerceIn(1, SleepTimer.MAX_MINUTES)) },
            valueRange = 1f..SleepTimer.MAX_MINUTES.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Midnight.Lavender,
                activeTrackColor = Midnight.Lavender,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(-5, -1, 1, 5).forEach { delta ->
                OutlinedButton(
                    onClick = { onSelectedChange((selected + delta).coerceIn(1, SleepTimer.MAX_MINUTES)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    Text(
                        text = if (delta > 0) "+$delta" else "−${-delta}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    Button(
        onClick = onStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(stringResource(R.string.start_emoji), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PauseMethodCard(hasAccess: Boolean) {
    val context = LocalContext.current
    GlassCard {
        Text(stringResource(R.string.pause_method_title), style = MaterialTheme.typography.titleMedium)
        if (hasAccess) {
            Text(
                text = stringResource(R.string.pause_method_direct),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.pause_method_fallback),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = { openNotificationAccess(context) },
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(stringResource(R.string.grant_access))
            }
            Text(
                text = stringResource(R.string.restricted_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { openAppInfo(context) }) {
                Text(stringResource(R.string.open_app_info))
            }
        }
    }
}

@Composable
private fun QuickSettingsCard() {
    val context = LocalContext.current
    GlassCard {
        Text(stringResource(R.string.qs_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.qs_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(
            onClick = { requestAddTile(context) },
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(stringResource(R.string.add_tile))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Visual building blocks
// ---------------------------------------------------------------------------------------------

/** Circular dial: soft glow, faint track and a lavender → moonlight-blue progress arc. */
@Composable
private fun TimerDial(progress: Float, content: @Composable ColumnScope.() -> Unit) {
    val animated by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "dial")
    val track = MaterialTheme.colorScheme.outlineVariant

    Box(modifier = Modifier.size(272.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val arcTopLeft = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(size.width - stroke, size.height - stroke)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Midnight.Lavender.copy(alpha = 0.16f), Color.Transparent),
                ),
                radius = size.minDimension / 2f,
            )
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (animated > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(Midnight.MoonBlue, Midnight.Lavender, Midnight.MoonBlue),
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

/** Translucent card that lets the night sky show through. */
@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

private class Star(val x: Float, val y: Float, val radiusDp: Float, val alpha: Float, val phase: Float)

/** Midnight gradient, a faint moon glow and slowly twinkling stars. */
@Composable
private fun NightSky() {
    val stars = remember {
        val rnd = Random(7)
        List(70) {
            Star(
                x = rnd.nextFloat(),
                y = rnd.nextFloat() * 0.8f,
                radiusDp = 0.5f + rnd.nextFloat() * 1.1f,
                alpha = 0.12f + rnd.nextFloat() * 0.4f,
                phase = rnd.nextFloat() * (2f * PI.toFloat()),
            )
        }
    }
    val twinkle = rememberInfiniteTransition(label = "twinkle")
    val t by twinkle.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 9000, easing = LinearEasing)),
        label = "t",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Midnight.SkyTop, Midnight.SkyMid, Midnight.SkyBottom),
            ),
        )
        val glowCenter = Offset(size.width * 0.9f, size.height * 0.04f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Midnight.MoonGlow.copy(alpha = 0.07f), Color.Transparent),
                center = glowCenter,
                radius = size.width * 0.75f,
            ),
            radius = size.width * 0.75f,
            center = glowCenter,
        )
        stars.forEach { star ->
            val shimmer = 0.6f + 0.4f * sin(t + star.phase)
            drawCircle(
                color = Midnight.StarWhite.copy(alpha = star.alpha * shimmer),
                radius = star.radiusDp.dp.toPx(),
                center = Offset(star.x * size.width, star.y * size.height),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// System screens
// ---------------------------------------------------------------------------------------------

private fun openNotificationAccess(context: Context) {
    val generic = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
            ComponentName(context, MediaListenerService::class.java).flattenToString(),
        )
    } else {
        generic
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(generic)
    }
}

private fun openAppInfo(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    )
}

private fun requestAddTile(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val statusBar = context.getSystemService(StatusBarManager::class.java) ?: return
    statusBar.requestAddTileService(
        ComponentName(context, SleepTileService::class.java),
        context.getString(R.string.app_name),
        Icon.createWithResource(context, R.drawable.ic_moon),
        context.mainExecutor,
    ) { }
}
