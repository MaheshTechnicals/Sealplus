package com.junkfood.seal.ui.component.card

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.junkfood.seal.R

@Composable
fun RecentDownloadCardActionMenu(
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onActionClick: (DownloadAction) -> Unit
) {
    Box {
        IconButton(onClick = onExpand) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.show_more_actions),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            DownloadAction.entries.forEach { action ->
                DropdownMenuItem(
                    text = { Text(stringResource(action.labelRes)) },
                    onClick = { onActionClick(action) },
                    leadingIcon = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            tint = if (action == DownloadAction.COPY_LINK)
                                MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.secondary
                        )
                    }
                )
            }
        }
    }
}