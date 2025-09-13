package com.example.linkit.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.linkit.data.TokenStore
import com.example.linkit.data.api.ApiService
import com.example.linkit.data.api.ProjectApiService
import com.example.linkit.data.local.LinkItDatabase
import com.example.linkit.data.local.dao.ConnectionDao
import com.example.linkit.data.local.dao.UserDao
import com.example.linkit.data.repo.AuthRepository
import com.example.linkit.data.repo.ImageRepository
import com.example.linkit.data.repo.ProfileRepository
import com.example.linkit.data.repo.ProjectRepository
import com.example.linkit.network.AuthInterceptor
import com.example.linkit.network.ResponseInterceptor
import com.example.linkit.util.Constants
import com.example.linkit.util.ImageCacheManager
import com.example.linkit.util.NetworkUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val Context.dataStore by preferencesDataStore("auth_prefs")

    @Provides
    @Singleton
    fun provideTokenStore(@ApplicationContext ctx: Context): TokenStore =
        TokenStore(ctx.dataStore)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LinkItDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            LinkItDatabase::class.java,
            "linkit_database"
        )
            .fallbackToDestructiveMigration(false)
            .build()
    }

    @Provides
    fun provideUserDao(database: LinkItDatabase): UserDao = database.userDao()

    @Provides
    fun provideConnectionDao(database: LinkItDatabase): ConnectionDao = database.connectionDao()

    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }

    @Provides
    @Singleton
    fun provideImageCacheManager(@ApplicationContext context: Context): ImageCacheManager {
        return ImageCacheManager(context)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStore: TokenStore): AuthInterceptor {
        return AuthInterceptor(tokenStore)
    }

    @Provides
    @Singleton
    fun provideResponseInterceptor(): ResponseInterceptor {
        return ResponseInterceptor()
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        responseInterceptor: ResponseInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(responseInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: ApiService,
        tokenStore: TokenStore
    ): AuthRepository {
        return AuthRepository(apiService, tokenStore)
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        apiService: ApiService,
        tokenStore: TokenStore,
        userDao: UserDao,
        connectionDao: ConnectionDao,
        networkUtils: NetworkUtils,
        imageCacheManager: ImageCacheManager
    ): ProfileRepository {
        return ProfileRepository(apiService, tokenStore, userDao, connectionDao, networkUtils, imageCacheManager)
    }

    @Provides
    @Singleton
    fun provideImageRepository(
        apiService: ApiService,
        tokenStore: TokenStore
    ): ImageRepository {
        return ImageRepository(apiService, tokenStore)
    }

    @Provides
    @Singleton
    fun provideProjectApiService(retrofit: Retrofit): ProjectApiService {
        return retrofit.create(ProjectApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideProjectRepository(
        projectApiService: ProjectApiService,
        tokenStore: TokenStore,
        networkUtils: NetworkUtils
    ): ProjectRepository {
        return ProjectRepository(projectApiService, tokenStore, networkUtils)
    }
}


