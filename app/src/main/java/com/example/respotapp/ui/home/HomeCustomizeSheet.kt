package com.example.respotapp.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.respotapp.domain.HomeModule
import com.example.respotapp.domain.HomeModuleType
import com.example.respotapp.ui.RespotPrimaryButton
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeCustomizeSheet(
    modules: List<HomeModule>,
    onToggleVisibility: (HomeModuleType) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onReorder(from.index, to.index)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Text(
            text = "Customize home",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Drag to reorder. Toggle modules on or off.",
            modifier = Modifier.padding(horizontal = 24.dp),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(modules, key = { it.type }) { module ->
                ReorderableItem(reorderableState, key = module.type) { isDragging ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .then(
                                if (isDragging) {
                                    Modifier.padding(4.dp)
                                } else Modifier
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                text = moduleLabel(module.type),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                            )
                        }
                        Switch(
                            checked = module.isVisible,
                            onCheckedChange = { onToggleVisibility(module.type) }
                        )
                    }
                }
            }
        }

        RespotPrimaryButton(
            text = "Save layout",
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }
}

private fun moduleLabel(type: HomeModuleType): String = when (type) {
    HomeModuleType.RECENTLY_PLAYED -> "Recently played"
    HomeModuleType.VIRTUAL_ALBUMS -> "Virtual albums"
    HomeModuleType.CUSTOM_PLAYLISTS -> "Playlists"
    HomeModuleType.LOCAL_TRACKS -> "Local music"
    HomeModuleType.LISTENING_STATS -> "Listening stats"
}
