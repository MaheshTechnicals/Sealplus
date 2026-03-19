package com.junkfood.seal.ui.component.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.ui.theme.recentCardContainer

@Composable
fun RecentDownloadCard(
    downloadInfo: DownloadedVideoInfo,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onShowDetails: () -> Unit,
    modifier: Modifier = Modifier
) {

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.recentCardContainer()
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RecentDownloadThumbnail(downloadInfo.thumbnailUrl)

            RecentDownloadCardContent(
                modifier = Modifier.weight(1f),
                title = downloadInfo.videoTitle,
                statusText = stringResource(R.string.completed),
                progressText = "100%"
            )

            // More button with dropdown menu
            RecentDownloadCardActionMenu(
                expanded = showMenu,
                onDismiss = { showMenu = false },
                onExpand = { showMenu = true },
                onActionClick = { action ->
                    when (action) {
                        DownloadAction.DETAILS -> onShowDetails()
                        DownloadAction.SHARE -> onShare()
                        DownloadAction.COPY_LINK -> onCopyLink()
                    }
                    showMenu = false
                }
            )
        }
    }
}
