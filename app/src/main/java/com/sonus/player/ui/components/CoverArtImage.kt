package com.sonus.player.ui.components

import android.content.ContentUris
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.sonus.player.domain.model.Track

@Composable
fun CoverArtImage(
    track: Track?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    if (track == null) {
        PlaceholderArt(modifier = modifier, size = size)
        return
    }

    // Use coverArtUrl for streaming tracks, albumArt URI for local
    val imageModel = if (track.coverArtUrl != null) {
        track.coverArtUrl
    } else {
        getAlbumArtUri(track.albumId)
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageModel)
            .crossfade(true)
            .build(),
        contentDescription = "${track.album} cover art",
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop,
        loading = {
            PlaceholderArt(size = size)
        },
        error = {
            GeneratedGradient(artistName = track.artist, size = size)
        }
    )
}

@Composable
private fun PlaceholderArt(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(size / 2),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GeneratedGradient(
    artistName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val hash = artistName.hashCode()
    val hue1 = (kotlin.math.abs(hash) % 360).toFloat()
    val hue2 = (hue1 + 60f) % 360f

    val color1 = Color.hsl(hue1, 0.6f, 0.5f)
    val color2 = Color.hsl(hue2, 0.5f, 0.4f)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(color1, color2))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(size / 3),
            tint = Color.White.copy(alpha = 0.7f)
        )
    }
}

private fun getAlbumArtUri(albumId: Long): Uri {
    val albumArtUri = Uri.parse("content://media/external/audio/albumart")
    return ContentUris.withAppendedId(albumArtUri, albumId)
}
