package io.embrace.shoppingcart.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.embrace.shoppingcart.network.ApiService
import io.embrace.shoppingcart.network.HybridApiService
import io.embrace.shoppingcart.BuildConfig
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiBindingModule {
    @Provides
    @Singleton
    fun provideApiService(
        @RealApi real: ApiService,
        @MockApi mock: ApiService
    ): ApiService = if (BuildConfig.USE_MOCK) {
        mock
    } else {
        HybridApiService(real, mock)
    }
}

