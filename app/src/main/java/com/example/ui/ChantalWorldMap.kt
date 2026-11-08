package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.DEFAULT_CHANTAL_MAP_REGIONS
import com.example.data.ChantalWorldMemory
import org.json.JSONObject

internal data class ChantalMapAnchor(val id: String, val x: Float, val y: Float)
internal val chantalMapAnchors = listOf(
    ChantalMapAnchor("reg_old_mill", .55f, .43f), ChantalMapAnchor("reg_amber_forest", .27f, .65f),
    ChantalMapAnchor("reg_misty_hollow", .64f, .77f), ChantalMapAnchor("reg_stone_halls", .85f, .48f),
    ChantalMapAnchor("reg_mountain_pass", .55f, .08f))
internal fun clampMapCenter(center: Float, image: Float, viewport: Float, scale: Float): Float {
    val half = (viewport / (2f * image * scale)).coerceAtMost(.5f)
    return center.coerceIn(half, 1f-half)
}
private fun records(memory: JSONObject, key: String) = memory.getJSONArray(key).let { a -> (0 until a.length()).map { a.getJSONObject(it) } }

@Composable
internal fun ChantalWorldMap(journeys: Int, selectedId: String, locked: Boolean,
    memory: JSONObject = ChantalWorldMemory.empty(), onChoose: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var detailsId by rememberSaveable { mutableStateOf<String?>(null) }
    var listOpen by rememberSaveable { mutableStateOf(false) }
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var centerX by rememberSaveable { mutableFloatStateOf(.5f) }
    var centerY by rememberSaveable { mutableFloatStateOf(.5f) }
    fun closeDetails() { detailsId = null; listOpen = false }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Карта троп", color=HobbitCream, style=MaterialTheme.typography.titleLarge)
        Image(painterResource(R.drawable.chantal_map_clean), "Открыть карту на весь экран", Modifier.fillMaxWidth().aspectRatio(1.79f)
            .clickable { expanded=true }.testTag("open_world_map"), contentScale=ContentScale.Fit)
        TextButton(onClick={expanded=true}) { Text("Развернуть карту") }
        DEFAULT_CHANTAL_MAP_REGIONS.forEach { place ->
            OutlinedButton(onClick={expanded=true; detailsId=place.id},modifier=Modifier.fillMaxWidth().testTag("region_${place.id}")) {
                Text("${if(journeys<place.minJourneysRequired) "🔒 " else ""}${place.titleRu}")
            }
        }
        TextButton(onClick={onChoose("")},enabled=!locked) { Text("Доверить маршрут Шанталь") }
    }
    if(expanded) Dialog(onDismissRequest={if(detailsId!=null || listOpen) closeDetails() else expanded=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=HobbitBg) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    TextButton(onClick={listOpen=!listOpen; detailsId=null}) { Text("Места",color=HobbitGold) }
                    TextButton(onClick={expanded=false; closeDetails()},modifier=Modifier.testTag("close_world_map")) { Text("Закрыть",color=HobbitCream) }
                }
                BoxWithConstraints(Modifier.weight(1f)) {
                    val landscape=maxWidth>maxHeight
                    val panelVisible=detailsId!=null || listOpen
                    val map: @Composable (Modifier)->Unit = { modifier ->
                        MapCanvas(modifier,scale,centerX,centerY,selectedId,memory,
                            onCamera={s,x,y->scale=s;centerX=x;centerY=y},onPlace={if(it.isEmpty()) { detailsId=null;listOpen=true } else { detailsId=it;listOpen=false }})
                    }
                    val panel: @Composable (Modifier)->Unit = { modifier ->
                        Surface(modifier,color=HobbitCardBg,shape=RoundedCornerShape(18.dp)) {
                            if(listOpen) Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                                DEFAULT_CHANTAL_MAP_REGIONS.forEach { region -> TextButton(onClick={detailsId=region.id;listOpen=false},modifier=Modifier.fillMaxWidth()) { Text(region.titleRu,color=HobbitCream) } }
                                TextButton(onClick={closeDetails()}) { Text("Вернуться к карте") }
                            } else DEFAULT_CHANTAL_MAP_REGIONS.firstOrNull { it.id==detailsId }?.let { region ->
                                Column(Modifier.padding(12.dp).testTag("map_place_details"),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                                        Text(region.titleRu,color=HobbitGold,style=MaterialTheme.typography.titleLarge)
                                        Text(region.loreRu,color=HobbitCream)
                                        val events=records(memory,"events").filter { it.getString("regionRu")==region.titleRu }
                                        Text("Завершённых приключений: ${events.size}",color=HobbitMuted)
                                        Text("Здесь можно встретить: ${region.npcsRu.joinToString()}",color=HobbitCream)
                                        if(journeys<region.minJourneysRequired) Text("Откроется после ${region.minJourneysRequired} походов. Осталось: ${region.minJourneysRequired-journeys}.",color=HobbitCream)
                                        if(locked) Text("Маршрут можно изменить после завершения похода и прочтения истории.",color=HobbitMuted)
                                        val sources=events.map { it.getString("episodeId") }.toSet()
                                        for(category in listOf("promises","relationships","magicItems","keepsakes")) records(memory,category).filter { it.getString("sourceEpisodeId") in sources }.takeLast(3).forEach { fact ->
                                            val event=events.first { it.getString("episodeId")==fact.getString("sourceEpisodeId") }
                                            DiaryEntry((if(category=="promises") if(fact.getString("status")=="open") "Дело: " else "✓ " else "")+fact.getString("descriptionRu"),
                                                (if(category=="magicItems") "Свойство: ${fact.getString("propertyRu")}\n" else "")+"${event.getString("summaryRu")}\nТвой ответ: ${event.getString("choiceRu")}")
                                        }
                                        events.takeLast(3).reversed().forEach { DiaryEntry("Поход ${it.getInt("journeyNumber")}","${it.getString("summaryRu")}\n${it.getString("choiceRu")}") }
                                    }
                                    Button(onClick={onChoose(region.id);closeDetails();expanded=false},enabled=journeys>=region.minJourneysRequired && !locked,modifier=Modifier.fillMaxWidth().testTag("choose_map_route")) {
                                        Text(if(selectedId==region.id) "Маршрут выбран" else "Выбрать маршрут")
                                    }
                                    TextButton(onClick={closeDetails()},modifier=Modifier.fillMaxWidth().testTag("close_map_details")) { Text("Вернуться к карте") }
                                }
                            }
                        }
                    }
                    if(landscape) Row(Modifier.fillMaxSize()) { map(Modifier.weight(1f).fillMaxHeight()); if(panelVisible) panel(Modifier.weight(1f).fillMaxHeight()) }
                    else Column(Modifier.fillMaxSize()) { map(Modifier.weight(if(panelVisible) .48f else 1f).fillMaxWidth()); if(panelVisible) panel(Modifier.weight(.52f).fillMaxWidth()) }
                }
            }
        }
    }
}

