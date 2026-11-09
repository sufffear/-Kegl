package com.example.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

/** Shared spacing; colors/type remain in the existing VelarisTheme. */
internal object VelarisSpacing {
    val Unit = 8.dp
    val Content = 16.dp
    val MaxContentWidth = 600.dp
    val TouchTarget = 48.dp
}

/** Scroll state is owned by the route's SaveableStateProvider, not a game repository. */
@Composable
internal fun VelarisScreen(scrollState: ScrollState, modifier: Modifier = Modifier,
    horizontalPadding: Dp = VelarisSpacing.Content,
    verticalPadding: Dp = VelarisSpacing.Content,
    maxContentWidth: Dp = VelarisSpacing.MaxContentWidth,
    content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.widthIn(max = maxContentWidth).fillMaxSize()
        .verticalScroll(scrollState).padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(VelarisSpacing.Content), content = content)
}

@Composable
internal fun VelarisPrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = com.example.ui.theme.LocalVelarisPalette.current
    androidx.compose.material3.Button(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = palette.primaryAccent, contentColor = palette.onPrimaryAccent)) {
        androidx.compose.material3.Text(text, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun VelarisSectionLink(title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = com.example.ui.theme.LocalVelarisPalette.current
    androidx.compose.material3.OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            androidx.compose.material3.Text(title, color = palette.textPrimary, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            androidx.compose.material3.Text(subtitle, color = palette.textSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}
