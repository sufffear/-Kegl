package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.DEFAULT_CHANTAL_MAP_REGIONS
import com.example.ui.theme.LocalVelarisPalette

@Composable
internal fun ChantalWorldMap(journeys: Int, selectedId: String, locked: Boolean, onChoose: (String) -> Unit) {
    val palette = LocalVelarisPalette.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var inspectedId by rememberSaveable { mutableStateOf(selectedId.ifBlank { DEFAULT_CHANTAL_MAP_REGIONS.first().id }) }
    val region = DEFAULT_CHANTAL_MAP_REGIONS.firstOrNull { it.id == inspectedId } ?: DEFAULT_CHANTAL_MAP_REGIONS.first()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Карта троп", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
        Box(Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(16.dp))
            .clickable { expanded = true }.testTag("open_world_map")) {
            Image(painterResource(R.drawable.chantal_map), "Открыть карту на весь экран", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        TextButton(onClick = { expanded = true }) { Text("Развернуть и исследовать карту") }
        DEFAULT_CHANTAL_MAP_REGIONS.forEach { place ->
            OutlinedButton(onClick = { inspectedId = place.id }, modifier = Modifier.fillMaxWidth().testTag("region_${place.id}")) {
                Text("${if (journeys >= place.minJourneysRequired) place.emoji else "🔒"} ${place.titleRu}")
            }
        }
        Text(region.titleRu, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
        Text(region.loreRu, color = palette.textPrimary)
        Text("Здесь можно встретить: ${region.npcsRu.joinToString()}", color = palette.textSecondary)
        val unlocked = journeys >= region.minJourneysRequired
        if (!unlocked) Text("Откроется после ${region.minJourneysRequired} походов. Осталось: ${region.minJourneysRequired - journeys}.", color = palette.textSecondary)
        Button(onClick = { onChoose(region.id) }, enabled = unlocked && !locked,
            modifier = Modifier.fillMaxWidth().testTag("choose_map_route")) {
            Text(if (selectedId == region.id) "Маршрут выбран · в домик" else "Выбрать для следующего похода")
        }
        TextButton(onClick = { onChoose("") }, enabled = !locked) { Text("Доверить маршрут Шанталь") }
    }
    if (expanded) Dialog(onDismissRequest = { expanded = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = palette.backgroundTop) {
            Column(Modifier.safeDrawingPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Карта троп · приближай двумя пальцами", color = palette.textPrimary)
                MapCanvas(Modifier.weight(1f), journeys) { inspectedId = it }
                Text(region.titleRu, color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
                Text(if (journeys >= region.minJourneysRequired) region.loreRu else "Доступ после ${region.minJourneysRequired} походов", color = palette.textSecondary)
                Button(onClick = { expanded = false }, modifier = Modifier.fillMaxWidth()) { Text("Подробнее о месте") }
            }
        }
    }
}

@Composable
private fun MapCanvas(modifier: Modifier, journeys: Int, onPlace: (String) -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val points = listOf(.53f to .35f, .25f to .58f, .65f to .74f, .87f to .32f, .57f to .12f)
    Column(modifier) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            fun clamp(value: Offset, zoom: Float) = Offset(value.x.coerceIn(-width * (zoom - 1) / 2, width * (zoom - 1) / 2), value.y.coerceIn(-height * (zoom - 1) / 2, height * (zoom - 1) / 2))
            val gestures = rememberTransformableState { zoom, movement, _ ->
                scale = (scale * zoom).coerceIn(1f, 4f)
                pan = clamp(pan + movement, scale)
            }
            Box(Modifier.fillMaxWidth().aspectRatio(1.5f).graphicsLayer {
                scaleX = scale; scaleY = scale; translationX = pan.x; translationY = pan.y
            }.transformable(gestures)) {
                Image(painterResource(R.drawable.chantal_map), "Карта мира", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    DEFAULT_CHANTAL_MAP_REGIONS.forEachIndexed { index, place ->
                        val point = points.getOrElse(index) { .5f to .5f }
                        FilledTonalButton(onClick = { onPlace(place.id) }, contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.offset(x = maxWidth * point.first - 24.dp, y = maxHeight * point.second - 24.dp)
                                .size(48.dp).testTag("map_pin_${place.id}")) {
                            Text(if (journeys >= place.minJourneysRequired) place.emoji else "🔒")
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { scale = (scale - .5f).coerceAtLeast(1f); pan = Offset.Zero }) { Text("−") }
            TextButton(onClick = { scale = 1f; pan = Offset.Zero }) { Text("Сброс") }
            TextButton(onClick = { scale = (scale + .5f).coerceAtMost(4f) }) { Text("+") }
        }
    }
}
