package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.DEFAULT_CHANTAL_MAP_REGIONS
import com.example.ui.theme.LocalVelarisPalette

// Coordinates refer to the original illustration, not the viewport. The mill has no printed label.
internal data class MapLabelArea(val regionId: String, val left: Float, val top: Float, val right: Float, val bottom: Float)
internal val chantalMapLabelAreas = listOf(
    MapLabelArea("reg_old_mill", .48f, .23f, .64f, .42f),
    MapLabelArea("reg_amber_forest", .235f, .62f, .35f, .675f),
    MapLabelArea("reg_misty_hollow", .59f, .69f, .725f, .755f),
    MapLabelArea("reg_stone_halls", .79f, .04f, .885f, .12f),
    MapLabelArea("reg_stone_halls", .79f, .47f, .94f, .525f),
    MapLabelArea("reg_mountain_pass", .45f, .035f, .635f, .09f)
)

@Composable
internal fun ChantalWorldMap(journeys: Int, selectedId: String, locked: Boolean, onChoose: (String) -> Unit) {
    val palette = LocalVelarisPalette.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var detailsId by rememberSaveable { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Карта троп", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
        val painter = painterResource(R.drawable.chantal_map)
        Image(painter, "Открыть карту на весь экран", Modifier.fillMaxWidth()
            .aspectRatio(painter.intrinsicSize.width / painter.intrinsicSize.height)
            .clip(RoundedCornerShape(16.dp)).clickable { expanded = true }.testTag("open_world_map"), contentScale = ContentScale.Fit)
        TextButton(onClick = { expanded = true }) { Text("Развернуть карту") }
        DEFAULT_CHANTAL_MAP_REGIONS.forEach { place ->
            OutlinedButton(onClick = { detailsId = place.id }, modifier = Modifier.fillMaxWidth().testTag("region_${place.id}")) {
                Text("${if (journeys >= place.minJourneysRequired) place.emoji else "🔒"} ${place.titleRu}")
            }
        }
        TextButton(onClick = { onChoose("") }, enabled = !locked) { Text("Доверить маршрут Шанталь") }
    }
    if (expanded) Dialog(onDismissRequest = { expanded = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = palette.backgroundTop) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                MapCanvas(Modifier.fillMaxSize()) { detailsId = it }
                IconButton(onClick = { expanded = false }, modifier = Modifier.align(Alignment.TopEnd).testTag("close_world_map")) {
                    Text("×", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { contentDescription = "Закрыть карту" })
                }
            }
        }
    }
    val region = DEFAULT_CHANTAL_MAP_REGIONS.firstOrNull { it.id == detailsId }
    if (region != null) Dialog(onDismissRequest = { detailsId = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), contentAlignment = Alignment.Center) {
            Card(Modifier.fillMaxWidth().heightIn(max = maxHeight).testTag("map_place_details")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(region.titleRu, style = MaterialTheme.typography.titleLarge)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(region.loreRu)
                        Text("Здесь можно встретить: ${region.npcsRu.joinToString()}")
                        if (journeys < region.minJourneysRequired) Text("Откроется после ${region.minJourneysRequired} походов. Осталось: ${region.minJourneysRequired - journeys}.")
                        if (locked) Text("Маршрут можно изменить после завершения текущего похода и прочтения истории.")
                    }
                    Button(onClick = { detailsId = null; expanded = false; onChoose(region.id) },
                        enabled = journeys >= region.minJourneysRequired && !locked,
                        modifier = Modifier.fillMaxWidth().testTag("choose_map_route")) {
                        Text(if (selectedId == region.id) "Маршрут выбран · в домик" else "Выбрать для следующего похода")
                    }
                    TextButton(onClick = { detailsId = null }, modifier = Modifier.fillMaxWidth().testTag("close_map_details")) { Text("Вернуться к карте") }
                }
            }
        }
    }
}

@Composable
private fun MapCanvas(modifier: Modifier, onPlace: (String) -> Unit) {
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val painter = painterResource(R.drawable.chantal_map)
    val aspect = painter.intrinsicSize.width / painter.intrinsicSize.height
    BoxWithConstraints(modifier.clip(RoundedCornerShape(0.dp)), contentAlignment = Alignment.Center) {
        val mapWidth = minOf(maxWidth, maxHeight * aspect)
        val mapHeight = mapWidth / aspect
        val density = androidx.compose.ui.platform.LocalDensity.current
        val viewportWidth = constraints.maxWidth.toFloat()
        val viewportHeight = constraints.maxHeight.toFloat()
        val imageWidth = with(density) { mapWidth.toPx() }
        val imageHeight = with(density) { mapHeight.toPx() }
        val gestures = rememberTransformableState { zoom, movement, _ ->
            scale = (scale * zoom).coerceIn(1f, 5f)
            val limitX = ((imageWidth * scale - viewportWidth) / 2).coerceAtLeast(0f)
            val limitY = ((imageHeight * scale - viewportHeight) / 2).coerceAtLeast(0f)
            pan = Offset((pan.x + movement.x).coerceIn(-limitX, limitX), (pan.y + movement.y).coerceIn(-limitY, limitY))
        }
        Box(Modifier.fillMaxSize().transformable(gestures), contentAlignment = Alignment.Center) {
            Box(Modifier.size(mapWidth, mapHeight).graphicsLayer {
                scaleX = scale; scaleY = scale; translationX = pan.x; translationY = pan.y
            }) {
                Image(painter, "Карта мира Шанталь", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                chantalMapLabelAreas.forEachIndexed { index, area ->
                    val place = DEFAULT_CHANTAL_MAP_REGIONS.first { it.id == area.regionId }
                    Box(Modifier.offset(x = mapWidth * area.left, y = mapHeight * area.top)
                        .size(mapWidth * (area.right - area.left), mapHeight * (area.bottom - area.top))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
                            role = Role.Button, onClickLabel = "О месте: ${place.titleRu}") { onPlace(area.regionId) }
                        .testTag("map_label_$index")
                        .then(Modifier.semanticsForMapLabel(place.titleRu)))
                }
            }
        }
    }
}

private fun Modifier.semanticsForMapLabel(title: String) = this.then(Modifier.semantics { contentDescription = title })
