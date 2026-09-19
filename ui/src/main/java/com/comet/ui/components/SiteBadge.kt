package com.comet.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.comet.ui.theme.TextTertiary

/**
 * Site badge (Part 2.5): favicon via https://{domain}/favicon.ico (Coil, cached),
 * 16dp, globe line icon fallback.
 */
@Composable
fun SiteBadge(
    domain: String?,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    val shaped = modifier.size(size).clip(CircleShape)
    if (domain.isNullOrBlank()) {
        Icon(
            imageVector = Icons.Rounded.Public,
            contentDescription = null,
            tint = TextTertiary,
            modifier = shaped,
        )
        return
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data("https://$domain/favicon.ico")
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = shaped,
        loading = { GlobeFallback(size) },
        error = { GlobeFallback(size) },
    )
}

@Composable
private fun GlobeFallback(size: Dp) {
    Icon(
        imageVector = Icons.Rounded.Public,
        contentDescription = null,
        tint = TextTertiary,
        modifier = Modifier.size(size),
    )
}
