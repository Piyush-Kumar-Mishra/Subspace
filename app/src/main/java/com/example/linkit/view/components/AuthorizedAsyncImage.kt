package com.example.linkit.view.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.linkit.R
import com.example.linkit.util.Constants

@Composable
fun AuthorizedAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val fullUrl = remember(imageUrl) {
        val url = imageUrl?.trim().orEmpty()
        when {
            url.isEmpty() -> null
            url.startsWith("http", ignoreCase = true) -> url
            else -> Constants.BASE_URL + url.removePrefix("/")
        }
    }

    AsyncImage(
        model = fullUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = painterResource(R.drawable.ic_person),
        error = painterResource(R.drawable.ic_person)
    )
}
