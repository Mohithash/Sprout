package com.mohithash.sprout.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/** Plant photo in an expressive shape, with a leaf fallback. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlantThumb(base64: String, size: Int = 56) {
    val bmp = remember(base64) { base64.takeIf { it.isNotBlank() }?.let { runCatching { Base64.decode(it, Base64.NO_WRAP).let { b -> BitmapFactory.decodeByteArray(b, 0, b.size) } }.getOrNull() } }
    if (bmp != null) Image(bmp.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.size(size.dp).clip(MaterialShapes.Cookie9Sided.toShape()))
    else ShapeIcon(Icons.Default.Spa, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, MaterialShapes.Cookie9Sided, size)
}