@Composable
private fun MapCanvas(modifier:Modifier, scale:Float, centerX:Float, centerY:Float, selected:String,memory:JSONObject,
    onCamera:(Float,Float,Float)->Unit,onPlace:(String)->Unit) {
    val painter=painterResource(R.drawable.chantal_map_clean)
    val currentScale by rememberUpdatedState(scale)
    val currentX by rememberUpdatedState(centerX)
    val currentY by rememberUpdatedState(centerY)
    BoxWithConstraints(modifier.clipToBounds(),contentAlignment=Alignment.Center) {
        val w=constraints.maxWidth.toFloat();val h=constraints.maxHeight.toFloat()
        val aspect=painter.intrinsicSize.width/painter.intrinsicSize.height
        val iw=minOf(w,h*aspect);val ih=iw/aspect
        val density=androidx.compose.ui.platform.LocalDensity.current
        val cx=clampMapCenter(centerX,iw,w,scale);val cy=clampMapCenter(centerY,ih,h,scale)
        fun transform(focus:Offset, pan:Offset, factor:Float) {
            val old=currentScale;val next=(old*factor).coerceIn(1f,5f)
            val oldX=clampMapCenter(currentX,iw,w,old);val oldY=clampMapCenter(currentY,ih,h,old)
            val x=oldX+(focus.x-w/2)/iw*(1/old-1/next)-pan.x/(iw*next)
            val y=oldY+(focus.y-h/2)/ih*(1/old-1/next)-pan.y/(ih*next)
            onCamera(next,clampMapCenter(x,iw,w,next),clampMapCenter(y,ih,h,next))
        }
        Box(Modifier.fillMaxSize().testTag("map_gesture_surface").semantics { stateDescription="Масштаб $scale" }
            .pointerInput(w,h) { detectTransformGestures { focus,pan,zoom,_->transform(focus,pan,zoom) } }
            .pointerInput(w,h) { detectTapGestures(onTap={ tap ->
                val s=currentScale
                val x=clampMapCenter(currentX,iw,w,s);val y=clampMapCenter(currentY,ih,h,s)
                val matches=chantalMapAnchors.filter { a ->
                    kotlin.math.abs(tap.x-(w/2+(a.x-x)*iw*s))<=with(density){48.dp.toPx()} &&
                    kotlin.math.abs(tap.y-(h/2+(a.y-y)*ih*s))<=with(density){24.dp.toPx()}
                }
                if(matches.size==1) onPlace(matches.first().id) else if(matches.size>1) onPlace("")
            },onDoubleTap={p-> if(currentScale>1.05f) onCamera(1f,.5f,.5f) else transform(p,Offset.Zero,2.5f)}) },contentAlignment=Alignment.Center) {
            Image(painter,"Пергаментная карта",Modifier.size(with(density){iw.toDp()},with(density){ih.toDp()}).graphicsLayer {
                scaleX=scale;scaleY=scale;translationX=(.5f-cx)*iw*scale;translationY=(.5f-cy)*ih*scale
            },contentScale=ContentScale.Fit)
            chantalMapAnchors.forEachIndexed { index,a ->
                val region=DEFAULT_CHANTAL_MAP_REGIONS.first { it.id==a.id }
                val px=w/2+(a.x-cx)*iw*scale;val py=h/2+(a.y-cy)*ih*scale
                if(px in 0f..w && py in 0f..h) Box(Modifier.align(Alignment.TopStart)
                    .offset { IntOffset((px-with(density){48.dp.toPx()}).toInt(),(py-with(density){24.dp.toPx()}).toInt()) }
                    .size(width=96.dp,height=48.dp)
                    .testTag("map_label_$index").semantics { contentDescription=region.titleRu; role=Role.Button; onClick { onPlace(a.id);true } },contentAlignment=Alignment.Center) {
                    // Keep geographic labels within their hit areas; full-size names remain in the accessible list and detail card.
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f))) {
                        Text((if(selected==a.id) "• " else "")+region.shortTitleRu,color=Color(0xFF302015),style=MaterialTheme.typography.labelLarge,
                            maxLines=1, overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
            val lantern = records(memory,"keepsakes").firstOrNull { it.getString("id")=="wooden_light" }
            val lanternEvent = lantern?.let { fact -> records(memory,"events").firstOrNull { it.getString("episodeId")==fact.getString("sourceEpisodeId") } }
            val lanternRegion = lanternEvent?.let { event -> DEFAULT_CHANTAL_MAP_REGIONS.firstOrNull { it.titleRu==event.getString("regionRu") } }
            if(lanternRegion != null) {
                val x=w/2+(.39f-cx)*iw*scale;val y=h/2+(.70f-cy)*ih*scale
                if(x in 0f..w && y in 0f..h) Text("✦",color=Color(0xFF8E4808),modifier=Modifier.align(Alignment.TopStart).offset { IntOffset(x.toInt(),y.toInt()) }
                    .size(48.dp).clickable { onPlace(lanternRegion.id) }.semantics { contentDescription="Фонарь у переправы: память о Брамбле" })
            }
        }
    }
}
