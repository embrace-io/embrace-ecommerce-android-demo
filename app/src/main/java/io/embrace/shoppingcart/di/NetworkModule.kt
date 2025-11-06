package io.embrace.shoppingcart.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.embrace.shoppingcart.mock.MockApiService
import io.embrace.shoppingcart.mock.MockNetworkConfig
import io.embrace.shoppingcart.mock.NetworkScenario
import io.embrace.shoppingcart.mock.MockNetworkConfigOverrides
import io.embrace.shoppingcart.network.ApiService
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder().addInterceptor(logging).build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
            Retrofit.Builder()
                    .baseUrl("https://dash-api.embrace.io/")
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()

    @Provides
    @Singleton
    @RealApi
    fun provideRealApiService(retrofit: Retrofit): ApiService =
            retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideMockNetworkConfig(): MockNetworkConfig {
        MockNetworkConfigOverrides.override?.let { return it }
        fun pickScenario(): NetworkScenario = NetworkScenario.values().random()
        val defaultDelays = listOf(0L, 200L, 500L)
        val slowDelays = listOf(2000L, 3000L, 5000L)
        return MockNetworkConfig(
            defaultDelayMs = defaultDelays.random(),
            slowDelayMs = slowDelays.random(),
            productsScenario = pickScenario(),
            categoriesScenario = pickScenario(),
            placeOrderScenario = pickScenario(),
        )
    }

    @Provides
    @Singleton
    @MockApi
    fun provideMockApiService(
        @ApplicationContext context: Context,
        moshi: Moshi,
        config: MockNetworkConfig
    ): ApiService = MockApiService(context, moshi, config)
}
