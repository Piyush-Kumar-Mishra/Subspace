package com.example.linkit

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.linkit.util.AuthImageInterceptor
import com.example.linkit.data.TokenStore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LinkItApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var tokenStore: TokenStore

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(AuthImageInterceptor(tokenStore))
            }
            .crossfade(true)
            .build()
    }
}
