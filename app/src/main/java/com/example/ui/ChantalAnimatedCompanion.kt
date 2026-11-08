package com.example.ui

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.ChantalVisualState
import kotlinx.coroutines.delay

/** Observe both runtime accessibility preference and foreground lifecycle; no background animation loop. */
@Composable
internal fun rememberCompanionMotionAllowed(): Boolean {
    val resolver = LocalContext.current.contentResolver
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    fun systemMotion() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    var enabled by remember { mutableStateOf(systemMotion()) }
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(resolver, lifecycle) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { enabled = systemMotion() }
        }
        val lifecycleObserver = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        lifecycle.addObserver(lifecycleObserver)
        onDispose { resolver.unregisterContentObserver(observer); lifecycle.removeObserver(lifecycleObserver) }
    }
    return enabled && resumed
}

@Composable
internal fun ChantalAnimatedCompanion(state: ChantalVisualState, transformed: Boolean, motionAllowed: Boolean) {
    var blink by remember { mutableStateOf(false) }
    var sway by remember { mutableStateOf(false) }
    LaunchedEffect(motionAllowed, state, transformed) {
        blink = false; sway = false
        if (motionAllowed && !transformed) while (true) {
            delay(1800); sway = !sway
            delay(1600); blink = true
            delay(130); blink = false; sway = !sway
        }
    }
    val angle by animateFloatAsState(if (motionAllowed && sway) 7f else 0f,
        tween(if (motionAllowed) 750 else 0), label = "chantal_tail")
    Surface(color = HobbitElevatedBg, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp).testTag("chantal_living_state"), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            if (!transformed) Canvas(Modifier.size(92.dp).semantics { contentDescription = "Шанталь: ${state.title}" }) {
                val unit = size.width / 100f
                fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
                val fur = Color(0xFFCB7136); val light = Color(0xFFF0D9AE); val dark = Color(0xFF38271F)
                // Separate tail and ears can move without distorting the existing portrait.
                rotate(-angle, point(60f, 72f)) {
                    drawOval(fur, point(54f, 48f), Size(43f * unit, 38f * unit))
                    drawOval(light, point(78f, 47f), Size(18f * unit, 23f * unit))
                }
                drawOval(fur, point(26f, 49f), Size(43f * unit, 42f * unit))
                drawOval(light, point(35f, 62f), Size(22f * unit, 26f * unit))
                fun ear(x: Float, tilt: Float) { rotate(tilt, point(x + 8f, 30f)) {
                    val path = Path().apply { moveTo(x * unit, 33f * unit); lineTo((x + 3) * unit, 7f * unit); lineTo((x + 20) * unit, 30f * unit); close() }
                    drawPath(path, fur)
                    val inside = Path().apply { moveTo((x + 5) * unit, 27f * unit); lineTo((x + 5) * unit, 15f * unit); lineTo((x + 14) * unit, 27f * unit); close() }
                    drawPath(inside, dark)
                } }
                ear(23f, angle / 2); ear(48f, -angle / 2)
                drawOval(fur, point(21f, 24f), Size(51f * unit, 39f * unit))
                drawOval(light, point(27f, 43f), Size(38f * unit, 20f * unit))
                val closed = blink || state == ChantalVisualState.TIRED || state == ChantalVisualState.HAPPY
                listOf(35f, 57f).forEach { x ->
                    if (closed) drawLine(dark, point(x - 3, 40f), point(x + 3, 40f), 2f * unit)
                    else drawCircle(dark, 2.4f * unit, point(x, 39f))
                }
                drawCircle(dark, 3f * unit, point(46f, 49f))
                drawLine(dark, point(46f, 51f), point(49f, 55f), unit)
                drawOval(dark, point(26f, 85f), Size(17f * unit, 8f * unit))
                drawOval(dark, point(50f, 85f), Size(17f * unit, 8f * unit))
                if (state == ChantalVisualState.PACKING || state == ChantalVisualState.TRAVELLING) {
                    drawRoundRect(Color(0xFF657449), point(57f, 58f), Size(18f * unit, 24f * unit), androidx.compose.ui.geometry.CornerRadius(4f * unit))
                    drawLine(light, point(59f, 65f), point(73f, 65f), 2f * unit)
                }
                if (state == ChantalVisualState.RETURNED) {
                    drawRoundRect(light, point(8f, 66f), Size(22f * unit, 14f * unit), androidx.compose.ui.geometry.CornerRadius(2f * unit))
                    drawLine(fur, point(10f, 68f), point(19f, 74f), unit)
                    drawLine(fur, point(19f, 74f), point(28f, 68f), unit)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.title, color = HobbitGold, style = MaterialTheme.typography.titleSmall)
                Text("«${state.thought}»", color = HobbitCream, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
