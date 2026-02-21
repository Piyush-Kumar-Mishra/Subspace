package com.example.linkit.view.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.linkit.R



@Composable
fun AuthorizedAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    isOffline: Boolean = false,
    cacheKey: String? = null
) {
    val context = LocalContext.current
    val effectiveKey = remember(imageUrl, cacheKey) {
        if (imageUrl == null) null
        else if (cacheKey != null) "${cacheKey}_$imageUrl"
        else imageUrl
    }

    val imageRequest = remember(imageUrl, isOffline, cacheKey) {
        coil.request.ImageRequest.Builder(context)
            .data(imageUrl)
            .apply {
                if (cacheKey != null) {
                    diskCacheKey(effectiveKey)
                    memoryCacheKey(effectiveKey)
                }
            }
            .networkCachePolicy(if (isOffline) coil.request.CachePolicy.DISABLED else coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .build()
    }

    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}