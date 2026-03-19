package com.junkfood.seal.ui.component.card

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.graphics.vector.ImageVector
import com.junkfood.seal.R

enum class DownloadAction(val labelRes: Int, val icon: ImageVector) {
    DETAILS(R.string.details, Icons.Outlined.Info),
    SHARE(R.string.share, Icons.Outlined.Share),
    COPY_LINK(R.string.copy_link, Icons.Outlined.Link)
}