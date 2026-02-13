package com.vigipro.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Square
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.vigipro.core.ui.theme.Dimens

enum class GridLayout(val columns: Int, val icon: ImageVector, val label: String) {
    SINGLE(1, Icons.Default.Square, "1x1"),
    GRID_2X2(2, Icons.Default.GridView, "2x2"),
    GRID_3X3(3, Icons.Default.Grid3x3, "3x3"),
    GRID_4X4(4, Icons.Default.Grid4x4, "4x4"),
}

@Composable
fun GridLayoutToggle(
    currentLayout: GridLayout,
    onLayoutChange: (GridLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextLayout = when (currentLayout) {
        GridLayout.SINGLE -> GridLayout.GRID_2X2
        GridLayout.GRID_2X2 -> GridLayout.GRID_3X3
        GridLayout.GRID_3X3 -> GridLayout.GRID_4X4
        GridLayout.GRID_4X4 -> GridLayout.SINGLE
    }

    IconButton(
        onClick = { onLayoutChange(nextLayout) },
        modifier = modifier,
    ) {
        Icon(
            imageVector = currentLayout.icon,
            contentDescription = "Layout ${currentLayout.label}",
            modifier = Modifier.size(Dimens.IconMd),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
